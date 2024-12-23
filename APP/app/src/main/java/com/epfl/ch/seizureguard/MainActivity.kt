package com.epfl.ch.seizureguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.epfl.ch.seizureguard.theme.AppTheme
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.wallet_manager.generateToken
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var onboardingViewModel: OnboardingViewModel
    val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(applicationContext)
    }

    private val seizureEventViewModel: SeizureEventViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val metricsViewModel: MetricsViewModel by viewModels()

    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0, -1.0))
    private var isSeizureDetected by mutableStateOf(false)
    private var isLoggedIn by mutableStateOf(false)

    private val walletViewModel: WalletViewModel by viewModels()
    private val addToGoogleWalletRequestCode = 1000

    private lateinit var databaseRoom: SeizureDao
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val seizureDetected = intent.getBooleanExtra("EXTRA_SEIZURE_DETECTED", false)
        if (seizureDetected && !isSeizureDetected) {
            Log.d("MainActivity", "Seizure detected (onNewIntent)")
            isSeizureDetected = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onboardingViewModel = ViewModelProvider(
            this,
            OnboardingViewModelFactory(this)
        )[OnboardingViewModel::class.java]

        if (intent?.getBooleanExtra("EXTRA_SEIZURE_DETECTED", false) == true) {
            Log.d("MainActivity", "Seizure detected")
            isSeizureDetected = true
        }

        databaseRoom = initializeDatabase()
        setContent {
            // onboardingViewModel.resetOnboarding(profileViewModel)
            val activity = LocalContext.current as FragmentActivity
            val biometricAuthenticator = BiometricAuthenticator(this)

            val firebaseLogin by onboardingViewModel.firebaseLogin.collectAsState()
            val isAuthenticated by profileViewModel.isAuthenticated.collectAsState()
            val showOnboarding by onboardingViewModel.showOnboarding.collectAsState()
            val profile by profileViewModel.profileState.collectAsState()

            val isTrainingEnabled = profile.isTrainingEnabled

            val navigationState = determineNavigationState(
                showOnboarding = showOnboarding,
                firebaseLogin = firebaseLogin,
                isAuthenticated = isAuthenticated,
                isLoggedIn = isLoggedIn,
                isSeizureDetected = isSeizureDetected
            )

            AppTheme {
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
                    "Login" -> LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        },
                        context = this,
                        activity = activity,
                        biometricAuthenticator = biometricAuthenticator,
                        profileViewModel = profileViewModel
                    )
                    "SeizureDetected" -> {
                        Toast.makeText(
                            this,
                            "Seizure detected. Screen not implemented.",
                            Toast.LENGTH_SHORT
                        ).show()
                        isSeizureDetected = false
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
            }
        }

        requestAllPermissions()
    }

    private fun determineNavigationState(
        showOnboarding: Boolean,
        firebaseLogin: Boolean,
        isAuthenticated: Boolean,
        isLoggedIn: Boolean,
        isSeizureDetected: Boolean
    ): String {
        Log.d("MainActivity", "states: $showOnboarding, $firebaseLogin, $isAuthenticated, $isLoggedIn, $isSeizureDetected")
        return when {
            showOnboarding  && !isAuthenticated && !firebaseLogin -> "Onboarding"
            showOnboarding  && !isAuthenticated && firebaseLogin  -> "FirebaseLogin"
            isAuthenticated && !isLoggedIn && !isSeizureDetected -> "Login"
            isSeizureDetected -> "SeizureDetected"
            else -> "MainScreen"
        }
    }

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
}
