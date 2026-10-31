package me.weishu.kernelsu.ui.screen.template

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.isNetworkAvailable
import me.weishu.kernelsu.data.model.TemplateInfo
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.util.UiMessage

/**
 * Breeze's app profile template list: owns the state/action wiring so the upstream
 * template screen does not have to know about Breeze.
 */
@Composable
fun TemplateEntry() {
    val navigator = LocalBreezeNavigator.current
    val viewModel = viewModel<TemplateViewModel>()
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val requestKey = "template_edit"
    var message by remember { mutableStateOf<UiMessage?>(null) }

    LaunchedEffect(Unit) {
        if (screenState.templateList.isEmpty()) {
            viewModel.fetchTemplates()
        }
    }

    LaunchedEffect(Unit) {
        navigator.observeResult<Boolean>(requestKey).collect { success ->
            if (success) {
                navigator.clearResult(requestKey)
                viewModel.fetchTemplates()
            }
        }
    }

    val importEmptyText = stringResource(R.string.app_profile_template_import_empty)
    val importSuccessText = stringResource(R.string.app_profile_template_import_success)
    val exportEmptyText = stringResource(R.string.app_profile_template_export_empty)

    fun showMessage(text: String) {
        message = UiMessage(text = text)
    }

    val uiState = screenState.copy(
        offline = !isNetworkAvailable(context),
        message = message,
    )
    val actions = TemplateActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onRefresh = { forceSync ->
            scope.launch {
                viewModel.fetchTemplates(forceSync)
            }
        },
        onImport = {
            scope.launch {
                clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()
                    ?.let { templateText ->
                        if (templateText.isEmpty()) {
                            showMessage(importEmptyText)
                            return@let
                        }
                        viewModel.importTemplates(
                            templateText,
                            onSuccess = {
                                showMessage(importSuccessText)
                                viewModel.fetchTemplates(false)
                            },
                            onFailure = { showMessage(it) },
                        )
                    }
            }
        },
        onExport = {
            scope.launch {
                viewModel.exportTemplates(
                    onTemplateEmpty = {
                        showMessage(exportEmptyText)
                    },
                    callback = { templateText ->
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("template", templateText))
                        )
                    },
                )
            }
        },
        onCreateTemplate = {
            navigator.push(
                BreezeRoute.TemplateEditor(TemplateInfo(), false)
            )
        },
        onOpenTemplate = { template ->
            navigator.push(
                BreezeRoute.TemplateEditor(template, !template.local)
            )
        },
        onMessageShown = { message = null },
    )

    AppProfileTemplateScreenBreeze(
        state = uiState,
        actions = actions,
    )
}
