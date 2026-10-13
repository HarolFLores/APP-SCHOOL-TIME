package com.floresvalle.schooltime.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object ThemePreference {
    private const val PREFS_NAME = "schooltime_theme_prefs"
    private const val KEY_THEME = "app_theme_mode"
    private var prefs: SharedPreferences? = null
    private val _themeState = mutableStateOf("SYSTEM")

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeState.value = prefs?.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun isDarkModeState(): State<String> = _themeState

    fun setThemeMode(mode: String) {
        _themeState.value = mode
        prefs?.edit()?.putString(KEY_THEME, mode)?.apply()
    }
}
