package me.weishu.kernelsu.ui.screen.appprofile

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.util.UiMessage

@Immutable
data class AppProfileUiState(
    val uid: Int,
    val packageName: String,
    val profile: Natives.Profile,
    val appGroup: GroupedApps,
    val sharedUserId: String,
    /** One-shot feedback shown by the scaffold; [AppProfileActions.onMessageShown] clears it. */
    val message: UiMessage? = null,
) {
    val isUidGroup get() = appGroup.apps.size > 1
}

@Immutable
data class AppProfileActions(
    val onBack: () -> Unit,
    val onLaunchApp: (String, Int) -> Unit,
    val onForceStopApp: (String, Int) -> Unit,
    val onRestartApp: (String, Int) -> Unit,
    val onViewTemplate: (String) -> Unit,
    val onManageTemplate: () -> Unit,
    val onMessageShown: (Long) -> Unit,
    val onProfileChange: (Natives.Profile) -> Unit,
)
