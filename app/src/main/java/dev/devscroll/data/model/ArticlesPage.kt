package dev.devscroll.data.model

data class ArticlesPage(
    val page: Int,
    val hasNext: Boolean,
    val totalPages: Int,
)
