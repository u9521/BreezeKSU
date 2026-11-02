package me.weishu.kernelsu.ui.component.breeze.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

private val OuterRadius = 16.dp
private val InnerRadius = 4.dp

/** Current row shapes provided by [SegmentedListGroup], or `null` when used standalone. */
val LocalSegmentedItemShapes = compositionLocalOf<ListItemShapes?> { null }

@Immutable
internal class SegmentedEntry(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

/** DSL scope for [SegmentedListGroup]. */
class SegmentedListScope {
    internal val entries = mutableListOf<SegmentedEntry>()

    /** Adds a row with stable [key] and 3D fold [visible] state. */
    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        entries.add(SegmentedEntry(key ?: entries.size, visible, content))
    }
}

/** Grouped list that assigns 16.dp outer / 4.dp inner corners via [LocalSegmentedItemShapes] and folds hidden rows in 3D. */
@Composable
fun SegmentedListGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SegmentedListScope.() -> Unit,
) {
    // Rebuild on each composition so cancelled compositions never retain stale rows.
    val entries = SegmentedListScope().apply(content).entries
    if (entries.isEmpty()) return

    val visibleKeys = entries.filter { it.visible }.map { it.key }
    val firstKey = visibleKeys.firstOrNull()
    val lastKey = visibleKeys.lastOrNull()

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }

        entries.forEach { entry ->
            key(entry.key) {
                SegmentedRowHost(
                    entry = entry,
                    isFirst = entry.key == firstKey,
                    isLast = entry.key == lastKey,
                )
            }
        }
    }
}

/** Convenience [SegmentedListGroup] overload for a pre-built list of row composables. */
@Composable
fun SegmentedListGroup(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    title: String = "",
    keys: List<Any?> = emptyList(),
) {
    SegmentedListGroup(modifier = modifier, title = title) {
        items.forEachIndexed { index, row ->
            item(key = keys.getOrNull(index) ?: index, content = row)
        }
    }
}

/** Animates one row's top-hinged 3D fold, height, opacity, and corner radii (specs in `SegmentedMotion.kt`). */
@Composable
private fun SegmentedRowHost(
    entry: SegmentedEntry,
    isFirst: Boolean,
    isLast: Boolean,
) {
    AnimatedVisibility(
        visible = entry.visible,
        enter = expandVertically(RowSizeEnterSpec, expandFrom = Alignment.Top) +
                fadeIn(RowAlphaEnterSpec),
        exit = shrinkVertically(RowSizeExitSpec, shrinkTowards = Alignment.Top) +
                fadeOut(RowAlphaExitSpec),
    ) {
        // Freeze edge flags while exiting so outer corners do not sharpen mid-fold.
        val exiting = transition.targetState == EnterExitState.PostExit
        val frozenFirst = remember { mutableStateOf(isFirst) }
        val frozenLast = remember { mutableStateOf(isLast) }
        SideEffect {
            if (!exiting) {
                frozenFirst.value = isFirst
                frozenLast.value = isLast
            }
        }
        val first = if (exiting) frozenFirst.value else isFirst
        val last = if (exiting) frozenLast.value else isLast

        val topRadius by animateDpAsState(
            targetValue = if (first) OuterRadius else InnerRadius,
            animationSpec = RowCornerSpec,
            label = "SegmentedTopRadius",
        )
        val bottomRadius by animateDpAsState(
            targetValue = if (last) OuterRadius else InnerRadius,
            animationSpec = RowCornerSpec,
            label = "SegmentedBottomRadius",
        )
        val shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )

        // 1f = flat, 0f = folded.
        val fold by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) RowFoldEnterSpec else RowFoldExitSpec
            },
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

        val shapes = ListItemDefaults.shapes(shape = shape).copy(pressedShape = shape)

        CompositionLocalProvider(LocalSegmentedItemShapes provides shapes) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        rotationX = RowFoldDegrees * (1f - fold)
                        scaleX = lerp(RowFoldScale, 1f, fold)
                        scaleY = lerp(RowFoldScale, 1f, fold)
                        translationY = -RowFoldRise.toPx() * (1f - fold)
                        cameraDistance = RowFoldCameraDistance
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .padding(top = if (first) 0.dp else ListItemDefaults.SegmentedGap),
            ) {
                entry.content()
            }
        }
    }
}