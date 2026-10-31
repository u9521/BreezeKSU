package me.weishu.kernelsu.ui.screen.install

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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.getCurrentKmi
import me.weishu.kernelsu.data.ksu.getSupportedKmis
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.util.PopupFeedBack

@Composable
fun ChooseKmiDialogBreeze(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (String?) -> Unit
) {
    val supportedKMIs by produceState(initialValue = emptyList()) {
        value = getSupportedKmis()
    }

    val currentKmi by produceState(initialValue = "") {
        value = getCurrentKmi()
    }

    KmiSelectDialog(
        visible = show,
        currentKmi = currentKmi,
        options = supportedKMIs,
        onConfirm = onSelected,
        onDismiss = onDismissRequest,
    )
}

@Composable
private fun KmiSelectDialog(
    visible: Boolean,
    currentKmi: String,
    options: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOption by rememberSaveable { mutableStateOf(currentKmi) }
    var canConfirm by remember { mutableStateOf(false) }
    val enableConfirm = { canConfirm = true }
    val haptic = LocalHapticFeedback.current
    // Reset selection when opened since the composable stays mounted across closes.
    LaunchedEffect(visible) {
        if (visible) {
            selectedOption = currentKmi
            canConfirm = false
        }
    }
    if (visible) PopupFeedBack()

    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = visible,
        modifier = Modifier.heightIn(max = 500.dp),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.select_kmi),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(
                        R.string.current_kmi,
                        currentKmi.let { it.ifBlank { "Unknown" } }),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }, text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                options.forEachIndexed { index, kmi ->
                    val isSelected = (kmi == selectedOption)
                    LaunchedEffect(isSelected) {
                        if (isSelected) enableConfirm()
                    }
                    SegmentedListItem(
                        modifier = Modifier.heightIn(64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        selected = isSelected, onClick = {
                            selectedOption = kmi
                        },
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                        shapes = ListItemDefaults.segmentedShapes(index, options.size),
                        leadingContent = {
                            RadioButton(
                                selected = isSelected, onClick = null
                            )
                        }, content = {
                            Column {
                                Text(
                                    text = kmi,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        },
                        supportingContent = if (kmi == currentKmi) {
                            {
                                Text(
                                    text = stringResource(R.string.current_device_kmi),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                }
            }
        }, confirmButton = {
            Button(
                enabled = canConfirm, onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onConfirm(selectedOption)
                }, shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }, dismissButton = {
            OutlinedButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onDismiss()
                }) {
                Text(stringResource(android.R.string.cancel))
            }
        })
}

