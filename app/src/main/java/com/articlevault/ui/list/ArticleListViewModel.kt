package com.articlevault.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Folder
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.LlmSummarizer
import com.articlevault.receiver.ShareReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ListFilter { ALL, UNREAD, READ, TAG }

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val workManager: WorkManager,
    private val summarizer: LlmSummarizer
) : ViewModel() {

    private val _filter = MutableStateFlow<ListFilter>(ListFilter.ALL)
    val filter: StateFlow<ListFilter> = _filter.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Multi-select state
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    // Duplicate warning
    private val _duplicateWarning = MutableStateFlow<String?>(null)
    val duplicateWarning: StateFlow<String?> = _duplicateWarning.asStateFlow()

    val folders = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val articles: StateFlow<List<Article>> = combine(
        _filter,
        _selectedFolderId,
        _selectedTag
    ) { filter, folderId, tag -> Triple(filter, folderId, tag) }
        .debounce(50)
        .flatMapLatest { (filter, folderId, tag) ->
            when {
                folderId != null -> repository.observeArticlesInFolder(folderId)
                tag != null -> repository.observeArticlesByTag(tag)
                filter == ListFilter.UNREAD -> repository.observeUnread()
                filter == ListFilter.READ -> repository.observeRead()
                else -> repository.observeAllArticles()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: ListFilter) {
        _filter.value = filter
        _selectedFolderId.value = null
        _selectedTag.value = null
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
        _filter.value = ListFilter.ALL
        _selectedTag.value = null
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
        _filter.value = if (tag != null) ListFilter.TAG else ListFilter.ALL
        _selectedFolderId.value = null
    }

    fun saveArticle(url: String) {
        viewModelScope.launch {
            val existing = repository.getArticleByUrl(url.trim())
            if (existing != null) {
                _duplicateWarning.value = existing.title
            } else {
                ShareReceiver.enqueueSave(workManager, url.trim())
            }
        }
    }

    fun saveArticleForce(url: String) {
        _duplicateWarning.value = null
        ShareReceiver.enqueueSave(workManager, url.trim())
    }

    fun dismissDuplicateWarning() {
        _duplicateWarning.value = null
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch {
            repository.deleteArticle(id)
        }
    }

    fun toggleRead(id: String, currentRead: Boolean) {
        viewModelScope.launch {
            repository.updateReadStatus(id, !currentRead)
        }
    }

    // Multi-select operations
    fun enterMultiSelect(id: String) {
        _isMultiSelectMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelection(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
        if (current.isEmpty()) _isMultiSelectMode.value = false
    }

    fun selectAll() {
        _selectedIds.value = articles.value.map { it.id }.toSet()
    }

    fun exitMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.deleteArticles(ids)
            exitMultiSelect()
        }
    }

    fun batchMarkRead() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.updateReadStatusBatch(ids, true)
            exitMultiSelect()
        }
    }

    fun batchMarkUnread() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.updateReadStatusBatch(ids, false)
            exitMultiSelect()
        }
    }

    fun batchMoveToFolder(folderId: Long) {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            ids.forEach { articleId ->
                repository.addToFolder(articleId, folderId)
            }
            exitMultiSelect()
        }
    }

    fun hasModel(): Boolean = summarizer.hasModel()
    fun getSelectedModelName(): String? = summarizer.getSelectedModelName()

    suspend fun summarize(articleId: String): Result<String> {
        return withContext(Dispatchers.Default) {
            val article = repository.getArticle(articleId)
                ?: return@withContext Result.failure(Exception("Article not found."))

            if (article.extractedText.isBlank()) {
                return@withContext Result.failure(Exception("No text content available."))
            }

            try {
                val summary = summarizer.summarize(article.extractedText)
                repository.updateSummary(articleId, summary)
                Result.success(summary)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
