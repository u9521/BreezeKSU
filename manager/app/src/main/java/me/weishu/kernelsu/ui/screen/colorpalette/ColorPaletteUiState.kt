package me.weishu.kernelsu.ui.screen.colorpalette

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.data.ColorMode
import me.weishu.kernelsu.data.UiSettings

/** What the palette page renders: the settings snapshot plus the values it derives from it. */
@Immutable
data class ColorPaletteUiState(
    val preferences: UiSettings,
) {
    val currentColorMode: ColorMode get() = preferences.colorMode
    val currentPaletteStyle: PaletteStyle get() = preferences.paletteStyle
    val currentColorSpec: ColorSpec.SpecVersion get() = preferences.colorSpec
}

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (PaletteStyle) -> Unit,
    val onSetColorSpec: (ColorSpec.SpecVersion) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableNavigationBadge: (Boolean) -> Unit,
    val onSetEnablePredictiveBack: (Boolean) -> Unit,
    val onSetPageScale: (Float) -> Unit,
    val onSetModuleDescriptionMaxLines: (Int) -> Unit,
)
