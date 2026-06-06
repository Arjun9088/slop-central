package com.articlevault.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.articlevault.data.BackupManager
import com.articlevault.data.model.ModelInfo
import com.articlevault.data.model.ModelRepository
import com.articlevault.ml.LlmSummarizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ModelUiState(
    val models: List<ModelInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val downloadingModel: String? = null,
    val downloadProgress: Float = 0f,
    val selectedModelFilename: String? = null,
    val summarizationMode: String = "local"
)

@HiltViewModel
class ModelSelectionViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val llmSummarizer: LlmSummarizer,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(ModelUiState())
    val state: StateFlow<ModelUiState> = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    init {
        loadModels()
        _state.value = _state.value.copy(
            summarizationMode = modelRepository.getSummarizationMode()
        )
    }

    fun loadModels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val models = modelRepository.getAvailableModels()
                _state.value = _state.value.copy(
                    models = models,
                    isLoading = false,
                    selectedModelFilename = modelRepository.getSelectedModelPath()
                        ?.let { File(it).name }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Could not load models. Check your internet connection."
                )
            }
        }
    }

    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                downloadingModel = model.id,
                downloadProgress = 0f,
                error = null
            )

            try {
                val destFile = modelRepository.getModelFile(model.filename)

                withContext(Dispatchers.IO) {
                    val requestBuilder = Request.Builder()
                        .url(model.downloadUrl)

                    val token = modelRepository.getHfToken()
                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }

                    val response = client.newCall(requestBuilder.build()).execute()
                    if (!response.isSuccessful) {
                        if (response.code == 401 || response.code == 404) {
                            throw Exception("Authentication required. Set your HuggingFace token.")
                        }
                        throw Exception("Download failed: HTTP ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()

                    body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead

                                if (totalBytes > 0) {
                                    _state.value = _state.value.copy(
                                        downloadProgress = totalRead.toFloat() / totalBytes
                                    )
                                }
                            }
                        }
                    }
                }

                modelRepository.saveModelMetadata(model)
                modelRepository.selectModel(model.filename)
                llmSummarizer.closeEngine()

                _state.value = _state.value.copy(
                    downloadingModel = null,
                    downloadProgress = 0f,
                    selectedModelFilename = model.filename
                )
            } catch (e: Exception) {
                modelRepository.getModelFile(model.filename).delete()
                _state.value = _state.value.copy(
                    downloadingModel = null,
                    downloadProgress = 0f,
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun selectModel(filename: String) {
        modelRepository.selectModel(filename)
        llmSummarizer.closeEngine()
        _state.value = _state.value.copy(selectedModelFilename = filename)
    }

    fun deleteModel(filename: String) {
        llmSummarizer.closeEngine()
        modelRepository.deleteModel(filename)
        _state.value = _state.value.copy(
            selectedModelFilename = modelRepository.getSelectedModelPath()?.let { File(it).name }
        )
        loadModels()
    }

    fun isDownloaded(filename: String): Boolean = modelRepository.isModelDownloaded(filename)

    // HF token
    fun getHfToken(): String? = modelRepository.getHfToken()
    fun setHfToken(token: String) { modelRepository.setHfToken(token) }
    fun clearHfToken() { modelRepository.clearHfToken() }

    // Summarization mode
    fun setSummarizationMode(mode: String) {
        modelRepository.setSummarizationMode(mode)
        _state.value = _state.value.copy(summarizationMode = mode)
    }

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

    fun restore(zipFile: java.io.File, onComplete: (Boolean) -> Unit) {
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
