package dev.devscroll.data.repository

import dev.devscroll.data.model.Article
import dev.devscroll.data.model.ArticlesPage
import dev.devscroll.data.model.CursorPage
import dev.devscroll.data.model.Source
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun observeArticles(): Flow<List<Article>>
    fun observeSaved(): Flow<List<Article>>
    fun observeSources(): Flow<List<Source>>
    /** Pass null [cursor] for the first page, then the previous page's nextCursor. */
    suspend fun fetchArticles(cursor: String?, sortType: String = "NEWEST", blogId: Long? = null): Result<CursorPage>
    suspend fun fetchSavedArticles(page: Int = 0): Result<ArticlesPage>
    suspend fun search(query: String, cursor: String? = null): Result<CursorPage>
    fun observeSearchResults(): Flow<List<Article>>
    suspend fun clearSearchResults()
    suspend fun clearFeedArticles()
    suspend fun clearSavedArticlesCache()
    suspend fun getArticleIds(): Set<Long>
    suspend fun markRead(id: Long)
    suspend fun toggleSave(id: Long): Result<Unit>
    suspend fun getArticle(id: Long): Article?
    suspend fun recordArticleHit(articleId: Long): Result<Unit>
    suspend fun deleteNewestArticles(count: Int)
    suspend fun unsubscribeBlog(blogId: Long): Result<Unit>
    suspend fun subscribeBlog(blogId: Long): Result<Unit>
    suspend fun refreshSources(): Result<Unit>
    fun observeSavingIds(): Flow<Set<Long>>
}
