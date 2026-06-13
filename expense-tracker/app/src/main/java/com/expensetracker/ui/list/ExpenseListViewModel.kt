package com.expensetracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val workManager: WorkManager
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = expenseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.delete(expense)
            SyncWorker.enqueueOneTime(workManager)
        }
    }
}
