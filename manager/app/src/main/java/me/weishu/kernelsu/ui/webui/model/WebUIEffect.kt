package me.weishu.kernelsu.ui.webui.model

/**
 * One-shot things the host has to do for the page. They are events, not state: collecting the
 * same effect twice must not run it twice, which is why they travel through a Channel.
 */
sealed interface WebUIEffect {
    data class ShowToast(val message: String) : WebUIEffect
    data object Finish : WebUIEffect
    data class RequestFileChooser(val requestId: Long) : WebUIEffect
    data class OpenExternalLink(val url: String) : WebUIEffect
}
