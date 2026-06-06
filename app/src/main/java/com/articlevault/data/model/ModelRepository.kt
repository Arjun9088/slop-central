package com.articlevault.data.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelsDir = File(context.getExternalFilesDir(null), "models").apply { mkdirs() }
    private val prefs: SharedPreferences =
        context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Supported model file extensions (in order of preference)
    // MediaPipe LlmInference needs .task or .tflite, NOT .litertlm
    private val supportedExtensions = listOf(".task", ".tflite")

    // Hardcoded fallback models for when HuggingFace is unreachable
    // Must use .task format (multi-prefill-seq) — .tflite is NOT compatible with MediaPipe LlmInference
    private val fallbackModels = listOf(
        ModelInfo(
            id = "litert-community/SmolLM-135M-Instruct",
            name = "SmolLM 135M Instruct",
            description = "HuggingFace's tiny 135M model. Extremely fast, basic quality. Apache-2.0 license.",
            sizeBytes = 350_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            filename = "SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            parameterCount = "135M",
            gated = false
        ),
        ModelInfo(
            id = "litert-community/Qwen2.5-0.5B-Instruct",
            name = "Qwen 2.5 0.5B Instruct",
            description = "Alibaba's 0.5B model. Small and capable. Apache-2.0 license.",
            sizeBytes = 900_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            filename = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            parameterCount = "0.5B",
            gated = false
        ),
        ModelInfo(
            id = "litert-community/Gemma3-1B-IT",
            name = "Gemma 3 1B IT",
            description = "Google's 1B instruction-tuned model. Gated — requires accepting license on HuggingFace first. May OOM on devices with <4GB free RAM.",
            sizeBytes = 700_000_000L,
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv1280.task",
            filename = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv1280.task",
            parameterCount = "1B",
            gated = true
        )
    )

    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://huggingface.co/api/models?author=litert-community&sort=downloads&direction=-1")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("ModelRepository", "HF API returned ${response.code}, using fallback")
                return@withContext fallbackModels
            }

            val body = response.body?.string() ?: return@withContext fallbackModels
            val jsonArray = JSONArray(body)

            val models = mutableListOf<ModelInfo>()
            for (i in 0 until jsonArray.length().coerceAtMost(20)) {
                val obj = jsonArray.getJSONObject(i)
                val modelId = obj.getString("id")
                val modelName = modelId.substringAfter("/")
                val isGated = obj.optString("gated", "false").let {
                    it == "auto" || it == "true"
                }

                // Find the best supported file in siblings
                // Must be .task format (multi-prefill-seq) — .tflite is NOT compatible
                val siblings = obj.optJSONArray("siblings") ?: continue
                var bestFile: String? = null
                var fileSize = 0L

                for (j in 0 until siblings.length()) {
                    val sibling = siblings.getJSONObject(j)
                    val filename = sibling.getString("filename")
                    if (filename.endsWith(".task") && filename.contains("multi-prefill-seq")) {
                        // Prefer quantized (q4 or q8) files
                        val isQuantized = filename.contains("_q4") || filename.contains("_q8")
                        if (bestFile == null || isQuantized) {
                            bestFile = filename
                            fileSize = sibling.optLong("size", 0L)
                            if (isQuantized) break
                        }
                    }
                }

                if (bestFile != null) {
                    models.add(
                        ModelInfo(
                            id = modelId,
                            name = modelName.replace("-", " "),
                            description = obj.optString("description", "LiteRT compatible model").take(200),
                            sizeBytes = fileSize,
                            downloadUrl = "https://huggingface.co/$modelId/resolve/main/$bestFile",
                            filename = bestFile,
                            parameterCount = extractParamCount(modelName),
                            gated = isGated
                        )
                    )
                }
            }

            if (models.isEmpty()) fallbackModels else models
        } catch (e: Exception) {
            Log.w("ModelRepository", "Failed to fetch models from HF: ${e.message}")
            fallbackModels
        }
    }

    fun getDownloadedModels(): List<ModelInfo> {
        return modelsDir.listFiles()
            ?.filter { file -> supportedExtensions.any { file.name.endsWith(it) } }
            ?.map { file ->
                val modelId = prefs.getString("model_${file.name}_id", file.nameWithoutExtension) ?: ""
                val modelName = prefs.getString("model_${file.name}_name", file.nameWithoutExtension) ?: ""
                ModelInfo(
                    id = modelId,
                    name = modelName,
                    description = "Downloaded model",
                    sizeBytes = file.length(),
                    downloadUrl = "",
                    filename = file.name
                )
            }
            ?: emptyList()
    }

    fun getSelectedModelPath(): String? {
        val filename = prefs.getString("selected_model", null) ?: return null
        val file = File(modelsDir, filename)
        return if (file.exists()) file.absolutePath else null
    }

    fun selectModel(filename: String) {
        prefs.edit().putString("selected_model", filename).apply()
    }

    fun isModelDownloaded(filename: String): Boolean {
        return File(modelsDir, filename).exists()
    }

    fun getModelFile(filename: String): File {
        return File(modelsDir, filename)
    }

    fun deleteModel(filename: String) {
        File(modelsDir, filename).delete()
        if (prefs.getString("selected_model", null) == filename) {
            prefs.edit().remove("selected_model").apply()
        }
    }

    fun saveModelMetadata(model: ModelInfo) {
        prefs.edit()
            .putString("model_${model.filename}_id", model.id)
            .putString("model_${model.filename}_name", model.name)
            .apply()
    }

    fun getHfToken(): String? {
        val token = prefs.getString("hf_token", null)
        return if (token.isNullOrBlank()) null else token
    }

    fun setHfToken(token: String) {
        prefs.edit().putString("hf_token", token.trim()).apply()
    }

    fun clearHfToken() {
        prefs.edit().remove("hf_token").apply()
    }

    // OpenAI-compatible API settings
    fun getApiEndpoint(): String? {
        val ep = prefs.getString("api_endpoint", null)
        return if (ep.isNullOrBlank()) null else ep.trimEnd('/')
    }

    fun setApiEndpoint(endpoint: String) {
        prefs.edit().putString("api_endpoint", endpoint.trim()).apply()
    }

    fun getApiKey(): String? {
        val key = prefs.getString("api_key", null)
        return if (key.isNullOrBlank()) null else key
    }

    fun setApiKey(key: String) {
        prefs.edit().putString("api_key", key.trim()).apply()
    }

    fun getApiModel(): String {
        return prefs.getString("api_model", "gpt-4o-mini") ?: "gpt-4o-mini"
    }

    fun setApiModel(model: String) {
        prefs.edit().putString("api_model", model.trim()).apply()
    }

    fun isApiConfigured(): Boolean {
        return getApiEndpoint() != null && getApiKey() != null
    }

    fun getSummarizationMode(): String {
        // "local" or "api"
        return prefs.getString("summarization_mode", "local") ?: "local"
    }

    fun setSummarizationMode(mode: String) {
        prefs.edit().putString("summarization_mode", mode).apply()
    }

    private fun extractParamCount(name: String): String {
        val regex = Regex("(\\d+\\.?\\d*[BM])", RegexOption.IGNORE_CASE)
        return regex.find(name)?.value ?: ""
    }
}
