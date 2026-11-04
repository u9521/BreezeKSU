package me.weishu.kernelsu.ui.webui.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.icons.MaterialSymbols

/**
 * Shown once per session when the page asked for edge-to-edge on a WebView that cannot provide
 * safe-area insets.
 *
 * Three ways out, reaching the same two callbacks: swipe the card away, tap it to open the
 * WebView package's store page, or tap the close button to dismiss. Swiping and the close
 * button both call [onDismiss], so the gesture is a second route to an existing action.
 *
 * The card carries a button role for TalkBack, which a bare `Card(onClick)` does not; the close
 * button is its own `IconButton` so tapping it cannot also fire the update.
 */
@Composable
fun OutdatedWebViewBanner(
    currentVersion: Int,
    requiredVersion: Int,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        onDismiss = { onDismiss() },
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            onClick = onUpdateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .semantics {
                    role = Role.Button
                    onClick(label = "") { onUpdateClick(); true }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.webview_outdated_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(
                            R.string.webview_outdated_message,
                            currentVersion,
                            requiredVersion,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = MaterialSymbols.Filled.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
