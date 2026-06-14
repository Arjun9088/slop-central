package com.expensetracker.data.sync

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(name: String): ThemeMode = when (name.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun getSpreadsheetId(): String? = prefs.getString("spreadsheet_id", null)

    fun setSpreadsheetId(id: String) {
        prefs.edit().putString("spreadsheet_id", id).apply()
    }

    fun getSheetName(): String? = prefs.getString("sheet_name", null)

    fun setSheetName(name: String) {
        prefs.edit().putString("sheet_name", name).apply()
    }

    fun getGoogleAccountEmail(): String? = prefs.getString("google_account", null)

    fun setGoogleAccountEmail(email: String) {
        prefs.edit().putString("google_account", email).apply()
    }

    fun isSyncEnabled(): Boolean = prefs.getBoolean("sync_enabled", true)

    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sync_enabled", enabled).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong("last_sync_time", 0)

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
    }

    fun getThemeMode(): ThemeMode =
        ThemeMode.fromName(prefs.getString("theme_mode", "system") ?: "system")

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}
