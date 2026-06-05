package com.articlevault.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.articlevault.data.db.entity.Article
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.receiver.ShareReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val articles = repository.observeAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveArticle(url: String) {
        ShareReceiver.enqueueSave(workManager, url.trim())
    }

    fun deleteArticle(id: String) {
        viewModelScope.launch {
            repository.deleteArticle(id)
        }
    }
}
