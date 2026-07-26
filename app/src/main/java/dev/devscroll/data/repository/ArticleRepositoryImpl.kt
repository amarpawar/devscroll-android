package dev.devscroll.data.repository

import dev.devscroll.data.local.DatabaseInitializer
import dev.devscroll.data.local.dao.ArticleDao
import dev.devscroll.data.local.dao.BlogDao
import dev.devscroll.data.local.dao.SavedArticleDao
import dev.devscroll.data.local.dao.SearchArticleDao
import dev.devscroll.data.local.entity.BlogEntity
import dev.devscroll.data.local.entity.toDomain
import dev.devscroll.data.local.entity.toSavedEntity as toSavedLocalEntity
import dev.devscroll.data.model.Article
import dev.devscroll.data.model.ArticlesPage
import dev.devscroll.data.model.Source
import dev.devscroll.data.remote.DevscrollApiService
import dev.devscroll.data.remote.dto.toEntity
import dev.devscroll.data.remote.dto.toSavedEntity
import dev.devscroll.data.remote.dto.toSearchEntity
import dev.devscroll.util.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val apiService: DevscrollApiService,
    private val articleDao: ArticleDao,
    private val savedArticleDao: SavedArticleDao,
    private val searchArticleDao: SearchArticleDao,
    private val blogDao: BlogDao,
    private val databaseInitializer: DatabaseInitializer,
    private val crashReporter: CrashReporter,
) : ArticleRepository {
    private val savingIds = MutableStateFlow<Set<Long>>(emptySet())

    override fun observeSavingIds(): Flow<Set<Long>> = savingIds.asStateFlow()


    override fun observeArticles(): Flow<List<Article>> =
        articleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSaved(): Flow<List<Article>> =
        savedArticleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSearchResults(): Flow<List<Article>> =
        searchArticleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSources(): Flow<List<Source>> =
        blogDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun fetchArticles(page: Int, sortType: String, blogId: Long?): Result<ArticlesPage> =
        runCatching {
            databaseInitializer.awaitReady()
            val response = apiService.getArticles(page = page, sortType = sortType, blogId = blogId)
            val blogEntities = response.articles.map { dto ->
                BlogEntity(
                    blogId = dto.blogId,
                    name = dto.blogName,
                    url = "",
                    articleCount = 0,
                    isSubscribed = true
                )
            }
            blogDao.insertAllIgnore(blogEntities)
            val ids = response.articles.map { it.articleId }
            val userStateMap = articleDao.getUserState(ids).associateBy { it.articleId }
            val savedIds = savedArticleDao.getIds().toSet()
            val entities = response.articles.map { dto ->
                val state = userStateMap[dto.articleId]
                dto.toEntity(
                    isRead = state?.isRead ?: false,
                    isSaved = dto.saved || dto.articleId in savedIds,
                )
            }
            articleDao.upsertAll(entities)
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun fetchSavedArticles(page: Int): Result<ArticlesPage> =
        runCatching {
            databaseInitializer.awaitReady()
            val response = apiService.getSavedArticles(page = page)
            val savedAt = System.currentTimeMillis()
            if (page == 0) {
                savedArticleDao.clearAll()
                articleDao.clearSavedFlags()
                searchArticleDao.clearSavedFlags()
            }
            val entities = response.articles.map { it.toSavedEntity(savedAt = savedAt) }
            savedArticleDao.upsertAll(entities)
            entities.forEach { 
                articleDao.setSaved(it.articleId, true)
                searchArticleDao.setSaved(it.articleId, true)
            }
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun search(query: String, page: Int): Result<ArticlesPage> =
        runCatching {
            databaseInitializer.awaitReady()
            if (page == 0) searchArticleDao.clearAll()
            val response = apiService.searchArticles(query = query, page = page)
            val blogEntities = response.articles.map { dto ->
                BlogEntity(
                    blogId = dto.blogId,
                    name = dto.blogName,
                    url = "",
                    articleCount = 0,
                    isSubscribed = true
                )
            }
            blogDao.insertAllIgnore(blogEntities)
            val ids = response.articles.map { it.articleId }
            val userStateMap = articleDao.getUserState(ids).associateBy { it.articleId }
            val savedIds = savedArticleDao.getIds().toSet()
            val entities = response.articles.map { dto ->
                val state = userStateMap[dto.articleId]
                dto.toSearchEntity(
                    isRead = state?.isRead ?: false,
                    isSaved = dto.saved || dto.articleId in savedIds,
                )
            }
            searchArticleDao.upsertAll(entities)
            ArticlesPage(page = response.page, hasNext = response.hasNext, totalPages = response.totalPages)
        }

    override suspend fun clearSearchResults() = searchArticleDao.clearAll()

    override suspend fun clearFeedArticles() {
        articleDao.clearAll()
    }

    override suspend fun clearSavedArticlesCache() {
        savedArticleDao.clearAll()
        articleDao.clearSavedFlags()
        searchArticleDao.clearSavedFlags()
    }

    override suspend fun getArticleIds(): Set<Long> = articleDao.getIds().toSet()

    override suspend fun markRead(id: Long) {
        articleDao.markRead(id)
        searchArticleDao.markRead(id)
    }

    override suspend fun toggleSave(id: Long): Result<Unit> {
        val article = getArticle(id)
            ?: return Result.failure(IllegalStateException("Article $id not found"))
        val currentlySaved = article.isSaved
        savingIds.update { it + id }
        try {
            // Write locally first so the bookmark reacts immediately, then roll back
            // if the server rejects it — otherwise local state silently diverges.
            applySavedLocally(article, saved = !currentlySaved)
            val apiResult = runCatching {
                if (currentlySaved) apiService.unsaveArticle(id) else apiService.saveArticle(id)
            }
            return apiResult.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error ->
                    applySavedLocally(article, saved = currentlySaved)
                    crashReporter.recordNonFatal(error, "Save toggle rejected — rolled back")
                    Result.failure(error)
                },
            )
        } finally {
            savingIds.update { it - id }
        }
    }

    private suspend fun applySavedLocally(article: Article, saved: Boolean) {
        articleDao.setSaved(article.id, saved)
        searchArticleDao.setSaved(article.id, saved)
        if (saved) {
            savedArticleDao.upsert(article.toSavedLocalEntity(savedAt = System.currentTimeMillis()))
        } else {
            savedArticleDao.delete(article.id)
        }
    }

    override suspend fun getArticle(id: Long): Article? {
        databaseInitializer.awaitReady()
        // saved_articles is the only table populated on the Saved tab, so it has to
        // be part of the lookup or saved articles cannot be opened in the reader.
        return articleDao.getById(id)?.toDomain()
            ?: searchArticleDao.getById(id)?.toDomain()
            ?: savedArticleDao.getById(id)?.toDomain()
    }

    override suspend fun recordArticleHit(articleId: Long): Result<Unit> = runCatching {
        apiService.recordArticleHit(articleId)
    }

    override suspend fun deleteNewestArticles(count: Int) =
        articleDao.deleteNewest(count)

    override suspend fun unsubscribeBlog(blogId: Long): Result<Unit> = runCatching {
        apiService.unsubscribeBlog(blogId)
        blogDao.setSubscribed(blogId, false)
    }

    override suspend fun subscribeBlog(blogId: Long): Result<Unit> = runCatching {
        apiService.subscribeBlog(blogId)
        blogDao.setSubscribed(blogId, true)
    }

    override suspend fun refreshSources(): Result<Unit> = runCatching {
        val response = apiService.getBlogs()
        val entities = response.map { dto ->
            BlogEntity(
                blogId = dto.blogId,
                name = dto.name,
                url = dto.url,
                articleCount = dto.articleCount,
                isSubscribed = dto.isSubscribed
            )
        }
        blogDao.replaceAll(entities)
    }
}
