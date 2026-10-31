package me.weishu.kernelsu.ui.nav

import androidx.navigation3.runtime.NavKey

/**
 * The content key shared by every entry that renders the main pager.
 *
 * Entries sharing a content key share their saveable state and their view models, and navigation3
 * identifies a scene by its class and this key - so all tab routes are one scene, not one each.
 * [me.weishu.kernelsu.ui.BreezeApp] renders that scene's content through a movable content, which
 * is what keeps the host (its pager and its pages) alive across a page change, and
 * `ui.animation.NavTransition` makes the page change itself a no-op instead of sliding one page
 * over the other.
 */
const val BreezeMainHostContentKey: String = "Breeze.MainHost"

/** The pager page this route shows, or `null` for anything that is not a page route. */
fun BreezeRoute.mainPageIndex(): Int? =
    BreezeRoute.TabRoute.entries.indexOf(this).takeIf { it >= 0 }

/** The route that shows [index] as the page. */
fun mainPageRoute(index: Int): BreezeRoute {
    return BreezeRoute.TabRoute.entries[index.coerceIn(0, BreezeRoute.TabRoute.entries.lastIndex)]
}

/**
 * The binding key that pairs a page's list pane with the details opened from it.
 *
 * Only the rail layout pairs panes: `BreezeListDetailScene` shows a detail beside the entry
 * underneath it when the two name the same binding key, and a page route is the list pane of every
 * detail opened from it.
 */
fun BreezeRoute.TabRoute.mainPageBindingKey(): String = name.lowercase()

/** True if this key is one of the pages, i.e. a route that owns the whole back stack. */
internal fun NavKey.isMainPage(): Boolean = this is BreezeRoute.TabRoute

/** The page the top of the back stack is bound to, or `null` if it is not a page route. */
fun BreezeNavigator.currentPageIndex(): Int? = (current() as? BreezeRoute.TabRoute)?.mainPageIndex()

/**
 * True while the top of the back stack is the pager host, i.e. the bottom bar / rail is visible.
 *
 * A page route is the host itself; a detail pushed on top of it is not.
 */
fun BreezeNavigator.isTopRoute(): Boolean = currentPageIndex() != null

/** Syncs the back stack to the tab at [index] after a pager swipe settles. */
fun BreezeNavigator.setPage(index: Int) {
    push(mainPageRoute(index))
}
