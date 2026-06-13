package com.expensetracker.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.db.entity.Expense
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    val filtered = if (selectedCategory != null) {
        expenses.filter { it.category == selectedCategory }
    } else {
        expenses
    }

    val grouped = filtered.groupBy { it.date }
    val sortedDates = grouped.keys.sortedByDescending { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = onNavigateToReceipt) {
                        Icon(Icons.Default.Edit, contentDescription = "Scan Receipt")
                    }
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.Star, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search expenses...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching expenses" else "No expenses yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first expense",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (date in sortedDates) {
                        val dayExpenses = grouped[date] ?: emptyList()
                        val dayTotal = dayExpenses.sumOf { it.amount }
                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

                        item(key = "header-$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currencyFormat.format(dayTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            items = dayExpenses,
                            key = { it.id }
                        ) { expense ->
                            ExpenseRow(
                                expense = expense,
                                onClick = { onEditExpense(expense.id) },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }
        }
    }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Delete \"${expense.description}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expense)
                    expenseToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expense.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expense.source == "sms") {
                        Text(
                            text = " • SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
