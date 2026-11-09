package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Which column of a split scene the calling content occupies. Position is decided purely by the
 * back stack: the older entry is the start column, the newer one the end column, and a scene that
 * did not pair the last two entries renders full screen ([LocalPaneSlot] is then null).
 */
enum class PaneSlot {
    /** Index 0 of the current scene's entries. */
    Start,

    /** Index 1; `null` means no pane is beside it. */
    End,
}

/** The column this content occupies, or null when it fills the screen. */
val LocalPaneSlot = compositionLocalOf<PaneSlot?> { null }
