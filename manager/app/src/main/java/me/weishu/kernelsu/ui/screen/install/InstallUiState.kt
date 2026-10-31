package me.weishu.kernelsu.ui.screen.install

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.data.ksu.LkmSelection
import me.weishu.kernelsu.ui.util.UiMessage

@Immutable
internal data class InstallUiState(
    val installMethod: InstallMethod?,
    val lkmSelection: LkmSelection,
    val partitionSelectionIndex: Int,
    val displayPartitions: List<String>,
    val remotePartitionSelectionIndex: Int,
    val remoteDisplayPartitions: List<String>,
    val currentKmi: String,
    val slotSuffix: String,
    val installMethodOptions: List<InstallMethod>,
    val canSelectPartition: Boolean,
    val advancedOptionsShown: Boolean,
    val allowShell: Boolean,
    val enableAdb: Boolean,
    val forceBackup: Boolean,
    val canForceBackup: Boolean,
    /** One-shot feedback shown by the scaffold; [InstallScreenActions.onMessageShown] clears it. */
    val message: UiMessage? = null,
    /** A remote boot-partition probe is in flight behind the spinner dialog. */
    val isProbing: Boolean = false,
    val showChooseKmiDialog: Boolean = false,
    val showDownloadDialog: Boolean = false,
)

@Immutable
internal data class InstallScreenActions(
    val onBack: () -> Unit,
    val onSelectMethod: (InstallMethod) -> Unit,
    val onDownloadFile: () -> Unit,
    val onSelectBootImage: () -> Unit,
    val onUploadLkm: () -> Unit,
    val onClearLkm: () -> Unit,
    val onSelectPartition: (Int) -> Unit,
    val onNext: () -> Unit,
    val onAdvancedOptionsClicked: () -> Unit,
    val onSelectAllowShell: (Boolean) -> Unit,
    val onSelectEnableAdb: (Boolean) -> Unit,
    val onSelectForceBackup: (Boolean) -> Unit,
    val onMessageShown: (Long) -> Unit,
    val onDismissChooseKmi: () -> Unit,
    val onSelectKmi: (String?) -> Unit,
    val onDismissDownloadDialog: () -> Unit,
    val onConfirmDownloadUrl: (String) -> Unit,
)
