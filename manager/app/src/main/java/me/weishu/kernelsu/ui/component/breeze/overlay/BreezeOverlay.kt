package me.weishu.kernelsu.ui.component.breeze.overlay

import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scrim
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import me.weishu.kernelsu.ui.theme.LocalEnableBlur

/** Scrim opacity matching `Theme.Material.Dialog.Alert` (`backgroundDimAmount = 0.6`). */
internal const val DialogScrimAlpha = 0.6f

/** Scrim opacity for a modal bottom sheet (`ScrimTokens.ContainerOpacity`). */
internal const val SheetScrimAlpha = 0.32f

/** Scrim fade-in and fade-out durations; card motion belongs to each component. */
internal const val OverlayEnterMillis = 220
internal const val OverlayExitMillis = 150

/** One overlay layer inside the shared overlay window, ordered by [sequence]. */
@Stable
internal class BreezeOverlayEntry(
    val visibleTransition: MutableTransitionState<Boolean>,
    val render: @Composable (isTopmost: Boolean) -> Unit,
) {
    /** Assigned when the layer is added to a host; 0 while it has never been shown. */
    var sequence: Long = 0L

    /** Surface color used to derive system-bar icon appearance when this layer is topmost. */
    var surfaceColor: Color = Color.Unspecified

    /** True while this layer is mounted in the overlay window. */
    val isVisible: Boolean
        get() = visibleTransition.currentState || visibleTransition.targetState
}

/**
 * State holding the stack of overlay layers that share a single window.
 *
 * Owns the backdrop [HazeState], so callers never wire a blur source by hand: the host records
 * the page below only while a layer is on screen.
 */
@Stable
class BreezeOverlayHostState {
    internal val entries = mutableStateListOf<BreezeOverlayEntry>()

    /** Backdrop recorded by [BreezeOverlayHost] and blurred by the topmost layer. */
    internal val hazeState = HazeState()

    /** True while at least one layer is mounted. */
    val isShowing: Boolean get() = entries.any { it.isVisible }

    private var nextSequence = 0L

    internal fun add(entry: BreezeOverlayEntry) {
        entry.sequence = nextSequence++
        entries += entry
    }

    internal fun remove(entry: BreezeOverlayEntry) {
        entries -= entry
    }
}

@Composable
fun rememberBreezeOverlayHostState(): BreezeOverlayHostState = remember { BreezeOverlayHostState() }

/** The nearest [BreezeOverlayHostState]; `null` outside a [BreezeOverlayHost]. */
val LocalBreezeOverlayHost = staticCompositionLocalOf<BreezeOverlayHostState?> { null }

/**
 * Owns the one window every overlay in [content] draws into.
 *
 * Layers stack in opening order ([BreezeOverlayEntry.sequence]), not composition order; the
 * topmost layer receives back presses and blurs the layers beneath it.
 *
 * The wrapping [Box] becomes the backdrop source only while a layer is showing and blur is on, so
 * closed overlays cost no offscreen capture.
 */
@Composable
fun BreezeOverlayHost(
    state: BreezeOverlayHostState,
    content: @Composable () -> Unit,
) {
    val captureBackdrop = state.isShowing && LocalEnableBlur.current
    CompositionLocalProvider(LocalBreezeOverlayHost provides state) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (captureBackdrop) {
                        Modifier
                            .hazeSource(state.hazeState, zIndex = 0f)
                            .background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
        if (state.isShowing) {
            OverlayWindow(
                layers = state.entries.filter { it.isVisible }.sortedBy { it.sequence },
                hazeState = state.hazeState.takeIf { captureBackdrop },
            )
        }
    }
}

/**
 * Registers this subtree as a backdrop source for overlays opened from it, one step above the page.
 * Full-screen layers such as an expanded search bar use it so dialogs blur them too.
 *
 * Captures nothing while no overlay is showing: the opaque background would otherwise paint over
 * the page for a layer that nothing blurs.
 */
@Composable
fun Modifier.overlayBackdrop(zIndex: Float = 1f): Modifier {
    val host = LocalBreezeOverlayHost.current ?: return this
    if (!host.isShowing || !LocalEnableBlur.current) return this
    return this
        .hazeSource(host.hazeState, zIndex = zIndex)
        .background(MaterialTheme.colorScheme.background)
}

/**
 * Full-screen modal overlay layer with a Compose scrim and host backdrop blur.
 *
 * Keeping the window full-screen avoids `WindowManager` relayouts when content height changes.
 * Inside [BreezeOverlayHost], layers share the host window; outside one (e.g. previews), each
 * opens a standalone window.
 *
 * @param visible whether the layer stays mounted in the overlay window.
 * @param scrimVisible whether the scrim is faded in; separate from [visible] so self-animating
 *   content can keep the layer mounted until its exit animation finishes.
 * @param appearance what is painted behind the card: blur, scrim strength and their shared timeline.
 */
@Composable
fun BreezeOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    scrimVisible: Boolean = visible,
    appearance: OverlayAppearance = OverlayAppearance.Default,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit,
) {
    val host = LocalBreezeOverlayHost.current

    // Read through rememberUpdatedState so the remembered entry updates without re-registering.
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val currentModifier by rememberUpdatedState(modifier)
    val currentDismissOnClickOutside by rememberUpdatedState(dismissOnClickOutside)
    val currentDismissOnBackPress by rememberUpdatedState(dismissOnBackPress)
    val currentContent by rememberUpdatedState(content)
    val currentScrimVisible by rememberUpdatedState(scrimVisible)
    val currentAppearance by rememberUpdatedState(appearance)

    val entry = remember {
        val visibleState = MutableTransitionState(false)
        BreezeOverlayEntry(visibleState) { isTopmost ->
            OverlayLayer(
                visibleTransition = visibleState,
                scrimVisible = currentScrimVisible,
                modifier = currentModifier,
                appearance = currentAppearance,
                onClickOutside = if (currentDismissOnClickOutside) currentOnDismiss else null,
                onBack = if (currentDismissOnBackPress) currentOnDismiss else null,
                isTopmost = isTopmost,
                content = currentContent,
            )
        }
    }
    entry.visibleTransition.targetState = visible
    entry.surfaceColor = surfaceColor

    if (host == null) {
        if (entry.isVisible) OverlayWindow(listOf(entry), hazeState = null)
        return
    }

    val shown = entry.isVisible
    DisposableEffect(host, shown) {
        if (shown) host.add(entry)
        onDispose { if (shown) host.remove(entry) }
    }
}

/** Shared full-screen [Dialog] window for both [BreezeOverlayHost] and standalone fallback. */
@Composable
private fun OverlayWindow(
    layers: List<BreezeOverlayEntry>,
    hazeState: HazeState?,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            scrimAlpha = 0f,
        ),
    ) {
        // Disable the theme's window scale animation so the full-screen backdrop does not scale.
        val view = LocalView.current
        DisposableEffect(view) {
            view.findDialogWindow()?.setWindowAnimations(0)
            onDispose {}
        }

        // Derive system-bar icon contrast from the topmost layer's surface color.
        val window = view.findDialogWindow()
        val topSurface = layers.lastOrNull { it.isVisible }?.surfaceColor
            ?.takeIf { it != Color.Unspecified }
            ?: MaterialTheme.colorScheme.surfaceContainerHigh
        val lightBars = remember(topSurface) { topSurface.luminance() > 0.5f }
        SideEffect {
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            OverlayStack(layers, layers.lastIndex, hazeState)
        }
    }
}

/**
 * Renders [layers] bottom-up. Layers below the topmost one are drawn, and also registered into
 * [hazeState] as sources, so the topmost layer's blur sees one stack instead of the page it covers.
 */
@Composable
private fun OverlayStack(
    layers: List<BreezeOverlayEntry>,
    index: Int,
    hazeState: HazeState?,
) {
    if (index < 0) return
    if (index > 0) {
        OverlayStack(layers, index - 1, hazeState)
    }
    val entry = layers[index]
    val isTopmost = index == layers.lastIndex
    // Sources stack above the page (0f) and the expanded search bar (1f); a lower modal covers both.
    val sourceModifier = if (hazeState != null && !isTopmost) {
        Modifier.hazeSource(hazeState, zIndex = 2f + index)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sourceModifier)
    ) {
        entry.render(isTopmost)
    }
}

/** Single overlay layer: topmost back handler, backdrop blur, animated scrim, and card slot. */
@Composable
private fun OverlayLayer(
    visibleTransition: MutableTransitionState<Boolean>,
    scrimVisible: Boolean,
    modifier: Modifier,
    appearance: OverlayAppearance,
    onClickOutside: (() -> Unit)?,
    onBack: (() -> Unit)?,
    isTopmost: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onBack != null && isTopmost) {
        // Only bind BackHandler on the topmost layer so back order follows stack order, not composition.
        BackHandler(enabled = visibleTransition.targetState, onBack = onBack)
    }

    // Drive both blur radius and scrim alpha from a single 0f-initialized Animatable.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(scrimVisible) {
        progress.animateTo(
            targetValue = if (scrimVisible) 1f else 0f,
            animationSpec = tween(
                if (scrimVisible) appearance.enterMillis else appearance.exitMillis,
            ),
        )
    }

    val hazeState = LocalBreezeOverlayHost.current?.hazeState
    val blurActive = isTopmost && LocalEnableBlur.current && hazeState != null
    val blurredModifier = if (blurActive) {
        appearance.blur.apply(modifier, hazeState, appearance.blurRadius * progress.value)
    } else {
        modifier
    }
    val dimAlpha = if (blurActive) appearance.scrimAlphaBlurred else appearance.scrimAlpha

    Box(modifier = blurredModifier.fillMaxSize()) {
        // Keep Scrim composed even at alpha 0 so touches cannot fall through during enter/exit.
        Scrim(
            contentDescription = "Close",
            onClick = onClickOutside,
            alpha = { progress.value * dimAlpha },
            color = MaterialTheme.colorScheme.scrim,
            modifier = if (isTopmost) Modifier else Modifier.clearAndSetSemantics {},
        )

        // Wrap in Box so the window is full-screen without forcing minHeight on children.
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

/** Walks up from `LocalView` to the enclosing `DialogLayout` that implements [DialogWindowProvider]. */
private fun View.findDialogWindow(): Window? {
    var node: ViewParent? = parent
    while (node != null) {
        if (node is DialogWindowProvider) return node.window
        node = node.parent
    }
    return null
}
