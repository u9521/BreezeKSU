package me.weishu.kernelsu.ui.webui.webview.asset

import android.util.Log
import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader.PathHandler
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import me.weishu.kernelsu.ui.webui.util.MimeUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

private const val DEFAULT_MIME_TYPE = "text/plain"
private const val TAG = "SuFilePathHandler"
private val FORBIDDEN_DATA_DIRS = arrayOf("/data/data", "/data/system")

/**
 * Serves a module webroot over the asset loader, reading through a root shell.
 *
 * Two synthetic stylesheets live here rather than as files: `internal/insets.css`, which declares
 * the safe-area variables in `env()` so the page decides whether to use them, and
 * `internal/colors.css`, the palette published by [MonetColorsProvider].
 */
class SuFilePathHandler(
    webRoot: File,
    private val shell: Shell,
    private val onInsetsRequested: (Boolean) -> Unit,
) : PathHandler {
    private val directory: File = runCatching { File(getCanonicalDirPath(webRoot)) }
        .getOrElse {
            throw IllegalArgumentException(
                "Failed to resolve the canonical path for the given directory: ${webRoot.path}", it
            )
        }

    init {
        require(isAllowedInternalStorageDir()) {
            "The given directory \"$webRoot\" doesn't exist under an allowed app internal storage directory"
        }
    }

    @WorkerThread
    override fun handle(path: String): WebResourceResponse {
        if (path == "internal/insets.css") {
            onInsetsRequested(true)
            return cssResponse(INSETS_CSS)
        }
        if (path == "internal/colors.css") {
            // Always served. The palette used to be gated on upstream's `color_mode`/`ui_mode`
            // pair, but this fork has one renderer and `ui_mode` has no writer left, so the
            // condition could only ever be true - a switch that implies a choice not there.
            return cssResponse(MonetColorsProvider.getColorsCss())
        }

        return try {
            val file = getCanonicalFileIfChild(directory, path)
            if (file == null) {
                Log.e(
                    TAG,
                    "The requested file: $path is outside the mounted directory: $directory",
                )
                WebResourceResponse(null, null, null)
            } else {
                WebResourceResponse(guessMimeType(path), null, openFile(file, shell))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error opening the requested path: $path", e)
            WebResourceResponse(null, null, null)
        }
    }

    private fun cssResponse(css: String) =
        WebResourceResponse(
            "text/css", "utf-8", ByteArrayInputStream(css.toByteArray(StandardCharsets.UTF_8))
        )

    private fun isAllowedInternalStorageDir(): Boolean {
        val dir = getCanonicalDirPath(directory)
        return FORBIDDEN_DATA_DIRS.none { dir.startsWith(it) }
    }

    private fun getCanonicalDirPath(file: File): String {
        var canonicalPath = file.canonicalPath
        if (!canonicalPath.endsWith("/")) canonicalPath += "/"
        return canonicalPath
    }

    private fun getCanonicalFileIfChild(parent: File, child: String): File? {
        val parentCanonicalPath = getCanonicalDirPath(parent)
        val childCanonicalPath = File(parent, child).canonicalPath
        return if (childCanonicalPath.startsWith(parentCanonicalPath)) File(childCanonicalPath) else null
    }

    private fun openFile(file: File, shell: Shell): InputStream {
        val suFile = SuFile(file.absolutePath)
        suFile.shell = shell
        val fis = SuFileInputStream.open(suFile)
        return if (file.path.endsWith(".svgz")) GZIPInputStream(fis) else fis
    }

    private fun guessMimeType(filePath: String): String =
        MimeUtil.getMimeFromFileName(filePath) ?: DEFAULT_MIME_TYPE

    companion object {
        /**
         * Safe-area variables for pages that opt into edge-to-edge. The WebView resolves
         * `env()` itself, so the values stay correct without the host pushing numbers.
         */
        private val INSETS_CSS = """
            :root {
              --safe-area-inset-top: env(safe-area-inset-top, 0px);
              --safe-area-inset-right: env(safe-area-inset-right, 0px);
              --safe-area-inset-bottom: env(safe-area-inset-bottom, 0px);
              --safe-area-inset-left: env(safe-area-inset-left, 0px);
              --window-inset-top: var(--safe-area-inset-top);
              --window-inset-bottom: var(--safe-area-inset-bottom);
              --window-inset-left: var(--safe-area-inset-left);
              --window-inset-right: var(--safe-area-inset-right);
              --f7-safe-area-top: var(--window-inset-top) !important;
              --f7-safe-area-bottom: var(--window-inset-bottom) !important;
              --f7-safe-area-left: var(--window-inset-left) !important;
              --f7-safe-area-right: var(--window-inset-right) !important;
            }
        """.trimIndent()
    }
}
