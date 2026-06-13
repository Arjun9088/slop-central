package com.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.db.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observeById(id: Long): Flow<Expense?>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun observeByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE syncedToSheet = 0")
    suspend fun getUnsynced(): List<Expense>

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    suspend fun getAllSyncedAndUnsynced(): List<Expense>

    @Query("SELECT * FROM expenses WHERE sheetRowId IS NOT NULL")
    suspend fun getAllSynced(): List<Expense>

    @Query("SELECT * FROM expenses WHERE date = :date AND description = :description AND amount = :amount LIMIT 1")
    suspend fun findByNaturalKey(date: String, description: String, amount: Double): Expense?

    @Query("SELECT * FROM expenses WHERE sheetRowId = :sheetRowId LIMIT 1")
    suspend fun findBySheetRowId(sheetRowId: Long): Expense?

    @Query("UPDATE expenses SET syncedToSheet = 1, sheetRowId = :sheetRowId WHERE id = :id")
    suspend fun markSynced(id: Long, sheetRowId: Long)

    @Query("UPDATE expenses SET syncedToSheet = 0, sheetRowId = NULL WHERE id = :id")
    suspend fun markUnsynced(id: Long)

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalInRange(startDate: String, endDate: String): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE date >= :startDate AND date <= :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getTotalByCategoryInRange(startDate: String, endDate: String): List<CategoryTotal>

    @Query("SELECT paymentMethod, SUM(amount) as total FROM expenses WHERE date >= :startDate AND date <= :endDate GROUP BY paymentMethod ORDER BY total DESC")
    suspend fun getTotalByPaymentMethodInRange(startDate: String, endDate: String): List<PaymentMethodTotal>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int
}

data class CategoryTotal(val category: String, val total: Double)
data class PaymentMethodTotal(val paymentMethod: String, val total: Double)
