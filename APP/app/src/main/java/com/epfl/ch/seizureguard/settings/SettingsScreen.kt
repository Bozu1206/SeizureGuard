package com.epfl.ch.seizureguard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Surface
import android.content.Intent
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.GsonBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileViewModel: ProfileViewModel,
    onLogoutClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by profileViewModel.profileState.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false 
                newPassword = ""
                confirmPassword = ""
                showError = false
            },
            title = { Text("Change Password") },
            text = {
                Column {
                    if (showError) {
                        Text(
                            text = "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                        profileViewModel.updatePassword(newPassword)
                        showPasswordDialog = false
                        newPassword = ""
                        confirmPassword = ""
                        showError = false
                    } else {
                        showError = true
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordDialog = false
                    newPassword = ""
                    confirmPassword = ""
                    showError = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = { 
                Text("Do you want to export your seizure history as JSON?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.exportSeizures(context)
                        showExportDialog = false
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Section "Security"
                SettingsSection(title = "Security") {
                    SettingsItem(
                        title = "Biometric Login",
                        icon = Icons.Default.Lock,
                        tint = if (profile.isBiometricEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        trailing = {
                            Switch(
                                checked = profile.isBiometricEnabled,
                                onCheckedChange = { isChecked ->
                                    profileViewModel.saveAuthPreference(isChecked)
                                }
                            )
                        }
                    )

                    SettingsItem(
                        title = "Change Password",
                        onClick = { showPasswordDialog = true },
                        icon = Icons.Default.Key
                    )
                }

                // Section "Model Training"
                SettingsSection(title = "Model Training") {
                    SettingsItem(
                        title = "Enable Training",
                        icon = Icons.Default.ModelTraining,
                        tint = if (profile.isTrainingEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        trailing = {
                            Switch(
                                checked = profile.isTrainingEnabled,
                                onCheckedChange = { isChecked ->
                                    profileViewModel.saveTrainingPreference(isChecked)
                                }
                            )
                        }
                    )
                }

                SettingsSection(title = "Data Management") {
                    SettingsItem(
                        title = "Export Seizure History",
                        onClick = { showExportDialog = true },
                        icon = Icons.Default.Download,
                        trailing = null
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout Button
                SettingsItem(
                    title = "Logout",
                    onClick = onLogoutClicked,
                    icon = Icons.Default.Logout,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    onClick: () -> Unit = {},
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            trailing?.invoke() ?: Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


