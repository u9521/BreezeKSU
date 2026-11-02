package me.weishu.kernelsu.ui.component.breeze.bottombar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.NavTabSpec
import me.weishu.kernelsu.ui.util.NavigationBarType
import me.weishu.kernelsu.ui.util.isRail

/**
 * Side navigation rail for wide layouts over [tabs]; opaque `surfaceBright` because content sits
 * beside the rail rather than underneath it.
 */
@Composable
fun NavigationRailBreeze(
    tabs: List<NavTabSpec>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    navBarType: NavigationBarType = NavigationBarType.Rail,
) {
    if (!navBarType.isRail()) return

    val defaultExpanded = navBarType != NavigationBarType.Rail
    var isExpanded by remember(navBarType) { mutableStateOf(defaultExpanded) }
    val state = rememberWideNavigationRailState(
        initialValue = if (defaultExpanded) {
            WideNavigationRailValue.Expanded
        } else {
            WideNavigationRailValue.Collapsed
        }
    )

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            state.expand()
        } else {
            state.collapse()
        }
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val layoutDirection = LocalLayoutDirection.current

    val insetWidth =
        WideNavigationRailDefaults.windowInsets.asPaddingValues()
            .calculateStartPadding(layoutDirection)

    // Resolve labels once so the width measurement and the items share the same strings.
    val tabLabels = tabs.map { it.label }

    // Size expanded rail to fit the longest tab label without truncation.
    val expandedWidthDp = remember(density, insetWidth, labelStyle, tabLabels) {
        val maxTextWidthPx = tabLabels.maxOfOrNull { label ->
            textMeasurer.measure(label, style = labelStyle).size.width
        } ?: 0
        val maxTextWidthDp = with(density) { maxTextWidthPx.toDp() }

        maxTextWidthDp + 104.dp + insetWidth
    }

    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidthDp else 96.dp + insetWidth,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "RailWidthAnimation"
    )

    WideNavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .requiredWidth(animatedWidth),
        state = state,
        colors = WideNavigationRailDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
        header = {
            IconButton(
                modifier = Modifier.padding(start = 24.dp),
                onClick = { isExpanded = !isExpanded },
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        MaterialSymbols.Rounded.MenuOpen
                    } else {
                        MaterialSymbols.Rounded.Menu
                    },
                    contentDescription = stringResource(
                        if (isExpanded) R.string.nav_rail_collapse else R.string.nav_rail_expand
                    ),
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            WideNavigationRailItem(
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
                label = { Text(tab.label) },
                railExpanded = isExpanded,
            )
        }
    }
}
