package me.weishu.kernelsu.ui.component.breeze.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.utils.resolveImageAlt
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import org.intellij.markdown.ast.ASTNode

/** Height the placeholder reserves; the transformer supplies the width and the card fills the box. */
internal const val MarkdownBrokenImageHeightDp = 96f

/** Placeholder card rendered for markdown images (remote images are not fetched). */
@Composable
internal fun MarkdownBrokenImage(
    alt: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMarkdownColors.current
    val typography = LocalMarkdownTypography.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.codeBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Filled.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        if (!alt.isNullOrBlank()) {
            Text(
                text = alt,
                style = typography.paragraph.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Resolves an image node's alt text against [source], returning `null` if [source] is absent. */
internal fun ASTNode.resolveAlt(source: String?): String? = source?.let { resolveImageAlt(it) }

