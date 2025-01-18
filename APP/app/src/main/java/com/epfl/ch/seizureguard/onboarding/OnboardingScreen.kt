package com.epfl.ch.seizureguard.onboarding

import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileCreationForm
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@SuppressLint("ShowToast")
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    profileViewModel: ProfileViewModel,
    onboardingViewModel: OnboardingViewModel
) {
    var showBiometricDialog by remember { mutableStateOf(false) }
    val profile: Profile = Profile.empty()
    val context = LocalContext.current

    val pages = listOf(
        "Welcome to SeizureGuard!",
        "How It Works?",
        "Your Privacy Matters!",
        "Tell us more about you!"
    )

    val currentPage by onboardingViewModel.currentPage.collectAsState()
    
    val pagerState = rememberPagerState(
        initialPage = currentPage
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

    Box(
        modifier = if (pagerState.currentPage == pages.lastIndex) {
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        } else {
            Modifier
                .fillMaxSize()
                .paint(
                    painterResource(id = R.drawable.bg),
                    contentScale = ContentScale.FillBounds
                )

                .padding(16.dp)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (page < images.size) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Image(
                                painter = painterResource(images[page]),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(64.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = pages[page],
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = descriptions[page],
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        CreateHealthProfileScreen(
                            profile = profile
                        )
                    }
                }
            }

            if (pagerState.currentPage != pages.lastIndex) {
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                )
            }

            if (pagerState.currentPage == pages.lastIndex) {
                Text(
                    text = "Already have an account?",
                    color = Color.Gray,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = {
                        onboardingViewModel.wantsToLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color.Black)
                ) {
                    Text(
                        "Login",
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Login,
                        contentDescription = "Login",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            ) {
                if (pagerState.currentPage == pages.lastIndex) {
                    Button(
                        onClick = {
                            if (!Profile.isComplete(profile = profile)) {
                                Toast.makeText(
                                    context,
                                    "Please fill in all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                profileViewModel.registerUser(profile)
                                profileViewModel.saveProfile()
                                showBiometricDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Finish",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
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

    if (showBiometricDialog) {
        BiometricOptInDialog(
            onConfirm = {
                profileViewModel.saveAuthPreference(isBiometric = true)
                onFinish() // Complete onboarding
            },
            onCancel = {
                profileViewModel.saveAuthPreference(isBiometric = false)
                showBiometricDialog = false
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
fun CreateHealthProfileScreen(profile: Profile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ProfileCreationForm(
            profile = profile
        )
    }
}



