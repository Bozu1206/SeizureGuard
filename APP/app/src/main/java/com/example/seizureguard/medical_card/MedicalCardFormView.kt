package com.example.seizureguard.medical_card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seizureguard.seizure_event.DropdownField
import com.example.seizureguard.wallet_manager.GoogleWalletToken

import com.google.wallet.button.WalletButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalCardForm(onWalletButtonClick: (GoogleWalletToken.PassRequest) -> Unit) {
    val epilepsyTypes = listOf("Generalized", "Focal", "Generalized + Focal", "Unknown")
    val drugs = listOf(
        "Lamotrigine",
        "Levetiracetam",
        "Valproate",
        "Carbamazepine",
        "Phenytoin",
        "Topiramate",
        "Gabapentin",
        "Pregabalin",
        "Clobazam",
        "Clonazepam",
        "Vigabatrin",
        "Perampanel",
        "Rufinamide",
        "Stiripentol",
        "Cannabidiol",
        "Fenfluramine",
        "Lacosamide",
        "Zonisamide",
        "Eslicarbazepine",
        "Oxcarbazepine",
        "Sodium valproate",
        "Ethosuximide",
        "Acetazolamide",
        "Piracetam",
        "Sulthiame",
        "Tiagabine",
        "Vigabatrin",
        "Zonisamide",
        "Cannabidiol",
        "Clobazam",
        "Clonazepam",
        "Diazepam",
        "Nitrazepam",
        "Phenobarbital"
    )

    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf(drugs[0]) }
    var selectedEpilepsy by remember { mutableStateOf("Focal") }
    var shouldShowDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create your Medical Card") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Enter your Name"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            OutlinedTextField(
                value = birthDate,
                onValueChange = { },
                label = { Text("Date of Birth") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { shouldShowDatePicker = !shouldShowDatePicker }) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = "Select date"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )


            if (shouldShowDatePicker) {
                DatePickerModal(
                    onDateSelected = { selectedDateMillis ->
                        birthDate = selectedDateMillis?.let { convertMillisToDate(it) } ?: ""
                    },
                    onDismiss = { shouldShowDatePicker = false }
                )

            }

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Emergency Contact") }, // Here we should maybe start an intent to get the contact from the phone
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            DropdownField(
                label = "Epilepsy type",
                options = epilepsyTypes,
                selectedOption = selectedEpilepsy,
                onOptionSelected = { selectedEpilepsy = it })

            DropdownField(
                label = "Medications",
                options = drugs,
                selectedOption = medications,
                onOptionSelected = { medications = it })

            Spacer(modifier = Modifier.weight(1f))

            WalletButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val request = GoogleWalletToken.PassRequest(
                        patientName = name,
                        emergencyContact = emergencyContact,
                        seizureType = selectedEpilepsy,
                        medication = medications,
                        birthdate = birthDate
                    )
                    onWalletButtonClick(request)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Preview
@Composable
fun MedicalCardFormPreview() {
    MedicalCardForm({})
}
