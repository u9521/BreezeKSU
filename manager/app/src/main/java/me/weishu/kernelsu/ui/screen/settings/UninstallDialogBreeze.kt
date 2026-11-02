package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.data.model.UninstallType
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.nav.BreezeRoute.Flash
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.util.PopupFeedBack
import me.weishu.kernelsu.ui.util.keyDownFeedBack

/** The catalogue icon lives here, not on the model: `data/` owns no drawings. */
private val UninstallType.icon: ImageVector
    get() = when (this) {
        UninstallType.TEMPORARY -> MaterialSymbols.Filled.Rounded.RemoveModerator
        UninstallType.PERMANENT -> MaterialSymbols.Filled.Rounded.DeleteForever
        UninstallType.RESTORE_STOCK_IMAGE -> MaterialSymbols.Filled.Rounded.RestartAlt
        UninstallType.NONE -> MaterialSymbols.Filled.Rounded.Adb
    }

@Composable
fun UninstallDialogBreeze(
    show: Boolean,
    onDismissRequest: () -> Unit
) {
    val navigator = LocalBreezeNavigator.current
    val context = LocalContext.current
    var pendingUninstallType by rememberSaveable { mutableStateOf<UninstallType?>(null) }

    val performUninstall = { type: UninstallType ->
        when (type) {
            UninstallType.TEMPORARY -> {
                Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
            }

            UninstallType.PERMANENT -> {
                navigator.push(Flash(FlashIt.FlashUninstall))
            }

            UninstallType.RESTORE_STOCK_IMAGE -> {
                navigator.push(Flash(FlashIt.FlashRestore))
            }

            UninstallType.NONE -> {}
        }
    }

    val confirmDialog = rememberConfirmDialog()
    val pending = pendingUninstallType
    if (pending != null) {
        val title = stringResource(pending.title)
        val message = stringResource(pending.message)

        LaunchedEffect(pending) {
            confirmDialog.setConfirm(
                ConfirmBody(
                    title = title,
                    body = ConfirmBody.Body.Text(message),
                    onConfirm = {
                        performUninstall(pending)
                        pendingUninstallType = null
                        onDismissRequest()
                    },
                    onDismiss = { pendingUninstallType = null },
                )
            )
        }
    } else {
        // The selection dialog closed without a choice, so any confirm left over goes with it.
        LaunchedEffect(Unit) { confirmDialog.setConfirm(null) }
    }

    UninstallSelectionDialogBreeze(
        visible = show,
        onDismissRequest = onDismissRequest,
        onOptionSelected = { type ->
            if (type == null) return@UninstallSelectionDialogBreeze
            pendingUninstallType = type
        },
    )
}

@Composable
private fun UninstallOptionItemBreeze(
    title: String,
    summary: String?,
    selected: Boolean = false,
    icon: ImageVector,
    index: Int,
    counts: Int,
    onClick: () -> Unit
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, counts),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Icon(
                imageVector = icon, contentDescription = null, modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp), tint = MaterialTheme.colorScheme.primary
            )
        },
        supportingContent = summary?.let {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    )
}

@Composable
private fun UninstallSelectionDialogBreeze(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onOptionSelected: (UninstallType?) -> Unit,
) {
    val keyFeedback = keyDownFeedBack()
    var selectedOption by rememberSaveable { mutableStateOf<UninstallType?>(null) }
    // Reset selection when opened since the composable stays mounted across closes.
    LaunchedEffect(visible) { if (visible) selectedOption = null }
    if (visible) PopupFeedBack()
    BreezeAlertDialog(
        onDismissRequest = onDismissRequest,
        visible = visible,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        icon = {
            Icon(
                MaterialSymbols.Filled.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(text = stringResource(R.string.settings_uninstall)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                val uninstallTypes =
                    UninstallType.entries.filter { it != UninstallType.TEMPORARY && it != UninstallType.NONE }
                uninstallTypes.forEachIndexed { index, type ->
                    UninstallOptionItemBreeze(
                        title = stringResource(type.title),
                        summary = stringResource(type.message),
                        selected = selectedOption == type,
                        icon = type.icon,
                        index = index,
                        counts = uninstallTypes.size,
                        onClick = {
                            selectedOption = type
                        })
                }
            }
        },
        confirmButton = {
            Button(enabled = selectedOption != null, onClick = {
                keyFeedback()
                onOptionSelected(selectedOption)
            }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = {
                keyFeedback()
                onDismissRequest()
            }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

