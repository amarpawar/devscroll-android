package dev.devscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.devscroll.data.local.entity.BlogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlogDao {
    @Upsert
    suspend fun upsertAll(blogs: List<BlogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(blogs: List<BlogEntity>)

    @Query("UPDATE blogs SET isSubscribed = :isSubscribed WHERE blogId = :blogId")
    suspend fun setSubscribed(blogId: Long, isSubscribed: Boolean)

    @Query("DELETE FROM blogs")
    suspend fun clearAll()

    @Query("SELECT * FROM blogs ORDER BY name ASC")
    fun observeAll(): Flow<List<BlogEntity>>
}
