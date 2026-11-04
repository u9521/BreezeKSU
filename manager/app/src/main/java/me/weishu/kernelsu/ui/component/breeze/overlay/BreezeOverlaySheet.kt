package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Backdrop strength for the sheet, 0 to 1, driven by how much of the sheet is on screen.
 *
 * `SheetState` anchors the sheet by its top edge, so [SheetState.requireOffset] is that edge's y
 * position inside the window: the hidden anchor is the window height and the expanded anchor is
 * one sheet height above it. Reach is therefore the sheet height, and the window height has to be
 * known only to locate the hidden anchor.
 *
 * The raw travel is shaped by [OverlayEnterEasing] before it reaches the backdrop. A sheet leaves
 * the bottom edge before it covers anything worth blurring, so a linear mapping keeps the page
 * sharp through the most visible part of the slide; the emphasized curve puts most of the blur in
 * the first third and flattens after half travel, which is also what stops the radius from visibly
 * breathing during the long second half. The mapping is one curve, not two stages: it stays
 * continuous through the partially expanded stop without having to know where that anchor is.
 *
 * Guarded because `requireOffset()` throws before the first layout and `hasExpandedState` stays
 * false until the anchors exist.
 *
 * Reading the offset during composition is snapshot-tracked: it is a MutableFloatState inside
 * `AnchoredDraggableState`, so drag, `show()` and `hide()` all move this value per frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class SheetFraction(
    private val state: SheetState,
    private val windowHeight: Int,
    private val sheetHeight: () -> Int,
) {
    fun value(): Float {
        if (!state.hasExpandedState) return 0f
        val reach = sheetHeight().toFloat()
        if (reach <= 0f) return 0f
        val offset = state.requireOffset()
        if (offset.isNaN()) return 0f
        val hiddenOffset = windowHeight.toFloat()
        if (hiddenOffset <= 0f) return 0f
        // How far the top edge has risen above the hidden anchor, over the whole travel.
        val travel = (1f - (offset - (hiddenOffset - reach)) / reach).coerceIn(0f, 1f)
        return OverlayEnterEasing.transform(travel)
    }
}

/** Insets equal to the sheet's drag offset, so a sheet stopped mid-screen consumes no top inset. */
@OptIn(ExperimentalMaterial3Api::class)
private class SheetWindowInsets(private val state: SheetState) : WindowInsets {
    // `requireOffset()` throws before the first layout pass; report 0 until anchors exist.
    private val offset: Int
        get() = if (state.hasExpandedState) state.requireOffset().toInt().coerceAtLeast(0) else 0

    override fun getTop(density: Density): Int = offset

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getBottom(density: Density): Int = 0

    override fun equals(other: Any?): Boolean =
        this === other || (other is SheetWindowInsets && other.state == state)

    override fun hashCode(): Int = state.hashCode()
}

/**
 * Material3 [BottomSheet] hosted as a layer in [BreezeOverlay] rather than its own dialog window.
 *
 * Parameters mirror `ModalBottomSheet` so a sheet body can be moved between the two unchanged.
 * The sheet is only as tall as its content: fill the window by giving the body a scrollable
 * `weight(1f)` child, not by passing a size.
 *
 * @param visible whether the sheet should be shown; the composable must remain in the tree while
 *   [visible] is `false` so the slide-out animation can complete. M3 unloads instead, which would
 *   cut the exit animation short.
 * @param sheetState anchors and current position; build it with `rememberBottomSheetState`.
 *   Reach is measured to the expanded anchor, so a sheet that keeps the partially expanded stop
 *   and is taller than half the window rests at a fraction below 1 — below full blur — rather than
 *   full strength. Pass `enabledValues = setOf(Hidden, Expanded)` to drop that stop.
 * @param sheetMaxWidth width cap; the sheet is centred in the overlay window when narrower.
 * @param backdrop what is painted behind the sheet; `null` follows the host, so turning blur off
 *   for the whole stack turns it off here too. Pass `OverlayBackdrop.sheetBlur()` to keep this
 *   one blurred regardless, or `OverlayBackdrop.Scrim` to keep it sharp.
 * @param contentWindowInsets insets applied inside the sheet surface, to the drag handle and the
 *   content alike. The surface itself still reaches the window edge, which is what lets an
 *   expanded sheet paint its background under the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreezeOverlaySheet(
    onDismissRequest: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    backdrop: OverlayBackdrop? = null,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    // Modal sheets sit flat: 0.dp matches ModalBottomSheet, while plain BottomSheet uses 1.dp.
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.modalWindowInsets
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    // Keep the overlay layer mounted until `state.hide()` finishes sliding out.
    var present by remember { mutableStateOf(false) }
    if (visible) present = true

    // Defer `onDismissRequest` until `state.hide()` completes so caller state stays intact during exit.
    var dismissing by remember { mutableStateOf(false) }

    // Reset above the `!present` early return so `dismissing` is cleared after the layer unmounts.
    LaunchedEffect(visible) {
        if (!visible) dismissing = false
    }

    if (!present) return

    var sheetHeight by remember { mutableIntStateOf(0) }
    // The hidden anchor sits at the window height; the dialog window is always full screen.
    val windowInfo = LocalWindowInfo.current
    val windowHeight = windowInfo.containerSize.height

    // Wait for a non-zero measured height and one frame before `show()` so BottomSheet does not
    // promote the initial partial anchor to expanded while anchors are still converged at 0.
    var shown by remember { mutableStateOf(false) }
    val onScreen = visible && !dismissing
    LaunchedEffect(sheetState, onScreen) {
        if (!onScreen) {
            // Slide out and fade the backdrop together, then unmount only once both are done:
            // unmounting when `hide()` returns pops the blur whenever the slide is the shorter one.
            coroutineScope {
                launch {
                    sheetState.hide()
                    if (dismissing) onDismissRequest()
                }
                launch { delay(OverlayExitMillis.milliseconds) }
            }
            present = false
            return@LaunchedEffect
        }
        if (!shown) {
            snapshotFlow { sheetState.hasExpandedState && sheetHeight > 0 }.first { it }
            withFrameNanos { }
            shown = true
        }
        sheetState.show()
    }

    val sheetWindowInsets = remember(sheetState) { SheetWindowInsets(sheetState) }

    // Every visible frame of the sheet comes from one offset: drag, show(), hide() and the anchors
    // settling all move it. Driving the backdrop from it is what keeps the two in step.
    val fraction = remember(sheetState, windowHeight) {
        SheetFraction(
            state = sheetState,
            windowHeight = windowHeight,
            sheetHeight = { sheetHeight },
        )
    }

    BreezeOverlay(
        onDismissRequest = { dismissing = true },
        visible = true,
        dismissOnClickOutside = dismissOnClickOutside,
        progress = OverlayProgress.External { fraction.value() },
        backdrop = backdrop,
        surfaceColor = containerColor,
    ) {
        // The surface reaches the window edge; only the handle and content are inset. Consuming the
        // offset keeps a partially expanded sheet from leaving a status-bar-high strip above it.
        BottomSheet(
            // Caller modifiers go first: a caller padding such as `statusBarsPadding()` reads the
            // window insets live, so applying it inside `consumeWindowInsets(sheetWindowInsets)`
            // would make its top padding shrink by the drag offset and the sheet's constraints grow
            // by the same amount on every drag frame — a 60Hz oscillation that blocks collapsing.
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(modifier)
                .alpha(if (shown) 1f else 0f)
                .onSizeChanged { sheetHeight = it.height }
                .consumeWindowInsets(sheetWindowInsets),
            state = sheetState,
            onDismissRequest = { dismissing = true },
            maxWidth = sheetMaxWidth,
            gesturesEnabled = sheetGesturesEnabled,
            // Back is handled by OverlayLayer so stacked modals pop top-down.
            backHandlerEnabled = false,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}
