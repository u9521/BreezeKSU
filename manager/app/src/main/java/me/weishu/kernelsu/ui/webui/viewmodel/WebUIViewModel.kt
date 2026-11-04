package me.weishu.kernelsu.ui.webui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import me.weishu.kernelsu.ui.webui.model.ModuleInfo
import me.weishu.kernelsu.ui.webui.model.WebUIBanner
import me.weishu.kernelsu.ui.webui.model.WebUIDialog
import me.weishu.kernelsu.ui.webui.model.WebUIEffect
import me.weishu.kernelsu.ui.webui.model.WebUILoadState
import me.weishu.kernelsu.ui.webui.model.WebUIState
import me.weishu.kernelsu.ui.webui.util.WebViewVersionUtil

/**
 * The WebUI host state machine. No Context, no View, no Compose: everything here is pure state
 * transition, which is why it can be tested and why the Activity cannot leak through it.
 */
class WebUIViewModel : ViewModel() {
    private val _state = MutableStateFlow(WebUIState())
    val state: StateFlow<WebUIState> = _state.asStateFlow()

    private val _effect = Channel<WebUIEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onWebViewFeatureResolved(version: Int, packageName: String, insetsSupported: Boolean) {
        _state.update {
            it.copy(
                webViewVersion = version,
                webViewPackageName = packageName,
                isInsetsSupported = insetsSupported,
            )
        }
    }

    fun onModuleValidated(moduleInfo: ModuleInfo) {
        _state.update { it.copy(moduleInfo = moduleInfo) }
    }

    fun setLoadState(loadState: WebUILoadState) {
        _state.update { it.copy(loadState = loadState) }
    }

    fun showDialog(dialog: WebUIDialog) {
        _state.update { it.copy(activeDialog = dialog) }
    }

    fun clearDialog() {
        _state.update { it.copy(activeDialog = null) }
    }

    fun dismissBanner(id: String) {
        _state.update { it.copy(banners = it.banners.filterNot { banner -> banner.id == id }) }
    }

    fun updateWebView() {
        val pkg =
            _state.value.webViewPackageName.ifBlank { WebViewVersionUtil.DEFAULT_WEBVIEW_PACKAGE }
        emitEffect(WebUIEffect.OpenExternalLink("market://details?id=$pkg"))
    }

    fun onHomePageLoaded() {
        if (_state.value.loadState != WebUILoadState.Ready) {
            _state.update { it.copy(loadState = WebUILoadState.Ready) }
        }
    }

    fun onHistoryChanged(canGoBack: Boolean) {
        _state.update { it.copy(webCanGoBack = canGoBack) }
    }

    fun onEdgeToEdgeChanged(enable: Boolean) {
        val bannerId = WebUIBanner.OutdatedWebView.ID
        _state.update { state ->
            // Announced at most once per session: the id is recorded when the banner is queued, so
            // neither a reload nor a second request can bring it back or duplicate it.
            val shouldWarn = enable &&
                    !state.isInsetsSupported &&
                    bannerId !in state.shownBannerIds

            if (!shouldWarn) {
                state.copy(isEdgeToEdge = enable)
            } else {
                state.copy(
                    isEdgeToEdge = true,
                    banners = state.banners + WebUIBanner.OutdatedWebView(
                        currentVersion = state.webViewVersion,
                        requiredVersion = WebViewVersionUtil.MIN_INSETS_VERSION,
                    ),
                    shownBannerIds = state.shownBannerIds + bannerId,
                )
            }
        }
    }

    fun notifyError(message: String) {
        emitEffects(WebUIEffect.ShowToast(message), WebUIEffect.Finish)
    }

    fun toast(message: String) {
        emitEffect(WebUIEffect.ShowToast(message))
    }

    fun exit() {
        emitEffect(WebUIEffect.Finish)
    }

    fun requestFileChooser(requestId: Long) {
        emitEffect(WebUIEffect.RequestFileChooser(requestId))
    }

    // trySend, not launch { send() }: the channel is BUFFERED, so this never suspends and two
    // calls keep their order. With launch a toast could overtake the Finish right behind it.
    private fun emitEffect(effect: WebUIEffect) {
        _effect.trySend(effect)
    }

    private fun emitEffects(vararg effects: WebUIEffect) {
        for (effect in effects) {
            _effect.trySend(effect)
        }
    }
}
