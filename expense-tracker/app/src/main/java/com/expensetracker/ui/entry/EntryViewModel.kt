package com.expensetracker.ui.entry

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.data.sync.SyncPreferences
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.db.entity.defaultCategories
import com.expensetracker.data.db.entity.defaultPaymentMethods
import com.expensetracker.data.sync.SyncWorker
import com.expensetracker.widget.QuickEntryWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EntryUiState(
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val description: String = "",
    val category: String = defaultCategories.first(),
    val amount: String = "",
    val paymentMethod: String = defaultPaymentMethods.first(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val syncPreferences: SyncPreferences,
    private val workManager: WorkManager,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private var editingExpenseId: Long? = null

    fun setInitialCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            val expense = expenseDao.getById(id) ?: return@launch
            editingExpenseId = expense.id
            _uiState.value = EntryUiState(
                date = expense.date,
                description = expense.description,
                category = expense.category,
                amount = expense.amount.toString(),
                paymentMethod = expense.paymentMethod,
                isEditing = true
            )
        }
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(date = date) }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun updateCategory(cat: String) {
        _uiState.update { it.copy(category = cat) }
    }

    fun updateAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    fun updatePaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun save() {
        val state = _uiState.value
        val amountValue = state.amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount") }
            return
        }
        if (state.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a description") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val expense = Expense(
                    id = editingExpenseId ?: 0,
                    date = state.date,
                    description = state.description.trim(),
                    category = state.category,
                    amount = amountValue,
                    paymentMethod = state.paymentMethod,
                    modifiedAt = now,
                    syncedToSheet = false
                )

                if (editingExpenseId != null) {
                    expenseDao.update(expense)
                } else {
                    expenseDao.insert(expense)
                }

                SyncWorker.enqueueOneTime(workManager)
                QuickEntryWidgetProvider.updateWidgets(application)

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = EntryUiState()
        editingExpenseId = null
    }
}
