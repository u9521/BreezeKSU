package me.weishu.kernelsu.ui.screen.module

import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.reboot
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo
import me.weishu.kernelsu.data.repository.isSoftRebootPreferred
import me.weishu.kernelsu.ui.component.breeze.bar.SearchAppBarBreeze
import me.weishu.kernelsu.ui.component.breeze.button.ActionButton
import me.weishu.kernelsu.ui.component.breeze.button.ButtonPosition
import me.weishu.kernelsu.ui.component.breeze.button.ButtonSpec
import me.weishu.kernelsu.ui.component.breeze.button.ButtonType
import me.weishu.kernelsu.ui.component.breeze.button.EnumeratedPriorityButtonRow
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.breeze.expressive.BreezePullToRefresh
import me.weishu.kernelsu.ui.component.breeze.expressive.ExpressiveSwitch
import me.weishu.kernelsu.ui.component.breeze.expressive.TonalCard
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.BreezeVerticalScrollbar
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlaySheet
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.ObserveAsEvents
import me.weishu.kernelsu.ui.util.PopupFeedBack

@Composable
fun ModulePagerBreeze(
    uiState: ModuleUiState,
    confirmDialogState: ModuleConfirmDialogState?,
    moduleEvent: Flow<ModuleEffect>,
    actions: ModuleActions,
    isActiveTab: Boolean = true,
) {
    val snackBarHost = remember { SnackbarHostState() }

    val context = LocalContext.current
    val resource = LocalResources.current

    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    val shortcutState = rememberModuleShortcutState(context)
    val showShortcutDialog = remember { mutableStateOf(false) }
    val confirmDialog = rememberConfirmDialog()

    fun openShortcutDialogForType(type: ShortcutType) {
        shortcutState.selectType(type)
        showShortcutDialog.value = true
    }

    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        shortcutState.updateIconUri(uri?.toString())
    }

    fun onModuleAddShortcut(module: Module, type: ShortcutType) {
        shortcutState.bindModule(module)
        openShortcutDialogForType(type)
    }

    val changelogRetry = stringResource(R.string.module_changelog_retry)

    LaunchedEffect(confirmDialogState) {
        if (confirmDialogState == null) {
            confirmDialog.setConfirm(null)
        } else {
            confirmDialog.setConfirm(
                ConfirmBody(
                    title = confirmDialogState.title,
                    body = when {
                        confirmDialogState.markdown -> ConfirmBody.Body.Markdown(confirmDialogState.content.orEmpty())
                        // Three empty-changelog variants; see ConfirmBody.Body.Empty.
                        confirmDialogState.changelogFailed -> ConfirmBody.Body.Empty(
                            icon = MaterialSymbols.Rounded.CloudOff,
                            message = confirmDialogState.content.orEmpty(),
                            action = ConfirmBody.Body.Empty.Action(
                                label = changelogRetry,
                                onClick = {
                                    val update =
                                        confirmDialogState.request as? ModuleConfirmRequest.Update
                                    val info = update?.updateInfo
                                    // Re-request in place so the dialog returns straight to its spinner.
                                    if (update != null && info != null) {
                                        actions.onRequestUpdateConfirmation(update.module, info)
                                    }
                                },
                            ),
                        )

                        confirmDialogState.changelogNotConfigured -> ConfirmBody.Body.Empty(
                            icon = MaterialSymbols.Rounded.LinkOff,
                            message = confirmDialogState.content.orEmpty(),
                        )

                        confirmDialogState.changelogEmpty -> ConfirmBody.Body.Empty(
                            icon = MaterialSymbols.Rounded.Draft,
                            message = confirmDialogState.content.orEmpty(),
                        )

                        else -> ConfirmBody.Body.Text(confirmDialogState.content.orEmpty())
                    },
                    confirm = confirmDialogState.confirm,
                    dismiss = confirmDialogState.dismiss,
                    loading = confirmDialogState.changelogLoading,
                    onConfirm = {
                        when (val request = confirmDialogState.request) {
                            is ModuleConfirmRequest.Uninstall -> actions.onUninstallModule(request.module)
                            is ModuleConfirmRequest.Update -> actions.onConfirmUpdate(request)
                        }
                    },
                    onDismiss = actions.onDismissConfirmRequest,
                )
            )
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarJob = remember { mutableStateOf<Job?>(null) }
    ObserveAsEvents(moduleEvent) { event ->
        when (event) {
            is ModuleEffect.Toast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }

            is ModuleEffect.SnackBar -> {
                // Cancel the previous reboot snackbar so a new one replaces it instead of queueing
                snackbarJob.value?.cancel()
                val softReboot = isSoftRebootPreferred()
                snackbarJob.value = scope.launch {
                    val result = snackBarHost.showSnackbar(
                        message = event.message,
                        actionLabel = resource.getString(if (softReboot) R.string.reboot_soft else R.string.reboot),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        reboot(if (softReboot) "soft_reboot" else "")
                    }
                }
            }
        }
    }

    BreezeScaffold(
        snackbarHostState = snackBarHost,
        topBar = {
            SearchAppBarBreeze(
                title = { Text(stringResource(R.string.module)) },
                searchText = uiState.searchText,
                onSearchTextChange = actions.onSearchTextChange,
                onClearClick = actions.onClearSearch,
                actions = {
                    SortMenu(uiState, actions)
                },
                isActiveTab = isActiveTab,
                searchContent = { bottomPadding, closeSearch ->
                    LaunchedEffect(uiState.searchText) {
                        searchListState.scrollToItem(0)
                    }
                    ModuleList(
                        topInnerPadding = 0.dp,
                        bottomInnerPadding = bottomPadding,
                        descriptionMaxLines = uiState.descriptionMaxLines,
                        modifier = Modifier.fillMaxSize(),
                        listState = searchListState,
                        displayModules = uiState.searchResults,
                        updateInfoMap = uiState.updateInfo,
                        actions = actions,
                        onClickModule = { module ->
                            if (module.hasWebUi) {
                                actions.onOpenWebUi(module)
                                closeSearch()
                            }
                        },
                        onModuleAddShortcut = { module, type -> onModuleAddShortcut(module, type) },
                        closeSearch = closeSearch,
                    )
                    BreezeVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(bottom = bottomPadding),
                        listState = searchListState,
                        durationMillis = 1500L,
                        visible = true,
                    )
                },
            )
        },
        floatingActionButton = {
            ModuleInstallFAB(
                uiState,
                actions
            )
        },
        fabLift = true,
        fabVisible = uiState.installButtonVisible,
    ) { innerPadding ->
        val showListScrollbar by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
        }
        if (uiState.magiskInstalled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.module_magisk_conflict),
                    textAlign = TextAlign.Center,
                )
            }
            return@BreezeScaffold
        }
        BreezePullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { actions.onRefresh() },
            modifier = Modifier.fillMaxSize(),
            indicatorModifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ModuleList(
                    topInnerPadding = innerPadding.calculateTopPadding(),
                    bottomInnerPadding = innerPadding.calculateBottomPadding(),
                    // FAB height + single-line snackbar height + its 12.dp vertical padding, so the
                    // last module can still scroll clear of both.
                    extraBottomPadding = 56.dp + 12.dp + 48.dp + 12.dp,
                    descriptionMaxLines = uiState.descriptionMaxLines,
                    listState = listState,
                    displayModules = uiState.moduleList,
                    updateInfoMap = uiState.updateInfo,
                    actions = actions,
                    onClickModule = { module ->
                        if (module.hasWebUi) {
                            actions.onOpenWebUi(module)
                        }
                    },
                    onModuleAddShortcut = { module, type -> onModuleAddShortcut(module, type) },
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

    ModuleShortcutSheet(
        show = showShortcutDialog.value,
        shortcutState = shortcutState,
        onDismiss = { showShortcutDialog.value = false },
        onPickShortcutIcon = { pickShortcutIconLauncher.launch("image/*") },
        onDeleteShortcut = {
            shortcutState.deleteShortcut(context)
            showShortcutDialog.value = false
        },
        onConfirmShortcut = {
            shortcutState.createShortcut(context)
            showShortcutDialog.value = false
        },
    )

    InstallModuleDialog(
        uris = uiState.pendingInstallUri?.let { listOf(it) } ?: emptyList(),
        getInstalledModules = { uiState.moduleList },
        onConfirmInstall = { actions.onConfirmPendingInstall() },
        onDismiss = actions.onDismissPendingInstall,
    )
}

@Composable
private fun ModuleList(
    topInnerPadding: Dp,
    bottomInnerPadding: Dp,
    descriptionMaxLines: Int,
    modifier: Modifier = Modifier,
    extraBottomPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    displayModules: List<Module>,
    updateInfoMap: Map<String, ModuleUpdateInfo>,
    actions: ModuleActions,
    onClickModule: (Module) -> Unit,
    onModuleAddShortcut: (Module, ShortcutType) -> Unit,
    closeSearch: () -> Unit? = {},
) {
    val loadingDialog = rememberLoadingDialog()
    // lock auto scroll
    remember(displayModules) {
        val currentIndex = Snapshot.withoutReadObservation { listState.firstVisibleItemIndex }
        val currentOffset =
            Snapshot.withoutReadObservation { listState.firstVisibleItemScrollOffset }
        listState.requestScrollToItem(currentIndex, currentOffset)
        null
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp + topInnerPadding,
            end = 16.dp,
            bottom = 16.dp + bottomInnerPadding + extraBottomPadding,
        ),
    ) {
        items(displayModules, key = { it.id }) { module ->
            val scope = rememberCoroutineScope()
            val moduleUpdateInfo = updateInfoMap[module.id] ?: ModuleUpdateInfo.Empty
            ModuleItem(
                modifier = Modifier.animateItem(),
                module = module,
                updateUrl = moduleUpdateInfo.downloadUrl,
                descriptionMaxLines = descriptionMaxLines,
                onUninstallClicked = {
                    if (module.remove) {
                        actions.onUndoUninstallModule(module)
                    } else {
                        actions.onRequestUninstallConfirmation(module)
                    }
                },
                onCheckChanged = {
                    actions.onToggleModule(module)
                },
                onUpdate = {
                    scope.launch {
                        loadingDialog.withLoading {
                            actions.onRequestUpdateConfirmation(module, moduleUpdateInfo)
                        }
                    }
                },
                onAddShortcut = { type -> onModuleAddShortcut(module, type) },
                onClick = { onClickModule(module) },
                onExecuteAction = { actions.onExecuteModuleAction(module) },
                closeSearch = { closeSearch() }
            )
        }
    }
}

@Composable
private fun ModuleShortcutSheet(
    show: Boolean,
    shortcutState: ModuleShortcutState,
    onDismiss: () -> Unit,
    onPickShortcutIcon: () -> Unit,
    onDeleteShortcut: () -> Unit,
    onConfirmShortcut: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    fun copyShortcutUrl() {
        val url = shortcutState.buildShortcutUrl() ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("KernelSU deep link", url))
        Toast.makeText(
            context,
            resources.getString(R.string.module_shortcut_scheme_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    BreezeOverlaySheet(
        onDismissRequest = onDismiss,
        visible = show,
        modifier = Modifier.imePadding(),
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        // The sheet owns the bottom inset: the horizontal padding stays, and the navigation bar is
        // unioned in so the drag handle and content clear it without the body adding its own padding.
        contentWindowInsets = {
            WindowInsets(left = 16.dp, right = 16.dp)
                .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.module_shortcut_title),
                style = MaterialTheme.typography.titleLarge
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 13.dp)
                    .size(100.dp)
                    .clip(RoundedCornerShape(25.dp))
            ) {
                val preview = shortcutState.previewIcon
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        modifier = Modifier.size(100.dp),
                        contentDescription = null,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = FixedScale(1.5f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPickShortcutIcon,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.module_shortcut_icon_pick)
                    )
                }
                AnimatedVisibility(
                    visible = shortcutState.iconUri != shortcutState.defaultShortcutIconUri,
                    enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                    exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
                ) {
                    IconButton(
                        onClick = shortcutState::resetIconToDefault,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Filled.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = shortcutState.name,
                onValueChange = shortcutState::updateName,
                label = { Text(stringResource(id = R.string.module_shortcut_name_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp)
            )
            if (shortcutState.hasExistingShortcut) {
                TextButton(
                    onClick = onDeleteShortcut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                ) {
                    Text(stringResource(id = R.string.module_shortcut_delete))
                }
            }
            TextButton(
                onClick = ::copyShortcutUrl,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            ) {
                Text(stringResource(id = R.string.module_shortcut_copy_scheme))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                Button(
                    onClick = onConfirmShortcut,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (shortcutState.hasExistingShortcut) {
                            stringResource(id = R.string.module_update)
                        } else {
                            stringResource(id = android.R.string.ok)
                        }
                    )
                }
            }
            // contentWindowInsets above already clears the navigation bar; this is the aesthetic gap only.
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModuleItem(
    modifier: Modifier = Modifier,
    module: Module,
    updateUrl: String,
    descriptionMaxLines: Int,
    onUninstallClicked: () -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: () -> Unit,
    onAddShortcut: (ShortcutType) -> Unit,
    onClick: () -> Unit,
    onExecuteAction: () -> Unit,
    closeSearch: () -> Unit
) {
    TonalCard(
        modifier = modifier.fillMaxWidth()
    ) {
        val textDecoration = if (!module.remove) null else TextDecoration.LineThrough
        val interactionSource = remember { MutableInteractionSource() }
        val indication = LocalIndication.current
        val hasDescription = module.description.isNotBlank()
        val maxLinesLimit = descriptionMaxLines
        var expanded by rememberSaveable(module.id) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .run {
                    if (module.hasWebUi) {
                        toggleable(
                            value = module.enabled,
                            enabled = !module.remove && module.enabled,
                            interactionSource = interactionSource,
                            role = Role.Button,
                            indication = indication,
                            onValueChange = { onClick() }
                        )
                    } else {
                        this
                    }
                }
                .padding(22.dp, 18.dp, 22.dp, 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val moduleVersion = stringResource(id = R.string.module_version)
                val moduleAuthor = stringResource(id = R.string.module_author)

                Column(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = module.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            lineHeight = MaterialTheme.typography.titleMedium.fontSize,
                            textDecoration = textDecoration,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (module.metamodule) {
                            StatusTagBreeze(
                                label = "META",
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    Text(
                        text = "$moduleVersion: ${module.version}",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = textDecoration
                    )

                    Text(
                        text = "$moduleAuthor: ${module.author}",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = textDecoration
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ExpressiveSwitch(
                        enabled = !module.update,
                        checked = module.enabled,
                        onCheckedChange = onCheckChanged,
                        interactionSource = if (!module.hasWebUi) interactionSource else remember { MutableInteractionSource() }
                    )
                }
            }

            if (hasDescription) {
                Spacer(modifier = Modifier.height(8.dp))

                ExpandableDescriptionText(
                    text = module.description,
                    expanded = expanded,
                    maxLinesLimit = maxLinesLimit,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded },
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = textDecoration,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StatusTagBreeze(
                label = module.id,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ModuleButtonRow(
                module = module,
                updateUrl = updateUrl,
                onExecuteAction = {
                    onExecuteAction()
                    closeSearch()
                },
                onOpenWebUi = {
                    onClick()
                    closeSearch()
                },
                onUpdate = onUpdate,
                onUninstallClicked = onUninstallClicked,
                onAddShortcut = onAddShortcut,
            )
        }
    }
}

@Composable
private fun ModuleButtonRow(
    module: Module,
    updateUrl: String,
    onExecuteAction: () -> Unit,
    onOpenWebUi: () -> Unit,
    onUpdate: () -> Unit,
    onUninstallClicked: () -> Unit,
    onAddShortcut: (ShortcutType) -> Unit,
) {
    val hasUpdate = updateUrl.isNotEmpty()
    val iconSize = ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight)
    val actionButtonsEnabled = !module.remove && module.enabled

    val actionText = stringResource(id = R.string.action)
    val openText = stringResource(id = R.string.open)
    val updateText = stringResource(id = R.string.module_update)
    val uninstallText = stringResource(id = R.string.uninstall)
    val undoText = stringResource(id = R.string.undo)

    val startButtons = remember(actionButtonsEnabled, actionText, openText) {
        buildList {
            add(
                ButtonSpec(
                    id = "action",
                    text = actionText,
                    isVisible = module.hasActionScript,
                    isEnabled = actionButtonsEnabled,
                    icon = {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = MaterialSymbols.Filled.Rounded.PlayArrow,
                            contentDescription = null
                        )
                    },
                    onClick = onExecuteAction,
                    buttonPosition = ButtonPosition.START,
                )
            )
            add(
                ButtonSpec(
                    id = "webui",
                    text = openText,
                    isVisible = module.hasWebUi,
                    isEnabled = actionButtonsEnabled,
                    icon = {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = MaterialSymbols.Rounded.Wysiwyg,
                            contentDescription = openText
                        )
                    },
                    onClick = onOpenWebUi,
                    buttonPosition = ButtonPosition.START,
                )
            )
        }
    }

    val endButtons = remember(hasUpdate, updateText, uninstallText, undoText, module.remove) {
        buildList {
            add(
                ButtonSpec(
                    id = "update",
                    text = updateText,
                    isVisible = hasUpdate,
                    isEnabled = !module.remove,
                    type = ButtonType.PRIMARY,
                    icon = {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = MaterialSymbols.Rounded.Download2,
                            contentDescription = updateText
                        )
                    },
                    onClick = onUpdate,
                    buttonPosition = ButtonPosition.END,
                )
            )
            add(
                ButtonSpec(
                    id = "uninstall",
                    text = if (module.remove) undoText else uninstallText,
                    isVisible = true,
                    isEnabled = true,
                    icon = {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = if (module.remove) {
                                MaterialSymbols.Filled.Rounded.Undo
                            } else {
                                MaterialSymbols.Filled.Rounded.Delete
                            },
                            contentDescription = if (module.remove) undoText else uninstallText
                        )
                    },
                    onClick = onUninstallClicked,
                    buttonPosition = ButtonPosition.END,
                )
            )
        }
    }

    val lastVisibleStartButtonId = remember(startButtons) {
        startButtons.lastOrNull { it.isVisible }?.id
    }
    val lastVisibleEndButtonId = remember(endButtons) {
        endButtons.lastOrNull { it.isVisible }?.id
    }

    EnumeratedPriorityButtonRow(
        centerSpacing = 150.dp,
        startButtons = startButtons,
        endButtons = endButtons,
    ) { spec, isExpanded ->
        val isLastVisibleInGroup = when (spec.buttonPosition) {
            ButtonPosition.START -> spec.id == lastVisibleStartButtonId
            ButtonPosition.END -> spec.id == lastVisibleEndButtonId
        }
        val animatedEndPadding by animateDpAsState(
            targetValue = if (isLastVisibleInGroup) 0.dp else 8.dp,
            label = "ModuleButtonSpacing"
        )
        val buttonModifier = Modifier.padding(end = animatedEndPadding)

        val onLongClick = when (spec.id) {
            "action" -> ({ onAddShortcut(ShortcutType.Action) })
            "webui" -> ({ onAddShortcut(ShortcutType.WebUI) })
            else -> null
        }
        ActionButton(
            modifier = buttonModifier,
            text = spec.text,
            icon = spec.icon,
            onClick = spec.onClick,
            onLongClick = onLongClick,
            visible = spec.isVisible,
            enabled = spec.isEnabled,
            isExpanded = isExpanded,
            buttonType = spec.type,
            buttonPosition = spec.buttonPosition,
        )
    }
}

@Composable
private fun SortMenu(
    uiState: ModuleUiState,
    actions: ModuleActions,
) {
    var showDropdown by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showDropdown = true }
    ) {
        Icon(
            imageVector = MaterialSymbols.Filled.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.settings)
        )
        DropdownMenuPopup(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            PopupFeedBack()
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                CheckableDropdownMenuItem(
                    text = { Text(stringResource(R.string.module_sort_action_first)) },
                    checked = uiState.sortActionFirst,
                    checkedLeadingIcon = {
                        Icon(
                            MaterialSymbols.Filled.Rounded.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        actions.onToggleSortActionFirst()
                    },
                    shapes = MenuDefaults.itemShape(index = 0, count = 2)
                )
                CheckableDropdownMenuItem(
                    text = { Text(stringResource(R.string.module_sort_enabled_first)) },
                    checked = uiState.sortEnabledFirst,
                    checkedLeadingIcon = {
                        Icon(
                            MaterialSymbols.Filled.Rounded.Check,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        actions.onToggleSortEnabledFirst()
                    },
                    shapes = MenuDefaults.itemShape(index = 1, count = 2)
                )
            }
        }
    }
}

@Composable
private fun ModuleInstallFAB(
    uiState: ModuleUiState,
    actions: ModuleActions,
) {
    val moduleInstall = stringResource(id = R.string.module_install)
    var zipUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    InstallModuleDialog(
        zipUris, { uiState.moduleList },
        onConfirmInstall = { actions.onOpenFlash(zipUris) },
        onDismiss = { zipUris = emptyList() }
    )

    val selectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val uris = mutableListOf<Uri>()
        if (activityResult.resultCode != RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val data = activityResult.data ?: return@rememberLauncherForActivityResult
        val clipData = data.clipData

        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i)?.uri?.let { uris.add(it) }
            }
        } else {
            data.data?.let { uris.add(it) }
        }
        zipUris = uris
    }

    ExtendedFloatingActionButton(
        modifier = Modifier
            .animateFloatingActionButton(
                uiState.installButtonVisible,
                alignment = Alignment.CenterEnd
            ),
        onClick = {
            // Select the zip files to install
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            selectZipLauncher.launch(intent)
        },
        icon = { Icon(MaterialSymbols.Filled.Rounded.Add, moduleInstall) },
        text = { Text(text = moduleInstall) },
    )
}
