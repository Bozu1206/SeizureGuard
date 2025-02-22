package com.epfl.ch.seizureguard.medication_tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (Medication) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var medicationName by remember { mutableStateOf("") }
    var medicationDosage by remember { mutableStateOf("") }
    var selectedShape by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var frequency by remember { mutableStateOf("Daily") }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTimes by remember { mutableStateOf(listOf(LocalTime.of(9, 0))) }
    var showConfirmation by remember { mutableStateOf(false) }

    val medicationForms = listOf(
        MedicationForm("tablet", Color(0xFFFF9800), "Round tablet"),
        MedicationForm("capsule", Color(0xFF2196F3), "Elongated capsule"),
        MedicationForm("liquid", Color(0xFF4CAF50), "Liquid medicine"),
        MedicationForm("injection", Color(0xFFF44336), "Injectable"),
        MedicationForm("inhaler", Color(0xFF9C27B0), "Inhaler"),
        MedicationForm("patch", Color(0xFF795548), "Adhesive patch"),
        MedicationForm("powder", Color(0xFF607D8B), "Powder form"),
        MedicationForm("drops", Color(0xFF00BCD4), "Liquid drops"),
        MedicationForm("cream", Color(0xFFE91E63), "Topical cream")
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                selectedTime = time
                selectedTimes = selectedTimes + time
                showTimePicker = false
            }
        )
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = {
                Text(
                    text = "Confirm Medication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecapItem("Name", medicationName)
                    RecapItem("Dosage", medicationDosage)
                    RecapItem("Form", selectedShape.replaceFirstChar { it.uppercase() })
                    RecapItem("Frequency", frequency)
                    if (frequency == "Daily") {
                        RecapItem(
                            "Times", 
                            selectedTimes.joinToString(", ") { 
                                it.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")) 
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdd(
                            Medication(
                                id = UUID.randomUUID().toString(),
                                name = medicationName,
                                dosage = medicationDosage,
                                frequency = frequency,
                                timeOfDay = selectedTimes.map { it.atDate(java.time.LocalDate.now()) },
                                shape = selectedShape
                            )
                        )
                        onDismiss()
                    }
                ) {
                    Text("Add Medication")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Edit")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { 
            Spacer(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1) / 3f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

            // Step title
            Text(
                text = when (currentStep) {
                    0 -> "Add Medication Details"
                    1 -> "Choose Medication Form"
                    else -> "Set Schedule"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Step description
            Text(
                text = when (currentStep) {
                    0 -> "Enter the name and dosage of your medication"
                    1 -> "Select the form of your medication"
                    else -> "Choose when you need to take this medication"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Content based on current step
            when (currentStep) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = medicationName,
                            onValueChange = { medicationName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Medication Name") },
                            placeholder = { Text("e.g., Aspirin") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )
                        
                        OutlinedTextField(
                            value = medicationDosage,
                            onValueChange = { medicationDosage = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Dosage") },
                            placeholder = { Text("e.g., 200 mg") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
                1 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(medicationForms) { form ->
                            MedicationFormItem(
                                form = form,
                                isSelected = form.id == selectedShape,
                                onSelect = { selectedShape = form.id }
                            )
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "How often do you take this medication?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FrequencyChip(
                                    text = "Daily",
                                    selected = frequency == "Daily",
                                    onClick = { frequency = "Daily" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FrequencyChip(
                                    text = "As Needed",
                                    selected = frequency == "As Needed",
                                    onClick = { frequency = "As Needed" }
                                )
                            }
                        }

                        if (frequency == "Daily") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "What time do you take it?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedTimes.forEach { time ->
                                        TimeChip(
                                            time = time,
                                            onClick = { showTimePicker = true },
                                            onRemove = {
                                                if (selectedTimes.size > 1) {
                                                    selectedTimes = selectedTimes - time
                                                }
                                            }
                                        )
                                    }
                                    
                                    if (selectedTimes.size < 4) {
                                        IconButton(
                                            onClick = { showTimePicker = true },
                                            modifier = Modifier
                                                .height(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add time",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        when (currentStep) {
                            2 -> {
                                showConfirmation = true
                            }
                            else -> currentStep++
                        }
                    },
                    enabled = when (currentStep) {
                        0 -> medicationName.isNotBlank() && medicationDosage.isNotBlank()
                        1 -> selectedShape.isNotBlank()
                        else -> true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (currentStep == 2) "Review" else "Next")
                }
            }
        }
    }
}

data class MedicationForm(
    val id: String,
    val color: Color,
    val description: String
)

@Composable
fun MedicationFormItem(
    form: MedicationForm,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected)
                        if (isDark)
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1B5E20),  // Dark green
                                    Color(0xFF2E7D32)   // Medium green
                                )
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE8F5E9)  // Very light green
                                )
                            )
                    else
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = form.id.first().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected)
                        if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = form.id.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun TimeChip(
    time: LocalTime,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var period by remember { mutableStateOf("AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time display
                Text(
                    text = String.format(
                        "%02d:%02d %s",
                        if (hour == 12) 12 else hour % 12,
                        minute,
                        period
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Hour selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hour")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                hour = if (hour <= 1) 12 else hour - 1
                            }
                        ) {
                            Text("-")
                        }
                        Text(
                            text = "${if (hour == 12) 12 else hour % 12}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { 
                                hour = if (hour >= 12) 1 else hour + 1
                            }
                        ) {
                            Text("+")
                        }
                    }
                }

                // Minute selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minute")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                minute = if (minute <= 0) 59 else minute - 1
                            }
                        ) {
                            Text("-")
                        }
                        Text(
                            text = String.format("%02d", minute),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { 
                                minute = if (minute >= 59) 0 else minute + 1
                            }
                        ) {
                            Text("+")
                        }
                    }
                }

                // AM/PM selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Period")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = period == "AM",
                            onClick = { period = "AM" },
                            label = { Text("AM") }
                        )
                        FilterChip(
                            selected = period == "PM",
                            onClick = { period = "PM" },
                            label = { Text("PM") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val adjustedHour = when {
                        period == "PM" && hour != 12 -> hour + 12
                        period == "AM" && hour == 12 -> 0
                        else -> hour
                    }
                    onConfirm(LocalTime.of(adjustedHour, minute))
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrequencyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surface,
        contentColor = if (selected) 
            MaterialTheme.colorScheme.onPrimary 
        else 
            MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecapItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
} 