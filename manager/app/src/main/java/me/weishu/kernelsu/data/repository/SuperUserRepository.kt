package me.weishu.kernelsu.data.repository

import me.weishu.kernelsu.data.model.AppInfo

interface SuperUserRepository {
    /**
     * The last app scan this process performed, shared by every screen. The scan binds the
     * root service and walks every package, so doing it per screen cost seconds on a cold start.
     */
    val apps: List<AppInfo>

    /**
     * Scans installed packages, joining a scan already in flight. [useCache] returns the
     * previous result when one exists; false when a screen must see newly installed apps.
     */
    suspend fun loadApps(useCache: Boolean): Result<Pair<List<AppInfo>, List<Int>>>

    /** Starts a scan in the background if none is running, so the first screen finds it warm. */
    fun prefetchApps()

    suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>>
}
