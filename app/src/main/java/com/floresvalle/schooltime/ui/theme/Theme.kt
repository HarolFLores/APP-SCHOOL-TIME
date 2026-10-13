package com.floresvalle.schooltime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DeepTealDark,
    onPrimary = Slate900,
    primaryContainer = DeepTealContainerDark,
    onPrimaryContainer = DeepTealOnContainerDark,
    secondary = MintDark,
    onSecondary = Slate900,
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = MintOnContainerDark,
    tertiary = AmberAlert,
    background = Slate950,
    onBackground = Slate50,
    surface = SurfaceDark,
    onSurface = Slate50,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Slate200,
    error = CoralAlert,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight
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
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    error = CoralAlert,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorContainerDark
)

@Composable
fun SchoolTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
