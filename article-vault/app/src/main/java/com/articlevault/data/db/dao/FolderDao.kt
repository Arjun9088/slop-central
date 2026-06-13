package com.articlevault.data.db.dao

import androidx.room.*
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.ArticleFolderCrossRef
import com.articlevault.data.db.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ArticleFolderCrossRef)

    @Query("DELETE FROM article_folder_cross_ref WHERE articleId = :articleId AND folderId = :folderId")
    suspend fun deleteCrossRef(articleId: String, folderId: Long)

    @Query("DELETE FROM article_folder_cross_ref WHERE articleId = :articleId")
    suspend fun deleteAllCrossRefsForArticle(articleId: String)

    @Query("""
        SELECT a.* FROM articles a
        INNER JOIN article_folder_cross_ref afc ON a.id = afc.articleId
        WHERE afc.folderId = :folderId
        ORDER BY a.savedAt DESC
    """)
    fun observeArticlesInFolder(folderId: Long): Flow<List<Article>>

    @Query("SELECT folderId FROM article_folder_cross_ref WHERE articleId = :articleId")
    suspend fun getFolderIdsForArticle(articleId: String): List<Long>

    @Transaction
    suspend fun setFoldersForArticle(articleId: String, folderIds: List<Long>) {
        deleteAllCrossRefsForArticle(articleId)
        folderIds.forEach { folderId ->
            insertCrossRef(ArticleFolderCrossRef(articleId, folderId))
        }
    }
}
