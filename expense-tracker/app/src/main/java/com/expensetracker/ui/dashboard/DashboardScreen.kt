package com.expensetracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.db.dao.CategoryTotal
import com.expensetracker.data.db.dao.MonthlyTotal
import com.expensetracker.data.db.dao.PaymentMethodTotal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Today",
                            amount = currencyFormat.format(state.todayTotal),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "This Month",
                            amount = currencyFormat.format(state.monthlyTotal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Total Expenses",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.totalCount}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (state.monthlyTrend.isNotEmpty()) {
                    item {
                        Text(
                            text = "Monthly Trend",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    item {
                        TrendChart(
                            data = state.monthlyTrend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                if (state.categoryBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = "By Category",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    val categoryMax = state.categoryBreakdown.maxOf { it.total }
                    items(state.categoryBreakdown) { item ->
                        CategoryRow(item, categoryMax, currencyFormat)
                    }
                }

                if (state.paymentBreakdown.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "By Payment Method",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    val paymentMax = state.paymentBreakdown.maxOf { it.total }
                    items(state.paymentBreakdown) { item ->
                        PaymentRow(item, paymentMax, currencyFormat)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TrendChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val maxAmount = data.maxOf { it.total }
            val minAmount = data.minOf { it.total }
            val range = (maxAmount - minAmount).coerceAtLeast(1.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (data.size - 1).coerceAtLeast(1)

                val points = data.mapIndexed { index, item ->
                    val x = index * stepX
                    val y = height - ((item.total - minAmount) / range * height).toFloat()
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val midX = (prev.x + curr.x) / 2
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(path, primaryColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }

                for (point in points) {
                    drawCircle(primaryColor, radius = 4f, center = point)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { item ->
                    Text(
                        text = item.month.takeLast(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CategoryRow(
    item: CategoryTotal,
    maxValue: Double,
    currencyFormat: NumberFormat
) {
    val fraction = if (maxValue > 0) (item.total / maxValue).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currencyFormat.format(item.total),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun PaymentRow(
    item: PaymentMethodTotal,
    maxValue: Double,
    currencyFormat: NumberFormat
) {
    val fraction = if (maxValue > 0) (item.total / maxValue).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.paymentMethod,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currencyFormat.format(item.total),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
