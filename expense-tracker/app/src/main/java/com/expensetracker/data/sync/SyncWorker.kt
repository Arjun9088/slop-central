package com.expensetracker.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.data.db.dao.ExpenseDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val sheetsService: GoogleSheetsService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "expense_sync"
        const val KEY_FORCE = "force"

        fun enqueuePeriodic(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTime(workManager: WorkManager, force: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putBoolean(KEY_FORCE, force)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueueUniqueWork(
                "${WORK_NAME}_onetime",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        if (!sheetsService.isConfigured()) {
            Log.d(TAG, "Sheets not configured, skipping sync")
            return Result.success()
        }

        return try {
            val result = sheetsService.fullSync(expenseDao)
            when (result) {
                is SyncResult.Success -> {
                    Log.d(TAG, "Sync complete: pushed=${result.pushed}, pulled=${result.pulled}, deleted=${result.deleted}")
                    Result.success()
                }
                is SyncResult.Error -> {
                    Log.e(TAG, "Sync failed: ${result.exception.message}", result.exception)
                    Result.retry()
                }
                is SyncResult.NotConfigured -> {
                    Log.d(TAG, "Not configured, skipping")
                    Result.success()
                }
                is SyncResult.ConsentRequired -> {
                    Log.w(TAG, "Consent required, retrying later")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
