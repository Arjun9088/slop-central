package com.expensetracker.ui.settings

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.expensetracker.sms.NotificationExpenseListener
import com.expensetracker.data.sync.ThemeMode
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra("authAccount")
            if (accountName != null) {
                viewModel.setGoogleAccount(accountName)
            }
        }
    }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onConsentGranted()
        } else {
            viewModel.onConsentDenied()
        }
    }

    LaunchedEffect(uiState.consentIntent) {
        uiState.consentIntent?.let { intent ->
            consentLauncher.launch(intent)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationAccessStatus()
    }

    if (uiState.showSheetPicker) {
        SpreadsheetPickerDialog(
            spreadsheets = uiState.spreadsheets,
            isLoading = uiState.isLoadingSpreadsheets,
            onSelect = { viewModel.selectSpreadsheet(it) },
            onRefresh = { viewModel.fetchSpreadsheets() },
            onDismiss = { viewModel.dismissSheetPicker() }
        )
    }

    if (uiState.showTabPicker) {
        TabPickerDialog(
            tabs = uiState.sheetTabs,
            isLoading = uiState.isLoadingTabs,
            onSelect = { viewModel.selectTab(it) },
            onRefresh = { viewModel.fetchSheetTabs() },
            onDismiss = { viewModel.dismissTabPicker() }
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Google Sheets Sync",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.googleAccount != null) {
                            "Connected: ${uiState.googleAccount}"
                        } else {
                            "Not connected"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val credential = GoogleAccountCredential.usingOAuth2(
                                context,
                                listOf(SheetsScopes.SPREADSHEETS)
                            )
                            accountPickerLauncher.launch(credential.newChooseAccountIntent())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.googleAccount != null) {
                                "Switch Account"
                            } else {
                                "Connect Google Account"
                            }
                        )
                    }
                }
            }

            if (uiState.googleAccount != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spreadsheet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { viewModel.fetchSpreadsheets() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        if (uiState.spreadsheetName != null) {
                            Text(
                                text = uiState.spreadsheetName!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "None selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.fetchSpreadsheets() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoadingSpreadsheets
                        ) {
                            if (uiState.isLoadingSpreadsheets) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(text = if (uiState.isLoadingSpreadsheets) "Loading..." else "Choose Spreadsheet")
                        }

                        if (uiState.spreadsheetId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sheet Tab",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { viewModel.fetchSheetTabs() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh tabs")
                                }
                            }

                            if (uiState.sheetName != null) {
                                Text(
                                    text = uiState.sheetName!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "None selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { viewModel.fetchSheetTabs() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoadingTabs
                            ) {
                                if (uiState.isLoadingTabs) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(end = 8.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Text(text = if (uiState.isLoadingTabs) "Loading..." else "Choose Tab")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sync",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-sync (every 15 min)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.syncEnabled,
                    onCheckedChange = { viewModel.setSyncEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.forceSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.spreadsheetId != null &&
                    uiState.sheetName != null &&
                    !uiState.isImporting
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(text = if (uiState.isImporting) "Syncing..." else "Full Sync (Two-way)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.pullFromSheet() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.spreadsheetId != null &&
                    uiState.sheetName != null &&
                    !uiState.isImporting
            ) {
                Text("Pull from Sheet Only")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.triggerSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.syncEnabled &&
                    uiState.spreadsheetId != null &&
                    uiState.sheetName != null
            ) {
                Text("Push Unsynced Only")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SMS Auto-Capture",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Automatically capture expenses from bank transaction SMS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val context = LocalContext.current
            val smsPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                coroutineScope.launch {
                    if (granted) {
                        snackbarHostState.showSnackbar("SMS permission granted. Auto-capture enabled.")
                    } else {
                        snackbarHostState.showSnackbar("SMS permission denied. Auto-capture disabled.")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val hasPermission = context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("SMS permission already granted")
                            }
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant SMS Permission")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Notification Capture",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Capture expenses from transaction notifications (recommended if you use Truecaller, GPay, PhonePe, etc.)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.notificationAccessEnabled) {
                            "Notification access enabled"
                        } else {
                            "Notification access disabled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.notificationAccessEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.notificationAccessEnabled) {
                                "Manage Notification Access"
                            } else {
                                "Enable Notification Access"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            ThemeModeOption(
                label = "Follow system",
                selected = uiState.themeMode == ThemeMode.SYSTEM,
                onClick = {
                    viewModel.setThemeMode(ThemeMode.SYSTEM)
                    onThemeChanged(ThemeMode.SYSTEM)
                }
            )
            ThemeModeOption(
                label = "Light",
                selected = uiState.themeMode == ThemeMode.LIGHT,
                onClick = {
                    viewModel.setThemeMode(ThemeMode.LIGHT)
                    onThemeChanged(ThemeMode.LIGHT)
                }
            )
            ThemeModeOption(
                label = "Dark",
                selected = uiState.themeMode == ThemeMode.DARK,
                onClick = {
                    viewModel.setThemeMode(ThemeMode.DARK)
                    onThemeChanged(ThemeMode.DARK)
                }
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SpreadsheetPickerDialog(
    spreadsheets: List<com.expensetracker.data.sync.SpreadsheetInfo>,
    isLoading: Boolean,
    onSelect: (com.expensetracker.data.sync.SpreadsheetInfo) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose a Spreadsheet")
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading your sheets...")
                }
            } else if (spreadsheets.isEmpty()) {
                Text("No spreadsheets found in your Google Drive.")
            } else {
                LazyColumn {
                    items(spreadsheets) { sheet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(sheet) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sheet.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TabPickerDialog(
    tabs: List<String>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose a Tab")
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading tabs...")
                }
            } else if (tabs.isEmpty()) {
                Text("No tabs found in this spreadsheet.")
            } else {
                LazyColumn {
                    items(tabs) { tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(tab) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
