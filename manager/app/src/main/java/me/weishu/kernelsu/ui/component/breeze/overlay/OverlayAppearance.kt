package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Backdrop configuration for a modal overlay: blur effect, scrim opacity, and shared fade timings.
 */
@Immutable
data class OverlayAppearance(
    val blur: OverlayBlur = OverlayBlur.Default,
    val blurRadius: Dp = OverlayBlurRadius,
    /** Scrim opacity when backdrop blur is inactive. */
    val scrimAlpha: Float = DialogScrimAlpha,
    /** Scrim opacity while backdrop blur is active (`0f` by default so blur alone separates the card). */
    val scrimAlphaBlurred: Float = 0f,
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
