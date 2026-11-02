package me.weishu.kernelsu.ui.component.common.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.IntSize

private const val MaxOffsetFraction = 1f / 6f

/**
 * Holds unconsumed programmatic scroll past the first or last page (published per frame by
 * [springAnimateToPage]) and translates pager content horizontally so boundary tabs
 * overshoot identically to middle tabs. Does not intercept gesture overscroll.
 */
@Stable
class BreezePagerOverscroll {

    internal var pageOvershoot by mutableFloatStateOf(0f)
        private set

    internal var containerSize: IntSize = IntSize.Zero

    val isInProgress: Boolean get() = pageOvershoot != 0f

    internal val maxOffset: Float
        get() = containerSize.width * MaxOffsetFraction

    internal fun updateSize(size: IntSize) {
        containerSize = size
    }

    internal fun setPageOvershoot(pixels: Float) {
        pageOvershoot = pixels.coerceIn(-maxOffset, maxOffset)
    }
}

/** Translates pager content horizontally by [BreezePagerOverscroll.pageOvershoot] without scaling or rounding. */
private class PageOvershootNode(
    private val effect: BreezePagerOverscroll,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        effect.updateSize(IntSize(size.width.toInt(), size.height.toInt()))
        val displacement = effect.pageOvershoot
        if (displacement == 0f || size.width <= 0f) {
            drawContent()
            return
        }
        withTransform({ translate(left = displacement, top = 0f) }) {
            this@draw.drawContent()
        }
    }
}

private data class PageOvershootElement(
    val effect: BreezePagerOverscroll,
) : ModifierNodeElement<PageOvershootNode>() {

    override fun create(): PageOvershootNode = PageOvershootNode(effect)

    override fun update(node: PageOvershootNode) = Unit
}

internal fun Modifier.pageOvershoot(effect: BreezePagerOverscroll): Modifier =
    this then PageOvershootElement(effect)

@Composable
fun rememberBreezePagerOverscroll(): BreezePagerOverscroll = remember { BreezePagerOverscroll() }
