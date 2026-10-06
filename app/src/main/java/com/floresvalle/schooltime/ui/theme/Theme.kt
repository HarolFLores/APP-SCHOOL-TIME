package com.floresvalle.schooltime.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DeepTealDark,
    onPrimary = Slate950,
    primaryContainer = DeepTealContainerDark,
    onPrimaryContainer = DeepTealOnContainerDark,
    secondary = MintDark,
    onSecondary = Slate950,
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = MintOnContainerDark,
    tertiary = AmberAlert,
    onTertiary = Slate950,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = CoralAlert,
    onError = Slate950,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFECDD3),
    background = Slate950,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Slate700,
    outlineVariant = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTealLight,
    onPrimary = White,
    primaryContainer = DeepTealContainerLight,
    onPrimaryContainer = DeepTealOnContainerLight,
    secondary = MintLight,
    onSecondary = White,
    secondaryContainer = MintContainerLight,
    onSecondaryContainer = MintOnContainerLight,
    tertiary = AmberAlert,
    onTertiary = White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF92400E),
    error = CoralAlert,
    onError = White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF991B1B),
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Slate200
)

@Composable
fun SchoolTimeTheme(
    darkTheme: Boolean = false, // Always default to LIGHT mode as requested (desactivado por predeterminado)
    dynamicColor: Boolean = false, // Enforce our custom Deep Teal palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
