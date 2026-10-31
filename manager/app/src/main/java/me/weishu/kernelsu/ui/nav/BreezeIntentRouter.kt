package me.weishu.kernelsu.ui.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import kotlinx.coroutines.channels.ReceiveChannel
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.data.service.DownloadService
import me.weishu.kernelsu.ksuApp

private const val SCHEME_KSU = "ksu"
private const val HOST_ACTION = "action"
private const val HOST_WEBUI = "webui"
private const val PARAM_ID = "id"
private const val PARAM_TOKEN = "token"

/**
 * Breeze's intent routing.
 *
 * Mirrors the upstream dispatcher's logic (upstream keeps its resolver private, so it cannot be
 * called from here) but pushes into Breeze's own navigation graph. The intent contract itself -
 * `ksu://` deep links, zip handling, the download service token - is upstream's and unchanged.
 *
 * Nothing here opens a dialog or an activity: a zip that needs confirmation is reported through
 * `onRequestInstallModule`, and a WebUI link through `onOpenWebUI`. Both hosts live in the
 * composition root, which is the only layer allowed to know about screens.
 */
private sealed interface BreezePendingAction {
    data class InstallModule(
        val uri: Uri,
        val requiresConfirmation: Boolean,
    ) : BreezePendingAction

    data class ExecuteAction(val moduleId: String) : BreezePendingAction

    data class OpenWebUI(val uri: Uri) : BreezePendingAction
}

private sealed interface KsuDeepLink {
    data class Action(val moduleId: String) : KsuDeepLink
    data class WebUi(val moduleId: String) : KsuDeepLink
}

private fun buildInternalWebUiUri(moduleId: String): Uri {
    return Uri.Builder()
        .scheme(SCHEME_KSU)
        .authority(HOST_WEBUI)
        .appendQueryParameter(PARAM_ID, moduleId)
        .build()
}

private fun parseValidatedDeepLink(uri: Uri?): KsuDeepLink? {
    if (uri?.scheme != SCHEME_KSU) return null

    val moduleId = uri.getQueryParameter(PARAM_ID)?.takeIf { it.isNotBlank() } ?: return null
    val token = uri.getQueryParameter(PARAM_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
    if (token != ksuApp.settingsRepository.intentToken) return null

    return when (uri.host) {
        HOST_ACTION -> KsuDeepLink.Action(moduleId)
        HOST_WEBUI -> KsuDeepLink.WebUi(moduleId)
        else -> null
    }
}

private fun resolveIntent(intent: Intent): BreezePendingAction? {
    // DownloadService notification: install module
    if (intent.action == DownloadService.ACTION_INSTALL_MODULE) {
        val token = intent.getStringExtra(DownloadService.EXTRA_TOKEN)?.takeIf { it.isNotBlank() }
            ?: return null
        if (token != ksuApp.settingsRepository.intentToken) return null
        val uriString = intent.getStringExtra(DownloadService.EXTRA_MODULE_URI) ?: return null
        return BreezePendingAction.InstallModule(
            uri = uriString.toUri(),
            requiresConfirmation = true,
        )
    }

    // File manager: open ZIP
    val viewUri = intent.data
    if (viewUri != null && viewUri.scheme == "content" && intent.type == "application/zip") {
        return BreezePendingAction.InstallModule(
            uri = viewUri,
            requiresConfirmation = true,
        )
    }

    return when (val deepLink = parseValidatedDeepLink(intent.data)) {
        is KsuDeepLink.Action -> BreezePendingAction.ExecuteAction(deepLink.moduleId)
        is KsuDeepLink.WebUi -> BreezePendingAction.OpenWebUI(buildInternalWebUiUri(deepLink.moduleId))
        null -> null
    }
}

/**
 * Collects incoming [Intent]s and routes them.
 *
 * [onRequestInstallModule] receives a zip that needs confirmation; [onOpenWebUI] receives the
 * internal `ksu://webui` uri of a module whose WebUI should open.
 */
@SuppressLint("StringFormatInvalid")
@Composable
fun BreezeIntentRouter(
    intentChannel: ReceiveChannel<Intent>,
    onOpenWebUI: (Uri) -> Unit,
    onRequestInstallModule: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalBreezeNavigator.current
    val isSafeMode = Natives.isSafeMode
    val isManager = Natives.isManager

    LaunchedEffect(intentChannel) {
        for (intent in intentChannel) {
            if (!isManager) continue
            when (val action = resolveIntent(intent)) {
                is BreezePendingAction.InstallModule -> {
                    if (isSafeMode) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.safe_mode_module_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                        continue
                    }
                    if (action.requiresConfirmation) {
                        onRequestInstallModule(action.uri)
                    } else {
                        navigator.push(BreezeRoute.Flash(FlashIt.FlashModules(listOf(action.uri))))
                    }
                }

                is BreezePendingAction.ExecuteAction -> {
                    navigator.push(
                        BreezeRoute.ExecuteModuleAction(
                            action.moduleId,
                            fromShortcut = true
                        )
                    )
                }

                is BreezePendingAction.OpenWebUI -> onOpenWebUI(action.uri)

                null -> Unit
            }
        }
    }
}
