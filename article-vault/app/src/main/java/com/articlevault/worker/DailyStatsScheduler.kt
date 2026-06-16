package com.articlevault.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.articlevault.data.NotificationPreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyStatsScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val preferences: NotificationPreferences
) {
    companion object {
        const val WORK_NAME = "daily_stats_work"
    }

    fun scheduleIfEnabled() {
        if (preferences.enabled) {
            schedule()
        } else {
            cancel()
        }
    }

    fun schedule() {
        cancel()
        val delay = computeDelay(preferences.hour, preferences.minute)
        val request = PeriodicWorkRequestBuilder<DailyStatsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun computeDelay(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - now
    }
}
