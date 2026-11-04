package me.weishu.kernelsu.ui.webui.model

import androidx.compose.runtime.Immutable

/**
 * A JavaScript dialog the page is blocked on. Every instance carries the callbacks that answer
 * it, so a dialog cannot be dismissed without the page learning about it.
 */
@Immutable
sealed interface WebUIDialog {
    val message: String
    val onDismiss: () -> Unit

    data class Alert(
        override val message: String,
        override val onDismiss: () -> Unit,
        val onConfirm: () -> Unit,
    ) : WebUIDialog

    data class Confirm(
        override val message: String,
        override val onDismiss: () -> Unit,
        val onConfirm: () -> Unit,
    ) : WebUIDialog

    data class Prompt(
        override val message: String,
        val defaultValue: String,
        override val onDismiss: () -> Unit,
        val onConfirm: (String) -> Unit,
    ) : WebUIDialog
}
