package me.weishu.kernelsu.ui.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.component.breeze.bar.SearchAppBarBreeze
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.util.onlyHorizontal
import me.weishu.kernelsu.ui.util.topBarHazeEffect
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.BreezePreviewTheme

private val sampleItems = List(30) { "Result item #${it + 1}" }

private data class PageItem(val title: String, val subtitle: String, val icon: ImageVector)

private val pageItems = listOf(
    PageItem("Getting Started", "Learn the basics of the app", MaterialSymbols.Filled.Rounded.Info),
    PageItem("Configuration", "Adjust your preferences and settings", MaterialSymbols.Filled.Rounded.Settings),
    PageItem("Favorites", "View and manage your saved items", MaterialSymbols.Filled.Rounded.Star),
    PageItem("Advanced Features", "Explore power-user capabilities", MaterialSymbols.Filled.Rounded.Info),
    PageItem("Appearance", "Customize themes, colors, and layout", MaterialSymbols.Filled.Rounded.AccountCircle),
    PageItem("Notifications", "Manage alert and push notification settings", MaterialSymbols.Filled.Rounded.Notifications),
    PageItem("Security & Privacy", "Configure permissions and encryption", MaterialSymbols.Filled.Rounded.Lock),
    PageItem("Storage", "View disk usage and clear cached data", MaterialSymbols.Filled.Rounded.Delete),
    PageItem("Developer Options", "Access debugging and experimental tools", MaterialSymbols.Filled.Rounded.Build),
    PageItem("Search Index", "Manage full-text search and indexing", MaterialSymbols.Filled.Rounded.Search),
    PageItem("Favorites Sync", "Sync saved items across devices", MaterialSymbols.Filled.Rounded.Favorite),
    PageItem("System Health", "View performance metrics and diagnostics", MaterialSymbols.Filled.Rounded.Info),
)

@Composable
fun SearchBarBreezePreviewImpl() {
    var searchText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()
    var selectedItem by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val searchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(
        scrollState = rememberSearchBarScrollState(initialScrollOffsetLimit = 0f),
    )
    Scaffold(
        topBar = {
            SearchAppBarBreeze(
                modifier = Modifier.topBarHazeEffect(hazeState, scrollBehavior),
                title = { Text("Search Demo") },
                subtitle = { Text("Search subtitle") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(MaterialSymbols.Filled.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onClearClick = { searchText = "" },
                scrollBehavior = scrollBehavior,
                searchBarScrollBehavior = searchBarScrollBehavior,
                snackbarHostState = snackbarHostState,
                hazeState = hazeState,
                searchContent = { bottomPadding, _ ->
                    SearchResults(
                        query = searchText,
                        bottomPadding = bottomPadding,
                        onItemClick = { item ->
                            selectedItem = item
                            scope.launch {
                                snackbarHostState.showSnackbar("Selected: $item")
                            }
                        }
                    )
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .padding(paddingValues.onlyHorizontal())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp), contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
        ) {
            item {
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(pageItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        modifier = Modifier,
                        leadingContent = {
                            Icon(item.icon, contentDescription = null)
                        },
                        supportingContent = { Text(item.subtitle) },
                        content = { Text(item.title) },
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap a search result to see the blur dialog + snackbar. Scroll the list to collapse the top bar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Blur dialog on search result tap
    selectedItem?.let { item ->
        BreezeAlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text("Result Detail") },
            text = {
                Column {
                    Text("You selected:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(item, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This dialog has background blur via the overlay window. Tap outside or press Dismiss to close.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Confirmed: $item")
                    }
                    selectedItem = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text("Dismiss")
                }
            },
        )
    }
}


@Composable
private fun SearchResults(
    query: String,
    bottomPadding: Dp,
    onItemClick: (String) -> Unit,
) {
    val filtered = if (query.isBlank()) {
        sampleItems
    } else {
        sampleItems.filter { it.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        items(filtered) { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    "No results for \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSearchBarBreeze() {
    BreezePreviewTheme {
        CompositionLocalProvider(
            LocalEnableBlur provides true,
        ) {
            SearchBarBreezePreviewImpl()
        }
    }
}
