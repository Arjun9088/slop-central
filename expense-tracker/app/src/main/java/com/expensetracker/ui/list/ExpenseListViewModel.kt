package com.expensetracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val workManager: WorkManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val expenses: StateFlow<List<Expense>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            expenseDao.observeAll()
        } else {
            expenseDao.search(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _categories.value = expenseDao.getAllCategories()
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = if (category != null) category else ""
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.delete(expense)
            SyncWorker.enqueueOneTime(workManager)
            _categories.value = expenseDao.getAllCategories()
        }
    }
}
