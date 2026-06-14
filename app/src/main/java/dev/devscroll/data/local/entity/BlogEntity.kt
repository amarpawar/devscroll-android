package dev.devscroll.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.devscroll.data.model.Source

@Entity(tableName = "blogs")
data class BlogEntity(
    @PrimaryKey val blogId: Long,
    val name: String,
    val url: String,
    val articleCount: Int,
    val isSubscribed: Boolean,
)

fun BlogEntity.toDomain() = Source(
    blogId = blogId,
    blogName = name,
    articleCount = articleCount,
    url = url,
    isSubscribed = isSubscribed
)
