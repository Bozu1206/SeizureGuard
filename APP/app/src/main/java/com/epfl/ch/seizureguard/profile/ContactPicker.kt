package com.epfl.ch.seizureguard.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun getContactPicker(
    context: Context,
    profileViewModel: ProfileViewModel
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    var contactName by remember { mutableStateOf<String?>(null) }
    var contactPhone by remember { mutableStateOf<String?>(null) }

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

                            contactName = it.getString(nameIndex)
                            contactPhone = it.getString(phoneIndex)

                            val contact = EmergencyContact(
                                name = contactName!!,
                                phone = contactPhone!!,
                                photoUri = null
                            )

                            if (profileViewModel.profileState.value.emergencyContacts.contains(
                                    contact
                                )
                            ) {
                                Toast.makeText(context, "Contact already added", Toast.LENGTH_SHORT)
                                    .show()
                                return@use
                            }

                            profileViewModel.updateEmergencyContacts(contact, isAdding = true)
                        }
                    }
                }
            }
        }
    )
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


