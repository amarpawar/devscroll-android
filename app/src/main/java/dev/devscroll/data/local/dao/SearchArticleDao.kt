package dev.devscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.devscroll.data.local.entity.SearchArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchArticleDao {

    @Upsert
    suspend fun upsertAll(articles: List<SearchArticleEntity>)

    @Query("DELETE FROM search_articles")
    suspend fun clearAll()

    @Query("""
        SELECT s.* FROM search_articles s
        INNER JOIN blogs b ON s.blogId = b.blogId
        WHERE b.isSubscribed = 1
        ORDER BY s.publishedOn DESC
    """)
    fun observeAll(): Flow<List<SearchArticleEntity>>

    @Query("SELECT * FROM search_articles WHERE articleId = :id")
    suspend fun getById(id: Long): SearchArticleEntity?

    @Query("UPDATE search_articles SET isSaved = :saved WHERE articleId = :id")
    suspend fun setSaved(id: Long, saved: Boolean)

    @Query("UPDATE search_articles SET isSaved = 0 WHERE isSaved = 1")
    suspend fun clearSavedFlags()

    @Query("UPDATE search_articles SET isSaved = NOT isSaved WHERE articleId = :id")
    suspend fun toggleSaved(id: Long)

    @Query("UPDATE search_articles SET isRead = 1 WHERE articleId = :id")
    suspend fun markRead(id: Long)
}
