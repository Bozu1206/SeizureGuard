package com.epfl.ch.seizureguard.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle

@Composable
fun ProfileCreationForm(profile: Profile) {
    val focusManager = LocalFocusManager.current
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var epi_type by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { focusManager.clearFocus() }
                )
            }
    ) {
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF5722),
                Color(0xFFFF9800),
                Color(0xFFFFC107),
            ),
            startX = 0f,
            endX = 900f
        )

        Text(
            text = buildAnnotatedString {
                append("Tell us more about ")
                withStyle(
                    style = SpanStyle(
                        brush = gradient,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append("yourself!")
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfilePicturePicker(profile = profile)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                ProfileTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        profile.name = it
                    },
                    label = "Name"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ProfileTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        profile.email = it
                    },
                    label = "Email"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Birthdate field & Date Picker
                BirthDateField(
                    birthDate = birthDate,
                    onClick = { showDatePicker = true }
                )
                if (showDatePicker) {
                    DatePickerModal(
                        onDateSelected = { selectedDateMillis ->
                            birthDate = selectedDateMillis?.let { convertMillisToDate(it) } ?: ""
                            profile.birthdate = birthDate
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Epilepsy type dropdown
                EpilepsyTypeField(
                    value = epi_type,
                    onValueChange = {
                        epi_type = it
                        profile.epi_type = it
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Password field
                PasswordTextField(
                    password = password,
                    onValueChange = {
                        password = it
                        profile.pwd = it
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // TODO: Medication selector
            // item { ... }
        }
    }
}


@Composable
private fun textFieldModifier() = Modifier
    .fillMaxWidth()
    .height(56.dp)
    .padding(horizontal = 2.dp)
    .shadow(
        elevation = 10.dp,
        shape = RoundedCornerShape(16.dp),
        spotColor = Color.Black.copy(alpha = 0.2f)
    )
    .background(Color.White, RoundedCornerShape(16.dp))

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color.Transparent,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    focusedBorderColor = Color.Transparent,
    unfocusedLabelColor = Color.Gray,
    focusedLabelColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedTextColor = Color.Black,
    cursorColor = Color.Black,
    selectionColors = TextSelectionColors(
        handleColor = Color.Black,
        backgroundColor = Color.Black.copy(alpha = 0.2f)
    ),
    disabledTextColor = Color.Black,
    disabledBorderColor = Color.Transparent,
    disabledPlaceholderColor = Color.Gray,
    disabledLabelColor = Color.Gray,
    disabledContainerColor = Color.White
)

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Enter your $label", color = Color.Gray) },
        modifier = textFieldModifier(),
        shape = RoundedCornerShape(16.dp),
        colors = customTextFieldColors(),
        textStyle = LocalTextStyle.current.copy(
            color = Color.Black
        ),
        trailingIcon = {
            when (label) {
                "Name" -> Icon(
                    Icons.Default.Person,
                    contentDescription = "Name",
                    tint = Color.Black
                )

                "Email" -> Icon(
                    Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color.Black
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun BirthDateField(birthDate: String, onClick: () -> Unit) {
    OutlinedTextField(
        value = birthDate,
        onValueChange = {},
        placeholder = { Text("Date of Birth") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = "Select date",
                    tint = Color.Black,
                )
            }
        },
        modifier = textFieldModifier()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        colors = customTextFieldColors(),
        enabled = false,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(
            color = Color.Black
        )
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
fun PasswordTextField(password: String, onValueChange: (String) -> Unit) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onValueChange,
        placeholder = { Text("Password", color = Color.Gray) },
        modifier = textFieldModifier(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (passwordVisible) "Hide password" else "Show password"
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, description, tint = Color.Black)
            }
        },
        colors = customTextFieldColors(),
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(
            color = Color.Black
        ),

        )
}

@Composable
fun EpilepsyTypeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Epilepsy Type"
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Focal", "Generalized", "Focal + Generalized", "Unknown")
    var selectedText by remember { mutableStateOf(value) }
    var dropDownWidth by remember { mutableStateOf(0) }

    Column {
        OutlinedTextField(
            value = selectedText,
            onValueChange = { },
            modifier = textFieldModifier()
                .clickable { expanded = true }
                .onSizeChanged { dropDownWidth = it.width },
            placeholder = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Open menu",
                    tint = Color.Black
                )
            },
            colors = customTextFieldColors(),
            shape = RoundedCornerShape(16.dp),
            readOnly = true,
            enabled = false
        )

        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .offset(y = 4.dp)
        ) {
            DropdownMenu(
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { dropDownWidth.toDp() })
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                suggestions.forEach { label ->
                    DropdownMenuItem(
                        onClick = {
                            selectedText = label
                            onValueChange(selectedText)
                            expanded = false
                        },
                        text = {

                            Text(
                                text = label,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        },
                        modifier = Modifier.background(Color.White)
                    )
                }
            }
        }
    }
}


