package com.epfl.ch.seizureguard.firebase

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.PasswordTextField
import com.epfl.ch.seizureguard.profile.ProfileTextField
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

@Composable
fun FirebaseLoginScreen(
    profileViewModel: ProfileViewModel, 
    onLoggedIn: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Animation de sortie
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        // Contenu principal avec animation de fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        ) {
            IconButton(
                onClick = onBackToOnboarding,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black, CircleShape)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back to onboarding",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.login_bg),
                        contentDescription = "Login",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top=16.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        ProfileTextField(value = email, onValueChange = { email = it }, label = "Email")
                        Spacer(modifier = Modifier.size(8.dp))
                        PasswordTextField(password = pwd, onValueChange = { pwd = it })
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            if (email.isEmpty() || pwd.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please fill in all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            isLoading = true
                            profileViewModel.loadProfileFromEmail(email, pwd) { result ->
                                coroutineScope.launch {
                                    delay(500)
                                    isLoading = false
                                    if (result != null) {
                                        profileViewModel.retrieveAndStoreFcmToken()
                                        onLoggedIn()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Login failed. Please check your credentials.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Login",
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Loading indicator centré sur l'écran
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.Black,
                    strokeWidth = 4.dp
                )
            }
        }
    }

}