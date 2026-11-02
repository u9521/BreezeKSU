package me.weishu.kernelsu.ui.screen.colorpalette

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.ui.LocalSettingsRepository
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator

/**
 * Breeze's color palette page: binds the live settings repository to the palette editor.
 *
 * Every write goes straight through [me.weishu.kernelsu.data.repository.SettingsRepository.update],
 * so the whole app re-themes on the next frame; no view model sits between the page and the
 * preference it edits.
 */
@Composable
fun ColorPaletteEntry() {
    val navigator = LocalBreezeNavigator.current
    val activity = LocalActivity.current
    val repository = LocalSettingsRepository.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    val state = ColorPaletteUiState(preferences = settings)
    val actions = ColorPaletteScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSetKeyColor = { color -> repository.update { it.copy(keyColor = color) } },
        onSetColorMode = { mode -> repository.update { it.copy(colorMode = mode) } },
        onSetColorStyle = { style -> repository.update { it.copy(paletteStyle = style) } },
        onSetColorSpec = { spec -> repository.update { it.copy(colorSpec = spec) } },
        onSetEnableBlur = { enabled -> repository.update { it.copy(enableBlur = enabled) } },
        onSetEnableNavigationBadge = { enabled ->
            repository.update { it.copy(enableNavigationBadge = enabled) }
        },
        // The repository flips ApplicationInfo for the next activity start; this window
        // needs a recreate of its own to pick up the new back gesture.
        onSetEnablePredictiveBack = { enabled ->
            repository.update { it.copy(enablePredictiveBack = enabled) }
            activity?.recreate()
        },
        onSetPageScale = { scale -> repository.update { it.copy(pageScale = scale) } },
        onSetModuleDescriptionMaxLines = { lines ->
            repository.update { it.copy(moduleDescriptionMaxLines = lines) }
        },
    )

    ColorPaletteScreenBreeze(state, actions)
}
