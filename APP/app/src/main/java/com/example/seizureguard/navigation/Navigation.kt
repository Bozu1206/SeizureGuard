package com.example.seizureguard.navigation

import ProfileViewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import com.example.seizureguard.history.HistoryScreen
import com.example.seizureguard.homescreen.HomeScreen
import com.example.seizureguard.inference.InferenceScreen
import com.example.seizureguard.profile.ProfileScreen
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.medical_card.MedicalCardScreen
import com.example.seizureguard.seizure_event.SeizureEventViewModel
import com.example.seizureguard.settings.SettingsScreen
import com.google.android.gms.samples.wallet.viewmodel.WalletUiState

@Composable
fun AppContent(
    metrics: Metrics,
    onRunInference: () -> Unit,
    payState: WalletUiState,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    profileViewModel: ProfileViewModel,
    seizureEventViewModel: SeizureEventViewModel
) {
    val navController = rememberNavController()

    Scaffold(bottomBar = { BottomNavigationBar(navController) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "profile",
            Modifier.padding(innerPadding)
        ) {
            composable("inference") {
                InferenceScreen(metrics = metrics, onRunInference = onRunInference)
            }
            composable("home") {
                HomeScreen(seizureEventViewModel =  seizureEventViewModel)
            }
            composable("profile") {
                ProfileScreen(profileScreenViewModel = profileViewModel, navController = navController, requestSavePass = requestSavePass)
            }
            composable("history") {
                HistoryScreen()
            }
            composable("medicalCardForm") {
                MedicalCardScreen(payState, requestSavePass)
            }
            composable("settings") {
                SettingsScreen(profileViewModel)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Inference", Icons.Default.Refresh, "inference"),
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
