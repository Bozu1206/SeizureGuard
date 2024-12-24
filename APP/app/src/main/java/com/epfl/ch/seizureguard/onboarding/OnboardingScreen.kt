package com.epfl.ch.seizureguard.onboarding

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.htmlEncode
import androidx.navigation.NavController
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileCreationForm
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    profileViewModel: ProfileViewModel,
    onboardingViewModel: OnboardingViewModel
) {
    var currentPage by remember { mutableStateOf(0) }
    var showBiometricDialog by remember { mutableStateOf(false) }
    val profile: Profile = Profile.empty()
    val p by profileViewModel.profileState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        "Welcome to SeizureGuard!",
        "How It Works?",
        "Your Privacy Matters",
        "Create Your Profile"
    )

    val descriptions = listOf(
        "Your trusted companion for monitoring and detecting seizures. Let’s get started to ensure your safety and peace of mind.",
        "SeizureGuard uses AI to detect potential seizures and sends timely alerts to keep you safe.",
        "Your data is securely stored and never shared without your consent. We’re committed to protecting your personal information.",
        "Let’s create your profile to personalize your experience."
    )

    val images = listOf(
        R.drawable.ob1,
        R.drawable.ob6,
        R.drawable.ob4
    )

    val modifier: Modifier
    if (currentPage == pages.lastIndex) {
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    } else {
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.obbg3),
                contentScale = ContentScale.FillBounds
            )
            .padding(16.dp)
    }

    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentPage < images.size) {
                // Onboarding Content
                Image(
                    painter = painterResource(images[currentPage]),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = pages[currentPage],
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = descriptions[currentPage],
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // Profile Creation Screen (last page)
                CreateHealthProfileScreen(profileViewModel = profileViewModel, profile = profile)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                if (currentPage == pages.lastIndex) {
                    Text(
                        text = "Already have an account?",
                        color = Color.Gray,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            onboardingViewModel.wantsToLogin()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Login")
                        Icon(
                            imageVector = Icons.Rounded.Login,
                            contentDescription = "Login",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Navigation (Back/Next/Finish)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    if (currentPage > 0) {
                        Button(
                            onClick = { currentPage-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (currentPage < pages.lastIndex) {
                        Button(
                            onClick = { currentPage++ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForwardIos,
                                contentDescription = "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!Profile.isComplete(profile = profile)) {
                                    // Show error message
                                    Log.d("OnboardingScreen", "Profile is incomplete: $profile")
                                    Toast.makeText(
                                        context,
                                        "Please fill in all fields",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                } else {
                                    Log.d("OnboardingScreen", "Profile is complete: $profile")
                                    profileViewModel.registerUser(profile)
                                    profileViewModel.saveProfile()
                                    showBiometricDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Finish")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowForwardIos,
                                contentDescription = "Finish",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

    }

// Biometric Opt-In Dialog
    if (showBiometricDialog) {
        BiometricOptInDialog(
            onConfirm = {
                profileViewModel.saveAuthPreference(isBiometric = true)
                onFinish() // Complete onboarding
            },
            onCancel = {
                profileViewModel.saveAuthPreference(isBiometric = false)
                showBiometricDialog = false // Dismiss dialog
                onFinish() // Complete onboarding
            }
        )
    }
}

@Composable
fun BiometricOptInDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Enable Biometric Login?") },
        text = { Text(text = "Would you like to use biometric authentication for faster and secure logins?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Enable")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Skip")
            }
        }
    )
}


@Composable
fun CreateHealthProfileScreen(profileViewModel: ProfileViewModel, profile: Profile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ProfileCreationForm(
            profileViewModel = profileViewModel,
            profile = profile
        )
    }
}



