package me.weishu.kernelsu.ui.component.common.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [HorizontalPager] preloading all pages (`beyondViewportPageCount = pageCount - 1` to prevent
 * blank entry frames) and decorating content with [BreezePagerOverscroll] for programmatic
 * boundary overshoot (pass the same [overscroll] to [PagerState.springAnimateToPage]).
 * Edge drag gestures keep platform stretch.
 */
@Composable
fun BreezeHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    key: ((index: Int) -> Any)? = null,
    overscroll: BreezePagerOverscroll? = rememberBreezePagerOverscroll(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val pageModifier = if (overscroll != null) Modifier.pageOvershoot(overscroll) else Modifier
    HorizontalPager(
        modifier = modifier,
        state = state,
        beyondViewportPageCount = (state.pageCount - 1).coerceAtLeast(0),
        userScrollEnabled = userScrollEnabled,
        key = key,
    ) { page ->
        Box(pageModifier) { pageContent(page) }
    }
}
