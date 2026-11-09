package me.weishu.kernelsu.ui.nav

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import me.weishu.kernelsu.ui.animation.IosPageEasing
import me.weishu.kernelsu.ui.util.LocalPaneHasNext
import me.weishu.kernelsu.ui.util.LocalPaneSlot
import me.weishu.kernelsu.ui.util.LocalSplitRatio
import me.weishu.kernelsu.ui.util.LocalSplitRatioSource
import me.weishu.kernelsu.ui.util.PaneSlot
import me.weishu.kernelsu.ui.util.isRailNavbar
import me.weishu.kernelsu.ui.util.rememberSingleSplitRatioSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How long a page takes to travel one slot along the strip. */
private const val SPLIT_PAGE_DURATION = 400

/** How long the column seam takes to open or close; slightly shorter so the columns settle first. */
private const val SPLIT_SEAM_DURATION = 340

/**
 * How long a page takes to slide in over the page it replaces. Shortest of the three: it never
 * travels past its own column, so the full duration would read as a pause rather than a slide.
 */
private const val SPLIT_COVER_DURATION = 320

/** Shortest retarget, so a release right at the end of a gesture still decelerates instead of cuts. */
private const val SPLIT_MIN_ANIMATION_DURATION = 120

/**
 * Width of the shadow a page casts on the page it slides across, as a fraction of its own width.
 * Same geometry as BreezeDimScene, so a page arriving inside a scene and one arriving with a scene
 * transition cast the same edge.
 */
private const val STRIP_SHADOW_WIDTH = 0.05f

/** Peak alpha of that shadow; matched to BreezeDimScene.SHADOW_ALPHA for the same reason. */
private const val STRIP_SHADOW_ALPHA = 0.05f

/** Portion of a cover travel the shadow holds at full strength, before fading out as the page lands. */
private const val SHADOW_FADE_FROM = 4f

/**
 * Shows the trailing run of pairable entries as one strip of pages under a moving viewport.
 *
 * The whole run lives in [entries] and [key] is its oldest entry, so drilling deeper never leaves
 * the scene: the pages slide inside one composition instead of the scene transition animating, and
 * only the pages around the viewport are composed at a time.
 */
internal class BreezeListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val metadata: Map<String, Any>,
    /** Every entry this scene owns, oldest first. */
    override val entries: List<NavEntry<T>>,
    val onBack: () -> Unit,
) : Scene<T> {

    override val content: @Composable () -> Unit = {
        val fallbackSource = rememberSingleSplitRatioSource()
        val source = LocalSplitRatioSource.current ?: fallbackSource
        val stack = entries.deduped()
        val startEntry = stack.getOrNull(stack.lastIndex - 1) ?: stack.first()
        // Read per pair, and only here: the ratio must not take part in the scene identity, or
        // `NavDisplay` would rebuild the scene instead of resizing the columns.
        val ratio = source.ratioFor(startEntry.contentKey, stack.last().contentKey)

        CompositionLocalProvider(LocalSplitRatio provides ratio) {
            BreezeStripLayout(
                modifier = Modifier.fillMaxSize(),
                entries = stack,
                ratio = ratio,
                onBack = onBack,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BreezeListDetailScene<*>

        return key == other.key &&
            entries == other.entries &&
            previousEntries == other.previousEntries
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + entries.hashCode()
        result = 31 * result + previousEntries.hashCode()
        return result
    }
}

/**
 * Builds a [BreezeListDetailScene] for the trailing run of pairable entries.
 *
 * A lone pane host is taken over as well: the scene then renders one full-width page, which is what
 * keeps its key - and with it the composition - alive across `[List] <-> [List | Detail]`. Leaving
 * that step to [SinglePaneSceneStrategy] would change the scene class and turn the split into a
 * scene transition.
 */
@Composable
fun <T : Any> rememberBreezeListDetailSceneStrategy(): SceneStrategy<T> {
    val useRail = isRailNavbar()
    return remember(useRail) { BreezeListDetailSceneStrategy(useRail) }
}

class BreezeListDetailSceneStrategy<T : Any>(
    private val useRail: Boolean,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!useRail) return null
        val last = entries.lastOrNull() ?: return null
        if (!last.metadata.containsKey(PANE_MARKERS_KEY)) return null

        // Walk back over the pairs that really do pair. A marked entry that does not pair with its
        // predecessor starts a new run and must not be swallowed into this scene.
        var runStart = entries.lastIndex
        while (runStart > 0 && canPair(entries[runStart - 1], entries[runStart])) {
            runStart--
        }

        return BreezeListDetailScene(
            key = entries[runStart].contentKey,
            // One back press must pop one entry, so the scene always leaves exactly the top entry
            // behind; `NavDisplay` multiplies the back callback by the size difference.
            previousEntries = entries.dropLast(1),
            metadata = last.metadata,
            entries = entries.subList(runStart, entries.size).toList(),
            onBack = onBack,
        )
    }

    private fun canPair(start: NavEntry<T>, end: NavEntry<T>): Boolean {
        if (start.contentKey == end.contentKey) return false
        val startMarkers = start.metadata[PANE_MARKERS_KEY] as? PaneMarkers ?: return false
        val endMarkers = end.metadata[PANE_MARKERS_KEY] as? PaneMarkers ?: return false
        val hostGroup = startMarkers.hostGroup ?: return false
        return hostGroup in endMarkers.detailGroups
    }
}

/**
 * One page of the strip, its column index, and whether it is sliding over the page it replaced.
 *
 * [hasNext] is whether another page sits to its right in the strip; it drives [LocalPaneHasNext],
 * the one thing a start-column page still needs to know about the column beside it.
 */
private class StripPage<T : Any>(
    val entry: NavEntry<T>,
    val index: Int,
    val covering: Boolean,
    val hasNext: Boolean,
)

/**
 * One horizontal strip of pages with a camera cursor, drawn as a viewport over it.
 *
 * Page `i` sits at offset `i - cursor`: offset 0 is the start column, offset 1 the end column, and
 * only `[-1, 2]` can still show a pixel. Moving the cursor is therefore all a push or a pop needs,
 * and because the column edge is exactly where offset 1 starts, two adjacent pages share an edge at
 * every fractional cursor position - the seam cannot crack, whatever the two columns measure.
 *
 * The strip always composes each [NavEntry.contentKey] exactly once. Entries are only ever added or
 * removed from [pages] as a whole, so a burst of replacements cannot compose one entry twice - that
 * would collide in `SaveableStateHolder` and crash the app.
 */
@Composable
private fun <T : Any> BreezeStripLayout(
    modifier: Modifier = Modifier,
    entries: List<NavEntry<T>>,
    ratio: MutableFloatState,
    onBack: () -> Unit,
) {
    // Two back-to-back navigations to the same target - a double tap on one row is enough - leave
    // the same content key in the stack twice. `SaveableStateHolder` refuses to host one key twice,
    // so the strip collapses them: the strip is a view over the stack, not a copy of it.
    val stack = entries.deduped()

    // Pages on the strip. Equal to [stack], plus up to two extras: a popped page kept until it has
    // slid out, and the page a new top is currently sliding over.
    var pages by remember { mutableStateOf(stack) }
    // Content key of the page sliding over the one it replaced, or null when nothing is covering.
    var coveringKey by remember { mutableStateOf<Any?>(null) }
    val seen = remember { SeenEntries<T>() }

    if (seen.entries != stack) {
        val known = seen.entries
        seen.entries = stack
        if (known != null) {
            // What should stay put while a new page slides over the end column: everything except
            // the page that is already covering, which the newcomer replaces.
            val base = if (coveringKey == null) pages else pages.dropLast(1)
            val baseKeys = base.map { it.contentKey }
            val newKeys = stack.map { it.contentKey }
            when {
                // Same sequence, different pages behind it: every tab shares one content key, so a
                // tab swap keeps the keys and only changes the entry. Nothing travels - the pages
                // are simply replaced.
                baseKeys == newKeys ->
                    if (base != stack) {
                        pages = stack
                        coveringKey = null
                    }

                // Replace: same depth, same prefix, one new top page. The new top slides over the
                // old one, so the start column never moves.
                stack.size == base.size && stack.size >= 2 &&
                    baseKeys.dropLast(1) == newKeys.dropLast(1) ->
                    if (base.last().contentKey != stack.last().contentKey) {
                        pages = base + stack.last()
                        coveringKey = stack.last().contentKey
                    }

                // Pop: keep what is on screen; the pages slide out on their own.
                stack.size < base.size && baseKeys.take(stack.size) == newKeys -> {
                    pages = base
                    coveringKey = null
                }

                else -> {
                    pages = stack
                    coveringKey = null
                }
            }
        }
    }

    // A single page is a closed seam: the start column takes the whole width and the end column has
    // none.
    val targetCursor = (stack.size - 2).coerceAtLeast(0).toFloat()
    val targetSeam = if (stack.size >= 2) 1f else 0f

    val cursor = remember { Animatable(targetCursor) }
    val seam = remember { Animatable(targetSeam) }
    // Keyed on the page it covers, so every replacement starts from zero on its own first frame.
    val cover = remember(coveringKey) { Animatable(0f) }
    var gesturing by remember { mutableStateOf(false) }

    // Structural key: a recomposition that produces an equal stack must not cancel a running travel.
    val stackKeys = remember(stack) { stack.map { it.contentKey } }

    LaunchedEffect(stackKeys, gesturing) {
        // A gesture owns both states while it runs; snapping them from here would fight the finger.
        if (gesturing) return@LaunchedEffect

        coroutineScope {
            launch {
                val travel = abs(targetCursor - cursor.value)
                cursor.animateTo(
                    targetValue = targetCursor,
                    animationSpec = tween(
                        durationMillis = travelDuration(travel, SPLIT_PAGE_DURATION),
                        easing = IosPageEasing,
                    ),
                )
            }
            launch {
                val travel = abs(targetSeam - seam.value)
                seam.animateTo(
                    targetValue = targetSeam,
                    animationSpec = tween(
                        durationMillis = travelDuration(travel, SPLIT_SEAM_DURATION),
                        easing = IosPageEasing,
                    ),
                )
            }
            launch {
                if (coveringKey != null) {
                    cover.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(SPLIT_COVER_DURATION, easing = IosPageEasing),
                    )
                }
            }
        }

        // Every travel has landed. Only now is it safe to drop the pages that were kept around for
        // their animation: trimming them any earlier would leave the covering page floating over a
        // hole where the page underneath used to be.
        if (pages != stack) pages = stack
        coveringKey = null
    }

    // Enabled only while the back press stays inside the strip. Leaving it - `[List] -> [Tab]` - is a
    // scene change, and `NavDisplay` has to run that one itself.
    PredictiveBackHandler(enabled = stack.size >= 2) { progressFlow ->
        val slidesStrip = stack.size >= 3
        gesturing = true
        try {
            progressFlow.collect { event ->
                if (slidesStrip) {
                    cursor.snapTo((targetCursor - event.progress).coerceAtLeast(0f))
                } else {
                    seam.snapTo((1f - event.progress).coerceAtLeast(0f))
                }
            }
            // Pop before the gesture is released, so the shorter stack and the released finger land
            // in the same frame and the effect above carries on from where the finger stopped.
            onBack()
        } finally {
            // A cancelled gesture leaves the state where the finger stopped; the effect above then
            // animates back over the remaining distance.
            gesturing = false
        }
    }

    val settledCursor by rememberUpdatedState(targetCursor)

    // Pages that can show a pixel. Only the integers of the cursor are read during composition, so a
    // whole travel recomposes the strip twice at most; the fraction is read by the layout phase,
    // which has to relayout anyway.
    val window by remember {
        derivedStateOf {
            val live = cursor.value
            val settled = settledCursor
            floor(min(live, settled)).toInt() to ceil(max(live, settled)).toInt() + 1
        }
    }

    val firstIndex = window.first.coerceIn(0, pages.lastIndex)
    // A covered page sits in the end column, one slot past the last travel target, so the window has
    // to reach it; the covering page itself is always the very last one.
    val lastIndex = if (coveringKey != null) {
        pages.lastIndex
    } else {
        window.second.coerceIn(firstIndex, pages.lastIndex)
    }

    // Last line of defence: `SaveableStateHolder` throws if one content key is composed twice, so the
    // list handed to composition is filtered rather than trusted. The covering page always wins - it
    // is the one the user just opened - and anything else repeating its key is dropped.
    val visible = remember(pages, firstIndex, lastIndex, coveringKey) {
        val used = HashSet<Any>()
        if (coveringKey != null) used.add(pages.last().contentKey)
        pages.subList(firstIndex, lastIndex + 1).mapIndexedNotNull { offset, entry ->
            val index = firstIndex + offset
            val covering = coveringKey != null && index == pages.lastIndex
            val hasNext = index < pages.lastIndex
            when {
                covering -> StripPage(entry, index, covering = true, hasNext = hasNext)
                used.add(entry.contentKey) -> StripPage(entry, index, covering = false, hasNext = hasNext)
                else -> null
            }
        }
    }

    val scrim = MaterialTheme.colorScheme.scrim
    // Filled by the measure pass and read by the draw pass of the same frame, which is the only
    // moment both are known: a shadow sits on the page *below*, so it cannot be drawn by the page
    // that casts it.
    val shadows = remember { StripShadows() }

    Layout(
        modifier = modifier
            .clipToBounds()
            .drawWithContent {
                drawContent()
                shadows.draw(this, scrim)
            },
        content = {
            // Exactly one composition per content key, always.
            visible.forEach { page ->
                key(page.entry.contentKey) {
                    StripPane(
                        entry = page.entry,
                        index = page.index,
                        hasNext = page.hasNext,
                        cursor = cursor,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val totalHeight = constraints.maxHeight

        val startFull = (totalWidth * ratio.floatValue).roundToInt().coerceIn(1, totalWidth - 1)
        val endFull = totalWidth - startFull

        // The live start column: the whole width while the seam is closed, the ratio once it is
        // open. The end column keeps its full width throughout, so opening the seam slides the end
        // page in from beyond the right edge instead of squeezing it.
        val startWidth = totalWidth + (startFull - totalWidth) * seam.value
        val position = cursor.value

        // Rounded once and reused as the neighbour's left edge, so two adjacent pages land on the
        // same pixel and the seam cannot crack.
        fun anchor(offset: Float): Int =
            if (offset <= 1f) {
                (offset * startWidth).roundToInt()
            } else {
                (startWidth + (offset - 1f) * endFull).roundToInt()
            }

        shadows.clear()
        val placements = ArrayList<Pair<Placeable, Int>>(measurables.size)
        visible.forEachIndexed { slot, page ->
            val left: Int
            val width: Int
            if (page.covering) {
                // Sliding in from beyond the right edge and landing on the end column, so the page
                // it replaces is simply covered instead of being pushed aside.
                width = endFull
                left = anchor(1f) + (endFull * (1f - cover.value)).roundToInt()
            } else {
                val offset = page.index - position
                left = anchor(offset)
                // A page already in a column keeps that column's width, so it slides at full size
                // instead of unfurling from zero width and reflowing its content. Only the page
                // between the two columns interpolates, which is exactly the chain travel.
                width = when {
                    offset >= 1f -> endFull
                    offset <= 0f -> startWidth.roundToInt()
                    else -> anchor(offset + 1f) - left
                }.coerceAtLeast(1)
            }
            placements += measurables[slot].measure(Constraints.fixed(width, totalHeight)) to left

            // Only a page that covers another one casts a shadow. A page travelling between the two
            // columns is being revealed by the strip rather than laid on top of anything, so a shadow
            // there reads as a smudge sliding across the screen and says nothing about depth. It
            // holds at full strength while the page travels and fades over the last stretch as it
            // lands; fading linearly from the first frame would peak while the page is still off
            // screen and be gone by the time the two pages overlap.
            if (page.covering) {
                val strength = ((1f - cover.value) * SHADOW_FADE_FROM).coerceIn(0f, 1f)
                if (strength > 0f && left > 0) {
                    shadows.add(left.toFloat(), width.toFloat(), strength)
                }
            }
        }

        layout(totalWidth, totalHeight) {
            // Drawn in strip order, so the covering page lands on top of the page it covers. Placed
            // in absolute coordinates rather than mirrored ones: everything above is left-to-right.
            placements.forEach { (placeable, left) -> placeable.place(left, 0) }
        }
    }
}

/**
 * The shadows pages cast on the page to their left, collected during measure and drawn in the same
 * frame.
 *
 * They are drawn on top of every page rather than by the page casting them: the gradient has to land
 * on the page *underneath*, which the caster's own clipped bounds exclude. Since it spans only the
 * strip to the left of the casting page's leading edge, it never touches the caster itself.
 */
private class StripShadows {
    private val edges = ArrayList<FloatArray>(4)

    fun clear() = edges.clear()

    /** @param left leading edge of the casting page, in strip coordinates. */
    fun add(left: Float, width: Float, strength: Float) {
        edges.add(floatArrayOf(left, width, strength))
    }

    fun draw(scope: DrawScope, scrim: Color) {
        with(scope) {
            edges.forEach { edge ->
                val left = edge[0]
                val width = edge[1] * STRIP_SHADOW_WIDTH
                val strength = edge[2]
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, scrim.copy(alpha = STRIP_SHADOW_ALPHA * strength)),
                        startX = left - width,
                        endX = left,
                    ),
                    topLeft = Offset(left - width, 0f),
                    size = Size(width, size.height),
                )
            }
        }
    }
}

/**
 * The stack with repeated content keys collapsed, keeping the newest of each.
 *
 * `SaveableStateHolder` treats a repeated key as a programming error and crashes, so a stack that
 * happens to name one target twice must never reach composition. Collapsing keeps the navigation
 * result intact for the user - the newest copy of a page is the one on screen.
 */
private fun <T : Any> List<NavEntry<T>>.deduped(): List<NavEntry<T>> {
    val seen = HashSet<Any>(size)
    val kept = ArrayList<NavEntry<T>>(size)
    for (entry in this) {
        if (!seen.add(entry.contentKey)) {
            kept.removeAll { it.contentKey == entry.contentKey }
        }
        kept += entry
    }
    return kept
}

/** The entries the strip was last reconciled against; bookkeeping only, never read by layout. */
private class SeenEntries<T : Any> {
    var entries: List<NavEntry<T>>? = null
}

/** A retarget keeps the speed of the full travel: a short remaining distance takes proportionally less. */
private fun travelDuration(travel: Float, fullDuration: Int): Int =
    (fullDuration * travel.coerceIn(0f, 1f)).roundToInt()
        .coerceIn(SPLIT_MIN_ANIMATION_DURATION, fullDuration)

/**
 * One page of the strip.
 *
 * The column it claims flips once, when the cursor passes halfway across it, so a page moving from
 * the end column to the start column does not swap its own chrome on the first frame of the travel.
 */
@Composable
private fun <T : Any> StripPane(
    entry: NavEntry<T>,
    index: Int,
    hasNext: Boolean,
    cursor: Animatable<Float, *>,
) {
    val slot by remember(index) {
        derivedStateOf { if (index - cursor.value <= 0.5f) PaneSlot.Start else PaneSlot.End }
    }

    CompositionLocalProvider(
        LocalPaneSlot provides slot,
        LocalPaneHasNext provides hasNext,
    ) {
        entry.Content()
    }
}
