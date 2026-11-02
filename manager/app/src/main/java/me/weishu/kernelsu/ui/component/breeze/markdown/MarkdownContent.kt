package me.weishu.kernelsu.ui.component.breeze.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.MarkdownState
import com.mikepenz.markdown.model.PlaceholderConfig
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.rememberMarkdownState
import me.weishu.kernelsu.ui.component.breeze.markdown.style.BreezeMarkdownStyle
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

/** Full markdown source text, used by image slots to resolve alt text ranges. */
internal val LocalMarkdownSource = compositionLocalOf<String?> { null }

/** Horizontal padding a top-level table may bleed across when wider than the text column. */
internal data class MarkdownTableBleed(val start: Dp, val end: Dp)

internal val LocalMarkdownTableBleed = staticCompositionLocalOf { MarkdownTableBleed(0.dp, 0.dp) }

/** Parses [content] into a [MarkdownState] with remembered parser inputs to avoid re-parsing on recomposition. */
@Composable
fun rememberLazyMarkdownState(content: String): MarkdownState {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) {
        MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)
    }
    val referenceLinkHandler = remember(content) { ReferenceLinkHandlerImpl() }
    return rememberMarkdownState(
        content = content,
        flavour = flavour,
        parser = parser,
        referenceLinkHandler = referenceLinkHandler,
    )
}

/**
 * Renders markdown in a self-scrolling [LazyColumn], composing only blocks near the viewport.
 *
 * - Owns the vertical scroller (do not wrap in another vertical scroll modifier) and horizontal
 *   [contentPadding] (top-level tables bleed across it to the component edge).
 * - Filters out top-level `EOL` and `LINK_DEFINITION` nodes so blank lines and reference-link
 *   tables do not add extra block gaps.
 * - Never loads remote images (see [MarkdownBrokenImage]).
 */
@Composable
fun LazyMarkdownContent(
    content: String,
    style: BreezeMarkdownStyle,
    state: MarkdownState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLinkClick: ((String) -> Unit)? = null,
) {
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    val bleed = MarkdownTableBleed(start, end)
    val linkHandler = onLinkClick?.let { click ->
        object : UriHandler {
            override fun openUri(uri: String) = click(uri)
        }
    }
    val body = @Composable {
        BoxWithConstraints(modifier = Modifier) {
            // Measured during layout so inline image placeholders get the final width on frame 1
            // (the library's containerSize is populated one frame late in onPlaced).
            val documentWidth = (maxWidth - start - end).value.sp
            CompositionLocalProvider(
                LocalMarkdownSource provides content,
            ) {
                Markdown(
                    markdownState = state,
                    colors = style.colors,
                    typography = style.typography,
                    padding = style.padding,
                    dimens = style.dimens,
                    extendedSpans = style.extendedSpans,
                    components = style.components,
                    imageTransformer = remember(documentWidth) {
                        NoExternalImageTransformer(
                            documentWidth
                        )
                    },
                    modifier = modifier,
                    success = { state, components, successModifier ->
                        // Custom LazyColumn instead of LazyMarkdownSuccess: filters out non-visual
                        // nodes and lets top-level tables bleed across contentPadding.
                        val nodes = remember(state.node) {
                            state.node.children.filter { it.isVisibleBlock() }
                        }
                        LazyColumn(
                            modifier = successModifier,
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            items(
                                items = nodes,
                                key = { it.startOffset },
                                contentType = { it.type },
                            ) { node ->
                                when {
                                    node.type == GFMElementTypes.TABLE -> CompositionLocalProvider(
                                        LocalMarkdownTableBleed provides bleed,
                                    ) {
                                        MarkdownElement(node, components, state.content)
                                    }

                                    else -> Box(Modifier.padding(start = start, end = end)) {
                                        MarkdownElement(node, components, state.content)
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
    if (linkHandler == null) {
        body()
    } else {
        CompositionLocalProvider(LocalUriHandler provides linkHandler) { body() }
    }
}

/** Filters out non-visual `EOL` and `LINK_DEFINITION` nodes so they do not add extra block gaps. */
internal fun ASTNode.isVisibleBlock(): Boolean =
    type != MarkdownTokenTypes.EOL && type != MarkdownElementTypes.LINK_DEFINITION

/** Fallback placeholder width when no document width has been measured yet. */
private const val MarkdownPlaceholderFallbackWidthDp = 320f

/**
 * [ImageTransformer] that resolves no remote images (`transform` returns null) and sizes the
 * inline placeholder box to the document width and [MarkdownBrokenImageHeightDp].
 */
private class NoExternalImageTransformer(
    private val widthSp: TextUnit,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? = null

    @Composable
    override fun intrinsicSize(painter: Painter): Size = Size.Unspecified

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((link: String, Size) -> Unit)?,
    ): PlaceholderConfig {
        val widthDp = if (widthSp.isSp) widthSp.value else MarkdownPlaceholderFallbackWidthDp
        return PlaceholderConfig(Size(widthDp, MarkdownBrokenImageHeightDp))
    }
}
