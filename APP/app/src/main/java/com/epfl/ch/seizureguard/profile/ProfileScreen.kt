package com.epfl.ch.seizureguard.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.epfl.ch.seizureguard.medical_card.WalletUiState
import com.epfl.ch.seizureguard.medical_card.WalletViewModel
import com.epfl.ch.seizureguard.theme.AppTheme
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.google.wallet.button.ButtonType
import com.google.wallet.button.WalletButton

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    navController: NavController,
    walletViewModel: WalletViewModel,
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
            UserProfileSection(profileViewModel, requestSavePass, walletViewModel, navController)

            Spacer(modifier = Modifier.height(24.dp))
            // Emergency Contacts Section
            EmergencyContactsSection(context, profileViewModel = profileViewModel)
        }
    }
}

@Composable
fun UserProfileSection(
    profileScreenViewModel: ProfileViewModel,
    onWalletButtonClick: (GoogleWalletToken.PassRequest) -> Unit,
    walletViewModel: WalletViewModel,
    navController: NavController
) {
    val profile by profileScreenViewModel.profileState.collectAsState()
    val context: Context = LocalContext.current
    var showProfileSettings by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberPhotoPickerLauncher(context, profile.uid) { newUri ->
        if (newUri != null) {
            isLoading = true
            profileScreenViewModel.updateProfileImage(newUri) {
                isLoading = false
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme())
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.background,
        ),
        modifier = Modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_PICK).apply {
                                type = "image/*"
                            }
                            photoPickerLauncher.launch(intent)
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.uri.isNotEmpty() && !isLoading) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = profile.uri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )


                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.1f)
                                                ),
                                                startY = 0f,
                                                endY = Float.POSITIVE_INFINITY
                                            )
                                        )
                                )

                                Text(
                                    text = profile.name.split(" ")[0],
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else if (!isLoading) {
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

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isSystemInDarkTheme()) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Your information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { showProfileSettings = !showProfileSettings },
                                modifier = Modifier
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InfoRow("Email", profile.email)
                            InfoRow("Birth Date", profile.birthdate)
                            InfoRow("Epilepsy Type", profile.epi_type)
                            InfoRow(
                                "Medication",
                                if (profile.medications.isEmpty()) "Lorazepam"
                                else profile.medications.joinToString(", ")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (walletViewModel.walletUiState.value !is WalletUiState.PassAdded) {
                WalletButton(
                    type = ButtonType.Add,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = {
                        val request = GoogleWalletToken.PassRequest(
                            uid = profile.uid,
                            patientName = profile.name,
                            emergencyContact = profileScreenViewModel.profileState.value.emergencyContacts.firstOrNull()?.phone
                                ?: "",
                            seizureType = profile.epi_type,
                            medication = "Lorazepam",
                            birthdate = profile.birthdate,
                        )
                        onWalletButtonClick(request)
                    }
                )
            }

            val gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xE2FF5722),
                    Color(0xBAFF9800),
                ),
                startX = 0f,
                endX = 900f
            )

            Button(
                onClick = { navController.navigate("medical_notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(gradient, shape = ButtonDefaults.shape)
                    .height(38.dp),
                shape = ButtonDefaults.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Medical Notes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
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
private fun InfoRow(
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
    }
}

@Composable
fun EmergencyContactsSection(context: Context, profileViewModel: ProfileViewModel) {
    val profile by profileViewModel.profileState.collectAsState()
    val emergencyContacts = profile.emergencyContacts
    val activity = LocalContext.current as Activity
    val contactPicker = getContactPicker(context, profileViewModel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${emergencyContacts.size}/3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            emergencyContacts.forEach { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = contact.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onEmergencyCall(context, contact.phone) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call contact",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    profileViewModel.updateEmergencyContacts(
                                        contact,
                                        isAdding = false
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove contact",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        if (emergencyContacts.size < 3) {
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add contact",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Contact")
            }
        }
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
            viewModel(),
            { }
        )

    }
}