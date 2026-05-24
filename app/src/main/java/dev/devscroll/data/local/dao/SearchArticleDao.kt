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

    @Query("SELECT * FROM search_articles ORDER BY publishedOn DESC")
    fun observeAll(): Flow<List<SearchArticleEntity>>

    @Query("SELECT * FROM search_articles WHERE articleId = :id")
    suspend fun getById(id: Long): SearchArticleEntity?

    @Query("UPDATE search_articles SET isSaved = NOT isSaved WHERE articleId = :id")
    suspend fun toggleSaved(id: Long)

    @Query("UPDATE search_articles SET isRead = 1 WHERE articleId = :id")
    suspend fun markRead(id: Long)
}
