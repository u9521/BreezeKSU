package me.weishu.kernelsu.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import me.weishu.kernelsu.ui.theme.LocalEnableBlur

private const val BarMaxTintAlpha = 0.7f
private val DefaultBarBlurRadius = 15.dp

/** Applies [style] blur to a floating bar when [LocalEnableBlur] is enabled. */
@Composable
fun Modifier.defaultHazeEffect(
    hazeState: HazeState,
    style: HazeBlurStyle,
): Modifier = if (LocalEnableBlur.current) {
    this.hazeBlur(input = HazeInput.Sources(hazeState), style = style)
} else {
    this
}

@Composable
fun Modifier.topBarHazeEffect(
    hazeState: HazeState,
    scrollBehavior: TopAppBarScrollBehavior?,
): Modifier {
    val collapseFraction = (scrollBehavior?.state?.collapsedFraction ?: 0f).coerceIn(0f, 1f)
    return if (collapseFraction > 0f) {
        defaultHazeEffect(hazeState, topBarHazeStyle(collapseFraction))
    } else {
        this
    }
}

@Composable
fun topBarHazeStyle(collapseFraction: Float): HazeBlurStyle {
    val background = MaterialTheme.colorScheme.surfaceContainer
    val tint = MaterialTheme.colorScheme.surfaceBright.copy(
        alpha = collapseFraction.coerceIn(0f, 1f) * BarMaxTintAlpha,
    )
    return HazeBlurStyle {
        backgroundColor(background)
        blurRadius(DefaultBarBlurRadius)
        noiseFactor(0f)
        colorEffects(listOf(HazeColorEffect.tint(tint)))
    }
}

@Composable
fun bottomBarHazeStyle(): HazeBlurStyle {
    val background = MaterialTheme.colorScheme.surfaceContainer
    val tint = MaterialTheme.colorScheme.surfaceBright.copy(alpha = BarMaxTintAlpha)
    return HazeBlurStyle {
        backgroundColor(background)
        blurRadius(DefaultBarBlurRadius)
        noiseFactor(0f)
        colorEffects(listOf(HazeColorEffect.tint(tint)))
    }
}
