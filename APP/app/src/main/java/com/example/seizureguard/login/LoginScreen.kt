package com.example.seizureguard.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seizureguard.biometrics.authenticateWithBiometrics
import com.example.seizureguard.biometrics.isBiometricEnabled

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    context: Context
) {
    val biometricEnabled = remember { isBiometricEnabled(context) } // Check biometric preference
    var showLoginError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login to SeizureGuard",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (biometricEnabled) {
                    authenticateWithBiometrics(
                        context = context,
                        onSuccess = {
                            onLoginSuccess() // Proceed to main content on successful login
                        },
                        onFailure = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            showLoginError = true
                        }
                    )
                } else {
                    // If biometrics not enabled, redirect to manual login
                    showLoginError = true
                }
            }
        ) {
            Text("Login with Biometrics")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showLoginError) {
            Text(
                text = "Authentication failed. Please try again.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
