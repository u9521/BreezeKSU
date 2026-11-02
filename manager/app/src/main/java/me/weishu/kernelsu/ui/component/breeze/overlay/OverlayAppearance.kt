package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Backdrop configuration for a modal overlay: blur effect, scrim opacity, and shared fade timings.
 */
@Immutable
data class OverlayAppearance(
    /** Blur this layer paints; when null the host's backdropBlur applies. */
    val blur: OverlayBlur? = null,
    val blurRadius: Dp = OverlayBlurRadius,
    /** Scrim opacity when backdrop blur is inactive. */
    val scrimAlpha: Float = DialogScrimAlpha,
    /** Scrim opacity while backdrop blur is active (`0f` by default so blur alone separates the card). */
    val scrimAlphaBlurred: Float = 0f,
    /**
     * Extra scrim opacity for each layer above the bottom one while blur is active.
     *
     * Blur alone makes a stack read brighter rather than deeper. Kept low because it multiplies
     * with the layer count: three layers must not end up darker than one unblurred dialog.
     */
    val scrimAlphaStacked: Float = 0.12f,
    val enterMillis: Int = OverlayEnterMillis,
    val exitMillis: Int = OverlayExitMillis,
) {
    companion object {
        /** Dialog default (`0.6` unblurred scrim, matching `Theme.Material.Dialog.Alert`). */
        val Default: OverlayAppearance = OverlayAppearance()

        /** Sheet default (`0.32` unblurred scrim, matching `ScrimTokens.ContainerOpacity`). */
        val Sheet: OverlayAppearance = OverlayAppearance(
            scrimAlpha = SheetScrimAlpha,
            scrimAlphaBlurred = 0f,
        )
    }
}
