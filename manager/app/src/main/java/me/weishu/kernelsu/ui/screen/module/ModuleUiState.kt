package me.weishu.kernelsu.ui.screen.module

import android.net.Uri
import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo

sealed interface ModuleConfirmRequest {
    data class Update(
        val module: Module,
        val downloadUrl: String,
        val fileName: String,
        /** Kept so a failed changelog fetch can be retried in place. */
        val updateInfo: ModuleUpdateInfo? = null,
    ) : ModuleConfirmRequest

    data class Uninstall(
        val module: Module,
    ) : ModuleConfirmRequest
}

@Immutable
data class ModuleConfirmDialogState(
    val request: ModuleConfirmRequest,
    val title: String,
    val content: String? = null,
    val markdown: Boolean = false,
    val confirm: String? = null,
    val dismiss: String? = null,
    /** Changelog fetch failed; the empty state offers a retry. */
    val changelogFailed: Boolean = false,
    /** Module `updateJson` declares no changelog URL. */
    val changelogNotConfigured: Boolean = false,
    /** Changelog URL returned a blank body. */
    val changelogEmpty: Boolean = false,
    /** Changelog fetch is in flight; shown inside the dialog before the text arrives. */
    val changelogLoading: Boolean = false,
)

sealed interface ModuleEffect {
    data class Toast(
        val message: String,
    ) : ModuleEffect

    data class SnackBar(
        val message: String,
    ) : ModuleEffect
}

@Immutable
data class ModuleUiState(
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val modules: List<Module> = emptyList(),
    val moduleList: List<Module> = emptyList(),
    val updateInfo: Map<String, ModuleUpdateInfo> = emptyMap(),
    /** Raw query from the search bar; empty means "not searching". */
    val searchText: String = "",
    val searchResults: List<Module> = emptyList(),
    val sortEnabledFirst: Boolean = false,
    val sortActionFirst: Boolean = false,
    val checkModuleUpdate: Boolean = true,
    /** How many lines a module description may occupy before it is truncated. */
    val descriptionMaxLines: Int = 4,
    val isSafeMode: Boolean = false,
    val magiskInstalled: Boolean = false,
    val confirmDialogState: ModuleConfirmDialogState? = null,
    /** A downloaded module archive waiting for the install confirmation dialog. */
    val pendingInstallUri: Uri? = null,
) {
    val installButtonVisible: Boolean
        get() = !(isSafeMode || magiskInstalled)
}

@Immutable
data class ModuleActions(
    val onRefresh: () -> Unit,
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onRequestUpdateConfirmation: (Module, ModuleUpdateInfo) -> Unit,
    val onRequestUninstallConfirmation: (Module) -> Unit,
    val onDismissConfirmRequest: () -> Unit,
    val onConfirmUpdate: (ModuleConfirmRequest.Update) -> Unit,
    val onOpenRepo: () -> Unit,
    val onToggleSortActionFirst: () -> Unit,
    val onToggleSortEnabledFirst: () -> Unit,
    val onOpenWebUi: (Module) -> Unit,
    val onToggleModule: (Module) -> Unit,
    val onUninstallModule: (Module) -> Unit,
    val onUndoUninstallModule: (Module) -> Unit,
    val onOpenFlash: (List<Uri>) -> Unit,
    val onExecuteModuleAction: (Module) -> Unit,
    val onConfirmPendingInstall: () -> Unit,
    val onDismissPendingInstall: () -> Unit,
)
