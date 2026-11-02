package me.weishu.kernelsu.ui.webui

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.dialog.InputDialogBreeze

/**
 * Bridges the WebView's JS dialogs onto the shared overlay window.
 *
 * Every path out of a dialog - confirm, cancel, back, outside tap - must report a result to the
 * `JsResult` the page is blocked on. A dismissed dialog that forgets it hangs the page's
 * JavaScript forever, so each dialog answers the page on every exit, not just on the confirm
 * button.
 *
 * The dialogs stay mounted at the top level instead of inside the event `when`: answering a
 * dialog flips [WebUIState.uiEvent] to [WebUIEvent.WebViewReady] on the next frame, and a dialog
 * that unmounted with its branch would cut its own 150ms exit animation short. Each dialog is
 * driven by a `visible`/`body` flag derived from the event, and both retain their last content
 * while animating out.
 */
@Composable
fun HandleWebUIEventBreeze(
    webUIState: WebUIState,
    fileLauncher: ActivityResultLauncher<Intent>
) {
    val event = webUIState.uiEvent
    val dialogTitle = webUIState.moduleName.ifBlank { stringResource(R.string.app_name) }

    // Alert and confirm share one dialog: an alert has no dismiss button, so both exits answer
    // the page the same way.
    val confirmBody: ConfirmBody? = when (event) {
        is WebUIEvent.ShowAlert -> ConfirmBody(
            title = dialogTitle,
            body = ConfirmBody.Body.Text(event.message),
        )

        is WebUIEvent.ShowConfirm -> ConfirmBody(
            title = dialogTitle,
            body = ConfirmBody.Body.Text(event.message),
            onDismiss = {},
        )

        else -> null
    }
    ConfirmDialogBreeze(
        body = confirmBody,
        onConfirm = {
            when (webUIState.uiEvent) {
                is WebUIEvent.ShowConfirm -> webUIState.onConfirmResult(true)
                is WebUIEvent.ShowAlert -> webUIState.onAlertResult()
                else -> Unit
            }
        },
        onDismiss = {
            when (webUIState.uiEvent) {
                is WebUIEvent.ShowConfirm -> webUIState.onConfirmResult(false)
                is WebUIEvent.ShowAlert -> webUIState.onAlertResult()
                else -> Unit
            }
        },
    )

    // Retain the last prompt while the input dialog animates out; an empty answer is a valid JS
    // prompt result, so confirm is never gated on content.
    var lastPrompt by remember { mutableStateOf<WebUIEvent.ShowPrompt?>(null) }
    if (event is WebUIEvent.ShowPrompt) lastPrompt = event
    InputDialogBreeze(
        visible = event is WebUIEvent.ShowPrompt,
        title = { Text(lastPrompt?.message.orEmpty()) },
        placeholder = {},
        initialText = lastPrompt?.defaultValue.orEmpty(),
        validator = { true },
        onConfirm = { webUIState.onPromptResult(it) },
        onDismiss = { webUIState.onPromptResult(null) },
    )

    if (event is WebUIEvent.ShowFileChooser) {
        LaunchedEffect(event) {
            try {
                fileLauncher.launch(event.intent)
            } catch (_: Exception) {
                webUIState.onFileChooserResult(null)
            }
        }
    }
}
