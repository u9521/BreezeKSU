package me.weishu.kernelsu.ui.component.breeze.list

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.weishu.kernelsu.ui.component.common.list.HorizontalScrollbar
import me.weishu.kernelsu.ui.component.common.list.ScrollbarAdapter
import me.weishu.kernelsu.ui.component.common.list.ScrollbarDefaults
import me.weishu.kernelsu.ui.component.common.list.ScrollbarStyle
import me.weishu.kernelsu.ui.component.common.list.VerticalScrollbar
import me.weishu.kernelsu.ui.component.common.list.rememberScrollbarAdapter

/** Time a scrollbar stays fully opaque after the last scroll before it fades out. */
private const val BreezeScrollbarFadeMillis = 1500L

/** Breeze-themed scrollbar style; the thumb uses `primary` over a translucent bright rail. */
@Composable
fun breezeScrollbarStyle(): ScrollbarStyle = ScrollbarDefaults.style.copy(
    color = MaterialTheme.colorScheme.primary,
    railColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
)

@Composable
fun BreezeVerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    visible: Boolean = true,
    durationMillis: Long = BreezeScrollbarFadeMillis,
    enter: EnterTransition = slideInHorizontally { it },
    exit: ExitTransition = slideOutHorizontally { it },
) {
    VerticalScrollbar(
        modifier = modifier,
        adapter = adapter,
        style = breezeScrollbarStyle(),
        reverseLayout = reverseLayout,
        visible = visible,
        durationMillis = durationMillis,
        enter = enter,
        exit = exit,
    )
}

@Composable
fun BreezeVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    visible: Boolean = true,
    durationMillis: Long = BreezeScrollbarFadeMillis,
) {
    BreezeVerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        reverseLayout = reverseLayout,
        visible = visible,
        durationMillis = durationMillis,
    )
}

@Composable
fun BreezeVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    visible: Boolean = true,
    durationMillis: Long = BreezeScrollbarFadeMillis,
) {
    BreezeVerticalScrollbar(
        adapter = rememberScrollbarAdapter(listState),
        modifier = modifier,
        reverseLayout = reverseLayout,
        visible = visible,
        durationMillis = durationMillis,
    )
}

@Composable
fun BreezeHorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    visible: Boolean = true,
    durationMillis: Long = BreezeScrollbarFadeMillis,
    enter: EnterTransition = slideInVertically { it },
    exit: ExitTransition = slideOutVertically { it },
) {
    HorizontalScrollbar(
        modifier = modifier,
        adapter = adapter,
        style = breezeScrollbarStyle(),
        reverseLayout = reverseLayout,
        visible = visible,
        durationMillis = durationMillis,
        enter = enter,
        exit = exit,
    )
}

@Suppress("unused")
@Composable
fun BreezeHorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    visible: Boolean = true,
    durationMillis: Long = BreezeScrollbarFadeMillis,
) {
    BreezeHorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        reverseLayout = reverseLayout,
        visible = visible,
        durationMillis = durationMillis,
    )
}
