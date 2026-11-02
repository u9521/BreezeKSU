package me.weishu.kernelsu.ui.screen.install

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.R.string.install
import me.weishu.kernelsu.data.ksu.LkmSelection
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.LoadingDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedCheckboxItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListScope
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedMenuRow
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedRadioItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.keyDownFeedBack

/**
 * @author weishu
 * @date 2024/3/12.
 */
@Composable
internal fun InstallScreenBreeze(
    uiState: InstallUiState,
    actions: InstallScreenActions,
) {
    val keyDownFeedBack = keyDownFeedBack()

    ChooseKmiDialogBreeze(
        show = uiState.showChooseKmiDialog,
        onDismissRequest = actions.onDismissChooseKmi,
        onSelected = actions.onSelectKmi,
    )
    DownloadDialogBreeze(
        show = uiState.showDownloadDialog,
        onConfirm = actions.onConfirmDownloadUrl,
        onDismiss = actions.onDismissDownloadDialog,
    )
    LoadingDialogBreeze(visible = uiState.isProbing)

    BreezeScaffold(
        message = uiState.message,
        onMessageShown = actions.onMessageShown,
        topBar = {
            BreezeTopAppBar(
                title = { Text(stringResource(install)) },
                navigationIcon = {
                    BreezeBackButton(
                        onClick = actions.onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                content = {
                    Text(stringResource(id = R.string.install_next))
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        MaterialSymbols.Filled.Rounded.ArrowForward,
                        stringResource(id = R.string.install_next)
                    )
                },
                modifier = Modifier
                    .animateFloatingActionButton(
                        visible = uiState.installMethod != null,
                        alignment = Alignment.CenterEnd,
                    ),
                onClick = {
                    keyDownFeedBack()
                    actions.onNext()
                },
            )
        },
        fabLift = true,
        fabVisible = uiState.installMethod != null,
    ) { innerPadding ->
        BreezeScrollColumn(
            innerPadding = innerPadding,
            topExtraSpacing = 16.dp,
        ) {
            InstallMethodSelector(
                state = uiState,
                onSelected = actions.onSelectMethod,
                onSelectBootImage = actions.onSelectBootImage,
                onDownloadFile = actions.onDownloadFile,
            )
            InstallConfigGroup(
                state = uiState,
                onForceBackup = actions.onSelectForceBackup,
                onSelectPartition = actions.onSelectPartition,
                onUploadLkm = actions.onUploadLkm,
                onClearLkm = actions.onClearLkm,
            )
            AdvancedOptionsGroup(
                state = uiState,
                onToggleExpanded = actions.onAdvancedOptionsClicked,
                onSelectAllowShell = actions.onSelectAllowShell,
                onSelectEnableAdb = actions.onSelectEnableAdb,
            )
            // Keep the last row clear of the floating action button and a single-line snackbar.
            // BreezeScrollColumn already reserves its own bottomExtraSpacing below this.
            Spacer(Modifier.height(56.dp + 12.dp + 48.dp + 12.dp))
        }
    }
}

@Composable
private fun InstallMethodSelector(
    state: InstallUiState,
    onSelected: (InstallMethod) -> Unit,
    onSelectBootImage: () -> Unit,
    onDownloadFile: () -> Unit,
) {
    val confirmDialog = rememberConfirmDialog()
    val dialogTitle = stringResource(android.R.string.dialog_alert_title)
    val dialogContent = stringResource(R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> onSelectBootImage()
            is InstallMethod.DownloadFile -> onDownloadFile()
            is InstallMethod.DirectInstall -> onSelected(option)
            is InstallMethod.DirectInstallToInactiveSlot -> confirmDialog.setConfirm(
                ConfirmBody(
                    title = dialogTitle,
                    body = ConfirmBody.Body.Text(dialogContent),
                    // No dismiss button: a warning with one way forward should not offer a
                    // second button that duplicates the back gesture.
                    onConfirm = { onSelected(InstallMethod.DirectInstallToInactiveSlot) },
                )
            )
        }
    }

    key(state.installMethodOptions.size) {
        SegmentedListGroup {
            state.installMethodOptions.forEach { option ->
                val isSelected = option.javaClass == state.installMethod?.javaClass
                item(key = option.label) {
                    SegmentedRadioItem(
                        title = stringResource(option.label),
                        summary = option.summary,
                        selected = isSelected,
                        onClick = { onClick(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallConfigGroup(
    state: InstallUiState,
    onForceBackup: (Boolean) -> Unit,
    onSelectPartition: (Int) -> Unit,
    onUploadLkm: () -> Unit,
    onClearLkm: () -> Unit,
) {
    val selectedLkmName = remember(state.lkmSelection) {
        (state.lkmSelection as? LkmSelection.LkmUri)?.let { it.uri.lastPathSegment ?: "(file)" }
    }
    val isDownloadFile = state.installMethod is InstallMethod.DownloadFile
    val partitions = if (isDownloadFile) state.remoteDisplayPartitions else state.displayPartitions
    val selectedPartition = if (isDownloadFile) {
        state.remoteDisplayPartitions.getOrNull(state.remotePartitionSelectionIndex).orEmpty()
    } else {
        state.displayPartitions.getOrNull(state.partitionSelectionIndex).orEmpty()
    }
    val slotSuffix = state.slotSuffix.ifBlank { null }

    SegmentedListGroup {
        item(key = "forceBackup", visible = state.canForceBackup) {
            SegmentedCheckboxItem(
                title = stringResource(R.string.install_force_backup),
                summary = stringResource(R.string.install_force_backup_summary),
                checked = state.forceBackup,
                onCheckedChange = onForceBackup,
            )
        }
        partitionSelector(
            visible = state.canSelectPartition,
            partitions = partitions,
            partition = selectedPartition,
            partitionSuffix = slotSuffix,
            onPartitionChange = onSelectPartition,
        )
        lkmSelector(
            selectedLkmName = selectedLkmName,
            onLaunchLkmPicker = onUploadLkm,
            onClearLkm = onClearLkm,
        )
    }
}

@Composable
private fun AdvancedOptionsGroup(
    state: InstallUiState,
    onToggleExpanded: () -> Unit,
    onSelectAllowShell: (Boolean) -> Unit,
    onSelectEnableAdb: (Boolean) -> Unit,
) {
    val rotationState by animateFloatAsState(
        targetValue = if (state.advancedOptionsShown) 180f else 0f,
        label = "InstallAdvancedOptionsRotation"
    )

    SegmentedListGroup {
        item(key = "advancedOptions") {
            SegmentedNavItem(
                title = stringResource(R.string.advanced_options),
                onClick = onToggleExpanded,
                trailing = {
                    Icon(
                        MaterialSymbols.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.expand),
                        modifier = Modifier.graphicsLayer { rotationZ = rotationState }
                    )
                },
            )
        }
        item(key = "allowShell", visible = state.advancedOptionsShown) {
            SegmentedCheckboxItem(
                title = stringResource(R.string.allow_shell),
                summary = stringResource(R.string.allow_shell_summary),
                checked = state.allowShell,
                onCheckedChange = onSelectAllowShell,
            )
        }
        item(key = "enableAdb", visible = state.advancedOptionsShown) {
            SegmentedCheckboxItem(
                title = stringResource(R.string.enable_adb),
                summary = stringResource(R.string.enable_adb_summary),
                checked = state.enableAdb,
                onCheckedChange = onSelectEnableAdb,
            )
        }
    }
}

private fun SegmentedListScope.lkmSelector(
    selectedLkmName: String?,
    onLaunchLkmPicker: () -> Unit,
    onClearLkm: () -> Unit,
) {
    item(key = selectedLkmName ?: "lkm") {
        SegmentedNavItem(
            title = stringResource(id = R.string.install_upload_lkm_file),
            summary = selectedLkmName?.let { name ->
                stringResource(id = R.string.selected_lkm, name)
            },
            leadingIcon = MaterialSymbols.Rounded.DriveFileMove,
            onClick = { onLaunchLkmPicker() },
            trailing = selectedLkmName?.let {
                {
                    IconButton(onClick = onClearLkm) {
                        Icon(MaterialSymbols.Filled.Rounded.Clear, null)
                    }
                }
            } ?: {},
        )
    }
}

private fun SegmentedListScope.partitionSelector(
    visible: Boolean,
    partitions: List<String>,
    partition: String,
    partitionSuffix: String?,
    onPartitionChange: (Int) -> Unit,
) {
    val suffix = if (partitionSuffix != null) " ($partitionSuffix)" else ""
    item(key = "$partition$partitionSuffix", visible = visible) {
        SegmentedMenuRow(
            title = "${stringResource(R.string.install_select_partition)}$suffix",
            selectedText = partition,
            leadingIcon = MaterialSymbols.Rounded.HardDrive,
            menuContent = { dismiss ->
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                    partitions.forEachIndexed { index, name ->
                        SelectableDropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onPartitionChange(index)
                                dismiss()
                            },
                            shapes = MenuDefaults.itemShape(index = index, count = partitions.size),
                            selected = name == partition,
                            selectedLeadingIcon = {
                                Icon(
                                    MaterialSymbols.Filled.Rounded.Check,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            }
                        )
                    }
                }
            },
        )
    }
}

