package com.example.seizureguard.profile

import ProfileViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileCreationForm(profileViewModel: ProfileViewModel) {
    val userName by profileViewModel.userName.collectAsState()
    val userEmail by profileViewModel.userEmail.collectAsState()
    val birthDate by profileViewModel.birthdate.collectAsState()
    val uri by profileViewModel.profilePictureUri.collectAsState()
    val password by profileViewModel.pwd.collectAsState()
    val epi_type by profileViewModel.epi_type.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp)
    ) {

        Text(
            text = "Create Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        ProfilePicturePicker(profileViewModel = profileViewModel)

        Spacer(modifier = Modifier.height(8.dp))

        ProfileTextField(
            value = userName, onValueChange = {
                profileViewModel.saveProfile(
                    it, userEmail, birthDate, uri, password, epi_type
                )
            }, label = "Name"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileTextField(
            value = userEmail, onValueChange = {
                profileViewModel.saveProfile(
                    userName, it, birthDate, uri, password, epi_type
                )
            }, label = "Email"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date of Birth field
        BirthDateField(birthDate = birthDate, onClick = { showDatePicker = !showDatePicker })

        // Date Picker Modal
        if (showDatePicker) {
            DatePickerModal(onDateSelected = { selectedDateMillis ->
                profileViewModel.saveProfile(userName,
                    userEmail,
                    selectedDateMillis?.let { convertMillisToDate(it) } ?: "",
                    uri,
                    password,
                    epi_type
                )
                showDatePicker = false
            }, onDismiss = { showDatePicker = false })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Epilespy Type field
        EpilepsyTypeField(
            value = epi_type,
            onValueChange = {
                Log.d("ProfileCreationForm", "Epilepsy Type: $it")
                profileViewModel.saveProfile(
                    userName, userEmail, birthDate, uri, password, it,
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordTextField(password = password, onValueChange = {
            profileViewModel.saveProfile(
                userName, userEmail, birthDate, uri, it, epi_type
            )
        })

    }
}

@Composable
fun ProfileTextField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = customTextFieldColors()
    )
}

@Composable
fun BirthDateField(
    birthDate: String, onClick: () -> Unit
) {
    OutlinedTextField(
        value = birthDate,
        onValueChange = {}, // Read-only field
        label = { Text("Date of Birth") },
        readOnly = true, // Prevent manual input
        trailingIcon = {
            IconButton(onClick = { onClick() }) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = "Select date",
                    tint = Color.hsl(270f, 0.61f, 0.24f)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() }, // Ensures the entire TextField is clickable
        colors = customTextFieldColors(),
        enabled = false,
        shape = RoundedCornerShape(16.dp)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = {
            onDateSelected(datePickerState.selectedDateMillis)
            onDismiss()
        }) {
            Text("OK")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }) {
        DatePicker(state = datePickerState)
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color.Transparent,
    unfocusedContainerColor = Color.hsl(266f, 0.92f, 0.95f).copy(0.6f),
    focusedBorderColor = Color.hsl(272f, 0.61f, 0.34f),
    unfocusedLabelColor = Color.hsl(272f, 0.61f, 0.34f),
    focusedLabelColor = Color.hsl(272f, 0.61f, 0.34f),
    unfocusedTextColor = Color.hsl(270f, 0.61f, 0.24f),
    focusedTextColor = Color.hsl(270f, 0.61f, 0.24f),
    disabledTextColor = Color.hsl(270f, 0.61f, 0.24f),
    disabledBorderColor = Color.Transparent,
    disabledPlaceholderColor = Color.hsl(272f, 0.61f, 0.34f),
    disabledLabelColor = Color.hsl(272f, 0.61f, 0.34f),
    disabledContainerColor = Color.hsl(266f, 0.92f, 0.95f).copy(0.6f),
)


@Composable
fun PasswordTextField(password: String, onValueChange: (String) -> Unit) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onValueChange,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, description)
            }
        },
        colors = customTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun EpilepsyTypeField(value: String, onValueChange: (String) -> Unit, label: String = "Epilepsy Type") {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Focal", "Generalized", "Focal + Generalized", "Unknown")
    var selectedText by remember { mutableStateOf(value) }
    var dropDownWidth by remember { mutableStateOf(0) }

    val icon = if (expanded) Icons.Filled.ArrowDropDown
    else Icons.Filled.ArrowDropDown

    Column {
        OutlinedTextField(
            value = selectedText,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged {
                    dropDownWidth = it.width
                },
            label = { Text("Epilepsy Type") },
            trailingIcon = {
                Icon(icon, "contentDescription", Modifier.clickable { expanded = !expanded })
            },
            colors = customTextFieldColors(),
            shape = RoundedCornerShape(16.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { dropDownWidth.toDp() }).background(Color.White),
        ) {
            suggestions.forEach { label ->
                DropdownMenuItem(
                    onClick = { selectedText = label; onValueChange(selectedText); expanded = false },
                    text = { Text(label) })
            }
        }
    }
}