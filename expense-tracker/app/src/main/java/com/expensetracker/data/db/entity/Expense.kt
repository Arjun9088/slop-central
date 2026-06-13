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
    val sheetRowId: Long? = null,
    val source: String = "manual",
    val dedupHash: String? = null
)

val defaultCategories = listOf(
    "Food and drink",
    "Shopping",
    "Transportation",
    "Entertainment",
    "Bills and utilities",
    "Other"
)

val defaultPaymentMethods = listOf(
    "Cash",
    "Credit Card",
    "Debit Card",
    "Digital Wallet",
    "gpay"
)
