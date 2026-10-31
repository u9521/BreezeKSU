package me.weishu.kernelsu.ui.webui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.BreezeShell
import me.weishu.kernelsu.ui.LocalSettingsRepository

/**
 * Breeze's WebUI activity content.
 *
 * The module WebUI is a second, independent window, so it is a top-level sibling host: the same
 * [BreezeShell] the manager window uses, the same settings flow, and the same shared overlay
 * window for its dialogs. Everything below the shell - the WebView, its insets bridge and the JS
 * event dialogs - is [WebUIScreen], which the manager window would render identically.
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
            BreezeWebUIContent(activity = activity, onFinish = onFinish)
        }
    }
}

@Composable
private fun BreezeWebUIContent(activity: ComponentActivity, onFinish: () -> Unit) {
    val moduleId = remember { activity.intent.data?.getQueryParameter("id") }
    val webUIState = remember { WebUIState() }

    LaunchedEffect(moduleId) {
        if (moduleId == null) {
            onFinish()
            return@LaunchedEffect
        }
        prepareWebView(activity, moduleId, webUIState)
    }

    DisposableEffect(Unit) {
        onDispose { webUIState.dispose(activity) }
    }

    when (val event = webUIState.uiEvent) {
        is WebUIEvent.Error -> {
            LaunchedEffect(event) {
                Toast.makeText(activity, event.message, Toast.LENGTH_SHORT).show()
                onFinish()
            }
        }

        is WebUIEvent.Close -> {
            LaunchedEffect(event) { onFinish() }
        }

        else -> {}
    }
    val isLoading = webUIState.uiEvent is WebUIEvent.Loading

    Crossfade(targetState = isLoading, animationSpec = tween(300)) { loading ->
        if (loading) {
            BreezeWebUILoading()
        } else {
            WebUIScreen(webUIState = webUIState)
        }
    }
}

@Composable
private fun BreezeWebUILoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}
