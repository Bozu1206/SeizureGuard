package com.epfl.ch.seizureguard.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun rememberPhotoPickerLauncher(
    context: Context,
    uid: String,
    onImagePicked: (Uri?) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedUri = result.data?.data
            if (selectedUri != null) {
                val localUri = copyImageToInternalStorage(
                    context,
                    selectedUri,
                    "profile_picture_${uid}.jpg"
                )
                onImagePicked(localUri)
            } else {
                onImagePicked(null)
            }
        } else {
            onImagePicked(null)
        }
    }
}


@Composable
fun ProfilePicturePicker(profile: Profile) {
    val context = LocalContext.current
    var uri by remember { mutableStateOf(profile.uri) }

    val photoPickerLauncher = rememberPhotoPickerLauncher(context, profile.uid) { newUri ->
        if (newUri != null) {
            uri = newUri.toString()
            profile.uri = uri
        }
    }

    ProfilePicturePickerHelper(
        uri = uri,
        onImagePick = {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            photoPickerLauncher.launch(intent)
        }
    )
}

@Composable
fun ProfilePicturePickerHelper(uri: String, onImagePick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Color.Gray.copy(0.1f)
                )
                .clickable {
                    onImagePick()
                },

            contentAlignment = Alignment.Center
        ) {
            if (uri.isNotEmpty()) {
                AsyncImage(
                    model = Uri.parse(uri),
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
                    tint = Color.Black,
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


