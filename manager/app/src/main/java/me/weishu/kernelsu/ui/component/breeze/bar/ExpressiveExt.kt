package me.weishu.kernelsu.ui.component.breeze.bar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.theme.LocalEnableBlur

/** Breeze top bar colours: transparent while blur is on, tonal otherwise. */
@Composable
internal fun expressiveTopBarColors() =
    if (LocalEnableBlur.current) TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent
    ) else TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceBright
    )

/** Disables direct drag on the app bar so collapse is driven only by nested content scroll. */
@Composable
internal fun TopAppBarScrollBehavior.disableDrag(): TopAppBarScrollBehavior {
    return remember(this) {
        object : TopAppBarScrollBehavior {
            override val state = this@disableDrag.state
            override val isPinned = true
            override val snapAnimationSpec = this@disableDrag.snapAnimationSpec
            override val flingAnimationSpec = this@disableDrag.flingAnimationSpec
            override val nestedScrollConnection = this@disableDrag.nestedScrollConnection
        }
    }
}
