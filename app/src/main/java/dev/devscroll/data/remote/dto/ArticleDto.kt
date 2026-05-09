package dev.devscroll.data.remote.dto

import dev.devscroll.data.local.entity.ArticleEntity
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

fun ArticleDto.toEntity(isRead: Boolean = false, isSaved: Boolean = false): ArticleEntity {
    val publishedMillis = try {
        isoFormat().parse(publishedOn)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
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
        thumbnailUrl = thumbnailUrl,
        title = title,
        url = url,
        isRead = isRead,
        isSaved = isSaved,
    )
}
