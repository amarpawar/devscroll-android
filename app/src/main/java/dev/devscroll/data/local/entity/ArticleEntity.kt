package dev.devscroll.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.devscroll.data.model.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val articleId: Long,
    val authors: List<String>,
    val blogId: Long,
    val blogName: String,
    val categories: List<String>,
    val description: String,
    val hitCount: Int,
    val publishedOn: Long,
    val readMinutes: Int,
    val thumbnailUrl: String?,
    val title: String,
    val url: String,
    val isRead: Boolean = false,
    val isSaved: Boolean = false,
)

fun ArticleEntity.toDomain() = Article(
    id = articleId,
    authors = authors,
    blogId = blogId,
    blogName = blogName,
    categories = categories,
    description = description,
    hitCount = hitCount,
    publishedOn = publishedOn,
    readMinutes = readMinutes,
    thumbnailUrl = thumbnailUrl,
    title = title,
    url = url,
    isHot = hitCount > 50,
    isRead = isRead,
    isSaved = isSaved,
)
