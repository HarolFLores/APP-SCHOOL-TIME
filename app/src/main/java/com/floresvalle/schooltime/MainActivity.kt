package com.floresvalle.schooltime

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.floresvalle.schooltime.receiver.NotificationChannels
import com.floresvalle.schooltime.ui.SchoolTimeApp
import com.floresvalle.schooltime.ui.theme.SchoolTimeTheme
import com.floresvalle.schooltime.ui.theme.ThemePreference
import com.floresvalle.schooltime.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SessionViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationChannels.createChannels(this)

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != 0
        ) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        ThemePreference.init(this)

        setContent {
            val darkModeSetting = ThemePreference.isDarkModeState().value
            val isDark = darkModeSetting == "DARK"
            SchoolTimeTheme(darkTheme = isDark) {
                SchoolTimeApp(viewModel = viewModel)
            }
        }
    }
}
