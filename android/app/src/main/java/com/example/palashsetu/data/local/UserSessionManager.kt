package com.example.palashsetu.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.palashsetu.data.model.TeacherProfile

object UserSessionManager {
    private const val PREFS_NAME = "palash_setu_user_session"
    private const val KEY_SALUTATION = "teacher_salutation"
    private const val KEY_NAME = "teacher_name"
    private const val KEY_PIN = "teacher_pin"
    private const val KEY_CONFIGURED = "is_configured"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getProfile(context: Context): TeacherProfile {
        val prefs = getPrefs(context)
        val salutation = prefs.getString(KEY_SALUTATION, "श्री") ?: "श्री"
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val pin = prefs.getString(KEY_PIN, "2604") ?: "2604"
        val isConfigured = prefs.getBoolean(KEY_CONFIGURED, false)
        return TeacherProfile(salutation, name, pin, isConfigured)
    }

    fun saveProfile(context: Context, salutation: String, name: String, pin: String) {
        getPrefs(context).edit()
            .putString(KEY_SALUTATION, salutation)
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PIN, pin)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    fun getTeacherDisplayName(context: Context): String {
        val profile = getProfile(context)
        return if (profile.name.isNotBlank()) {
            "${profile.salutation} ${profile.name}"
        } else {
            "शिक्षक (Teacher)"
        }
    }

    fun getTeacherAvatar(context: Context): String {
        return getProfile(context).avatarEmoji
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val profile = getProfile(context)
        // If not yet configured, 2604 or any 4 digits work; if configured, compare with saved PIN or 2604 demo fallback
        return enteredPin == profile.pin || enteredPin == "2604"
    }

    fun resetSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
