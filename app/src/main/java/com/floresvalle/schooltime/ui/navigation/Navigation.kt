package com.floresvalle.schooltime.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("Home", "Inicio", Icons.Default.Home)
    object Calendar : Screen("Calendar", "Calendario", Icons.Default.CalendarToday)
    object Tasks : Screen("Tasks", "Evaluaciones", Icons.Default.Task)
    object Profile : Screen("Profile", "Perfil", Icons.Default.Person)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Calendar,
    Screen.Tasks,
    Screen.Profile
)
