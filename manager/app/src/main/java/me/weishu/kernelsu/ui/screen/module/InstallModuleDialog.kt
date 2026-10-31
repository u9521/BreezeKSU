package me.weishu.kernelsu.ui.screen.module

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.ModuleParser
import me.weishu.kernelsu.data.ksu.getFileName
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.rememberConfirmDialog

@Composable
fun InstallModuleDialog(
    uris: List<Uri>,
    getInstalledModules: suspend () -> List<Module>,
    onConfirmInstall: (List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    if (uris.isEmpty()) return

    val context = LocalContext.current
    val confirmTitle = stringResource(R.string.module)
    val multiConfirmContent = stringResource(R.string.module_install_prompt_with_name)
    val confirmDialog = rememberConfirmDialog()

    LaunchedEffect(uris) {
        val single = uris.size == 1
        val shell = ConfirmBody(
            title = confirmTitle,
            onConfirm = {
                onConfirmInstall(uris)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        if (!single) {
            val moduleNames = uris.mapIndexed { index, uri ->
                "\n${index + 1}. ${uri.getFileName(context)}"
            }.joinToString("")
            confirmDialog.setConfirm(
                shell.copy(body = ConfirmBody.Body.Text(multiConfirmContent.format(moduleNames)))
            )
            return@LaunchedEffect
        }

        // Open in loading state first so one spinner covers both zip extraction and markdown parsing.
        confirmDialog.setConfirm(shell.copy(body = ConfirmBody.Body.Markdown(""), loading = true))

        val moduleList = getInstalledModules()
        val description = withContext(Dispatchers.IO) {
            ModuleParser.getModuleInstallDesc(context, uris.first(), moduleList)
        }
        confirmDialog.setConfirm(
            shell.copy(body = ConfirmBody.Body.Markdown(description), loading = false)
        )
    }
}
