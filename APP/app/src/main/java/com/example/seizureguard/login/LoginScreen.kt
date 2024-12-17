package com.example.seizureguard.login

import ProfileViewModel
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.seizureguard.R
import com.example.seizureguard.biometrics.BiometricAuthenticator
import com.example.seizureguard.biometrics.authenticateWithBiometrics
import com.example.seizureguard.biometrics.isBiometricEnabled
import com.example.seizureguard.profile.PasswordTextField
import com.example.seizureguard.profile.ProfileTextField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    context: Context,
    activity: FragmentActivity,
    biometricAuthenticator: BiometricAuthenticator,
    profileViewModel: ProfileViewModel
) {
    var showLoginError by remember { mutableStateOf(false) }

    val correctPassword by profileViewModel.pwd.collectAsState()
    var password by remember { mutableStateOf("") }
    val authMode by profileViewModel.auth_mode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.obbg3),
                contentScale = ContentScale.FillBounds
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Align le titre en haut
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Welcome back ${profileViewModel.userName.value}!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                )
            }



            // Aligne l'image au milieu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_bg),
                    contentDescription = "SeizureGuard Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            // Aligne le bouton en bas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (authMode == "biometric") {
                    Button(
                        onClick = {
                            biometricAuthenticator.promptBiometricAuth(
                                title = "Login",
                                subTitle = "Use your fingerprint",
                                negativeButtonText = "Cancel",
                                fragmentActivity = activity,
                                onSuccess = {
                                    onLoginSuccess()
                                },
                                onError = { _, errorString ->
                                    Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show()
                                },
                                onFailed = {
                                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Login")
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))

                        PasswordTextField(password = password, onValueChange = { password = it })

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (password == correctPassword) onLoginSuccess()
                                else {
                                    showLoginError = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Login")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showLoginError) {
                        Toast.makeText(context, "Authentication Failed. Please retry.", Toast.LENGTH_SHORT).show()
                        showLoginError = false
                    }
                }
            }
        }
    }
}
