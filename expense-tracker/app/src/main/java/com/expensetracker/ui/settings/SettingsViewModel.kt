package com.expensetracker.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.expensetracker.data.db.dao.ExpenseDao
import com.expensetracker.data.sync.GoogleSheetsService
import com.expensetracker.data.sync.SheetsResult
import com.expensetracker.data.sync.SpreadsheetInfo
import com.expensetracker.data.sync.SyncPreferences
import com.expensetracker.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val googleAccount: String? = null,
    val spreadsheetId: String? = null,
    val spreadsheetName: String? = null,
    val sheetName: String? = null,
    val syncEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val sheetsServiceReady: Boolean = false,
    val spreadsheets: List<SpreadsheetInfo> = emptyList(),
    val sheetTabs: List<String> = emptyList(),
    val isLoadingSpreadsheets: Boolean = false,
    val isLoadingTabs: Boolean = false,
    val showSheetPicker: Boolean = false,
    val showTabPicker: Boolean = false,
    val consentIntent: Intent? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncPreferences: SyncPreferences,
    private val workManager: WorkManager,
    private val sheetsService: GoogleSheetsService,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        restoreSheetsService()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            googleAccount = syncPreferences.getGoogleAccountEmail(),
            spreadsheetId = syncPreferences.getSpreadsheetId(),
            spreadsheetName = syncPreferences.getSpreadsheetId(),
            sheetName = syncPreferences.getSheetName(),
            syncEnabled = syncPreferences.isSyncEnabled(),
            lastSyncTime = syncPreferences.getLastSyncTime()
        )
    }

    private fun restoreSheetsService() {
        val email = syncPreferences.getGoogleAccountEmail() ?: return
        try {
            sheetsService.buildServiceForAccount(email)
            _uiState.update { it.copy(sheetsServiceReady = true) }
        } catch (_: Exception) {
        }
    }

    fun setGoogleAccount(email: String) {
        syncPreferences.setGoogleAccountEmail(email)
        sheetsService.buildServiceForAccount(email)
        _uiState.update { it.copy(googleAccount = email, sheetsServiceReady = true) }
        fetchSpreadsheets()
    }

    fun fetchSpreadsheets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSpreadsheets = true) }
            when (val result = sheetsService.listSpreadsheets()) {
                is SheetsResult.Success -> {
                    _uiState.update {
                        it.copy(
                            spreadsheets = result.data,
                            isLoadingSpreadsheets = false,
                            showSheetPicker = true
                        )
                    }
                }
                is SheetsResult.ConsentRequired -> {
                    _uiState.update {
                        it.copy(
                            isLoadingSpreadsheets = false,
                            consentIntent = result.consentIntent
                        )
                    }
                }
                is SheetsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingSpreadsheets = false,
                            importMessage = "Failed to load sheets: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }

    fun selectSpreadsheet(sheet: SpreadsheetInfo) {
        syncPreferences.setSpreadsheetId(sheet.id)
        _uiState.update {
            it.copy(
                spreadsheetId = sheet.id,
                spreadsheetName = sheet.name,
                showSheetPicker = false,
                sheetName = null
            )
        }
        fetchSheetTabs()
    }

    fun fetchSheetTabs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTabs = true) }
            when (val result = sheetsService.listSheetTabs()) {
                is SheetsResult.Success -> {
                    _uiState.update {
                        it.copy(
                            sheetTabs = result.data,
                            isLoadingTabs = false,
                            showTabPicker = true
                        )
                    }
                }
                is SheetsResult.ConsentRequired -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTabs = false,
                            consentIntent = result.consentIntent
                        )
                    }
                }
                is SheetsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTabs = false,
                            importMessage = "Failed to load tabs: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tabName: String) {
        syncPreferences.setSheetName(tabName)
        _uiState.update {
            it.copy(
                sheetName = tabName,
                showTabPicker = false
            )
        }
    }

    fun onConsentGranted() {
        _uiState.update { it.copy(consentIntent = null) }
        fetchSpreadsheets()
    }

    fun onConsentDenied() {
        _uiState.update { it.copy(consentIntent = null) }
    }

    fun dismissSheetPicker() {
        _uiState.update { it.copy(showSheetPicker = false) }
    }

    fun dismissTabPicker() {
        _uiState.update { it.copy(showTabPicker = false) }
    }

    fun setSyncEnabled(enabled: Boolean) {
        syncPreferences.setSyncEnabled(enabled)
        _uiState.update { it.copy(syncEnabled = enabled) }
        if (enabled) {
            SyncWorker.enqueuePeriodic(workManager)
        } else {
            workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        }
    }

    fun triggerSync() {
        SyncWorker.enqueueOneTime(workManager, force = false)
    }

    fun forceSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importMessage = null) }
            val result = sheetsService.fullSync(expenseDao)
            when (result) {
                is com.expensetracker.data.sync.SyncResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importMessage = "Sync done: ${result.pushed} pushed, ${result.pulled} pulled, ${result.deleted} deleted"
                        )
                    }
                }
                is com.expensetracker.data.sync.SyncResult.Error -> {
                    _uiState.update {
                        it.copy(isImporting = false, importMessage = "Sync failed: ${result.exception.message}")
                    }
                }
                is com.expensetracker.data.sync.SyncResult.NotConfigured -> {
                    _uiState.update {
                        it.copy(isImporting = false, importMessage = "Select a spreadsheet and tab first")
                    }
                }
                is com.expensetracker.data.sync.SyncResult.ConsentRequired -> {
                    _uiState.update {
                        it.copy(isImporting = false, consentIntent = result.consentIntent)
                    }
                }
            }
        }
    }

    fun importFromSheet() {
        forceSync()
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }
}
