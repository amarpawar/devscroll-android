package dev.devscroll.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.devscroll.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

data class ArticleUserState(
    @ColumnInfo(name = "articleId") val articleId: Long,
    @ColumnInfo(name = "isRead") val isRead: Boolean,
    @ColumnInfo(name = "isSaved") val isSaved: Boolean,
)

data class SourceRow(
    @ColumnInfo(name = "blogId") val blogId: Long,
    @ColumnInfo(name = "blogName") val blogName: String,
    @ColumnInfo(name = "articleCount") val articleCount: Int,
)

@Dao
interface ArticleDao {

    @Upsert
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Query("SELECT articleId, isRead, isSaved FROM articles WHERE articleId IN (:ids)")
    suspend fun getUserState(ids: List<Long>): List<ArticleUserState>

    @Query("SELECT * FROM articles ORDER BY publishedOn DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY publishedOn DESC")
    fun observeSaved(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT blogId, blogName, COUNT(*) AS articleCount
        FROM articles
        GROUP BY blogId
        ORDER BY blogName ASC
    """)
    fun observeSources(): Flow<List<SourceRow>>

    @Query("UPDATE articles SET isRead = 1 WHERE articleId = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE articles SET isSaved = :saved WHERE articleId = :id")
    suspend fun setSaved(id: Long, saved: Boolean)

    @Query("UPDATE articles SET isSaved = NOT isSaved WHERE articleId = :id")
    suspend fun toggleSaved(id: Long)

    @Query("SELECT articleId FROM articles")
    suspend fun getIds(): List<Long>

    @Query("SELECT * FROM articles WHERE articleId = :id")
    suspend fun getById(id: Long): ArticleEntity?

    @Query("""
        DELETE FROM articles
        WHERE articleId IN (
            SELECT articleId FROM articles
            ORDER BY publishedOn DESC
            LIMIT :count
        )
    """)
    suspend fun deleteNewest(count: Int)
}
