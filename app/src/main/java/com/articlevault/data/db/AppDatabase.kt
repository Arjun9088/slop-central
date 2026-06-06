package com.articlevault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.articlevault.data.db.dao.ArticleDao
import com.articlevault.data.db.dao.FolderDao
import com.articlevault.data.db.dao.TagDao
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.ArticleFts
import com.articlevault.data.db.entity.ArticleFolderCrossRef
import com.articlevault.data.db.entity.ArticleTagCrossRef
import com.articlevault.data.db.entity.Folder
import com.articlevault.data.db.entity.Tag

@Database(
    entities = [
        Article::class,
        ArticleFts::class,
        Tag::class,
        ArticleTagCrossRef::class,
        Folder::class,
        ArticleFolderCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
}
