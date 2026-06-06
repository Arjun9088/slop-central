package com.articlevault.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val excerpt: String,
    @ColumnInfo(name = "content")
    val extractedText: String,
    val htmlPath: String = "",
    val summary: String = "",
    val read: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val domain: String = "",
    val readingProgress: Float = 0f,
    val readAt: Long? = null,
    val lastOpenedAt: Long? = null
)
