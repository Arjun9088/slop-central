package com.expensetracker.data.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SpreadsheetInfo(
    val id: String,
    val name: String
)

sealed class SheetsResult<out T> {
    data class Success<T>(val data: T) : SheetsResult<T>()
    data class ConsentRequired(val consentIntent: Intent) : SheetsResult<Nothing>()
    data class Error(val exception: Exception) : SheetsResult<Nothing>()
}

@Singleton
class GoogleSheetsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPreferences: SyncPreferences
) {
    private var sheetsService: Sheets? = null
    private var driveService: Drive? = null

    private fun buildCredential(email: String): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS, "https://www.googleapis.com/auth/drive.readonly")
        ).apply {
            selectedAccount = android.accounts.Account(email, "com.google")
        }
    }

    private fun buildServices(cred: GoogleAccountCredential) {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        sheetsService = Sheets.Builder(transport, jsonFactory, cred)
            .setApplicationName("ExpenseTracker")
            .build()

        driveService = Drive.Builder(transport, jsonFactory, cred)
            .setApplicationName("ExpenseTracker")
            .build()
    }

    fun buildServiceForAccount(email: String) {
        buildServices(buildCredential(email))
    }

    fun isServiceReady(): Boolean = sheetsService != null

    fun isConfigured(): Boolean =
        sheetsService != null &&
            syncPreferences.getSpreadsheetId() != null &&
            syncPreferences.getSheetName() != null

    private fun getSheetRange(range: String): String {
        val sheetName = syncPreferences.getSheetName() ?: "Sheet1"
        return "'$sheetName'!$range"
    }

    suspend fun listSpreadsheets(): SheetsResult<List<SpreadsheetInfo>> {
        val drive = driveService ?: return SheetsResult.Error(IllegalStateException("Not configured"))

        return withContext(Dispatchers.IO) {
            try {
                val result: FileList = drive.files().list()
                    .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .setPageSize(100)
                    .execute()

                SheetsResult.Success(result.files.map { SpreadsheetInfo(id = it.id, name = it.name) })
            } catch (e: UserRecoverableAuthIOException) {
                SheetsResult.ConsentRequired(e.intent)
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException) {
                val message = e.cause?.message ?: e.message ?: "Unknown auth error"
                if (message.contains("UnregisteredOnApiConsole", ignoreCase = true)) {
                    SheetsResult.Error(IllegalStateException("App not registered in Google Cloud Console. Add the app's SHA-1 fingerprint to your OAuth client."))
                } else if (message.contains("NeedRemoteConsent", ignoreCase = true) || message.contains("UserRecoverable", ignoreCase = true)) {
                    val intent = android.content.Intent()
                    SheetsResult.Error(IllegalStateException("Authorization needed. Please reconnect your Google account."))
                } else {
                    SheetsResult.Error(e)
                }
            } catch (e: Exception) {
                SheetsResult.Error(e)
            }
        }
    }

    suspend fun listSheetTabs(): SheetsResult<List<String>> {
        val service = sheetsService ?: return SheetsResult.Error(IllegalStateException("Not configured"))
        val spreadsheetId = syncPreferences.getSpreadsheetId()
            ?: return SheetsResult.Error(IllegalStateException("No spreadsheet selected"))

        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
                val tabNames = spreadsheet.sheets.map { 
                    Log.d("GoogleSheetsService", "Tab: '${it.properties.title}', rows=${it.properties.gridProperties.rowCount}, cols=${it.properties.gridProperties.columnCount}")
                    it.properties.title 
                }
                SheetsResult.Success(tabNames)
            } catch (e: UserRecoverableAuthIOException) {
                SheetsResult.ConsentRequired(e.intent)
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "List tabs failed: ${e.message}", e)
                SheetsResult.Error(e)
            }
        }
    }

    suspend fun appendRow(expense: Expense): Long? {
        val service = sheetsService ?: return null
        val spreadsheetId = syncPreferences.getSpreadsheetId() ?: return null

        return withContext(Dispatchers.IO) {
            try {
                val values = listOf(
                    listOf(
                        expense.date,
                        expense.description,
                        expense.category,
                        expense.amount.toString(),
                        expense.paymentMethod,
                        expense.modifiedAt.toString()
                    )
                )

                val body = ValueRange().setValues(values)
                val response = service.spreadsheets().values()
                    .append(spreadsheetId, getSheetRange("B:G"), body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                response.updates?.updatedRange?.let { range ->
                    val rowStr = range.substringAfter("!").substringBefore(":")
                        .filter { it.isDigit() }
                    rowStr.toLongOrNull()
                }
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Append failed: ${e.message}", e)
                null
            }
        }
    }

    suspend fun updateRow(rowId: Long, expense: Expense): Boolean {
        val service = sheetsService ?: return false
        val spreadsheetId = syncPreferences.getSpreadsheetId() ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val range = getSheetRange("B$rowId:G$rowId")
                val values = listOf(
                    listOf(
                        expense.date,
                        expense.description,
                        expense.category,
                        expense.amount.toString(),
                        expense.paymentMethod,
                        expense.modifiedAt.toString()
                    )
                )

                val body = ValueRange().setValues(values)
                service.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute()

                true
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Update failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deleteRow(rowId: Long): Boolean {
        val service = sheetsService ?: return false
        val spreadsheetId = syncPreferences.getSpreadsheetId() ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val request = com.google.api.services.sheets.v4.model.DeleteDimensionRequest().apply {
                    range = com.google.api.services.sheets.v4.model.DimensionRange().apply {
                        sheetId = getSheetId()
                        dimension = "ROWS"
                        startIndex = rowId.toInt() - 1
                        endIndex = rowId.toInt()
                    }
                }
                val batchRequest = com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest().apply {
                    requests = listOf(com.google.api.services.sheets.v4.model.Request().apply { deleteDimension = request })
                }

                service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()
                true
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Delete failed: ${e.message}", e)
                false
            }
        }
    }

    private suspend fun getSheetId(): Int {
        val service = sheetsService ?: return 0
        val spreadsheetId = syncPreferences.getSpreadsheetId() ?: return 0
        val sheetName = syncPreferences.getSheetName() ?: return 0

        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
                spreadsheet.sheets.first { it.properties.title == sheetName }.properties.sheetId
            } catch (e: Exception) {
                0
            }
        }
    }

    suspend fun fetchAllRows(): List<List<Any>> {
        val service = sheetsService ?: return emptyList()
        val spreadsheetId = syncPreferences.getSpreadsheetId() ?: return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val sheetName = syncPreferences.getSheetName() ?: "Sheet1"
                val response = service.spreadsheets().values()
                    .get(spreadsheetId, "'$sheetName'")
                    .execute()
                response.getValues() ?: emptyList()
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Fetch failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    data class SheetExpense(
        val rowNumber: Long,
        val date: String,
        val description: String,
        val category: String,
        val amount: Double,
        val paymentMethod: String,
        val modifiedAt: Long
    )

    fun parseRowsToSheetExpenses(rows: List<List<Any>>): List<SheetExpense> {
        if (rows.isEmpty()) return emptyList()

        // Find the header row by looking for "Date" in any column
        var headerIndex = -1
        for (i in rows.indices) {
            val row = rows[i]
            if (row.any { it.toString().trim().equals("Date", ignoreCase = true) }) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) return emptyList()

        // Find column indices from header
        val headerRow = rows[headerIndex]
        val colMap = mutableMapOf<String, Int>()
        for (i in headerRow.indices) {
            val name = headerRow[i].toString().trim().lowercase()
            when {
                name.contains("date") -> colMap["date"] = i
                name.contains("desc") -> colMap["description"] = i
                name.contains("categ") -> colMap["category"] = i
                name.contains("amount") -> colMap["amount"] = i
                name.contains("payment") || name.contains("method") -> colMap["payment"] = i
            }
        }

        val dateCol = colMap["date"] ?: 0
        val descCol = colMap["description"] ?: 1
        val catCol = colMap["category"] ?: 2
        val amountCol = colMap["amount"] ?: 3
        val payCol = colMap["payment"] ?: 4

        return rows.drop(headerIndex + 1).mapIndexedNotNull { index, row ->
            try {
                if (row.size <= dateCol) return@mapIndexedNotNull null
                if (row.all { it.toString().isBlank() }) return@mapIndexedNotNull null
                val nonEmpty = row.count { it.toString().isNotBlank() }
                if (nonEmpty < 3) return@mapIndexedNotNull null

                val date = row.getOrNull(dateCol)?.toString()?.trim() ?: return@mapIndexedNotNull null
                if (date.isBlank()) return@mapIndexedNotNull null

                val description = row.getOrNull(descCol)?.toString()?.trim() ?: ""
                val categoryRaw = row.getOrNull(catCol)?.toString()?.trim() ?: ""
                val amountStr = row.getOrNull(amountCol)?.toString()?.trim()?.replace(",", "")?.replace("₹", "")?.replace("$", "")?.replace("€", "")?.replace("£", "") ?: ""
                val paymentRaw = row.getOrNull(payCol)?.toString()?.trim() ?: ""
                val modifiedAt = row.getOrNull(5)?.toString()?.trim()?.toLongOrNull() ?: 0L

                val amount = amountStr.toDoubleOrNull() ?: return@mapIndexedNotNull null

                SheetExpense(
                    rowNumber = (headerIndex + index + 2).toLong(),
                    date = date,
                    description = description,
                    category = categoryRaw,
                    amount = amount,
                    paymentMethod = paymentRaw,
                    modifiedAt = modifiedAt
                )
            } catch (e: Exception) {
                Log.w("GoogleSheetsService", "Skipping row $index: ${e.message}")
                null
            }
        }
    }

    fun parseRowsToExpenses(rows: List<List<Any>>): List<Expense> {
        if (rows.isEmpty()) return emptyList()

        return rows.drop(1).mapNotNull { row ->
            try {
                if (row.size < 5) return@mapNotNull null

                val date = row[0].toString().trim()
                val description = row[1].toString().trim()
                val category = row[2].toString().trim()
                val amountStr = row[3].toString().trim().replace(",", "").replace("₹", "").replace("$", "").replace("€", "").replace("£", "")
                val paymentMethod = row[4].toString().trim()

                val amount = amountStr.toDoubleOrNull() ?: return@mapNotNull null

                Expense(
                    date = date,
                    description = description,
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    syncedToSheet = true
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun fullSync(expenseDao: ExpenseDao): SyncResult {
        if (!isConfigured()) return SyncResult.NotConfigured

        return withContext(Dispatchers.IO) {
            try {
                val sheetRows = fetchAllRows()
                if (sheetRows.isEmpty()) {
                    Log.w("GoogleSheetsService", "Full sync: sheet returned empty rows, aborting to protect local data")
                    return@withContext SyncResult.Error(IllegalStateException("Sheet fetch returned empty; skipping sync to protect local data"))
                }
                val sheetExpenses = parseRowsToSheetExpenses(sheetRows)
                val localExpenses = expenseDao.getAllSyncedAndUnsynced()

                Log.d("GoogleSheetsService", "Full sync: ${sheetExpenses.size} sheet, ${localExpenses.size} local")

                val sheetByRowId = sheetExpenses.associateBy { it.rowNumber }
                val localBySheetRowId = localExpenses.filter { it.sheetRowId != null }
                    .associateBy { it.sheetRowId!! }

                var pushed = 0
                var pulled = 0
                var deleted = 0

                val sheetNaturalKeys = sheetExpenses.map {
                    Triple(it.date, it.description, it.amount)
                }.toSet()

                for (local in localExpenses) {
                    val localKey = Triple(local.date, local.description, local.amount)

                    if (local.sheetRowId != null) {
                        val sheetExp = sheetByRowId[local.sheetRowId]
                        if (sheetExp == null) {
                            // Sheet is append-only; unlink local record so it can be re-pushed
                            expenseDao.update(local.copy(sheetRowId = null, syncedToSheet = false))
                        } else if (local.modifiedAt > sheetExp.modifiedAt) {
                            updateRow(local.sheetRowId, local)
                            expenseDao.markSynced(local.id, local.sheetRowId)
                            pushed++
                        } else if (sheetExp.modifiedAt > local.modifiedAt) {
                            expenseDao.update(local.copy(
                                date = sheetExp.date,
                                description = sheetExp.description,
                                category = sheetExp.category,
                                amount = sheetExp.amount,
                                paymentMethod = sheetExp.paymentMethod,
                                modifiedAt = sheetExp.modifiedAt,
                                syncedToSheet = true,
                                sheetRowId = sheetExp.rowNumber
                            ))
                            pulled++
                        }
                    } else if (localKey !in sheetNaturalKeys) {
                        val rowId = appendRow(local)
                        if (rowId != null) {
                            expenseDao.markSynced(local.id, rowId)
                            pushed++
                        }
                    }
                }

                for (sheetExp in sheetExpenses) {
                    if (sheetExp.rowNumber !in localBySheetRowId.keys) {
                        // Row exists in sheet but not locally → link by natural key if possible
                        val localMatch = expenseDao.findByNaturalKey(
                            sheetExp.date, sheetExp.description, sheetExp.amount
                        )
                        if (localMatch != null && localMatch.sheetRowId == null) {
                            expenseDao.update(localMatch.copy(
                                sheetRowId = sheetExp.rowNumber,
                                syncedToSheet = true
                            ))
                        }
                        // Sheet is append-only; never delete rows from it
                    }
                }

                SyncResult.Success(pushed = pushed, pulled = pulled, deleted = deleted)
            } catch (e: UserRecoverableAuthIOException) {
                SyncResult.ConsentRequired(e.intent)
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Full sync failed: ${e.message}", e)
                SyncResult.Error(e)
            }
        }
    }

    suspend fun pullFromSheet(expenseDao: ExpenseDao): SyncResult {
        if (!isConfigured()) return SyncResult.NotConfigured

        return withContext(Dispatchers.IO) {
            try {
                val sheetRows = fetchAllRows()
                val sheetExpenses = parseRowsToSheetExpenses(sheetRows)
                val localExpenses = expenseDao.getAllSyncedAndUnsynced()

                var pulled = 0

                val localBySheetRowId = localExpenses.filter { it.sheetRowId != null }
                    .associateBy { it.sheetRowId!! }

                for (sheetExp in sheetExpenses) {
                    if (sheetExp.rowNumber in localBySheetRowId.keys) {
                        // Already linked → update if sheet is newer
                        val local = localBySheetRowId[sheetExp.rowNumber]!!
                        if (sheetExp.modifiedAt > local.modifiedAt) {
                            expenseDao.update(local.copy(
                                date = sheetExp.date,
                                description = sheetExp.description,
                                category = sheetExp.category,
                                amount = sheetExp.amount,
                                paymentMethod = sheetExp.paymentMethod,
                                modifiedAt = sheetExp.modifiedAt,
                                syncedToSheet = true,
                                sheetRowId = sheetExp.rowNumber
                            ))
                            pulled++
                        }
                    } else {
                        // Not linked → check natural key or insert new
                        val localMatch = expenseDao.findByNaturalKey(
                            sheetExp.date, sheetExp.description, sheetExp.amount
                        )
                        if (localMatch != null) {
                            if (localMatch.sheetRowId == null) {
                                expenseDao.update(localMatch.copy(
                                    sheetRowId = sheetExp.rowNumber,
                                    syncedToSheet = true
                                ))
                            }
                        } else {
                            expenseDao.insert(Expense(
                                date = sheetExp.date,
                                description = sheetExp.description,
                                category = sheetExp.category,
                                amount = sheetExp.amount,
                                paymentMethod = sheetExp.paymentMethod,
                                modifiedAt = sheetExp.modifiedAt,
                                syncedToSheet = true,
                                sheetRowId = sheetExp.rowNumber
                            ))
                            pulled++
                        }
                    }
                }

                SyncResult.Success(pushed = 0, pulled = pulled, deleted = 0)
            } catch (e: UserRecoverableAuthIOException) {
                SyncResult.ConsentRequired(e.intent)
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Pull failed: ${e.message}", e)
                SyncResult.Error(e)
            }
        }
    }
}

sealed class SyncResult {
    data class Success(val pushed: Int, val pulled: Int, val deleted: Int) : SyncResult()
    data class ConsentRequired(val consentIntent: Intent) : SyncResult()
    data class Error(val exception: Exception) : SyncResult()
    data object NotConfigured : SyncResult()
}
