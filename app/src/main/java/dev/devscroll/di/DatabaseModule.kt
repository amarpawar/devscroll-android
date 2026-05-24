package dev.devscroll.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.devscroll.data.local.AppDatabase
import dev.devscroll.data.local.dao.ArticleDao
import dev.devscroll.data.local.dao.SavedArticleDao
import dev.devscroll.data.local.dao.SearchArticleDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "devscroll.db")
            .build()

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideSavedArticleDao(database: AppDatabase): SavedArticleDao = database.savedArticleDao()

    @Provides
    fun provideSearchArticleDao(database: AppDatabase): SearchArticleDao = database.searchArticleDao()
}
