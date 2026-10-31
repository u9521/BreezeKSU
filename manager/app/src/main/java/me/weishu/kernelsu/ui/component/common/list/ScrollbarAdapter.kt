package me.weishu.kernelsu.ui.component.common.list

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Bridge between a scrollbar and a scrollable state. */
interface ScrollbarAdapter {
    val scrollOffset: Double
    val contentSize: Double
    val viewportSize: Double
    suspend fun scrollTo(scrollOffset: Double)
}

/** [ScrollbarAdapter] for [ScrollState]. */
internal class ScrollableScrollbarAdapter(
    private val scrollState: ScrollState
) : ScrollbarAdapter {

    override val scrollOffset: Double
        get() = scrollState.value.toDouble()

    override val contentSize: Double
        get() = (scrollState.maxValue + scrollState.viewportSize).toDouble()

    override val viewportSize: Double
        get() = scrollState.viewportSize.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        scrollState.scrollTo(scrollOffset.roundToInt())
    }
}

/** [ScrollbarAdapter] for [LazyListState], estimating total size from visible items. */
internal class LazyListScrollbarAdapter(
    private val scrollState: LazyListState
) : ScrollbarAdapter {

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo) {
            if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
        }.toDouble()

    private val totalLineCount get() = scrollState.layoutInfo.totalItemsCount
    private val contentPadding get() = with(scrollState.layoutInfo) { beforeContentPadding + afterContentPadding }
    private val lineSpacing get() = scrollState.layoutInfo.mainAxisItemSpacing

    private fun averageVisibleLineSize(): Double {
        val visibleItems = scrollState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0.0
        val first = visibleItems.first()
        val last = visibleItems.last()
        val count = visibleItems.size
        return (last.offset + last.size - first.offset - (count - 1) * lineSpacing).toDouble() / count
    }

    private val averageVisibleLineSizeWithSpacing get() = averageVisibleLineSize() + lineSpacing

    override val scrollOffset: Double
        get() {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            val firstVisibleItem = visibleItems.firstOrNull() ?: return 0.0

            return firstVisibleItem.index * averageVisibleLineSizeWithSpacing - firstVisibleItem.offset
        }

    override val contentSize: Double
        get() {
            val totalCount = totalLineCount
            return averageVisibleLineSize() * totalCount +
                    lineSpacing * (totalCount - 1).coerceAtLeast(0) +
                    contentPadding
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this.scrollOffset

        if (abs(distance) <= viewportSize) {
            scrollState.scrollBy(distance.toFloat())
        } else {
            snapTo(scrollOffset)
        }
    }

    private suspend fun snapTo(scrollOffset: Double) {
        val maxOffset = (contentSize - viewportSize).coerceAtLeast(0.0)
        val coercedOffset = scrollOffset.coerceIn(0.0, maxOffset)
        val avgSize = averageVisibleLineSizeWithSpacing

        if (avgSize <= 0) return

        val index = (coercedOffset / avgSize).toInt().coerceIn(0, totalLineCount - 1)
        val offset = (coercedOffset - index * avgSize).toInt().coerceAtLeast(0)

        scrollState.scrollToItem(index, offset)
    }
}

/** Maps [ScrollbarAdapter] metrics to thumb size/position and handles thumb drag. */
internal class SliderAdapter(
    val adapter: ScrollbarAdapter,
    private val trackSize: Int,
    private val minHeight: Float,
    private val reverseLayout: Boolean,
    private val isVertical: Boolean,
    private val coroutineScope: CoroutineScope
) {
    private val contentSize get() = adapter.contentSize

    private val visiblePart: Double
        get() {
            val cs = contentSize
            return if (cs == 0.0) 1.0 else (adapter.viewportSize / cs).coerceAtMost(1.0)
        }

    val thumbSize: Double
        get() = (trackSize * visiblePart).coerceAtLeast(minHeight.toDouble())

    private val scrollScale: Double
        get() {
            val extraScrollbarSpace = trackSize - thumbSize
            val extraContentSpace = (contentSize - adapter.viewportSize).coerceAtLeast(0.0)
            return if (extraContentSpace == 0.0) 1.0 else extraScrollbarSpace / extraContentSpace
        }

    private val rawPosition: Double
        get() = scrollScale * adapter.scrollOffset

    val position: Double
        get() = if (reverseLayout) trackSize - thumbSize - rawPosition else rawPosition

    private var unscrolledDragDistance = 0.0
    private val dragMutex = Mutex()

    fun onDragStarted() {
        unscrolledDragDistance = 0.0
    }

    fun onDragDelta(offset: Offset) {
        coroutineScope.launch {
            dragMutex.withLock {
                val dragDelta = if (isVertical) offset.y else offset.x
                val currentPosition = position
                val maxPosition = (trackSize - thumbSize).coerceAtLeast(0.0)

                val targetThumbPosition = (currentPosition + dragDelta + unscrolledDragDistance)
                    .coerceIn(0.0, maxPosition)
                val sliderDelta = targetThumbPosition - currentPosition

                val targetContentOffset = if (reverseLayout) {
                    (trackSize - thumbSize - targetThumbPosition) / scrollScale
                } else {
                    targetThumbPosition / scrollScale
                }

                adapter.scrollTo(targetContentOffset)
                // Track overshoot past track bounds so reversing direction responds immediately.
                unscrolledDragDistance += dragDelta - sliderDelta
            }
        }
    }
}

/** [ScrollbarAdapter] for [LazyGridState], estimating total size by rows/columns. */
internal class LazyGridScrollbarAdapter(
    private val scrollState: LazyGridState
) : ScrollbarAdapter {

    private val isVertical: Boolean
        get() = scrollState.layoutInfo.orientation == Orientation.Vertical

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo) {
            if (isVertical) viewportSize.height else viewportSize.width
        }.toDouble()

    private val totalItemsCount get() = scrollState.layoutInfo.totalItemsCount
    private val contentPadding get() = with(scrollState.layoutInfo) { beforeContentPadding + afterContentPadding }
    private val lineSpacing get() = scrollState.layoutInfo.mainAxisItemSpacing

    private fun lineOf(item: LazyGridItemInfo) = if (isVertical) item.row else item.column
    private fun mainAxisOffset(item: LazyGridItemInfo) =
        if (isVertical) item.offset.y else item.offset.x

    private fun mainAxisSize(item: LazyGridItemInfo) =
        if (isVertical) item.size.height else item.size.width

    private fun averageVisibleLineSize(): Double {
        val visibleItems = scrollState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0.0

        val firstItem = visibleItems.first()
        val lastItem = visibleItems.last()
        val firstLine = lineOf(firstItem)
        val lastLine = lineOf(lastItem)
        val lineCount = lastLine - firstLine + 1
        if (lineCount <= 1) return mainAxisSize(firstItem).toDouble()

        val totalDistance = mainAxisOffset(lastItem) - mainAxisOffset(firstItem)
        return (totalDistance.toDouble() / (lineCount - 1)) - lineSpacing
    }

    private val averageVisibleLineSizeWithSpacing get() = averageVisibleLineSize() + lineSpacing

    private val averageItemsPerLine: Double
        get() {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return 1.0
            val lineCount = lineOf(visibleItems.last()) - lineOf(visibleItems.first()) + 1
            return visibleItems.size.toDouble() / lineCount
        }

    private val estimatedTotalLines: Double
        get() {
            val itemsPerLine = averageItemsPerLine
            if (itemsPerLine == 0.0) return 0.0
            return totalItemsCount.toDouble() / itemsPerLine
        }

    override val scrollOffset: Double
        get() {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            val firstVisibleItem = visibleItems.firstOrNull() ?: return 0.0
            val firstLine = lineOf(firstVisibleItem)
            return firstLine * averageVisibleLineSizeWithSpacing - mainAxisOffset(firstVisibleItem)
        }

    override val contentSize: Double
        get() {
            val totalLines = estimatedTotalLines
            return averageVisibleLineSize() * totalLines +
                    lineSpacing * (totalLines - 1).coerceAtLeast(0.0) +
                    contentPadding
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this.scrollOffset
        if (abs(distance) <= viewportSize) {
            scrollState.scrollBy(distance.toFloat())
        } else {
            snapTo(scrollOffset)
        }
    }

    private suspend fun snapTo(scrollOffset: Double) {
        val maxOffset = (contentSize - viewportSize).coerceAtLeast(0.0)
        val coercedOffset = scrollOffset.coerceIn(0.0, maxOffset)
        val avgSize = averageVisibleLineSizeWithSpacing
        if (avgSize <= 0) return

        val targetLine = (coercedOffset / avgSize).toInt()
        val offset = (coercedOffset - targetLine * avgSize).toInt().coerceAtLeast(0)
        val targetIndex =
            (targetLine * averageItemsPerLine).toInt().coerceIn(0, totalItemsCount - 1)
        scrollState.scrollToItem(targetIndex, offset)
    }
}

internal val SliderAdapter.thumbPixelRange: IntRange
    get() {
        val start = position.roundToInt()
        val end = (start + thumbSize.roundToInt())
        return start until end
    }

internal val IntRange.size get() = last + 1 - first
internal fun Modifier.scrollbarDrag(
    sliderAdapter: SliderAdapter
): Modifier = pointerInput(sliderAdapter) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        sliderAdapter.onDragStarted()
        drag(down.id) { change ->
            sliderAdapter.onDragDelta(change.positionChange())
            change.consume()
        }
    }
}