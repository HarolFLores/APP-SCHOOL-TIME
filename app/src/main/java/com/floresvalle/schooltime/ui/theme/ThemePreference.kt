package com.floresvalle.schooltime.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object ThemePreference {
    private const val PREFS_NAME = "schooltime_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_mode" // "LIGHT", "DARK"

    // Always default to LIGHT mode as requested (desactivado por predeterminado)
    val isDarkModeState = mutableStateOf("LIGHT")

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Explicitly fallback to "LIGHT" so it does not auto-detect system dark theme
        isDarkModeState.value = prefs.getString(KEY_DARK_MODE, "LIGHT") ?: "LIGHT"
    }

    fun isDark(): Boolean {
        return isDarkModeState.value == "DARK"
    }

    fun setDarkMode(context: Context, mode: String) {
        val normalized = if (mode == "DARK") "DARK" else "LIGHT"
        isDarkModeState.value = normalized
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DARK_MODE, normalized).apply()
    }

    fun toggleDarkMode(context: Context): Boolean {
        val newMode = if (isDark()) "LIGHT" else "DARK"
        setDarkMode(context, newMode)
        return newMode == "DARK"
    }
}
