package me.weishu.kernelsu.ui.webui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * The WebUI host state that must outlive recomposition but cannot live in a ViewModel.
 *
 * A [WebView] is bound to the Activity that created it and leaks through a ViewModel, so the
 * view, the shells it opened and the pending file-chooser callback are kept here.
 */
@Stable
class WebUIRuntime {
    var webView: WebView? by mutableStateOf(null)
        private set

    private val fileChooserRequestId = AtomicLong(0L)
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingFileChooserIntent: Intent? = null
    private val fileCallbackLock = Any()

    private val activeShells = ConcurrentHashMap.newKeySet<Shell>()
    private var viewInterface: WebViewInterface? = null
    private var downloadInterface: WebUIDownloadInterface? = null

    /** Owns the teardown work that must not block the main thread. Cancelled by [dispose]. */
    private val disposeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Blob downloads for this window. The download interface registers them and the WebView
     * client serves the internal URLs, so both have to see the same instance.
     */
    val blobs = BlobDownloadHandler()

    fun attach(view: WebView) {
        disposeWebView()
        webView = view
    }

    fun registerShell(shell: Shell) {
        activeShells.add(shell)
    }

    fun unregisterShell(shell: Shell) {
        activeShells.remove(shell)
    }

    fun registerInterfaces(view: WebViewInterface, download: WebUIDownloadInterface) {
        viewInterface = view
        downloadInterface = download
    }

    /**
     * Records the chooser the page is blocked on. A newer request supersedes an older one: the
     * older callback is answered with null instead of being leaked, or its page hangs forever.
     */
    fun beginFileChooser(intent: Intent, callback: ValueCallback<Array<Uri>>?): Long {
        synchronized(fileCallbackLock) {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = callback
            pendingFileChooserIntent = intent
            return fileChooserRequestId.incrementAndGet()
        }
    }

    fun takeFileChooserIntent(requestId: Long): Intent? {
        synchronized(fileCallbackLock) {
            return if (requestId == fileChooserRequestId.get()) pendingFileChooserIntent else null
        }
    }

    fun consumePendingFileCallback(requestId: Long, uris: Array<Uri>?) {
        synchronized(fileCallbackLock) {
            if (requestId == fileChooserRequestId.get()) {
                pendingFileCallback?.onReceiveValue(uris)
                pendingFileCallback = null
                pendingFileChooserIntent = null
            }
        }
    }

    /**
     * Drops the current view without tearing the rest of the runtime down.
     *
     * Returning true from onRenderProcessGone promises the view is already discarded, and
     * touching it afterwards kills the process, so it is detached here; the shells and the
     * file-chooser callback wait for [dispose].
     */
    fun detachAndDestroy() {
        disposeWebView()
    }

    private fun disposeWebView() {
        webView?.let { view ->
            view.stopLoading()
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        webView = null
    }

    fun dispose() {
        synchronized(fileCallbackLock) {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            pendingFileChooserIntent = null
        }
        viewInterface?.destroy()
        viewInterface = null
        downloadInterface?.destroy()
        downloadInterface = null
        disposeWebView()
        val shellsToClose = activeShells.toSet()
        activeShells.clear()
        if (shellsToClose.isNotEmpty()) {
            // Shell.close() blocks on the process it started, so it cannot run on the main
            // thread. An IO coroutine has the same threading and one owner for the work.
            disposeScope.launch {
                shellsToClose.forEach { shell -> runCatching { shell.close() } }
            }.invokeOnCompletion { disposeScope.cancel() }
        } else {
            disposeScope.cancel()
        }
    }
}
