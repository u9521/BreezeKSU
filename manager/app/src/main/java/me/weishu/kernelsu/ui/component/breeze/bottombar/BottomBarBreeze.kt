package me.weishu.kernelsu.ui.component.breeze.bottombar

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.NavTabSpec
import me.weishu.kernelsu.ui.util.NavigationBarType

/**
 * Height the floating bar occupies above the navigation-bar inset.
 *
 * The bar's own default `windowInsets` adds the system inset, so a page that avoids the bar
 * clears `BottomBarHeight + navigationBars.bottom`.
 */
val BottomBarHeight = 80.dp

/**
 * Floating bottom bar over [tabs]; transparent when blur is enabled so the page shows through.
 * Takes plain tab data so gallery demos and the real navigation share one implementation.
 */
@Composable
fun BottomBarBreeze(
    tabs: List<NavTabSpec>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    navBarType: NavigationBarType = NavigationBarType.Bar,
) {
    val blurEnabled = LocalEnableBlur.current

    val compactBarHeight = BottomAppBarDefaults.FlexibleBottomAppBarHeight

    FlexibleBottomAppBar(
        modifier = modifier,
        containerColor = if (blurEnabled) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceBright
        },
        expandedHeight = if (navBarType == NavigationBarType.Bar) BottomBarHeight else compactBarHeight,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onTabSelected(index) },
                icon = {
                    NavTabIcon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        badgeCount = tab.badgeCount,
                        badgeAlert = tab.badgeAlert,
                    )
                },
                label = {
                    Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

/**
 * Tab icon with an optional count badge. An alert badge keeps the error container so it reads
 * as a warning; the accent tone uses the primary color.
 */
@Composable
internal fun NavTabIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    badgeCount: Int = 0,
    badgeAlert: Boolean = false,
) {
    if (badgeCount <= 0) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
        return
    }
    BadgedBox(
        badge = {
            if (badgeAlert) {
                Badge { Text(badgeCount.toString()) }
            } else {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(badgeCount.toString())
                }
            }
        },
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}
