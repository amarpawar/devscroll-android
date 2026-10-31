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
public val format_align_left: ImageVector by lazy {
    ImageVector.Builder(
        name = "format_align_left",
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
                moveTo(3f, 21f)
                verticalLineTo(19f)
                horizontalLineTo(21f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 17f)
                verticalLineTo(15f)
                horizontalLineTo(15f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(21f)
                verticalLineToRelative(2f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 9f)
                verticalLineTo(7f)
                horizontalLineTo(15f)
                verticalLineTo(9f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 5f)
                verticalLineTo(3f)
                horizontalLineTo(21f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                close()
            }
        }
        .build()
}
