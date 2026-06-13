package com.expensetracker.ui.receipt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.data.db.entity.defaultCategories
import com.expensetracker.data.db.entity.defaultPaymentMethods
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceiptScreen(
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && uiState.imageUri != null) {
            viewModel.processImage(uiState.imageUri!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onBack()
                    }) {
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

            OutlinedButton(
                onClick = {
                    val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    viewModel.resetState()
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Take Receipt Photo")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scanning receipt...")
                    }
                }
            }

            if (uiState.imageUri != null && !uiState.isProcessing) {
                Text(
                    text = "Extracted Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.updateAmount(it) },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    prefix = { Text("₹") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    defaultCategories.forEach { cat ->
                        FilterChip(
                            selected = uiState.category == cat,
                            onClick = { viewModel.updateCategory(cat) },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Payment Method", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    defaultPaymentMethods.forEach { method ->
                        FilterChip(
                            selected = uiState.paymentMethod == method,
                            onClick = { viewModel.updatePaymentMethod(method) },
                            label = { Text(method) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    Text(if (uiState.isSaving) "Saving..." else "Save Expense")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.recognizedText.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "OCR Text",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                uiState.recognizedText.take(500),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
