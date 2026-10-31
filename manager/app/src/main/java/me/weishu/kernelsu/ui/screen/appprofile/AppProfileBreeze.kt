package me.weishu.kernelsu.ui.screen.appprofile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.ownerNameForUid
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.button.SplitScreenRatioButton
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.LocalSegmentedItemShapes
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedMenuRow
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.component.common.AppIconImage
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.screen.profile.RootProfileConfigBreeze
import me.weishu.kernelsu.ui.util.LocalIsDetailPane

/**
 * @author weishu
 * @date 2023/5/16.
 */
@Composable
fun AppProfileScreenBreeze(
    state: AppProfileUiState,
    actions: AppProfileActions,
) {
    val profile = state.profile
    val isSpecialApp = state.appGroup.primary.special
    val isRootGranted = !isSpecialApp && profile.allowSu

    val initialRootMode = when {
        profile.rootUseDefault -> Mode.Default
        profile.rootTemplate != null -> Mode.Template
        else -> Mode.Custom
    }
    var rootMode by rememberSaveable(profile) {
        mutableStateOf(initialRootMode)
    }
    var nonRootMode by rememberSaveable(profile) {
        mutableStateOf(if (profile.nonRootUseDefault) Mode.Default else Mode.Custom)
    }
    val currentMode = if (isRootGranted) rootMode else nonRootMode

    BreezeScaffold(
        message = state.message,
        onMessageShown = actions.onMessageShown,
        topBar = {
            BreezeTopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    if (LocalIsDetailPane.current) return@BreezeTopAppBar
                    BreezeBackButton(
                        onClick = actions.onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
                actions = { SplitScreenRatioButton() },
            )
        },
    ) { innerPadding ->
        BreezeScrollColumn(
            innerPadding = innerPadding,
            horizontalPadding = 0.dp,
            verticalArrangement = Arrangement.Top,
        ) {
            AppInfoGroup(
                appIcon = {
                    AppIconImage(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(48.dp)
                            .height(48.dp),
                        packageInfo = state.appGroup.primary.packageInfo,
                        label = state.appGroup.primary.label,
                    )
                },
                appLabel = if (state.isUidGroup) ownerNameForUid(state.uid) else state.appGroup.primary.label,
                appUid = state.uid,
                appVersionName = state.appGroup.primary.packageInfo.versionName ?: "",
                appVersionCode = state.appGroup.primary.packageInfo.longVersionCode,
                packageName = if (state.isUidGroup) state.sharedUserId else if (isSpecialApp) state.appGroup.primary.displayIdentifier else state.packageName,
                affectedAppCount = state.appGroup.apps.size,
                isRootGranted = isRootGranted,
                isSpecialApp = isSpecialApp,
                mode = currentMode.text,
                menuContent = if (state.isUidGroup || isSpecialApp) {
                    null
                } else {
                    appMenuContent(
                        packageName = state.packageName,
                        userId = state.uid / 100000,
                        actions = actions,
                    )
                },
                onAllowSuChange = {
                    actions.onProfileChange(profile.copy(allowSu = it))
                }
            )

            ModeChipBar(mode = currentMode, showTemplate = isRootGranted) { newMode ->
                if (isRootGranted) {
                    // template mode shouldn't change profile here!
                    if (newMode == Mode.Default || newMode == Mode.Custom) {
                        actions.onProfileChange(
                            profile.copy(
                                rootUseDefault = newMode == Mode.Default,
                                rootTemplate = null,
                            )
                        )
                    }
                    rootMode = newMode
                } else {
                    actions.onProfileChange(
                        profile.copy(nonRootUseDefault = newMode == Mode.Default)
                    )
                    nonRootMode = newMode
                }
            }

            Crossfade(targetState = isRootGranted, label = "AppProfileMode") { hasRoot ->
                if (hasRoot) {
                    RootProfile(
                        profile = profile,
                        mode = rootMode,
                        onProfileChange = actions.onProfileChange,
                    )
                } else {
                    AppProfileConfigBreeze(
                        modifier = Modifier.padding(all = 16.dp),
                        enabled = nonRootMode == Mode.Custom,
                        profile = profile,
                        onProfileChange = actions.onProfileChange,
                    )
                }
            }

            AffectedAppColumn(
                affectedApps = state.appGroup.apps,
                actions = actions,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * App identity row: icon, label, version lines, the UID / USER tags, and a contextual menu when
 * the app has one.
 */
@Composable
private fun AppInfoRow(
    appIcon: @Composable () -> Unit,
    appLabel: String,
    appUid: Int,
    userId: Int,
    supporting: @Composable () -> Unit,
    menuContent: (@Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit)?,
) {
    val tags: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusTagBreeze(
                label = "UID $appUid",
                backgroundColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )
            if (userId != 0) {
                StatusTagBreeze(
                    label = "USER $userId",
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }
    if (menuContent == null) {
        SegmentedListItem(
            onClick = {},
            shapes = LocalSegmentedItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
            leadingContent = appIcon,
            supportingContent = supporting,
            trailingContent = tags,
        ) {
            Text(appLabel)
        }
    } else {
        SegmentedMenuRow(
            title = appLabel,
            menuContent = menuContent,
            leadingContent = appIcon,
            supportingContent = supporting,
            trailingContent = tags,
        )
    }
}

@Composable
private fun AppInfoGroup(
    appIcon: @Composable () -> Unit,
    appLabel: String,
    appUid: Int,
    appVersionName: String,
    appVersionCode: Long,
    packageName: String,
    affectedAppCount: Int,
    isRootGranted: Boolean,
    isSpecialApp: Boolean,
    mode: String,
    menuContent: (@Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit)?,
    onAllowSuChange: (Boolean) -> Unit,
) {
    val isUidGroup = affectedAppCount > 1
    val userId = appUid / 100000

    SegmentedListGroup(modifier = Modifier.padding(16.dp)) {
        item(key = "app") {
            AppInfoRow(
                appIcon = appIcon,
                appLabel = appLabel,
                appUid = appUid,
                userId = userId,
                menuContent = menuContent,
                supporting = {
                    Column {
                        if (isSpecialApp) {
                            Text(packageName)
                        } else if (!isUidGroup) {
                            Text("$appVersionName ($appVersionCode)")
                            if (packageName.isNotEmpty()) {
                                Text(packageName)
                            }
                        } else {
                            if (packageName.isNotEmpty()) {
                                Text(packageName)
                            }
                            Text(stringResource(R.string.group_contains_apps, affectedAppCount))
                        }
                    }
                },
            )
        }
        item(key = "allowSu", visible = !isSpecialApp) {
            SegmentedSwitchItem(
                title = stringResource(R.string.superuser),
                leadingIcon = MaterialSymbols.Rounded.Security,
                checked = isRootGranted,
                onCheckedChange = onAllowSuChange,
            )
        }
        item(key = "profile") {
            SegmentedNavItem(
                title = stringResource(R.string.profile),
                summary = mode,
                leadingIcon = MaterialSymbols.Filled.Rounded.AccountCircle,
                onClick = {},
                trailing = {},
            )
        }
    }
}

@Composable
private fun ModeChipBar(
    mode: Mode,
    showTemplate: Boolean,
    onModeChange: (Mode) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        item(key = Mode.Default) {
            FilterChip(
                modifier = Modifier.animateItem(),
                selected = mode == Mode.Default,
                label = { Text(stringResource(R.string.profile_default)) },
                onClick = { onModeChange(Mode.Default) },
            )
        }
        if (showTemplate) {
            item(key = Mode.Template) {
                FilterChip(
                    modifier = Modifier.animateItem(),
                    selected = mode == Mode.Template,
                    label = { Text(stringResource(R.string.profile_template)) },
                    onClick = { onModeChange(Mode.Template) },
                )
            }
        }
        item(key = Mode.Custom) {
            FilterChip(
                modifier = Modifier.animateItem(),
                selected = mode == Mode.Custom,
                label = { Text(stringResource(R.string.profile_custom)) },
                onClick = { onModeChange(Mode.Custom) },
            )
        }
    }
}

@Composable
private fun RootProfile(
    profile: Natives.Profile,
    mode: Mode,
    onProfileChange: (Natives.Profile) -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val direction = if (isRtl == (targetState.ordinal > initialState.ordinal)) -1 else 1
            slideInHorizontally(
                initialOffsetX = { it * direction },
                animationSpec = motionScheme.defaultSpatialSpec()
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it * direction },
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        },
        label = "RootProfileMode",
    ) { targetMode ->
        when (targetMode) {
            Mode.Template -> {
                TemplateConfigBreeze(profile, onProfileChange)
            }

            Mode.Custom -> {
                RootProfileConfigBreeze(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    readOnly = false,
                    profile = profile,
                    onProfileChange = onProfileChange,
                )
            }

            Mode.Default -> {
                Box(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}


@Composable
private fun AffectedAppColumn(
    affectedApps: List<AppInfo>,
    actions: AppProfileActions,
) {
    if (affectedApps.size < 2) {
        return
    }

    SegmentedListGroup(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        title = stringResource(R.string.app_profile_affects_following_apps),
    ) {
        affectedApps.forEach { app ->
            val versionCode = app.packageInfo.longVersionCode

            val appIcon: @Composable () -> Unit = {
                AppIconImage(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(48.dp)
                        .height(48.dp),
                    packageInfo = app.packageInfo,
                    label = app.label,
                )
            }
            val supporting: @Composable () -> Unit = {
                Column {
                    Text(app.displayIdentifier)
                    if (!app.special) {
                        Text("${app.packageInfo.versionName} ($versionCode)")
                    }
                }
            }
            item(key = app.packageName.ifEmpty { app.uid.toString() }) {
                if (app.special) {
                    SegmentedListItem(
                        onClick = {},
                        shapes = LocalSegmentedItemShapes.current
                            ?: ListItemDefaults.segmentedShapes(0, 1),
                        leadingContent = appIcon,
                        supportingContent = supporting,
                    ) {
                        Text(app.label)
                    }
                } else {
                    SegmentedMenuRow(
                        title = app.label,
                        menuContent = appMenuContent(
                            packageName = app.packageName,
                            userId = app.uid / 100000,
                            actions = actions,
                        ),
                        leadingContent = appIcon,
                        supportingContent = supporting,
                    )
                }
            }
        }
    }
}

private fun appMenuContent(
    packageName: String,
    userId: Int,
    actions: AppProfileActions,
): @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit = { dismissMenu ->
    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
        SelectableDropdownMenuItem(
            selected = false,
            shapes = MenuDefaults.itemShape(index = 0, count = 3),
            text = { Text(stringResource(R.string.launch_app)) },
            onClick = {
                dismissMenu()
                actions.onLaunchApp(packageName, userId)
            },
        )
        SelectableDropdownMenuItem(
            selected = false,
            shapes = MenuDefaults.itemShape(index = 1, count = 3),
            text = { Text(stringResource(R.string.force_stop_app)) },
            onClick = {
                dismissMenu()
                actions.onForceStopApp(packageName, userId)
            },
        )
        SelectableDropdownMenuItem(
            selected = false,
            shapes = MenuDefaults.itemShape(index = 2, count = 3),
            text = { Text(stringResource(R.string.restart_app)) },
            onClick = {
                dismissMenu()
                actions.onRestartApp(packageName, userId)
            },
        )
    }
}
