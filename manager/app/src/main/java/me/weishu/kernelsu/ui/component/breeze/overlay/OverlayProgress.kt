package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Fade-in and fade-out timings of the default backdrop; card motion belongs to each component.
 *
 * Both use Material's emphasized curves, the same pair the dialog card animates on, so the
 * backdrop and the card it sits behind never drift apart.
 */
internal const val OverlayEnterMillis = 220
internal const val OverlayExitMillis = 150
internal val OverlayEnterEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
internal val OverlayExitEasing: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/**
 * Where a layer's backdrop progress comes from: 0 is gone, 1 is fully shown.
 *
 * Every visual decision behind a card (blur radius, scrim opacity, stacked dim) reads this single
 * value, so a component that animates its own content can drive all of them from one timeline.
 */
@Stable
sealed interface OverlayProgress {
    /**
     * Fade from 0 to 1 on [visible], over the host's timings.
     *
     * The default: a component that only knows "open" or "closed" gets a backdrop that fades in and
     * out with it, without owning any animation state.
     */
    @Immutable
    data class Fade(
        val visible: Boolean,
        val enterMillis: Int = OverlayEnterMillis,
        val exitMillis: Int = OverlayExitMillis,
        val enterEasing: Easing = OverlayEnterEasing,
        val exitEasing: Easing = OverlayExitEasing,
    ) : OverlayProgress

    /**
     * Follow a value the caller supplies.
     *
     * The caller owns the whole timeline: the overlay neither animates nor interpolates, it only
     * reads [value] once per frame. Use this to tie the backdrop to a gesture or to a
     * component-owned animation, so the two can never drift apart.
     *
     * [value] must be safe to call before the source has a position (first frame, after the content
     * was released) and must stay inside `0f..1f`; the overlay clamps but cannot recover from a
     * source that throws.
     */
    fun interface External : OverlayProgress {
        fun value(): Float
    }
}
