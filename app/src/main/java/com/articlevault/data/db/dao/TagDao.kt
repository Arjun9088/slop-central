package com.articlevault.data.db.dao

import androidx.room.*
import com.articlevault.data.db.entity.ArticleTagCrossRef
import com.articlevault.data.db.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ArticleTagCrossRef)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT t.* FROM tags t INNER JOIN article_tag_cross_ref atr ON t.name = atr.tagName WHERE atr.articleId = :articleId")
    fun observeTagsForArticle(articleId: String): Flow<List<Tag>>

    @Transaction
    suspend fun insertTagsForArticle(articleId: String, tagNames: List<String>) {
        tagNames.forEach { tagName ->
            insert(Tag(tagName))
            insertCrossRef(ArticleTagCrossRef(articleId, tagName))
        }
    }
}
