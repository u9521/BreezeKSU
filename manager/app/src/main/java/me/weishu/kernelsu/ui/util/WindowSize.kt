package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo

/** Navigation affordance the current window width asks for. */
enum class NavigationBarType { ShortBar, Bar, Rail, WideRail, Drawer }

fun NavigationBarType.isRail(): Boolean =
    this == NavigationBarType.Rail ||
            this == NavigationBarType.WideRail ||
            this == NavigationBarType.Drawer

/**
 * Measures with the physical screen density so a reduced `LocalDensity` (page scale) cannot make a
 * phone look like a tablet and flip the navigation into rail mode.
 */
@Composable
fun rememberNavigationBarType(): NavigationBarType {
    val containerSize = LocalWindowInfo.current.containerSize
    val physicalDensity = LocalResources.current.displayMetrics.density
    val widthDp = containerSize.width / physicalDensity
    val heightDp = containerSize.height / physicalDensity
    val isCompactHeight = heightDp < 480f
    return when {
        widthDp >= 1200f -> NavigationBarType.Drawer
        widthDp >= 840f -> if (isCompactHeight) {
            NavigationBarType.Rail
        } else {
            NavigationBarType.WideRail
        }

        widthDp >= 600f -> NavigationBarType.Rail
        else -> if (isCompactHeight) NavigationBarType.ShortBar else NavigationBarType.Bar
    }
}

@Composable
fun isRailNavbar(): Boolean = rememberNavigationBarType().isRail()
