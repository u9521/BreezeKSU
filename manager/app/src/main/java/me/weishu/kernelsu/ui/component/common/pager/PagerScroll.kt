package me.weishu.kernelsu.ui.component.common.pager

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.pager.PagerState

/** Page-switch spring: `dampingRatio = 0.88f` (~0.30% overshoot per hop), `stiffness = 450f` (~414ms settle). */
private val PageSpringSpec = spring<Float>(dampingRatio = 0.88f, stiffness = 450f)

/**
 * Scrolls smoothly to [target] via [MutatePriority.UserInput], bypassing foundation
 * `animateScrollToPage`'s `snapToItem` jump across >=3 pages and forwarding unconsumed boundary
 * overshoot to [overscroll].
 */
suspend fun PagerState.springAnimateToPage(
    target: Int,
    overscroll: BreezePagerOverscroll? = null,
) {
    if (target !in 0 until pageCount) return
    val destination = target.coerceIn(0, pageCount - 1)
    scroll(MutatePriority.UserInput) {
        animateToPage(this, destination, overscroll)
    }
}

/** Runs the spring scroll to [target] inside an existing [scrollScope]. */
suspend fun PagerState.animateToPage(
    scrollScope: ScrollScope,
    target: Int,
    overscroll: BreezePagerOverscroll? = null,
) {
    with(scrollScope) {
        if (pageCount == 0) return@with
        val destination = target.coerceIn(0, pageCount - 1)
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        updateTargetPage(destination)
        if (pageSize > 0) {
            val distance = (destination - currentPage - currentPageOffsetFraction) * pageSize
            var previous = 0f
            animate(
                initialValue = 0f,
                targetValue = distance,
                animationSpec = PageSpringSpec,
            ) { value, _ ->
                previous += scrollBy(value - previous)
                // Publish unconsumed boundary delta so BreezePagerOverscroll translates the remainder.
                overscroll?.setPageOvershoot(previous - value)
            }
            // Clear sub-pixel residual so overscroll returns to idle.
            overscroll?.setPageOvershoot(0f)
        }
        // Commit currentPage inside the scroll mutation so cancellation leaves state consistent.
        if (pageCount > 0) updateCurrentPage(destination.coerceAtMost(pageCount - 1))
    }
}
