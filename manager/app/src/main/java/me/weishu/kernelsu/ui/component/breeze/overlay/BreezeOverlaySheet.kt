package me.weishu.kernelsu.ui.component.breeze.overlay

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/** Insets equal to the sheet's drag offset, so a sheet stopped mid-screen consumes no top inset. */

@OptIn(ExperimentalMaterial3Api::class)
private class SheetWindowInsets(private val state: SheetState) : WindowInsets {
    // `requireOffset()` throws before the first layout pass; report 0 until anchors exist.
    private val offset: Int
        get() = if (state.hasExpandedState) state.requireOffset().toInt().coerceAtLeast(0) else 0

    override fun getTop(density: Density): Int = offset

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getBottom(density: Density): Int = 0

    override fun equals(other: Any?): Boolean =
        this === other || (other is SheetWindowInsets && other.state == state)

    override fun hashCode(): Int = state.hashCode()
}

/**
 * Material3 [BottomSheet] hosted as a layer in [BreezeOverlay] rather than its own dialog window.
 *
 * Parameters mirror `ModalBottomSheet` so a sheet body can be moved between the two unchanged.
 * The sheet is only as tall as its content: fill the window by giving the body a scrollable
 * `weight(1f)` child, not by passing a size.
 *
 * @param visible whether the sheet should be shown; the composable must remain in the tree while
 *   [visible] is `false` so the slide-out animation can complete. M3 unloads instead, which would
 *   cut the exit animation short.
 * @param sheetState anchors and current position; build it with `rememberBottomSheetState`.
 * @param sheetMaxWidth width cap; the sheet is centred in the overlay window when narrower.
 * @param contentWindowInsets insets applied inside the sheet surface, to the drag handle and the
 *   content alike. The surface itself still reaches the window edge, which is what lets an
 *   expanded sheet paint its background under the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreezeOverlaySheet(
    onDismissRequest: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    // Modal sheets sit flat: 0.dp matches ModalBottomSheet, while plain BottomSheet uses 1.dp.
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.modalWindowInsets
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    // Keep the overlay layer mounted until `state.hide()` finishes sliding out.
    var present by remember { mutableStateOf(false) }
    if (visible) present = true

    // Defer `onDismissRequest` until `state.hide()` completes so caller state stays intact during exit.
    var dismissing by remember { mutableStateOf(false) }

    // Reset above the `!present` early return so `dismissing` is cleared after the layer unmounts.
    LaunchedEffect(visible) {
        if (!visible) dismissing = false
    }

    if (!present) return

    var sheetHeight by remember { mutableIntStateOf(0) }

    // Wait for a non-zero measured height and one frame before `show()` so BottomSheet does not
    // promote the initial partial anchor to expanded while anchors are still converged at 0.
    var shown by remember { mutableStateOf(false) }
    val onScreen = visible && !dismissing
    LaunchedEffect(sheetState, onScreen) {
        if (!onScreen) {
            sheetState.hide()
            present = false
            if (dismissing) onDismissRequest()
            return@LaunchedEffect
        }
        if (!shown) {
            snapshotFlow { sheetState.hasExpandedState && sheetHeight > 0 }.first { it }
            withFrameNanos { }
            shown = true
        }
        sheetState.show()
    }

    val sheetWindowInsets = remember(sheetState) { SheetWindowInsets(sheetState) }

    BreezeOverlay(
        onDismissRequest = { dismissing = true },
        visible = true,
        dismissOnClickOutside = dismissOnClickOutside,
        scrimVisible = visible && shown,
        appearance = OverlayAppearance.Sheet,
        surfaceColor = containerColor,
    ) {
        // The surface reaches the window edge; only the handle and content are inset. Consuming the
        // offset keeps a partially expanded sheet from leaving a status-bar-high strip above it.
        BottomSheet(
            // Caller modifiers go first: a caller padding such as `statusBarsPadding()` reads the
            // window insets live, so applying it inside `consumeWindowInsets(sheetWindowInsets)`
            // would make its top padding shrink by the drag offset and the sheet's constraints grow
            // by the same amount on every drag frame — a 60Hz oscillation that blocks collapsing.
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(modifier)
                .alpha(if (shown) 1f else 0f)
                .onSizeChanged { sheetHeight = it.height }
                .consumeWindowInsets(sheetWindowInsets),
            state = sheetState,
            onDismissRequest = { dismissing = true },
            maxWidth = sheetMaxWidth,
            gesturesEnabled = sheetGesturesEnabled,
            // Back is handled by OverlayLayer so stacked modals pop top-down.
            backHandlerEnabled = false,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            dragHandle = dragHandle,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}
