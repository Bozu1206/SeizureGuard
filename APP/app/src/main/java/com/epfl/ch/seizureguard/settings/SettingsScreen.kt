package com.epfl.ch.seizureguard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.ch.seizureguard.profile.ProfileViewModel

@Composable
fun SettingsScreen(profileViewModel: ProfileViewModel, onLogoutClicked: () -> Unit = {}) {
    val profile by profileViewModel.profileState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Settings", modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Biometric Login",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = profile.isBiometricEnabled,
                onCheckedChange = {
                    if (it) profileViewModel.saveAuthPreference(true)
                    else profileViewModel.saveAuthPreference(false)
                },
                thumbContent = if (profile.isBiometricEnabled) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Enable Training",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = profile.isTrainingEnabled,
                onCheckedChange = {
                    profileViewModel.saveTrainingPreference(it)
                },
                thumbContent = if (profile.isTrainingEnabled) {
                    {
                        Icon(
                            imageVector = Icons.Filled.ModelTraining,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                }
            )
        }

        Button(onClick = { onLogoutClicked() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp) // Add padding
        ) {
            Text("Logout")
        }
    }
}

