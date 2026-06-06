package com.articlevault.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("reading_prefs", Context.MODE_PRIVATE)

    var fontSizePercent: Int
        get() = prefs.getInt("font_size", 100)
        set(value) = prefs.edit().putInt("font_size", value.coerceIn(50, 200)).apply()
}
