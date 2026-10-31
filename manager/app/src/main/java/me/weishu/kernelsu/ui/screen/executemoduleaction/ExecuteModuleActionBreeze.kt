package me.weishu.kernelsu.ui.screen.executemoduleaction

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.text.ShellLogScaffold
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.keyDownFeedBack

@Composable
fun ExecuteModuleActionScreenBreeze(
    state: ExecuteModuleActionUiState,
    actions: ExecuteModuleActionScreenActions,
) {
    val keyFeedback = keyDownFeedBack()
    BackHandler(enabled = !state.isComplete) { }
    ShellLogScaffold(
        title = { Text(stringResource(R.string.action)) },
        text = state.text,
        onBack = actions.onBack,
        onSave = actions.onSaveLog,
        message = state.message,
        onMessageShown = actions.onMessageShown,
        fabLift = true,
        fabVisible = state.isComplete,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    keyFeedback()
                    actions.onClose()
                },
                icon = { Icon(MaterialSymbols.Filled.Rounded.Close, null) },
                text = { Text(stringResource(R.string.close)) },
                modifier = Modifier
                    .animateFloatingActionButton(state.isComplete, alignment = Alignment.CenterEnd),
            )
        },
    )
}
