package com.floresvalle.schooltime.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Calendar : Screen("calendar", "Calendario", Icons.Default.CalendarToday)
    object Tasks : Screen("tasks", "Evaluaciones", Icons.Default.Task)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
}
