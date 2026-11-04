package me.weishu.kernelsu.ui.webui.model

import androidx.compose.runtime.Immutable

/** A dismissible notice stacked at the top of the WebUI. */
@Immutable
sealed interface WebUIBanner {
    val id: String

    data class OutdatedWebView(
        val currentVersion: Int,
        val requiredVersion: Int,
    ) : WebUIBanner {
        override val id: String get() = ID

        companion object {
            const val ID = "outdated_webview"
        }
    }
}
