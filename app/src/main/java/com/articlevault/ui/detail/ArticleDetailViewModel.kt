package com.articlevault.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.ReadingPreferences
import com.articlevault.data.ScrollPositionStore
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Tag
import com.articlevault.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: ArticleRepository,
    val scrollStore: ScrollPositionStore,
    val readingPrefs: ReadingPreferences
) : ViewModel() {

    private val _htmlContent = MutableStateFlow<String?>(null)
    val htmlContent: StateFlow<String?> = _htmlContent.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch.asStateFlow()

    fun article(id: String): Flow<Article?> = repository.observeArticle(id)

    fun tags(articleId: String): Flow<List<Tag>> = repository.observeTagsForArticle(articleId)

    fun loadHtmlContent(htmlPath: String) {
        if (htmlPath.isBlank()) return
        viewModelScope.launch {
            _htmlContent.value = withContext(Dispatchers.IO) {
                try { File(htmlPath).readText() } catch (_: Exception) { null }
            }
        }
    }

    fun updateReadingProgress(articleId: String, progress: Float) {
        viewModelScope.launch {
            repository.updateReadingProgress(articleId, progress.coerceIn(0f, 1f))
        }
    }

    fun updateLastOpenedAt(articleId: String) {
        viewModelScope.launch {
            repository.updateLastOpenedAt(articleId, System.currentTimeMillis())
        }
    }

    fun markAsRead(articleId: String) {
        viewModelScope.launch {
            repository.updateReadStatus(articleId, true)
        }
    }

    fun toggleSearch() {
        _showSearch.value = !_showSearch.value
        if (!_showSearch.value) _searchQuery.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteArticle(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            scrollStore.clear(id)
            val article = repository.getArticle(id)
            article?.let { File(it.htmlPath).delete() }
            repository.deleteArticle(id)
            onDeleted()
        }
    }
}
