package com.articlevault.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.articlevault.data.AppStorage
import com.articlevault.data.db.AppDatabase
import com.articlevault.data.db.dao.ArticleDao
import com.articlevault.data.db.dao.FolderDao
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
    fun provideAppStorage(@ApplicationContext context: Context): AppStorage {
        return AppStorage(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, appStorage: AppStorage): AppDatabase {
        appStorage.initializeStorage()
        val dbFile = appStorage.dbFile

        val migration2to3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN htmlContent TEXT NOT NULL DEFAULT ''")
            }
        }

        val migration3to4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
            }
        }

        val migration4to5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN read INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS article_folder_cross_ref (
                        articleId TEXT NOT NULL,
                        folderId INTEGER NOT NULL,
                        PRIMARY KEY(articleId, folderId),
                        FOREIGN KEY(articleId) REFERENCES articles(id) ON DELETE CASCADE,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_article_folder_cross_ref_articleId ON article_folder_cross_ref(articleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_article_folder_cross_ref_folderId ON article_folder_cross_ref(folderId)")
            }
        }

        val migration5to6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN htmlPath TEXT NOT NULL DEFAULT ''")
            }
        }

        val migration6to7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN wordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN domain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE articles ADD COLUMN readingProgress REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE articles ADD COLUMN readAt INTEGER")
                db.execSQL("ALTER TABLE articles ADD COLUMN lastOpenedAt INTEGER")
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbFile.absolutePath
        )
            .addMigrations(migration2to3, migration3to4, migration4to5, migration5to6, migration6to7)
            .build()
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

    @Provides
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
}
