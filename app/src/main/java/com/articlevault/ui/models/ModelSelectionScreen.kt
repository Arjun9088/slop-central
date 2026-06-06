package com.articlevault.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.articlevault.data.model.ModelInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    onBack: () -> Unit,
    viewModel: ModelSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var modelToDelete by remember { mutableStateOf<ModelInfo?>(null) }

    // HF token dialog
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(viewModel.getHfToken() ?: "") }

    // API settings dialog
    var showApiDialog by remember { mutableStateOf(false) }
    var apiEndpointInput by remember { mutableStateOf(viewModel.getApiEndpoint() ?: "") }
    var apiKeyInput by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var apiModelInput by remember { mutableStateOf(viewModel.getApiModel()) }

    // Delete confirmation
    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete model?") },
            text = { Text("\"${model.name}\" will be removed from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(model.filename)
                    modelToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // HF Token dialog
    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("HuggingFace Token") },
            text = {
                Column {
                    Text("Required for gated models. Get yours at huggingface.co/settings/tokens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Access Token") },
                        placeholder = { Text("hf_...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tokenInput.isBlank()) viewModel.clearHfToken()
                    else viewModel.setHfToken(tokenInput)
                    showTokenDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // API Settings dialog
    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = { Text("OpenAI-Compatible API") },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Backup/restore
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupResultMsg by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activityResult = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
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
                            viewModel.backup { path ->
                                backupResultMsg = "Backed up to Downloads"
                            }
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

    // === Summarization mode toggle ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Summarization source", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.summarizationMode == "local",
                            onClick = { viewModel.setSummarizationMode("local") },
                            label = { Text("Local model") }
                        )
                        FilterChip(
                            selected = state.summarizationMode == "api",
                            onClick = { viewModel.setSummarizationMode("api") },
                            label = { Text("API") }
                        )
                    }
                    if (state.summarizationMode == "api") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (viewModel.isApiConfigured()) "Endpoint: ${viewModel.getApiEndpoint()}\nModel: ${viewModel.getApiModel()}"
                            else "Not configured — tap below to set up",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewModel.isApiConfigured()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { showApiDialog = true }) {
                            Text(if (viewModel.isApiConfigured()) "Edit API settings" else "Configure API")
                        }
                    }
                }
            }

            // === HF Token section ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { showTokenDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HuggingFace Token", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (viewModel.getHfToken() != null) "Set (tap to change)" else "Not set — required for gated models",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewModel.getHfToken() != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (viewModel.getHfToken() != null) {
                        IconButton(onClick = {
                            viewModel.clearHfToken()
                            tokenInput = ""
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // === Backup section ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { showBackupDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Backup & Restore", style = MaterialTheme.typography.titleSmall)
                        Text("Export/import database and articles as ZIP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // === Download progress ===
            if (state.downloadingModel != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Downloading model...", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${(state.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // === Error ===
            if (state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(state.error!!, modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // === Model list ===
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.models, key = { it.id }) { model ->
                        val isDownloaded = viewModel.isDownloaded(model.filename)
                        val isSelected = state.selectedModelFilename == model.filename

                        ModelCard(
                            model = model,
                            isDownloaded = isDownloaded,
                            isSelected = isSelected,
                            isDownloading = state.downloadingModel == model.id,
                            downloadProgress = if (state.downloadingModel == model.id) state.downloadProgress else 0f,
                            onDownload = { viewModel.downloadModel(model) },
                            onSelect = { viewModel.selectModel(model.filename) },
                            onDelete = { modelToDelete = model }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (model.parameterCount.isNotBlank()) {
                        Text("${model.parameterCount} params • ${model.sizeDisplay}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (model.gated) {
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(onClick = {}, label = { Text("Gated — requires HF token") },
                    modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(model.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isDownloaded) {
                    Button(onClick = onDownload, enabled = !isDownloading,
                        modifier = Modifier.weight(1f)) { Text("Download") }
                } else {
                    OutlinedButton(onClick = onSelect, enabled = !isSelected,
                        modifier = Modifier.weight(1f)) { Text(if (isSelected) "Selected" else "Use this model") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
