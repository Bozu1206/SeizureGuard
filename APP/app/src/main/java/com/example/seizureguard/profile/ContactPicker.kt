package com.example.seizureguard.profile

import EmergencyContact
import ProfileViewModel
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.util.UUID


@Composable
fun getContactPicker(context: Context, profileViewModel: ProfileViewModel): ManagedActivityResultLauncher<Intent, ActivityResult> {
    var contactName by remember { mutableStateOf<String?>(null) }
    var contactPhone by remember { mutableStateOf<String?>(null) }
    var contactPicture by remember { mutableStateOf<String?>(null) }

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex =
                                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val phoneIndex =
                                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val pictureIndex =
                                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                            contactName = it.getString(nameIndex)
                            contactPhone = it.getString(phoneIndex)
                            contactPicture = it.getString(pictureIndex)

                            var localUri: Uri? = null
                            if (contactPicture != null) {
                               localUri = copyImageToInternalStorage(
                                    context,
                                    Uri.parse(contactPicture),
                                    "contact_picture_${UUID.randomUUID()}.jpg"
                                )
                            }
                            val contact = EmergencyContact(
                                name = contactName!!,
                                phone = contactPhone!!,
                                photoUri = localUri?.toString()
                            )

                            profileViewModel.saveEmergencyContact(contact = contact)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ContactPicker(context: Context, profileViewModel: ProfileViewModel) {
    val activity = LocalContext.current as Activity
    val contactPicker = getContactPicker(context, profileViewModel)

    val contactList = profileViewModel.emergencyContacts.collectAsState()
    val contactName = contactList.value.firstOrNull()?.name
    val contactPhone = contactList.value.firstOrNull()?.phone
    val contactPicture = contactList.value.firstOrNull()?.photoUri

    if (contactName != null && contactPhone != null) {
        Text(
            text = "Emergency Contact",
            modifier = Modifier
                .padding(vertical = 16.dp)
                .wrapContentSize(Alignment.TopStart),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
        ContactDetails(
            contactName = contactName,
            contactPhone = contactPhone,
            contactPicture = contactPicture,
            onClick = {}
        )
    } else {
        Button(onClick = {
            if (hasContactPermission(context)) {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                }
                contactPicker.launch(intent)
            } else {
                requestContactPermission(context, activity)
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Emergency Contact",
                modifier = Modifier.size(24.dp)
            )
            Text(text = "Add Emergency Contact")
        }

        Text(
            text = "You can add more contacts later.",
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

@Composable
fun ContactDetails(
    contactName: String,
    contactPhone: String,
    contactPicture: String?,
    onClick: () -> Unit
) {
    OnboardingEmergencyContactCard(
        name = contactName,
        phone = contactPhone,
        picture = if (contactPicture != null) Uri.parse(contactPicture) else null,
        onClick = onClick
    )
}


@Composable
fun OnboardingEmergencyContactCard(
    name: String,
    phone: String,
    picture: Uri?,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color
                    .hsl(266f, 0.92f, 0.95f)
                    .copy(0.6f), shape = RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, Color.hsl(272f, 0.61f, 0.34f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
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
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}


fun hasContactPermission(context: Context): Boolean {
    // on below line checking if permission is present or not.
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED;
}

fun requestContactPermission(context: Context, activity: Activity) {
    // on below line if permission is not granted requesting permissions.
    if (!hasContactPermission(context)) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), 1)
    }
}


