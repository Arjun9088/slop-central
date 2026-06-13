package com.articlevault.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.db.dao.DomainCount
import com.articlevault.data.repository.ArticleRepository
import com.articlevault.ml.DomainClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class StatsState(
    val totalArticles: Int = 0,
    val totalRead: Int = 0,
    val totalWords: Int = 0,
    val totalWordsRead: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val topDomains: List<DomainCount> = emptyList(),
    val domainTypeDistribution: Map<DomainClassifier.SiteType, Int> = emptyMap(),
    val readingActivity: Map<String, Int> = emptyMap(),
    val articlesPerDay: Map<String, Int> = emptyMap(),
    val avgReadingTimeMinutes: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val totalArticles = repository.countArticles()
            val totalRead = repository.countRead()
            val totalWords = repository.totalWordCount()
            val totalWordsRead = repository.totalWordsRead()
            val topDomains = repository.getTopDomains(10)
            val readTimestamps = repository.getReadTimestamps()
            val savedTimestamps = repository.getAllSavedTimestamps()

            val streaks = computeStreaks(readTimestamps)
            val readingActivity = computeActivity(readTimestamps, 30)
            val articlesPerDay = computeActivity(savedTimestamps, 30)
            val domainTypes = computeDomainTypes(repository)
            val avgReadingTime = if (totalArticles > 0) DomainClassifier.extractReadingTimeMinutes(totalWords / totalArticles) else 0

            _state.value = StatsState(
                totalArticles = totalArticles,
                totalRead = totalRead,
                totalWords = totalWords,
                totalWordsRead = totalWordsRead,
                currentStreak = streaks.first,
                longestStreak = streaks.second,
                topDomains = topDomains,
                domainTypeDistribution = domainTypes,
                readingActivity = readingActivity,
                articlesPerDay = articlesPerDay,
                avgReadingTimeMinutes = avgReadingTime
            )
        }
    }

    private suspend fun computeDomainTypes(repository: ArticleRepository): Map<DomainClassifier.SiteType, Int> {
        val articles = repository.getAllRead()
        val allArticles = repository.observeAllArticles()
        // Use top domains and classify them
        val topDomains = repository.getTopDomains(50)
        val typeCounts = mutableMapOf<DomainClassifier.SiteType, Int>()
        for (dc in topDomains) {
            val type = DomainClassifier.classify("https://${dc.domain}")
            typeCounts[type] = (typeCounts[type] ?: 0) + dc.count
        }
        return typeCounts.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    private fun computeStreaks(timestamps: List<Long>): Pair<Int, Int> {
        if (timestamps.isEmpty()) return 0 to 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val readDates = timestamps
            .map { sdf.format(Date(it)) }
            .distinct()
            .sorted()

        if (readDates.isEmpty()) return 0 to 0

        val today = sdf.format(Date())
        val yesterday = sdf.format(Date(System.currentTimeMillis() - 86400000))

        // Current streak: consecutive days ending today or yesterday
        var currentStreak = 0
        val calendar = Calendar.getInstance()

        // Start from the most recent date
        val mostRecent = readDates.last()
        if (mostRecent != today && mostRecent != yesterday) {
            // No streak if most recent read was not today or yesterday
            return 0 to computeLongestStreak(readDates)
        }

        calendar.time = sdf.parse(mostRecent)!!
        val dateSet = readDates.toSet()

        while (true) {
            val dateStr = sdf.format(calendar.time)
            if (dateSet.contains(dateStr)) {
                currentStreak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return currentStreak to maxOf(currentStreak, computeLongestStreak(readDates))
    }

    private fun computeLongestStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var longest = 1
        var current = 1

        for (i in 1 until dates.size) {
            val prev = sdf.parse(dates[i - 1])!!.time
            val curr = sdf.parse(dates[i])!!.time
            val diffDays = ((curr - prev) / 86400000).toInt()
            if (diffDays == 1) {
                current++
                longest = maxOf(longest, current)
            } else if (diffDays > 1) {
                current = 1
            }
        }
        return longest
    }

    private fun computeActivity(timestamps: List<Long>, days: Int): Map<String, Int> {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days + 1)

        val dayCounts = mutableMapOf<String, Int>()
        repeat(days) {
            dayCounts[sdf.format(cal.time)] = 0
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (ts in timestamps) {
            val dateStr = dateSdf.format(Date(ts))
            val displayDate = sdf.format(dateSdf.parse(dateStr)!!)
            dayCounts[displayDate] = (dayCounts[displayDate] ?: 0) + 1
        }

        return dayCounts
    }
}
