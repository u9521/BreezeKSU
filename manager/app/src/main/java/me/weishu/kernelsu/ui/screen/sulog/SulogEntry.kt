package me.weishu.kernelsu.ui.screen.sulog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator

/**
 * Breeze's su log page: owns the state/action wiring so the upstream su log screen does
 * not have to know about Breeze.
 */
@Composable
fun SulogEntry() {
    val navigator = LocalBreezeNavigator.current
    val viewModel = viewModel<SulogViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshLatest()
    }

    val state = SulogUiState(
        isLoading = uiState.isLoading,
        isRefreshing = uiState.isRefreshing,
        sulogStatus = uiState.sulogStatus,
        isSulogEnabled = uiState.isSulogEnabled,
        searchText = uiState.searchText,
        selectedFilters = uiState.selectedFilters,
        files = uiState.files,
        selectedFilePath = uiState.selectedFilePath,
        entries = uiState.entries,
        visibleEntries = uiState.visibleEntries,
        errorMessage = uiState.errorMessage,
    )
    val actions = SulogActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onRefresh = viewModel::refreshLatest,
        onEnableSulog = viewModel::enableSulog,
        onCleanFile = viewModel::cleanFile,
        onSearchTextChange = viewModel::setSearchText,
        onToggleFilter = viewModel::toggleFilter,
        onSelectFile = viewModel::refresh,
    )

    SulogScreenBreeze(state, actions)
}
