package me.weishu.kernelsu.ui.webui.ui.component.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.dialog.InputDialogBreeze
import me.weishu.kernelsu.ui.webui.model.WebUIDialog

/**
 * A `window.alert`/`confirm`/`prompt` the page is blocked on, drawn with Breeze dialogs.
 *
 * They must be [ConfirmDialogBreeze] and [InputDialogBreeze] rather than a Material
 * `AlertDialog`: only those render through the shared overlay window, which is what supplies the
 * blur, the scrim and the motion.
 *
 * Answering the page is the host's job. `onConfirm`/`onDismiss` clear the model and answer it,
 * because a `ConfirmDialogBreeze` left alone stays mounted while its body is non-null.
 */
@Composable
fun WebUIDialogContent(
    dialog: WebUIDialog,
    title: String,
    onClear: () -> Unit,
) {
    // Alert and confirm share one dialog: an alert has no dismiss button, so both exits answer
    // the page the same way.
    val confirmBody: ConfirmBody? = when (dialog) {
        is WebUIDialog.Alert -> ConfirmBody(
            title = title,
            body = ConfirmBody.Body.Text(dialog.message),
        )

        is WebUIDialog.Confirm -> ConfirmBody(
            title = title,
            body = ConfirmBody.Body.Text(dialog.message),
            // A non-null dismiss handler is what makes the Cancel button appear.
            onDismiss = {},
        )

        is WebUIDialog.Prompt -> null
    }
    ConfirmDialogBreeze(
        body = confirmBody,
        onConfirm = {
            onClear()
            when (dialog) {
                is WebUIDialog.Alert -> dialog.onConfirm()
                is WebUIDialog.Confirm -> dialog.onConfirm()
                is WebUIDialog.Prompt -> Unit
            }
        },
        onDismiss = {
            onClear()
            dialog.onDismiss()
        },
    )

    // Retain the last prompt while the input dialog animates out; an empty answer is a valid JS
    // prompt result, so confirm is never gated on content.
    var lastPrompt by remember { mutableStateOf<WebUIDialog.Prompt?>(null) }
    if (dialog is WebUIDialog.Prompt) lastPrompt = dialog
    InputDialogBreeze(
        visible = dialog is WebUIDialog.Prompt,
        title = { Text(lastPrompt?.message.orEmpty()) },
        placeholder = {},
        initialText = lastPrompt?.defaultValue.orEmpty(),
        validator = { true },
        confirmText = stringResource(R.string.confirm),
        onConfirm = { answer ->
            onClear()
            lastPrompt?.onConfirm(answer)
        },
        onDismiss = {
            onClear()
            lastPrompt?.onDismiss?.invoke()
        },
    )
}
