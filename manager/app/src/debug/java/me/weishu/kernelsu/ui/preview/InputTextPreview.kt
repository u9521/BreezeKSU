package me.weishu.kernelsu.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.breeze.text.OutlinedTextEdit
import me.weishu.kernelsu.ui.component.breeze.text.rememberOutlinedTextEditState

@Composable
fun InputTextPreview() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "InputText — Outlined Text Edit",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextEdit(
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            state = rememberOutlinedTextEditState(
                text = name,
                onValueChange = { name = it },
            ),
            defaultSupportingText = { Text("Enter your name") }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextEdit(
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            state = rememberOutlinedTextEditState(
                text = email,
                validator = { input ->
                    if (input.isNotEmpty() && !input.contains("@")) "Invalid email format" else null
                },
                onValueChange = { email = it },
            ),
            defaultSupportingText = { Text("Enter a valid email address") }
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "Both fields commit when they lose focus, not on every keystroke.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewInputText() {
    InputTextPreview()
}
