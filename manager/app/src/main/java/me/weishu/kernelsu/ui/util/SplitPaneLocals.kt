package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf

/** Live list/detail split ratio; `null` when no list-detail scene is active. */
val LocalSplitRatio = compositionLocalOf<MutableFloatState?> { null }

/** True inside the list half of a list-detail scene. */
val LocalIsListPane = compositionLocalOf { false }

/** True inside the detail half of a list-detail scene. */
val LocalIsDetailPane = compositionLocalOf { false }

/** True when the list half currently has a detail pane beside it. */
val LocalHasDetailPane = compositionLocalOf { false }
