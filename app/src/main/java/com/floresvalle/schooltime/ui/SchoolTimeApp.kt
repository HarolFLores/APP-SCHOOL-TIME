package com.floresvalle.schooltime.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.floresvalle.schooltime.ui.components.AddContentBottomSheet
import com.floresvalle.schooltime.ui.components.NotificationsBottomSheet
import com.floresvalle.schooltime.ui.components.QuickProfileDialog
import com.floresvalle.schooltime.ui.navigation.BottomNavItems
import com.floresvalle.schooltime.ui.navigation.Screen
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.util.AuthValidation
import com.floresvalle.schooltime.viewmodel.AuthViewModel
import com.floresvalle.schooltime.viewmodel.RegisterViewModel
import com.floresvalle.schooltime.viewmodel.SessionViewModel

@Composable
fun SchoolTimeApp(viewModel: SessionViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val registerViewModel: RegisterViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = BottomNavItems.any { it.route == currentRoute }
    val showFab = showBottomBar && currentRoute != Screen.Profile.route

    var showBottomSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showQuickProfileDialog by remember { mutableStateOf(false) }

    val userData by viewModel.currentUserData.collectAsState()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SchoolTimeBottomBar(navController = navController)
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        }
    ) { innerPadding ->
        val startRoute = when {
            authViewModel.isUserLoggedIn -> Screen.Home.route
            !AuthPreferences.isOnboardingCompleted(context) -> "onboarding"
            else -> "auth"
        }

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate("auth") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("auth") {
                AuthScreen(
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        viewModel.updateCurrentUserId()
                        viewModel.refreshData()
                        navController.navigate(Screen.Home.route) {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        registerViewModel.clearGoogleFlow()
                        navController.navigate("register")
                    },
                    onNavigateToGoogleOnboarding = { email, name, photo, uid ->
                        registerViewModel.setGoogleFlow(email, name, photo, uid)
                        val encodedEmail = Uri.encode(email)
                        val encodedName = Uri.encode(name)
                        navController.navigate("register?email=$encodedEmail&name=$encodedName")
                    },
                    onNavigateToOnboarding = {
                        navController.navigate("onboarding")
                    }
                )
            }
            composable(
                route = "register?email={email}&name={name}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val googleEmail = AuthValidation.parseOptionalNavArg(backStackEntry.arguments?.getString("email"))
                val googleName = AuthValidation.parseOptionalNavArg(backStackEntry.arguments?.getString("name"))

                RegisterScreen(
                    viewModel = registerViewModel,
                    googleEmail = googleEmail,
                    googleName = googleName,
                    onNavigateBack = {
                        registerViewModel.cancelRegistration()
                        registerViewModel.clearGoogleFlow()
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        authViewModel.isUserLoggedIn = true
                        viewModel.updateCurrentUserId()
                        viewModel.refreshData()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("auth") {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onOpenNotifications = { showNotificationsSheet = true },
                    onOpenQuickProfile = { showQuickProfileDialog = true }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    viewModel = viewModel,
                    onOpenNotifications = { showNotificationsSheet = true },
                    onOpenQuickProfile = { showQuickProfileDialog = true }
                )
            }
            composable(Screen.Tasks.route) {
                TasksExamsScreen(
                    viewModel = viewModel,
                    onOpenNotifications = { showNotificationsSheet = true },
                    onOpenQuickProfile = { showQuickProfileDialog = true }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("auth") {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onOpenNotifications = { showNotificationsSheet = true },
                    onOpenQuickProfile = { showQuickProfileDialog = true }
                )
            }
        }

        if (showBottomSheet) {
            val allowedTypes = when (currentRoute) {
                Screen.Calendar.route -> listOf(0) // Solo cursos/clases en Calendario
                Screen.Tasks.route -> listOf(1, 2)  // Solo trabajos y exámenes en la sección de Trabajos
                else -> listOf(0, 1, 2)             // Todos los 3 tipos en Inicio
            }

            AddContentBottomSheet(
                viewModel = viewModel,
                allowedTypes = allowedTypes,
                onDismiss = { showBottomSheet = false }
            )
        }

        if (showNotificationsSheet) {
            NotificationsBottomSheet(
                viewModel = viewModel,
                onDismiss = { showNotificationsSheet = false }
            )
        }

        if (showQuickProfileDialog) {
            QuickProfileDialog(
                onDismiss = { showQuickProfileDialog = false },
                onNavigateToProfile = {
                    showQuickProfileDialog = false
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    showQuickProfileDialog = false
                    authViewModel.logout()
                    navController.navigate("auth") {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                cycleName = userData?.currentCycle ?: "1er Ciclo"
            )
        }
    }
}

@Composable
fun SchoolTimeBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val barColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF006C88)
    val activeCircleColor = if (isDark) MaterialTheme.colorScheme.primary else Color.White
    val activeIconColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color(0xFF006C88)
    val inactiveIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.85f)
    val borderStroke = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = barColor,
            border = borderStroke,
            shadowElevation = 10.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    if (selected) {
                        // Elemento activo adaptativo
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(activeCircleColor)
                                .clickable {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                tint = activeIconColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                tint = inactiveIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
