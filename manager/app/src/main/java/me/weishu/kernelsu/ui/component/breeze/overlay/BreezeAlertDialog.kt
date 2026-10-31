package me.weishu.kernelsu.ui.component.breeze.overlay

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse

/**
 * An M3 `AlertDialog` laid out inside [BreezeOverlay].
 *
 * Mirrors Material3's internal `AlertDialogContent` / `AlertDialogImpl` inside a full-screen
 * overlay window so animated body height changes never trigger `WindowManager` relayouts.
 */
@Composable
fun BreezeAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    visible: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) {
    // Keep the layer mounted via `present` until `motion` finishes its exit transition.
    val motion = remember { MutableTransitionState(false) }
    var present by remember { mutableStateOf(false) }

    if (visible) present = true
    motion.targetState = visible

    if (present && !visible && !motion.currentState && !motion.targetState) present = false

    if (!present) return

    BreezeOverlay(
        onDismissRequest = onDismissRequest,
        visible = true,
        scrimVisible = visible,
        dismissOnClickOutside = dismissOnClickOutside,
        dismissOnBackPress = dismissOnBackPress,
        surfaceColor = containerColor,
    ) {
        val slidePx = with(LocalDensity.current) { DialogEnterOffset.toPx() }
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        AnimatedVisibility(
            visibleState = motion,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(DialogEnterMillis, easing = DialogEnterEasing)) +
                    slideInVertically(tween(DialogEnterMillis, easing = DialogEnterEasing)) {
                        slidePx.toInt()
                    },
            exit = fadeOut(tween(DialogExitMillis, easing = DialogExitEasing)) +
                    slideOutVertically(tween(DialogExitMillis, easing = DialogExitEasing)) {
                        slidePx.toInt()
                    },
        ) {
            // Restore BoxScope inside AnimatedVisibilityScope so Modifier.align centers the card.
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = modifier
                        .align(Alignment.Center)
                        .padding(insets)
                        .sizeIn(
                            minWidth = DialogMinWidth,
                            maxWidth = minOf(DialogMaxWidth, platformDialogWidthCap()),
                        )
                        .semantics { paneTitle = DialogPaneTitle }
                        // Consume taps on the card so they do not reach the scrim behind it.
                        .pointerInput(Unit) {},
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation,
                ) {
                    AlertDialogContent(
                        buttons = { AlertDialogButtons(confirmButton, dismissButton) },
                        icon = icon,
                        title = title,
                        text = text,
                        iconContentColor = iconContentColor,
                        titleContentColor = titleContentColor,
                        textContentColor = textContentColor,
                    )
                }
            }
        }
    }
}

/** Dialog card layout; `weight(1f, fill = false)` on [text] lets long content scroll without pushing buttons off-screen. */
@Composable
private fun AlertDialogContent(
    buttons: @Composable () -> Unit,
    icon: (@Composable () -> Unit)?,
    title: (@Composable () -> Unit)?,
    text: (@Composable () -> Unit)?,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    Column(modifier = Modifier.padding(DialogPadding)) {
        icon?.let {
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(
                    Modifier
                        .padding(IconPadding)
                        .align(Alignment.CenterHorizontally)
                ) { it() }
            }
        }
        title?.let {
            ProvideContentColorTextStyle(
                titleContentColor,
                MaterialTheme.typography.headlineSmall
            ) {
                Box(
                    Modifier
                        .padding(TitlePadding)
                        .align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally),
                ) {
                    it()
                }
            }
        }
        text?.let {
            ProvideContentColorTextStyle(textContentColor, MaterialTheme.typography.bodyMedium) {
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(TextPadding)
                        .align(Alignment.Start),
                ) {
                    it()
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.End)) { buttons() }
    }
}

/**
 * Confirm and dismiss side by side, stacked when they do not fit.
 *
 * Mirrors `AlertDialogFlowRow`: the layout direction is flipped so confirm is first in the flow
 * (it wraps to the leading edge when stacked) while still reading after dismiss in LTR.
 */
@Composable
private fun AlertDialogButtons(
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)?,
) {
    val originalLayoutDirection = LocalLayoutDirection.current
    val buttonPaddingFromMics =
        LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } - ButtonDefaults.MinHeight
    CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection.flip()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ButtonsMainAxisSpacing),
            verticalArrangement = Arrangement.spacedBy(
                (ButtonsCrossAxisSpacing - buttonPaddingFromMics)
                    .coerceIn(0.dp, ButtonsCrossAxisSpacing),
            ),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection) {
                confirmButton()
                dismissButton?.invoke()
            }
        }
    }
}

/** Sets [LocalContentColor] and [LocalTextStyle] together, as Material3's own dialogs do. */
@Composable
private fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit,
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

private fun LayoutDirection.flip(): LayoutDirection = when (this) {
    LayoutDirection.Ltr -> LayoutDirection.Rtl
    LayoutDirection.Rtl -> LayoutDirection.Ltr
}

/** Platform floating-dialog width cap (`android:dimen/config_prefDialogWidth`), or [DialogMaxWidth]. */
@SuppressLint("DiscouragedApi")
@Composable
private fun platformDialogWidthCap(): Dp {
    val resources = LocalResources.current
    val density = LocalDensity.current
    return remember(resources, density) {
        val id = resources.getIdentifier("config_prefDialogWidth", "dimen", "android")
        if (id == 0) {
            DialogMaxWidth
        } else {
            with(density) { resources.getDimensionPixelSize(id).toDp() }
        }
    }
}

// Material3 AlertDialog dimensions and platform popup_enter_material / popup_exit_material motion.
private val DialogMinWidth = 280.dp
private val DialogMaxWidth = 560.dp
private const val DialogEnterMillis = 220
private const val DialogExitMillis = 150
private val DialogEnterOffset = 20.dp
private val DialogEnterEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val DialogExitEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val DialogPadding = PaddingValues(24.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 8.dp
private const val DialogPaneTitle = "Dialog"
