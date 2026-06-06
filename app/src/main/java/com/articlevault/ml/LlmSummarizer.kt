package com.articlevault.ml

import android.content.Context
import android.util.Log
import com.articlevault.data.model.ModelRepository
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmSummarizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository
) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LlmSummarizer"
        private const val SYSTEM_PROMPT = """You are a precise article summarizer. Create a comprehensive summary as bullet points preserving all key information.

Rules:
- Output ONLY bullet points (start each line with •)
- Capture ALL main points, arguments, and conclusions
- Preserve specific facts: names, numbers, dates, locations, technical details
- Each bullet should be a single, complete thought
- Do NOT omit important context or nuance
- Do NOT add information not present in the original
- Do NOT include any preamble or introductory text
- Aim for 5-10 bullets depending on article length"""
    }

    suspend fun summarize(text: String): String = withContext(Dispatchers.Default) {
        val mode = modelRepository.getSummarizationMode()

        if (mode == "api") {
            summarizeViaApi(text)
        } else {
            summarizeLocal(text)
        }
    }

    private suspend fun summarizeViaApi(text: String): String {
        val endpoint = modelRepository.getApiEndpoint()
            ?: throw IllegalStateException("API endpoint not configured. Set it in Models → API Settings.")
        val apiKey = modelRepository.getApiKey()
            ?: throw IllegalStateException("API key not configured. Set it in Models → API Settings.")
        val model = modelRepository.getApiModel()

        val maxChars = 8000
        val truncated = if (text.length > maxChars) text.take(maxChars) + "..." else text

        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            put(JSONObject().put("role", "user").put("content", "Summarize this article:\n\n$truncated"))
        }

        val requestBody = JSONObject()
            .put("model", model)
            .put("messages", messagesArray)
            .put("max_tokens", 1024)
            .put("temperature", 0.7)
            .toString()

        val url = "$endpoint/v1/chat/completions"
        Log.d(TAG, "Calling API: $url with model $model")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            val errorMsg = try {
                JSONObject(responseBody ?: "").optJSONObject("error")?.optString("message")
            } catch (_: Exception) { null }
            throw Exception("API error ${response.code}: ${errorMsg ?: responseBody ?: "Unknown error"}")
        }

        val json = JSONObject(responseBody ?: throw Exception("Empty response"))
        val content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        Log.d(TAG, "API summary received: ${content.length} chars")
        return content.trim()
    }

    private suspend fun summarizeLocal(text: String): String {
        val modelPath = modelRepository.getSelectedModelPath()
            ?: throw IllegalStateException("No AI model downloaded. Go to the Models tab to download one, or switch to API mode.")

        try {
            ensureEngine(modelPath)

            val maxChars = 3000
            val truncated = if (text.length > maxChars) text.take(maxChars) + "..." else text
            val prompt = "$SYSTEM_PROMPT\n\nSummarize this article:\n\n$truncated"

            Log.d(TAG, "Generating summary with local model: $modelPath")

            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTopP(0.95f)
                .setTemperature(0.7f)
                .build()

            val session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
            session.addQueryChunk(prompt)
            val result = session.generateResponse()
            session.close()

            Log.d(TAG, "Local summary generated: ${result.length} chars")
            return result.trim()
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory loading model", e)
            closeEngine()
            throw IllegalStateException("Not enough memory for this model. Try a smaller model or use API mode.")
        } catch (e: Exception) {
            Log.e(TAG, "Local summarization failed: ${e.message}", e)
            throw e
        }
    }

    fun hasModel(): Boolean = modelRepository.getSelectedModelPath() != null
    fun isApiConfigured(): Boolean = modelRepository.isApiConfigured()
    fun getSummarizationMode(): String = modelRepository.getSummarizationMode()

    fun getSelectedModelName(): String? {
        val mode = modelRepository.getSummarizationMode()
        if (mode == "api") {
            return "API: ${modelRepository.getApiModel()}"
        }
        val path = modelRepository.getSelectedModelPath() ?: return null
        return path.substringAfterLast("/").substringBeforeLast(".")
    }

    private fun ensureEngine(modelPath: String) {
        if (llmInference != null && currentModelPath == modelPath) return
        closeEngine()

        Log.d(TAG, "Loading model: $modelPath (this may take a minute)")
        try {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "Engine loaded with CPU backend")
        } catch (e: OutOfMemoryError) {
            throw IllegalStateException("Not enough memory to load this model. Try a smaller model or use API mode.")
        }
        currentModelPath = modelPath
    }

    fun closeEngine() {
        llmInference?.close()
        llmInference = null
        currentModelPath = null
    }
}
