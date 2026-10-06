package com.floresvalle.schooltime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.floresvalle.schooltime.data.AppDatabase
import com.floresvalle.schooltime.ui.SchoolTimeApp
import com.floresvalle.schooltime.ui.theme.SchoolTimeTheme
import com.floresvalle.schooltime.ui.theme.ThemePreference
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.viewmodel.SessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: SessionViewModel by viewModels()
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_cleanup", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("room_wiped_v12", false)) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    AppDatabase.clearAllData(applicationContext)
                    AuthPreferences.clearSession(applicationContext)
                    Firebase.auth.signOut()
                    prefs.edit().putBoolean("room_wiped_v12", true).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ThemePreference.init(this)

        setContent {
            val darkModeSetting = ThemePreference.isDarkModeState.value
            val isDark = (darkModeSetting == "DARK")

            SchoolTimeTheme(darkTheme = isDark) {
                SchoolTimeApp(viewModel = viewModel)
            }
        }
    }
}
