package me.weishu.kernelsu.ui.webui.util

import android.content.Context
import androidx.webkit.WebViewCompat

/**
 * Which WebView feature the host may rely on.
 *
 * One boundary, and it only governs the *safe-area* CSS: below it, only fullscreen WebViews
 * received `env(safe-area-inset-*)`.
 *
 * The IME is not covered here. Chromium shrinks its own visual viewport from the IME insets, so
 * the host pads for it only on the branch where the page did not ask for the layout itself.
 */
object WebViewVersionUtil {
    const val MIN_INSETS_VERSION = 144
    const val DEFAULT_WEBVIEW_PACKAGE = "com.google.android.webview"

    fun getWebViewMajorVersion(context: Context): Int {
        val pkg =
            runCatching { WebViewCompat.getCurrentWebViewPackage(context) }.getOrNull() ?: return 0
        return pkg.versionName?.substringBefore(".")?.toIntOrNull() ?: 0
    }

    fun getWebViewPackageName(context: Context): String {
        val pkg = runCatching { WebViewCompat.getCurrentWebViewPackage(context) }.getOrNull()
        return pkg?.packageName ?: DEFAULT_WEBVIEW_PACKAGE
    }

    fun supportInsets(majorVersion: Int) = majorVersion >= MIN_INSETS_VERSION
}
