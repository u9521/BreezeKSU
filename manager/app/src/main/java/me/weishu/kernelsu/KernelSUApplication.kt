package me.weishu.kernelsu

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import android.system.Os
import java.io.File
import java.util.Locale
import me.weishu.kernelsu.data.repository.ModuleRepository
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl
import me.weishu.kernelsu.data.repository.TemplateRepository
import me.weishu.kernelsu.data.repository.TemplateRepositoryImpl
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass

lateinit var ksuApp: KernelSUApplication

/**
 * Process-wide dependency holder.
 *
 * Each repository is created once and handed out by property, because a second instance would
 * read the same preference file into its own state flow and the two would drift apart. Screens
 * therefore take `ksuApp.settingsRepository` (or a constructor default of it) rather than
 * constructing a repository themselves.
 */
class KernelSUApplication : Application() {

    companion object {
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val applicationInfoClass = ApplicationInfo::class.java
                val method = applicationInfoClass.getDeclaredMethod(
                    "setEnableOnBackInvokedCallback",
                    Boolean::class.javaPrimitiveType
                )
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    lateinit var okhttpClient: OkHttpClient

    /** Single source of persisted settings; every screen reads this one flow. */
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(this) }

    val superUserRepository: SuperUserRepository by lazy { SuperUserRepositoryImpl() }

    val moduleRepository: ModuleRepository by lazy { ModuleRepositoryImpl() }

    val templateRepository: TemplateRepository by lazy { TemplateRepositoryImpl() }

    private fun isUserUnlocked(): Boolean =
        getSystemService(UserManager::class.java)?.isUserUnlocked == true

    override fun onCreate() {
        super.onCreate()
        ksuApp = this

        if (!isUserUnlocked()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val enable = settingsRepository.settings.value.enablePredictiveBack
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }

        // Warm the app scan before the first SuperUser screen asks for it: the pass over
        // every installed package is what makes that screen slow on a cold start.
        superUserRepository.prefetchApps()

        val webroot = File(dataDir, "webroot")
        if (!webroot.exists()) {
            webroot.mkdir()
        }

        // Provide working env for rust's temp_dir()
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "KernelSU/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }
}
