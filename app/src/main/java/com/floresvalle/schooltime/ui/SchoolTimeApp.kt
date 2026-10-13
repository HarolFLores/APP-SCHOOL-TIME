package com.floresvalle.schooltime.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.floresvalle.schooltime.ui.components.NotificationsBottomSheet
import com.floresvalle.schooltime.ui.components.QuickProfileDialog
import com.floresvalle.schooltime.ui.navigation.BottomNavItems
import com.floresvalle.schooltime.ui.navigation.Screen
import com.floresvalle.schooltime.util.AuthPreferences
import com.floresvalle.schooltime.viewmodel.AuthViewModel
import com.floresvalle.schooltime.viewmodel.RegisterViewModel
import com.floresvalle.schooltime.viewmodel.SessionViewModel

@Composable
fun SchoolTimeApp(viewModel: SessionViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hasCompletedOnboarding = remember { AuthPreferences.isOnboardingCompleted(context) }
    val isLoggedIn = remember { AuthPreferences.isSessionActive(context) }

    val startDestination = remember {
        when {
            !hasCompletedOnboarding -> "onboarding"
            !isLoggedIn -> "auth"
            else -> Screen.Home.route
        }
    }

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showQuickProfileDialog by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUserData.collectAsStateWithLifecycle()

    val isBottomBarVisible = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    BottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
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
                val authViewModel: AuthViewModel = viewModel()
                AuthScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register?email=&name=")
                    },
                    onGoogleSignUpPrefill = { name, email, _, _ ->
                        navController.navigate("register?email=$email&name=$name")
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }

            composable("register?email={email}&name={name}") { backStackEntry ->
                val emailParam = backStackEntry.arguments?.getString("email") ?: ""
                val nameParam = backStackEntry.arguments?.getString("name") ?: ""
                val registerViewModel: RegisterViewModel = viewModel()
                RegisterScreen(
                    viewModel = registerViewModel,
                    initialEmail = emailParam,
                    initialName = nameParam,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("register?email={email}&name={name}") { inclusive = true }
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onLogout = {
                        AuthPreferences.clearSession(context)
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
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
                    onOpenProfileDialog = { showQuickProfileDialog = true },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tasks.route) {
                TasksExamsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        AuthPreferences.clearSession(context)
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenNotifications = { showNotificationsSheet = true },
                    onOpenProfileDialog = { showQuickProfileDialog = true }
                )
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (showQuickProfileDialog) {
        QuickProfileDialog(
            user = currentUser,
            onDismiss = { showQuickProfileDialog = false },
            onNavigateToProfile = {
                showQuickProfileDialog = false
                navController.navigate(Screen.Profile.route)
            },
            onLogout = {
                showQuickProfileDialog = false
                AuthPreferences.clearSession(context)
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
