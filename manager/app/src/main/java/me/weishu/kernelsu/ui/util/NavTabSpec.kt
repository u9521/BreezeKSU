package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Skin-agnostic description of one navigation destination. [badgeAlert] picks the badge tone.
 */
@Immutable
data class NavTabSpec(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val badgeAlert: Boolean = false,
)
