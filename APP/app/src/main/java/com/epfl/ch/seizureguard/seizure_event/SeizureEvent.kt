package com.epfl.ch.seizureguard.seizure_event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.profile.ProfileTextField
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.google.accompanist.flowlayout.FlowRow

data class DefaultState(
    val type: String = "Focal",
    val duration: Int = 0,
    val severity: Int = 2,
    val triggers: List<String> = emptyList(),
    val timestamp: Long = -1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSeizureEventModal(
    onDismiss: () -> Unit,
    onClick: (SeizureEvent) -> Unit,
    label: String = "Log a Seizure",
    default: DefaultState = DefaultState()
) {
    var selectedOption by remember { mutableStateOf(default.type) }
    var duration by remember { mutableStateOf(default.duration.toString()) }
    var severity by remember { mutableStateOf(default.severity.toFloat()) }
    val selectedTriggers = remember { mutableStateListOf(*default.triggers.toTypedArray()) }


    val options = listOf("Focal", "Generalized", "Unknown")
    val triggerOptions = listOf("Stress", "Lack of Sleep", "Flashing Lights", "Other")

    val sliderColor = when {
        severity <= 2f -> Color(0xFF4CAF50)
        severity <= 3f -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            DropdownField(
                label = "Select Seizure Type",
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it }
            )

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (minutes)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )


            Column {
                Text("Severity")
                Slider(
                    value = severity,
                    onValueChange = { severity = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = sliderColor,
                        activeTrackColor = sliderColor,
                        inactiveTrackColor = sliderColor.copy(alpha = 0.3f)
                    )
                )
                Text(
                    text = "Selected Severity: ${severity.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = sliderColor
                )
            }

            MultiSelectChips(
                options = triggerOptions,
                selectedOptions = selectedTriggers,
                onSelectionChanged = { selected ->
                    if (!selectedTriggers.contains(selected)) selectedTriggers.add(selected)
                }
            )

            val seizureType = selectedOption
            val seizureEvent = SeizureEvent(
                type = seizureType,
                duration = duration.toIntOrNull() ?: 0,
                severity = severity.toInt(),
                triggers = selectedTriggers,
                timestamp = System.currentTimeMillis()
            )

            Button(
                onClick = { onClick(seizureEvent) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Event")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    text = { Text(option) }
                )
            }
        }
    }
}


@Composable
fun MultiSelectChips(
    options: List<String>,
    selectedOptions: MutableList<String>,
    onSelectionChanged: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable {
                        if (isSelected) selectedOptions.remove(option)
                        else onSelectionChanged(option)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = option,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

data class SeizureEvent(
    val type: String = "",
    val duration: Int = 0,
    val severity: Int = 0,
    val triggers: List<String> = emptyList(),
    var timestamp: Long = 0
)

@Preview(showBackground = true)
@Composable
fun PreviewLogSeizureEventModal() {
    LogSeizureEventModal(
        onDismiss = {},
        onClick = { /* Handle SeizureEvent */ },
        label = "Preview Log Seizure",
        default = DefaultState()
    )
}