package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.R.string.settings
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedMenuRow
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.icons.MaterialSymbols

@Composable
fun SettingPagerBreeze(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    val preferences = uiState.preferences
    val ksuVersion = if (Natives.isManager) Natives.version else null
    val isKsuValid = ksuVersion != null
    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    UninstallDialogBreeze(
        show = showUninstallDialog,
        onDismissRequest = { showUninstallDialog = false },
    )

    BreezeScaffold(
        topBar = {
            BreezeTopAppBar(title = { Text(stringResource(settings)) })
        },
    ) { innerPadding ->
        BreezeScrollColumn(innerPadding = innerPadding) {
            SegmentedListGroup {
                item(key = "checkUpdate") {
                    SegmentedSwitchItem(
                        title = stringResource(id = R.string.settings_check_update),
                        summary = stringResource(id = R.string.settings_check_update_summary),
                        leadingIcon = MaterialSymbols.Filled.Rounded.SystemUpdate,
                        checked = preferences.checkUpdate,
                        onCheckedChange = actions.onSetCheckUpdate,
                    )
                }

                item(key = "checkModuleUpdate", visible = isKsuValid) {
                    SegmentedSwitchItem(
                        title = stringResource(id = R.string.settings_module_check_update),
                        summary = stringResource(id = R.string.settings_check_update_summary),
                        leadingIcon = MaterialSymbols.Filled.Rounded.SystemUpdateAlt,
                        checked = preferences.checkModuleUpdate,
                        onCheckedChange = actions.onSetCheckModuleUpdate,
                    )
                }
            }

            SegmentedListGroup {
                item(key = "theme") {
                    SegmentedNavItem(
                        title = stringResource(id = R.string.settings_theme),
                        summary = stringResource(id = R.string.settings_theme_summary),
                        leadingIcon = MaterialSymbols.Filled.Rounded.Palette,
                        onClick = actions.onOpenTheme,
                    )
                }
            }

            if (isKsuValid) {
                SegmentedListGroup {
                    item(key = "profileTemplate") {
                        SegmentedNavItem(
                            title = stringResource(id = R.string.settings_profile_template),
                            summary = stringResource(id = R.string.settings_profile_template_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.Description,
                            onClick = actions.onOpenProfileTemplate,
                        )
                    }
                }
            }

            if (isKsuValid) {
                SegmentedListGroup {
                    item(key = "suCompat") {
                        // Needed twice: as the trailing selected text and as the menu contents.
                        val suCompatModeItems = listOf(
                            stringResource(id = R.string.settings_mode_enable_by_default),
                            stringResource(id = R.string.settings_mode_disable_until_reboot),
                            stringResource(id = R.string.settings_mode_disable_always),
                        )
                        SegmentedMenuRow(
                            title = stringResource(id = R.string.settings_sucompat),
                            summary = when (uiState.suCompatStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sucompat_summary)
                            },
                            selectedText = suCompatModeItems.getOrNull(uiState.suCompatMode),
                            leadingIcon = MaterialSymbols.Filled.Rounded.AdminPanelSettings,
                            enabled = uiState.suCompatStatus == "supported",
                            menuContent = { dismissMenu ->
                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    suCompatModeItems.forEachIndexed { index, name ->
                                        SelectableDropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                actions.onSetSuCompatMode(index)
                                                dismissMenu()
                                            },
                                            shapes = MenuDefaults.itemShape(
                                                index = index,
                                                count = suCompatModeItems.size
                                            ),
                                            selected = index == uiState.suCompatMode,
                                            selectedLeadingIcon = {
                                                Icon(
                                                    MaterialSymbols.Filled.Rounded.Check,
                                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                                    contentDescription = null,
                                                )
                                            }
                                        )
                                    }
                                }
                            },
                        )
                    }

                    item(key = "kernelUmount") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_kernel_umount),
                            summary = when (uiState.kernelUmountStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_kernel_umount_summary)
                            },
                            leadingIcon = MaterialSymbols.Filled.Rounded.LayersClear,
                            enabled = uiState.kernelUmountStatus == "supported",
                            checked = uiState.isKernelUmountEnabled,
                            onCheckedChange = actions.onSetKernelUmountEnabled,
                        )
                    }

                    item(key = "selinuxHide") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_selinux_hide),
                            summary = when (uiState.selinuxHideStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_selinux_hide_summary)
                            },
                            leadingIcon = MaterialSymbols.Filled.Rounded.Security,
                            enabled = uiState.selinuxHideStatus == "supported",
                            checked = uiState.isSelinuxHideEnabled,
                            onCheckedChange = actions.onSetSelinuxHideEnabled,
                        )
                    }

                    item(key = "sulog") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_sulog),
                            summary = when (uiState.sulogStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sulog_summary)
                            },
                            leadingIcon = MaterialSymbols.Filled.Rounded.Article,
                            enabled = uiState.sulogStatus == "supported",
                            checked = uiState.isSulogEnabled,
                            onCheckedChange = actions.onSetSulogEnabled,
                        )
                    }

                    item(key = "adbRoot") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_adb_root),
                            summary = when (uiState.adbRootStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_adb_root_summary)
                            },
                            leadingIcon = MaterialSymbols.Filled.Rounded.Adb,
                            checked = uiState.isAdbRootEnabled,
                            onCheckedChange = actions.onSetAdbRootEnabled,
                        )
                    }

                    item(key = "shellSuAdbOnly") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_shell_su_adb_only),
                            summary = when (uiState.shellSuAdbOnlyStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                                else -> stringResource(id = R.string.settings_shell_su_adb_only_summary)
                            },
                            leadingIcon = MaterialSymbols.Filled.Rounded.VerifiedUser,
                            enabled = uiState.shellSuAdbOnlyStatus == "supported",
                            checked = uiState.isShellSuAdbOnlyEnabled,
                            onCheckedChange = actions.onSetShellSuAdbOnlyEnabled,
                        )
                    }

                    item(key = "softReboot") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_soft_reboot),
                            summary = stringResource(id = R.string.settings_soft_reboot_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.RestartAlt,
                            enabled = !uiState.isLateLoadMode,
                            checked = uiState.isLateLoadMode || preferences.useSoftReboot,
                            onCheckedChange = actions.onSetUseSoftReboot,
                        )
                    }
                }

                SegmentedListGroup {
                    item(key = "umountModules") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_umount_modules_default),
                            summary = stringResource(id = R.string.settings_umount_modules_default_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.Rule,
                            checked = uiState.isDefaultUmountModules,
                            onCheckedChange = actions.onSetDefaultUmountModules,
                        )
                    }

                    item(key = "webDebugging") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.enable_web_debugging),
                            summary = stringResource(id = R.string.enable_web_debugging_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.DeveloperMode,
                            checked = preferences.enableWebDebugging,
                            onCheckedChange = actions.onSetEnableWebDebugging,
                        )
                    }

                    item(key = "autoJailbreak") {
                        SegmentedSwitchItem(
                            title = stringResource(id = R.string.settings_auto_jailbreak),
                            summary = stringResource(id = R.string.settings_auto_jailbreak_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.FlashOn,
                            enabled = uiState.isLateLoadMode,
                            checked = preferences.autoJailbreak,
                            onCheckedChange = actions.onSetAutoJailbreak,
                        )
                    }
                }
            }

            SegmentedListGroup {
                item(key = "uninstall") {
                    SegmentedNavItem(
                        title = stringResource(id = R.string.settings_uninstall),
                        leadingIcon = MaterialSymbols.Filled.Rounded.Delete,
                        enabled = !uiState.isLateLoadMode && uiState.isLkmMode,
                        onClick = { showUninstallDialog = true },
                        trailing = {},
                    )
                }
            }

            SegmentedListGroup {
                item(key = "sendLog") {
                    SegmentedNavItem(
                        title = stringResource(id = R.string.send_log),
                        leadingIcon = MaterialSymbols.Filled.Rounded.BugReport,
                        onClick = { showBottomSheet = true },
                        trailing = {},
                    )
                }

                item(key = "about") {
                    SegmentedNavItem(
                        title = stringResource(id = R.string.about),
                        leadingIcon = MaterialSymbols.Filled.Rounded.Info,
                        onClick = actions.onOpenAbout,
                    )
                }
            }

            SendLogBottomSheetBreeze(
                visible = showBottomSheet,
                onDismiss = { showBottomSheet = false },
            )
        }
    }
}
