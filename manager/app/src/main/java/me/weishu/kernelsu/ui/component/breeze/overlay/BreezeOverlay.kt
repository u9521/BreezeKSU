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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** One overlay layer inside the shared overlay window, ordered by [sequence]. */
@Stable
internal class BreezeOverlayEntry(
    val visibleTransition: MutableTransitionState<Boolean>,
    val render: @Composable (isTopmost: Boolean, isBottomLayer: Boolean) -> Unit,
) {
    /** Assigned when the layer is added to a host; 0 while it has never been shown. */
    var sequence: Long = 0L

    /**
     * Backdrop this layer paints, or `null` to use the host's default.
     *
     * Snapshot-backed, unlike [surfaceColor]: the host reads it while composing, before [render]
     * runs, so a caller that switches blur off on a live layer must still invalidate the capture.
     */
    var backdrop: OverlayBackdrop? by mutableStateOf(null)

    /** Where this layer's backdrop progress comes from. */
    var progress: OverlayProgress by mutableStateOf(OverlayProgress.Fade(visible = false))

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

/** Backdrop of the enclosing [OverlayWindow]: the recorded content, or `null` when none is needed. */
private val LocalOverlayHazeState = staticCompositionLocalOf<HazeState?> { null }

/** True while the enclosing host records a backdrop its layers would sample. */
private val LocalOverlayCapturesBackdrop = staticCompositionLocalOf { false }

/** Backdrop this layer paints: its own when set, otherwise the host default. */
private fun BreezeOverlayEntry.effectiveBackdrop(host: OverlayBackdrop?): OverlayBackdrop? =
    backdrop ?: host

/** True while this layer would sample the host's recorded content. */
private fun BreezeOverlayEntry.samplesHost(host: OverlayBackdrop?): Boolean =
    effectiveBackdrop(host)?.samplesHost == true

/**
 * Owns the one window every overlay in [content] draws into.
 *
 * Layers stack in opening order ([BreezeOverlayEntry.sequence]), not composition order; the
 * topmost layer receives back presses and blurs the layers beneath it.
 *
 * The wrapping [Box] becomes the backdrop source only while a layer that will sample it is
 * showing, so closed overlays and stacks of sharp layers cost no offscreen capture.
 *
 * @param backdrop default backdrop of every layer in this host; `null` turns host blur off for the
 *   whole stack, individual layers can still opt back in by passing their own.
 */
@Composable
fun BreezeOverlayHost(
    state: BreezeOverlayHostState,
    backdrop: OverlayBackdrop? = OverlayBackdrop.Default,
    content: @Composable () -> Unit,
) {
    // Capture only when some layer will sample the host, so a stack of sharp layers costs nothing.
    val capturesBackdrop = state.isShowing &&
        state.entries.any { it.isVisible && it.samplesHost(backdrop) }
    CompositionLocalProvider(
        LocalBreezeOverlayHost provides state,
        LocalOverlayCapturesBackdrop provides capturesBackdrop,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (capturesBackdrop) {
                        Modifier
                            .hazeSource(state.hazeState, zIndex = 0f)
                            .background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
        if (state.isShowing) {
            OverlayWindow(
                layers = state.entries.filter { it.isVisible }.sortedBy { it.sequence },
                hazeState = state.hazeState.takeIf { capturesBackdrop },
                hostBackdrop = backdrop,
            )
        }
    }
}

/**
 * Registers this subtree as a backdrop source for overlays opened from it, one step above the page.
 * Full-screen layers such as an expanded search bar use it so dialogs blur them too.
 *
 * Captures nothing while no overlay shows a blur: the opaque background would otherwise paint over
 * the page for a layer that nothing blurs.
 */
@Composable
fun Modifier.overlayBackdrop(zIndex: Float = 1f): Modifier {
    val host = LocalBreezeOverlayHost.current ?: return this
    if (!LocalOverlayCapturesBackdrop.current) return this
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
 * @param progress where the backdrop's 0..1 progress comes from; defaults to fading on [visible].
 *   Pass [OverlayProgress.External] to tie it to a gesture or to the card's own animation.
 * @param backdrop what is painted behind the card, or `null` for the host's default.
 * @param surfaceColor tints the system bar icons while this layer is topmost.
 */
@Composable
fun BreezeOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    progress: OverlayProgress = OverlayProgress.Fade(visible = visible),
    backdrop: OverlayBackdrop? = null,
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
    val currentProgress by rememberUpdatedState(progress)
    val currentBackdrop by rememberUpdatedState(backdrop)

    val entry = remember {
        val visibleState = MutableTransitionState(false)
        BreezeOverlayEntry(visibleState) { isTopmost, isBottomLayer ->
            OverlayLayer(
                visibleTransition = visibleState,
                progress = currentProgress,
                backdrop = currentBackdrop,
                modifier = currentModifier,
                onClickOutside = if (currentDismissOnClickOutside) currentOnDismiss else null,
                onBack = if (currentDismissOnBackPress) currentOnDismiss else null,
                isTopmost = isTopmost,
                isBottomLayer = isBottomLayer,
                content = currentContent,
            )
        }
    }
    entry.visibleTransition.targetState = visible
    entry.surfaceColor = surfaceColor
    entry.backdrop = backdrop
    entry.progress = progress

    if (host == null) {
        // Standalone window: nothing behind it is recorded, so every layer paints its plain scrim.
        if (entry.isVisible) {
            OverlayWindow(listOf(entry), hazeState = null, hostBackdrop = backdrop)
        }
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
    hostBackdrop: OverlayBackdrop?,
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

        CompositionLocalProvider(LocalOverlayHazeState provides hazeState) {
            Box(modifier = Modifier.fillMaxSize()) {
                OverlayStack(layers, hazeState, hostBackdrop)
            }
        }
    }
}

/**
 * Renders [layers] bottom-up, each layer registered as a blur source for the ones above it.
 *
 * Every layer keeps its own blur while mounted, so a covered layer stays blurred and returns
 * untouched when the layer above leaves. Keyed flat on the entry, never nested per index: nesting
 * shifts a layer's composition position whenever the stack changes size, which discards its state
 * and replays its blur from zero.
 */
@Composable
private fun OverlayStack(
    layers: List<BreezeOverlayEntry>,
    hazeState: HazeState?,
    hostBackdrop: OverlayBackdrop?,
) {
    // Only layers under a sampling one are read; above the last of those nothing samples them.
    val lastSamplingIndex = layers.indexOfLast { it.samplesHost(hostBackdrop) }
    layers.forEachIndexed { index, entry ->
        key(entry) {
            // Sources stack above the page (0f) and the expanded search bar (1f).
            val sourceModifier = if (hazeState != null && index < lastSamplingIndex) {
                Modifier.hazeSource(state = hazeState, zIndex = 2f + index)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(sourceModifier),
            ) {
                entry.render(index == layers.lastIndex, index == 0)
            }
        }
    }
}

/**
 * Single overlay layer: topmost back handler, backdrop, and card slot.
 *
 * The backdrop owns everything behind the card; this layer only resolves the two facts it must not
 * guess — which progress to read, and how strong the scrim is for this layer's position.
 *
 * @param isTopmost receives the back press and outside taps.
 * @param isBottomLayer the layer the stack opened with; it carries no stacked dim.
 */
@Composable
private fun OverlayLayer(
    visibleTransition: MutableTransitionState<Boolean>,
    progress: OverlayProgress,
    backdrop: OverlayBackdrop?,
    modifier: Modifier,
    onClickOutside: (() -> Unit)?,
    onBack: (() -> Unit)?,
    isTopmost: Boolean,
    isBottomLayer: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onBack != null && isTopmost) {
        // Only bind BackHandler on the topmost layer so back order follows stack order, not composition.
        BackHandler(enabled = visibleTransition.targetState, onBack = onBack)
    }

    val value = progress.resolve()

    val hazeState = LocalOverlayHazeState.current
    val resolved = backdrop ?: OverlayBackdrop.Default
    // A backdrop that paints no host blur keeps its plain scrim: nothing else separates the card
    // from what is behind it. Layers above the bottom one add a small dim so a stack reads as
    // pushed back rather than brighter.
    val hasBlur = hazeState != null && resolved.samplesHost
    val scrimAlpha = when {
        !hasBlur -> resolved.scrimAlpha
        isBottomLayer -> resolved.scrimAlphaBlurred
        else -> resolved.scrimAlphaBlurred + resolved.scrimAlphaStacked
    }
    val scope = remember(value, hazeState, scrimAlpha, isTopmost, onClickOutside) {
        LayerBackdropScope(value, hazeState, scrimAlpha, isTopmost, onClickOutside)
    }

    // Modifier stays outside the backdrop: it is the caller's sizing, not part of what gets painted.
    Box(modifier = modifier.fillMaxSize()) {
        resolved.Backdrop(scope) { content() }
    }
}

/**
 * Reads [OverlayProgress] as a per-frame value.
 *
 * `Fade` animates a private [Animatable]; `External` just forwards the caller's value. Both return
 * a snapshot-backed [State], so the layer recomposes when it moves.
 */
@Composable
private fun OverlayProgress.resolve(): Float = when (this) {
    is OverlayProgress.Fade -> {
        val animated = remember { Animatable(0f) }
        LaunchedEffect(visible) {
            animated.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (visible) enterMillis else exitMillis,
                    easing = if (visible) enterEasing else exitEasing,
                ),
            )
        }
        animated.value
    }

    is OverlayProgress.External -> value().coerceIn(0f, 1f)
}

@Immutable
private class LayerBackdropScope(
    override val progress: Float,
    override val hazeState: HazeState?,
    override val scrimAlpha: Float,
    override val isTopmost: Boolean,
    override val onClickOutside: (() -> Unit)?,
) : OverlayBackdropScope

/** Walks up from `LocalView` to the enclosing `DialogLayout` that implements [DialogWindowProvider]. */
private fun View.findDialogWindow(): Window? {
    var node: ViewParent? = parent
    while (node != null) {
        if (node is DialogWindowProvider) return node.window
        node = node.parent
    }
    return null
}
