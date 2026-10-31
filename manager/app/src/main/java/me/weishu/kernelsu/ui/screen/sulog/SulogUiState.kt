package me.weishu.kernelsu.ui.screen.sulog

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.data.ksu.SulogEntry
import me.weishu.kernelsu.data.ksu.SulogEventFilter
import me.weishu.kernelsu.data.ksu.SulogFile
import me.weishu.kernelsu.data.ksu.defaultSulogEventFilters

/** Snapshot rendered by [SulogScreenBreeze]; [SulogViewModel] is its only producer. */
@Immutable
data class SulogUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val sulogStatus: String = "",
    val isSulogEnabled: Boolean = false,
    val searchText: String = "",
    val selectedFilters: Set<SulogEventFilter> = defaultSulogEventFilters(),
    val files: List<SulogFile> = emptyList(),
    val selectedFilePath: String? = null,
    val entries: List<SulogEntry> = emptyList(),
    val visibleEntries: List<SulogEntry> = emptyList(),
    val errorMessage: String? = null,
)

@Immutable
data class SulogActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onEnableSulog: () -> Unit,
    val onCleanFile: () -> Unit,
    val onSearchTextChange: (String) -> Unit,
    val onToggleFilter: (SulogEventFilter) -> Unit,
    val onSelectFile: (String) -> Unit,
)

@Immutable
data class SulogFileSelector(
    val items: List<String>,
    val selectedIndex: Int,
)
