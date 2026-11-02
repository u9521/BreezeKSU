package me.weishu.kernelsu.ui.util

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MILLIS = 150L

@OptIn(FlowPreview::class)
fun CoroutineScope.launchSearchQueryCollector(
    searchQuery: StateFlow<String>,
    onQuery: suspend (String) -> Unit,
): Job {
    return launch {
        searchQuery
            .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)
            .distinctUntilChanged()
            .collectLatest(onQuery)
    }
}
