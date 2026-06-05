package com.articlevault.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.db.entity.Article
import com.articlevault.data.db.entity.Tag
import com.articlevault.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val allTags: StateFlow<List<Tag>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<Article>> = _query
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchArticles(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun onTagSelected(tag: String) {
        _query.value = tag
    }
}
