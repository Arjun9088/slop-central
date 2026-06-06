package com.articlevault.ml

import android.util.Log
import com.articlevault.data.model.ModelRepository
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
    private val modelRepository: ModelRepository
) {
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
        val endpoint = modelRepository.getApiEndpoint()
            ?: throw IllegalStateException("API endpoint not configured. Set it in Settings → API Settings.")
        val apiKey = modelRepository.getApiKey()
            ?: throw IllegalStateException("API key not configured. Set it in Settings → API Settings.")
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
        return@withContext content.trim()
    }

    fun isApiConfigured(): Boolean = modelRepository.isApiConfigured()

    fun getSelectedModelName(): String {
        val model = modelRepository.getApiModel()
        val endpoint = modelRepository.getApiEndpoint() ?: "API"
        return "$model via $endpoint"
    }
}
