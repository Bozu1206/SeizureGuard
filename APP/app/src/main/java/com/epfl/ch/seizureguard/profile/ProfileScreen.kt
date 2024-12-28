package com.epfl.ch.seizureguard.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import com.epfl.ch.seizureguard.theme.AppTheme
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.google.wallet.button.ButtonType
import com.google.wallet.button.WalletButton
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    navController: NavController,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter

    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserProfileSection(profileViewModel, requestSavePass)

            Spacer(modifier = Modifier.height(24.dp))

            // Emergency Contacts Section
            EmergencyContactsSection(context, profileViewModel = profileViewModel)
        }
    }
}

@Composable
fun UserProfileSection(
    profileScreenViewModel: ProfileViewModel,
    onWalletButtonClick: (GoogleWalletToken.PassRequest) -> Unit
) {
    val profile by profileScreenViewModel.profileState.collectAsState()
    val context: Context = LocalContext.current
    var showProfileSettings by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberPhotoPickerLauncher(context, profile.uid) { newUri ->
        if (newUri != null) {
            profileScreenViewModel.updateProfileField("uri", newUri.toString())
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.onSurface
            )
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header avec photo et infos basiques
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo de profil
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable {
                            val intent = Intent(Intent.ACTION_PICK).apply {
                                type = "image/*"
                            }
                            photoPickerLauncher.launch(intent)
                        }
                ) {
                    if (profile.uri.isNotEmpty()) {
                        AsyncImage(
                            model = Uri.parse(profile.uri),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile Picture",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Informations de base
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(
                    onClick = { showProfileSettings = !showProfileSettings }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medical Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                MedicalInfoRow(
                    label = "Birth Date",
                    value = profile.birthdate
                )

                Spacer(modifier = Modifier.height(8.dp))

                MedicalInfoRow(
                    label = "Epilepsy Type",
                    value = profile.epi_type
                )

                Spacer(modifier = Modifier.height(8.dp))

                MedicalInfoRow(
                    label = "Medication",
                    value = if (profile.medications.isEmpty()) "None"
                           else profile.medications.joinToString(", ")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Wallet Button
            WalletButton(
                type = ButtonType.Add,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    val request = GoogleWalletToken.PassRequest(
                        uid = profile.uid,
                        patientName = profile.name,
                        emergencyContact = profileScreenViewModel.profileState.value.emergencyContacts.firstOrNull()?.phone ?: "",
                        seizureType = profile.epi_type,
                        medication = "",
                        birthdate = profile.birthdate,
                    )
                    onWalletButtonClick(request)
                }
            )
        }
    }

    if (showProfileSettings) {
        EditProfile(
            onDismissRequest = {
                showProfileSettings = false
                profileScreenViewModel.saveProfile()
            },
            profileScreenViewModel
        )
    }
}

@Composable
private fun MedicalInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        if (label == "Medication" && value != "None") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmergencyContactsSection(context: Context, profileViewModel: ProfileViewModel) {
    val profile by profileViewModel.profileState.collectAsState()
    val emergencyContacts = profile.emergencyContacts
    val activity = LocalContext.current as Activity
    val contactPicker = getContactPicker(context, profileViewModel)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.onSurface
            )
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${emergencyContacts.size}/5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contacts List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (emergencyContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emergency contacts yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(emergencyContacts) { contact ->
                            EmergencyContactCard(
                                name = contact.name,
                                phone = contact.phone,
                                picture = contact.photoUri?.let { Uri.parse(it) },
                                profileViewModel = profileViewModel
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Contact Button
            Button(
                onClick = {
                    if (hasContactPermission(context)) {
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                        contactPicker.launch(intent)
                    } else {
                        requestContactPermission(context, activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (emergencyContacts.size >= 5) 0.5f else 1f),
                enabled = emergencyContacts.size < 5
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add Contact")
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    name: String,
    phone: String,
    picture: Uri?,
    profileViewModel: ProfileViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contact Picture
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (picture != null) {
                    AsyncImage(
                        model = picture,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Contact Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            val context = LocalContext.current
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        onEmergencyCall(context, phone)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (profileViewModel.profileState.value.emergencyContacts.size > 1) {
                    IconButton(
                        onClick = {
                            profileViewModel.updateEmergencyContacts(
                                EmergencyContact(
                                    name = name,
                                    phone = phone,
                                    photoUri = picture?.toString()
                                ),
                                isAdding = false
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadMedicalDataSection(navController: NavController) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = { navController.navigate("medicalCardForm") },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = "Create Medical Card")
        }

        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = "Upload Medical History")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Uploading your medical history helps improve emergency response.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(
    onDismissRequest: () -> Unit,
    profileScreenViewModel: ProfileViewModel
) {
    val profile by profileScreenViewModel.profileState.collectAsState()
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var type by remember { mutableStateOf(profile.epi_type) }

    ModalBottomSheet(
        onDismissRequest = {
            profileScreenViewModel.updateProfileField("name", name)
            profileScreenViewModel.updateProfileField("email", email)
            profileScreenViewModel.updateProfileField("epi_type", type)

            profileScreenViewModel.saveProfile()
            onDismissRequest()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Edit your profile",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileTextField(value = name, onValueChange = { name = it }, label = "Name")

            Spacer(modifier = Modifier.height(8.dp))

            ProfileTextField(value = email, onValueChange = {
                email = it
            }, label = "Email")

            Spacer(modifier = Modifier.height(8.dp))

            EpilepsyTypeField(
                value = type, onValueChange = { type = it }, label = "Epilepsy Type"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    profileScreenViewModel.updateMultipleFieldsAndSave(
                        mapOf(
                            "name" to name,
                            "email" to email,
                            "epi_type" to type
                        )
                    )
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Save Changes")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen(
            viewModel(),
            navController = NavController(context = LocalContext.current),
            {})
    }
}