package com.articlevault.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.db.entity.Folder
import com.articlevault.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    val folders = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name.trim())
        }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch {
            repository.renameFolder(id, name.trim())
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolder(id)
        }
    }
}
