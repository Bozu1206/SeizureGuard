package com.epfl.ch.seizureguard.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.google.wallet.button.ButtonType
import com.google.wallet.button.WalletButton

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
    val userName by profileScreenViewModel.userName.collectAsState()
    val userEmail by profileScreenViewModel.userEmail.collectAsState()
    val birthdate by profileScreenViewModel.birthdate.collectAsState()
    val profilePictureUri by profileScreenViewModel.profilePictureUri.collectAsState()
    val epi_type by profileScreenViewModel.epi_type.collectAsState()

    var showProfileSettings by remember { mutableStateOf(false) }
    val photoPickerLauncher = getPhotoPicker(LocalContext.current, profileScreenViewModel)

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
            if (profilePictureUri != null) {
                AsyncImage(
                    model = profilePictureUri,
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
                        text = userName,
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
                    text = userEmail,
                    style = MaterialTheme.typography.labelMedium,
                )

                Text(
                    text = birthdate,
                    style = MaterialTheme.typography.labelMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Epilepsy Type: $epi_type",
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
                        profileScreenViewModel.persistProfile()
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
                    uid = profileScreenViewModel.userId.value,
                    patientName = profileScreenViewModel.userName.value,
                    emergencyContact = profileScreenViewModel.emergencyContacts.value.firstOrNull()?.phone
                        ?: "",
                    seizureType = profileScreenViewModel.epi_type.value,
                    medication = "",
                    birthdate = profileScreenViewModel.birthdate.value
                )
                onWalletButtonClick(request)
            }
        )
    }
}

@Composable
fun EmergencyContactsSection(context: Context, profileViewModel: ProfileViewModel) {
    val emergencyContacts by profileViewModel.emergencyContacts.collectAsState()
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

        if (profileViewModel.emergencyContacts.value.size > 1) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        profileViewModel.removeEmergencyContact(phone)
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
    val userName by profileScreenViewModel.userName.collectAsState()
    val userMail by profileScreenViewModel.userEmail.collectAsState()
    val epi_type by profileScreenViewModel.epi_type.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
            ProfileTextField(value = userName, onValueChange = {
                profileScreenViewModel.updateUserName(it)
            }, label = "Name")

            Spacer(modifier = Modifier.height(8.dp))

            ProfileTextField(value = userMail, onValueChange = {
                profileScreenViewModel.updateUserMail(it)
            }, label = "Email")

            Spacer(modifier = Modifier.height(8.dp))

            EpilepsyTypeField(
                value = epi_type, onValueChange =
                {
                    profileScreenViewModel.updateEpilepsyType(it)
                }, label = "Epilepsy Type"
            )

            Spacer(modifier = Modifier.height(16.dp))
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
