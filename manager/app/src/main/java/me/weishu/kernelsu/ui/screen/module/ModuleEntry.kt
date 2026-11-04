package me.weishu.kernelsu.ui.screen.module

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.data.service.download
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.util.createWebuiIntent

/**
 * Breeze's module page.
 *
 * A finished update is not pushed straight to the flash screen: the downloaded URI is parked
 * and Breeze's own install dialog shows what the archive contains first.
 */
@Composable
fun ModuleEntry(
    isCurrentPage: Boolean = true,
) {
    val navigator = LocalBreezeNavigator.current
    val context = LocalContext.current
    val resource = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = viewModel<ModuleViewModel>()
    val scope = rememberCoroutineScope()
    val rawUiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingInstallModuleUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // No result launcher: the WebUI runs in its own task and cannot change the module list while
    // it is open, so there is nothing to await. Opening it is a plain start.

    // Request notification permission for download progress notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Download works regardless of result */ }
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    var hasActivated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            if (!hasActivated) {
                hasActivated = true
                viewModel.refreshEnvironmentState()
                viewModel.initializePreferences()
                val state = viewModel.uiState.value
                if (!state.hasLoaded && !state.isRefreshing) {
                    viewModel.fetchModuleList()
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else if (rawUiState.searchText.isNotEmpty()) {
            viewModel.setSearchText("")
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            val state = viewModel.uiState.value
            if (!state.isRefreshing) {
                viewModel.fetchModuleList(
                    checkUpdate = !state.hasLoaded || viewModel.isNeedRefresh,
                    resort = !state.hasLoaded,
                )
            }
        }
        initialResumeHandled.value = true
        onPauseOrDispose {}
    }

    val actions = ModuleActions(
        onRefresh = {
            viewModel.fetchModuleList(checkUpdate = true)
        },
        onSearchTextChange = viewModel::setSearchText,
        onClearSearch = {
            viewModel.setSearchText("")
        },
        onRequestUpdateConfirmation = { module, updateInfo ->
            viewModel.requestUpdateConfirmation(module, updateInfo)
        },
        onRequestUninstallConfirmation = { module ->
            viewModel.requestUninstallConfirmation(module)
        },
        onDismissConfirmRequest = {
            viewModel.dismissConfirmRequest()
        },
        onConfirmUpdate = { request ->
            scope.launch {
                download(
                    url = request.downloadUrl,
                    fileName = request.fileName,
                    onDownloaded = { uri ->
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            pendingInstallModuleUri = uri
                        } else {
                            navigator.push(BreezeRoute.Flash(FlashIt.FlashModules(listOf(uri))))
                            viewModel.markNeedRefresh()
                        }
                    },
                    onDownloading = {
                        viewModel.emitEffect(
                            ModuleEffect.Toast(
                                resource.getString(R.string.module_downloading)
                                    .format(request.module.name)
                            )
                        )
                    },
                )
            }
            viewModel.dismissConfirmRequest()
        },
        onOpenRepo = { },
        onToggleSortActionFirst = {
            viewModel.toggleSortActionFirst()
        },
        onToggleSortEnabledFirst = {
            viewModel.toggleSortEnabledFirst()
        },
        onOpenWebUi = { module ->
            context.startActivity(createWebuiIntent(context, module.id))
        },
        onToggleModule = { module ->
            viewModel.toggleModule(module)
        },
        onUninstallModule = { module ->
            viewModel.uninstallModule(module)
        },
        onUndoUninstallModule = { module ->
            viewModel.undoUninstallModule(module)
        },
        onOpenFlash = { uris ->
            if (uris.isNotEmpty()) {
                navigator.push(BreezeRoute.Flash(FlashIt.FlashModules(uris)))
                viewModel.markNeedRefresh()
            }
        },
        onExecuteModuleAction = { module ->
            navigator.push(BreezeRoute.ExecuteModuleAction(module.id))
            viewModel.markNeedRefresh()
        },
        onConfirmPendingInstall = {
            pendingInstallModuleUri?.let { uri ->
                navigator.push(BreezeRoute.Flash(FlashIt.FlashModules(listOf(uri))))
                viewModel.markNeedRefresh()
            }
            pendingInstallModuleUri = null
        },
        onDismissPendingInstall = { pendingInstallModuleUri = null },
    )

    ModulePagerBreeze(
        uiState = rawUiState.copy(pendingInstallUri = pendingInstallModuleUri),
        confirmDialogState = rawUiState.confirmDialogState,
        moduleEvent = viewModel.moduleEvent,
        actions = actions,
        isActiveTab = isCurrentPage,
    )
}
