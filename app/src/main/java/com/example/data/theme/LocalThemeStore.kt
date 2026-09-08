package com.example.data.theme

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MobileAppTheme
import org.json.JSONObject

/**
 * Local storage manager for caching remote themes using SharedPreferences.
 * Provides offline fallback capability so the app launches instantly with
 * the most recently synced theme or the default palette.
 */
class LocalThemeStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveTheme(theme: MobileAppTheme) {
        try {
            val jsonString = theme.toJson().toString()
            prefs.edit().putString(KEY_CACHED_THEME, jsonString).apply()
        } catch (_: Exception) {
        }
    }

    fun getCachedTheme(): MobileAppTheme? {
        val jsonString = prefs.getString(KEY_CACHED_THEME, null) ?: return null
        return try {
            MobileAppTheme.fromJson(JSONObject(jsonString))
        } catch (_: Exception) {
            null
        }
    }

    fun clearCache() {
        prefs.edit().remove(KEY_CACHED_THEME).apply()
    }

    companion object {
        private const val PREFS_NAME = "homease_theme_prefs"
        private const val KEY_CACHED_THEME = "cached_active_theme_json"
    }
}
