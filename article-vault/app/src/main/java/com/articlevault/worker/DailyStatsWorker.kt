package com.articlevault.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.articlevault.data.NotificationPreferences
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.DomainClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltWorker
class DailyStatsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
    private val prefs: NotificationPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!prefs.enabled) return Result.success()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return Result.success()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        val savedToday = repository.countSavedToday(startOfDay, endOfDay)
        val readToday = repository.countReadToday(startOfDay, endOfDay)
        val wordsReadToday = repository.totalWordsReadToday(startOfDay, endOfDay)
        val wordsUnreadToday = repository.totalWordsUnreadToday(startOfDay, endOfDay)
        val unreadCount = repository.countUnreadToday(startOfDay, endOfDay)
        val topDomains = repository.getTopDomainsToday(startOfDay, endOfDay)
        val readTimestamps = repository.getReadTimestamps()
        val streak = computeStreak(readTimestamps)

        val readingTimeMin = DomainClassifier.extractReadingTimeMinutes(wordsUnreadToday)
        val topSource = topDomains.firstOrNull()?.domain ?: "\u2014"

        showNotification(savedToday, readToday, wordsReadToday, unreadCount, readingTimeMin, streak, topSource)
        return Result.success()
    }

    private fun showNotification(
        saved: Int, read: Int, wordsRead: Int, unread: Int,
        timeMin: Int, streak: Int, topSource: String
    ) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val lines = mutableListOf<String>()

        if (saved == 0 && read == 0) {
            lines.add("No activity today \u2014 keep your streak going!")
        } else {
            lines.add("Saved today:        $saved articles")
            lines.add("Read today:         $read articles")
            lines.add("Words read:         $wordsRead")
            lines.add("Unread today:       $unread articles")
            lines.add("Unread reading time:  ~$timeMin min")
            lines.add("Current streak:     $streak days")
            lines.add("Top source:         $topSource")
        }

        val inboxStyle = NotificationCompat.InboxStyle()
        for (line in lines) {
            inboxStyle.addLine(line)
        }

        val notification = NotificationCompat.Builder(applicationContext, "daily_stats")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Daily Reading Summary")
            .setContentText(lines.first())
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }

    private fun computeStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val readDates = timestamps.map { sdf.format(Date(it)) }.distinct().sorted()
        if (readDates.isEmpty()) return 0
        val today = sdf.format(Date())
        val yesterday = sdf.format(Date(System.currentTimeMillis() - 86_400_000))
        val mostRecent = readDates.last()
        if (mostRecent != today && mostRecent != yesterday) return 0
        val dateSet = readDates.toSet()
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(mostRecent)!!
        var streak = 0
        while (true) {
            if (dateSet.contains(sdf.format(cal.time))) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }
}
