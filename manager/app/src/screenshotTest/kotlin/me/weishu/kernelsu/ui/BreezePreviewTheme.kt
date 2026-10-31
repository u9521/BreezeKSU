package me.weishu.kernelsu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.data.ColorMode
import me.weishu.kernelsu.data.UiSettings
import me.weishu.kernelsu.ui.theme.BreezeTheme

/**
 * Theme wrapper for Breeze `@Preview`s.
 *
 * The settings snapshot is a constant rather than a read of `ksuApp.settingsRepository`, which
 * would throw under layoutlib where no `Application` exists.
 */
@Composable
fun BreezePreviewTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val settings = if (dark) PREVIEW_SETTINGS.copy(colorMode = ColorMode.DARK) else PREVIEW_SETTINGS
    BreezeTheme(settings = settings, content = content)
}

/** Light, Monet off, TonalSpot: what a fresh install looks like, with no I/O. */
private val PREVIEW_SETTINGS = UiSettings(
    colorMode = ColorMode.LIGHT,
    keyColor = Color(0xFFC6B091).toArgb(),
    paletteStyle = PaletteStyle.TonalSpot,
    colorSpec = ColorSpec.SpecVersion.SPEC_2025,
)
