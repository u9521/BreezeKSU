package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import me.weishu.kernelsu.ksuApp

private const val WEBUI_ACTIVITY = "me.weishu.kernelsu.ui.webui.WebUIActivity"

/**
 * The one way to open a module WebUI, usable from anywhere above the WebUI host package.
 *
 * The activity is named by component, so `ui/screen` need not import `ui/webui`; the layer gate
 * forbids that edge. The module id lives in the URI because `documentLaunchMode="intoExisting"`
 * keys a task on the intent data, so two modules get two tasks and reopening reuses its task.
 */
fun createWebuiIntent(context: Context, moduleId: String): Intent {
    val canonicalUri = Uri.Builder()
        .scheme("ksu")
        .authority("webui")
        .appendQueryParameter("id", moduleId)
        .appendQueryParameter("token", ksuApp.settingsRepository.intentToken)
        .build()
    return Intent(Intent.ACTION_VIEW)
        .setComponent(android.content.ComponentName(context, WEBUI_ACTIVITY))
        .setData(canonicalUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
}
