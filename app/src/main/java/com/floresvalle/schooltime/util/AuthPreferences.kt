package com.floresvalle.schooltime.util

import android.content.Context
import android.content.SharedPreferences

object AuthPreferences {
    private const val PREFS_NAME = "schooltime_secure_auth_prefs"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_USER_ID = "saved_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LAST_LOGIN_TIME = "last_login_time"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun setRememberMe(context: Context, remember: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
    }

    fun shouldRememberMe(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_ME, true)
    }

    fun saveUserSession(context: Context, userId: String, email: String, rememberMe: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_SAVED_USER_ID, userId)
            if (rememberMe) {
                putString(KEY_SAVED_EMAIL, email)
            } else {
                remove(KEY_SAVED_EMAIL)
            }
            putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun getSavedEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_SAVED_EMAIL, null)
    }

    fun getLastUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_SAVED_USER_ID, null)
    }

    fun isSessionActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession(context: Context) {
        val prefs = getPrefs(context)
        val remember = prefs.getBoolean(KEY_REMEMBER_ME, true)
        val savedEmail = if (remember) prefs.getString(KEY_SAVED_EMAIL, null) else null

        prefs.edit().apply {
            clear()
            putBoolean(KEY_REMEMBER_ME, remember)
            if (savedEmail != null) {
                putString(KEY_SAVED_EMAIL, savedEmail)
            }
            apply()
        }
    }
}
