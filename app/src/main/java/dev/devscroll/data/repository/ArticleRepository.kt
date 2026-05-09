package dev.devscroll.data.repository

import dev.devscroll.data.model.Article
import dev.devscroll.data.model.ArticlesPage
import dev.devscroll.data.model.Source
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun observeArticles(): Flow<List<Article>>
    fun observeSaved(): Flow<List<Article>>
    fun observeSources(): Flow<List<Source>>
    suspend fun fetchArticles(page: Int, sortType: String = "NEWEST"): Result<ArticlesPage>
    suspend fun markRead(id: Long)
    suspend fun toggleSave(id: Long)
    suspend fun getArticle(id: Long): Article?
}
