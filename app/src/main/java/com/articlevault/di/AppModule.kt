package com.articlevault.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.articlevault.data.db.AppDatabase
import com.articlevault.data.db.dao.ArticleDao
import com.articlevault.data.db.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "article_vault.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}
