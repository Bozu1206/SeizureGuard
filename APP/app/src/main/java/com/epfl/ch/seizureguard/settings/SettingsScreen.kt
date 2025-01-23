package com.epfl.ch.seizureguard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.epfl.ch.seizureguard.R
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileViewModel: ProfileViewModel,
    onLogoutClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by profileViewModel.profileState.collectAsState()
    val isParentMode by profileViewModel.parentMode.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
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
            title = { Text(stringResource(R.string.change_password)) },
            text = {
                Column {
                    if (showError) {
                        Text(
                            text = stringResource(R.string.passwords_do_not_match),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
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
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    newPassword = ""
                    confirmPassword = ""
                    showError = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_data)) },
            text = {
                if (profile.pastSeizures.isEmpty()) {
                    Text(stringResource(R.string.no_seizure_history_to_export))
                } else {
                    Text(stringResource(R.string.do_you_want_to_export_your_seizure_history_as_json))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profile.pastSeizures.isNotEmpty()) {
                            profileViewModel.exportSeizures(context)
                        }
                        showExportDialog = false
                    }
                ) {
                    if (profile.pastSeizures.isNotEmpty()) {
                        Text(stringResource(R.string.export))
                    } else {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false }
                ) {
                    if (profile.pastSeizures.isNotEmpty()) Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.about_seizureguard),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.version_1_0_0),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.developed_by),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.authors),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
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
                SettingsSection(title = stringResource(R.string.security)) {
                    SettingsItem(
                        title = stringResource(R.string.biometric_login),
                        icon = Icons.Default.Lock,
                        tint = if (profile.isBiometricEnabled)
                            Color(0xFF248a3d)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        caption = stringResource(R.string.use_fingerprint_or_face_recognition_to_secure_your_app),
                        trailing = {
                            Switch(
                                checked = profile.isBiometricEnabled,
                                onCheckedChange = { isChecked ->
                                    profileViewModel.saveAuthPreference(isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF248a3d),
                                    checkedThumbColor = Color.White,
                                )
                            )
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.change_password),
                        onClick = { showPasswordDialog = true },
                        icon = Icons.Default.Key
                    )
                }

                if (!isParentMode) {
                    // Section "Power Options"
                    SettingsSection(title = stringResource(R.string.power_options)) {
                        SettingsItem(
                            title = stringResource(R.string.enable_model_training),
                            icon = Icons.Default.ModelTraining,
                            tint = if (profile.isTrainingEnabled)
                                Color(0xFF248a3d)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            caption = stringResource(R.string.allow_the_app_to_learn_from_your_seizure_patterns),
                            trailing = {
                                Switch(
                                    checked = profile.isTrainingEnabled,
                                    onCheckedChange = { isChecked ->
                                        profileViewModel.saveTrainingPreference(isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = Color(0xFF248a3d),
                                        checkedThumbColor = Color.White,
                                    )
                                )
                            }
                        )
                        SettingsDropdownItem(
                            title = stringResource(R.string.power_mode),
                            icon = Icons.Default.Power,
                            tint = if (profile.isTrainingEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            caption = stringResource(R.string.adjust_power_consumption_and_detection_accuracy),
                            options = listOf(
                                context.getString(R.string.low_power_mode),
                                context.getString(R.string.normal_power_mode),
                                context.getString(R.string.high_performance_mode)
                            ),
                            selectedOption = profile.powerMode,
                            onOptionSelected = { option ->
                                profileViewModel.savePowerModePreference(option)
                            }
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.parent_mode)) {
                    SettingsItem(
                        title = stringResource(R.string.parent_mode),
                        icon = Icons.Default.SupervisorAccount,
                        tint = if (isParentMode)
                            Color(0xFF248a3d)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        caption = stringResource(R.string.enable_monitoring_and_control_features_for_caregivers),
                        trailing = {
                            Switch(
                                checked = isParentMode,
                                onCheckedChange = { isChecked ->
                                    profileViewModel.saveParentPreference(isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF248a3d),
                                    checkedThumbColor = Color.White,
                                )
                            )
                        }
                    )
                }

                SettingsSection(title = stringResource(R.string.developer_options)) {
                    SettingsItem(
                        title = stringResource(R.string.export_seizure_history),
                        onClick = { showExportDialog = true },
                        icon = Icons.Default.Download,
                        trailing = null
                    )
                    SettingsItem(
                        title = stringResource(R.string.debug_mode),
                        icon = Icons.Default.DeveloperBoard,
                        tint = if (profile.isDebugEnabled)
                            Color(0xFF248a3d)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        caption = stringResource(R.string.show_additional_information_for_troubleshooting),
                        trailing = {
                            val restartAppString = stringResource(R.string.restart_app_to_correctly_update_debug_mode)
                            Switch(
                                checked = profile.isDebugEnabled,
                                onCheckedChange = { isChecked ->
                                    Toast.makeText(
                                        context,
                                        restartAppString,
                                        Toast.LENGTH_LONG
                                    ).show()
                                    profileViewModel.saveDebugPreference(isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF248a3d),
                                    checkedThumbColor = Color.White,
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                SettingsSection("") {
                    SettingsItem(
                        title = stringResource(R.string.about_seizureguard),
                        onClick = { showAboutDialog = true },
                        icon = Icons.Default.Info,
                        trailing = null
                    )
                    SettingsItem(
                        title = stringResource(R.string.logout),
                        onClick = {
                            profileViewModel.logout()
                            onLogoutClicked()
                        },
                        icon = Icons.AutoMirrored.Filled.Logout,
                        tint = MaterialTheme.colorScheme.error,
                        background = MaterialTheme.colorScheme.errorContainer.copy(0.6f),
                        weight = FontWeight.Bold
                    )
                }
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}


@Composable
private fun SettingsItem(
    title: String,
    onClick: () -> Unit = {},
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    weight: FontWeight = FontWeight.Normal,
    caption: String? = null
) {
    Column {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = background,
            tonalElevation = 4.dp,
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = weight,
                    )
                }
                trailing?.invoke() ?: Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val trailing: @Composable () -> Unit = {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    SettingsItem(
        title = title,
        onClick = {

        },
        icon = icon,
        tint = tint,
        caption = caption,
        trailing = trailing
    )
}
