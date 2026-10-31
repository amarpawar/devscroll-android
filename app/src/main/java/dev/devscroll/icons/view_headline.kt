package dev.devscroll.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val view_headline: ImageVector by lazy {
    ImageVector.Builder(
        name = "view_headline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(4f, 15f)
                verticalLineTo(13f)
                horizontalLineTo(20f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveToRelative(0f, 4f)
                verticalLineTo(17f)
                horizontalLineTo(20f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 11f)
                verticalLineTo(9f)
                horizontalLineTo(20f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(20f)
                verticalLineTo(7f)
                horizontalLineTo(4f)
                close()
            }
        }
        .build()
}