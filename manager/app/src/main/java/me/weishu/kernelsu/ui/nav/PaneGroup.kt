package me.weishu.kernelsu.ui.nav

import androidx.navigation3.runtime.NavKey

/**
 * A subject a pane can talk about. Two adjacent entries pair into a split scene when the newer one
 * lists a group that the older one is the pane host of.
 */
enum class PaneGroup {
    /** The superuser list, and the app profiles opened from it. */
    SuperUser,

    /** One app's profile, and the profile templates it points at. */
    App,

    /** The template list, and one template's editor. */
    Template,
}

/**
 * A route that renders content about [paneGroup] and can therefore host a detail column beside
 * itself. [paneGroup] is null for a route that is grouped so callers can name it but never hosts on
 * its own - every tab is a [NavKey] of the same enum, and only some of them split.
 */
interface PaneHost {
    val paneGroup: PaneGroup?
}

/** A route that may be pushed beside a host whose [PaneHost.paneGroup] it lists. */
interface PaneDetail {
    val paneGroups: Set<PaneGroup>
}

/**
 * Pane markers of one route, published as [androidx.navigation3.runtime.NavEntry] metadata. A
 * strategy cannot read the route back from an entry (navigation3 keeps the key private), so this
 * is the only channel it has.
 */
data class PaneMarkers(
    val hostGroup: PaneGroup?,
    val detailGroups: Set<PaneGroup>,
)

/** Metadata key holding [PaneMarkers]; unused by navigation3 itself. */
const val PANE_MARKERS_KEY: String = "Breeze.Pane.Markers"

/**
 * Pane markers for this route, or an empty map when it never takes part in a split.
 *
 * Markers are only published when the route can actually do something with them: a route that hosts
 * nothing and is no one's detail stays out, so the split scene never takes over a screen that would
 * only ever show one page.
 */
fun NavKey.paneMarkers(): Map<String, Any> {
    val route = this
    val host = (route as? PaneHost)?.paneGroup
    val details = (route as? PaneDetail)?.paneGroups ?: emptySet()
    if (host == null && details.isEmpty()) return emptyMap()
    return mapOf(PANE_MARKERS_KEY to PaneMarkers(hostGroup = host, detailGroups = details))
}
