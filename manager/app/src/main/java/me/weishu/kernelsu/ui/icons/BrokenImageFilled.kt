package me.weishu.kernelsu.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val brokenImageFilled: MaterialSymbol = MaterialSymbol(
    name = "broken_image",
    style = SymbolStyle.FilledRounded,
) {
    ImageVector.Builder(
        name = "BrokenImage",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(12.4f)
            lineToRelative(2.3f, 2.3f)
            quadTo(5.6f, 15f, 6f, 15f)
            reflectiveQuadTo(6.7f, 14.7f)
            lineTo(10f, 11.4f)
            lineToRelative(3.3f, 3.3f)
            quadTo(13.6f, 15f, 14f, 15f)
            reflectiveQuadToRelative(0.7f, -0.3f)
            lineTo(18f, 11.4f)
            lineToRelative(3f, 3f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineToRelative(6.57f)
            lineTo(18.7f, 9.27f)
            quadTo(18.4f, 8.98f, 18f, 8.98f)
            reflectiveQuadToRelative(-0.7f, 0.3f)
            lineTo(14f, 12.58f)
            lineTo(10.7f, 9.27f)
            quadTo(10.4f, 8.98f, 10f, 8.98f)
            reflectiveQuadTo(9.3f, 9.27f)
            lineTo(6f, 12.58f)
            lineToRelative(-3f, -3f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            close()
        }
    }.build()
}
