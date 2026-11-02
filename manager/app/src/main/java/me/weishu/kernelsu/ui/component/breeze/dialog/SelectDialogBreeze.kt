package me.weishu.kernelsu.ui.component.breeze.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.util.PopupFeedBack

/** Single-choice segmented radio dialog; resets selection to [current] whenever opened. */
@Composable
fun <T> SelectDialogBreeze(
    visible: Boolean,
    current: T?,
    options: List<T>,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    optionContent: @Composable (item: T, selected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    var selectedOption by rememberSaveable { mutableStateOf(current) }
    val canConfirm = selectedOption != null
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(visible) {
        if (visible) selectedOption = current
    }
    if (visible) PopupFeedBack()

    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = visible,
        modifier = modifier.heightIn(max = 500.dp),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                title()
                subtitle?.invoke()
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = option == selectedOption
                    SegmentedListItem(
                        modifier = Modifier.heightIn(64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        selected = isSelected,
                        onClick = { selectedOption = option },
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                        shapes = ListItemDefaults.segmentedShapes(index, options.size),
                        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                        content = { optionContent(option, isSelected) },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canConfirm,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    selectedOption?.let(onConfirm)
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onDismiss()
                },
            ) {
                Text(dismissText)
            }
        },
    )
}
