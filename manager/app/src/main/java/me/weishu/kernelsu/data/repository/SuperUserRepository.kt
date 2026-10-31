package me.weishu.kernelsu.data.repository

import me.weishu.kernelsu.data.model.AppInfo

interface SuperUserRepository {
    /**
     * The last app scan this process performed, newest first-read value, shared by every screen.
     *
     * The scan binds the root service and walks every installed package, so doing it once per
     * screen is what made opening a SuperUser page cost seconds on a cold start.
     */
    val apps: List<AppInfo>

    /**
     * Scans installed packages, joining a scan already in flight instead of starting a second one.
     *
     * [useCache] returns the previous result when one exists; it is false when a screen has to
     * see apps installed since that scan.
     */
    suspend fun loadApps(useCache: Boolean): Result<Pair<List<AppInfo>, List<Int>>>

    /** Starts a scan in the background if none is running, so the first screen finds it warm. */
    fun prefetchApps()

    suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>>
}
