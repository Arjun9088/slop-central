package com.articlevault.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.articlevault.ml.DomainClassifier

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Reading Stats",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Overview cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    label = "Saved",
                    value = state.totalArticles.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Done,
                    label = "Read",
                    value = state.totalRead.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Info,
                    label = "Avg Read",
                    value = "${state.avgReadingTimeMinutes}m",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Words stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Info,
                    label = "Words Saved",
                    value = formatNumber(state.totalWords),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Done,
                    label = "Words Read",
                    value = formatNumber(state.totalWordsRead),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Streak section
        item {
            StreakCard(
                currentStreak = state.currentStreak,
                longestStreak = state.longestStreak
            )
        }

        // Top domains
        if (state.topDomains.isNotEmpty()) {
            item {
                Text(
                    "Top Sources",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                DomainChart(domains = state.topDomains)
            }
        }

        // Domain type distribution
        if (state.domainTypeDistribution.isNotEmpty()) {
            item {
                Text(
                    "Content Mix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                DomainTypeChart(types = state.domainTypeDistribution)
            }
        }

        // Reading activity (last 30 days)
        if (state.readingActivity.isNotEmpty()) {
            item {
                Text(
                    "Articles Read (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                ActivityChart(data = state.readingActivity)
            }
        }

        // Articles saved activity
        if (state.articlesPerDay.isNotEmpty()) {
            item {
                Text(
                    "Articles Saved (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                ActivityChart(data = state.articlesPerDay)
            }
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakCard(currentStreak: Int, longestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currentStreak",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Current Streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$longestStreak",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "Best Streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (currentStreak > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (currentStreak == 1) "1 day in a row! Keep going!"
                    else "$currentStreak days in a row! Keep it up!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DomainChart(domains: List<com.articlevault.data.db.dao.DomainCount>) {
    val maxCount = domains.firstOrNull()?.count ?: 1
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            domains.take(8).forEach { dc ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dc.domain,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(100.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        ) {
                            drawRoundRect(
                                color = surfaceVariant,
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = primaryColor,
                                size = Size(
                                    size.width * (dc.count.toFloat() / maxCount),
                                    size.height
                                ),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        dc.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DomainTypeChart(types: Map<DomainClassifier.SiteType, Int>) {
    val total = types.values.sum().toFloat()
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Horizontal stacked bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                var xOffset = 0f
                types.entries.forEachIndexed { index, (_, count) ->
                    val width = size.width * (count / total)
                    drawRect(
                        color = colors[index % colors.size],
                        topLeft = Offset(xOffset, 0f),
                        size = Size(width, size.height)
                    )
                    xOffset += width
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Legend
            types.entries.forEachIndexed { index, (type, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        type.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$count (${(count * 100 / total).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityChart(data: Map<String, Int>) {
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val entries = data.entries.toList()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { (label, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(22.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width(16.dp)
                                .height(80.dp)
                        ) {
                            // Background
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                            // Bar
                            val barHeight = size.height * (count.toFloat() / maxVal)
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                entries.forEach { (label, _) ->
                    Text(
                        label.takeLast(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.Center,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                    )
                }
            }
        }
    }
}

private fun formatNumber(n: Int): String {
    return when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK".format(n / 1_000.0)
        else -> n.toString()
    }
}
