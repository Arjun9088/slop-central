package com.articlevault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.articlevault.worker.DailyStatsScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArticleVaultApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dailyStatsScheduler: DailyStatsScheduler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        dailyStatsScheduler.scheduleIfEnabled()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                "article_saved",
                "Article saves",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications when articles finish saving"
            }
            nm.createNotificationChannel(channel)

            val dailyChannel = NotificationChannel(
                "daily_stats",
                "Daily reading stats",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of your reading activity"
            }
            nm.createNotificationChannel(dailyChannel)
        }
    }
}
