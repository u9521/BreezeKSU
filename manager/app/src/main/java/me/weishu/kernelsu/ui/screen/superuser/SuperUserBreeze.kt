package me.weishu.kernelsu.ui.screen.superuser

import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.ownerNameForUid
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.ui.component.breeze.bar.SearchAppBarBreeze
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.expressive.BreezePullToRefresh
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.BreezeVerticalScrollbar
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.common.AppIconImage
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.util.LocalHasDetailPane
import me.weishu.kernelsu.ui.util.PopupFeedBack
import me.weishu.kernelsu.ui.util.keyDownFeedBack

@Composable
fun SuperUserPagerBreeze(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    isActiveTab: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalBreezeNavigator.current
    val onBack = if (LocalHasDetailPane.current) {
        dropUnlessResumed { navigator.pop() }
    } else {
        null
    }
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val expandedUids = remember { mutableStateOf(setOf<Int>()) }

    val haptic = LocalHapticFeedback.current

    BreezeScaffold(
        topBar = {
            val showListScrollbar by remember(scrollBehavior) {
                derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
            }
            SearchAppBarBreeze(
                title = { Text(stringResource(R.string.superuser)) },
                searchText = uiState.searchText,
                navigationIcon = {
                    Row {
                        BackIconButton(onBack, scrollBehavior)
                        IconButton(onClick = actions.onOpenSulog) {
                            Icon(
                                imageVector = MaterialSymbols.Filled.Rounded.Article,
                                contentDescription = stringResource(R.string.settings_sulog),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                onSearchTextChange = {
                    actions.onSearchTextChange(it)
                    scope.launch { listState.scrollToItem(0) }
                },
                onClearClick = actions.onClearSearch,
                actions = {
                    SortMenu(uiState, actions)
                    FilterMenu(uiState, actions)
                },
                isActiveTab = isActiveTab,
                searchContent = { bottomPadding, closeSearch ->
                    LaunchedEffect(uiState.searchText) {
                        searchListState.scrollToItem(0)
                    }
                    GroupedAppList(
                        groups = uiState.searchResults,
                        listState = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp + bottomPadding
                        ),
                        expandGroups = true,
                        onGroupClick = { group ->
                            closeSearch()
                            actions.onOpenProfile(group)
                        },
                        onAppClick = { group, _ ->
                            closeSearch()
                            actions.onOpenProfile(group)
                        },
                    )
                    BreezeVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(bottom = bottomPadding),
                        listState = searchListState,
                        durationMillis = 1500L,
                        visible = showListScrollbar,
                    )
                }
            )
        },
    ) { innerPadding ->
        val showListScrollbar by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
        }
        BreezePullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                actions.onRefresh()
            },
            modifier = Modifier.fillMaxSize(),
            indicatorModifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GroupedAppList(
                    groups = uiState.groupedApps,
                    listState = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp + innerPadding.calculateTopPadding(),
                        bottom = 16.dp + innerPadding.calculateBottomPadding(),
                    ),
                    expandedUids = expandedUids.value,
                    onToggleExpand = { uid ->
                        expandedUids.value = if (expandedUids.value.contains(uid)) {
                            expandedUids.value - uid
                        } else {
                            expandedUids.value + uid
                        }
                    },
                    onGroupClick = actions.onOpenProfile,
                    onAppClick = { group, _ -> actions.onOpenProfile(group) },
                )
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
private fun GroupedAppList(
    groups: List<GroupedApps>,
    listState: LazyListState,
    modifier: Modifier,
    contentPadding: PaddingValues,
    expandedUids: Set<Int> = emptySet(),
    expandGroups: Boolean = false,
    onToggleExpand: ((Int) -> Unit)? = null,
    onGroupClick: (GroupedApps) -> Unit,
    onAppClick: (GroupedApps, AppInfo) -> Unit,
) {
    // lock auto scroll
    remember(groups) {
        val currentIndex = Snapshot.withoutReadObservation { listState.firstVisibleItemIndex }
        val currentOffset =
            Snapshot.withoutReadObservation { listState.firstVisibleItemScrollOffset }
        listState.requestScrollToItem(currentIndex, currentOffset)
        null
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(
            items = groups,
            key = { _, group -> group.uid },
        ) { index, group ->
            val isExpanded =
                (expandGroups || expandedUids.contains(group.uid)) && group.apps.size > 1
            val previousExpanded = if (index > 0) {
                val previous = groups[index - 1]
                (expandGroups || expandedUids.contains(previous.uid)) && previous.apps.size > 1
            } else {
                true
            }
            val nextExpanded = if (index < groups.lastIndex) {
                val next = groups[index + 1]
                (expandGroups || expandedUids.contains(next.uid)) && next.apps.size > 1
            } else {
                true
            }

            val targetTopRadius = if (isExpanded || previousExpanded) 16.dp else 4.dp
            val targetBottomRadius = if (isExpanded) 4.dp else if (nextExpanded) 16.dp else 4.dp
            val targetTopPadding = if (index == 0) 0.dp else if (isExpanded || previousExpanded) {
                16.dp
            } else {
                ListItemDefaults.SegmentedGap
            }

            val animatedTopRadius by animateDpAsState(
                targetValue = targetTopRadius,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "TopCorner",
            )
            val animatedBottomRadius by animateDpAsState(
                targetValue = targetBottomRadius,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "BottomCorner",
            )
            val animatedTopPadding by animateDpAsState(
                targetValue = targetTopPadding,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "TopPadding",
            )

            val itemShape = RoundedCornerShape(
                topStart = animatedTopRadius,
                topEnd = animatedTopRadius,
                bottomStart = animatedBottomRadius,
                bottomEnd = animatedBottomRadius,
            )

            Column(
                modifier = Modifier
                    .padding(top = animatedTopPadding.coerceAtLeast(0.dp))
                    .animateItem()
            ) {
                GroupItem(
                    group = group,
                    shape = itemShape,
                    onToggleExpand = if (group.apps.size > 1 && !expandGroups) {
                        { onToggleExpand?.invoke(group.uid) }
                    } else {
                        null
                    },
                ) { onGroupClick(group) }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            fadeIn(MaterialTheme.motionScheme.slowSpatialSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    Column(
                        modifier = Modifier.padding(top = ListItemDefaults.SegmentedGap),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        group.apps.forEachIndexed { appIndex, app ->
                            val shapes = ListItemDefaults.segmentedShapes(
                                index = appIndex + 1,
                                count = group.apps.size + 1,
                            )
                            SimpleAppItem(
                                app = app,
                                matched = group.matchedIdentifiers.contains(app.displayIdentifier),
                                shape = shapes.shape,
                            ) {
                                onAppClick(group, app)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconItem(
    title: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
    shapes: ListItemShapes,
    packageInfo: android.content.pm.PackageInfo,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    colors: androidx.compose.material3.ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        verticalAlignment = Alignment.CenterVertically,
        colors = colors,
        shapes = shapes,
        leadingContent = {
            AppIconImage(
                packageInfo = packageInfo,
                label = packageInfo.packageName,
                modifier = Modifier
                    .padding(4.dp)
                    .size(iconSize)
            )
        },
        content = title,
        supportingContent = supportingContent,
    )
}

@Composable
private fun SimpleAppItem(
    app: AppInfo,
    matched: Boolean = false,
    shape: Shape = RoundedCornerShape(0.dp),
    onNavigate: () -> Unit,
) {
    val colors = if (matched) {
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            supportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
        )
    } else {
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    AppIconItem(
        title = { Text(app.label, overflow = TextOverflow.Ellipsis, maxLines = 1) },
        supportingContent = {
            Text(
                app.displayIdentifier,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        shapes = ListItemDefaults.shapes(shape = shape, pressedShape = shape),
        packageInfo = app.packageInfo,
        iconSize = 40.dp,
        colors = colors,
        onClick = onNavigate,
    )
}

@Composable
private fun GroupItem(
    group: GroupedApps,
    shape: Shape,
    onToggleExpand: (() -> Unit)?,
    onClickPrimary: () -> Unit,
) {
    val isGroup = group.apps.size > 1
    val title = if (isGroup) {
        "${group.ownerName ?: ownerNameForUid(group.uid)} (${group.uid})"
    } else {
        group.primary.label
    }
    val summaryText = if (isGroup) {
        stringResource(R.string.group_contains_apps, group.apps.size)
    } else {
        group.primary.displayIdentifier
    }
    SegmentedListItem(
        onClick = onClickPrimary,
        onLongClick = onToggleExpand,
        verticalAlignment = Alignment.CenterVertically,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shapes = ListItemDefaults.shapes(shape = shape, pressedShape = shape),
        content = {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = summaryText,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                FlowRow {
                    val userId = group.uid / 100000
                    val packageInfo = group.primary.packageInfo
                    val applicationFlags = packageInfo.applicationInfo?.flags ?: 0

                    if (userId != 0) {
                        StatusTagBreeze(
                            label = "USER $userId",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (group.anyAllowSu) {
                        StatusTagBreeze(
                            label = "ROOT",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    } else if (group.shouldUmount) {
                        StatusTagBreeze(
                            label = "UMOUNT",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                    if (group.anyCustom) {
                        StatusTagBreeze(
                            label = "CUSTOM",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    if (applicationFlags.and(ApplicationInfo.FLAG_SYSTEM) != 0
                        || applicationFlags.and(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    ) {
                        StatusTagBreeze(
                            label = "SYSTEM",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            backgroundColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (!packageInfo.sharedUserId.isNullOrEmpty()) {
                        StatusTagBreeze(
                            label = "SHARED UID",
                            modifier = Modifier.padding(top = 4.dp),
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            backgroundColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        },
        leadingContent = {
            AppIconImage(
                packageInfo = group.primary.packageInfo,
                label = group.primary.label,
                modifier = Modifier.size(48.dp)
            )
        },
    )
}

@Composable
private fun BackIconButton(onBack: (() -> Unit)?, scrollBehavior: TopAppBarScrollBehavior) {
    AnimatedVisibility(
        visible = onBack != null,
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                ),
        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                ),
    ) {
        BreezeBackButton(
            onClick = onBack ?: {},
            collapseFraction = scrollBehavior.state.collapsedFraction,
        )
    }
}


@Composable
private fun FilterMenu(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
) {
    var showDropdown by remember { mutableStateOf(false) }

    IconButton(onClick = { showDropdown = true }) {
        Icon(
            imageVector = MaterialSymbols.Filled.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.settings)
        )

        DropdownMenuPopup(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                CheckableDropdownMenuItem(
                    text = { Text(stringResource(R.string.show_system_apps)) },
                    checked = uiState.showSystemApps,
                    checkedLeadingIcon = {
                        Icon(
                            MaterialSymbols.Filled.Rounded.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        actions.onToggleShowSystemApps()
                    },
                    shapes = MenuDefaults.itemShape(index = 0, count = 2)
                )
                if (uiState.userIds.size > 1) {
                    CheckableDropdownMenuItem(
                        text = { Text(stringResource(R.string.show_only_primary_user_apps)) },
                        checked = uiState.showOnlyPrimaryUserApps,
                        checkedLeadingIcon = {
                            Icon(
                                MaterialSymbols.Filled.Rounded.Check,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        onCheckedChange = {
                            actions.onToggleShowOnlyPrimaryUserApps()
                        },
                        shapes = MenuDefaults.itemShape(index = 1, count = 2)
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val keydownFB = keyDownFeedBack()
    val sortEntries = listOf(
        AppSortType.NAME to R.string.sort_by_name,
        AppSortType.PACKAGE_NAME to R.string.sort_by_package_name,
        AppSortType.INSTALL_TIME to R.string.sort_by_install_time,
        AppSortType.UPDATE_TIME to R.string.sort_by_update_time,
    )
    val sortConfig = uiState.sortConfig
    IconButton(onClick = { showSortMenu = true }) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Sort,
            contentDescription = stringResource(R.string.menu_sort)
        )

        DropdownMenuPopup(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            PopupFeedBack()
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                sortEntries.onEachIndexed { index, (type, resId) ->
                    SelectableDropdownMenuItem(
                        text = { Text(stringResource(resId)) },
                        selected = sortConfig.sortType == type,
                        selectedLeadingIcon = {
                            Icon(
                                MaterialSymbols.Filled.Rounded.Check,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            keydownFB()
                            actions.onUpdateSortConfig(sortConfig.withType(type))
                        },
                        shapes = MenuDefaults.itemShape(
                            index = index,
                            count = sortEntries.size
                        )
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                CheckableDropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_reverse)) },
                    checked = sortConfig.reversed,
                    checkedLeadingIcon = {
                        Icon(
                            MaterialSymbols.Filled.Rounded.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        keydownFB()
                        actions.onUpdateSortConfig(sortConfig.toggleReversed())
                    },
                    shapes = MenuDefaults.itemShape(
                        index = 0,
                        count = 1
                    ),
                )
            }
        }
    }
}
