package com.expensetracker.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.db.entity.ExpenseCategory
import com.expensetracker.data.db.entity.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    expenseId: Long = 0L,
    onBack: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        if (expenseId > 0) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.resetState()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Expense" else "Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.date,
                onValueChange = { viewModel.updateDate(it) },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExpenseCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.category == category,
                        onClick = { viewModel.updateCategory(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("₹") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = uiState.paymentMethod == method,
                        onClick = { viewModel.updatePaymentMethod(method) },
                        label = { Text(method.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(
                    text = if (uiState.isSaving) "Saving..." else "Save Expense",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(uiState.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        viewModel.updateDate(date)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(Unit) {
        if (expenseId == 0L) {
            focusRequester.requestFocus()
        }
    }
}
