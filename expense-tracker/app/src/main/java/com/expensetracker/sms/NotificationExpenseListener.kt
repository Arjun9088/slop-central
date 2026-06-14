package com.expensetracker.sms

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.WorkManager
import com.expensetracker.data.db.AppDatabase
import com.expensetracker.data.db.entity.Expense
import com.expensetracker.data.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens to notifications from SMS, banking and UPI apps and attempts to
 * auto-capture expenses. This is a reliable fallback when a third-party SMS
 * app (e.g. Truecaller) does not propagate the standard SMS_RECEIVED broadcast.
 */
@AndroidEntryPoint
class NotificationExpenseListener : NotificationListenerService() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var workManager: WorkManager

    private val processedKeys = LinkedHashSet<String>()
    private val maxCacheSize = 200

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        if (!isInterestingPackage(packageName)) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val fullText = buildString {
            append(title)
            if (bigText.isNotBlank()) {
                append(" ")
                append(bigText)
            }
            if (subText.isNotBlank() && subText != bigText) {
                append(" ")
                append(subText)
            }
        }

        if (fullText.isBlank()) return

        val cacheKey = "${sbn.key}|$fullText"
        synchronized(processedKeys) {
            if (processedKeys.contains(cacheKey)) return
            processedKeys.add(cacheKey)
            if (processedKeys.size > maxCacheSize) {
                processedKeys.firstOrNull()?.let { processedKeys.remove(it) }
            }
        }

        Log.d(TAG, "Notification from $packageName: $fullText")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                processNotificationText(fullText)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    private suspend fun processNotificationText(text: String) {
        val parsed = TransactionParser.parse("", text) ?: return

        val expenseDao = database.expenseDao()
        if (expenseDao.countByDedupHash(parsed.dedupHash) > 0) return

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
        Log.i(TAG, "Captured expense from notification: ${expense.description} ₹${expense.amount}")
        SyncWorker.enqueueOneTime(workManager)
    }

    private fun isInterestingPackage(packageName: String): Boolean {
        return knownPackages.any { packageName.contains(it, ignoreCase = true) } ||
            isSmsPackage(packageName)
    }

    private fun isSmsPackage(packageName: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val defaultSms = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
                packageName == defaultSms
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    companion object {
        private const val TAG = "NotificationExpenseListener"
        private const val DEDUP_TIME_WINDOW_MS = 60_000L

        /**
         * Package names or substrings for apps that commonly show transaction
         * notifications in India. We listen to these explicitly plus the
         * current default SMS app.
         */
        private val knownPackages = setOf(
            "truecaller",                       // SMS / Truecaller
            "google.android.apps.messaging",    // Google Messages
            "google.android.apps.nbu.paisa",    // Google Pay (GPay)
            "phonepe",                          // PhonePe
            "one97.paytm",                      // Paytm
            "in.org.npci.upiapp",               // BHIM
            "bharatpe",
            "amazonpay",
            "cred",
            "com.whatsapp",                     // WhatsApp payments notifications
            "com.sbi",
            "com.icicibank",
            "com.hdfcbank",
            "com.axis.mobile",
            "com.bankbazaar",
            "com.android.incallui"              // Some carriers show SMS here
        )

        fun isEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(packageName)
        }
    }
}
