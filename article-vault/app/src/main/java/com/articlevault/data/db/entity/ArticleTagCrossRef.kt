package com.articlevault.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "article_tag_cross_ref",
    primaryKeys = ["articleId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["name"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("articleId"),
        Index("tagName")
    ]
)
data class ArticleTagCrossRef(
    val articleId: String,
    val tagName: String
)
