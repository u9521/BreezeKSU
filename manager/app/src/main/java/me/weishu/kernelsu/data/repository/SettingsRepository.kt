package me.weishu.kernelsu.data.repository

import kotlinx.coroutines.flow.StateFlow
import me.weishu.kernelsu.data.UiSettings

/**
 * Persisted preferences plus the kernel/ksud feature switches the settings screens drive.
 *
 * There is exactly one instance per process - `KernelSUApplication.settingsRepository` - so
 * every reader sees the same [settings] flow and a write from one screen shows up in the next
 * one immediately. Constructing your own would give you a second, silently divergent copy.
 */
interface SettingsRepository {
    /** Observable snapshot; every write publishes a new immutable value. */
    val settings: StateFlow<UiSettings>

    /** Applies [transform] atomically and persists only the keys it changed. */
    fun update(transform: (UiSettings) -> UiSettings)

    val intentToken: String

    suspend fun getSuCompatStatus(): String
    suspend fun getSuCompatPersistValue(): Long?
    fun isSuEnabled(): Boolean
    fun setSuEnabled(enabled: Boolean): Boolean
    fun setSuCompatModePref(mode: Int)
    fun getSuCompatModePref(): Int

    suspend fun getKernelUmountStatus(): String
    fun isKernelUmountEnabled(): Boolean
    fun setKernelUmountEnabled(enabled: Boolean): Boolean

    suspend fun getSelinuxHideStatus(): String
    fun isSelinuxHideEnabled(): Boolean
    fun setSelinuxHideEnabled(enabled: Boolean): Int

    suspend fun getSulogStatus(): String
    suspend fun getSulogPersistValue(): Long?
    suspend fun getSulogValue(): Long?
    fun setSulogEnabled(enabled: Boolean): Boolean

    suspend fun getAdbRootStatus(): String
    suspend fun getAdbRootPersistValue(): Long?
    suspend fun getAdbRootValue(): Long?
    fun setAdbRootEnabled(enabled: Boolean): Boolean
    suspend fun getShellSuAdbOnlyStatus(): String
    suspend fun getShellSuAdbOnlyPersistValue(): Long?
    suspend fun getShellSuAdbOnlyValue(): Long?
    fun setShellSuAdbOnlyEnabled(enabled: Boolean): Boolean

    fun isDefaultUmountModules(): Boolean
    fun setDefaultUmountModules(enabled: Boolean): Boolean

    fun isLkmMode(): Boolean

    fun execKsudFeatureSave()
}
