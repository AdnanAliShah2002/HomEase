package com.example.util

import android.content.Context
import android.content.SharedPreferences

data class UserSession(
    val phone: String,
    val role: String, // "CUSTOMER" or "PROVIDER"
    val loggedInAt: Long = System.currentTimeMillis()
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "homease_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_PHONE = "session_phone"
        private const val KEY_ROLE = "session_role"
        private const val KEY_LOGGED_IN_AT = "logged_in_at"
    }

    fun saveSession(phone: String, role: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_PHONE, phone)
            .putString(KEY_ROLE, role)
            .putLong(KEY_LOGGED_IN_AT, System.currentTimeMillis())
            .apply()
    }

    fun getSession(): UserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val phone = prefs.getString(KEY_PHONE, null)
        val role = prefs.getString(KEY_ROLE, null)
        val loggedInAt = prefs.getLong(KEY_LOGGED_IN_AT, 0L)

        return if (isLoggedIn && !phone.isNullOrBlank() && !role.isNullOrBlank()) {
            UserSession(phone = phone, role = role, loggedInAt = loggedInAt)
        } else {
            null
        }
    }

    fun hasActiveSession(): Boolean {
        return getSession() != null
    }

    fun clearSession() {
        prefs.edit()
            .clear()
            .apply()
    }
}
