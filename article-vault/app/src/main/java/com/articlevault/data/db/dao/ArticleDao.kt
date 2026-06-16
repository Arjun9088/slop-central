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

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): Article?

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE articles SET summary = :summary WHERE id = :id")
    suspend fun updateSummary(id: String, summary: String)

    @Query("UPDATE articles SET read = :isRead, readAt = :readAt WHERE id = :id")
    suspend fun updateReadStatus(id: String, isRead: Boolean, readAt: Long?)

    @Query("UPDATE articles SET read = :isRead WHERE id = :id")
    suspend fun updateReadStatusSimple(id: String, isRead: Boolean)

    @Query("UPDATE articles SET readingProgress = :progress WHERE id = :id")
    suspend fun updateReadingProgress(id: String, progress: Float)

    @Query("UPDATE articles SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpenedAt(id: String, timestamp: Long)

    @Query("SELECT * FROM articles WHERE read = 0 ORDER BY savedAt DESC")
    fun observeUnread(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE read = 1 ORDER BY savedAt DESC")
    fun observeRead(): Flow<List<Article>>

    @Query("SELECT a.* FROM articles a INNER JOIN article_tag_cross_ref atr ON a.id = atr.articleId WHERE atr.tagName = :tag ORDER BY a.savedAt DESC")
    fun observeByTag(tag: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE rowid IN (SELECT rowid FROM articles_fts WHERE articles_fts MATCH :query) ORDER BY savedAt DESC")
    fun search(query: String): Flow<List<Article>>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE read = 1")
    suspend fun countRead(): Int

    @Query("SELECT SUM(wordCount) FROM articles")
    suspend fun totalWordCount(): Int

    @Query("SELECT SUM(wordCount) FROM articles WHERE read = 1")
    suspend fun totalWordsRead(): Int

    @Query("SELECT domain, COUNT(*) as count FROM articles WHERE domain != '' GROUP BY domain ORDER BY count DESC LIMIT :limit")
    suspend fun getTopDomains(limit: Int = 10): List<DomainCount>

    @Query("SELECT readAt FROM articles WHERE read = 1 AND readAt IS NOT NULL ORDER BY readAt DESC")
    suspend fun getReadTimestamps(): List<Long>

    @Query("SELECT savedAt FROM articles ORDER BY savedAt DESC")
    suspend fun getAllSavedTimestamps(): List<Long>

    @Query("SELECT * FROM articles WHERE read = 1 ORDER BY readAt DESC")
    suspend fun getAllRead(): List<Article>

    @Query("SELECT COUNT(*) FROM articles WHERE savedAt >= :startOfDayMs AND savedAt < :endOfDayMs")
    suspend fun countSavedToday(startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT COUNT(*) FROM articles WHERE read = 1 AND readAt >= :startOfDayMs AND readAt < :endOfDayMs")
    suspend fun countReadToday(startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT COALESCE(SUM(wordCount), 0) FROM articles WHERE read = 1 AND readAt >= :startOfDayMs AND readAt < :endOfDayMs")
    suspend fun totalWordsReadToday(startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT COALESCE(SUM(wordCount), 0) FROM articles WHERE read = 0 AND savedAt >= :startOfDayMs AND savedAt < :endOfDayMs")
    suspend fun totalWordsUnreadToday(startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT COUNT(*) FROM articles WHERE read = 0 AND savedAt >= :startOfDayMs AND savedAt < :endOfDayMs")
    suspend fun countUnreadToday(startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT domain, COUNT(*) as count FROM articles WHERE domain != '' AND savedAt >= :startOfDayMs AND savedAt < :endOfDayMs GROUP BY domain ORDER BY count DESC LIMIT :limit")
    suspend fun getTopDomainsToday(startOfDayMs: Long, endOfDayMs: Long, limit: Int = 1): List<DomainCount>
}

data class DomainCount(
    val domain: String,
    val count: Int
)
