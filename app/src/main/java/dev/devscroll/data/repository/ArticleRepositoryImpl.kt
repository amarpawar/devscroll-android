package dev.devscroll.data.repository

import dev.devscroll.data.local.dao.ArticleDao
import dev.devscroll.data.local.entity.toDomain
import dev.devscroll.data.model.Article
import dev.devscroll.data.model.ArticlesPage
import dev.devscroll.data.model.Source
import dev.devscroll.data.remote.DevscrollApiService
import dev.devscroll.data.remote.dto.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val apiService: DevscrollApiService,
    private val articleDao: ArticleDao,
) : ArticleRepository {

    override fun observeArticles(): Flow<List<Article>> =
        articleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSaved(): Flow<List<Article>> =
        articleDao.observeSaved().map { list -> list.map { it.toDomain() } }

    override fun observeSources(): Flow<List<Source>> =
        articleDao.observeSources().map { rows ->
            rows.map { Source(blogId = it.blogId, blogName = it.blogName, articleCount = it.articleCount) }
        }

    override suspend fun fetchArticles(page: Int, sortType: String): Result<ArticlesPage> =
        runCatching {
            val response = apiService.getArticles(page = page, sortType = sortType)

            val ids = response.articles.map { it.articleId }
            val userStateMap = articleDao.getUserState(ids).associateBy { it.articleId }

            val entities = response.articles.map { dto ->
                val state = userStateMap[dto.articleId]
                dto.toEntity(
                    isRead = state?.isRead ?: false,
                    isSaved = state?.isSaved ?: false,
                )
            }
            articleDao.upsertAll(entities)

            ArticlesPage(
                page = response.page,
                hasNext = response.hasNext,
                totalPages = response.totalPages,
            )
        }

    override suspend fun markRead(id: Long) = articleDao.markRead(id)

    override suspend fun toggleSave(id: Long) {
        val entity = articleDao.getById(id) ?: return
        articleDao.setSaved(id, !entity.isSaved)
    }

    override suspend fun getArticle(id: Long): Article? =
        articleDao.getById(id)?.toDomain()
}
