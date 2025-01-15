package com.epfl.ch.seizureguard.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.biometrics.BiometricAuthenticator
import com.epfl.ch.seizureguard.profile.PasswordTextField
import com.epfl.ch.seizureguard.profile.ProfileViewModel


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    context: Context,
    activity: FragmentActivity,
    biometricAuthenticator: BiometricAuthenticator,
    profileViewModel: ProfileViewModel
) {
    val profile by profileViewModel.profileState.collectAsState()
    var showLoginError by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val correctPassword = profile.pwd

    val shouldUseBiometricsInitially = (profile.auth_mode == "biometric")
    var useBiometric by remember { mutableStateOf(shouldUseBiometricsInitially) }
    var biometricAttempts by remember { mutableStateOf(0) }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Welcome back ${profile.name.split(" ")[0]}!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                )
            }


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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (useBiometric && biometricAttempts < 3) {
                    LaunchedEffect(Unit) {
                        biometricAuthenticator.promptBiometricAuth(
                            title = "Please authenticate",
                            subTitle = "Use your fingerprint",
                            negativeButtonText = "Cancel",
                            fragmentActivity = activity,
                            onSuccess = {
                                onLoginSuccess()
                            },
                            onError = { _, errorString ->
                                Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show()
                                useBiometric = false
                            },
                            onFailed = {
                                Toast.makeText(context, "Authentication failed, you have ${3-biometricAttempts} tries left", Toast.LENGTH_SHORT)
                                    .show()
                                if(biometricAttempts >= 3){
                                    useBiometric = false
                                }else{
                                    biometricAttempts++
                                }
                            }
                        )
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
                        Toast.makeText(
                            context,
                            "Authentication Failed. Please retry.",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLoginError = false
                    }
                }
            }
        }
    }
}
