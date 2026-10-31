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
public val event_list: ImageVector by lazy {
    ImageVector.Builder(
        name = "event_list",
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
                moveTo(16f, 21f)
                quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                reflectiveQuadTo(14f, 19f)
                verticalLineTo(15f)
                quadToRelative(0f, -0.83f, 0.59f, -1.41f)
                reflectiveQuadTo(16f, 13f)
                horizontalLineToRelative(4f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                quadTo(22f, 14.18f, 22f, 15f)
                verticalLineToRelative(4f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(20f, 21f)
                horizontalLineTo(16f)
                close()
                moveToRelative(0f, -2f)
                horizontalLineToRelative(4f)
                verticalLineTo(15f)
                horizontalLineTo(16f)
                verticalLineToRelative(4f)
                close()
                moveTo(2f, 18f)
                verticalLineTo(16f)
                horizontalLineToRelative(9f)
                verticalLineToRelative(2f)
                horizontalLineTo(2f)
                close()
                moveTo(16f, 11f)
                quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
                reflectiveQuadTo(14f, 9f)
                verticalLineTo(5f)
                quadTo(14f, 4.17f, 14.59f, 3.59f)
                reflectiveQuadTo(16f, 3f)
                horizontalLineToRelative(4f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                reflectiveQuadTo(22f, 5f)
                verticalLineTo(9f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(20f, 11f)
                horizontalLineTo(16f)
                close()
                moveTo(16f, 9f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineTo(16f)
                verticalLineTo(9f)
                close()
                moveTo(2f, 8f)
                verticalLineTo(6f)
                horizontalLineToRelative(9f)
                verticalLineTo(8f)
                horizontalLineTo(2f)
                close()
                moveToRelative(16f, 9f)
                close()
                moveTo(18f, 7f)
                close()
            }
        }
        .build()
}
