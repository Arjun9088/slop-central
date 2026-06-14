package com.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.dao.CategoryTotal
import com.expensetracker.data.db.dao.MonthlyTotal
import com.expensetracker.data.db.dao.PaymentMethodTotal
import com.expensetracker.data.db.entity.Expense
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
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val monthlyTotal: Double = 0.0,
    val todayTotal: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val avgTransactionAmount: Double = 0.0,
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val paymentBreakdown: List<PaymentMethodTotal> = emptyList(),
    val monthlyTrend: List<MonthlyTotal> = emptyList(),
    val topExpenses: List<Expense> = emptyList(),
    val totalCount: Int = 0,
    val lastMonthTotal: Double = 0.0,
    val monthChangePercent: Double? = null,
    val projectedMonthEnd: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0

    init {
        val now = LocalDate.now()
        selectedYear = now.year
        selectedMonth = now.monthValue
        loadData()
    }

    fun goToPreviousMonth() {
        if (selectedMonth == 1) {
            selectedMonth = 12
            selectedYear -= 1
        } else {
            selectedMonth -= 1
        }
        loadData()
    }

    fun goToNextMonth() {
        val now = LocalDate.now()
        if (selectedYear == now.year && selectedMonth >= now.monthValue) return
        if (selectedMonth == 12) {
            selectedMonth = 1
            selectedYear += 1
        } else {
            selectedMonth += 1
        }
        loadData()
    }

    fun canGoForward(): Boolean {
        val now = LocalDate.now()
        return !(selectedYear == now.year && selectedMonth >= now.monthValue)
    }

    private fun monthLabel(year: Int, month: Int): String {
        val monthName = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
        return "$monthName $year"
    }

    fun loadData() {
        val year = selectedYear
        val month = selectedMonth

        val targetDate = LocalDate.of(year, month, 1)
        val daysInMonth = targetDate.lengthOfMonth()
        val monthStart = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val monthEnd = targetDate.withDayOfMonth(daysInMonth).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val prevMonthDate = targetDate.minusMonths(1)
        val prevMonthStart = prevMonthDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val prevMonthEnd = prevMonthDate.withDayOfMonth(prevMonthDate.lengthOfMonth())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        val today = LocalDate.now()
        val isCurrentMonth = year == today.year && month == today.monthValue
        val dayOfMonth = if (isCurrentMonth) today.dayOfMonth else daysInMonth

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val todayTotal = if (isCurrentMonth) {
                    expenseDao.getTotalInRange(
                        today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    ) ?: 0.0
                } else {
                    0.0
                }
                val monthlyTotal = expenseDao.getTotalInRange(monthStart, monthEnd) ?: 0.0
                val lastMonthTotal = expenseDao.getTotalInRange(prevMonthStart, prevMonthEnd) ?: 0.0
                val monthlyTxCount = expenseDao.countInRange(monthStart, monthEnd)
                val categoryBreakdown = expenseDao.getTotalByCategoryInRange(monthStart, monthEnd)
                val paymentBreakdown = expenseDao.getTotalByPaymentMethodInRange(monthStart, monthEnd)
                val monthlyTrend = expenseDao.getMonthlyTotals(12)
                val topExpenses = expenseDao.getTopExpensesInRange(monthStart, monthEnd, 5)

                val dailyAverage = if (dayOfMonth > 0) monthlyTotal / dayOfMonth else 0.0
                val avgTransactionAmount = if (monthlyTxCount > 0) monthlyTotal / monthlyTxCount else 0.0
                val projectedMonthEnd = if (isCurrentMonth) dailyAverage * daysInMonth else monthlyTotal
                val monthChangePercent = if (lastMonthTotal > 0) {
                    ((monthlyTotal - lastMonthTotal) / lastMonthTotal) * 100
                } else null

                _state.update {
                    it.copy(
                        monthLabel = monthLabel(year, month),
                        isCurrentMonth = isCurrentMonth,
                        todayTotal = todayTotal,
                        monthlyTotal = monthlyTotal,
                        dailyAverage = dailyAverage,
                        avgTransactionAmount = avgTransactionAmount,
                        lastMonthTotal = lastMonthTotal,
                        monthChangePercent = monthChangePercent,
                        projectedMonthEnd = projectedMonthEnd,
                        categoryBreakdown = categoryBreakdown,
                        paymentBreakdown = paymentBreakdown,
                        monthlyTrend = monthlyTrend.reversed(),
                        topExpenses = topExpenses,
                        totalCount = monthlyTxCount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
