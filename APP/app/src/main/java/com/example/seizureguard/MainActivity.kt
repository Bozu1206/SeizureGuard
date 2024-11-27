package com.example.seizureguard

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.seizureguard.dl.DataLoader
import com.example.seizureguard.dl.InferenceProcessor
import com.example.seizureguard.dl.OnnxHelper
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Metrics for model validation
    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0, -1.0))
    private var isSeizureDetected by mutableStateOf(false)

    private lateinit var databaseRoom: SeizureDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = requireNotNull(this).application
        val dataSource = SeizureDatabase.getInstance(application).seizureDao

        databaseRoom = dataSource

        setContent {
            val context = LocalContext.current
            val dataLoader = DataLoader()
            val onnxHelper = OnnxHelper()
            val inferenceProcessor =
                InferenceProcessor(context, dataLoader, onnxHelper, onSeizureDetected = {
                    runOnUiThread {
                        isSeizureDetected = true
                    }
                })

            AppTheme {
                if (isSeizureDetected) {
                    SeizureDetectedScreen(
                        onDismiss = { isSeizureDetected = false },
                        onEmergencyCall = { onEmergencyCall(context = context) }
                    )
                } else {
                    AppContent(
                        metrics = metrics,
                        onRunInference = {
                            inferenceProcessor.runInference { newMetrics ->
                                metrics = newMetrics
                                Log.d(
                                    "ValidationMetrics",
                                    "F1 Score: ${metrics.f1}, Precision: ${metrics.precision}, " +
                                            "Recall: ${metrics.recall}, FPR: ${metrics.fpr}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    metrics: Metrics,
    onRunInference: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            Modifier.padding(innerPadding)
        ) {
            composable("inference") {
                InferenceScreen(metrics = metrics, onRunInference = onRunInference)
            }
            composable("home") {
                HomeScreen()
            }
            composable("profile") {
                ProfileScreen()
            }
            composable("history") {
                HistoryScreen()
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
        BottomNavItem("History", Icons.Default.DateRange, "history")
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

@Composable
fun InferenceScreen(
    metrics: Metrics,
    onRunInference: () -> Unit,
) {
    InferenceHomePage(metrics = metrics, onPerformInference = onRunInference)
}

fun logAllPastSeizures(seizureDao: SeizureDao) { // temporary, for debug purpose
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val seizures = seizureDao.getAllSeizureEvents()
            seizures.forEach { seizure ->
                Log.d(
                    "SeizureLog", """
                        Seizure ID: ${seizure.seizureKey}
                        Type: ${seizure.type}
                        Duration: ${seizure.duration} minutes
                        Severity: ${seizure.severity}
                        Triggers: ${seizure.triggers.joinToString(", ")}
                        Timestamp: ${seizure.timestamp}
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            Log.e("SeizureLogError", "Failed to fetch seizures: ${e.message}")
        }
    }
}



