package me.weishu.kernelsu.ui.webui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.topjohnwu.superuser.Shell
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.createRootShell
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.loadModuleIconBitmap
import me.weishu.kernelsu.ui.webui.WebUIDownloadInterface
import me.weishu.kernelsu.ui.webui.WebUIRuntime
import me.weishu.kernelsu.ui.webui.WebViewInterface
import me.weishu.kernelsu.ui.webui.model.ModuleInfo
import me.weishu.kernelsu.ui.webui.model.WebUILoadState
import me.weishu.kernelsu.ui.webui.util.WebViewVersionUtil
import me.weishu.kernelsu.ui.webui.util.setTaskDescription
import me.weishu.kernelsu.ui.webui.viewmodel.WebUIViewModel
import me.weishu.kernelsu.ui.webui.webview.asset.SuFilePathHandler

/**
 * Validates the module, opens the root shell, and builds a page-ready WebView. Everything the
 * page can reach is wired here, so the clients and the JS interfaces stay unaware of each
 * other. Failures go through [WebUIViewModel.notifyError].
 */
internal suspend fun prepareWebView(
    activity: Activity,
    runtime: WebUIRuntime,
    viewModel: WebUIViewModel,
) {
    val moduleId = activity.intent.data?.getQueryParameter("id")
    if (moduleId == null) {
        viewModel.notifyError(activity.getString(R.string.webui_invalid_module_id))
        return
    }

    resolveWebViewVersion(activity, viewModel)

    val moduleInfo = checkModule(viewModel, moduleId, activity) ?: return
    viewModel.onModuleValidated(moduleInfo)

    val moduleDir = "/data/adb/modules/${moduleId}"

    val iconUri = moduleInfo.webUiIconPath?.takeIf { it.isNotBlank() }?.let { "su:$it" }
    val iconBitmap = withContext(Dispatchers.IO) { loadModuleIconBitmap(activity, iconUri) }
    activity.setTaskDescription(moduleInfo.name, iconBitmap)

    val shell = withContext(Dispatchers.IO) { createRootShell(true) }
    try {
        runtime.registerShell(shell)
    } catch (t: Throwable) {
        runCatching { shell.close() }
        throw t
    }

    val webView = createWebView(activity)
    val assetLoader = createAssetLoader(moduleDir, shell, viewModel)
    webView.webViewClient = WebUiClient(activity, assetLoader, runtime, viewModel)
    webView.webChromeClient = WebUiChromeClient(runtime, viewModel)

    val webViewInterface = WebViewInterface(runtime, moduleDir, viewModel)
    val downloadInterface = WebUIDownloadInterface(runtime)
    webView.addJavascriptInterface(webViewInterface, KSU_JS_INTERFACE_NAME)
    webView.addJavascriptInterface(downloadInterface, KSU_DOWNLOAD_JS_INTERFACE_NAME)
    webView.setDownloadListener { url, _, _, _, _ ->
        // Blob downloads are served by BlobDownloadHandler and must not be handed off.
        if (Uri.parse(url).host != INTERNAL_BLOB_DOWNLOAD_HOST) {
            downloadInterface.openExternal(url)
        }
    }

    runtime.registerInterfaces(webViewInterface, downloadInterface)
    runtime.attach(webView)
    viewModel.setLoadState(WebUILoadState.LoadingPage)
}

/**
 * Reads the WebView version once per screen. It only decides whether the page can rely on
 * safe-area `env()` values; the host never derives insets from it.
 */
private fun resolveWebViewVersion(activity: Activity, viewModel: WebUIViewModel) {
    val version = WebViewVersionUtil.getWebViewMajorVersion(activity)
    viewModel.onWebViewFeatureResolved(
        version = version,
        packageName = WebViewVersionUtil.getWebViewPackageName(activity),
        insetsSupported = WebViewVersionUtil.supportInsets(version),
    )
}

private suspend fun checkModule(
    viewModel: WebUIViewModel,
    moduleId: String,
    context: Context,
): ModuleInfo? {
    viewModel.setLoadState(WebUILoadState.LoadingModule)
    return withContext(Dispatchers.IO) {
        val moduleInfo = ksuApp.moduleRepository.getModules()
            .getOrDefault(emptyList())
            .find { info -> info.id == moduleId }

        when {
            moduleInfo == null -> {
                viewModel.notifyError(context.getString(R.string.no_such_module, moduleId))
                null
            }

            !moduleInfo.hasWebUi || !moduleInfo.enabled || moduleInfo.update || moduleInfo.remove -> {
                viewModel.notifyError(
                    context.getString(
                        R.string.module_unavailable,
                        moduleInfo.name
                    )
                )
                null
            }

            else -> ModuleInfo(moduleId, moduleInfo.name, moduleInfo.webUiIconPath)
        }
    }
}

private fun createAssetLoader(
    moduleDir: String,
    shell: Shell,
    viewModel: WebUIViewModel,
): WebViewAssetLoader {
    val webRoot = File("${moduleDir}/webroot")
    return WebViewAssetLoader.Builder()
        .setDomain(WEBUI_DOMAIN)
        .addPathHandler(
            "/",
            SuFilePathHandler(webRoot, shell) { enable ->
                viewModel.onEdgeToEdgeChanged(enable)
            }
        )
        .build()
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(activity: Activity): WebView {
    val webView = WebView(activity)
    webView.setBackgroundColor(Color.TRANSPARENT)

    WebView.setWebContentsDebuggingEnabled(ksuApp.settingsRepository.settings.value.enableWebDebugging)

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
    }
    return webView
}
