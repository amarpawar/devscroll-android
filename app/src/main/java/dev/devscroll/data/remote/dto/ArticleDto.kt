package dev.devscroll.data.remote.dto

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

private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

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
        authors = authors,
        blogId = blogId,
        blogName = blogName,
        categories = categories.map { it.slug },
        description = description,
        hitCount = hitCount,
        publishedOn = publishedMillis,
        readMinutes = readMinutes,
        thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
        title = title,
        url = url,
        isRead = isRead,
        isSaved = isSaved,
    )
}

fun ArticleDto.toSavedEntity(savedAt: Long = System.currentTimeMillis()) = SavedArticleEntity(
    articleId = articleId,
    authors = authors,
    blogId = blogId,
    blogName = blogName,
    categories = categories.map { it.slug },
    description = description,
    hitCount = hitCount,
    publishedOn = parsedPublishedMillis(),
    readMinutes = readMinutes,
    thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
    title = title,
    url = url,
    savedAt = savedAt,
)

fun ArticleDto.toSearchEntity(isRead: Boolean = false, isSaved: Boolean = false) = SearchArticleEntity(
    articleId = articleId,
    authors = authors,
    blogId = blogId,
    blogName = blogName,
    categories = categories.map { it.slug },
    description = description,
    hitCount = hitCount,
    publishedOn = parsedPublishedMillis(),
    readMinutes = readMinutes,
    thumbnailUrl = getFullThumbnailUrl(thumbnailUrl),
    title = title,
    url = url,
    isRead = isRead,
    isSaved = isSaved,
)

private fun ArticleDto.parsedPublishedMillis() = try {
    isoFormat().parse(publishedOn)?.time ?: System.currentTimeMillis()
} catch (e: Exception) {
    System.currentTimeMillis()
}
