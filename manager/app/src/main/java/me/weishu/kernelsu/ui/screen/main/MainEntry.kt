package me.weishu.kernelsu.ui.screen.main

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.ksu.getSuperuserCount
import me.weishu.kernelsu.ui.LocalSettingsRepository
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.nav.isTopRoute
import me.weishu.kernelsu.ui.nav.mainPageRoute
import me.weishu.kernelsu.ui.nav.setPage
import me.weishu.kernelsu.ui.screen.home.HomeEntry
import me.weishu.kernelsu.ui.screen.module.ModuleEntry
import me.weishu.kernelsu.ui.screen.module.ModuleViewModel
import me.weishu.kernelsu.ui.screen.settings.SettingsEntry
import me.weishu.kernelsu.ui.screen.superuser.SuperUserEntry
import me.weishu.kernelsu.ui.screen.superuser.SuperUserViewModel
import me.weishu.kernelsu.ui.util.NavTabSpec

/**
 * Skin-agnostic main tab host: resolves tab data, syncs the route with the pager and routes each
 * page to its entry. Swapping the skin replaces the single [MainScreenBreeze] call only.
 *
 * Nothing here draws a component or a Material widget: badges are plain counts handed to
 * [NavTabSpec], and the bar itself lives in the skin.
 */
@Composable
fun MainEntry(
    tabRoute: BreezeRoute.TabRoute,
    pagerState: PagerState,
) {
    val navigator = LocalBreezeNavigator.current
    val settings by LocalSettingsRepository.current.settings.collectAsStateWithLifecycle()
    val isFullFeatured = Natives.isFullFeatured()

    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    val superUserViewModel = viewModel<SuperUserViewModel>()
    val grantedUidCount by remember(superUserViewModel) {
        superUserViewModel.uiState
            .map { state -> state.groupedApps.count { it.anyAllowSu } }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(0)

    var startupPreloadStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isFullFeatured) {
        if (!isFullFeatured || startupPreloadStarted) {
            return@LaunchedEffect
        }

        moduleViewModel.initializePreferences()
        val moduleState = moduleViewModel.uiState.value
        if (!moduleState.hasLoaded) {
            if (!moduleState.isRefreshing) moduleViewModel.fetchModuleList()
            moduleViewModel.uiState.first { it.hasLoaded }
        }
        moduleViewModel.syncModuleUpdateInfo(moduleViewModel.uiState.value.modules)

        val superUserState = superUserViewModel.uiState.value
        if (!superUserState.hasLoaded) {
            superUserViewModel.initializePreferences()
            if (superUserState.isRefreshing) {
                superUserViewModel.uiState.first { it.hasLoaded }
            } else {
                superUserViewModel.loadAppList().join()
            }
        }

        startupPreloadStarted = true
    }

    // Loading the app list just for a badge is too expensive; read the kernel allowlist instead.
    val badgeEnabled = settings.enableNavigationBadge && isFullFeatured
    var superuserCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(badgeEnabled, grantedUidCount) {
        superuserCount =
            if (badgeEnabled) withContext(Dispatchers.IO) { getSuperuserCount() } else 0
    }

    val moduleUpdatableCount = if (badgeEnabled) {
        moduleUiState.updateInfo.count { it.value.downloadUrl.isNotBlank() }
    } else {
        0
    }
    val moduleEnabledCount = if (badgeEnabled) {
        moduleUiState.modules.count { it.enabled }
    } else {
        0
    }

    val tabs = BreezeRoute.TabRoute.entries.map { route ->
        NavTabSpec(
            label = stringResource(route.labelRes),
            selectedIcon = route.selectedIcon,
            unselectedIcon = route.unselectedIcon,
            badgeCount = when (route) {
                BreezeRoute.TabRoute.Home -> 0
                BreezeRoute.TabRoute.SuperUser -> if (badgeEnabled) superuserCount else 0
                BreezeRoute.TabRoute.Module ->
                    moduleUpdatableCount.takeIf { it > 0 } ?: moduleEnabledCount

                BreezeRoute.TabRoute.Settings -> 0
            },
            badgeAlert = route == BreezeRoute.TabRoute.Module && moduleUpdatableCount > 0,
        )
    }

    MainScreenBreeze(
        tabRouteIndex = tabRoute.ordinal,
        tabs = tabs,
        pagerState = pagerState,
        isTopRoute = navigator.isTopRoute(),
        isFullFeatured = isFullFeatured,
        onSelectTab = { navigator.push(mainPageRoute(it)) },
        onSettledPageChanged = { navigator.setPage(it) },
        onBackToHome = { navigator.push(BreezeRoute.TabRoute.Home) },
    ) { pageIndex, isActiveTab ->
        when (BreezeRoute.TabRoute.entries[pageIndex]) {
            BreezeRoute.TabRoute.Home -> HomeEntry(isCurrentPage = isActiveTab)
            BreezeRoute.TabRoute.SuperUser -> SuperUserEntry(isCurrentPage = isActiveTab)
            BreezeRoute.TabRoute.Module -> ModuleEntry(isCurrentPage = isActiveTab)
            BreezeRoute.TabRoute.Settings -> SettingsEntry(isCurrentPage = isActiveTab)
        }
    }
}
