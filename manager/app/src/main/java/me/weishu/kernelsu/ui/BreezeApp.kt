package me.weishu.kernelsu.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.channels.ReceiveChannel
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.animation.navPopTransitionSpec
import me.weishu.kernelsu.ui.animation.navPredictivePopTransitionSpec
import me.weishu.kernelsu.ui.animation.navTransitionSpec
import me.weishu.kernelsu.ui.nav.BreezeIntentRouter
import me.weishu.kernelsu.ui.nav.BreezeMainHostContentKey
import me.weishu.kernelsu.ui.nav.BreezeNavigator
import me.weishu.kernelsu.ui.nav.BreezeRoute
import me.weishu.kernelsu.ui.nav.LocalBreezeNavigator
import me.weishu.kernelsu.ui.nav.currentPageIndex
import me.weishu.kernelsu.ui.nav.paneMarkers
import me.weishu.kernelsu.ui.nav.rememberBreezeListDetailSceneStrategy
import me.weishu.kernelsu.ui.nav.rememberBreezeNavigator
import me.weishu.kernelsu.ui.nav.withDimmedUnderneath
import me.weishu.kernelsu.ui.screen.about.AboutEntry
import me.weishu.kernelsu.ui.screen.appprofile.AppProfileEntry
import me.weishu.kernelsu.ui.screen.colorpalette.ColorPaletteEntry
import me.weishu.kernelsu.ui.screen.executemoduleaction.ExecuteModuleActionEntry
import me.weishu.kernelsu.ui.screen.flash.FlashEntry
import me.weishu.kernelsu.ui.screen.install.InstallEntry
import me.weishu.kernelsu.ui.screen.main.MainEntry
import me.weishu.kernelsu.ui.screen.module.InstallModuleDialog
import me.weishu.kernelsu.ui.screen.sulog.SulogEntry
import me.weishu.kernelsu.ui.screen.template.TemplateEntry
import me.weishu.kernelsu.ui.screen.templateeditor.TemplateEditorEntry
import me.weishu.kernelsu.ui.util.LocalSplitRatioSource
import me.weishu.kernelsu.ui.util.createWebuiIntent
import me.weishu.kernelsu.ui.util.rememberSingleSplitRatioSource

/**
 * Composition root: settings, density scaling, navigator, main pager and the navigation host.
 *
 * The host is wrapped in `movableContentOf` so a skin swap around [BreezeShell] keeps the back
 * stack, the pager and every `NavEntry` view model store alive.
 */
@Composable
fun BreezeApp(
    intentChannel: ReceiveChannel<Intent>,
    onContentReady: () -> Unit = {},
) {
    val settingsRepository = ksuApp.settingsRepository
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()

    val navigator = rememberBreezeNavigator(BreezeRoute.TabRoute.Home)
    // Owned above NavDisplay: replacing the tab entry must not recreate the pager.
    val pagerState = rememberPagerState(
        initialPage = navigator.currentPageIndex() ?: 0,
        pageCount = { BreezeRoute.TabRoute.entries.size },
    )

    val baseDensity = LocalDensity.current
    val density = remember(baseDensity, settings.pageScale) {
        Density(baseDensity.density * settings.pageScale, baseDensity.fontScale)
    }

    val navHost = remember {
        movableContentOf { AppNavHost(navigator, pagerState, intentChannel, onContentReady) }
    }

    // Owned here, above the nav host: one ratio for every split scene, kept across page and route
    // changes. Swap the source to decide the ratio per pair instead.
    val splitRatioSource = rememberSingleSplitRatioSource()

    CompositionLocalProvider(
        LocalSettingsRepository provides settingsRepository,
        LocalBreezeNavigator provides navigator,
        LocalSplitRatioSource provides splitRatioSource,
        LocalDensity provides density,
    ) {
        BreezeShell(settings = settings) { navHost() }
    }
}

/**
 * Route table of the app. Tab routes share [BreezeMainHostContentKey] so navigation3 treats all of
 * them as one scene and switching tabs never runs a scene transition.
 */
@Composable
private fun AppNavHost(
    navigator: BreezeNavigator,
    pagerState: PagerState,
    intentChannel: ReceiveChannel<Intent>,
    onContentReady: () -> Unit,
) {
    val context = LocalContext.current
    // Hosted above NavDisplay so incoming deep links stay active across route changes.
    var pendingZipInstallUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    BreezeIntentRouter(
        intentChannel = intentChannel,
        onOpenWebUI = { uri ->
            // The router hands back the ksu://webui URI it parsed; the helper owns how a WebUI
            // window is opened, so the root never names the activity class.
            uri.getQueryParameter("id")?.let { moduleId ->
                context.startActivity(createWebuiIntent(context, moduleId))
            }
        },
        onRequestInstallModule = { uri -> pendingZipInstallUri = uri },
    )

    InstallModuleDialog(
        uris = listOfNotNull(pendingZipInstallUri),
        getInstalledModules = { ksuApp.moduleRepository.getModules().getOrDefault(emptyList()) },
        onConfirmInstall = {
            pendingZipInstallUri?.let { uri ->
                navigator.push(BreezeRoute.Flash(FlashIt.FlashModules(listOf(uri))))
            }
            pendingZipInstallUri = null
        },
        onDismiss = { pendingZipInstallUri = null },
    )

    val mainHost = remember {
        movableContentOf<BreezeRoute.TabRoute> { page ->
            MainEntry(tabRoute = page, pagerState = pagerState)
        }
    }

    val provider = entryProvider<NavKey>(
        fallback = { key ->
            val route = key as? BreezeRoute.TabRoute ?: error("Unknown screen $key")
            NavEntry(
                key,
                BreezeMainHostContentKey,
                // Tabs share one content key, so only SuperUser publishes pane markers; the rest
                // would have the split scene swallow a page that can never show a second one.
                route.paneMarkers(),
            ) { mainHost(route) }
        },
    ) {
        entry<BreezeRoute.About> { AboutEntry() }
        entry<BreezeRoute.Sulog> { SulogEntry() }
        entry<BreezeRoute.ColorPalette> { ColorPaletteEntry() }
        // Pane markers ride along as entry metadata; the scene strategy reads them instead of the
        // route type, because navigation3 keeps an entry's key private.
        entry<BreezeRoute.AppProfileTemplate>(metadata = { it.paneMarkers() }) { TemplateEntry() }
        entry<BreezeRoute.TemplateEditor>(metadata = { it.paneMarkers() }) { key ->
            TemplateEditorEntry(key.template, key.readOnly)
        }
        entry<BreezeRoute.AppProfile>(metadata = { it.paneMarkers() }) { key ->
            AppProfileEntry(
                key.uid
            )
        }
        entry<BreezeRoute.Install> { InstallEntry() }
        entry<BreezeRoute.Flash> { key -> FlashEntry(key.flashIt) }
        entry<BreezeRoute.ExecuteModuleAction> { key ->
            ExecuteModuleActionEntry(
                key.moduleId,
                key.fromShortcut
            )
        }
    }

    val listDetailStrategy = rememberBreezeListDetailSceneStrategy<NavKey>()
    val sceneStrategies = remember(listDetailStrategy) {
        listOf(
            listDetailStrategy.withDimmedUnderneath(),
            SinglePaneSceneStrategy<NavKey>().withDimmedUnderneath(),
        )
    }
    NavDisplay(
        backStack = navigator.backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = sceneStrategies,
        entryProvider = provider,
        onBack = { navigator.pop() },
        transitionSpec = navTransitionSpec(),
        popTransitionSpec = navPopTransitionSpec(),
        predictivePopTransitionSpec = navPredictivePopTransitionSpec(),
    )

    SideEffect { onContentReady() }
}
