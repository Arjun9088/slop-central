package com.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val description: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val syncedToSheet: Boolean = false,
    val sheetRowId: Long? = null
)

enum class ExpenseCategory(val displayName: String) {
    FOOD_DRINK("Food and drink"),
    SHOPPING("Shopping"),
    TRANSPORTATION("Transportation"),
    ENTERTAINMENT("Entertainment"),
    BILLS_UTILITIES("Bills and utilities"),
    OTHER("Other")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    DIGITAL_WALLET("Digital Wallet"),
    GPAY("GPay")
}
