package com.articlevault.data.repository

import com.articlevault.data.db.dao.ArticleDao
import com.articlevault.data.db.dao.DomainCount
import com.articlevault.data.db.dao.FolderDao
import com.articlevault.data.db.dao.TagDao
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Folder
import com.articlevault.data.db.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val tagDao: TagDao,
    private val folderDao: FolderDao
) {

    // Articles
    fun observeAllArticles(): Flow<List<Article>> = articleDao.observeAll()
    fun observeArticle(id: String): Flow<Article?> = articleDao.observeById(id)
    fun observeArticlesByTag(tag: String): Flow<List<Article>> = articleDao.observeByTag(tag)
    fun observeUnread(): Flow<List<Article>> = articleDao.observeUnread()
    fun observeRead(): Flow<List<Article>> = articleDao.observeRead()
    fun searchArticles(query: String): Flow<List<Article>> = articleDao.search(query)

    suspend fun getArticle(id: String): Article? = articleDao.getById(id)
    suspend fun getArticleByUrl(url: String): Article? = articleDao.getByUrl(url)
    suspend fun insertArticle(article: Article) = articleDao.insert(article)
    suspend fun updateArticle(article: Article) = articleDao.update(article)
    suspend fun deleteArticle(id: String) = articleDao.deleteById(id)
    suspend fun deleteArticles(ids: List<String>) = articleDao.deleteByIds(ids)
    suspend fun updateSummary(id: String, summary: String) = articleDao.updateSummary(id, summary)
    suspend fun updateReadStatus(id: String, isRead: Boolean, readAt: Long? = if (isRead) System.currentTimeMillis() else null) =
        articleDao.updateReadStatus(id, isRead, readAt)
    suspend fun updateReadStatusBatch(ids: List<String>, isRead: Boolean) {
        val readAt = if (isRead) System.currentTimeMillis() else null
        ids.forEach { articleDao.updateReadStatus(it, isRead, readAt) }
    }
    suspend fun updateReadingProgress(id: String, progress: Float) = articleDao.updateReadingProgress(id, progress)
    suspend fun updateLastOpenedAt(id: String, timestamp: Long) = articleDao.updateLastOpenedAt(id, timestamp)

    // Tags
    fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()
    fun observeTagsForArticle(articleId: String): Flow<List<Tag>> = tagDao.observeTagsForArticle(articleId)
    suspend fun insertTags(articleId: String, tagNames: List<String>) = tagDao.insertTagsForArticle(articleId, tagNames)

    // Folders
    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()
    fun observeArticlesInFolder(folderId: Long): Flow<List<Article>> = folderDao.observeArticlesInFolder(folderId)
    suspend fun createFolder(name: String): Long = folderDao.insert(Folder(name = name))
    suspend fun renameFolder(id: Long, name: String) = folderDao.update(Folder(id = id, name = name))
    suspend fun deleteFolder(id: Long) = folderDao.deleteById(id)
    suspend fun setFoldersForArticle(articleId: String, folderIds: List<Long>) = folderDao.setFoldersForArticle(articleId, folderIds)
    suspend fun getFolderIdsForArticle(articleId: String): List<Long> = folderDao.getFolderIdsForArticle(articleId)
    suspend fun addToFolder(articleId: String, folderId: Long) = folderDao.insertCrossRef(
        com.articlevault.data.db.entity.ArticleFolderCrossRef(articleId, folderId)
    )

    // Stats
    suspend fun countArticles(): Int = articleDao.count()
    suspend fun countRead(): Int = articleDao.countRead()
    suspend fun totalWordCount(): Int = articleDao.totalWordCount()
    suspend fun totalWordsRead(): Int = articleDao.totalWordsRead()
    suspend fun getTopDomains(limit: Int = 10): List<DomainCount> = articleDao.getTopDomains(limit)
    suspend fun getReadTimestamps(): List<Long> = articleDao.getReadTimestamps()
    suspend fun getAllSavedTimestamps(): List<Long> = articleDao.getAllSavedTimestamps()
    suspend fun getAllRead(): List<Article> = articleDao.getAllRead()

    suspend fun countSavedToday(s: Long, e: Long): Int = articleDao.countSavedToday(s, e)
    suspend fun countReadToday(s: Long, e: Long): Int = articleDao.countReadToday(s, e)
    suspend fun totalWordsReadToday(s: Long, e: Long): Int = articleDao.totalWordsReadToday(s, e)
    suspend fun totalWordsUnreadToday(s: Long, e: Long): Int = articleDao.totalWordsUnreadToday(s, e)
    suspend fun countUnreadToday(s: Long, e: Long): Int = articleDao.countUnreadToday(s, e)
    suspend fun getTopDomainsToday(s: Long, e: Long, limit: Int = 1): List<DomainCount> =
        articleDao.getTopDomainsToday(s, e, limit)
}
