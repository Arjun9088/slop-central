package com.articlevault.data.repository

import com.articlevault.data.db.dao.ArticleDao
import com.articlevault.data.db.dao.TagDao
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val tagDao: TagDao
) {

    fun observeAllArticles(): Flow<List<Article>> = articleDao.observeAll()

    fun observeArticle(id: String): Flow<Article?> = articleDao.observeById(id)

    fun observeArticlesByTag(tag: String): Flow<List<Article>> = articleDao.observeByTag(tag)

    fun searchArticles(query: String): Flow<List<Article>> = articleDao.search(query)

    fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()

    fun observeTagsForArticle(articleId: String): Flow<List<Tag>> =
        tagDao.observeTagsForArticle(articleId)

    suspend fun getArticle(id: String): Article? = articleDao.getById(id)

    suspend fun insertArticle(article: Article) = articleDao.insert(article)

    suspend fun updateArticle(article: Article) = articleDao.update(article)

    suspend fun deleteArticle(id: String) = articleDao.deleteById(id)

    suspend fun insertTags(articleId: String, tagNames: List<String>) =
        tagDao.insertTagsForArticle(articleId, tagNames)
}
