package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import me.weishu.kernelsu.ui.theme.LocalEnableBlur

/** Blur radius of the page behind the overlays at full strength, in dp. */
internal val OverlayBlurRadius = 20.dp

/** Backdrop blur strategy for an overlay layer, driven by the per-frame animated `radius`. */
@Stable
class OverlayBlur(
    private val effect: @Composable (Modifier, HazeState, Dp) -> Modifier,
) {
    @Suppress("ModifierFactoryExtensionFunction")
    @Composable
    fun apply(modifier: Modifier, state: HazeState, radius: Dp): Modifier =
        if (radius <= 0.dp) modifier else effect(modifier, state, radius)

    companion object {
        /** Page blur, gated on the user's blur setting. */
        val Default: OverlayBlur = OverlayBlur { modifier, state, radius ->
            if (LocalEnableBlur.current) modifier.overlayHaze(state, radius) else modifier
        }
    }
}

/** Haze backdrop effect; relies on [BreezeOverlayHost] painting the opaque source background. */
private fun Modifier.overlayHaze(state: HazeState, radius: Dp): Modifier = this.hazeBlur(
    input = HazeInput.Sources(state),
    style = HazeBlurStyle {
        blurRadius(radius)
        noiseFactor(0.0065f)
    },
)
