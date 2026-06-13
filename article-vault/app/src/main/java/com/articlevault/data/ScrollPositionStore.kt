package com.articlevault.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrollPositionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scroll_positions", Context.MODE_PRIVATE)

    fun save(articleId: String, scrollY: Int) {
        prefs.edit().putInt(articleId, scrollY).apply()
    }

    fun get(articleId: String): Int {
        return prefs.getInt(articleId, 0)
    }

    fun clear(articleId: String) {
        prefs.edit().remove(articleId).apply()
    }
}
