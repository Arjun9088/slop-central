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
    val htmlContent: String,
    val savedAt: Long = System.currentTimeMillis()
)
