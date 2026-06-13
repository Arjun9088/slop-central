package com.expensetracker.data.repository

import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun observeAll(): Flow<List<Expense>> = expenseDao.observeAll()

    fun observeByDateRange(startDate: String, endDate: String): Flow<List<Expense>> =
        expenseDao.observeByDateRange(startDate, endDate)

    fun observeByCategory(category: String): Flow<List<Expense>> =
        expenseDao.observeByCategory(category)

    suspend fun getById(id: Long): Expense? = expenseDao.getById(id)

    suspend fun insert(expense: Expense): Long = expenseDao.insert(expense)

    suspend fun update(expense: Expense) = expenseDao.update(expense)

    suspend fun delete(expense: Expense) = expenseDao.delete(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteById(id)

    suspend fun getUnsynced(): List<Expense> = expenseDao.getUnsynced()

    suspend fun markSynced(id: Long, sheetRowId: Long) = expenseDao.markSynced(id, sheetRowId)

    suspend fun markUnsynced(id: Long) = expenseDao.markUnsynced(id)

    suspend fun getTotalInRange(startDate: String, endDate: String): Double? =
        expenseDao.getTotalInRange(startDate, endDate)

    suspend fun count(): Int = expenseDao.count()
}
