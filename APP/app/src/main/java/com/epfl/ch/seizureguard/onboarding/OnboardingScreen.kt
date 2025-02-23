@file:Suppress("DEPRECATION")

package com.epfl.ch.seizureguard.onboarding

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileCreationForm
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.tools.GradientBorderSmall
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlin.random.Random

@SuppressLint("ShowToast")
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    profileViewModel: ProfileViewModel,
    onboardingViewModel: OnboardingViewModel
) {
    var showBiometricDialog by remember { mutableStateOf(false) }
    var showWelcomeAnimation by remember { mutableStateOf(false) }
    var biometricDialogDismissed by remember { mutableStateOf(false) }
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
        "Your trusted companion for monitoring and detecting seizures. Let's get started to ensure your safety and peace of mind.",
        "SeizureGuard uses AI to detect potential seizures and sends timely alerts to keep you safe.",
        "Your data is securely stored and never shared without your consent. We're committed to protecting your personal information.",
        "Let's create your profile to personalize your experience."
    )

    val images = listOf(
        R.drawable.ob1,
        R.drawable.ob6,
        R.drawable.ob4
    )
    Box(
        modifier = if (pagerState.currentPage == pages.lastIndex) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .paint(
                    painterResource(id = R.drawable.bg),
                    contentScale = ContentScale.FillBounds
                )
        }
    ) {
        GradientBorderSmall(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .zIndex(1f)
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .zIndex(2f),
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

            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            ) {
                if (pagerState.currentPage == pages.lastIndex) {
                    Text(
                        text = "Already have an account?",
                        color = Color.Gray,
                        fontWeight = FontWeight.Light,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            onboardingViewModel.wantsToLogin()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.2.dp, Color.Black)
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
                    Spacer(modifier = Modifier.height(8.dp))
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
                showBiometricDialog = false
                biometricDialogDismissed = true
            },
            onCancel = {
                profileViewModel.saveAuthPreference(isBiometric = false)
                showBiometricDialog = false
                biometricDialogDismissed = true
            }
        )
    }

    LaunchedEffect(biometricDialogDismissed) {
        if (biometricDialogDismissed) {
            delay(300) // Wait for dialog dismiss animation
            showWelcomeAnimation = true
        }
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
    Box(modifier = Modifier.fillMaxSize()) {
        ProfileCreationForm(profile = profile)
    }
}


private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float = Random.nextFloat() * 15f + 3f,
    val rotation: Float = Random.nextFloat() * 360f,
    val velocity: Offset = Offset(0f, 0f)
)

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Surface(
            color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(16.dp)
                .shadow(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(32.dp)

            ) {
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 28.sp
                )
                Text(
                    text = "SeizureGuard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Don't worry now, we are keeping track of everything!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}