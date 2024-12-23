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
    profileScreenViewModel: ProfileViewModel = viewModel(),
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
            UserProfileSection(profileScreenViewModel, requestSavePass)

            Spacer(modifier = Modifier.height(24.dp))

            // Emergency Contacts Section
            EmergencyContactsSection(context, profileScreenViewModel)

            // Spacer(modifier = Modifier.height(24.dp))

            // Upload Medical History Button
            // UploadMedicalDataSection(navController)
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

    val photoPickerLauncher = rememberPhotoPickerLauncher(context) { newUri ->
        if (newUri != null) {
            profileScreenViewModel.updateProfileField("uri", newUri.toString())
        }
    }

    Log.d("Current Profile:", profile.toString())

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.onSurface
            )
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()

    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Color.Transparent)
        ) {
            // User Avatar
            if (profile.uri != "") {
                AsyncImage(
                    model = Uri.parse(profile.uri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .padding(18.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_PICK).apply {
                                type = "image/*"
                            }
                            photoPickerLauncher.launch(intent)
                        }
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Profile Picture",
                    tint = Color.hsl(270f, 0.61f, 0.24f),
                    modifier = Modifier.size(65.dp)
                )
            }


            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(12.dp)
            ) {
                Row {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                showProfileSettings = !showProfileSettings
                            }
                    )
                }

                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.labelMedium,
                )

                Text(
                    text = profile.birthdate,
                    style = MaterialTheme.typography.labelMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Epilepsy Type: ${profile.epi_type}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Medication: None",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (showProfileSettings) {
                    EditProfile(onDismissRequest = {
                        showProfileSettings = false
                        profileScreenViewModel.saveProfile()
                    }, profileScreenViewModel)
                }
            }
        }

        WalletButton(
            type = ButtonType.Add,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .padding(vertical = 10.dp)
                .height(60.dp),
            onClick = {
                val request = GoogleWalletToken.PassRequest(
                    uid = profile.uid,
                    patientName = profile.name,
                    emergencyContact = profileScreenViewModel.profileState.value.emergencyContacts.first().phone,
                    seizureType = profile.epi_type,
                    medication = "",
                    birthdate = profile.birthdate,
                )
                Log.d("ProfileScreen", "Wallet Button Clicked: $request")
                onWalletButtonClick(request)
            }
        )
    }
}

@Composable
fun EmergencyContactsSection(context: Context, profileViewModel: ProfileViewModel) {
    val profile by profileViewModel.profileState.collectAsState()
    val emergencyContacts = profile.emergencyContacts
    val activity = LocalContext.current as Activity
    val contactPicker = getContactPicker(context, profileViewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp) // Space for the button at the bottom
        ) {
            Text(
                text = "Emergency Contacts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emergencyContacts) { contact ->
                    val uri =
                        if (contact.photoUri != null) Uri.parse(contact.photoUri) else null
                    EmergencyContactCard(
                        name = contact.name,
                        phone = contact.phone,
                        picture = uri,
                        profileViewModel = profileViewModel
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (emergencyContacts.size < 5) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align to the bottom
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Another Contact (${5 - emergencyContacts.size} remaining)",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(text = "Add Emergency Contact")
                }

                Text(
                    text = "Add up to 5 emergency contacts",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .wrapContentWidth(align = Alignment.CenterHorizontally)
                )
            }
        }
    }
}


@Composable
fun EmergencyContactCard(
    name: String,
    phone: String,
    picture: Uri?,
    profileViewModel: ProfileViewModel,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer
                    .copy(0.3f), shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)

            .clickable {
                onClick()
            }

    ) {
        if (picture != null) {
            AsyncImage(
                model = picture,
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = "Call",
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onEmergencyCall(
                        context = context,
                        phone = phone
                    )
                }
        )

        Spacer(modifier = Modifier.width(16.dp))

        if (profileViewModel.profileState.value.emergencyContacts.size > 1) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        profileViewModel.updateEmergencyContacts(
                            EmergencyContact(
                                name = name,
                                phone = phone,
                                photoUri = picture.toString()
                            ), isAdding = false
                        )
                    }
            )
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
