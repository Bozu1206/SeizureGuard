package com.epfl.ch.seizureguard

import android.Manifest
import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.epfl.ch.seizureguard.biometrics.BiometricAuthenticator
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.dl.utils.SampleBroadcastService
import com.epfl.ch.seizureguard.history.HistoryViewModel
import com.epfl.ch.seizureguard.inference.InferenceService
import com.epfl.ch.seizureguard.login.LoginScreen
import com.epfl.ch.seizureguard.onboarding.OnboardingScreen
import com.epfl.ch.seizureguard.seizure_event.SeizureDao
import com.epfl.ch.seizureguard.seizure_event.SeizureDatabase
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.theme.AppTheme
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.wallet_manager.generateToken
import com.epfl.ch.seizureguard.medical_card.WalletViewModel
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModel
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModelFactory
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: OnboardingViewModel
    val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(applicationContext)
    }

    private val seizureEventViewModel: SeizureEventViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val metricsViewModel: MetricsViewModel by viewModels()

    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0, -1.0))
    private var isSeizureDetected by mutableStateOf(false)

    private val walletViewModel: WalletViewModel by viewModels()
    private val addToGoogleWalletRequestCode = 1000

    private lateinit var databaseRoom: SeizureDao
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        if (intent?.getBooleanExtra("EXTRA_SEIZURE_DETECTED", false) == true) {
            Log.d("MainActivity", "Seizure detected (onNewIntent)")
            isSeizureDetected = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            OnboardingViewModelFactory(this)
        )[OnboardingViewModel::class.java]

        if (intent?.getBooleanExtra(
                "EXTRA_SEIZURE_DETECTED",
                false
            ) == true
        ) {
            Log.d("MainActivity", "Seizure detected")
            isSeizureDetected = true
        }

        databaseRoom = initializeDatabase()
        setContent {
            val activity = LocalContext.current as FragmentActivity
            val biometricAuthenticator = BiometricAuthenticator(this)

            AppTheme {
                // DEBUG
                // viewModel.resetOnboarding(profileViewModel)

                // Fetch app states
                val showOnboarding by viewModel.showOnboarding.collectAsState()
                val isTrainingEnabled by profileViewModel.isTrainingEnabled.collectAsState()
                var isLoggedIn by remember { mutableStateOf(true) }

                if (showOnboarding) {
                    // Show OnboardingScreen
                    OnboardingScreen(
                        onFinish = {
                            isLoggedIn = true
                            viewModel.completeOnboarding()
                        },
                        profileViewModel = profileViewModel
                    )
                } else if (!isLoggedIn && !isSeizureDetected) {
                    LoginScreen(
                        onLoginSuccess = { isLoggedIn = true },
                        context = this,
                        activity = activity,
                        biometricAuthenticator = biometricAuthenticator,
                        profileViewModel = profileViewModel
                    )
                } /*else if (isSeizureDetected) {
                    *//*SeizureDetectedScreen(
                        onDismiss = { isSeizureDetected = false },
                        onEmergencyCall = { onEmergencyCall(this) },
                        seizureEventViewModel = seizureEventViewModel
                    )*//*
                }*/ else {
                    // Show main app content
                    MainScreen(
                        onRunInference = {
                            Log.d("MainActivity", "Starting SampleBroadcastService")
                            Intent(applicationContext, SampleBroadcastService::class.java).also {
                                startService(it)
                            }

                            Log.d("MainActivity", "Starting InferenceService")
                            Intent(applicationContext, InferenceService::class.java).also {
                                Log.d("MainActivity", "starting intent")
                                it.action = InferenceService.Actions.START.toString()
                                it.putExtra("IS_TRAINING_ENABLED", isTrainingEnabled)
                                startForegroundService(it)
                            }
                        },
                        metrics = metrics,
                        payState = walletViewModel.walletUiState.collectAsStateWithLifecycle().value,
                        requestSavePass = ::requestSavePass,
                        profileViewModel = profileViewModel,
                        seizureEventViewModel = seizureEventViewModel,
                        historyViewModel = historyViewModel,
                        metricsViewModel = metricsViewModel
                    )
                }
            }
        }


        requestNotificationPermission()
        requestIgnoreBatteryOptimizationPermission()
        requestForegroundServicesPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) { // triggered when the use responds to the notification permission request
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted TO DO
            } else {
                Toast.makeText(
                    this,
                    "Notification permission is required for displaying notifications.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestIgnoreBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("InferenceService", "Battery optimization is enabled for this app.")
                // Prompt the user to disable optimizations
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Log.d("InferenceService", "Battery optimization is already disabled.")
            }
        }
    }

    private fun requestForegroundServicesPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                4
            )
        }
        if (checkSelfPermission(android.Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                5
            )
        }
    }

    private fun initializeDatabase(): SeizureDao {
        val application = requireNotNull(this).application
        return SeizureDatabase.getInstance(application).seizureDao
    }

    private fun updateMetrics(newMetrics: Metrics) {
        metrics = newMetrics
        Log.d(
            "ValidationMetrics", """
            F1 Score: ${metrics.f1}, Precision: ${metrics.precision}, 
            Recall: ${metrics.recall}, FPR: ${metrics.fpr}
        """.trimIndent()
        )
    }

    private fun requestSavePass(request: GoogleWalletToken.PassRequest) {
        lifecycleScope.launch {
            val token = generateToken(request)
            walletViewModel.savePassesJwt(token, this@MainActivity, addToGoogleWalletRequestCode)
        }
    }
}


fun logAllPastSeizures(seizureDao: SeizureDao) { // temporary, for debug purpose
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val seizures = seizureDao.getAllSeizureEvents().value
            seizures?.forEach { seizure ->
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



