package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import android.os.PowerManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.reboot
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog

data class RebootListOption(
    @param:StringRes val labelRes: Int,
    val reason: String,
)

@Composable
fun getRebootListOption(): List<RebootListOption> {
    val pm = LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?

    @Suppress("DEPRECATION")
    val isRebootingUserspaceSupported = pm?.isRebootingUserspaceSupported == true

    return buildList {
        add(RebootListOption(R.string.reboot, ""))
        if (isRebootingUserspaceSupported) {
            add(RebootListOption(R.string.reboot_userspace, "userspace"))
        }
        add(RebootListOption(R.string.reboot_soft, "soft_reboot"))
        add(RebootListOption(R.string.reboot_recovery, "recovery"))
        add(RebootListOption(R.string.reboot_bootloader, "bootloader"))
        add(RebootListOption(R.string.reboot_download, "download"))
        add(RebootListOption(R.string.reboot_edl, "edl"))
    }
}

/**
 * Reboots on selection, confirming first in jailbreak mode where a plain reboot drops root.
 *
 * The pending confirmation is held in `rememberSaveable` so the dialog survives Activity
 * recreation on rotation.
 */
@Composable
fun rememberRebootAction(): (String) -> Unit {
    val title = stringResource(R.string.reboot)
    val message = stringResource(R.string.jailbreak_reboot_warning)
    val confirmDialog = rememberConfirmDialog()
    var awaitingConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(awaitingConfirm, title, message) {
        confirmDialog.setConfirm(
            if (awaitingConfirm) {
                ConfirmBody(
                    title = title,
                    body = ConfirmBody.Body.Text(message),
                    // Reboot without clearing awaitingConfirm first, which would cancel this effect.
                    onConfirm = { reboot() },
                    onDismiss = { awaitingConfirm = false },
                )
            } else {
                null
            }
        )
    }

    return { reason ->
        if (Natives.isLateLoadMode && reason.isEmpty()) {
            awaitingConfirm = true
        } else {
            reboot(reason)
        }
    }
}
