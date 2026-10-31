package me.weishu.kernelsu.ui.component.breeze.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Top-rounded press shape anchored to the tab row baseline. */
private val TabPressShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
private val IndicatorHeight = 3.dp
private val IndicatorShape = RoundedCornerShape(IndicatorHeight / 2)

/** Composable slots for a single tab in [ExpressiveTabRow]. */
data class TabSpec(
    val text: @Composable () -> Unit,
    val icon: (@Composable () -> Unit)? = null,
)

/**
 * [PrimaryTabRow] with a top-rounded press overlay (replacing [Tab]'s circular ripple) and a
 * stretchy content-width underline whose leading edge advances first, trailing edge contracts,
 * and leading edge recoils on close.
 */
@Composable
fun ExpressiveTabRow(
    selectedTabIndex: Int,
    tabs: List<TabSpec>,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    PrimaryTabRow(
        selectedTabIndex = safeIndex,
        modifier = modifier,
        containerColor = containerColor,
        indicator = { StretchyIndicator(safeIndex, indicatorColor) },
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = safeIndex == index
            val interactionSource = remember { MutableInteractionSource() }
            Box {
                Tab(
                    selected = selected,
                    onClick = {},
                    modifier = Modifier.fillMaxSize(),
                    text = {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.titleSmallEmphasized,
                            content = tab.text,
                        )
                    },
                    icon = tab.icon,
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onTabClick(index) },
                        )
                        .clip(TabPressShape)
                        .indication(interactionSource, ripple()),
                )
            }
        }
    }
}

/** Content-width underline driven by independent start and end springs. */
@Composable
private fun TabIndicatorScope.StretchyIndicator(
    selectedIndex: Int,
    color: Color,
) {
    val scope = rememberCoroutineScope()
    val edges = remember { IndicatorEdges() }
    Box(
        Modifier
            .tabIndicatorLayout { measurable, constraints, tabPositions ->
                val position = tabPositions.getOrNull(selectedIndex)
                if (position == null) {
                    layout(0, 0) {}
                } else {
                    edges.animateTo(position, scope, this)

                    val start = edges.start.roundToInt()
                    val end = edges.end.roundToInt().coerceAtLeast(start)
                    val width = end - start
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = width,
                            maxWidth = width,
                            minHeight = 0,
                            maxHeight = Constraints.Infinity,
                        ),
                    )
                    val rowHeight = if (constraints.maxHeight == Constraints.Infinity) {
                        placeable.height
                    } else {
                        constraints.maxHeight
                    }
                    layout(constraints.maxWidth, rowHeight) {
                        placeable.place(start, rowHeight - placeable.height)
                    }
                }
            }
            .height(IndicatorHeight)
            .background(color, IndicatorShape),
    )
}

/** Pixel bounds of the indicator, read during measurement. */
private class IndicatorEdges {
    private var startAnim: Animatable<Float, AnimationVector1D>? = null
    private var endAnim: Animatable<Float, AnimationVector1D>? = null

    // Track targets synchronously; Animatable.targetValue updates only after the coroutine starts.
    private var startTarget = Float.NaN
    private var endTarget = Float.NaN
    private var moveJob: Job? = null

    val start: Float get() = startAnim?.value ?: 0f
    val end: Float get() = endAnim?.value ?: 0f

    fun animateTo(position: TabPosition, scope: CoroutineScope, density: Density) {
        with(density) {
            val inset = (position.width - position.contentWidth) / 2
            val targetStart = (position.left + inset).toPx()
            val targetEnd = (position.right - inset).toPx()

            val start = startAnim
            val end = endAnim
            if (start == null || end == null) {
                startAnim = Animatable(targetStart)
                endAnim = Animatable(targetEnd)
                startTarget = targetStart
                endTarget = targetEnd
                return
            }
            if (targetStart == startTarget && targetEnd == endTarget) return

            val movingRight = targetEnd > endTarget
            startTarget = targetStart
            endTarget = targetEnd

            moveJob?.cancel()
            moveJob = scope.launch {
                val lead = if (movingRight) end else start
                val trail = if (movingRight) start else end
                val leadTarget = if (movingRight) targetEnd else targetStart
                val trailTarget = if (movingRight) targetStart else targetEnd
                val direction = if (movingRight) 1f else -1f

                // 1. Advance leading edge to target.
                lead.animateTo(leadTarget, ReachSpec)

                // 2. Contract trailing edge onto leading edge.
                val contracting = launch { trail.animateTo(trailTarget, ContractSpec) }

                // 3. Kick leading edge into an underdamped recoil when contraction nears completion.
                val from = trail.value
                val span = abs(trailTarget - from)
                val threshold = span * OvershootAt
                snapshotFlow { abs(trailTarget - trail.value) }
                    .first { it <= threshold }
                lead.animateTo(
                    targetValue = leadTarget,
                    animationSpec = OvershootSpec,
                    initialVelocity = direction * OvershootKick,
                )
                contracting.join()
            }
        }
    }
}

private val ReachSpec = spring<Float>(dampingRatio = 1f, stiffness = 900f)
private val ContractSpec = spring<Float>(dampingRatio = 1f, stiffness = 500f)
private val OvershootSpec = spring<Float>(dampingRatio = 0.55f, stiffness = 500f)
private const val OvershootAt = 0.2f
private const val OvershootKick = 900f
