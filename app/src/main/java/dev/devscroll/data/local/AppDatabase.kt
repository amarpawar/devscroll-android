package dev.devscroll.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.devscroll.data.local.dao.ArticleDao
import dev.devscroll.data.local.dao.BlogDao
import dev.devscroll.data.local.dao.SavedArticleDao
import dev.devscroll.data.local.dao.SearchArticleDao
import dev.devscroll.data.local.entity.ArticleEntity
import dev.devscroll.data.local.entity.BlogEntity
import dev.devscroll.data.local.entity.SavedArticleEntity
import dev.devscroll.data.local.entity.SearchArticleEntity

@Database(
    entities = [ArticleEntity::class, SavedArticleEntity::class, SearchArticleEntity::class, BlogEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun savedArticleDao(): SavedArticleDao
    abstract fun searchArticleDao(): SearchArticleDao
    abstract fun blogDao(): BlogDao
}
