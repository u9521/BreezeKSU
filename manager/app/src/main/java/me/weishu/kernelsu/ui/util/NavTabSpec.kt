package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Skin-agnostic description of one navigation destination, rendered by any navigation bar or rail.
 *
 * [badgeAlert] picks the badge tone: an alert badge uses the default error container, an accent one
 * the primary color.
 */
@Immutable
data class NavTabSpec(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val badgeAlert: Boolean = false,
)
