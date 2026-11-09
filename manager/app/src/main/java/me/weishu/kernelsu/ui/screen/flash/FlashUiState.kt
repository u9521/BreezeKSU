package me.weishu.kernelsu.ui.screen.flash

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.util.UiMessage

@Immutable
data class FlashUiState(
    val text: String,
    val showRebootAction: Boolean,
    val flashingStatus: FlashingStatus,
    val showJailbreakWarning: Boolean,
    @param:StringRes val rebootLabelRes: Int,
    /** One-shot feedback shown by the scaffold; [FlashScreenActions.onMessageShown] clears it. */
    val message: UiMessage? = null,
)

@Immutable
data class FlashScreenActions(
    val onBack: () -> Unit,
    val onSaveLog: () -> Unit,
    val onReboot: () -> Unit,
    val onConfirmJailbreakWarning: () -> Unit,
    val onDismissJailbreakWarning: () -> Unit,
    val onMessageShown: (Long) -> Unit,
)
