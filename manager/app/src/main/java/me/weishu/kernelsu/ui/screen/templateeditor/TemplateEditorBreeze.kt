package me.weishu.kernelsu.ui.screen.templateeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.button.SplitScreenRatioButton
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffoldScope
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.component.breeze.text.OutlinedTextEdit
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.screen.profile.RootProfileConfigBreeze
import me.weishu.kernelsu.ui.util.LocalIsDetailPane

@Composable
fun TemplateEditorScreenBreeze(
    state: TemplateEditorUiState,
    actions: TemplateEditorActions,
) {
    BreezeScaffold(
        topBar = {
            TopBar(
                title = if (state.isCreation) {
                    stringResource(R.string.app_profile_template_create)
                } else if (state.readOnly) {
                    stringResource(R.string.app_profile_template_view)
                } else {
                    stringResource(R.string.app_profile_template_edit)
                },
                readOnly = state.readOnly,
                onBack = actions.onBack,
                onDelete = actions.onDelete,
                onSave = actions.onSave,
            )
        },
    ) { innerPadding ->
        // Hoisted: `validator` below is a plain lambda, so it cannot call stringResource itself.
        val idConflictError = stringResource(id = R.string.app_profile_template_id_exist)
        val idInvalidError = stringResource(id = R.string.app_profile_template_id_invalid)
        val idTooLongError = stringResource(id = R.string.app_profile_template_id_too_long)
        val keyboardController = LocalSoftwareKeyboardController.current

        BreezeScrollColumn(
            innerPadding = innerPadding,
            modifier = Modifier.imePadding(),
            topExtraSpacing = 16.dp,
            // OutlinedTextEdit already reserves a 16.dp supportingText slot below every field.
            verticalArrangement = Arrangement.Top,
        ) {
            OutlinedTextEdit(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.app_profile_template_id)) },
                text = state.template.id,
                readOnly = !state.isCreation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() },
                ),
                validator = { input ->
                    when {
                        isTemplateExist(input) -> idConflictError
                        !isValidTemplateId(input) -> idInvalidError
                        input.length > 255 -> idTooLongError
                        else -> null
                    }
                },
                onValueChange = actions.onIdChange,
            )

            OutlinedTextEdit(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.app_profile_template_name)) },
                text = state.template.name,
                readOnly = state.readOnly,
                onValueChange = actions.onNameChange,
            )

            OutlinedTextEdit(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.module_author)) },
                text = state.template.author,
                readOnly = state.readOnly,
                onValueChange = actions.onAuthorChange,
            )

            OutlinedTextEdit(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.app_profile_template_description)) },
                text = state.template.description,
                readOnly = state.readOnly,
                singleLine = false,
                onValueChange = actions.onDescriptionChange,
            )

            RootProfileConfigBreeze(
                readOnly = state.readOnly,
                profile = toNativeProfile(state.template),
                onProfileChange = actions.onProfileChange,
            )
        }
    }
}

@Composable
private fun BreezeScaffoldScope.TopBar(
    title: String,
    readOnly: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    BreezeTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (LocalIsDetailPane.current) return@BreezeTopAppBar
            BreezeBackButton(
                onClick = onBack,
                collapseFraction = scrollBehavior.state.collapsedFraction,
            )
        },
        actions = {
            SplitScreenRatioButton()
            if (readOnly) return@BreezeTopAppBar
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = MaterialSymbols.Filled.Rounded.DeleteForever,
                    contentDescription = stringResource(id = R.string.app_profile_template_delete),
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = MaterialSymbols.Filled.Rounded.Save,
                    contentDescription = stringResource(id = R.string.app_profile_template_save),
                )
            }
        },
    )
}
