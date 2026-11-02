package me.weishu.kernelsu.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/** Retains only start and end padding so page content can scroll under floating top and bottom bars. */
@Composable
fun PaddingValues.onlyHorizontal(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        top = 0.dp,
        bottom = 0.dp,
        start = this.calculateStartPadding(layoutDirection),
        end = this.calculateEndPadding(layoutDirection),
    )
}
