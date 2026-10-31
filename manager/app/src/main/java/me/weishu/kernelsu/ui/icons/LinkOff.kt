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
internal val linkOff: MaterialSymbol = MaterialSymbol(
    name = "link_off",
    style = SymbolStyle(SymbolFamily.Rounded, SymbolFill.Unfilled, 24, 400, 0, 50),
) {
    ImageVector.Builder(
        name = "LinkOff",
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
            moveTo(15.63f, 12.77f)
            lineTo(13.85f, 11f)
            horizontalLineTo(15f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(16f, 12f)
            quadToRelative(0f, 0.25f, -0.1f, 0.45f)
            reflectiveQuadToRelative(-0.28f, 0.32f)
            close()
            moveTo(20.5f, 21.9f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(19.1f, 21.9f)
            lineTo(2.1f, 4.9f)
            quadTo(1.83f, 4.63f, 1.83f, 4.2f)
            reflectiveQuadTo(2.1f, 3.5f)
            quadTo(2.38f, 3.22f, 2.8f, 3.22f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineToRelative(17f, 17f)
            quadToRelative(0.28f, 0.27f, 0.28f, 0.7f)
            reflectiveQuadTo(20.5f, 21.9f)
            close()
            moveTo(7f, 17f)
            quadTo(4.93f, 17f, 3.46f, 15.54f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 10.27f, 3.05f, 8.92f)
            reflectiveQuadTo(5.75f, 7.15f)
            lineTo(7.6f, 9f)
            horizontalLineTo(7f)
            quadTo(5.75f, 9f, 4.88f, 9.88f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(7f, 15f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 16f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 17f)
            horizontalLineTo(7f)
            close()
            moveTo(9f, 13f)
            quadTo(8.58f, 13f, 8.29f, 12.71f)
            quadTo(8f, 12.43f, 8f, 12f)
            reflectiveQuadTo(8.29f, 11.29f)
            quadTo(8.58f, 11f, 9f, 11f)
            horizontalLineTo(9.63f)
            lineToRelative(1.98f, 2f)
            horizontalLineTo(9f)
            close()
            moveToRelative(9.5f, 2.8f)
            quadTo(18.28f, 15.45f, 18.34f, 15.05f)
            reflectiveQuadToRelative(0.41f, -0.63f)
            quadTo(19.33f, 14f, 19.66f, 13.38f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 10.75f, 19.13f, 9.88f)
            reflectiveQuadTo(17f, 9f)
            horizontalLineTo(14f)
            quadTo(13.58f, 9f, 13.29f, 8.71f)
            reflectiveQuadTo(13f, 8f)
            quadTo(13f, 7.57f, 13.29f, 7.29f)
            reflectiveQuadTo(14f, 7f)
            horizontalLineToRelative(3f)
            quadToRelative(2.07f, 0f, 3.54f, 1.46f)
            reflectiveQuadTo(22f, 12f)
            quadToRelative(0f, 1.22f, -0.56f, 2.29f)
            reflectiveQuadToRelative(-1.56f, 1.76f)
            quadToRelative(-0.35f, 0.22f, -0.75f, 0.16f)
            quadTo(18.73f, 16.15f, 18.5f, 15.8f)
            close()
        }
    }.build()
}
