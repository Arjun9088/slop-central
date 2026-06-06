package com.articlevault.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.BackupManager
import com.articlevault.data.ThemePreferences
import com.articlevault.data.model.ModelRepository
import com.articlevault.ml.LlmSummarizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val error: String? = null
)

@HiltViewModel
class ModelSelectionViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val llmSummarizer: LlmSummarizer,
    private val backupManager: BackupManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun isDarkMode(): Boolean = themePreferences.isDarkMode

    // API settings
    fun getApiEndpoint(): String? = modelRepository.getApiEndpoint()
    fun setApiEndpoint(ep: String) { modelRepository.setApiEndpoint(ep) }
    fun getApiKey(): String? = modelRepository.getApiKey()
    fun setApiKey(key: String) { modelRepository.setApiKey(key) }
    fun getApiModel(): String = modelRepository.getApiModel()
    fun setApiModel(model: String) { modelRepository.setApiModel(model) }
    fun isApiConfigured(): Boolean = modelRepository.isApiConfigured()

    // Backup
    fun backup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            val result = backupManager.export()
            if (result.success) {
                onResult(result.path ?: "Backup completed.")
            } else {
                _state.value = _state.value.copy(error = result.error)
            }
        }
    }

    fun restore(zipFile: File, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            val result = backupManager.import(zipFile)
            onComplete(result.success)
            if (!result.success) {
                _state.value = _state.value.copy(error = result.error)
            }
        }
    }
}
