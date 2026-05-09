package dev.devscroll.data.model

data class Article(
    val id: Long,
    val authors: List<String>,
    val blogId: Long,
    val blogName: String,
    val categories: List<String>,   // category slugs
    val description: String,
    val hitCount: Int,
    val publishedOn: Long,          // epoch millis
    val thumbnailUrl: String?,
    val title: String,
    val url: String,
    val readMinutes: Int,           // derived: word count / 200, min 1
    val isHot: Boolean,             // derived: hitCount > 50
    val isRead: Boolean,            // local only
    val isSaved: Boolean,           // local only
)
