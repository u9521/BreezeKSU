package me.weishu.kernelsu.ui.component.breeze.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlaySheet

/** One large tappable action in [ActionSheetBreeze]. */
data class SheetAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

/** Bottom sheet presenting a horizontal row of large circular icon actions. */
@Composable
fun ActionSheetBreeze(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    BreezeOverlaySheet(
        onDismissRequest = onDismissRequest,
        visible = visible,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 32.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            actions.forEachIndexed { index, action ->
                if (index > 0) Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledTonalIconButton(
                        modifier = Modifier.size(64.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            action.onClick()
                        },
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(text = action.label, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
