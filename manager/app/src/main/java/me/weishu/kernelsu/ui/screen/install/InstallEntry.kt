package me.weishu.kernelsu.ui.screen.install

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.LkmSelection
import me.weishu.kernelsu.data.ksu.getAvailablePartitions
import me.weishu.kernelsu.data.ksu.getCurrentKmi
import me.weishu.kernelsu.data.ksu.getDefaultPartition
import me.weishu.kernelsu.data.ksu.getSlotSuffix
import me.weishu.kernelsu.data.ksu.isAbDevice
import me.weishu.kernelsu.data.ksu.rootAvailable
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.util.UiMessage

/**
 * Breeze's install page: owns the state/action wiring so the upstream install screen
 * does not have to know about Breeze.
 */
@Composable
fun InstallEntry() {
    val navigator = LocalBreezeNavigator.current
    val context = LocalContext.current

    var message by remember { mutableStateOf<UiMessage?>(null) }

    var installMethod by rememberSaveable { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by rememberSaveable { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var partitionSelectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var hasCustomSelected by rememberSaveable { mutableStateOf(false) }
    val showChooseKmiDialog = rememberSaveable { mutableStateOf(false) }
    var advancedOptionsShown by rememberSaveable { mutableStateOf(false) }
    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdb by rememberSaveable { mutableStateOf(false) }
    var forceBackup by rememberSaveable { mutableStateOf(false) }

    val currentKmi by produceState(initialValue = "") { value = getCurrentKmi() }
    val partitions by produceState(initialValue = emptyList()) { value = getAvailablePartitions() }
    val defaultPartition by produceState(initialValue = "") { value = getDefaultPartition() }
    val rootAvailable by produceState(initialValue = false) { value = rootAvailable() }
    val isAbDevice by produceState(initialValue = false) { value = isAbDevice() }
    val isGkiDevice by produceState(initialValue = false) { value = getKernelVersion().isGKI() }

    val selectFileTip = stringResource(id = R.string.select_file_tip, defaultPartition)
    val selectFileTipNoGki = stringResource(id = R.string.select_file_tip_nogki)
    val onlyKoFileMsg = stringResource(id = R.string.install_only_support_ko_file)
    val installMethodOptions = remember(
        rootAvailable,
        isAbDevice,
        isGkiDevice,
        selectFileTip,
        selectFileTipNoGki
    ) {
        buildList {
            add(InstallMethod.SelectFile(summary = if (isGkiDevice) selectFileTip else selectFileTipNoGki))
            if (rootAvailable && isGkiDevice) {
                add(InstallMethod.DirectInstall)
                if (isAbDevice) add(InstallMethod.DirectInstallToInactiveSlot)
            }
        }
    }

    val isOta = installMethod is InstallMethod.DirectInstallToInactiveSlot
    val slotSuffix by produceState(initialValue = "", isOta) { value = getSlotSuffix(isOta) }
    val defaultIndex = remember(partitions, defaultPartition) {
        partitions.indexOf(defaultPartition).coerceAtLeast(0)
    }

    LaunchedEffect(partitions, defaultIndex, hasCustomSelected) {
        if (partitions.isEmpty()) return@LaunchedEffect
        if (!hasCustomSelected) {
            partitionSelectionIndex = defaultIndex.coerceIn(0, partitions.lastIndex)
        } else if (partitionSelectionIndex > partitions.lastIndex) {
            partitionSelectionIndex = partitions.lastIndex
        }
    }

    val displayPartitions = remember(partitions, defaultPartition) {
        partitions.map { name -> if (defaultPartition == name) "$name (default)" else name }
    }

    fun showMessage(text: String) {
        message = UiMessage(text = text)
    }

    val onInstall = {
        installMethod?.let { method ->
            navigator.push(
                BreezeRoute.Flash(
                    FlashIt.FlashBoot(
                        boot = if (method is InstallMethod.SelectFile) method.uri else null,
                        lkm = lkmSelection,
                        ota = method is InstallMethod.DirectInstallToInactiveSlot,
                        partition = partitions.getOrNull(partitionSelectionIndex),
                        allowShell = allowShell,
                        enableAdb = enableAdb,
                        backup = method is InstallMethod.SelectFile && forceBackup
                    )
                )
            )
        }
    }

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                if (isKoFile(context, uri)) {
                    lkmSelection = LkmSelection.LkmUri(uri)
                } else {
                    lkmSelection = LkmSelection.KmiNone
                    showMessage(onlyKoFileMsg)
                }
            }
        }
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                installMethod = InstallMethod.SelectFile(
                    uri,
                    summary = if (isGkiDevice) selectFileTip else selectFileTipNoGki
                )
            }
        }
    }

    val state = InstallUiState(
        installMethod = installMethod,
        lkmSelection = lkmSelection,
        partitionSelectionIndex = partitionSelectionIndex,
        displayPartitions = displayPartitions,
        currentKmi = currentKmi,
        slotSuffix = slotSuffix,
        installMethodOptions = installMethodOptions,
        canSelectPartition = installMethod is InstallMethod.DirectInstall ||
                installMethod is InstallMethod.DirectInstallToInactiveSlot,
        advancedOptionsShown = advancedOptionsShown,
        allowShell = allowShell,
        enableAdb = enableAdb,
        forceBackup = forceBackup,
        canForceBackup = installMethod is InstallMethod.SelectFile,
        message = message,
        showChooseKmiDialog = showChooseKmiDialog.value,
    )
    val actions = InstallScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSelectMethod = { method -> installMethod = method },
        onSelectBootImage = {
            selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
            })
        },
        onUploadLkm = {
            selectLkmLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
            })
        },
        onClearLkm = { lkmSelection = LkmSelection.KmiNone },
        onSelectPartition = { index ->
            hasCustomSelected = true
            partitionSelectionIndex = index
        },
        onNext = {
            val isLkmSelected = lkmSelection != LkmSelection.KmiNone
            val isKmiUnknown = currentKmi.isBlank()
            val isKmiUnresolved = when (installMethod) {
                is InstallMethod.SelectFile -> true
                else -> isKmiUnknown
            }
            if (!isLkmSelected && isKmiUnresolved) {
                showChooseKmiDialog.value = true
            } else {
                onInstall()
            }
        },
        onAdvancedOptionsClicked = {
            advancedOptionsShown = !advancedOptionsShown
        },
        onSelectAllowShell = {
            allowShell = it
        },
        onSelectEnableAdb = {
            enableAdb = it
        },
        onSelectForceBackup = {
            forceBackup = it
        },
        onMessageShown = { message = null },
        onDismissChooseKmi = { showChooseKmiDialog.value = false },
        onSelectKmi = { kmi ->
            showChooseKmiDialog.value = false
            kmi?.let {
                lkmSelection = LkmSelection.KmiString(it)
                onInstall()
            }
        },
    )

    InstallScreenBreeze(state, actions)
}
