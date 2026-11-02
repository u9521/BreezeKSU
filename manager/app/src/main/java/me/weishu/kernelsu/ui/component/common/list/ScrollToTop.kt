package me.weishu.kernelsu.ui.component.common.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Last key [ScrollToTopOnChange] scrolled for; a plain field so updating it never recomposes. */
private class KeyRef(var value: Any?)

/**
 * Jumps [state] back to the first item when [key] changes, e.g. a new search query.
 *
 * Never scrolls on first composition or when the screen is re-entered from a pushed route, so the
 * list keeps the offset `LazyListState` saved. [itemCount] is read lazily, after the new items are
 * measured.
 */
@Composable
fun ScrollToTopOnChange(
    state: LazyListState,
    key: Any?,
    itemCount: () -> Int,
) {
    // Initialised to the first key so the opening composition counts as "unchanged".
    val lastKey = remember { KeyRef(key) }
    val currentItemCount by rememberUpdatedState(itemCount)
    val keyChanged = lastKey.value != key

    // Reset the item provider synchronously: with stable item keys LazyColumn would otherwise
    // keep the previous key's offset and only scroll visually afterwards.
    SideEffect {
        if (keyChanged) {
            state.requestScrollToItem(0)
            lastKey.value = key
        }
    }

    LaunchedEffect(key) {
        // `keyChanged` is captured at composition; the SideEffect above already cleared it.
        if (!keyChanged || currentItemCount() == 0) return@LaunchedEffect
        scrollToTopAfterListSettles(state, timeoutMillis = SettleTimeoutMillis)
    }
}

/**
 * Scrolls [state] to the top once the current scroll gesture settles, or after [timeoutMillis]
 * when the list is still settling. Keeps the jump invisible: no intermediate frame is drawn.
 */
suspend fun scrollToTopAfterListSettles(
    state: LazyListState,
    timeoutMillis: Long = SettleTimeoutMillis,
) {
    withTimeoutOrNull(timeoutMillis.milliseconds) {
        snapshotFlow { state.isScrollInProgress }.first { !it }
    }
    state.scrollToItem(0)
}

/** How long [scrollToTopAfterListSettles] waits for an in-flight fling before jumping anyway. */
private const val SettleTimeoutMillis = 3000L
