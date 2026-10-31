package me.weishu.kernelsu.ui.screen.flash

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.text.ShellLogScaffold
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.keyDownFeedBack

@Composable
fun FlashScreenBreeze(
    state: FlashUiState,
    actions: FlashScreenActions,
) {
    // Mounted unconditionally so the dialog's exit animation plays when the warning is cleared.
    ConfirmDialogBreeze(
        body = rememberJailbreakFlashWarningBody(
            visible = state.showJailbreakWarning,
            onConfirm = actions.onConfirmJailbreakWarning,
            onDismiss = actions.onDismissJailbreakWarning,
        ),
        onConfirm = actions.onConfirmJailbreakWarning,
        onDismiss = actions.onDismissJailbreakWarning,
    )

    ShellLogScaffold(
        title = {
            Text(
                stringResource(
                    when (state.flashingStatus) {
                        FlashingStatus.FLASHING -> R.string.flashing
                        FlashingStatus.SUCCESS -> R.string.flash_success
                        FlashingStatus.FAILED -> R.string.flash_failed
                    },
                )
            )
        },
        text = state.text,
        onBack = actions.onBack,
        onSave = actions.onSaveLog,
        message = state.message,
        onMessageShown = actions.onMessageShown,
        fabLift = true,
        fabVisible = state.showRebootAction,
        floatingActionButton = {
            val keyFeedback = keyDownFeedBack()
            ExtendedFloatingActionButton(
                onClick = {
                    keyFeedback()
                    actions.onReboot()
                },
                icon = { Icon(MaterialSymbols.Filled.Rounded.Refresh, contentDescription = null) },
                text = { Text(stringResource(state.rebootLabelRes)) },
                modifier = Modifier
                    .animateFloatingActionButton(
                        state.showRebootAction,
                        alignment = Alignment.CenterEnd
                    ),
            )
        },
    )
}
