package me.weishu.kernelsu.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Breeze's back stack and result bus.
 *
 * A local copy of the upstream navigator (`ui.navigation3.Navigator`) so a rewrite there, the
 * pending miuix-navigation migration included, cannot move Breeze's navigation.
 */
@Suppress("unused")
class BreezeNavigator(
    initialKey: NavKey
) {
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(initialKey)

    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()

    /**
     * Navigate to [key].
     *
     * A page route ([BreezeRoute.isMainPage]) is not stacked on anything: it *is* the page on
     * screen, so it owns the whole back stack, which is emptied and rebuilt around it and drops
     * the details of the page that was left. Going back therefore returns to the first page, not
     * to the tab that was tapped.
     *
     * Every other key is pushed on top of the page it was opened from, including one of the same
     * type: two app profiles are two pages, not a replacement. Use [replace] to swap the top entry,
     * or [pushOrReplaceSameClass] to open "the selected item" without growing the stack.
     */
    fun push(key: NavKey) {
        if (key.isMainPage()) {
            if (backStack.size == 1 && current() == key) return
            backStack.clear()
            backStack.add(key)
            return
        }
        if (current() == key) return
        backStack.add(key)
    }

    /**
     * Pushes [key], or swaps the top entry when that entry is already the same class.
     *
     * For a detail a page presents as "the open item": opening another one replaces it instead of
     * stacking, so one back press still leaves the detail. Without it a list that opens the next
     * item from inside a detail would stack two pages of the same kind and the split scene would
     * show them side by side.
     */
    fun pushOrReplaceSameClass(key: NavKey) {
        if (key.isMainPage()) {
            push(key)
            return
        }
        val top = current()
        when {
            top == null -> backStack.add(key)
            top == key -> Unit
            top.javaClass == key.javaClass -> backStack[backStack.lastIndex] = key
            else -> backStack.add(key)
        }
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    fun replaceAll(keys: List<NavKey>) {
        if (keys.isEmpty()) {
            return
        }
        if (backStack.isNotEmpty()) {
            backStack.clear()
            backStack.addAll(keys)
        }
    }

    /**
     * Pop the top key if present. The bottom entry is the page on screen (a page route) and is never
     * popped: an empty back stack makes androidx.navigation3's `NavDisplay` fail its
     * `require(backStack.isNotEmpty())` check. Matches upstream's `ui.navigation3.Navigator` after
     * `manager: fix a crash issue`.
     */
    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    /**
     * Pop until predicate matches the top key, keeping the root entry.
     */
    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun navigateForResult(route: BreezeRoute, requestKey: String) {
        ensureChannel(requestKey)
        push(route)
    }

    fun <T : Any> setResult(requestKey: String, value: T) {
        ensureChannel(requestKey).tryEmit(value)
        pop()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(requestKey: String): SharedFlow<T> {
        return ensureChannel(requestKey) as SharedFlow<T>
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearResult(requestKey: String) {
        ensureChannel(requestKey).resetReplayCache()
    }

    fun current(): NavKey? {
        return backStack.lastOrNull()
    }

    fun backStackSize(): Int {
        return backStack.size
    }

    private fun ensureChannel(key: String): MutableSharedFlow<Any> {
        return resultBus.getOrPut(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 0) }
    }

    companion object {
        val Saver: Saver<BreezeNavigator, Any> = listSaver(save = { navigator ->
            navigator.backStack.toList()
        }, restore = { savedList ->
            val initialKey = savedList.firstOrNull() ?: BreezeRoute.TabRoute.Home
            val navigator = BreezeNavigator(initialKey)
            navigator.backStack.clear()
            navigator.backStack.addAll(savedList)
            navigator
        })
    }
}

@Composable
fun rememberBreezeNavigator(startRoute: NavKey): BreezeNavigator {
    return rememberSaveable(startRoute, saver = BreezeNavigator.Saver) {
        BreezeNavigator(startRoute)
    }
}

val LocalBreezeNavigator = staticCompositionLocalOf<BreezeNavigator> {
    error("LocalBreezeNavigator not provided")
}
