package me.weishu.kernelsu.ui.component.breeze.bar

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.component.breeze.feedback.BreezeSnackBarHost
import me.weishu.kernelsu.ui.component.breeze.overlay.overlayBackdrop
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffoldScope
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.topBarHazeEffect

/**
 * Collapsing top bar whose search field expands into a full-screen contained search bar.
 *
 * @param hazeState backdrop the bar blurs once it starts collapsing.
 * @param isActiveTab false while another tab owns the screen; the expanded field then collapses
 *   and clears itself, so returning to the tab never restores a stale search.
 */
@Composable
fun SearchAppBarBreeze(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    searchBarScrollBehavior: SearchBarScrollBehavior? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    hazeState: HazeState,
    isActiveTab: Boolean = true,
    searchContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit = { _, _ -> },
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    val scope = rememberCoroutineScope()
    val searchBarState = rememberContainedSearchBarState()
    val textFieldState = rememberTextFieldState()
    val currentQuery = textFieldState.text.toString()
    val isSearchExpanded =
        searchBarState.currentValue != SearchBarValue.Collapsed ||
                searchBarState.targetValue != SearchBarValue.Collapsed
    var previousSearchBarValue by remember { mutableStateOf(searchBarState.currentValue) }
    val clearSearchText: () -> Unit = {
        textFieldState.setTextAndPlaceCursorAtEnd("")
        onClearClick()
    }
    val collapseAndClear: () -> Unit = {
        clearSearchText()
        scope.launch { searchBarState.animateToCollapsed() }
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    // Leaving the tab collapses and clears the field so the next visit starts from a clean slate.
    LaunchedEffect(isActiveTab) {
        if (!isActiveTab) collapseAndClear()
    }

    LaunchedEffect(searchText) {
        val current = textFieldState.text.toString()
        if (current != searchText) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchText)
        }
    }

    LaunchedEffect(textFieldState, searchText) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { value ->
                if (value != searchText) {
                    onSearchTextChange(value)
                }
            }
    }

    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                val collapsedFromExpanded =
                    previousSearchBarValue != SearchBarValue.Collapsed &&
                            value == SearchBarValue.Collapsed
                previousSearchBarValue = value
                if (collapsedFromExpanded) {
                    clearSearchText()
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
    }

    val blurEnabled = LocalEnableBlur.current
    val collapseFraction = (scrollBehavior?.state?.collapsedFraction ?: 0f).coerceIn(0f, 1f)
    val easedCollapseFraction = FastOutLinearInEasing.transform(collapseFraction)
    // Transparent when blur is active so the caller's haze effect shows through.
    val topBarBackgroundColor = if (blurEnabled) {
        Color.Transparent
    } else {
        lerp(
            start = MaterialTheme.colorScheme.surfaceContainer,
            stop = MaterialTheme.colorScheme.surfaceBright,
            fraction = easedCollapseFraction,
        )
    }
    val containedSearchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
    // Fade input field alpha slightly as the top bar collapses in blur mode.
    val appBarInputFieldContainerColors = if (blurEnabled) {
        val background = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = 1f - (0.5f * easedCollapseFraction),
        )
        containedSearchBarColors.inputFieldColors.copy(
            focusedContainerColor = background,
            unfocusedContainerColor = background,
            disabledContainerColor = background,
        )
    } else {
        containedSearchBarColors.inputFieldColors.copy(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
    // Keep inner containers transparent; parent Column draws the background.
    val transparentAppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
        searchBarColors = containedSearchBarColors.copy(
            containerColor = Color.Transparent,
            dividerColor = Color.Transparent,
        ),
        scrolledSearchBarContainerColor = Color.Transparent,
        appBarContainerColor = Color.Transparent,
        scrolledAppBarContainerColor = Color.Transparent,
    )
    // Opaque surface for the expanded full-screen search window.
    val fullScreenSearchBarColors = SearchBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dividerColor = Color.Transparent,
        inputFieldColors = appBarInputFieldContainerColors.copy(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )

    val inputField: @Composable (TextFieldColors) -> Unit = { textFieldColors ->
        SearchBarDefaults.InputField(
            modifier = Modifier.fillMaxWidth(),
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            colors = textFieldColors,
            onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            leadingIcon = {
                if (isSearchExpanded) {
                    IconButton(
                        onClick = { collapseAndClear() },
                        content = { Icon(MaterialSymbols.Filled.Rounded.ArrowBack, null) },
                    )
                } else {
                    Icon(MaterialSymbols.Filled.Rounded.Search, null)
                }
            },
            trailingIcon = {
                if (isSearchExpanded && currentQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { clearSearchText() },
                        content = { Icon(MaterialSymbols.Filled.Rounded.Close, null) },
                    )
                }
            },
            interactionSource = interactionSource,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (scrollBehavior != null) Modifier.topBarHazeEffect(
                    hazeState,
                    scrollBehavior
                ) else Modifier
            )
            .background(topBarBackgroundColor)
    ) {
        LargeFlexibleTopAppBar(
            title = title,
            subtitle = subtitle,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            navigationIcon = { if (navigationIcon != null) navigationIcon() },
            actions = { if (actions != null) actions() },
            scrollBehavior = scrollBehavior?.disableDrag(),
        )

        AppBarWithSearch(
            state = searchBarState,
            inputField = { inputField(appBarInputFieldContainerColors) },
            colors = transparentAppBarWithSearchColors,
            scrollBehavior = searchBarScrollBehavior,
            windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
            contentPadding = PaddingValues(horizontal = 8.dp),
        )
    }
    ExpandedFullScreenContainedSearchBar(
        // Register above the page (zIndex = 1f) so dialogs opened from results blur search UI.
        modifier = Modifier.overlayBackdrop(zIndex = 1f),
        state = searchBarState,
        collapsedShape = RectangleShape,
        inputField = {
            inputField(fullScreenSearchBarColors.inputFieldColors)
        },
        windowInsets = {
            SearchBarDefaults.fullScreenWindowInsets
                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        },
        colors = fullScreenSearchBarColors,
        content = {
            val bottomPadding =
                SearchBarDefaults.fullScreenWindowInsets.asPaddingValues().calculateBottomPadding()
            Box(Modifier.fillMaxSize()) {
                if (currentQuery.isNotEmpty()) {
                    searchContent(bottomPadding, collapseAndClear)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding(),
                ) {
                    BreezeSnackBarHost(hostState = snackbarHostState)
                }
            }
        },
    )
}

/** [SearchAppBarBreeze] wired to the enclosing `BreezeScaffold`'s scroll behavior, haze and snackbar host. */
@Composable
fun BreezeScaffoldScope.SearchAppBarBreeze(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    searchBarScrollBehavior: SearchBarScrollBehavior? = this.searchBarScrollBehavior,
    isActiveTab: Boolean = true,
    searchContent: @Composable BoxScope.(bottomPadding: Dp, closeSearch: () -> Unit) -> Unit = { _, _ -> },
) {
    SearchAppBarBreeze(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        searchText = searchText,
        onSearchTextChange = onSearchTextChange,
        onClearClick = onClearClick,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        searchBarScrollBehavior = searchBarScrollBehavior,
        snackbarHostState = snackbarHostState,
        hazeState = hazeState,
        isActiveTab = isActiveTab,
        searchContent = searchContent,
    )
}
