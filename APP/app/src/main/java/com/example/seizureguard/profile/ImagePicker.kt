package com.example.seizureguard.profile

import ProfileViewModel
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ProfilePicturePicker(profileViewModel: ProfileViewModel) {
    val profilePictureUri by profileViewModel.profilePictureUri.collectAsState()
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")

                    val localUri = copyImageToInternalStorage(context, uri, "profile_picture.jpg")
                    if (localUri != null) {
                        profileViewModel.saveProfile(
                            name = profileViewModel.userName.value,
                            email = profileViewModel.userEmail.value,
                            birthdate = profileViewModel.birthdate.value,
                            uri = localUri,
                            pwd = profileViewModel.pwd.value,
                            epi_type = profileViewModel.epi_type.value
                        )
                    } else {
                        Log.e("PhotoPicker", "Failed to copy image to internal storage.")
                    }
                }
            }
        }
    )


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Color
                        .hsl(266f, 0.92f, 0.95f)
                        .copy(0.6f)
                )
                .clickable {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                    }
                    photoPickerLauncher.launch(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            if (profilePictureUri != null) {
                AsyncImage(
                    model = profilePictureUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Default Profile Picture",
                    tint = Color.hsl(270f, 0.61f, 0.24f),
                    modifier = Modifier.size(65.dp)
                )
            }
        }
    }
}

fun copyImageToInternalStorage(context: Context, uri: Uri, fileName: String): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, fileName)

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        Log.e("CopyImage", "Failed to copy image: ${e.message}")
        null
    }
}


