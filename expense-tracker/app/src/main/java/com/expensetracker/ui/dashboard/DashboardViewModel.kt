package com.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.dao.CategoryTotal
import com.expensetracker.data.db.dao.MonthlyTotal
import com.expensetracker.data.db.dao.PaymentMethodTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardState(
    val monthlyTotal: Double = 0.0,
    val todayTotal: Double = 0.0,
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val paymentBreakdown: List<PaymentMethodTotal> = emptyList(),
    val monthlyTrend: List<MonthlyTotal> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                val monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)

                val todayTotal = expenseDao.getTotalInRange(today, today) ?: 0.0
                val monthlyTotal = expenseDao.getTotalInRange(monthStart, monthEnd) ?: 0.0
                val categoryBreakdown = expenseDao.getTotalByCategoryInRange(monthStart, monthEnd)
                val paymentBreakdown = expenseDao.getTotalByPaymentMethodInRange(monthStart, monthEnd)
                val monthlyTrend = expenseDao.getMonthlyTotals(12)
                val count = expenseDao.count()

                _state.update {
                    it.copy(
                        todayTotal = todayTotal,
                        monthlyTotal = monthlyTotal,
                        categoryBreakdown = categoryBreakdown,
                        paymentBreakdown = paymentBreakdown,
                        monthlyTrend = monthlyTrend.reversed(),
                        totalCount = count,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
