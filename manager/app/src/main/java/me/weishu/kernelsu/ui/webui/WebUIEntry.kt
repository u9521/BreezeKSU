package me.weishu.kernelsu.ui.webui

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.ObserveAsEvents
import me.weishu.kernelsu.ui.webui.model.WebUIEffect
import me.weishu.kernelsu.ui.webui.ui.WebUIOverlayHost
import me.weishu.kernelsu.ui.webui.ui.WebUIScreen
import me.weishu.kernelsu.ui.webui.ui.rememberFileLauncher
import me.weishu.kernelsu.ui.webui.viewmodel.WebUIViewModel
import me.weishu.kernelsu.ui.webui.webview.prepareWebView

/**
 * Binds the view model to the window: the one-shot setup, the effect collector and the
 * activity-scoped [WebUIRuntime]. Platform capabilities stop here, which is what keeps
 * [WebUIScreen] a function of state and nothing else.
 */
@Composable
fun WebUIEntry(activity: ComponentActivity, onFinish: () -> Unit) {
    val viewModel = viewModel<WebUIViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val runtime = remember { WebUIRuntime() }

    DisposableEffect(runtime) {
        onDispose { runtime.dispose() }
    }

    val launchFile = rememberFileLauncher(runtime::consumePendingFileCallback)

    LaunchedEffect(Unit) {
        prepareWebView(
            activity = activity,
            runtime = runtime,
            viewModel = viewModel,
        )
    }

    // Lifecycle-aware, like every other one-shot channel in the manager: Finish and
    // OpenExternalLink must not run while the window is stopped, and a file chooser must not be
    // launched from a non-resumed activity.
    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is WebUIEffect.ShowToast ->
                Toast.makeText(activity, effect.message, Toast.LENGTH_SHORT).show()

            WebUIEffect.Finish -> onFinish()

            is WebUIEffect.RequestFileChooser -> {
                val intent = runtime.takeFileChooserIntent(effect.requestId)
                if (intent != null) {
                    launchFile(effect.requestId, intent)
                } else {
                    // The request was superseded or the runtime was replaced. Answering the page
                    // with null is the only way to unblock it; otherwise its picker never returns.
                    runtime.consumePendingFileCallback(effect.requestId, null)
                }
            }

            is WebUIEffect.OpenExternalLink -> {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                } catch (_: Exception) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.webui_failed_to_open_link, effect.url),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WebUIScreen(state = state, runtime = runtime)
        WebUIOverlayHost(state = state, viewModel = viewModel)
    }
}
