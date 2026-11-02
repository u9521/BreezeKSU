package me.weishu.kernelsu.ui.component.breeze.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.model.MarkdownState
import com.mikepenz.markdown.model.State
import me.weishu.kernelsu.ui.component.breeze.markdown.LazyMarkdownContent
import me.weishu.kernelsu.ui.component.breeze.markdown.rememberLazyMarkdownState
import me.weishu.kernelsu.ui.component.breeze.markdown.style.BreezeMarkdownStyle
import me.weishu.kernelsu.ui.component.breeze.markdown.style.Default
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeAlertDialog
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlay

/** The non-dismissible spinner dialog, drawn as a modal layer of the overlay window. */
@Composable
fun LoadingDialogBreeze(visible: Boolean) {
    BreezeOverlay(
        onDismissRequest = {},
        visible = visible,
        dismissOnClickOutside = false,
        dismissOnBackPress = false,
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(100.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }
}

/** Stateless confirmation dialog for [ConfirmBody], hosted inside [BreezeAlertDialog]. */
@Composable
fun ConfirmDialogBreeze(
    body: ConfirmBody?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Retain the last non-null body while the exit animation plays.
    var lastBody by remember { mutableStateOf(body) }
    if (body != null) lastBody = body
    val shown = body ?: lastBody ?: return

    val markdownContent = (shown.body as? ConfirmBody.Body.Markdown)?.markdown
    val markdownState = markdownContent?.let { rememberLazyMarkdownState(it) }
    val markdownStateValue by (markdownState?.state?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })
    val parsing = markdownStateValue.isParsing(markdownContent)

    BreezeAlertDialog(
        onDismissRequest = onDismiss,
        visible = body != null,
        modifier = Modifier.animateContentSize(
            // Smoothly resize when parsed content replaces the loading spinner.
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
        title = { Text(shown.title) },
        text = {
            val uriHandler = LocalUriHandler.current
            val waiting = shown.loading || parsing
            // Keep both layers composed and cross-fade their alphas.
            val contentAlpha by animateFloatAsState(
                targetValue = if (waiting) 0f else 1f,
                animationSpec = BodyFadeSpec,
                label = "ConfirmContentAlpha",
            )
            val spinnerAlpha by animateFloatAsState(
                targetValue = if (waiting) 1f else 0f,
                animationSpec = BodyFadeSpec,
                label = "ConfirmSpinnerAlpha",
            )
            Box {
                Box(modifier = Modifier.graphicsLayer { alpha = contentAlpha }) {
                    when (val content = shown.body) {
                        is ConfirmBody.Body.Markdown -> MarkdownDialogBody(
                            content = content.markdown,
                            state = markdownState!!,
                            onLinkClick = uriHandler::openUri,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        is ConfirmBody.Body.Text -> Box(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        ) {
                            Text(text = content.text)
                        }

                        is ConfirmBody.Body.Empty -> EmptyStateBody(content)

                        is ConfirmBody.Body.Custom -> content.content()
                    }
                }
                if (spinnerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LoadingHeight)
                            .graphicsLayer { alpha = spinnerAlpha },
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // Always enable confirm when there is no dismiss button so the dialog has an exit.
                enabled = shown.onDismiss == null ||
                        shown.confirmEnabled && !shown.loading && !parsing,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(shown.confirm ?: stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = shown.onDismiss?.let {
            {
                OutlinedButton(
                    onClick = onDismiss,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(shown.dismiss ?: stringResource(id = android.R.string.cancel))
                }
            }
        },
    )
}

/** Centered muted icon, message, and optional [OutlinedButton] action for [ConfirmBody.Body.Empty]. */
@Composable
internal fun EmptyStateBody(content: ConfirmBody.Body.Empty) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = content.icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = content.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        content.action?.let { action ->
            OutlinedButton(onClick = action.onClick, shapes = ButtonDefaults.shapes()) {
                Text(action.label)
            }
        }
    }
}

/**
 * True while [this] is still parsing [content] (including the one-frame gap before
 * [MarkdownState] updates from the previous document's [State.Success]).
 */
private fun State?.isParsing(content: String?): Boolean = when {
    this == null -> false
    content.isNullOrEmpty() -> false
    this is State.Loading -> true
    this is State.Success -> this.content != content
    else -> false
}

/** Horizontal padding of the dialog text slot. */
internal val MarkdownDialogSidePadding = 24.dp

/** Markdown body that bleeds past the card's 24dp horizontal padding so wide tables reach the dialog edge. */
@Composable
internal fun MarkdownDialogBody(
    content: String,
    state: MarkdownState,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val bodyWidth = (maxWidth + MarkdownDialogSidePadding * 2).coerceAtMost(DialogMaxBodyWidth)
        Box(
            modifier = Modifier
                .width(bodyWidth)
                .bleedHorizontally(MarkdownDialogSidePadding),
        ) {
            LazyMarkdownContent(
                content = content,
                style = BreezeMarkdownStyle.Default,
                state = state,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = MarkdownDialogSidePadding),
                onLinkClick = onLinkClick,
            )
        }
    }
}

/** Measures the child [amount] wider on each side and places it at `-amount`, undoing parent horizontal padding. */
private fun Modifier.bleedHorizontally(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth + extra
            } else {
                constraints.maxWidth
            },
        ),
    )
    val width = (placeable.width - extra).coerceAtLeast(0)
    layout(width, placeable.height) { placeable.place(-amount.roundToPx(), 0) }
}

/** Maximum dialog text slot width (560dp dialog max width minus 48dp horizontal padding). */
private val DialogMaxBodyWidth = 512.dp

/** Height of the spinner slot, so the dialog does not collapse to the buttons while loading. */
private val LoadingHeight = 96.dp

/** Fade used when the body slot swaps between the spinner and the content. */
private val BodyFadeSpec = tween<Float>(durationMillis = 300)
