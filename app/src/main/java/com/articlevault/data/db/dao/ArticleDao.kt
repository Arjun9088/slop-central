package com.articlevault.data.db.dao

import androidx.room.*
import com.articlevault.data.db.entity.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: Article)

    @Update
    suspend fun update(article: Article)

    @Query("SELECT * FROM articles ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): Article?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: String): Flow<Article?>

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT a.* FROM articles a INNER JOIN article_tag_cross_ref atr ON a.id = atr.articleId WHERE atr.tagName = :tag ORDER BY a.savedAt DESC")
    fun observeByTag(tag: String): Flow<List<Article>>

    // FTS search — uses rowid subquery for external content FTS4 tables
    @Query("SELECT * FROM articles WHERE rowid IN (SELECT rowid FROM articles_fts WHERE articles_fts MATCH :query) ORDER BY savedAt DESC")
    fun search(query: String): Flow<List<Article>>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int
}
