package com.epfl.ch.seizureguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.epfl.ch.seizureguard.alert.SeizureDetectedScreen
import com.epfl.ch.seizureguard.biometrics.BiometricAuthenticator
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.dl.utils.SampleBroadcastService
import com.epfl.ch.seizureguard.firebase.FirebaseLoginScreen
import com.epfl.ch.seizureguard.history.HistoryViewModel
import com.epfl.ch.seizureguard.inference.InferenceService
import com.epfl.ch.seizureguard.login.LoginScreen
import com.epfl.ch.seizureguard.medical_card.WalletViewModel
import com.epfl.ch.seizureguard.onboarding.OnboardingScreen
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModel
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModelFactory
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModelFactory
import com.epfl.ch.seizureguard.seizure_event.SeizureDao
import com.epfl.ch.seizureguard.seizure_event.SeizureDatabase
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.seizure_detection.SeizureDetectionViewModel
import com.epfl.ch.seizureguard.theme.AppTheme
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.wallet_manager.generateToken
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var onboardingViewModel: OnboardingViewModel
    val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(applicationContext, application = this.application)
    }

    private val seizureEventViewModel: SeizureEventViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val metricsViewModel: MetricsViewModel by viewModels()

    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0, -1.0))
    private var isLoggedIn by mutableStateOf(false)

    private val walletViewModel: WalletViewModel by viewModels()
    private val addToGoogleWalletRequestCode = 1000

    private lateinit var databaseRoom: SeizureDao
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2

    private val seizureDetectionViewModel by lazy {
        (application as RunningApp).seizureDetectionViewModel
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onboardingViewModel = ViewModelProvider(
            this,
            OnboardingViewModelFactory(this)
        )[OnboardingViewModel::class.java]

        databaseRoom = initializeDatabase()
        setContent {
            val isSeizureDetected by seizureDetectionViewModel.isSeizureDetected.collectAsState()
            val firebaseLogin by onboardingViewModel.firebaseLogin.collectAsState()
            val isAuthenticated by profileViewModel.isAuthenticated.collectAsState()
            val showOnboarding by onboardingViewModel.showOnboarding.collectAsState()
            val profile by profileViewModel.profileState.collectAsState()
            val isTrainingEnabled = profile.isTrainingEnabled

            val navigationState = determineNavigationState(
                showOnboarding = showOnboarding,
                firebaseLogin = firebaseLogin,
                isAuthenticated = isAuthenticated,
                isLoggedIn = isLoggedIn
            )
            
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (navigationState) {
                        "Onboarding" -> OnboardingScreen(
                            onFinish = {
                                isLoggedIn = true
                                onboardingViewModel.completeOnboarding()
                            },
                            profileViewModel = profileViewModel,
                            onboardingViewModel = onboardingViewModel
                        )
                        "FirebaseLogin" -> FirebaseLoginScreen(
                            profileViewModel = profileViewModel,
                            onLoggedIn = {
                                isLoggedIn = true
                                onboardingViewModel.completeOnboarding()
                            }
                        )
                        "Login" -> {
                            val context = LocalContext.current
                            LoginScreen(
                                onLoginSuccess = {
                                    isLoggedIn = true
                                },
                                context = context,
                                activity = context as FragmentActivity,
                                biometricAuthenticator = BiometricAuthenticator(context),
                                profileViewModel = profileViewModel
                            )
                        }
                        "MainScreen" -> MainScreen(
                            onRunInference = { startInferenceServices(isTrainingEnabled) },
                            metrics = metrics,
                            payState = walletViewModel.walletUiState.collectAsStateWithLifecycle().value,
                            requestSavePass = ::requestSavePass,
                            profileViewModel = profileViewModel,
                            seizureEventViewModel = seizureEventViewModel,
                            historyViewModel = historyViewModel,
                            metricsViewModel = metricsViewModel,
                            onLogoutClicked = {
                                profileViewModel.setAuthenticated(false)
                                isLoggedIn = false
                                onboardingViewModel.resetOnboarding(profileViewModel)
                            }
                        )
                    }

                    if (isSeizureDetected) {
                        val context = LocalContext.current
                        SeizureDetectedScreen(
                            onDismiss = {
                                seizureDetectionViewModel.onSeizureHandled()
                            },
                            onEmergencyCall = { onEmergencyCall(context) },
                            profileViewModel = profileViewModel
                        )
                    }
                }
            }
        }

        requestAllPermissions()
    }

    private fun determineNavigationState(
        showOnboarding: Boolean,
        firebaseLogin: Boolean,
        isAuthenticated: Boolean,
        isLoggedIn: Boolean
    ): String {
        return when {
            showOnboarding && !isAuthenticated && !firebaseLogin -> "Onboarding"
            showOnboarding && !isAuthenticated && firebaseLogin -> "FirebaseLogin"
            isAuthenticated && !isLoggedIn -> "Login"
            else -> "MainScreen"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), NOTIFICATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun startInferenceServices(isTrainingEnabled: Boolean) {
        Intent(applicationContext, SampleBroadcastService::class.java).also {
            startService(it)
        }

        Intent(applicationContext, InferenceService::class.java).also {
            it.action = InferenceService.Actions.START.toString()
            it.putExtra("IS_TRAINING_ENABLED", isTrainingEnabled)
            startForegroundService(it)
        }
    }

    private fun initializeDatabase(): SeizureDao {
        val application = requireNotNull(this).application
        return SeizureDatabase.getInstance(application).seizureDao
    }

    private fun requestSavePass(request: GoogleWalletToken.PassRequest) {
        lifecycleScope.launch {
            val token = generateToken(request)
            Log.d("MainActivity", "Generated token: $token")
            walletViewModel.savePassesJwt(token, this@MainActivity, addToGoogleWalletRequestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == ProfileViewModel.EXPORT_JSON_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            profileViewModel.handleExportResult(this, data?.data)
        }
    }
}
