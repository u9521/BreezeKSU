package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.R.string.app_name
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.LoadingDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.breeze.expressive.TonalCard
import me.weishu.kernelsu.ui.component.breeze.feedback.StatusTagBreeze
import me.weishu.kernelsu.ui.component.breeze.list.LocalSegmentedItemShapes
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.icons.MaterialSymbols

@Composable
fun HomePagerBreeze(
    state: HomeUiState,
    actions: HomeActions,
) {
    LoadingDialogBreeze(visible = state.isJailbreakLoading)

    BreezeScaffold(
        topBar = {
            BreezeTopAppBar(
                title = { Text(stringResource(app_name)) },
                actions = { RebootListPopupBreeze() },
            )
        },
    ) { innerPadding ->
        BreezeScrollColumn(innerPadding = innerPadding) {
            if (state.showManagerPrBuildWarning) {
                WarningCard(
                    stringResource(id = R.string.home_pr_build_warning),
                    level = WarningLevel.Notice
                )
            } else if (state.showKernelPrBuildWarning) {
                WarningCard(
                    stringResource(id = R.string.home_pr_kernel_warning),
                    level = WarningLevel.Notice
                )
            }
            if (state.requiresNewKernel) {
                WarningCard(
                    stringResource(
                        id = if (state.lkmMode == true) R.string.require_kernel_version else R.string.require_kernel_version_gki
                    ),
                    onClick = if (state.lkmMode == true) actions.onInstallClick else null
                )
            }
            if (state.requiresNewManager) {
                WarningCard(
                    stringResource(
                        id = R.string.require_manager_version
                    )
                )
            }
            if (state.showRootWarning) {
                WarningCard(stringResource(id = R.string.grant_root_failed))
            }
            if (state.checkUpdateEnabled) {
                UpdateCard(state = state, actions = actions)
            }
            StatusCard(
                state = state,
                actions = actions,
            )
            InfoCard(systemInfo = state.systemInfo)
            SupportLinks(onOpenUrl = actions.onOpenUrl)
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val newVersion = state.latestVersionInfo
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog()
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            level = WarningLevel.Notice
        ) {
            if (newVersion.changelog.isEmpty()) {
                actions.onOpenUrl(newVersion.downloadUrl)
            } else {
                updateDialog.setConfirm(
                    ConfirmBody(
                        title = title,
                        body = ConfirmBody.Body.Markdown(newVersion.changelog),
                        confirm = updateText,
                        onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) },
                    )
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val ksuActive = state.ksuVersion != null
    val notInstalled = !ksuActive && state.kernelVersion.isGKI()

    val containerColor = if (ksuActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    val statusIcon = when {
        ksuActive -> MaterialSymbols.Rounded.CheckCircle
        notInstalled -> MaterialSymbols.Rounded.Warning
        else -> MaterialSymbols.Rounded.Block
    }
    val statusTitle = when {
        ksuActive -> stringResource(R.string.home_working)
        notInstalled -> stringResource(R.string.home_not_installed)
        else -> stringResource(R.string.home_unsupported)
    }
    val statusSummary = when {
        ksuActive -> stringResource(
            R.string.home_working_version,
            "${state.ksuVersion}-${state.kernelUAPIVersion}"
        )

        notInstalled -> stringResource(R.string.home_click_to_install)
        else -> stringResource(R.string.home_unsupported_reason)
    }
    val workingMode = if (ksuActive) {
        when (state.lkmMode) {
            null -> ""
            true -> "LKM"
            else -> "GKI"
        }
    } else ""

    val statusTrailing: (@Composable () -> Unit)? = if (notInstalled && state.isSELinuxPermissive) {
        {
            Button(
                onClick = actions.onJailbreakClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.home_jailbreak))
            }
        }
    } else null

    val cardColors = ListItemDefaults.segmentedColors(
        containerColor = containerColor,
        contentColor = contentColor,
        leadingContentColor = contentColor,
        trailingContentColor = contentColor,
        supportingContentColor = contentColor.copy(alpha = 0.7f),
    )

    val statusTags: @Composable () -> Unit = {
        if (workingMode != "") {
            Spacer(Modifier.width(4.dp))
            StatusTagBreeze(
                label = workingMode,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                backgroundColor = MaterialTheme.colorScheme.primary
            )
        }
        if (ksuActive && state.isSafeMode) {
            Spacer(Modifier.width(4.dp))
            StatusTagBreeze(
                label = stringResource(id = R.string.safe_mode),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                backgroundColor = MaterialTheme.colorScheme.errorContainer
            )
        }
        if (ksuActive && state.isLateLoadMode) {
            Spacer(Modifier.width(4.dp))
            StatusTagBreeze(
                label = stringResource(id = R.string.jailbreak_mode),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                backgroundColor = MaterialTheme.colorScheme.errorContainer
            )
        }
    }

    // A card whose colours differ from the group default, so it uses the raw row rather than one of
    // the standard segmented items.
    SegmentedListGroup {
        item(key = "status") {
            SegmentedListItem(
                onClick = actions.onInstallClick,
                shapes = LocalSegmentedItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
                colors = cardColors,
                modifier = Modifier.heightIn(min = 88.dp),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = {
                    Icon(statusIcon, contentDescription = statusTitle)
                },
                trailingContent = statusTrailing,
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusSummary,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.showCustomLkmBadge) {
                            Spacer(Modifier.width(4.dp))
                            StatusTagBreeze(
                                label = stringResource(R.string.home_lkm_custom),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            )
                        }
                    }
                },
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        statusTags()
                    }
                },
            )
        }
    }
}

@Composable
private fun WarningCard(
    message: String,
    level: WarningLevel = WarningLevel.Error,
    onClick: (() -> Unit)? = null
) {
    val containerColor = when (level) {
        WarningLevel.Error -> MaterialTheme.colorScheme.errorContainer
        WarningLevel.Notice -> MaterialTheme.colorScheme.tertiaryContainer
    }
    TonalCard(containerColor = containerColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        }
    }
}

@Composable
private fun SupportLinks(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hoisted: the click handler below is not a composable context.
    val learnMoreUrl = stringResource(R.string.home_learn_kernelsu_url)

    SegmentedListGroup(modifier = modifier.fillMaxWidth()) {
        item(key = "support") {
            SegmentedNavItem(
                title = stringResource(R.string.home_support_title),
                summary = stringResource(R.string.home_support_content),
                leadingIcon = MaterialSymbols.Filled.Rounded.VolunteerActivism,
                onClick = { onOpenUrl("https://patreon.com/weishu") },
                trailing = {},
            )
        }
        item(key = "learn") {
            SegmentedNavItem(
                title = stringResource(R.string.home_learn_kernelsu),
                summary = stringResource(R.string.home_click_to_learn_kernelsu),
                leadingIcon = MaterialSymbols.Filled.Rounded.MenuBook,
                onClick = { onOpenUrl(learnMoreUrl) },
                trailing = {},
            )
        }
    }
}

@Composable
private fun InfoCard(
    systemInfo: SystemInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        SegmentedListGroup(modifier = Modifier.fillMaxWidth()) {
            item(key = "managerVersion") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Filled.Rounded.Tag,
                    label = stringResource(R.string.home_manager_version),
                    value = systemInfo.managerVersion,
                )
            }
            item(key = "kernel") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Rounded.DeveloperBoard,
                    label = stringResource(R.string.home_kernel),
                    value = systemInfo.kernelVersion,
                )
            }
            item(key = "deviceModel") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Rounded.Smartphone,
                    label = stringResource(R.string.home_device_model),
                    value = systemInfo.deviceModel,
                )
            }
            item(key = "fingerprint") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Filled.Rounded.Fingerprint,
                    label = stringResource(R.string.home_fingerprint),
                    value = systemInfo.fingerprint,
                )
            }
        }
        SegmentedListGroup(modifier = Modifier.fillMaxWidth()) {
            item(key = "selinux") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Rounded.Security,
                    label = stringResource(R.string.home_selinux_status),
                    value = when (systemInfo.selinuxStatus) {
                        "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
                        "Permissive" -> stringResource(R.string.selinux_status_permissive)
                        "Disabled" -> stringResource(R.string.selinux_status_disabled)
                        else -> stringResource(R.string.selinux_status_unknown)
                    },
                )
            }
            item(key = "seccomp") {
                SegmentedInfoRow(
                    icon = MaterialSymbols.Rounded.FilterList,
                    label = stringResource(R.string.home_seccomp_status),
                    value = when (systemInfo.seccompStatus) {
                        -1 -> stringResource(R.string.seccomp_status_not_supported)
                        0 -> stringResource(R.string.seccomp_status_disabled)
                        1 -> stringResource(R.string.seccomp_status_strict)
                        2 -> stringResource(R.string.seccomp_status_filter)
                        else -> stringResource(R.string.seccomp_status_unknown)
                    },
                )
            }
        }
    }
}

/** A read-only row: [label] as the title, [value] underneath and [icon] at the leading edge. */
@Composable
private fun SegmentedInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    SegmentedListItem(
        onClick = {},
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        verticalAlignment = Alignment.CenterVertically,
        shapes = LocalSegmentedItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = label)
        },
        supportingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
