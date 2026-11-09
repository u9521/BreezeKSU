package me.weishu.kernelsu.ui.component.breeze.markdown.style

import androidx.compose.runtime.Immutable
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownDimens
import com.mikepenz.markdown.model.MarkdownExtendedSpans
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.MarkdownTypography

/** Bundles colors, typography, padding, dimens, spans, and component overrides for `Markdown`. */
@Immutable
data class BreezeMarkdownStyle(
    val colors: MarkdownColors,
    val typography: MarkdownTypography,
    val padding: MarkdownPadding,
    val dimens: MarkdownDimens,
    val extendedSpans: MarkdownExtendedSpans,
    val components: MarkdownComponents,
) {
    companion object
}
