package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.IKsuInterface
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.ksu.KsuCli
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.data.model.WEBVIEW_ZYGOTE_PROFILE_KEY
import me.weishu.kernelsu.data.model.WEBVIEW_ZYGOTE_UID
import me.weishu.kernelsu.data.service.KsuService
import me.weishu.kernelsu.ksuApp

class SuperUserRepositoryImpl : SuperUserRepository {

    companion object {
        private const val TAG = "SuperUserRepository"
    }

    private val appsLock = Any()
    private var cachedApps: List<AppInfo> = emptyList()
    private var cachedUserIds: List<Int> = emptyList()

    /**
     * The scan in flight, if any.
     *
     * Callers join it rather than start a second one: two screens opening at once would
     * otherwise bind the root service twice and parse the package list twice.
     */
    private var appListScan: Deferred<Result<Pair<List<AppInfo>, List<Int>>>>? = null
    private val scanScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val apps: List<AppInfo>
        get() = synchronized(appsLock) { cachedApps }

    override suspend fun loadApps(useCache: Boolean): Result<Pair<List<AppInfo>, List<Int>>> {
        val scan = synchronized(appsLock) {
            if (useCache && cachedApps.isNotEmpty()) {
                return Result.success(cachedApps to cachedUserIds)
            }
            appListScan?.takeIf { it.isActive } ?: scanScope.async {
                getAppList()
                    .onSuccess { (apps, ids) ->
                        synchronized(appsLock) {
                            cachedApps = apps
                            cachedUserIds = ids
                        }
                    }
                    .onFailure { Log.e(TAG, "loadApps failed", it) }
            }.also { appListScan = it }
        }
        return scan.await()
    }

    override fun prefetchApps() {
        synchronized(appsLock) {
            if (cachedApps.isNotEmpty()) return
            if (appListScan?.isActive == true) return
            appListScan = scanScope.async {
                getAppList()
                    .onSuccess { (apps, ids) ->
                        synchronized(appsLock) {
                            cachedApps = apps
                            cachedUserIds = ids
                        }
                    }
                    .onFailure { Log.e(TAG, "prefetchApps failed", it) }
            }
        }
    }

    private suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!KsuCli.SHELL.isRoot) {
                    return@withContext Result.failure(
                        IllegalStateException("Root access is required")
                    )
                }

                val result = connectKsuService {
                    Log.w(TAG, "KsuService disconnected")
                }

                var currentBinder = result.first
                var currentConnection = result.second

                try {
                    suspend fun reconnect(): IKsuInterface {
                        withContext(Dispatchers.Main) {
                            RootService.unbind(currentConnection)
                        }
                        val retry = connectKsuService { Log.w(TAG, "KsuService disconnected") }
                        currentBinder = retry.first
                        currentConnection = retry.second
                        return IKsuInterface.Stub.asInterface(currentBinder)
                    }

                    val pm = ksuApp.packageManager
                    val start = SystemClock.elapsedRealtime()

                    var iface = IKsuInterface.Stub.asInterface(currentBinder)
                    val idsArray = try {
                        iface.userIds
                    } catch (_: Exception) {
                        iface = reconnect()
                        iface.userIds
                    }

                    val slice = try {
                        iface.getPackages(0)
                    } catch (_: Exception) {
                        iface = reconnect()
                        iface.getPackages(0)
                    }

                    val packages = slice.list
                    val newApps = packages.filter {
                        val ai = it.applicationInfo ?: return@filter false
                        ai.uid != WEBVIEW_ZYGOTE_UID &&
                                (ai.flags and ApplicationInfo.FLAG_HAS_CODE) != 0
                    }.map {
                        val appInfo = it.applicationInfo!!
                        val profile = Natives.getAppProfile(it.packageName, appInfo.uid)
                        AppInfo(
                            label = appInfo.loadLabel(pm).toString(),
                            packageInfo = it,
                            profile = profile,
                        )
                    }.toMutableList()

                    // WebView Zygote is a single system UID, not a per-user package. Reuse the system icon.
                    val systemInfo = ApplicationInfo(pm.getApplicationInfo("android", 0)).apply {
                        uid = WEBVIEW_ZYGOTE_UID
                    }
                    val placeholder = PackageInfo().apply {
                        packageName = ""
                        applicationInfo = systemInfo
                    }
                    newApps += AppInfo(
                        label = "WebView Zygote",
                        packageInfo = placeholder,
                        profile = Natives.getAppProfile(
                            WEBVIEW_ZYGOTE_PROFILE_KEY,
                            WEBVIEW_ZYGOTE_UID
                        ),
                        profileKey = WEBVIEW_ZYGOTE_PROFILE_KEY,
                        special = true,
                    )

                    Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}")
                    Pair(newApps, idsArray.toList())
                } finally {
                    withContext(Dispatchers.Main) {
                        RootService.unbind(currentConnection)
                    }
                }
            }
        }

    override suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (currentApps.isEmpty()) return@runCatching emptyList()

                currentApps.map {
                    val profile = Natives.getAppProfile(it.profileKey, it.uid)
                    it.copy(profile = profile)
                }.also { updated ->
                    synchronized(appsLock) { cachedApps = updated }
                }
            }
        }

    private suspend inline fun connectKsuService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnect()
                }

                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (cont.isActive) {
                        cont.resume(binder as IBinder to this)
                    }
                }
            }

            cont.invokeOnCancellation {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    RootService.unbind(connection)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        RootService.unbind(connection)
                    }
                }
            }

            val intent = Intent(ksuApp, KsuService::class.java)

            val task = RootService.bindOrTask(
                intent,
                Shell.EXECUTOR,
                connection,
            )
            val shell = KsuCli.SHELL
            task?.let { shell.execTask(it) }
        }
    }
}
