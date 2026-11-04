package me.weishu.kernelsu.ui.webui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.BreezeShell
import me.weishu.kernelsu.ui.LocalSettingsRepository

/**
 * Breeze's WebUI activity content.
 *
 * The module WebUI is a second, independent window: the same [BreezeShell], settings flow and
 * shared overlay as the manager window. Everything below the shell is [WebUIEntry].
 */
@Composable
fun BreezeWebUIRoot(
    activity: ComponentActivity,
    onFinish: () -> Unit,
) {
    // The WebUI window is a second composition, so it subscribes to the same flow the manager
    // window does instead of reading a preference once at entry.
    val settingsRepository = ksuApp.settingsRepository
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalSettingsRepository provides settingsRepository) {
        BreezeShell(settings = settings) {
            // Must sit inside the shell: it reads MaterialTheme.colorScheme, which the shell
            // provides, and that is what makes the served internal/colors.css non-empty.
            MonetColorsProvider.UpdateCss()
            WebUIEntry(activity = activity, onFinish = onFinish)
        }
    }
}
