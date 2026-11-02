package me.weishu.kernelsu.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.weishu.kernelsu.ui.component.breeze.bottombar.BottomBarBreeze
import me.weishu.kernelsu.ui.component.breeze.bottombar.BreezeNavigationLayout
import me.weishu.kernelsu.ui.component.breeze.bottombar.NavigationLayoutType
import me.weishu.kernelsu.ui.component.breeze.bottombar.NavigationRailBreeze
import me.weishu.kernelsu.ui.component.breeze.bottombar.rememberBreezeNavLayoutState
import me.weishu.kernelsu.ui.component.breeze.bottombar.rememberMainPagerState
import me.weishu.kernelsu.ui.component.common.pager.BreezeHorizontalPager
import me.weishu.kernelsu.ui.component.common.pager.rememberBreezePagerOverscroll
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.NavTabSpec
import me.weishu.kernelsu.ui.util.bottomBarHazeStyle
import me.weishu.kernelsu.ui.util.defaultHazeEffect
import me.weishu.kernelsu.ui.util.isRail
import me.weishu.kernelsu.ui.util.onlyHorizontal
import me.weishu.kernelsu.ui.util.rememberNavigationBarType

/**
 * Breeze skin of the main tab host: navigation bar or rail around the tab pager.
 *
 * Owns the tab-route to pager sync, the back gesture that returns a non-home tab to Home, and the
 * pager itself. The page bodies arrive through [content] so they stay skin-agnostic.
 *
 * @param isFullFeatured false when the kernel UAPI version does not match this manager build. The
 *   tab bar is then hidden and the pager is not swipeable: with no kernel to drive, the tab host is
 *   not a place the user can usefully navigate from. This preserves upstream's degradation.
 */
@Composable
fun MainScreenBreeze(
    tabRouteIndex: Int,
    tabs: List<NavTabSpec>,
    pagerState: PagerState,
    isTopRoute: Boolean,
    isFullFeatured: Boolean,
    onSelectTab: (Int) -> Unit,
    onSettledPageChanged: (Int) -> Unit,
    onBackToHome: () -> Unit,
    content: @Composable (pageIndex: Int, isActiveTab: Boolean) -> Unit,
) {
    val navBarType = rememberNavigationBarType()
    val useNavigationRail = navBarType.isRail()
    val mainScreenHazeState = rememberHazeState()
    val navState = rememberBreezeNavLayoutState(
        initialValue = if (!isFullFeatured) {
            NavigationLayoutType.HIDDEN
        } else if (useNavigationRail) {
            NavigationLayoutType.SIDE
        } else {
            NavigationLayoutType.BOTTOM
        }
    )

    val mainPagerState = rememberMainPagerState(pagerState = pagerState, animatePageChanges = true)
    // One overscroll state shared by pager drawing and programmatic spring scrolls.
    val pagerOverscroll = rememberBreezePagerOverscroll()
    SideEffect { mainPagerState.overscroll = pagerOverscroll }

    // Route -> pager
    LaunchedEffect(tabRouteIndex) { mainPagerState.animateToPage(tabRouteIndex) }

    // Pager -> route: read `settledPage` (not `currentPage`, which fires on intermediate pages).
    // Guard with `!isNavigating` (keep programmatic scrolls) and `isTopRoute` (predictive back
    // pre-composes the underlying tab).
    val settledPage = pagerState.settledPage
    LaunchedEffect(settledPage) {
        if (!mainPagerState.isNavigating && isTopRoute) onSettledPageChanged(settledPage)
    }

    // Keep the tab highlight in sync during user swipes.
    val currentPage = pagerState.currentPage
    LaunchedEffect(currentPage) { mainPagerState.syncPage() }

    // Back on a non-home tab, or mid tab scroll, returns to Home instead of leaving the app.
    val isPagerBackHandlerEnabled = isTopRoute &&
            (tabRouteIndex != 0 || mainPagerState.selectedPage != 0 || mainPagerState.isNavigating)
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = onBackToHome,
    )

    // A full-featured build shows the bar (or the rail) while a tab owns the back stack. A build
    // whose kernel UAPI version does not match keeps it hidden: there is nothing below to navigate.
    LaunchedEffect(navState.targetValue, isTopRoute, useNavigationRail, isFullFeatured) {
        when {
            !isFullFeatured || !isTopRoute -> navState.hideNavController()
            useNavigationRail -> navState.showRail()
            else -> navState.showBar()
        }
    }

    val isNavVisible = isTopRoute && navState.targetValue != NavigationLayoutType.HIDDEN

    BreezeNavigationLayout(
        state = navState,
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomBarBreeze(
                    tabs = tabs,
                    selectedIndex = mainPagerState.selectedPage,
                    onTabSelected = onSelectTab,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .defaultHazeEffect(mainScreenHazeState, bottomBarHazeStyle()),
                    navBarType = navBarType,
                )
            }
        },
        sideBar = {
            NavigationRailBreeze(
                tabs = tabs,
                selectedIndex = mainPagerState.selectedPage,
                onTabSelected = onSelectTab,
                navBarType = navBarType,
            )
        },
    ) { contentPadding ->
        BreezeHorizontalPager(
            modifier = Modifier
                .then(
                    if (LocalEnableBlur.current) {
                        Modifier.hazeSource(mainScreenHazeState)
                    } else {
                        Modifier
                    }
                )
                .padding(contentPadding.onlyHorizontal()),
            state = pagerState,
            // A build whose kernel does not match has no tab bar to reveal, so the pager must not
            // become a way to navigate the tabs it cannot show.
            userScrollEnabled = isFullFeatured && isNavVisible,
            key = { page -> BreezeRoute.TabRoute.entries[page].name },
            overscroll = pagerOverscroll,
        ) { page ->
            content(page, mainPagerState.selectedPage == page)
        }
    }
}
