package com.example.seizureguard.profile

import ProfileViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import coil.request.ImageRequest
import com.example.seizureguard.ui.theme.AppTheme

@Composable
fun ProfileScreen(
    profileScreenViewModel: ProfileViewModel = viewModel(),
    navController: NavController
) {
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
            // User Profile Section
            UserProfileSection(profileScreenViewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // Emergency Contacts Section
            // EmergencyContactsSection()

            // Spacer(modifier = Modifier.height(24.dp))

            // Upload Medical History Button
            // UploadMedicalDataSection(navController)
        }
    }
}

@Composable
fun UserProfileSection(profileScreenViewModel: ProfileViewModel) {
    val userName by profileScreenViewModel.userName.collectAsState()
    val userEmail by profileScreenViewModel.userEmail.collectAsState()
    val birthdate by profileScreenViewModel.birthdate.collectAsState()
    val profilePictureUri by profileScreenViewModel.profilePictureUri.collectAsState()
    val epi_type by profileScreenViewModel.epi_type.collectAsState()

    var showProfileSettings by remember { mutableStateOf(false) }


    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.background(Color.Transparent)) {
            // User Avatar
            if (profilePictureUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .padding(12.dp)
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
                    .padding(20.dp)
            ) {
                Row {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
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
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = birthdate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Epilepsy Type: $epi_type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (showProfileSettings) {
                    EditProfile(onDismissRequest = {
                        showProfileSettings = false
                        profileScreenViewModel.persistProfile()
                    }, profileScreenViewModel)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Emergency Contacts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        EmergencyContactCard(name = "Wife", phone = "+41 78 235 98 76")
        EmergencyContactCard(name = "Sister", phone = "+41 78 235 98 78")
        EmergencyContactCard(name = "Doctor Smith", phone = "+41 78 235 98 79")

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun EmergencyContactCard(name: String, phone: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
            .border(0.3.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
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
fun EditProfile(onDismissRequest: () -> Unit, profileScreenViewModel: ProfileViewModel) {
    val userName by profileScreenViewModel.userName.collectAsState()
    val userMail by profileScreenViewModel.userEmail.collectAsState()
    val epi_type by profileScreenViewModel.epi_type.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(text = "Edit your profile", style = MaterialTheme.typography.headlineSmall)
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
        ProfileScreen(viewModel(), navController = NavController(context = LocalContext.current))
    }
}
