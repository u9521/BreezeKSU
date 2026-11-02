package me.weishu.kernelsu.ui.screen.module

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.Collator
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.hasMagisk
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo
import me.weishu.kernelsu.data.repository.ModuleRepository
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.PinyinUtil
import me.weishu.kernelsu.ui.util.launchSearchQueryCollector
import okhttp3.Request
import me.weishu.kernelsu.data.ksu.toggleModule as toggleModuleUtil
import me.weishu.kernelsu.data.ksu.undoUninstallModule as undoUninstallModuleUtil
import me.weishu.kernelsu.data.ksu.uninstallModule as uninstallModuleUtil

class ModuleViewModel(
    private val repo: ModuleRepository = ksuApp.moduleRepository,
    private val settingsRepo: SettingsRepository = ksuApp.settingsRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "ModuleViewModel"
    }

    private data class ModuleUpdateSignature(
        val updateJson: String,
        val versionCode: Int,
        val enabled: Boolean,
        val update: Boolean,
        val remove: Boolean
    )

    private data class ModuleUpdateCache(
        val signature: ModuleUpdateSignature,
        val info: ModuleUpdateInfo
    )

    private val _uiState = MutableStateFlow(ModuleUiState())
    val uiState: StateFlow<ModuleUiState> = _uiState.asStateFlow()

    // One-shot UI events (toast/snackbar): buffered Channel, never dropped/duplicated/overwritten
    private val _moduleEvent = Channel<ModuleEffect>(Channel.BUFFERED)
    val moduleEvent: Flow<ModuleEffect> = _moduleEvent.receiveAsFlow()

    private val updateInfoMutex = Mutex()
    private var updateInfoCache: MutableMap<String, ModuleUpdateCache> = mutableMapOf()
    private val updateInfoInFlight = mutableSetOf<String>()
    private val searchQuery = MutableStateFlow("")

    private var fetchJob: Job? = null

    var isNeedRefresh = false
        private set

    init {
        viewModelScope.launchSearchQueryCollector(searchQuery, ::applySearchText)
    }

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun initializePreferences() {
        val settings = settingsRepo.settings.value
        _uiState.update {
            it.copy(
                checkModuleUpdate = settings.checkModuleUpdate,
                descriptionMaxLines = settings.moduleDescriptionMaxLines,
                sortEnabledFirst = settings.moduleSortEnabledFirst,
                sortActionFirst = settings.moduleSortActionFirst,
            )
        }
        updateModuleList()
    }

    fun toggleSortActionFirst() {
        val newValue = !_uiState.value.sortActionFirst
        settingsRepo.update { it.copy(moduleSortActionFirst = newValue) }
        _uiState.update { it.copy(sortActionFirst = newValue) }
        updateModuleList()
    }

    fun toggleSortEnabledFirst() {
        val newValue = !_uiState.value.sortEnabledFirst
        settingsRepo.update { it.copy(moduleSortEnabledFirst = newValue) }
        _uiState.update { it.copy(sortEnabledFirst = newValue) }
        updateModuleList()
    }

    fun refreshEnvironmentState() {
        viewModelScope.launch {
            val magiskInstalled = withContext(Dispatchers.IO) { hasMagisk() }
            val isSafeMode = Natives.isSafeMode
            _uiState.update {
                it.copy(
                    magiskInstalled = magiskInstalled,
                    isSafeMode = isSafeMode,
                )
            }
        }
    }

    /**
     * Single entry point for the search bar.
     *
     * The query is published to [searchQuery], whose debounced collector runs the
     * filter. Expanded/collapsed state lives in Material's `SearchBarState` inside the
     * search bar itself, so nothing here tracks it.
     */
    fun setSearchText(text: String) {
        if (_uiState.value.searchText == text) return
        _uiState.update { it.copy(searchText = text) }
        searchQuery.value = text
    }

    private fun filterModules(modules: List<Module>, text: String): List<Module> {
        if (text.isEmpty()) return emptyList()

        return modules.filter {
            it.id.contains(text, true) || it.name.contains(text, true) ||
                    it.description.contains(text, true) || it.author.contains(text, true) ||
                    PinyinUtil.toPinyin(it.name).contains(text, true)
        }
    }

    private suspend fun applySearchText(text: String) {
        if (text.isEmpty()) {
            updateModuleList()
            return
        }

        val result = withContext(Dispatchers.IO) {
            val state = _uiState.value
            filterModules(state.modules, text).sortedWith(moduleComparator(state))
        }

        _uiState.update { it.copy(searchResults = result) }
    }

    private fun updateModuleList(resort: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val searchText = state.searchText
            val shorted = if (resort || state.moduleList.isEmpty()) {
                state.modules.sortedWith(moduleComparator(state))
            } else {
                // Order-preserving reload: keep order, refresh data, drop removed, append new (no re-sort on toggle/uninstall)
                val byId = state.modules.associateBy { it.id }
                val existingIds = state.moduleList.mapTo(HashSet()) { it.id }
                state.moduleList.mapNotNull { byId[it.id] } + state.modules.filter { it.id !in existingIds }
            }
            val searchResults = filterModules(shorted, searchText)

            _uiState.update {
                it.copy(moduleList = shorted, searchResults = searchResults)
            }
        }
    }

    private fun moduleComparator(state: ModuleUiState): Comparator<Module> {
        return compareBy<Module>(
            {
                val executable = it.hasWebUi || it.hasActionScript
                when {
                    it.metamodule && it.enabled -> 0
                    state.sortEnabledFirst && state.sortActionFirst -> when {
                        it.enabled && executable -> 1
                        it.enabled -> 2
                        executable -> 3
                        else -> 4
                    }

                    state.sortEnabledFirst && !state.sortActionFirst -> if (it.enabled) 1 else 2
                    !state.sortEnabledFirst && state.sortActionFirst -> if (executable) 1 else 2
                    else -> 1
                }
            },
            { if (state.sortEnabledFirst) !it.enabled else 0 },
            { if (state.sortActionFirst) !(it.hasWebUi || it.hasActionScript) else 0 },
        ).thenBy(Collator.getInstance(Locale.getDefault()), Module::id)
    }

    suspend fun loadModuleList(resort: Boolean = true) {
        val parsedModules = withContext(Dispatchers.IO) {
            repo.getModules().getOrElse {
                Log.e(TAG, "fetchModuleList: ", it)
                emptyList()
            }
        }

        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    modules = parsedModules,
                )
            }
            // Trigger recalculation of moduleList
            updateModuleList(resort)
            isNeedRefresh = false
        }
    }

    fun fetchModuleList(checkUpdate: Boolean = false, resort: Boolean = true) {
        fetchJob?.cancel()
        _uiState.update { it.copy(isRefreshing = true) }
        fetchJob = viewModelScope.launch {
            try {
                val start = SystemClock.elapsedRealtime()

                loadModuleList(resort)

                if (checkUpdate) syncModuleUpdateInfo(_uiState.value.modules)

                Log.i(
                    TAG,
                    "load cost: ${SystemClock.elapsedRealtime() - start}, modules: ${_uiState.value.modules}"
                )
            } finally {
                _uiState.update { it.copy(isRefreshing = false, hasLoaded = true) }
            }
        }
    }

    private fun Module.toSignature(): ModuleUpdateSignature {
        return ModuleUpdateSignature(
            updateJson = updateJson,
            versionCode = versionCode,
            enabled = enabled,
            update = update,
            remove = remove
        )
    }

    suspend fun syncModuleUpdateInfo(modules: List<Module>) {
        if (!_uiState.value.checkModuleUpdate) return

        val modulesToFetch = mutableListOf<Triple<String, Module, ModuleUpdateSignature>>()
        val removedIds = mutableSetOf<String>()

        updateInfoMutex.withLock {
            val ids = modules.map { it.id }.toSet()
            updateInfoCache.keys.filter { it !in ids }.forEach { removedId ->
                removedIds += removedId
                updateInfoCache.remove(removedId)
                updateInfoInFlight.remove(removedId)
            }

            modules.forEach { module ->
                val signature = module.toSignature()
                val cached = updateInfoCache[module.id]
                if ((cached == null || cached.signature != signature) && updateInfoInFlight.add(
                        module.id
                    )
                ) {
                    modulesToFetch += Triple(module.id, module, signature)
                }
            }
        }

        val fetchedEntries = coroutineScope {
            modulesToFetch.map { (id, module, signature) ->
                async {
                    val info = withTimeoutOrNull(5_000L.milliseconds) {
                        withContext(Dispatchers.IO) { checkUpdate(module) }
                    } ?: ModuleUpdateInfo.Empty
                    id to ModuleUpdateCache(signature, info)
                }
            }.awaitAll()
        }

        val changedEntries = mutableListOf<Pair<String, ModuleUpdateInfo>>()
        updateInfoMutex.withLock {
            fetchedEntries.forEach { (id, entry) ->
                val existing = updateInfoCache[id]
                if (existing == null || existing.signature != entry.signature || existing.info != entry.info) {
                    updateInfoCache[id] = entry
                    changedEntries += id to entry.info
                }
                updateInfoInFlight.remove(id)
            }
        }

        if (removedIds.isEmpty() && changedEntries.isEmpty()) {
            return
        }

        withContext(Dispatchers.Main) {
            _uiState.update { state ->
                val newMap = state.updateInfo.toMutableMap()
                removedIds.forEach { newMap.remove(it) }
                changedEntries.forEach { (id, info) ->
                    newMap[id] = info
                }
                state.copy(updateInfo = newMap)
            }
        }
    }

    fun requestUpdateConfirmation(module: Module, updateInfo: ModuleUpdateInfo) {
        // Open in loading state first so the network wait has immediate feedback inside the dialog.
        val pending = ModuleConfirmDialogState(
            request = ModuleConfirmRequest.Update(
                module = module,
                downloadUrl = updateInfo.downloadUrl,
                fileName = "${module.name}-${updateInfo.version}.zip",
                updateInfo = updateInfo,
            ),
            title = ksuApp.resources.getString(R.string.module_changelog),
            content = null,
            markdown = false,
            confirm = ksuApp.resources.getString(R.string.module_update),
            changelogLoading = true,
        )
        _uiState.update { it.copy(confirmDialogState = pending) }

        viewModelScope.launch {
            val dialogState = buildUpdateConfirmDialogState(module, updateInfo)
            // Drop the result if the dialog was dismissed or replaced while the fetch was in flight.
            _uiState.update {
                if (it.confirmDialogState == pending) it.copy(confirmDialogState = dialogState) else it
            }
        }
    }

    fun requestUninstallConfirmation(module: Module) {
        val res = ksuApp.resources
        _uiState.update {
            it.copy(
                confirmDialogState = ModuleConfirmDialogState(
                    request = ModuleConfirmRequest.Uninstall(module),
                    title = res.getString(R.string.module),
                    content = (if (module.metamodule) res.getString(R.string.metamodule_uninstall_confirm) else res.getString(
                        R.string.module_uninstall_confirm
                    )).format(
                        module.name
                    ),
                    confirm = res.getString(R.string.uninstall),
                    dismiss = res.getString(android.R.string.cancel),
                )
            )
        }
    }

    fun dismissConfirmRequest() {
        _uiState.update { it.copy(confirmDialogState = null) }
    }

    fun emitEffect(effect: ModuleEffect) {
        _moduleEvent.trySend(effect)
    }

    fun toggleModule(module: Module) {
        viewModelScope.launch {
            val res = ksuApp.resources
            val success = withContext(Dispatchers.IO) {
                toggleModuleUtil(module.id, !module.enabled)
            }
            if (success) {
                fetchModuleList(checkUpdate = true, resort = false)
                emitEffect(ModuleEffect.SnackBar(res.getString(R.string.reboot_to_apply)))
            } else {
                val message =
                    if (module.enabled) R.string.module_failed_to_disable else R.string.module_failed_to_enable
                emitEffect(ModuleEffect.SnackBar(res.getString(message).format(module.name)))
            }
        }
    }

    fun uninstallModule(module: Module) {
        viewModelScope.launch {
            val res = ksuApp.resources
            val success = withContext(Dispatchers.IO) {
                uninstallModuleUtil(module.id)
            }
            if (success) {
                fetchModuleList(checkUpdate = true, resort = false)
            }
            _uiState.update { it.copy(confirmDialogState = null) }
            emitEffect(
                ModuleEffect.SnackBar(
                    res.getString(
                        if (success) R.string.module_uninstall_success else R.string.module_uninstall_failed
                    ).format(module.name)
                )
            )
        }
    }

    fun undoUninstallModule(module: Module) {
        viewModelScope.launch {
            val res = ksuApp.resources
            val success = withContext(Dispatchers.IO) {
                undoUninstallModuleUtil(module.id)
            }
            if (success) {
                fetchModuleList(checkUpdate = true, resort = false)
            }
            emitEffect(
                ModuleEffect.SnackBar(
                    res.getString(
                        if (success) R.string.module_undo_uninstall_success else R.string.module_undo_uninstall_failed
                    ).format(module.name)
                )
            )
        }
    }

    /** Outcome of fetching a module's changelog on demand. */
    private sealed interface ChangelogResult {
        data object NotConfigured : ChangelogResult
        data object Empty : ChangelogResult
        data class Loaded(val markdown: String) : ChangelogResult
        data class Failed(val message: String?) : ChangelogResult
    }

    private suspend fun fetchChangelog(url: ModuleUpdateInfo.ChangelogUrl): ChangelogResult {
        val configured =
            url as? ModuleUpdateInfo.ChangelogUrl.Configured ?: return ChangelogResult.NotConfigured
        return withContext(Dispatchers.IO) {
            runCatching {
                ksuApp.okhttpClient.newCall(
                    Request.Builder().url(configured.url).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@use ChangelogResult.Failed(response.message)
                    val text = response.body.string()
                    if (text.isBlank()) ChangelogResult.Empty else ChangelogResult.Loaded(text)
                }
            }.getOrElse { ChangelogResult.Failed(it.message) }
        }
    }

    private suspend fun buildUpdateConfirmDialogState(
        module: Module,
        updateInfo: ModuleUpdateInfo,
    ): ModuleConfirmDialogState {
        val res = ksuApp.resources
        return when (val changelog = fetchChangelog(updateInfo.changelog)) {
            is ChangelogResult.Loaded -> ModuleConfirmDialogState(
                request = ModuleConfirmRequest.Update(
                    module = module,
                    downloadUrl = updateInfo.downloadUrl,
                    fileName = "${module.name}-${updateInfo.version}.zip",
                    updateInfo = updateInfo,
                ),
                title = res.getString(R.string.module_changelog),
                content = changelog.markdown,
                markdown = true,
                confirm = res.getString(R.string.module_update),
            )

            // Neither outcome is retryable, so keep them as distinct flags rather than offering retry.
            ChangelogResult.NotConfigured, ChangelogResult.Empty -> {
                val notConfigured = changelog == ChangelogResult.NotConfigured
                ModuleConfirmDialogState(
                    request = ModuleConfirmRequest.Update(
                        module = module,
                        downloadUrl = updateInfo.downloadUrl,
                        fileName = "${module.name}-${updateInfo.version}.zip",
                        updateInfo = updateInfo,
                    ),
                    title = res.getString(R.string.module_changelog),
                    content = res.getString(
                        if (notConfigured) {
                            R.string.module_changelog_not_configured
                        } else {
                            R.string.module_changelog_empty
                        }
                    ),
                    markdown = false,
                    confirm = res.getString(R.string.module_update),
                    changelogNotConfigured = notConfigured,
                    changelogEmpty = !notConfigured,
                )
            }

            // A failed changelog offers retry without blocking the update itself.
            is ChangelogResult.Failed -> ModuleConfirmDialogState(
                request = ModuleConfirmRequest.Update(
                    module = module,
                    downloadUrl = updateInfo.downloadUrl,
                    fileName = "${module.name}-${updateInfo.version}.zip",
                    updateInfo = updateInfo,
                ),
                title = res.getString(R.string.module_changelog),
                content = res.getString(R.string.module_changelog_load_failed),
                markdown = false,
                confirm = res.getString(R.string.module_update),
                changelogFailed = true,
            )
        }
    }

    private suspend fun checkUpdate(m: Module): ModuleUpdateInfo {
        return repo.checkUpdate(m).getOrDefault(ModuleUpdateInfo.Empty)
    }
}
