package com.expensetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.expensetracker.data.sync.GoogleSheetsService
import com.expensetracker.data.sync.SyncPreferences
import com.expensetracker.data.sync.SyncWorker
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sheetsService: GoogleSheetsService

    @Inject
    lateinit var syncPreferences: SyncPreferences

    @Inject
    lateinit var workManager: WorkManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            restoreSheetsService()
            SyncWorker.enqueuePeriodic(workManager)
        }
    }

    private fun restoreSheetsService() {
        val email = syncPreferences.getGoogleAccountEmail() ?: return
        try {
            sheetsService.buildServiceForAccount(email)
        } catch (_: Exception) {
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
