package dev.devscroll.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import dev.devscroll.data.model.Article
import kotlin.math.abs

private val thumbnailSymbols = listOf("//", "#!", "~/", "\$_", "&&", "||", ">>", "{}", "[]", "=>", "::", "!!")
private val thumbnailBgColors = listOf(
    Color(0xFF1A2A10), Color(0xFF0A2035), Color(0xFF2A1E08),
    Color(0xFF2D0A12), Color(0xFF1E1535), Color(0xFF0D1A2E), Color(0xFF0D2020),
)
private val thumbnailStripeColor = Color(0xFF2A3A2A).copy(alpha = 0.6f)

/** Builds the diagonal-stripe pattern once for a given size so it isn't recomputed each frame. */
private fun buildStripePath(size: Size, spacing: Float): Path = Path().apply {
    var offset = -size.height
    while (offset < size.width) {
        moveTo(offset, 0f)
        lineTo(offset + size.height, size.height)
        offset += spacing
    }
}

@Composable
fun ArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onSaveClick: (Article) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (article.isHot) {
        HotArticleCard(
            article = article,
            onArticleClick = onArticleClick,
            onSaveClick = onSaveClick,
            isSaving = isSaving,
            modifier = modifier,
        )
    } else {
        StandardArticleCard(
            article = article,
            onArticleClick = onArticleClick,
            onSaveClick = onSaveClick,
            isSaving = isSaving,
            modifier = modifier,
        )
    }
}

@Composable
private fun StandardArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onSaveClick: (Article) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article) }
            .alpha(if (article.isRead) 0.6f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Metadata row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SourceBadge(blogName = article.blogName, size = 18.dp)
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
                        maxLines = 3,
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
                    loading = {
                        TerminalThumbnail(seed = article.id)
                    },
                    error = {
                        TerminalThumbnail(seed = article.id)
                    }
                )
            } else {
                TerminalThumbnail(seed = article.id)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Categories + bookmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (article.categories.isNotEmpty()) {
                Text(
                    text = article.categories.joinToString(", ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(
                onClick = { if (!isSaving) onSaveClick(article) },
                enabled = !isSaving,
                modifier = Modifier.size(36.dp)
            ) {
                if (isSaving) {
                    TerminalSpinner(modifier = Modifier.size(18.dp))
                } else {
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
}

@Composable
private fun TerminalThumbnail(seed: Any) {
    val hash = abs(seed.hashCode())
    val symbol = thumbnailSymbols[hash % thumbnailSymbols.size]
    val bgColor = thumbnailBgColors[hash % thumbnailBgColors.size]
    Box(
        modifier = Modifier
            .size(72.dp)
            .drawWithCache {
                val stripes = buildStripePath(size, spacing = 10.dp.toPx())
                val strokeWidth = 1.dp.toPx()
                onDrawBehind {
                    drawRect(color = bgColor)
                    drawPath(
                        path = stripes,
                        color = thumbnailStripeColor,
                        style = Stroke(width = strokeWidth),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
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

@Composable
private fun HotArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onSaveClick: (Article) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article) }
            .alpha(if (article.isRead) 0.6f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        HotThumbnail(article = article)
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SourceBadge(blogName = article.blogName, size = 18.dp)
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
        }

        Spacer(Modifier.height(8.dp))
        
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (article.categories.isNotEmpty()) {
                Text(
                    text = article.categories.joinToString(", ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(
                onClick = { if (!isSaving) onSaveClick(article) },
                enabled = !isSaving,
                modifier = Modifier.size(36.dp)
            ) {
                if (isSaving) {
                    TerminalSpinner(modifier = Modifier.size(18.dp))
                } else {
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
}

@Composable
private fun HotThumbnail(article: Article) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        if (article.thumbnailUrl != null) {
            SubcomposeAsyncImage(
                model = article.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    HotTerminalThumbnail(seed = article.id)
                },
                error = {
                    HotTerminalThumbnail(seed = article.id)
                }
            )
        } else {
            HotTerminalThumbnail(seed = article.id)
        }
        
        HotThumbnailOverlay(article = article)
    }
}

@Composable
private fun HotTerminalThumbnail(seed: Any) {
    val hash = abs(seed.hashCode())
    val symbol = thumbnailSymbols[hash % thumbnailSymbols.size]
    val bgColor = thumbnailBgColors[hash % thumbnailBgColors.size]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val stripes = buildStripePath(size, spacing = 12.dp.toPx())
                val strokeWidth = 1.5.dp.toPx()
                onDrawBehind {
                    drawRect(color = bgColor)
                    drawPath(
                        path = stripes,
                        color = thumbnailStripeColor,
                        style = Stroke(width = strokeWidth),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
            color = Color(0xFF4A8A5A).copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HotThumbnailOverlay(article: Article) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Text(
            text = "// preview",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        val shape = RoundedCornerShape(2.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .border(1.dp, MaterialTheme.colorScheme.primary, shape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "🔥 HOT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        val hexString = "0x" + article.id.toString(16).lowercase()
        Text(
            text = hexString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun TerminalSpinner(modifier: Modifier = Modifier) {
    val chars = listOf("\\", "|", "/", "-")
    var charIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(150L)
            charIndex = (charIndex + 1) % chars.size
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = chars[charIndex],
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
