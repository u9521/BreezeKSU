package me.weishu.kernelsu.ui.component.breeze.markdown.style

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.LocalMarkdownA11yLabels
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.LocalMarkdownPadding
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownBlockQuote
import com.mikepenz.markdown.compose.elements.MarkdownCheckBox
import com.mikepenz.markdown.compose.elements.MarkdownCodeBackground
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownDivider
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.compose.elements.MarkdownListItems
import com.mikepenz.markdown.compose.elements.MarkdownOrderedList
import com.mikepenz.markdown.compose.elements.listDepth
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.component.breeze.markdown.LocalMarkdownSource
import me.weishu.kernelsu.ui.component.breeze.markdown.LocalMarkdownTableBleed
import me.weishu.kernelsu.ui.component.breeze.markdown.MarkdownBrokenImage
import me.weishu.kernelsu.ui.component.breeze.markdown.MarkdownTableBleed
import me.weishu.kernelsu.ui.component.breeze.markdown.isVisibleBlock
import me.weishu.kernelsu.ui.component.breeze.markdown.resolveAlt
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes


private val CopiedFeedback = 1200.milliseconds
private val ListMarkerWidth = 22.dp
private val CheckboxSize = 16.dp

/** End padding already applied by `MarkdownCheckBox` on the indicator modifier. */
private val LibraryCheckboxGap = 4.dp
private val CheckboxCorner = 2.dp
private val CheckboxStroke = 2.dp
private val SectionRuleAbove = 10.dp
private val SectionRuleBelow = 12.dp

private val HeadingTypes: Set<IElementType> = setOf(
    MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
    MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2,
)

/** Extra accent colors for checkboxes, code/table headers, and block quotes not covered by [MarkdownColors]. */
@Immutable
data class BreezeMarkdownAccent(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Text colour inside a block quote; `typography.quote.color` is the bar. */
    val quoteContent: Color,
    /** Alpha applied to [accentContainer] behind code-block and table headers. */
    val headerTintAlpha: Float = 0.45f,
    /** Alpha applied to [accentContainer] behind block quotes. */
    val quoteContainerAlpha: Float = 0.32f,
    /** Alpha applied to [accentContainer] on alternate table rows. */
    val tableStripeAlpha: Float = 0.35f,
)

/**
 * Component slot overrides for [BreezeMarkdownStyle].
 *
 * All custom slots are passed in a single [markdownComponents] call because omitted slots reset to
 * library defaults.
 */
fun breezeMarkdownComponents(accent: BreezeMarkdownAccent): MarkdownComponents = markdownComponents(
    heading1 = { model -> ReleaseHeading(model, model.typography.h1) },
    heading2 = { model -> ReleaseHeading(model, model.typography.h2) },
    heading3 = { model -> ReleaseHeading(model, model.typography.h3) },
    setextHeading1 = { model ->
        ReleaseHeading(model, model.typography.h1, MarkdownTokenTypes.SETEXT_CONTENT)
    },
    setextHeading2 = { model ->
        ReleaseHeading(model, model.typography.h2, MarkdownTokenTypes.SETEXT_CONTENT)
    },
    unorderedList = { model ->
        BulletList(model)
    },
    orderedList = { model ->
        MarkdownOrderedList(
            content = model.content,
            node = model.node,
            style = model.typography.ordered,
            depth = model.listDepth,
            markerModifier = {
                Modifier
                    .widthIn(min = ListMarkerWidth)
                    .padding(end = 4.dp)
            },
        )
    },
    // GFM wraps standalone images in PARAGRAPH (inline slot); handle both slots identically.
    image = { model ->
        MarkdownBrokenImage(alt = model.node.resolveAlt(LocalMarkdownSource.current))
    },
    inlineImage = { model ->
        MarkdownBrokenImage(alt = model.node.resolveAlt(LocalMarkdownSource.current))
    },
    checkbox = { model ->
        MarkdownCheckBox(
            content = model.content,
            node = model.node,
            style = model.typography.text,
            checkedIndicator = { checked, modifier ->
                CheckboxGlyph(
                    checked = checked,
                    accent = accent,
                    style = model.typography.text,
                    // The indicator owns its size and placement inside the marker column.
                    modifier = modifier,
                )
            },
        )
    },
    blockQuote = { model ->
        val dimens = LocalMarkdownDimens.current
        val shape = RoundedCornerShape(
            topStart = 4.dp,
            bottomStart = 4.dp,
            topEnd = dimens.codeBackgroundCornerSize,
            bottomEnd = dimens.codeBackgroundCornerSize,
        )
        // MarkdownBlockQuote uses `style.color` for the bar and `style.lineHeight` for blank `>` lines.
        val quoteBar = model.typography.quote.copy(lineHeight = 6.sp)
        val quoteParagraph = model.typography.paragraph.copy(color = accent.quoteContent)
        val quoteTypography = remember(model.typography, quoteParagraph) {
            DefaultMarkdownTypography(
                h1 = model.typography.h1,
                h2 = model.typography.h2,
                h3 = model.typography.h3,
                h4 = model.typography.h4,
                h5 = model.typography.h5,
                h6 = model.typography.h6,
                text = quoteParagraph,
                code = model.typography.code,
                inlineCode = model.typography.inlineCode,
                quote = quoteParagraph,
                paragraph = quoteParagraph,
                ordered = model.typography.ordered,
                bullet = model.typography.bullet,
                list = quoteParagraph,
                textLink = model.typography.textLink,
                table = model.typography.table,
                alertTitle = model.typography.alertTitle,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(shape)
                .background(
                    color = accent.accentContainer.copy(alpha = accent.quoteContainerAlpha),
                    shape = shape,
                ),
        ) {
            CompositionLocalProvider(LocalMarkdownTypography provides quoteTypography) {
                MarkdownBlockQuote(
                    content = model.content,
                    node = model.node,
                    style = quoteBar,
                )
            }
        }
    },
    codeFence = { model ->
        MarkdownCodeFence(
            content = model.content,
            node = model.node,
            style = model.typography.code,
        ) { code, language, codeStyle ->
            BreezeCodeBlock(code, language, codeStyle, accent)
        }
    },
    codeBlock = { model ->
        MarkdownCodeBlock(
            content = model.content,
            node = model.node,
            style = model.typography.code,
        ) { code, language, codeStyle ->
            BreezeCodeBlock(code, language, codeStyle, accent)
        }
    },
    table = { model -> BreezeTable(model, accent) },
)

/** Unordered list with canvas-drawn circular bullets in a fixed [ListMarkerWidth] column. */
@Composable
private fun BulletList(model: MarkdownComponentModel) {
    val color = model.typography.bullet.color.takeOrElse { LocalContentColor.current }
    val depth = model.listDepth
    // Sized per depth so nesting stays legible without changing the column.
    val diameter = when (depth) {
        0 -> 5.dp
        1 -> 4.dp
        else -> 3.dp
    }
    MarkdownListItems(
        content = model.content,
        node = model.node,
        depth = depth,
        markerModifier = { Modifier.widthIn(min = ListMarkerWidth) },
        // (index, listNumber, child): the same slot the library fills with a BulletHandler glyph.
        bullet = { _, _, _ ->
            BulletMarker(color = color, diameter = diameter, style = model.typography.bullet)
        },
    )
}

/** Circular bullet centered vertically within the first line's height. */
@Composable
private fun BulletMarker(color: Color, diameter: Dp, style: TextStyle) {
    val top = with(LocalDensity.current) {
        ((style.lineHeight.toDp() - diameter) / 2).coerceAtLeast(0.dp)
    }
    Canvas(
        modifier = Modifier
            .padding(top = top)
            .size(diameter),
    ) {
        drawCircle(color = color)
    }
}

/** Custom 16dp task checkbox that fits inside [ListMarkerWidth] (M3 `Checkbox` hardcodes 20dp). */
@Composable
private fun CheckboxGlyph(
    checked: Boolean,
    accent: BreezeMarkdownAccent,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val top = with(LocalDensity.current) {
        ((style.lineHeight.toDp() - CheckboxSize) / 2).coerceAtLeast(0.dp)
    }
    Canvas(
        modifier
            .padding(
                top = top,
                end = ListMarkerWidth - CheckboxSize - LibraryCheckboxGap,
            )
            .size(CheckboxSize),
    ) {
        val stroke = CheckboxStroke.toPx()
        // Inset by half the stroke so the border stays inside the canvas bounds.
        val half = stroke / 2f
        val boxWidth = size.width - stroke
        val boxHeight = size.height - stroke
        val radius = CornerRadius(CheckboxCorner.toPx())
        if (checked) {
            drawRoundRect(
                color = accent.accent,
                topLeft = Offset(half, half),
                size = Size(boxWidth, boxHeight),
                cornerRadius = radius,
            )
            // The same three points Material 3 traces, inside the same inset box.
            val check = Path().apply {
                moveTo(half + boxWidth * 0.2f, half + boxHeight * 0.5f)
                lineTo(half + boxWidth * 0.4f, half + boxHeight * 0.7f)
                lineTo(half + boxWidth * 0.8f, half + boxHeight * 0.3f)
            }
            drawPath(
                path = check,
                color = accent.onAccent,
                style = Stroke(width = stroke, cap = StrokeCap.Square),
            )
        } else {
            drawRoundRect(
                color = outline,
                topLeft = Offset(half, half),
                size = Size(boxWidth, boxHeight),
                cornerRadius = radius,
                style = Stroke(width = stroke),
            )
        }
    }
}

/**
 * Markdown table that stays rounded inside the text column when narrow, or scrolls horizontally
 * across [LocalMarkdownTableBleed] with a square overflowing edge when wide.
 */
@Composable
private fun BreezeTable(model: MarkdownComponentModel, accent: BreezeMarkdownAccent) {
    val dimens = LocalMarkdownDimens.current
    val bleed = LocalMarkdownTableBleed.current
    val columns = remember(model.node) {
        model.node.children.firstOrNull { it.type == GFMElementTypes.HEADER }
            ?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0
    }
    val naturalWidth: Dp = dimens.tableCellWidth * columns
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val textColumn = (maxWidth - bleed.start - bleed.end).coerceIn(0.dp, maxWidth)
        if (naturalWidth <= textColumn) {
            TableFrame(
                model = model,
                accent = accent,
                columns = columns,
                shape = RoundedCornerShape(dimens.tableCornerSize),
                modifier = Modifier
                    .padding(start = bleed.start, end = bleed.end)
                    .fillMaxWidth(),
            )
        } else {
            val scroll = rememberScrollState()
            TableFrame(
                model = model,
                accent = accent,
                columns = columns,
                shape = overflowingShape(naturalWidth, maxWidth, bleed, dimens.tableCornerSize),
                modifier = Modifier
                    .horizontalScroll(scroll)
                    .padding(start = bleed.start, end = bleed.end)
                    .width(max(naturalWidth, maxWidth - bleed.start)),
            )
        }
    }
}

/** Squares the corner on the overflowing side of a wide table (layout-fixed, independent of scroll). */
private fun overflowingShape(
    naturalWidth: Dp,
    componentWidth: Dp,
    bleed: MarkdownTableBleed,
    radius: Dp,
): RoundedCornerShape {
    val overflow = naturalWidth + bleed.start + bleed.end - componentWidth
    val trailingOverflows = overflow + bleed.start >= bleed.end
    val start = if (trailingOverflows) radius else radius.coerceAtMost(overflow + bleed.end)
    val end = if (trailingOverflows) radius.coerceAtMost(overflow + bleed.start) else radius
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

@Composable
private fun TableFrame(
    model: MarkdownComponentModel,
    accent: BreezeMarkdownAccent,
    columns: Int,
    shape: RoundedCornerShape,
    modifier: Modifier,
) {
    val colors = LocalMarkdownColors.current
    val rows =
        remember(model.node) { model.node.children.count { it.type == GFMElementTypes.ROW } + 1 }
    val aligns = remember(model.node, model.content) { model.node.tableAligns(model.content) }
    Column(
        modifier = modifier
            .semantics { collectionInfo = CollectionInfo(rowCount = rows, columnCount = columns) }
            .clip(shape)
            .background(colors.tableBackground, shape)
            .border(width = 1.dp, color = colors.dividerColor.copy(alpha = 0.65f), shape = shape),
    ) {
        val style = model.typography.table
        var rowIndex = 1
        model.node.children.forEach { child ->
            when (child.type) {
                GFMElementTypes.HEADER -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.accentContainer.copy(alpha = accent.headerTintAlpha)),
                ) {
                    TableRow(
                        content = model.content,
                        row = child,
                        style = style.copy(
                            color = accent.onAccentContainer,
                            fontWeight = FontWeight.Bold,
                        ),
                        aligns = aligns,
                        rowIndex = 0,
                    )
                }

                GFMElementTypes.ROW -> {
                    val stripe = if (rowIndex % 2 == 0) {
                        accent.accentContainer.copy(alpha = accent.tableStripeAlpha)
                    } else {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stripe)
                    ) {
                        TableRow(
                            content = model.content,
                            row = child,
                            style = style,
                            aligns = aligns,
                            rowIndex = rowIndex,
                        )
                    }
                    rowIndex++
                }

                GFMTokenTypes.TABLE_SEPARATOR -> MarkdownDivider()
            }
        }
    }
}

/** Table row that applies GFM per-column alignment (upstream `MarkdownTableRow` forces start alignment). */
@Composable
private fun TableRow(
    content: String,
    row: ASTNode,
    style: TextStyle,
    aligns: List<TextAlign>,
    rowIndex: Int,
) {
    val cellPadding = LocalMarkdownDimens.current.tableCellPadding
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        row.children.filter { it.type == GFMTokenTypes.CELL }.forEachIndexed { colIndex, cell ->
            val align = aligns.getOrElse(colIndex) { TextAlign.Start }
            Column(
                modifier = Modifier
                    .padding(cellPadding)
                    .weight(1f)
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = rowIndex, rowSpan = 1,
                            columnIndex = colIndex, columnSpan = 1,
                        )
                    },
            ) {
                TableCellText(
                    content = content,
                    cell = cell,
                    style = style.copy(textAlign = align),
                )
            }
        }
    }
}

/** Renders cell text at full cell width without a wrapping `SpanStyle` so paragraph [TextAlign] takes effect. */
@Composable
private fun TableCellText(content: String, cell: ASTNode, style: TextStyle) {
    val settings = annotatorSettings()
    val text = remember(content, cell, style, settings) {
        val built = buildAnnotatedString {
            buildMarkdownAnnotatedString(
                content = content,
                node = cell,
                annotatorSettings = settings
            )
        }
        // Trim trailing whitespace added by the annotator so centered/end-aligned cells align cleanly.
        var end = built.length
        while (end > 0 && built[end - 1].isWhitespace()) end--
        if (end == built.length) built else built.subSequence(0, end)
    }
    BasicText(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = style,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Clip,
    )
}

/** Parses per-column [TextAlign] from the GFM separator row (`:---`, `:---:`, `---:`). */
private fun ASTNode.tableAligns(content: String): List<TextAlign> {
    val separator =
        children.firstOrNull { it.type == GFMTokenTypes.TABLE_SEPARATOR } ?: return emptyList()
    // `TABLE_SEPARATOR` is a single leaf token containing the raw pipe-delimited string.
    return separator.getUnescapedTextInNode(content)
        .split('|')
        // A leading and trailing piece are the outer pipes, not cells.
        .drop(1)
        .dropLast(1)
        .map { piece ->
            val cell = piece.trim()
            when {
                cell.startsWith(":") && cell.endsWith(":") -> TextAlign.Center
                cell.endsWith(":") -> TextAlign.End
                else -> TextAlign.Start
            }
        }
}

/** H1–H3 heading preceded by a divider rule when it starts a new top-level section after body content. */
@Composable
private fun ReleaseHeading(
    model: MarkdownComponentModel,
    style: TextStyle,
    contentChildType: IElementType = MarkdownTokenTypes.ATX_CONTENT,
) {
    val separated = remember(model.node) { model.node.startsSection() }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (separated) {
            Spacer(Modifier.height(SectionRuleAbove))
            MarkdownDivider(color = LocalMarkdownColors.current.dividerColor)
            Spacer(Modifier.height(SectionRuleBelow))
        }
        MarkdownHeader(
            content = model.content,
            node = model.node,
            style = style,
            contentChildType = contentChildType,
        )
    }
}

/** Whether a top-level heading follows content, so a rule should separate it from that content. */
private fun ASTNode.startsSection(): Boolean {
    val parent = parent ?: return false
    if (parent.type != MarkdownElementTypes.MARKDOWN_FILE) return false
    val siblings = parent.children
    val previous = siblings.subList(0, siblings.indexOf(this)).lastOrNull { it.isVisibleBlock() }
        ?: return false
    return previous.type != MarkdownTokenTypes.HORIZONTAL_RULE && previous.type !in HeadingTypes
}

/** Code block with language header, inline copy-to-clipboard button, and selectable text. */
@Composable
private fun BreezeCodeBlock(
    code: String,
    language: String?,
    style: TextStyle,
    accent: BreezeMarkdownAccent,
) {
    val colors = LocalMarkdownColors.current
    val dimens = LocalMarkdownDimens.current
    val padding = LocalMarkdownPadding.current
    val a11yLabels = LocalMarkdownA11yLabels.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(dimens.codeBackgroundCornerSize)
    var copied by remember(code) { mutableStateOf(false) }

    MarkdownCodeBackground(
        color = colors.codeBackground,
        shape = shape,
        border = BorderStroke(1.dp, colors.dividerColor.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        showHeader = false,
        language = language,
        code = code,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.accentContainer.copy(alpha = accent.headerTintAlpha))
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language?.lowercase() ?: a11yLabels.codeFallbackLanguage.lowercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    ),
                    color = accent.onAccentContainer,
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("markdown", code))
                            )
                            copied = true
                            delay(CopiedFeedback)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (copied) {
                            MaterialSymbols.Filled.Rounded.Check
                        } else {
                            MaterialSymbols.Filled.Rounded.ContentCopy
                        },
                        contentDescription = if (copied) "Copied" else "Copy",
                        tint = accent.onAccentContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            MarkdownDivider(
                color = colors.dividerColor.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
            SelectionContainer {
                Text(
                    text = code,
                    style = style,
                    color = style.color.takeOrElse { colors.text },
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(padding.codeBlock),
                )
            }
        }
    }
}