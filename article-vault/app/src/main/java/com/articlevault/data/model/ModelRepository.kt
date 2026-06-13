package com.articlevault.data.model

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

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
}
