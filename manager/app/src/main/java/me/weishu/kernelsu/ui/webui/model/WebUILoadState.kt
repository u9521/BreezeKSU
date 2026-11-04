package me.weishu.kernelsu.ui.webui.model

import androidx.compose.runtime.Immutable

/** What the host is currently waiting for. Only [Ready] means the page can be interacted with. */
@Immutable
sealed interface WebUILoadState {
    data object Idle : WebUILoadState
    data object LoadingModule : WebUILoadState
    data object LoadingPage : WebUILoadState
    data object Ready : WebUILoadState
}

/**
 * Everything the WebUI host renders from.
 *
 * No field carries an IME height: when the keyboard opens only the visual viewport shrinks, so
 * the host must not pad on top of the page or the content is padded twice.
 */
@Immutable
data class WebUIState(
    val loadState: WebUILoadState = WebUILoadState.Idle,
    /** The module whose page is on screen; the dialogs are titled with its name. */
    val moduleInfo: ModuleInfo? = null,
    val activeDialog: WebUIDialog? = null,
    val isEdgeToEdge: Boolean = false,
    val isInsetsSupported: Boolean = true,
    val webViewVersion: Int = 0,
    val webViewPackageName: String = "",
    val banners: List<WebUIBanner> = emptyList(),
    val shownBannerIds: Set<String> = emptySet(),
    val webCanGoBack: Boolean = false,
) {
    val isLoading: Boolean get() = loadState != WebUILoadState.Ready
}
