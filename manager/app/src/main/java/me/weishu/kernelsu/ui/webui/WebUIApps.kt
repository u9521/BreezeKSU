package me.weishu.kernelsu.ui.webui

import kotlinx.coroutines.runBlocking
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.ksuApp

/**
 * The installed-app list the WebUI sees, read through the cached repository: `ksu.pm()` and
 * the `ksu://icon/` handler are both called per request from the bridge thread.
 */
internal fun webUIApps(): List<AppInfo> = runBlocking {
    ksuApp.superUserRepository.loadApps(useCache = true).getOrNull()?.first.orEmpty()
}
