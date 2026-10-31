package me.weishu.kernelsu.ui.component.breeze.markdown.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAlertColors
import com.mikepenz.markdown.model.markdownAlertDimens
import com.mikepenz.markdown.model.markdownAlertPadding
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownPadding

/**
 * Default compact Markdown style: primary-colored H1–H3 section headings, hanging-indent lists,
 * 14/22sp body typography, and translucent `secondaryContainer` wells visible across light, dark, and AMOLED themes.
 */
@Composable
fun breezeMarkdownDefault(): BreezeMarkdownStyle {
    val scheme = MaterialTheme.colorScheme
    val base = MaterialTheme.typography
    val body = base.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = scheme.onSurface,
    )
    val release = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = scheme.primary)
    val well = scheme.secondaryContainer.copy(alpha = 0.55f)
    val accent = BreezeMarkdownAccent(
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        quoteContent = scheme.onSurfaceVariant,
        headerTintAlpha = 0.40f,
        quoteContainerAlpha = 0.22f,
        tableStripeAlpha = 0.18f,
    )
    return BreezeMarkdownStyle(
        colors = markdownColor(
            text = scheme.onSurface,
            codeBackground = well,
            inlineCodeBackground = scheme.secondaryContainer.copy(alpha = 0.75f),
            dividerColor = scheme.outlineVariant,
            tableBackground = well,
            alert = markdownAlertColors(
                note = scheme.primary,
                tip = scheme.tertiary,
                important = scheme.secondary,
                warning = scheme.tertiary,
                caution = scheme.error,
            ),
            highlightText = scheme.onTertiaryContainer,
            highlightBackground = scheme.tertiaryContainer,
        ),
        typography = markdownTypography(
            h1 = release.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
            h2 = release.copy(fontSize = 17.sp, lineHeight = 24.sp),
            h3 = release.copy(fontSize = 16.sp, lineHeight = 24.sp),
            h4 = base.titleSmall.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            ),
            h5 = body.copy(fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant),
            h6 = body.copy(fontWeight = FontWeight.Medium, color = scheme.onSurfaceVariant),
            text = body,
            paragraph = body,
            list = body,
            ordered = body.copy(fontWeight = FontWeight.SemiBold, color = scheme.primary),
            bullet = body.copy(fontWeight = FontWeight.Bold, color = scheme.primary),
            // `quote.color` is the quote bar; the quote's text takes `accent.quoteContent`.
            quote = body.copy(color = scheme.primary),
            code = body.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp,
            ),
            inlineCode = body.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                color = scheme.onSecondaryContainer,
            ),
            table = body.copy(fontSize = 13.sp, lineHeight = 19.sp),
            textLink = TextLinkStyles(
                style = body.copy(
                    color = scheme.primary,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ).toSpanStyle(),
                pressedStyle = body.copy(
                    color = scheme.onPrimaryContainer,
                    background = scheme.primaryContainer,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ).toSpanStyle(),
            ),
            alertTitle = body.copy(fontWeight = FontWeight.Bold, color = Color.Unspecified),
        ),
        padding = markdownPadding(
            block = 8.dp,
            list = 0.dp,
            listItemTop = 2.dp,
            listItemBottom = 2.dp,
            // Nested lists already start at their parent's text column; see the list slots.
            listIndent = 0.dp,
            codeBlock = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            blockQuote = PaddingValues(start = 14.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
            blockQuoteText = PaddingValues(vertical = 0.dp),
            blockQuoteBar = PaddingValues.Absolute(
                left = 1.5.dp,
                top = 0.dp,
                right = 0.dp,
                bottom = 0.dp
            ),
            alert = markdownAlertPadding(
                container = PaddingValues(start = 14.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                content = PaddingValues(vertical = 0.dp),
                bar = PaddingValues.Absolute(
                    left = 1.5.dp,
                    top = 2.dp,
                    right = 0.dp,
                    bottom = 2.dp
                ),
                iconSpacing = 6.dp,
                titleSpacing = 2.dp,
            ),
        ),
        dimens = markdownDimens(
            dividerThickness = 1.dp,
            codeBackgroundCornerSize = 12.dp,
            blockQuoteThickness = 3.dp,
            tableMaxWidth = Dp.Unspecified,
            tableCellWidth = 96.dp,
            tableCellPadding = 8.dp,
            tableCornerSize = 12.dp,
            alert = markdownAlertDimens(barThickness = 3.dp, iconSize = 16.dp),
        ),
        extendedSpans = markdownExtendedSpans {
            val outline = MaterialTheme.colorScheme.outlineVariant
            remember(outline) {
                ExtendedSpans(
                    RoundedCornerSpanPainter(
                        cornerRadius = 4.sp,
                        stroke = RoundedCornerSpanPainter.Stroke(
                            color = { outline },
                            width = 0.5.sp
                        ),
                        padding = RoundedCornerSpanPainter.TextPaddingValues(
                            // Any horizontal padding paints over the neighbouring glyph.
                            horizontal = 0.sp,
                            vertical = 1.sp,
                        ),
                        topMargin = 2.sp,
                        bottomMargin = 2.sp,
                    ),
                )
            }
        },
        components = breezeMarkdownComponents(accent),
    )
}

/** See [breezeMarkdownDefault]. */
val BreezeMarkdownStyle.Companion.Default: BreezeMarkdownStyle
    @Composable get() = breezeMarkdownDefault()
