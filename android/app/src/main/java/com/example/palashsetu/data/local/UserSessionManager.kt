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
    private const val KEY_LANGUAGE = "app_language" // "hi" or "en"

    // Onboarding walkthrough persistence
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_ONBOARDING_LAST_STEP = "onboarding_last_step"

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

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "hi") ?: "hi"
    }

    fun saveLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getTeacherDisplayName(context: Context, language: String = getLanguage(context)): String {
        val profile = getProfile(context)
        val isHindi = language == "hi"
        val normalizedSal = when {
            profile.salutation.contains("श्रीमती") || profile.salutation.equals("Ma'am", ignoreCase = true) -> if (isHindi) "श्रीमती" else "Ma'am"
            profile.salutation.contains("शिक्षक") || profile.salutation.equals("Teacher", ignoreCase = true) -> if (isHindi) "शिक्षक" else "Teacher"
            else -> if (isHindi) "श्री" else "Sir"
        }
        return if (profile.name.isNotBlank()) {
            "$normalizedSal ${profile.name}"
        } else {
            if (isHindi) "शिक्षक" else "Teacher"
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

    // ── Onboarding walkthrough ────────────────────────────────────────────────

    /**
     * Returns true if the teacher has already completed (or explicitly dismissed)
     * the first-time onboarding walkthrough.
     */
    fun hasCompletedOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    /**
     * Marks the walkthrough as fully completed. After this, [hasCompletedOnboarding]
     * returns true and the overlay will never auto-trigger again (replay is manual).
     */
    fun markOnboardingComplete(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .putInt(KEY_ONBOARDING_LAST_STEP, 0)
            .apply()
    }

    /**
     * Persists the current step index so that a mid-walkthrough power-off
     * resumes from the last viewed step on next launch.
     */
    fun saveOnboardingProgress(context: Context, stepIndex: Int) {
        getPrefs(context).edit().putInt(KEY_ONBOARDING_LAST_STEP, stepIndex).apply()
    }

    /**
     * Returns the step index to resume from (0 if never started or after completion).
     */
    fun getOnboardingLastStep(context: Context): Int {
        return getPrefs(context).getInt(KEY_ONBOARDING_LAST_STEP, 0)
    }
}
