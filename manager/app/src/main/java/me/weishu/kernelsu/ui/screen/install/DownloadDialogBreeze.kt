package me.weishu.kernelsu.ui.screen.install

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog

/** Prompts for an HTTPS URL to download a module package. */
@Composable
fun DownloadDialogBreeze(
    show: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    // Clear the input on each open since the composable stays mounted across closes.
    LaunchedEffect(show) { if (show) url = "" }
    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = show,
        title = { Text(stringResource(R.string.download_dialog_title)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text(stringResource(R.string.download_dialog_msg)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                enabled = isValidDownloadUrl(url.trim()),
                onClick = { onConfirm(url.trim()) },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            OutlinedButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

private fun isValidDownloadUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    val uri = url.toUri()
    return uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrEmpty() &&
            !uri.path.isNullOrEmpty()
}
