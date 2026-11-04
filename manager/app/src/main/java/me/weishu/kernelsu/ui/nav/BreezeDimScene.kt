package me.weishu.kernelsu.ui.nav

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import me.weishu.kernelsu.ui.animation.IosCoveredPageEasing
import me.weishu.kernelsu.ui.animation.IosScrimEasing
import me.weishu.kernelsu.ui.animation.NAV_TRANSITION_DURATION

// Calibrated against Flutter's Cupertino page transition. Its own strengths read as barely
// visible on a device and its authors mark them "eyeballed", so these are nudged up; geometry,
// curves and proportions are kept.

/** How far the page underneath the top one is dimmed at full travel. Flutter: 0.094. */
private const val DIM_ALPHA = 0.12f

/** Peak alpha of the top page's edge shadow. Flutter: 0.016. */
private const val SHADOW_ALPHA = 0.05f

/** How far the top page's edge shadow bleeds onto the page underneath, as a fraction of the width. */
private const val SHADOW_WIDTH_FRACTION = 0.05f

/**
 * Wraps a [SceneStrategy] so the page underneath the top one is dimmed, mimicking iOS: pushing
 * covers a dimmed page, popping reveals one that brightens as the top page leaves.
 *
 * Drawn here rather than as `fadeOut`/`fadeIn` transition specs, which only read as dimming
 * because something dark happens to be painted behind the navigation host. The scrim and the
 * shadow sit in the top page's own coordinates, so they follow it through the slide and through
 * a predictive back gesture without an animation of their own.
 *
 * That the top page can paint the page underneath at all is down to `AnimatedContent` never
 * clipping its children, `navDisplay` passing a null `sizeTransform` included.
 *
 * Assumes Breeze's left-to-right slide specs: content moves horizontally only, and the page
 * underneath is always at a negative x offset.
 */
internal fun <T : Any> SceneStrategy<T>.withDimmedUnderneath(): SceneStrategy<T> =
    DimSceneStrategy(this)

private class DimSceneStrategy<T : Any>(
    private val delegate: SceneStrategy<T>
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? =
        with(delegate) { calculateScene(entries) }?.let { DimScene(it) }
}

private class DimScene<T : Any>(
    private val delegate: Scene<T>
) : Scene<T> by delegate {

    override val content: @Composable () -> Unit = {
        val transition = LocalNavAnimatedContentScope.current.transition

        // Painted to the left of this page's own origin, and navigation3 only ever puts the
        // page underneath at a negative offset, so nothing painted from a page below lands in the
        // window. `PreEnter` is the one state meaning "not on screen yet".
        val paints = transition.targetState != EnterExitState.PreEnter

        // How far the top page has travelled: 0f off screen, 1f settled. The scrim and the shadow
        // read the same state but carry their own curve, the way Flutter drives them.
        //
        // Frozen at the boundaries during a predictive back drag, where navigation3 moves the
        // content with a mutable transform instead: geometry follows the finger, strength waits
        // for release.
        val dimFraction by transition.animateFloat(
            transitionSpec = { tween(NAV_TRANSITION_DURATION, easing = IosScrimEasing) },
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        val shadowFraction by transition.animateFloat(
            transitionSpec = { tween(NAV_TRANSITION_DURATION, easing = IosCoveredPageEasing) },
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

        val scrimColor = MaterialTheme.colorScheme.scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (paints) {
                        // Everything left of the top page is the page underneath: dim it, then let
                        // it brighten as the top page leaves.
                        val dim = dimFraction * DIM_ALPHA
                        if (dim > 0f) {
                            drawRect(
                                color = scrimColor,
                                alpha = dim,
                                topLeft = Offset(-size.width, 0f),
                                size = Size(size.width, size.height),
                            )
                        }
                        // Depth cue: a shadow on the page underneath, positioned from this page's
                        // leading edge so it stays correct at any offset, mid gesture included.
                        val shadow = size.width * SHADOW_WIDTH_FRACTION
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimColor.copy(alpha = shadowFraction * SHADOW_ALPHA),
                                ),
                                startX = -shadow,
                                endX = 0f,
                            ),
                            topLeft = Offset(-shadow, 0f),
                            size = Size(shadow, size.height),
                        )
                    }
                    drawContent()
                }
        ) {
            delegate.content()
        }
    }

    // Scenes are looked up by equality while a transition is in flight; creating a new wrapper
    // every recomposition must not read as a different scene.
    override fun equals(other: Any?): Boolean =
        other is DimScene<*> && delegate == other.delegate

    override fun hashCode(): Int = delegate.hashCode()
}
