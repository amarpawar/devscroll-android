package dev.devscroll.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import dev.devscroll.data.model.Article
import kotlin.math.abs

@Composable
fun ArticleCard(
    article: Article,
    onArticleClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onArticleClick() }
            .alpha(if (article.isRead) 0.6f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Metadata row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SourceBadge(blogName = article.blogName)
            Text(
                text = "#${article.authors.firstOrNull() ?: article.blogName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(
                text = timeAgo(article.publishedOn),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(
                text = "${article.readMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (article.isHot) {
                Text(
                    text = "[HOT]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title + thumbnail
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Top),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = article.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (article.thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = article.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp),
                    loading = { TerminalThumbnail(seed = article.id) },
                    error = { TerminalThumbnail(seed = article.id) },
                )
            } else {
                TerminalThumbnail(seed = article.id)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Categories + bookmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (article.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    article.categories.take(4).forEach { category ->
                        TagChip(label = category)
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = onSaveClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (article.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (article.isSaved) "Unsave" else "Save",
                    tint = if (article.isSaved)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TerminalThumbnail(seed: Any) {
    val symbols = listOf("//", "#!", "~/", "\$_", "&&", "||", ">>", "{}", "[]", "=>", "::", "!!")
    val bgColors = listOf(
        Color(0xFF1A2A10), Color(0xFF0A2035), Color(0xFF2A1E08),
        Color(0xFF2D0A12), Color(0xFF1E1535), Color(0xFF0D1A2E), Color(0xFF0D2020),
    )
    val hash = abs(seed.hashCode())
    val symbol = symbols[hash % symbols.size]
    val bgColor = bgColors[hash % bgColors.size]
    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(72.dp)) {
            drawRect(color = bgColor)
            val stripeColor = Color(0xFF2A3A2A).copy(alpha = 0.6f)
            val spacing = 10.dp.toPx()
            clipRect {
                var offset = -size.height
                while (offset < size.width) {
                    drawLine(
                        color = stripeColor,
                        start = androidx.compose.ui.geometry.Offset(offset, 0f),
                        end = androidx.compose.ui.geometry.Offset(offset + size.height, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                    offset += spacing
                }
            }
        }
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF4A8A5A),
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun timeAgo(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000}d ago"
        else -> "${diff / 604_800_000}w ago"
    }
}
