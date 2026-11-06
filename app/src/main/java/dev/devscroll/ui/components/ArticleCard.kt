package dev.devscroll.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Flag
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.platform.LocalContext
import dev.devscroll.data.model.Article
import dev.devscroll.navigation.reportArticle
import dev.devscroll.navigation.shareArticle
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

/**
 * One article row. [style] only changes how the title, description and thumbnail are laid out;
 * the meta row (author, age, report) and footer (tags, share, save) are the same in every style.
 * When null (Saved, Search), hot articles get the big preview and the rest the small thumbnail.
 */
@Composable
fun ArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onSaveClick: (Article) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier,
    style: FeedStyle? = null,
) {
    val resolved = style ?: if (article.isHot) FeedStyle.BIG_THUMBNAILS else FeedStyle.SMALL_THUMBNAILS
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onArticleClick(article) }
            .alpha(if (article.isRead) 0.6f else 1f)
            .padding(vertical = 12.dp),
    ) {
        // Horizontal padding is applied per section so the big thumbnail can run edge to edge.
        // The big preview carries its own HOT badge, so the inline tag would be redundant there.
        MetaRow(
            article = article,
            showHotTag = resolved != FeedStyle.BIG_THUMBNAILS,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        if (resolved == FeedStyle.BIG_THUMBNAILS) {
            HotThumbnail(article = article)
            Spacer(Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            when (resolved) {
                FeedStyle.BIG_THUMBNAILS -> {
                    ArticleTitle(article, MaterialTheme.typography.titleMedium, maxLines = 3)
                    ArticleDescription(article, maxLines = 2)
                }
                FeedStyle.SMALL_THUMBNAILS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.Top),
                        ) {
                            ArticleTitle(article, MaterialTheme.typography.titleSmall, maxLines = 3)
                            ArticleDescription(article, maxLines = 3)
                        }
                        SmallThumbnail(article)
                    }
                }
                FeedStyle.TEXT_ONLY -> {
                    ArticleTitle(article, MaterialTheme.typography.titleSmall, maxLines = 3)
                    ArticleDescription(article, maxLines = 3)
                }
                FeedStyle.COMPACT_LIST -> {
                    ArticleTitle(article, MaterialTheme.typography.titleSmall, maxLines = 2)
                }
            }

            Spacer(Modifier.height(8.dp))

            FooterRow(article, onSaveClick, isSaving)
        }
    }
}

/** Source badge, author, age and optional [HOT] tag, with the report button on the right. */
@Composable
private fun MetaRow(article: Article, showHotTag: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nested row owns the weight so the author's own weight is measured against
        // this row alone. Two weighted siblings would split the free space between
        // them and truncate the author name at half its available width.
        Row(
            modifier = Modifier.weight(1f),
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
            if (showHotTag && article.isHot) {
                Text(
                    text = "[HOT]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        ReportButton(article)
    }
}

@Composable
private fun ArticleTitle(article: Article, style: TextStyle, maxLines: Int) {
    Text(
        text = article.title,
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ArticleDescription(article: Article, maxLines: Int) {
    if (article.description.isBlank()) return
    Spacer(Modifier.height(4.dp))
    Text(
        text = article.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SmallThumbnail(article: Article) {
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

/** Categories on the left; share and save on the right. */
@Composable
private fun FooterRow(article: Article, onSaveClick: (Article) -> Unit, isSaving: Boolean) {
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
        ShareButton(article)
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

/**
 * Report action, sized to sit in the metadata row without adding height to it —
 * the row is only as tall as its labelSmall text plus the 18dp source badge.
 */
@Composable
private fun ReportButton(article: Article) {
    val context = LocalContext.current
    TerminalIconButton(
        icon = Icons.Outlined.Flag,
        contentDescription = "Report this article",
        buttonSize = 22.dp,
        iconSize = 14.dp,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    ) {
            context.reportArticle(
                articleId = article.id,
                title = article.title,
                url = article.url,
                source = article.blogName,
            )
        }
}

/** Shares the article link. Stateless, so both card variants drop it in identically. */
@Composable
private fun ShareButton(article: Article) {
    val context = LocalContext.current
    TerminalIconButton(Icons.Filled.Share, "Share", buttonSize = 36.dp, iconSize = 18.dp) { context.shareArticle(article.title, article.url) }
}

@Composable
private fun TerminalThumbnail(seed: Any) {
    val hash = abs(seed.hashCode())
    val symbol = thumbnailSymbols[hash % thumbnailSymbols.size]
    val bgColor = thumbnailBgColors[hash % thumbnailBgColors.size]
    Box(
        modifier = Modifier
            .size(72.dp)
            .clipToBounds()
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
private fun HotThumbnail(article: Article) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
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
            .clipToBounds()
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
        // The big layout is used for every article when picked from the feed menu,
        // so the badge only shows for articles that are actually hot.
        if (article.isHot) {
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
        }
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
