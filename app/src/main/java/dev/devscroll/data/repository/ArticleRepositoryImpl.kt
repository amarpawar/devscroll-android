package dev.devscroll.data.repository

import dev.devscroll.data.local.dao.ArticleDao
import dev.devscroll.data.local.dao.SavedArticleDao
import dev.devscroll.data.local.dao.SearchArticleDao
import dev.devscroll.data.local.entity.toDomain
import dev.devscroll.data.model.Article
import dev.devscroll.data.model.ArticlesPage
import dev.devscroll.data.model.Source
import dev.devscroll.data.remote.DevscrollApiService
import dev.devscroll.data.remote.dto.toEntity
import dev.devscroll.data.remote.dto.toSavedEntity
import dev.devscroll.data.remote.dto.toSearchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val apiService: DevscrollApiService,
    private val articleDao: ArticleDao,
    private val savedArticleDao: SavedArticleDao,
    private val searchArticleDao: SearchArticleDao,
) : ArticleRepository {

    override fun observeArticles(): Flow<List<Article>> =
        articleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSaved(): Flow<List<Article>> =
        savedArticleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSearchResults(): Flow<List<Article>> =
        searchArticleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSources(): Flow<List<Source>> =
        articleDao.observeSources().map { rows ->
            rows.map { Source(blogId = it.blogId, blogName = it.blogName, articleCount = it.articleCount) }
        }

    override suspend fun fetchArticles(page: Int, sortType: String): Result<ArticlesPage> =
        runCatching {
            val response = apiService.getArticles(page = page, sortType = sortType)
            val ids = response.articles.map { it.articleId }
            val userStateMap = articleDao.getUserState(ids).associateBy { it.articleId }
            val savedIds = savedArticleDao.getIds().toSet()
            val entities = response.articles.map { dto ->
                val state = userStateMap[dto.articleId]
                dto.toEntity(
                    isRead = state?.isRead ?: false,
                    isSaved = dto.articleId in savedIds,
                )
            }
            articleDao.upsertAll(entities)
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun fetchSavedArticles(page: Int): Result<ArticlesPage> =
        runCatching {
            val response = apiService.getSavedArticles(page = page)
            val savedAt = System.currentTimeMillis()
            val entities = response.articles.map { it.toSavedEntity(savedAt = savedAt) }
            savedArticleDao.upsertAll(entities)
            entities.forEach { articleDao.setSaved(it.articleId, true) }
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun search(query: String, page: Int): Result<ArticlesPage> =
        runCatching {
            if (page == 0) searchArticleDao.clearAll()
            val response = apiService.searchArticles(query = query, page = page)
            val ids = response.articles.map { it.articleId }
            val userStateMap = articleDao.getUserState(ids).associateBy { it.articleId }
            val savedIds = savedArticleDao.getIds().toSet()
            val entities = response.articles.map { dto ->
                val state = userStateMap[dto.articleId]
                dto.toSearchEntity(
                    isRead = state?.isRead ?: false,
                    isSaved = dto.articleId in savedIds,
                )
            }
            searchArticleDao.upsertAll(entities)
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun clearSearchResults() = searchArticleDao.clearAll()

    override suspend fun getArticleIds(): Set<Long> = articleDao.getIds().toSet()

    override suspend fun markRead(id: Long) {
        articleDao.markRead(id)
        searchArticleDao.markRead(id)
    }

    override suspend fun toggleSave(id: Long) {
        runCatching { apiService.toggleSave(id) }
        articleDao.toggleSaved(id)
        searchArticleDao.toggleSaved(id)
        savedArticleDao.delete(id)
    }

    override suspend fun getArticle(id: Long): Article? =
        articleDao.getById(id)?.toDomain()
            ?: searchArticleDao.getById(id)?.toDomain()

    override suspend fun deleteNewestArticles(count: Int) =
        articleDao.deleteNewest(count)
}
