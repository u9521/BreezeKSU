package me.weishu.kernelsu.ui.screen.templateeditor

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.deleteAppProfileTemplate
import me.weishu.kernelsu.data.model.TemplateInfo
import me.weishu.kernelsu.toOrdinalList
import me.weishu.kernelsu.toRootProfileFlags
import me.weishu.kernelsu.ui.util.BreezeFormState
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator

/**
 * Breeze's app profile template editor: owns the editing state/action wiring so the
 * upstream template editor screen does not have to know about Breeze.
 */
@Composable
fun TemplateEditorEntry(template: TemplateInfo, readOnly: Boolean) {
    val navigator = LocalBreezeNavigator.current
    val context = LocalContext.current
    val isCreation = template.id.isBlank()

    var currentTemplate by rememberSaveable { mutableStateOf(template) }
    val formState = remember { BreezeFormState() }

    val saveTemplateFailed = stringResource(id = R.string.app_profile_template_save_failed)

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun finishEditing() {
        if (!readOnly) {
            navigator.setResult("template_edit", true)
        } else {
            navigator.pop()
        }
    }

    // Breeze always saves explicitly from the app bar, so edits are never auto-saved.
    fun updateTemplate(updatedTemplate: TemplateInfo) {
        currentTemplate = updatedTemplate
    }

    val uiState = TemplateEditorUiState(
        template = currentTemplate,
        initialTemplate = template,
        readOnly = readOnly,
        isCreation = isCreation,
    )

    fun saveCurrentTemplate() {
        // The focused field's draft only reaches currentTemplate through this flush: clearing focus
        // does not run the focus callback inside the click handler.
        if (!formState.commitAll()) {
            showToast(saveTemplateFailed)
            return
        }
        if (saveTemplate(currentTemplate, isCreation)) {
            navigator.setResult("template_edit", true)
        } else {
            showToast(saveTemplateFailed)
        }
    }

    val actions = TemplateEditorActions(
        onBack = dropUnlessResumed { finishEditing() },
        onDelete = {
            if (deleteAppProfileTemplate(currentTemplate.id)) {
                navigator.setResult("template_edit", true)
            }
        },
        onSave = ::saveCurrentTemplate,
        onNameChange = { value ->
            updateTemplate(currentTemplate.copy(name = value))
        },
        onIdChange = { value ->
            currentTemplate = currentTemplate.copy(id = value)
        },
        onAuthorChange = { value ->
            updateTemplate(currentTemplate.copy(author = value))
        },
        onDescriptionChange = { value ->
            updateTemplate(currentTemplate.copy(description = value))
        },
        onProfileChange = { profile ->
            updateTemplate(
                currentTemplate.copy(
                    uid = profile.uid,
                    gid = profile.gid,
                    groups = profile.groups,
                    capabilities = profile.capabilities,
                    context = profile.context,
                    namespace = profile.namespace,
                    flags = profile.flags.toRootProfileFlags().toOrdinalList(),
                )
            )
        },
    )

    TemplateEditorScreenBreeze(
        state = uiState,
        actions = actions,
        formState = formState,
    )
}
