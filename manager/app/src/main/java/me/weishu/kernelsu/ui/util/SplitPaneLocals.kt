package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf

/**
 * Live start-column ratio of the split scene the calling content sits in; `null` when no split
 * scene is active. The owning scene provides the state it got from [SplitRatioSource], so writing to
 * it resizes the columns.
 */
val LocalSplitRatio = compositionLocalOf<MutableFloatState?> { null }

/**
 * True while another page sits to the right of this one in the strip.
 *
 * The split scene knows positions, not routes, so this is the only way a list half can tell that a
 * detail is currently beside it. It flips on the page's own index rather than on the cursor, so a
 * page keeps its chrome for the whole travel it is part of.
 */
val LocalPaneHasNext = compositionLocalOf { false }
