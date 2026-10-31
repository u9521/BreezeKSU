package me.weishu.kernelsu.ui.component.breeze.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberSearchBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.weishu.kernelsu.ui.component.breeze.bottombar.BottomBarHeight
import me.weishu.kernelsu.ui.component.breeze.bottombar.LocalNavigationBottomInset
import me.weishu.kernelsu.ui.component.breeze.feedback.BreezeSnackBarHost
import me.weishu.kernelsu.ui.component.breeze.list.BreezeVerticalScrollbar
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.UiMessage
import me.weishu.kernelsu.ui.util.onlyHorizontal

/** State shared by every slot of one [BreezeScaffold]. */
@Stable
class BreezeScaffoldScope internal constructor(
    val scrollBehavior: TopAppBarScrollBehavior,
    val searchBarScrollBehavior: SearchBarScrollBehavior,
    val hazeState: HazeState,
    val snackbarHostState: SnackbarHostState,
)

/**
 * Full-screen Breeze page scaffold.
 *
 * Owns four things every page would otherwise repeat: the floating navigation-bar inset from
 * [LocalNavigationBottomInset], the action-button clearance, the [UiMessage] to snackbar hand-off,
 * and the backdrop capture the top and bottom bars blur. The page only supplies content.
 *
 * @param fabLift lift the action button (and, one-to-one, the snackbar and the content's bottom
 *   inset) clear of where [BottomBarHeight] would be, so the button sits at the same height a tab
 *   page's button does. The lift is the *difference* to the
 *   `BottomBarHeight + navigation bar` target, so a page that already clears it — a tab page whose
 *   scaffold unions in [LocalNavigationBottomInset] — is left untouched.
 * @param fabVisible whether the action button is actually on screen. A button hidden by
 *   `animateFloatingActionButton` measures to zero and the Scaffold drops its clearance and
 *   snackbar offset, so the lift has to be dropped with it or the page keeps a dead band of empty
 *   space the button no longer needs.
 * @param message one-shot message to show; [onMessageShown] fires with its id once handled.
 * @param hazeState source recorded by the content box; top bars in [topBar] blur it.
 */
@Composable
fun BreezeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable BreezeScaffoldScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    fabLift: Boolean = false,
    fabVisible: Boolean = true,
    message: UiMessage? = null,
    onMessageShown: (Long) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
    searchBarScrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(
        scrollState = rememberSearchBarScrollState(initialScrollOffsetLimit = 0f),
    ),
    hazeState: HazeState = rememberHazeState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { BreezeSnackBarHost(hostState = it) },
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable BreezeScaffoldScope.(PaddingValues) -> Unit,
) {
    val scope = remember(scrollBehavior, searchBarScrollBehavior, hazeState, snackbarHostState) {
        BreezeScaffoldScope(scrollBehavior, searchBarScrollBehavior, hazeState, snackbarHostState)
    }

    // Union instead of consume: a tab page must clear the floating bar, and every page must still
    // clear the system navigation bar that the bar overlays. Union takes the larger inset per edge,
    // so the floating bar and the system bar cannot double-count.
    val navigationInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val pageInsets = contentWindowInsets.union(
        WindowInsets(bottom = LocalNavigationBottomInset.current)
    )
    val pageBottomInset = pageInsets.asPaddingValues().calculateBottomPadding()

    // The height a tab page's action button already sits at: the bar, plus the system inset the bar
    // itself clears. Only the shortfall is added, so a page that already clears the bar keeps its
    // button exactly where it is, and a page in rail mode (no bar) is lifted to the same height.
    val liftTarget = BottomBarHeight + navigationInset
    val liftAmount = if (fabLift && fabVisible) {
        (liftTarget - pageBottomInset).coerceAtLeast(0.dp)
    } else {
        0.dp
    }
    // Animating the inset rather than the button keeps the two in step: the Scaffold derives the
    // button position, the snackbar offset and the content padding from this single value.
    val animatedLift by animateDpAsState(
        targetValue = liftAmount,
        label = "FabLift",
    )
    val effectiveInsets = if (animatedLift > 0.dp) {
        pageInsets.add(WindowInsets(bottom = animatedLift))
    } else {
        pageInsets
    }

    if (message != null) {
        LaunchedEffect(message.id) {
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            onMessageShown(message.id)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(searchBarScrollBehavior.nestedScrollConnection),
        topBar = { scope.topBar() },
        bottomBar = bottomBar,
        snackbarHost = { snackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = effectiveInsets,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (LocalEnableBlur.current) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            scope.content(innerPadding)
        }
    }
}

/**
 * Standard vertical scroll column for a [BreezeScaffold] page.
 *
 * Applies the scaffold's horizontal insets and reserves its top and bottom insets as spacers, so
 * content scrolls under the bars while the first and last items stay reachable.
 *
 * The bottom reservation is exactly [bottomExtraSpacing] past the scaffold's bottom inset: the
 * trailing spacer gives back the gap [verticalArrangement] would otherwise insert before it, so a
 * spacedBy column cannot add one more gap below an item nobody spaced. The leading spacer keeps
 * its gap, which is the distance the arrangement owes the first item.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun BreezeScaffoldScope.BreezeScrollColumn(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    showScrollbar: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    topExtraSpacing: Dp = 0.dp,
    bottomExtraSpacing: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.onlyHorizontal())
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding),
            verticalArrangement = verticalArrangement,
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding() + topExtraSpacing))

            content()

            // Sized as if the arrangement's gap before it did not exist: that gap is part of the
            // bottom reservation, not extra room under the last item.
            Spacer(
                Modifier.height(
                    (innerPadding.calculateBottomPadding() + bottomExtraSpacing - verticalArrangement.spacing)
                        .coerceAtLeast(0.dp)
                )
            )
        }

        if (showScrollbar) {
            BreezeVerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
            )
        }
    }
}
