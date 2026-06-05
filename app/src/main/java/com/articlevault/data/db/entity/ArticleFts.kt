package com.articlevault.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Article::class)
@Entity(tableName = "articles_fts")
data class ArticleFts(
    val title: String,
    val content: String
)
