package me.weishu.kernelsu.ui.component.breeze.dialog

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
import androidx.compose.ui.text.input.KeyboardType
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog

/** Single-field text input dialog; resets to [initialText] on open and gates confirm on [validator]. */
@Composable
fun InputDialogBreeze(
    visible: Boolean,
    title: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    validator: (String) -> Boolean = { it.isNotBlank() },
) {
    var text by remember { mutableStateOf(initialText) }
    LaunchedEffect(visible) { if (visible) text = initialText }

    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = visible,
        modifier = modifier,
        title = title,
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = placeholder,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                enabled = validator(text.trim()),
                onClick = { onConfirm(text.trim()) },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}
