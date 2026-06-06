package com.articlevault.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    onBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    viewModel: ModelSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showApiDialog by remember { mutableStateOf(false) }
    var apiEndpointInput by remember { mutableStateOf(viewModel.getApiEndpoint() ?: "") }
    var apiKeyInput by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var apiModelInput by remember { mutableStateOf(viewModel.getApiModel()) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupResultMsg by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activityResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = java.io.File(context.cacheDir, "restore_temp.zip")
            inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
            viewModel.restore(tempFile) { success ->
                backupResultMsg = if (success) "Restore successful. Restart the app." else "Restore failed."
            }
        }
    }

    // API Settings dialog
    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = { Text("API Settings") },
            text = {
                Column {
                    Text("Works with OpenAI, Ollama, LiteLLM, vLLM, or any OpenAI-compatible endpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiEndpointInput,
                        onValueChange = { apiEndpointInput = it },
                        label = { Text("API Endpoint") },
                        placeholder = { Text("https://api.openai.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiModelInput,
                        onValueChange = { apiModelInput = it },
                        label = { Text("Model") },
                        placeholder = { Text("gpt-4o-mini") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setApiEndpoint(apiEndpointInput)
                    viewModel.setApiKey(apiKeyInput)
                    viewModel.setApiModel(apiModelInput)
                    showApiDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Backup dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = {
                Column {
                    Text("Backup saves your database and articles to a ZIP file in Downloads. Restore loads a previously saved backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (backupResultMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(backupResultMsg!!, style = MaterialTheme.typography.bodySmall,
                            color = if (backupResultMsg!!.contains("successful"))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.backup { backupResultMsg = "Backed up to Downloads" }
                        }) { Text("Export") }
                        OutlinedButton(onClick = {
                            activityResult.launch("application/zip")
                        }) { Text("Import") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error
            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        state.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // API Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showApiDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Summarization API", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (viewModel.isApiConfigured()) {
                            "Endpoint: ${viewModel.getApiEndpoint()}\nModel: ${viewModel.getApiModel()}"
                        } else {
                            "Not configured — tap to set up"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (viewModel.isApiConfigured()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Backup & Restore
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showBackupDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Export/import database and articles as ZIP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dark Mode
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "White text on black background",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.isDarkMode(),
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            }
        }
    }
}
