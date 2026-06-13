package com.expensetracker.ui.receipt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.db.entity.defaultCategories
import com.expensetracker.data.db.entity.defaultPaymentMethods
import com.expensetracker.data.sync.SyncWorker
import com.expensetracker.sms.CategoryClassifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReceiptUiState(
    val imageUri: Uri? = null,
    val recognizedText: String = "",
    val amount: String = "",
    val description: String = "",
    val category: String = defaultCategories.first(),
    val paymentMethod: String = defaultPaymentMethods.first(),
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val result = recognizer.process(image).await()
                val fullText = result.text

                _uiState.update { it.copy(recognizedText = fullText) }

                val parsed = parseReceiptText(fullText)
                _uiState.update {
                    it.copy(
                        amount = parsed.amount,
                        description = parsed.description,
                        category = parsed.category,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "OCR failed: ${e.message}")
                }
            }
        }
    }

    private data class ParsedReceipt(
        val amount: String,
        val description: String,
        val category: String
    )

    private fun parseReceiptText(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var amount = ""
        var merchant = ""

        val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹|total|amount|grand\s*total)\s*:?\s*(?:₹|rs\.?)?\s*([\d,]+(?:\.\d{1,2})?)""")
        val amountOnlyRegex = Regex("""(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)""")

        for (line in lines.reversed()) {
            val match = amountRegex.find(line) ?: amountOnlyRegex.find(line)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "")
                break
            }
        }

        if (amount.isBlank()) {
            val allAmounts = Regex("""([\d,]+\.\d{2})""").findAll(text)
                .map { it.groupValues[1].replace(",", "") }
                .mapNotNull { it.toDoubleOrNull() }
                .toList()
            if (allAmounts.isNotEmpty()) {
                amount = allAmounts.max().toString()
            }
        }

        for (line in lines.take(5)) {
            val clean = line.replace(Regex("""[^A-Za-z0-9\s&.'_/-]"""), "").trim()
            if (clean.length in 3..40 && clean.any { it.isLetter() }) {
                merchant = clean
                break
            }
        }

        val description = merchant.ifBlank { "Receipt" }
        val category = CategoryClassifier.classify("$description $text")

        return ParsedReceipt(
            amount = amount,
            description = description,
            category = category
        )
    }

    fun updateAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun updateCategory(cat: String) {
        _uiState.update { it.copy(category = cat) }
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
                val expense = Expense(
                    date = state.date,
                    description = state.description.trim(),
                    category = state.category,
                    amount = amountValue,
                    paymentMethod = state.paymentMethod,
                    source = "receipt",
                    syncedToSheet = false
                )
                expenseDao.insert(expense)
                SyncWorker.enqueueOneTime(workManager)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}") }
            }
        }
    }

    fun resetState() {
        _uiState.value = ReceiptUiState()
    }
}
