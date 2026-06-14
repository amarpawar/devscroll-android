package dev.devscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.devscroll.data.local.entity.SavedArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedArticleDao {

    @Upsert
    suspend fun upsertAll(articles: List<SavedArticleEntity>)

    @Upsert
    suspend fun upsert(article: SavedArticleEntity)

    @Query("DELETE FROM saved_articles WHERE articleId = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_articles")
    suspend fun clearAll()

    @Query("SELECT * FROM saved_articles ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedArticleEntity>>

    @Query("SELECT articleId FROM saved_articles")
    suspend fun getIds(): List<Long>

    @Query("SELECT * FROM saved_articles WHERE articleId = :id")
    suspend fun getById(id: Long): SavedArticleEntity?

    @Query("SELECT COUNT(*) FROM saved_articles")
    suspend fun count(): Int

    @Query("""
        DELETE FROM saved_articles
        WHERE articleId IN (
            SELECT articleId FROM saved_articles
            ORDER BY savedAt ASC
            LIMIT :count
        )
    """)
    suspend fun deleteOldest(count: Int)
}
