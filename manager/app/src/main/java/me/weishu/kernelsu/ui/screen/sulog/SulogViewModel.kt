package me.weishu.kernelsu.ui.screen.sulog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.ksu.SulogEntry
import me.weishu.kernelsu.data.ksu.SulogEventFilter
import me.weishu.kernelsu.data.ksu.SulogFileCleanAction
import me.weishu.kernelsu.data.ksu.buildVisibleSulogEntries
import me.weishu.kernelsu.data.ksu.cleanSulogFile
import me.weishu.kernelsu.data.ksu.defaultSulogEventFilters
import me.weishu.kernelsu.data.ksu.deleteSulogFile
import me.weishu.kernelsu.data.ksu.listSulogFiles
import me.weishu.kernelsu.data.ksu.parseSulogLines
import me.weishu.kernelsu.data.ksu.readSulogFile
import me.weishu.kernelsu.data.ksu.resolveSulogFileCleanAction
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.ksuApp

class SulogViewModel(
    private val repo: SettingsRepository = ksuApp.settingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SulogUiState())
    val uiState: StateFlow<SulogUiState> = _uiState.asStateFlow()
    private val entriesFlow = MutableStateFlow<List<SulogEntry>>(emptyList())
    private val searchTextFlow = MutableStateFlow("")
    private val selectedFiltersFlow = MutableStateFlow(defaultSulogEventFilters())

    private var refreshJob: Job? = null

    init {
        val savedFilters = repo.settings.value.suLogFilters
            ?.mapNotNull { raw -> SulogEventFilter.entries.firstOrNull { it.name == raw } }
            ?.toSet()
            ?.ifEmpty { defaultSulogEventFilters() }
            ?: defaultSulogEventFilters()
        selectedFiltersFlow.value = savedFilters
        _uiState.update { it.copy(selectedFilters = savedFilters) }

        viewModelScope.launch(Dispatchers.Default) {
            combine(
                entriesFlow,
                searchTextFlow,
                selectedFiltersFlow
            ) { entries, searchText, selectedFilters ->
                buildVisibleSulogEntries(entries, searchText, selectedFilters)
            }.collect { visibleEntries ->
                _uiState.update { it.copy(visibleEntries = visibleEntries) }
            }
        }
    }

    fun refresh(preferredFilePath: String? = _uiState.value.selectedFilePath) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val sulogStatus = repo.getSulogStatus()
                val isSulogEnabled = if (sulogStatus == "forced") {
                    repo.getSulogValue() == 1L
                } else {
                    repo.getSulogPersistValue() == 1L
                }
                val files = listSulogFiles()
                currentCoroutineContext().ensureActive()
                val selectedFile = when {
                    files.isEmpty() -> null
                    preferredFilePath != null -> files.firstOrNull { it.path == preferredFilePath }
                        ?: files.first()

                    else -> files.first()
                }
                val logLines = selectedFile?.let { readSulogFile(it.path) }.orEmpty()
                val entries = parseSulogLines(logLines)
                val currentState = _uiState.value
                currentCoroutineContext().ensureActive()
                entriesFlow.value = entries
                SulogUiState(
                    isLoading = false,
                    sulogStatus = sulogStatus,
                    isSulogEnabled = isSulogEnabled,
                    searchText = currentState.searchText,
                    selectedFilters = currentState.selectedFilters,
                    files = files,
                    selectedFilePath = selectedFile?.path,
                    entries = entries,
                    visibleEntries = currentState.visibleEntries,
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    throw error
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun refreshLatest() {
        refresh(preferredFilePath = null)
    }

    fun enableSulog() {
        val preferredFilePath = _uiState.value.selectedFilePath
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (repo.setSulogEnabled(true)) {
                repo.execKsudFeatureSave()
            }
            refresh(preferredFilePath)
        }
    }

    fun cleanFile() {
        val currentState = _uiState.value
        val path = currentState.selectedFilePath ?: return
        val cleanAction = resolveSulogFileCleanAction(currentState.files, path)
        viewModelScope.launch(Dispatchers.IO) {
            when (cleanAction) {
                SulogFileCleanAction.Clear -> cleanSulogFile(path)
                SulogFileCleanAction.Delete -> deleteSulogFile(path)
            }
            refresh(path)
        }
    }

    fun setSearchText(searchText: String) {
        searchTextFlow.value = searchText
        _uiState.update { currentState ->
            currentState.copy(
                searchText = searchText,
            )
        }
    }

    fun toggleFilter(filter: SulogEventFilter) {
        _uiState.update { currentState ->
            val selectedFilters = currentState.selectedFilters.toMutableSet().apply {
                if (!add(filter)) remove(filter)
            }
            selectedFiltersFlow.value = selectedFilters
            repo.update {
                it.copy(suLogFilters = selectedFilters.map { filter -> filter.name }.toSet())
            }
            currentState.copy(
                selectedFilters = selectedFilters,
            )
        }
    }
}
