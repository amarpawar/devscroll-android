package dev.devscroll.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val badgeColors = listOf(
    Color(0xFF9ECE6A),
    Color(0xFF7DCFFF),
    Color(0xFFE0AF68),
    Color(0xFFF7768E),
    Color(0xFFBB9AF7),
    Color(0xFF7AA2F7),
    Color(0xFF73DACA),
)

fun blogBadgeColor(blogName: String): Color =
    badgeColors[abs(blogName.hashCode()) % badgeColors.size]

@Composable
fun SourceBadge(
    blogName: String,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    val color = blogBadgeColor(blogName)
    Box(
        modifier = modifier
            .size(size)
            .border(1.dp, color, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = blogName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
