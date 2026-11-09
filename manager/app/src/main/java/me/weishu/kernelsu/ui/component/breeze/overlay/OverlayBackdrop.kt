package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scrim
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState

/** Scrim opacity matching `Theme.Material.Dialog.Alert` (`backgroundDimAmount = 0.6`). */
internal const val DialogScrimAlpha = 0.6f

/** Scrim opacity for a modal bottom sheet (`ScrimTokens.ContainerOpacity`). */
internal const val SheetScrimAlpha = 0.32f

/**
 * What a layer paints behind its card.
 *
 * One implementation slot replaces a pile of appearance flags: a component that needs a different
 * backdrop supplies its own instead of asking for another parameter. The overlay keeps owning what
 * must not vary — back press, outside taps, keeping the scrim composed through enter and exit, and
 * the stacked dim — and hands the results to [Backdrop] through [OverlayBackdropScope].
 *
 * Implementations are recomposed whenever the layer state changes, so keep them cheap and free of
 * their own animation state; hold that in the caller.
 */
@Stable
interface OverlayBackdrop {
    /**
     * True when this backdrop samples the host's recorded content.
     *
     * Recording is what makes the page and the layers below blurrable. A backdrop that needs them
     * declares `true` and gets them for free: the host registers the page, full-screen layers, and
     * the layers below this one as sources. A backdrop that draws its own effect and never samples
     * the host declares `false`, and nothing is recorded.
     */
    val samplesHost: Boolean

    /** Scrim opacity while this backdrop paints no host blur. */
    val scrimAlpha: Float get() = DialogScrimAlpha

    /** Scrim opacity while host blur is painting; `0f` so the blur alone separates the card. */
    val scrimAlphaBlurred: Float get() = 0f

    /**
     * Extra scrim applied to each layer above the bottom one while blur is painting.
     *
     * Blur alone makes a stack read brighter rather than deeper. The overlay multiplies this in, so
     * a custom backdrop inherits the behaviour; honour [OverlayBackdropScope.scrimAlpha] to keep it.
     */
    val scrimAlphaStacked: Float get() = 0.12f

    /** Paints the backdrop and hosts the card. [content] is the card slot. */
    @Composable
    fun Backdrop(scope: OverlayBackdropScope, content: @Composable BoxScope.() -> Unit)

    companion object {
        /** Host blur with a dialog's scrim strengths, fading on the layer's progress. */
        val Default: OverlayBackdrop = blur()

        /** Opaque scrim, no blur, nothing recorded. What a layer gets when blur is turned off. */
        val Scrim: OverlayBackdrop = ScrimOnly

        /**
         * Host blur, fading with [OverlayBackdropScope.progress].
         *
         * @param blur the effect to paint; the stock page blur by default.
         * @param radius blur radius at full progress.
         */
        fun blur(
            blur: OverlayBlur = OverlayBlur.Default,
            radius: Dp = OverlayBlurRadius,
            scrimAlpha: Float = DialogScrimAlpha,
            scrimAlphaBlurred: Float = 0f,
            stackedAlpha: Float = 0.12f,
        ): OverlayBackdrop = BlurBackdrop(blur, radius, scrimAlpha, scrimAlphaBlurred, stackedAlpha)

        /** Host blur with a sheet's scrim strengths. */
        fun sheetBlur(
            blur: OverlayBlur = OverlayBlur.Default,
            radius: Dp = OverlayBlurRadius,
        ): OverlayBackdrop = blur(
            blur = blur,
            radius = radius,
            scrimAlpha = SheetScrimAlpha,
            scrimAlphaBlurred = 0f,
        )
    }
}

/** Everything a backdrop may read while painting. */
@Stable
interface OverlayBackdropScope {
    /** 0 gone, 1 fully shown, already clamped. */
    val progress: Float

    /** The host's recorded content; `null` when nothing is sampled this frame. */
    val hazeState: HazeState?

    /**
     * Opacity of this layer's scrim, resolved for its position in the stack.
     *
     * Falls back to [OverlayBackdrop.scrimAlpha] when no blur paints, and adds
     * [OverlayBackdrop.scrimAlphaStacked] for every layer above the bottom one.
     */
    val scrimAlpha: Float

    /** The layer the stack opened with; never carries outside-tap semantics of its own. */
    val isTopmost: Boolean

    /** Outside-tap callback, or `null` when this layer does not dismiss on outside taps. */
    val onClickOutside: (() -> Unit)?
}

@Immutable
private class BlurBackdrop(
    private val blur: OverlayBlur,
    private val radius: Dp,
    override val scrimAlpha: Float,
    override val scrimAlphaBlurred: Float,
    override val scrimAlphaStacked: Float,
) : OverlayBackdrop {
    override val samplesHost: Boolean = true

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is BlurBackdrop && other.blur === blur && other.radius == radius &&
                    other.scrimAlpha == scrimAlpha &&
                    other.scrimAlphaBlurred == scrimAlphaBlurred &&
                    other.scrimAlphaStacked == scrimAlphaStacked
                )

    override fun hashCode(): Int {
        var result = blur.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + scrimAlpha.hashCode()
        result = 31 * result + scrimAlphaBlurred.hashCode()
        result = 31 * result + scrimAlphaStacked.hashCode()
        return result
    }

    @Composable
    override fun Backdrop(scope: OverlayBackdropScope, content: @Composable BoxScope.() -> Unit) {
        val hazeState = scope.hazeState
        val painted = if (hazeState != null) {
            blur.apply(Modifier, hazeState, radius * scope.progress)
        } else {
            Modifier
        }
        Box(modifier = painted.fillMaxSize()) {
            OverlayScrim(scope, content)
        }
    }
}

@Immutable
private object ScrimOnly : OverlayBackdrop {
    override val samplesHost: Boolean = false

    @Composable
    override fun Backdrop(scope: OverlayBackdropScope, content: @Composable BoxScope.() -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayScrim(scope, content)
        }
    }
}

/**
 * Scrim plus the card slot, shared by the built-in backdrops.
 *
 * The scrim stays composed at alpha 0 so taps cannot fall through during enter and exit.
 */
@Composable
private fun OverlayScrim(
    scope: OverlayBackdropScope,
    content: @Composable BoxScope.() -> Unit,
) {
    Scrim(
        contentDescription = "Close",
        onClick = scope.onClickOutside,
        alpha = { scope.progress * scope.scrimAlpha },
        color = MaterialTheme.colorScheme.scrim,
        modifier = if (scope.isTopmost) Modifier else Modifier.clearAndSetSemantics {},
    )
    Box(modifier = Modifier.fillMaxSize()) { content() }
}
