package me.weishu.kernelsu.ui.component.breeze.bar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffoldScope
import me.weishu.kernelsu.ui.util.topBarHazeEffect

/**
 * Collapsing large top app bar with backdrop blur (or tonal fill when blur is off).
 * Requires the scrolling content to attach `hazeSource(hazeState)`; `BreezeScaffold` does that.
 */
@Composable
fun BreezeTopAppBar(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) {
            TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        } else {
            TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
        },
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = expressiveTopBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    hazeState: HazeState,
) {
    LargeFlexibleTopAppBar(
        title = title,
        modifier = modifier.topBarHazeEffect(hazeState, scrollBehavior),
        subtitle = subtitle,
        navigationIcon = navigationIcon,
        actions = actions,
        titleHorizontalAlignment = titleHorizontalAlignment,
        collapsedHeight = collapsedHeight,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior?.disableDrag(),
    )
}

/** [BreezeTopAppBar] wired to the enclosing `BreezeScaffold`'s scroll behavior and haze source. */
@Composable
fun BreezeScaffoldScope.BreezeTopAppBar(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) {
            TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        } else {
            TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
        },
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = expressiveTopBarColors(),
) {
    BreezeTopAppBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navigationIcon = navigationIcon,
        actions = actions,
        titleHorizontalAlignment = titleHorizontalAlignment,
        collapsedHeight = collapsedHeight,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
        hazeState = hazeState,
    )
}
