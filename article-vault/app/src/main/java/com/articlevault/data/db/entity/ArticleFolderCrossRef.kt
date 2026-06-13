package com.articlevault.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "article_folder_cross_ref",
    primaryKeys = ["articleId", "folderId"],
    foreignKeys = [
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("articleId"),
        Index("folderId")
    ]
)
data class ArticleFolderCrossRef(
    val articleId: String,
    val folderId: Long
)
