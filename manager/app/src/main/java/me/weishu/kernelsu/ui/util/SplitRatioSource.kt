package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The seam a split scene reads its column ratio from.
 *
 * A scene never owns the ratio: it asks for the state of the pair it is about to show and only
 * reads that state while laying out, so a caller may write to it at any time and resize the columns
 * live. Returning the same state for every pair means one global width; returning a state per
 * [startKey] / [endKey] pair means each pair keeps its own.
 */
fun interface SplitRatioSource {
    /** The start-column ratio of the pair [startKey] | [endKey], as a fraction of the width. */
    fun ratioFor(startKey: Any, endKey: Any): MutableFloatState
}

/**
 * Application-wide [SplitRatioSource]. Provide one to decide how the columns of every split scene
 * are sized and who may resize them; when nothing is provided the host falls back to
 * [rememberSingleSplitRatioSource].
 */
val LocalSplitRatioSource = staticCompositionLocalOf<SplitRatioSource?> { null }

/** A [SplitRatioSource] that shares one ratio between every pair. */
@Composable
fun rememberSingleSplitRatioSource(initialRatio: Float = 0.4f): SplitRatioSource {
    val shared = remember { mutableFloatStateOf(initialRatio) }
    return remember(shared) {
        SplitRatioSource { _, _ -> shared }
    }
}
