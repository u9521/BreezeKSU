package me.weishu.kernelsu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import me.weishu.kernelsu.data.UiSettings
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlayHost
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlayHostState
import me.weishu.kernelsu.ui.component.breeze.overlay.rememberBreezeOverlayHostState
import me.weishu.kernelsu.ui.theme.BreezeTheme
import me.weishu.kernelsu.ui.theme.LocalEnableBlur

/** Settings repository of the running app; provided once by [BreezeApp] and by the WebUI host. */
val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository> {
    error("LocalSettingsRepository not provided")
}

/**
 * Skin runtime shell: theme, blur flag and the shared overlay window around [content].
 *
 * Swapping skins means replacing this composable only; the navigator, the pager state and every
 * `NavEntry` view model store live above it and therefore survive the swap.
 */
@Composable
fun BreezeShell(
    settings: UiSettings,
    overlayHost: BreezeOverlayHostState = rememberBreezeOverlayHostState(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalEnableBlur provides settings.enableBlur) {
        BreezeTheme(settings = settings) {
            BreezeOverlayHost(state = overlayHost) {
                content()
            }
        }
    }
}
