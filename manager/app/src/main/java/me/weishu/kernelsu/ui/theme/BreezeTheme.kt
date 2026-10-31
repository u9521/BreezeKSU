package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.data.UiSettings

/** Material 2025 specs only cover these palettes; the rest fall back to 2021. */
val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
            this == PaletteStyle.Neutral ||
            this == PaletteStyle.Vibrant ||
            this == PaletteStyle.Expressive

/** Downgrades [ColorSpec.SpecVersion.SPEC_2025] for palettes it does not cover. */
fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

/** Gates every backdrop blur in the app, so turning it off also skips offscreen capture. */
val LocalEnableBlur = staticCompositionLocalOf { false }

/** Material Expressive theme that animates color scheme transitions and syncs system bar icon contrast. */
@Composable
fun BreezeTheme(
    settings: UiSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = settings.colorMode.isDark || (settings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = settings.colorMode.isAmoled
    val dynamicColor = settings.keyColor == 0

    val colorScheme = rememberBreezeColorScheme(
        seedColor = if (dynamicColor) Color.Unspecified else Color(settings.keyColor),
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = settings.paletteStyle,
        colorSpec = settings.colorSpec,
    )

    // The main window is the only place that sets bar icon contrast; overlays handle their own.
    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val animatedColorScheme = colorScheme.animateAsState()
    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}
