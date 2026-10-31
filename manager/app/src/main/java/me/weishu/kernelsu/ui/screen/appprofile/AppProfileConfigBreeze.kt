package me.weishu.kernelsu.ui.screen.appprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedSwitchItem

@Composable
fun AppProfileConfigBreeze(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    profile: Natives.Profile,
    onProfileChange: (Natives.Profile) -> Unit,
) {
    SegmentedListGroup(modifier = modifier) {
        item(key = "umountModules") {
            SegmentedSwitchItem(
                title = stringResource(R.string.profile_umount_modules),
                summary = stringResource(R.string.profile_umount_modules_summary),
                enabled = enabled,
                checked = if (enabled) {
                    profile.umountModules
                } else {
                    Natives.isDefaultUmountModules()
                },
                onCheckedChange = {
                    onProfileChange(
                        profile.copy(
                            umountModules = it,
                            nonRootUseDefault = false
                        )
                    )
                },
            )
        }
    }
}
