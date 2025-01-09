package com.epfl.ch.seizureguard.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.history.HistoryViewModel
import com.epfl.ch.seizureguard.inference.InferenceHomePage
import com.epfl.ch.seizureguard.medical_card.MedicalCardScreen
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.settings.SettingsScreen
import com.epfl.ch.seizureguard.medical_card.WalletUiState
import com.epfl.ch.seizureguard.profile.ProfileViewModel

@Composable
fun AppContent(
    metrics: Metrics,
    onRunInference: () -> Unit,
    onPauseInference: () -> Unit,
    payState: WalletUiState,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    onLogoutClicked: () -> Unit,
    profileViewModel: ProfileViewModel,
    seizureEventViewModel: SeizureEventViewModel,
    historyViewModel: HistoryViewModel,
    metricsViewModel: MetricsViewModel
) {
    val navController = rememberNavController()

    Scaffold(bottomBar = { BottomNavigationBar(navController) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inference",
            Modifier.padding(innerPadding)
        ) {
            composable("inference") {
                InferenceHomePage(
                    profileViewModel = profileViewModel,
                    onPerformInference = onRunInference,
                    onPauseInference = onPauseInference,
                    metricsViewModel = metricsViewModel
                )
            }
            composable("home") {
                HomeScreen(
                    seizureEventViewModel = seizureEventViewModel,
                    historyViewModel = historyViewModel,
                    profileViewModel = profileViewModel,
                    navController = navController
                )
            }
            composable("profile") {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    navController = navController,
                    requestSavePass = requestSavePass
                )
            }
            composable("history") {
                HistoryScreen(profileViewModel = profileViewModel)
            }
            composable("medicalCardForm") {
                MedicalCardScreen(payState, requestSavePass)
            }
            composable("settings") {
                SettingsScreen(profileViewModel = profileViewModel, onLogoutClicked = onLogoutClicked)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Monitor", Icons.Default.MonitorHeart, "inference"),
        BottomNavItem("Profile", Icons.Default.Person, "profile"),
        BottomNavItem("History", Icons.Default.DateRange, "history"),
        BottomNavItem("Settings", Icons.Default.Settings, "settings")
    )

    NavigationBar {
        val currentRoute = currentRoute(navController)
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// Helper to determine current route
@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)
