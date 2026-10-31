package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.topjohnwu.superuser.ShellUtils
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import me.weishu.kernelsu.KernelSUApplication
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.ColorMode
import me.weishu.kernelsu.data.UiSettings
import me.weishu.kernelsu.data.ksu.execKsud
import me.weishu.kernelsu.data.ksu.getFeaturePersistValue
import me.weishu.kernelsu.data.ksu.getFeatureStatus
import me.weishu.kernelsu.data.ksu.getFeatureValue
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.magica.BootCompletedReceiver

private const val SETTINGS_PREFS = "settings"
private const val KEY_USE_SOFT_REBOOT = "soft_reboot"

/** Range the page-scale slider offers; values outside it are clamped on read and write. */
private const val MIN_PAGE_SCALE = 0.8f
private const val MAX_PAGE_SCALE = 1.1f

/** Range the module description slider offers, clamped the same way. */
private const val MIN_MODULE_DESCRIPTION_LINES = 1
private const val MAX_MODULE_DESCRIPTION_LINES = 10

/** Prefer soft reboot: always in jailbreak mode, or when the setting is enabled. */
fun isSoftRebootPreferred(): Boolean =
    Natives.isLateLoadMode || ksuApp.settingsRepository.settings.value.useSoftReboot

/**
 * [SettingsRepository] backed by [SharedPreferences].
 *
 * Values are read once into a [MutableStateFlow] and re-published on every [update], so the UI
 * observes one source of truth instead of re-reading a preference on each recomposition. Writes
 * keep the enum values as names and the booleans as booleans - the same shapes earlier builds
 * wrote - so an installed app keeps its settings across the upgrade.
 */
class SettingsRepositoryImpl(
    private val context: Context = ksuApp,
) : SettingsRepository {

    private companion object {
        private const val TAG = "Settings"
        private const val INTENT_TOKEN_KEY = "intent_token"
        private val secureRandom = SecureRandom()
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    private val mutableSettings = MutableStateFlow(readFromPrefs())

    override val settings: StateFlow<UiSettings> = mutableSettings.asStateFlow()

    override fun update(transform: (UiSettings) -> UiSettings) {
        var previous: UiSettings? = null
        val updated = mutableSettings.updateAndGet { current ->
            previous = current
            transform(current).normalized()
        }
        val before = previous ?: return
        writeChanged(before, updated)
        applySideEffects(before, updated)
    }

    private fun UiSettings.normalized(): UiSettings = copy(
        pageScale = pageScale.coerceIn(MIN_PAGE_SCALE, MAX_PAGE_SCALE),
        moduleDescriptionMaxLines = moduleDescriptionMaxLines
            .coerceIn(MIN_MODULE_DESCRIPTION_LINES, MAX_MODULE_DESCRIPTION_LINES),
    )

    private fun readFromPrefs(): UiSettings = UiSettings(
        colorMode = ColorMode.fromValue(prefs.getInt(KEY_COLOR_MODE, ColorMode.SYSTEM.value)),
        keyColor = prefs.getInt(KEY_KEY_COLOR, 0),
        paletteStyle = prefs.enumOrDefault(KEY_PALETTE_STYLE, PaletteStyle.TonalSpot),
        colorSpec = prefs.enumOrDefault(KEY_COLOR_SPEC, ColorSpec.SpecVersion.SPEC_2025),
        enableBlur = prefs.getBoolean(KEY_ENABLE_BLUR, true),
        pageScale = prefs.getFloat(KEY_PAGE_SCALE, 1f)
            .coerceIn(MIN_PAGE_SCALE, MAX_PAGE_SCALE),
        enableNavigationBadge = prefs.getBoolean(KEY_ENABLE_NAVIGATION_BADGE, true),
        enablePredictiveBack = prefs.getBoolean(KEY_ENABLE_PREDICTIVE_BACK, false),
        moduleDescriptionMaxLines = prefs.getInt(KEY_MODULE_DESCRIPTION_MAX_LINES, 4)
            .coerceIn(MIN_MODULE_DESCRIPTION_LINES, MAX_MODULE_DESCRIPTION_LINES),
        checkUpdate = prefs.getBoolean(KEY_CHECK_UPDATE, true),
        checkModuleUpdate = prefs.getBoolean(KEY_MODULE_CHECK_UPDATE, true),
        enableWebDebugging = prefs.getBoolean(KEY_ENABLE_WEB_DEBUGGING, false),
        moduleSortEnabledFirst = prefs.getBoolean(KEY_MODULE_SORT_ENABLED_FIRST, false),
        moduleSortActionFirst = prefs.getBoolean(KEY_MODULE_SORT_ACTION_FIRST, false),
        superuserShowSystemApps = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false),
        superuserShowOnlyPrimaryUserApps = prefs.getBoolean(KEY_SHOW_ONLY_PRIMARY_USER_APPS, false),
        superuserSortOption = prefs.getInt(KEY_SUPERUSER_SORT_OPTION, 0),
        suLogFilters = prefs.getStringSet(KEY_SULOG_FILTERS, null)?.toSet(),
        autoJailbreak = prefs.getBoolean(KEY_AUTO_JAILBREAK, false),
        useSoftReboot = prefs.getBoolean(KEY_USE_SOFT_REBOOT, false),
    )

    /** Persists only what [transform] actually changed, so unrelated keys are never rewritten. */
    private fun writeChanged(previous: UiSettings, updated: UiSettings) {
        prefs.edit {
            if (previous.colorMode != updated.colorMode) {
                putInt(KEY_COLOR_MODE, updated.colorMode.value)
            }
            if (previous.keyColor != updated.keyColor) putInt(KEY_KEY_COLOR, updated.keyColor)
            if (previous.paletteStyle != updated.paletteStyle) {
                putString(KEY_PALETTE_STYLE, updated.paletteStyle.name)
            }
            if (previous.colorSpec != updated.colorSpec) {
                putString(KEY_COLOR_SPEC, updated.colorSpec.name)
            }
            if (previous.enableBlur != updated.enableBlur) {
                putBoolean(KEY_ENABLE_BLUR, updated.enableBlur)
            }
            if (previous.pageScale != updated.pageScale) {
                putFloat(KEY_PAGE_SCALE, updated.pageScale)
            }
            if (previous.enableNavigationBadge != updated.enableNavigationBadge) {
                putBoolean(KEY_ENABLE_NAVIGATION_BADGE, updated.enableNavigationBadge)
            }
            if (previous.enablePredictiveBack != updated.enablePredictiveBack) {
                putBoolean(KEY_ENABLE_PREDICTIVE_BACK, updated.enablePredictiveBack)
            }
            if (previous.moduleDescriptionMaxLines != updated.moduleDescriptionMaxLines) {
                putInt(KEY_MODULE_DESCRIPTION_MAX_LINES, updated.moduleDescriptionMaxLines)
            }
            if (previous.checkUpdate != updated.checkUpdate) {
                putBoolean(KEY_CHECK_UPDATE, updated.checkUpdate)
            }
            if (previous.checkModuleUpdate != updated.checkModuleUpdate) {
                putBoolean(KEY_MODULE_CHECK_UPDATE, updated.checkModuleUpdate)
            }
            if (previous.enableWebDebugging != updated.enableWebDebugging) {
                putBoolean(KEY_ENABLE_WEB_DEBUGGING, updated.enableWebDebugging)
            }
            if (previous.moduleSortEnabledFirst != updated.moduleSortEnabledFirst) {
                putBoolean(KEY_MODULE_SORT_ENABLED_FIRST, updated.moduleSortEnabledFirst)
            }
            if (previous.moduleSortActionFirst != updated.moduleSortActionFirst) {
                putBoolean(KEY_MODULE_SORT_ACTION_FIRST, updated.moduleSortActionFirst)
            }
            if (previous.superuserShowSystemApps != updated.superuserShowSystemApps) {
                putBoolean(KEY_SHOW_SYSTEM_APPS, updated.superuserShowSystemApps)
            }
            if (previous.superuserShowOnlyPrimaryUserApps != updated.superuserShowOnlyPrimaryUserApps) {
                putBoolean(
                    KEY_SHOW_ONLY_PRIMARY_USER_APPS,
                    updated.superuserShowOnlyPrimaryUserApps
                )
            }
            if (previous.superuserSortOption != updated.superuserSortOption) {
                putInt(KEY_SUPERUSER_SORT_OPTION, updated.superuserSortOption)
            }
            if (previous.suLogFilters != updated.suLogFilters) {
                putStringSet(KEY_SULOG_FILTERS, updated.suLogFilters)
            }
            if (previous.autoJailbreak != updated.autoJailbreak) {
                putBoolean(KEY_AUTO_JAILBREAK, updated.autoJailbreak)
            }
            if (previous.useSoftReboot != updated.useSoftReboot) {
                putBoolean(KEY_USE_SOFT_REBOOT, updated.useSoftReboot)
            }
        }
    }

    /**
     * Work a preference implies outside the preference file.
     *
     * Both cases are edges - they fire only when the value actually flipped - because the
     * receiver toggle kills and restarts a component, and the predictive-back flag lives on
     * [android.content.pm.ApplicationInfo], which the platform reads once per activity start.
     */
    private fun applySideEffects(previous: UiSettings, updated: UiSettings) {
        if (previous.autoJailbreak != updated.autoJailbreak) {
            runCatching {
                ksuApp.packageManager.setComponentEnabledSetting(
                    ComponentName(ksuApp, BootCompletedReceiver::class.java),
                    if (updated.autoJailbreak) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure {
                Log.e(TAG, "failed to change boot receiver state to ${updated.autoJailbreak}", it)
            }
        }

        if (previous.enablePredictiveBack != updated.enablePredictiveBack &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            KernelSUApplication.setEnableOnBackInvokedCallback(
                context.applicationInfo,
                updated.enablePredictiveBack,
            )
        }
    }

    override val intentToken: String
        get() {
            val existing = prefs.getString(INTENT_TOKEN_KEY, null)
            if (!existing.isNullOrBlank()) return existing
            val token = ByteArray(32).also(secureRandom::nextBytes)
                .joinToString(separator = "") { "%02x".format(it) }
            prefs.edit { putString(INTENT_TOKEN_KEY, token) }
            return token
        }

    override suspend fun getSuCompatStatus(): String = getFeatureStatus("su_compat")

    override suspend fun getSuCompatPersistValue(): Long? = getFeaturePersistValue("su_compat")

    override fun isSuEnabled(): Boolean = Natives.isSuEnabled()

    override fun setSuEnabled(enabled: Boolean): Boolean = Natives.setSuEnabled(enabled)

    override fun setSuCompatModePref(mode: Int) = prefs.edit { putInt("su_compat_mode", mode) }

    override fun getSuCompatModePref(): Int = prefs.getInt("su_compat_mode", 0)

    override suspend fun getKernelUmountStatus(): String = getFeatureStatus("kernel_umount")

    override fun isKernelUmountEnabled(): Boolean = Natives.isKernelUmountEnabled()

    override fun setKernelUmountEnabled(enabled: Boolean): Boolean =
        Natives.setKernelUmountEnabled(enabled)

    override suspend fun getSelinuxHideStatus(): String = getFeatureStatus("selinux_hide")

    override fun isSelinuxHideEnabled(): Boolean = Natives.isSelinuxHideEnabled()

    override fun setSelinuxHideEnabled(enabled: Boolean): Int =
        Natives.setSelinuxHideEnabled(enabled)

    override suspend fun getSulogStatus(): String = getFeatureStatus("sulog")

    override suspend fun getSulogPersistValue(): Long? = getFeaturePersistValue("sulog")

    override suspend fun getSulogValue(): Long? = getFeatureValue("sulog")

    override fun setSulogEnabled(enabled: Boolean): Boolean =
        execKsud("feature set sulog ${if (enabled) 1 else 0}", true)

    override suspend fun getAdbRootStatus(): String = getFeatureStatus("adb_root")

    override suspend fun getAdbRootPersistValue(): Long? = getFeaturePersistValue("adb_root")

    override suspend fun getAdbRootValue(): Long? = getFeatureValue("adb_root")

    override fun setAdbRootEnabled(enabled: Boolean): Boolean =
        if (execKsud("feature set adb_root ${if (enabled) 1 else 0}", true)) {
            ShellUtils.fastCmd("setprop ctl.restart adbd")
            true
        } else {
            false
        }

    override suspend fun getShellSuAdbOnlyStatus(): String = getFeatureStatus("shell_su_adb_only")

    override suspend fun getShellSuAdbOnlyPersistValue(): Long? =
        getFeaturePersistValue("shell_su_adb_only")

    override suspend fun getShellSuAdbOnlyValue(): Long? = getFeatureValue("shell_su_adb_only")

    override fun setShellSuAdbOnlyEnabled(enabled: Boolean): Boolean =
        execKsud("feature set shell_su_adb_only ${if (enabled) 1 else 0}", true)

    override fun isDefaultUmountModules(): Boolean = Natives.isDefaultUmountModules()

    override fun setDefaultUmountModules(enabled: Boolean): Boolean =
        Natives.setDefaultUmountModules(enabled)

    override fun isLkmMode(): Boolean = Natives.isLkmMode

    override fun execKsudFeatureSave() {
        execKsud("feature save", true)
    }
}

private const val KEY_COLOR_MODE = "color_mode"
private const val KEY_KEY_COLOR = "key_color"
private const val KEY_PALETTE_STYLE = "color_style"
private const val KEY_COLOR_SPEC = "color_spec"
private const val KEY_ENABLE_BLUR = "enable_blur"
private const val KEY_PAGE_SCALE = "page_scale"
private const val KEY_ENABLE_NAVIGATION_BADGE = "enable_navigation_badge"
private const val KEY_ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
private const val KEY_MODULE_DESCRIPTION_MAX_LINES = "module_description_max_lines"
private const val KEY_CHECK_UPDATE = "check_update"
private const val KEY_MODULE_CHECK_UPDATE = "module_check_update"
private const val KEY_ENABLE_WEB_DEBUGGING = "enable_web_debugging"
private const val KEY_MODULE_SORT_ENABLED_FIRST = "module_sort_enabled_first"
private const val KEY_MODULE_SORT_ACTION_FIRST = "module_sort_action_first"
private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
private const val KEY_SHOW_ONLY_PRIMARY_USER_APPS = "show_only_primary_user_apps"
private const val KEY_SUPERUSER_SORT_OPTION = "superuser_sort_option"
private const val KEY_SULOG_FILTERS = "sulog_filters"
private const val KEY_AUTO_JAILBREAK = "auto_jailbreak"

/** Reads an enum stored by name, falling back to [default] for missing or renamed values. */
private inline fun <reified T : Enum<T>> SharedPreferences.enumOrDefault(
    key: String,
    default: T,
): T {
    val stored = getString(key, null) ?: return default
    return runCatching { enumValueOf<T>(stored) }.getOrDefault(default)
}
