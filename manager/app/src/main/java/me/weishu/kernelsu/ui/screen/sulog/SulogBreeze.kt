package me.weishu.kernelsu.ui.screen.sulog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.SulogEntry
import me.weishu.kernelsu.data.ksu.SulogEventFilter
import me.weishu.kernelsu.ui.component.breeze.bar.SearchAppBarBreeze
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.expressive.BreezePullToRefresh
import me.weishu.kernelsu.ui.component.breeze.expressive.TonalCard
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.BreezeVerticalScrollbar
import me.weishu.kernelsu.ui.component.breeze.menu.BrMenuBox
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.PopupFeedBack


@Composable
fun SulogScreenBreeze(
    state: SulogUiState,
    actions: SulogActions,
) {
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedEntry by remember { mutableStateOf<SulogEntry?>(null) }
    var localSearchText by remember { mutableStateOf(state.searchText) }

    LaunchedEffect(state.searchText) {
        localSearchText = state.searchText
    }

    SulogDetailDialog(
        entry = selectedEntry,
        onDismiss = { selectedEntry = null },
    )

    BreezeScaffold(
        topBar = {
            val showListScrollbar by remember(scrollBehavior) {
                derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
            }
            SearchAppBarBreeze(
                title = { Text(stringResource(R.string.settings_sulog)) },
                searchText = localSearchText,
                onSearchTextChange = {
                    localSearchText = it
                    actions.onSearchTextChange(it)
                    scope.launch { searchListState.scrollToItem(0) }
                },
                onClearClick = {
                    localSearchText = ""
                    actions.onSearchTextChange("")
                },
                navigationIcon = {
                    BreezeBackButton(
                        onClick = actions.onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
                actions = {
                    IconButton(onClick = actions.onCleanFile) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.DeleteSweep,
                            contentDescription = stringResource(R.string.sulog_clean_title),
                        )
                    }
                    FilterMenu(state, actions)
                },
                searchContent = { bottomPadding, _ ->
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp + bottomPadding,
                        ),
                    ) {
                        suLogEntriesSection(
                            entries = state.visibleEntries,
                            errorMessage = state.errorMessage,
                            onEntryClick = { selectedEntry = it },
                        )
                    }
                    BreezeVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(bottom = bottomPadding),
                        listState = searchListState,
                        durationMillis = 1500L,
                        visible = showListScrollbar,
                    )
                },
            )
        },
    ) { innerPadding ->
        val showListScrollbar by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
        }
        BreezePullToRefresh(
            isRefreshing = state.isLoading || state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicatorModifier = Modifier.padding(top = innerPadding.calculateTopPadding() + 8.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp + innerPadding.calculateTopPadding(),
                        bottom = 16.dp + innerPadding.calculateBottomPadding(),
                    ),
                ) {
                    item {
                        SulogStatusSection(state, actions)
                    }

                    item {
                        LogTimeCard(state, actions)
                    }

                    suLogEntriesSection(
                        entries = state.visibleEntries,
                        errorMessage = state.errorMessage,
                        onEntryClick = { selectedEntry = it },
                    )
                }

                BreezeVerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                    listState = listState,
                    durationMillis = 1500L,
                    visible = showListScrollbar,
                )
            }
        }
    }
}

@Composable
private fun FilterMenu(
    state: SulogUiState,
    actions: SulogActions,
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showFilterMenu = true }) {
        Icon(
            imageVector = MaterialSymbols.Rounded.FilterList,
            contentDescription = stringResource(R.string.sulog_filter_title),
        )
    }
    DropdownMenuPopup(
        expanded = showFilterMenu,
        onDismissRequest = { showFilterMenu = false },
    ) {
        PopupFeedBack()
        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
            SulogEventFilter.entries.forEachIndexed { index, filter ->
                CheckableDropdownMenuItem(
                    text = { Text(sulogFilterLabel(filter)) },
                    checked = filter in state.selectedFilters,
                    checkedLeadingIcon = {
                        Icon(
                            MaterialSymbols.Filled.Rounded.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        actions.onToggleFilter(filter)
                    },
                    shapes = MenuDefaults.itemShape(
                        index = index,
                        count = SulogEventFilter.entries.size
                    )
                )
            }
        }
    }
}

private fun LazyListScope.suLogEntriesSection(
    entries: List<SulogEntry>,
    errorMessage: String?,
    onEntryClick: (SulogEntry) -> Unit,
) {
    when {
        errorMessage != null -> item {
            SulogMessageCard(
                modifier = Modifier.fillParentMaxSize(),
                title = stringResource(R.string.sulog_failed_to_load),
                summary = errorMessage,
            )
        }

        else -> {
            itemsIndexed(entries, key = { index, entry -> "$index-${entry.key}" }) { index, entry ->
                SulogItem(Modifier.animateItem(), index, entries.size, onEntryClick, entry)
            }
        }
    }
}

@Composable
private fun SulogItem(
    modifier: Modifier = Modifier,
    index: Int,
    count: Int,
    onEntryClick: (SulogEntry) -> Unit,
    entry: SulogEntry
) {
    SegmentedListItem(
        modifier = modifier,
        shapes = ListItemDefaults.segmentedShapes(index, count)
            .let { it.copy(pressedShape = it.shape) },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        onClick = { onEntryClick(entry) },
        content = { Text(sulogEntryTitle(entry)) },
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sulogEntryDescription(entry)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                entry.timestampText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val colors = listOf(
                        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary,
                        MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary,
                    )
                    sulogEntrySummaryTags(entry).forEachIndexed { index, tag ->
                        val (bg, fg) = colors.getOrElse(index) { colors.last() }
                        StatusTagBreeze(label = tag, backgroundColor = bg, contentColor = fg)
                    }
                }
            }
        },
        trailingContent = {
            sulogEntryStatus(entry)?.let { Text(it) }
        },
    )
    if (index < count) Spacer(Modifier.padding(bottom = 2.dp))
}

@Composable
private fun SulogStatusSection(
    state: SulogUiState,
    actions: SulogActions,
) {
    when (state.sulogStatus) {
        "unsupported" -> {
            WarningCard(text = stringResource(R.string.sulog_unsupported_title))
        }

        "managed" -> {
            WarningCard(text = stringResource(R.string.feature_status_managed_summary))
        }

        "supported" if !state.isSulogEnabled -> {
            WarningCard(
                text = stringResource(R.string.sulog_disabled_title),
                action = {
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = actions.onEnableSulog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.sulog_enable_action))
                    }
                },
            )
        }

        else -> Unit
    }
}


@Composable
private fun LogTimeCard(
    state: SulogUiState,
    actions: SulogActions,
) {
    val fileSelector = buildSulogFileSelector(state.files, state.selectedFilePath)
    BrMenuBox(
        modifier = Modifier
            .padding(bottom = 16.dp, top = 8.dp)
            .clip(MaterialTheme.shapes.large),
        enabled = fileSelector.items.isNotEmpty(),
        menuContent = { dismiss ->
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                fileSelector.items.forEachIndexed { index, string ->
                    SelectableDropdownMenuItem(
                        text = { Text(string) },
                        selected = fileSelector.selectedIndex == index,
                        selectedLeadingIcon = {
                            Icon(
                                MaterialSymbols.Filled.Rounded.Check,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            state.files.getOrNull(index)?.let { file ->
                                actions.onSelectFile(file.path)
                                dismiss()
                            }
                        },
                        shapes = MenuDefaults.itemShape(index, fileSelector.items.size)
                    )
                }
            }
        },
        content = {
            SegmentedListItem(
                onClick = {},
                shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.large),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                trailingContent = {
                    Text(
                        text = fileSelector.items.getOrNull(fileSelector.selectedIndex) ?: "",
                        color = MaterialTheme.colorScheme.primary
                    )
                }) { Text(stringResource(R.string.sulog_log_files)) }
        },
    )
}

@Composable
private fun SulogMessageCard(
    modifier: Modifier,
    title: String,
    summary: String? = null,
) {
    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (summary != null) {
                Text(
                    summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WarningCard(
    text: String,
    action: (@Composable () -> Unit)? = null,
) {
    TonalCard(
        modifier = Modifier.padding(bottom = 16.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            action?.invoke()
        }
    }
}

@Composable
private fun SulogDetailDialog(
    entry: SulogEntry?,
    onDismiss: () -> Unit,
) {
    // Held while the exit animation plays: the caller closes the dialog by clearing the entry.
    var lastEntry by remember { mutableStateOf(entry) }
    if (entry != null) lastEntry = entry
    val shown = entry ?: lastEntry ?: return

    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = entry != null,
        title = { Text(sulogEntryTitle(shown)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = sulogEntryDetailText(shown),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
