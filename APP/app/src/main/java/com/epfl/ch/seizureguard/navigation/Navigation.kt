package com.epfl.ch.seizureguard.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.history.HistoryScreen
import com.epfl.ch.seizureguard.homescreen.HomeScreen
import com.epfl.ch.seizureguard.profile.ProfileScreen
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.inference.InferenceHomePage
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.settings.SettingsScreen
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import androidx.compose.foundation.layout.Box
import com.epfl.ch.seizureguard.wallet_manager.WalletViewModel
import com.epfl.ch.seizureguard.profile.MedicalNotesScreen
import com.epfl.ch.seizureguard.history.SeizureStatsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Monitor : Screen("inference", "Monitor", Icons.Default.MonitorHeart)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object History : Screen("history", "History", Icons.Default.DateRange)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object MedicalNotes : Screen("medical_notes", "Medical Notes", Icons.Default.Notes)
    object SeizureStats : Screen("seizure_stats", "Statistics", Icons.Default.ShowChart)

    companion object {
        fun getNavItems(isParentMode: Boolean) = if (!isParentMode) {
            listOf(Home, Monitor, Profile, History, Settings)
        } else {
            listOf(Home, Profile, History, Settings)
        }
    }
}

@Composable
fun AppContent(
    onRunInference: () -> Unit,
    onPauseInference: () -> Unit,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    onLogoutClicked: () -> Unit,
    profileViewModel: ProfileViewModel,
    seizureEventViewModel: SeizureEventViewModel,
    walletViewModel: WalletViewModel,
    metricsViewModel: MetricsViewModel
) {
    val navController = rememberNavController()
    val isParentMode by profileViewModel.parentMode.collectAsState()
    val currentRoute = currentRoute(navController)

    Scaffold(
        bottomBar = { 
            Box{
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    tonalElevation = 0.dp,
                ) {
                    Screen.getNavItems(isParentMode).forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xB9FF9800),
                                selectedTextColor = Color(0xB9FF9800),
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Monitor.route) {
                InferenceHomePage(
                    profileViewModel = profileViewModel,
                    onPerformInference = onRunInference,
                    onPauseInference = onPauseInference,
                    metricsViewModel = metricsViewModel
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    seizureEventViewModel = seizureEventViewModel,
                    profileViewModel = profileViewModel,
                    navController = navController
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    navController = navController,
                    requestSavePass = requestSavePass,
                    walletViewModel = walletViewModel,
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    profileViewModel = profileViewModel,
                    navController = navController
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(profileViewModel = profileViewModel, onLogoutClicked = onLogoutClicked)
            }
            composable(Screen.MedicalNotes.route) {
                MedicalNotesScreen(
                    profileViewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SeizureStats.route) {
                SeizureStatsScreen(
                    profileViewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}
