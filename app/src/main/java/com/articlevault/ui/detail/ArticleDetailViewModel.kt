package com.articlevault.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Tag
import com.articlevault.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    fun article(id: String): Flow<Article?> = repository.observeArticle(id)

    fun tags(articleId: String): Flow<List<Tag>> = repository.observeTagsForArticle(articleId)

    fun deleteArticle(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteArticle(id)
            onDeleted()
        }
    }
}
