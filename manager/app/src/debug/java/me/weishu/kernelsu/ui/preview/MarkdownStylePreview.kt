package me.weishu.kernelsu.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.BreezePreviewTheme
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmBody
import me.weishu.kernelsu.ui.component.breeze.dialog.ConfirmDialogBreeze
import me.weishu.kernelsu.ui.component.breeze.dialog.MarkdownDialogBody
import me.weishu.kernelsu.ui.component.breeze.dialog.MarkdownDialogSidePadding
import me.weishu.kernelsu.ui.component.breeze.markdown.LazyMarkdownContent
import me.weishu.kernelsu.ui.component.breeze.markdown.rememberLazyMarkdownState
import me.weishu.kernelsu.ui.component.breeze.markdown.style.BreezeMarkdownStyle
import me.weishu.kernelsu.ui.component.breeze.markdown.style.Default

/**
 * The reading style, rendered against three documents.
 *
 * Two compatibility passes cover the compatibility corpus, so every construct is checked even
 * when changelogs rarely use it.
 *
 * The showcase passes render a realistic module changelog at the width of the narrowest phone
 * the style is tuned for. One goes through the real dialog body, so the table is judged on the
 * path a user sees; a dark pass follows, because a changelog is just as often read at night.
 *
 * The compatibility previews run to the height cap the screenshot harness enforces - asking
 * for more silently truncates the render.
 */
@Preview(name = "inline", showBackground = true, widthDp = 360, heightDp = 3000)
@Composable
fun MarkdownInlinePreview() = MarkdownPreview(MARKDOWN_COMPAT_INLINE)

@Preview(name = "block", showBackground = true, widthDp = 360, heightDp = 3000)
@Composable
fun MarkdownBlockPreview() = MarkdownPreview(MARKDOWN_COMPAT_BLOCK)

/** The real path: the dialog's own slot, whose padding the body undoes. */
@Preview(name = "showcase - dialog", showBackground = true, widthDp = 360, heightDp = 1900)
@Composable
fun MarkdownShowcaseDialogPreview() = DialogPreview(MARKDOWN_SHOWCASE)

@Preview(name = "showcase - dark", showBackground = true, widthDp = 360, heightDp = 1900)
@Composable
fun MarkdownShowcaseDarkPreview() = DialogPreview(MARKDOWN_SHOWCASE, dark = true)

/**
 * The whole confirm dialog, actions included.
 *
 * The other passes render only the body, leaving confirm and dismiss unchecked - and they
 * silently fell back to unstyled text buttons once. This pass exists so a change to them shows
 * up in the render rather than in a device screenshot.
 */
@Preview(name = "confirm dialog - awaiting", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ConfirmDialogAwaitingPreview() = ConfirmDialogPreviewImpl(awaiting = true)

@Preview(name = "confirm dialog", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ConfirmDialogPreview() = ConfirmDialogPreviewImpl(awaiting = false)

@Composable
private fun ConfirmDialogPreviewImpl(awaiting: Boolean) {
    BreezePreviewTheme {
        val body = remember(awaiting) {
            ConfirmBody(
                title = "Module",
                body = ConfirmBody.Body.Markdown(if (awaiting) "" else MARKDOWN_SHOWCASE),
                confirm = "OK",
                dismiss = "Cancel",
                loading = awaiting,
                onDismiss = {},
            )
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            ConfirmDialogBreeze(body = body, onConfirm = {}, onDismiss = {})
        }
    }
}

@Composable
private fun DialogPreview(
    document: String,
    dark: Boolean = false,
) {
    BreezePreviewTheme(dark = dark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(modifier = Modifier.padding(horizontal = MarkdownDialogSidePadding)) {
                MarkdownDialogBody(
                    content = document,
                    state = rememberLazyMarkdownState(document),
                    onLinkClick = {},
                )
            }
        }
    }
}

@Composable
private fun MarkdownPreview(document: String) {
    BreezePreviewTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            LazyMarkdownContent(
                content = document,
                style = BreezeMarkdownStyle.Default,
                state = rememberLazyMarkdownState(document),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = MarkdownDialogSidePadding, vertical = 8.dp),
            )
        }
    }
}