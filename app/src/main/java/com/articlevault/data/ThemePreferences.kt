package com.articlevault.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    val isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)

    fun toggleDarkMode() {
        prefs.edit().putBoolean("dark_mode", !isDarkMode).apply()
    }
}
