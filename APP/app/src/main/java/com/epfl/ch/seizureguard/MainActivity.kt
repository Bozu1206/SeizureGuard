package com.epfl.ch.seizureguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.epfl.ch.seizureguard.alert.SeizureDetectedParentScreen
import com.epfl.ch.seizureguard.alert.SeizureDetectedScreen
import com.epfl.ch.seizureguard.biometrics.BiometricAuthenticator
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.dl.utils.SampleBroadcastService
import com.epfl.ch.seizureguard.firebase.FirebaseLoginScreen
import com.epfl.ch.seizureguard.inference.InferenceService
import com.epfl.ch.seizureguard.login.LoginScreen
import com.epfl.ch.seizureguard.navigation.AppContent
import com.epfl.ch.seizureguard.onboarding.OnboardingScreen
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModel
import com.epfl.ch.seizureguard.onboarding.OnboardingViewModelFactory
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.theme.AppTheme
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.wallet_manager.WalletViewModel
import com.epfl.ch.seizureguard.wallet_manager.generateToken
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var onboardingViewModel: OnboardingViewModel
    lateinit var profileViewModel: ProfileViewModel

    private val seizureEventViewModel: SeizureEventViewModel by viewModels()
    private val metricsViewModel: MetricsViewModel by viewModels()

    private var isLoggedIn by mutableStateOf(false)

    private val walletViewModel: WalletViewModel by viewModels()
    private val addToGoogleWalletRequestCode = 1000

    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2

    private val seizureDetectionViewModel by lazy {
        (application as RunningApp).seizureDetectionViewModel
    }

    // Track if all necessary permissions have been granted.
    private var permissionsGranted = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAllPermissions()

    }

    private fun determineNavigationState(
        showOnboarding: Boolean,
        firebaseLogin: Boolean,
        isAuthenticated: Boolean,
        isLoggedIn: Boolean,
        skipAuthentication: Boolean
    ): String {
        return when {
            showOnboarding && !isAuthenticated && !firebaseLogin -> "Onboarding"
            showOnboarding && !isAuthenticated && firebaseLogin -> "FirebaseLogin"
            isAuthenticated && !isLoggedIn && !skipAuthentication -> "Login"
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
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            onPermissionsGranted() // All permissions are already granted.
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            // Check that all permissions are granted.
            permissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (permissionsGranted) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, "Please grant permissions to continue", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun onPermissionsGranted() {
        onboardingViewModel = ViewModelProvider(
            this,
            OnboardingViewModelFactory(this)
        )[OnboardingViewModel::class.java]
        profileViewModel = RunningApp.getInstance(application as RunningApp).profileViewModel

        var isSeizureDetectedParentExtra =
            intent?.getBooleanExtra("EXTRA_SEIZURE_DETECTED_PARENT", false) ?: false
        val isSeizureDetectedExtra =
            intent?.getBooleanExtra("EXTRA_SEIZURE_DETECTED", false) ?: false

        setContent {
            val location by profileViewModel.latestLocation.collectAsState()
            val latitude = location?.latitude
            val longitude = location?.longitude

            val isSeizureDetected by seizureDetectionViewModel.isSeizureDetected.collectAsState()
            val firebaseLogin by onboardingViewModel.firebaseLogin.collectAsState()
            val isAuthenticated by profileViewModel.isAuthenticated.collectAsState()
            val showOnboarding by onboardingViewModel.showOnboarding.collectAsState()
            val profile by profileViewModel.profileState.collectAsState()
            val isTrainingEnabled = profile.isTrainingEnabled
            val isDebugEnabled = profile.isDebugEnabled
            val skipAuthentication: Boolean =
                isSeizureDetected || isSeizureDetectedParentExtra || isSeizureDetectedExtra

            val navigationState = determineNavigationState(
                showOnboarding = showOnboarding,
                firebaseLogin = firebaseLogin,
                isAuthenticated = isAuthenticated,
                isLoggedIn = isLoggedIn,
                skipAuthentication = skipAuthentication
            )

            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isSeizureDetectedParentExtra) {
                        val context = LocalContext.current
                        SeizureDetectedParentScreen(
                            latitude = latitude,
                            longitude = longitude,
                            onDismiss = {
                                seizureDetectionViewModel.onSeizureHandled()
                            },
                            onEmergencyCall = { onEmergencyCall(context) },
                            profileViewModel = profileViewModel,
                            context = context
                        )
                    } else {
                        if (isSeizureDetected || isSeizureDetectedExtra) {
                            val context = LocalContext.current
                            SeizureDetectedScreen(
                                onDismiss = {
                                    seizureDetectionViewModel.onSeizureHandled()
                                },
                                onEmergencyCall = { onEmergencyCall(context) },
                                profileViewModel = profileViewModel
                            )
                        } else {
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
                                    },
                                    onBackToOnboarding = {
                                        onboardingViewModel.wantsToRegister()
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

                                "MainScreen" -> {
                                    val context = LocalContext.current
                                    AppContent(
                                        onRunInference = {
                                            startInferenceServices(
                                                isTrainingEnabled,
                                                isDebugEnabled
                                            )
                                        },
                                        onPauseInference = {
                                            val stopIntent = Intent(
                                                context,
                                                InferenceService::class.java
                                            ).apply {
                                                action = InferenceService.Actions.STOP.toString()
                                            }
                                            context.startService(stopIntent)
                                        },
                                        walletViewModel = walletViewModel,
                                        requestSavePass = ::requestSavePass,
                                        profileViewModel = profileViewModel,
                                        seizureEventViewModel = seizureEventViewModel,
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
                    }
                }
            }
        }
    }

    // Starting the correct foreground services for starting inference
    private fun startInferenceServices(
        isTrainingEnabled: Boolean,
        isDebugEnabled: Boolean
    ) {
        Intent(applicationContext, SampleBroadcastService::class.java).also {
            startService(it)
        }
        Intent(applicationContext, InferenceService::class.java).also {
            it.action = InferenceService.Actions.START.toString()
            it.putExtra("IS_TRAINING_ENABLED", isTrainingEnabled)
            it.putExtra("IS_DEBUG_ENABLED", isDebugEnabled)
            startForegroundService(it)
        }
    }

    private fun requestSavePass(request: GoogleWalletToken.PassRequest) {
        lifecycleScope.launch {
            val token = generateToken(request)
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
