package me.weishu.kernelsu.ui.webui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.weishu.kernelsu.ui.webui.WebUIRuntime
import me.weishu.kernelsu.ui.webui.model.WebUIState
import me.weishu.kernelsu.ui.webui.ui.component.WebViewContainer

/**
 * The WebView and the masks over it.
 *
 * Insets are the whole story here. A page that asked for edge-to-edge gets an unpadded view and
 * pads itself from the variables `internal/insets.css` serves; when it cannot, the host pads.
 * The IME is never the host's business either way - Chromium shrinks the visual viewport by
 * itself, so padding for it would shorten the page twice.
 */
@Composable
fun WebUIScreen(
    state: WebUIState,
    runtime: WebUIRuntime,
) {
    BackHandler(enabled = state.webCanGoBack) {
        runtime.webView?.goBack()
    }

    // The host pads unless the page already did. `safeDrawing` is already
    // `systemBars` + `ime` + `displayCutout`, so unioning the IME in again is a no-op that reads as
    // though it were missing - two earlier wrong conclusions came of that misreading.
    //
    // "Can" is the whole point: the page pads from env(safe-area-inset-*), which does not
    // resolve on an older WebView, so dropping the host's padding there would leave the content
    // under the status bar and the cutout. One of the two always pads; never both.
    val pagePadsItself = state.isEdgeToEdge && state.isInsetsSupported
    val insetsPadding = if (pagePadsItself) {
        PaddingValues(0.dp)
    } else {
        WindowInsets.safeDrawing.asPaddingValues()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetsPadding)
        ) {
            runtime.webView?.let { webView ->
                WebViewContainer(webView = webView)
            }
        }

        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            WebUILoadingMask()
        }
    }

    HandleWebViewLifecycle(runtime)
    HandleConfigurationChanges(runtime)
}

@Composable
private fun HandleWebViewLifecycle(runtime: WebUIRuntime) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, runtime) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runtime.webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> runtime.webView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun HandleConfigurationChanges(runtime: WebUIRuntime) {
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.fontScale, runtime.webView) {
        runtime.webView?.settings?.textZoom = (configuration.fontScale * 100).toInt()
    }
}
