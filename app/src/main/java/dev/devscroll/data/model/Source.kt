package dev.devscroll.data.model

data class Source(
    val blogId: Long,
    val blogName: String,
    val articleCount: Int,
    val url: String = "",
    val isSubscribed: Boolean = true,
)
