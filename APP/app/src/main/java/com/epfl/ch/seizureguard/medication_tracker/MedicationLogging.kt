package com.epfl.ch.seizureguard.medication_tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationLoggingDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onLog: (MedicationLog) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var sideEffects by remember { mutableStateOf("") }
    var effectiveness by remember { mutableStateOf(5) }
    var mood by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Medication",
                style = MaterialTheme.typography.headlineSmall,
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
                Text(
                    text = "Taking ${medication.name} - ${medication.dosage}",
                    style = MaterialTheme.typography.titleMedium
                )

                // Effectiveness rating
                Column {
                    Text(
                        text = "How effective was this medication?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = effectiveness.toFloat(),
                        onValueChange = { effectiveness = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Not effective", style = MaterialTheme.typography.bodySmall)
                        Text("Very effective", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Mood rating
                Column {
                    Text(
                        text = "How do you feel?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = mood.toFloat(),
                        onValueChange = { mood = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Not good", style = MaterialTheme.typography.bodySmall)
                        Text("Very good", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Side effects
                OutlinedTextField(
                    value = sideEffects,
                    onValueChange = { sideEffects = it },
                    label = { Text("Side Effects") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val logEntry = MedicationLog(
                        medicationId = medication.id,
                        timestamp = LocalDateTime.now(),
                        effectiveness = effectiveness,
                        mood = mood,
                        sideEffects = sideEffects.takeIf { it.isNotBlank() },
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onLog(logEntry)
                }
            ) {
                Text("Log Medication")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 