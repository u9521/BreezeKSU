package me.weishu.kernelsu.data

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/** Light/dark preference; [SYSTEM] follows the OS and [DARK_AMOLED] paints pure black surfaces. */
enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    DARK_AMOLED(3);

    val isSystem: Boolean get() = this == SYSTEM
    val isDark: Boolean get() = this == DARK || this == DARK_AMOLED
    val isAmoled: Boolean get() = this == DARK_AMOLED

    companion object {
        /**
         * Normalises a stored `color_mode`. Upstream's 0..6 range folded its Monet variants onto
         * four modes here; nothing is written back, so an older value simply reads as its new one.
         */
        fun fromValue(value: Int): ColorMode = when (value) {
            1, 4 -> LIGHT
            2, 5 -> DARK
            3, 6 -> DARK_AMOLED
            else -> SYSTEM
        }
    }
}

/**
 * Immutable snapshot of every persisted appearance and behaviour preference.
 *
 * Pure Kotlin on purpose: this is Layer 1, so it may not name a Compose type. Every write
 * publishes a new snapshot rather than mutating a preference behind a reader's back.
 */
data class UiSettings(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val enableBlur: Boolean = true,
    val pageScale: Float = 1.0f,
    val enableNavigationBadge: Boolean = true,
    val enablePredictiveBack: Boolean = false,
    val moduleDescriptionMaxLines: Int = 4,
    val checkUpdate: Boolean = true,
    val checkModuleUpdate: Boolean = true,
    val enableWebDebugging: Boolean = false,
    val moduleSortEnabledFirst: Boolean = false,
    val moduleSortActionFirst: Boolean = false,
    val superuserShowSystemApps: Boolean = false,
    val superuserShowOnlyPrimaryUserApps: Boolean = false,
    val superuserSortOption: Int = 0,
    val suLogFilters: Set<String>? = null,
    val useSoftReboot: Boolean = false,
)
