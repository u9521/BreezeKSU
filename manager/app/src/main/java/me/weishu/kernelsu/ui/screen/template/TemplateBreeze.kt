package me.weishu.kernelsu.ui.screen.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.TemplateInfo
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.BreezeVerticalScrollbar
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffoldScope
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.PopupFeedBack
import me.weishu.kernelsu.ui.util.keyDownFeedBack

@Composable
fun AppProfileTemplateScreenBreeze(
    state: TemplateUiState,
    actions: TemplateActions,
) {
    val listState = rememberLazyListState()

    BreezeScaffold(
        message = state.message,
        onMessageShown = actions.onMessageShown,
        topBar = {
            TopBar(
                onBack = actions.onBack,
                onRefresh = { actions.onRefresh(true) },
                onImport = actions.onImport,
                onExport = actions.onExport,
            )
        },
        floatingActionButton = {
            NewTemplateFab(onClick = actions.onCreateTemplate)
        },
        fabLift = true,
    ) { innerPadding ->
        val showListScrollbar by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
        }
        val templates = state.templateList

        if (templates.isEmpty() && !state.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (state.offline) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.network_offline),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { actions.onRefresh(false) }) {
                            Text(stringResource(R.string.network_retry))
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No templates found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { actions.onRefresh(true) }) {
                            Text(stringResource(R.string.network_retry))
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp + innerPadding.calculateTopPadding(),
                        bottom = 16.dp + 56.dp + 12.dp + 48.dp + 12.dp + innerPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(
                        items = templates,
                        key = { _, template -> template.id },
                    ) { index, template ->
                        TemplateItem(
                            modifier = Modifier.animateItem(),
                            template = template,
                            index = index,
                            count = templates.size,
                            onClick = { actions.onOpenTemplate(template) },
                        )
                    }
                }

                BreezeVerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
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
private fun NewTemplateFab(
    onClick: () -> Unit,
) {
    val keyFeedback = keyDownFeedBack()
    ExtendedFloatingActionButton(
        onClick = {
            keyFeedback()
            onClick()
        },
        icon = { Icon(MaterialSymbols.Filled.Rounded.Add, null) },
        text = { Text(stringResource(id = R.string.app_profile_template_create)) },
    )
}

@Composable
private fun TemplateItem(
    modifier: Modifier,
    template: TemplateInfo,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count)
            .let { it.copy(pressedShape = it.shape) },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        content = {
            Text(
                text = template.name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = "${template.id}${if (template.author.isEmpty()) "" else "@${template.author}"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = template.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow {
                    StatusTagBreeze(
                        label = "UID: ${template.uid}",
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                    )
                    StatusTagBreeze(
                        label = "GID: ${template.gid}",
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                    StatusTagBreeze(
                        label = template.context,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    )
                    if (template.local) {
                        StatusTagBreeze(
                            label = "local",
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        StatusTagBreeze(
                            label = "remote",
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BreezeScaffoldScope.TopBar(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    BreezeTopAppBar(
        title = { Text(stringResource(R.string.settings_profile_template)) },
        navigationIcon = {
            BreezeBackButton(
                onClick = onBack,
                collapseFraction = scrollBehavior.state.collapsedFraction,
            )
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = MaterialSymbols.Filled.Rounded.Sync,
                    contentDescription = null,
                )
            }
            ImportExportMenuButton(
                onImport = onImport,
                onExport = onExport,
            )
        },
    )
}

@Composable
private fun ImportExportMenuButton(
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    val showDropdown = remember { mutableStateOf(false) }

    IconButton(onClick = { showDropdown.value = true }) {
        Icon(
            imageVector = MaterialSymbols.Rounded.SwapVert,
            contentDescription = stringResource(id = R.string.app_profile_import_export),
        )

        DropdownMenuPopup(
            expanded = showDropdown.value,
            onDismissRequest = { showDropdown.value = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                SelectableDropdownMenuItem(
                    selected = false,
                    shapes = MenuDefaults.itemShape(index = 0, count = 2),
                    text = { Text(stringResource(id = R.string.app_profile_import_from_clipboard)) },
                    onClick = {
                        onImport()
                        showDropdown.value = false
                    },
                )
                SelectableDropdownMenuItem(
                    selected = false,
                    shapes = MenuDefaults.itemShape(index = 1, count = 2),
                    text = { Text(stringResource(id = R.string.app_profile_export_to_clipboard)) },
                    onClick = {
                        onExport()
                        showDropdown.value = false
                    },
                )
            }
            PopupFeedBack()
        }
    }
}
