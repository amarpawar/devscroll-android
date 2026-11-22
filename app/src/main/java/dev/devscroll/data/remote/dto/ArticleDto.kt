package dev.devscroll.data.remote.dto

import androidx.core.text.HtmlCompat
import dev.devscroll.BuildConfig
import dev.devscroll.data.local.entity.ArticleEntity
import dev.devscroll.data.local.entity.SavedArticleEntity
import dev.devscroll.data.local.entity.SearchArticleEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CategoryDto(
    val categoryId: Long,
    val name: String,
    val slug: String,
)

data class ArticleDto(
    val articleId: Long,
    val authors: List<String>,
    val blogId: Long,
    val blogName: String,
    val categories: List<CategoryDto>,
    val description: String,
    val hitCount: Int,
    val publishedOn: String,
    val readMinutes: Int,
    val thumbnailUrl: String?,
    val title: String,
    val url: String,
    val saved: Boolean = false,
)

data class ArticlesPageDto(
    val articles: List<ArticleDto>,
    val hasNext: Boolean,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
)

// Cursor-paginated response (feed, search); nextCursor is null on the last page.
data class CursorPageDto(
    val articles: List<ArticleDto>,
    val hasNext: Boolean,
    val nextCursor: String?,
)

private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * The API passes feed text through with HTML entities intact (e.g. `&#8217;` for ’), so decode
 * them before the text reaches the database. Skips the parse when there's nothing to decode.
 */
private fun String.decodeHtmlEntities(): String =
    if ('&' !in this) this
    else HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()

private fun getFullThumbnailUrl(thumbnail: String?): String? {
    if (thumbnail == null) return null
    if (thumbnail.startsWith("http://", ignoreCase = true) || 
        thumbnail.startsWith("https://", ignoreCase = true)) {
        return thumbnail
    }
    return "${BuildConfig.API_BASE_URL}devscroll/api/thumbnails/$thumbnail"
}

fun ArticleDto.toEntity(isRead: Boolean = false, isSaved: Boolean = false): ArticleEntity {
    val publishedMillis = parsedPublishedMillis()
    return ArticleEntity(
        articleId = articleId,
        authors = authors.map { it.decodeHtmlEntities() },
        blogId = blogId,
        blogName = blogName.decodeHtmlEntities(),
        categories = categories.map { it.slug },
        description = description.decodeHtmlEntities(),
        hitCount = hitCount,
        publishedOn = publishedMillis,
        readMinutes = readMinutes,
        thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
        title = title.decodeHtmlEntities(),
        url = url,
        isRead = isRead,
        isSaved = isSaved,
    )
}

fun ArticleDto.toSavedEntity(savedAt: Long = System.currentTimeMillis()) = SavedArticleEntity(
    articleId = articleId,
    authors = authors.map { it.decodeHtmlEntities() },
    blogId = blogId,
    blogName = blogName.decodeHtmlEntities(),
    categories = categories.map { it.slug },
    description = description.decodeHtmlEntities(),
    hitCount = hitCount,
    publishedOn = parsedPublishedMillis(),
    readMinutes = readMinutes,
    thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
    title = title.decodeHtmlEntities(),
    url = url,
    savedAt = savedAt,
)

fun ArticleDto.toSearchEntity(isRead: Boolean = false, isSaved: Boolean = false) = SearchArticleEntity(
    articleId = articleId,
    authors = authors.map { it.decodeHtmlEntities() },
    blogId = blogId,
    blogName = blogName.decodeHtmlEntities(),
    categories = categories.map { it.slug },
    description = description.decodeHtmlEntities(),
    hitCount = hitCount,
    publishedOn = parsedPublishedMillis(),
    readMinutes = readMinutes,
    thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
    title = title.decodeHtmlEntities(),
    url = url,
    isRead = isRead,
    isSaved = isSaved,
)

private fun ArticleDto.parsedPublishedMillis() = try {
    isoFormat().parse(publishedOn)?.time ?: System.currentTimeMillis()
} catch (e: Exception) {
    System.currentTimeMillis()
}
