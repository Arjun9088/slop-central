package com.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.sync.SyncWorker
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString("") { it.messageBody }
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processSms(sender, fullBody)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processSms(sender: String, body: String) {
        val parsed = TransactionParser.parse(sender, body) ?: return

        val expenseDao = database.expenseDao()

        val existing = expenseDao.countByDedupHash(parsed.dedupHash)
        if (existing > 0) return

        val timeWindow = System.currentTimeMillis() - DEDUP_TIME_WINDOW_MS
        if (expenseDao.countByAmountAndTimeWindow(parsed.amount, timeWindow) > 0) return

        val category = CategoryClassifier.classify(parsed.merchant)

        val expense = Expense(
            date = parsed.date,
            description = parsed.rawDescription,
            category = category,
            amount = parsed.amount,
            paymentMethod = parsed.paymentMethod,
            source = "sms",
            dedupHash = parsed.dedupHash,
            syncedToSheet = false
        )

        expenseDao.insert(expense)
        SyncWorker.enqueueOneTime(workManager)
    }

    companion object {
        private const val DEDUP_TIME_WINDOW_MS = 60_000L
    }
}
