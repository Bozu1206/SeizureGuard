package com.epfl.ch.seizureguard.medication_tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import java.util.UUID
import androidx.compose.foundation.layout.FlowRow
import java.time.LocalTime

@Composable
fun MedicationTrackerScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    navController: NavController
) {
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showEditMedicationDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    val profile by profileViewModel.profileState.collectAsState()

    // Get selected date's medications and logs
    val selectedDateStart = selectedDate.atStartOfDay()
    val selectedDateEnd = selectedDate.atTime(23, 59, 59)

    // Create a key for forcing recomposition when medications are taken
    var medicationUpdateKey by remember { mutableStateOf(0) }

    // Function to handle medication taking with persistence
    val handleMedicationTaken = { medicationId: String ->
        profileViewModel.logMedication(
            medicationId = medicationId,
            timestamp = selectedDate.atTime(LocalDateTime.now().toLocalTime())
        )
        medicationUpdateKey++ // Force recomposition
    }

    // Function to handle medication taking with details
    val handleMedicationTakenWithDetails = { logEntry: MedicationLog ->
        profileViewModel.logMedicationWithDetails(logEntry)
        medicationUpdateKey++ // Force recomposition
    }

    // Function to handle medication deletion
    val handleMedicationDelete = { medicationId: String ->
        profileViewModel.removeMedication(medicationId)
        medicationUpdateKey++ // Force recomposition
    }

    // Function to handle medication editing
    val handleMedicationEdit = { medication: Medication ->
        profileViewModel.updateMedication(medication)
        medicationUpdateKey++ // Force recomposition
    }

    // Function to handle adding new medication
    val handleMedicationAdd = { medication: Medication ->
        profileViewModel.addMedication(medication)
        medicationUpdateKey++ // Force recomposition
    }

    val selectedDateLogs = remember(selectedDate, profile.medicationLogs, medicationUpdateKey) {
        profile.medicationLogs.filter { log ->
            log.timestamp.isAfter(selectedDateStart) && log.timestamp.isBefore(selectedDateEnd)
        }
    }

    val scheduledMedications = remember(profile.medications) {
        profile.medications.filter { medication ->
            medication.frequency == "Daily" || medication.frequency == "As Needed"
        }
    }

    val asNeededMedications = remember(scheduledMedications) {
        scheduledMedications.filter { it.frequency == "As Needed" }
    }

    // Calculate progress for selected date's medications
    val dailyMedications = remember(scheduledMedications, selectedDateLogs, medicationUpdateKey) {
        scheduledMedications.filter { it.frequency == "Daily" }
            .map { medication ->
                medication.copy(
                    logs = selectedDateLogs.filter { it.medicationId == medication.id }
                )
            }
    }

    val takenMedications = remember(selectedDateLogs, medicationUpdateKey) {
        selectedDateLogs.map { it.medicationId }.toSet()
    }
    
    // Calculate total required intakes and completed intakes for selected date
    val totalRequiredIntakes = remember(dailyMedications) {
        dailyMedications.sumOf { medication -> medication.timeOfDay.size }
    }

    val completedIntakes = remember(selectedDateLogs, dailyMedications, medicationUpdateKey) {
        selectedDateLogs.count { log ->
            dailyMedications.any { it.id == log.medicationId }
        }
    }
    
    val progressPercentage = remember(totalRequiredIntakes, completedIntakes) {
        if (totalRequiredIntakes > 0) {
            completedIntakes.toFloat() / totalRequiredIntakes
        } else {
            null  // Return null when there are no medications instead of 1f
        }
    }

    // Calculate progress for a specific date
    val calculateProgressForDate = { date: LocalDate ->
        val dateStart = date.atStartOfDay()
        val dateEnd = date.atTime(23, 59, 59)
        
        val dateLogs = profile.medicationLogs.filter { log ->
            log.timestamp.isAfter(dateStart) && log.timestamp.isBefore(dateEnd)
        }
        
        val dailyMedsForDate = scheduledMedications.filter { it.frequency == "Daily" }
        val totalRequired = dailyMedsForDate.sumOf { it.timeOfDay.size }
        val completed = dateLogs.count { log ->
            dailyMedsForDate.any { it.id == log.medicationId }
        }
        
        if (totalRequired > 0) {
            completed.toFloat() / totalRequired
        } else {
            null  // Return null when there are no medications
        }
    }

    // Calculate progress for the selected date
    val selectedDateProgress = remember(selectedDate, profile.medicationLogs, medicationUpdateKey) {
        calculateProgressForDate(selectedDate)
    }

    // Calculate progress for each visible date
    val dateProgressMap = remember(profile.medicationLogs, medicationUpdateKey) {
        val progressMap = mutableMapOf<LocalDate, Float?>()
        val startDate = today.minusDays(15)
        val endDate = today.plusDays(15)
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            progressMap[currentDate] = calculateProgressForDate(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        progressMap
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back_button),
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground)
                            .padding(4.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.medication_tracker),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showAddMedicationDialog = true },
                ) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(R.string.add_medication),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF9800),
                                        Color(0xFFFFC107)
                                    )
                                )
                            )
                            .padding(4.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Weekly calendar with progress
            item {
                WeeklyCalendar(
                    today = today,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    dateProgressMap = dateProgressMap
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Show message when no medications are registered
            if (dailyMedications.isEmpty() && asNeededMedications.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = "No medications registered yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Tap the + button in the top right corner to add your first medication",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Daily Medications Section
            if (dailyMedications.isNotEmpty()) {
                item {
                    Text(
                        text = if (selectedDate == today) "Today's Medications" else "Medications for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(
                    items = dailyMedications,
                    key = { medication -> "${medication.id}-${medicationUpdateKey}" }
                ) { medication ->
                    // Count actual takes for this medication on the selected date
                    val medicationTakes = selectedDateLogs.count { log -> log.medicationId == medication.id }
                    MedicationCard(
                        medication = medication,
                        currentTakes = medicationTakes,
                        selectedDate = selectedDate,
                        onTaken = { logEntry: MedicationLog? ->
                            if (logEntry != null) {
                                handleMedicationTakenWithDetails(logEntry)
                            } else {
                                handleMedicationTaken(medication.id)
                            }
                        },
                        onEdit = { medicationToEdit = medication; showEditMedicationDialog = true },
                        onDelete = { handleMedicationDelete(medication.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // As Needed Medications section
            if (asNeededMedications.isNotEmpty()) {
                item {
                    Text(
                        text = "As Needed Medications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(
                    items = asNeededMedications,
                    key = { medication -> "${medication.id}-${medicationUpdateKey}" }
                ) { medication ->
                    val medicationTakes = selectedDateLogs.count { log -> log.medicationId == medication.id }
                    MedicationCard(
                        medication = medication,
                        currentTakes = medicationTakes,
                        selectedDate = selectedDate,
                        onTaken = { logEntry: MedicationLog? ->
                            if (logEntry != null) {
                                handleMedicationTakenWithDetails(logEntry)
                            } else {
                                handleMedicationTaken(medication.id)
                            }
                        },
                        onEdit = { medicationToEdit = medication; showEditMedicationDialog = true },
                        onDelete = { handleMedicationDelete(medication.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

        }
    }

    if (showAddMedicationDialog) {
        AddMedicationDialog(
            onDismiss = { showAddMedicationDialog = false },
            onAdd = { medication ->
                handleMedicationAdd(medication)
                showAddMedicationDialog = false
            }
        )
    }

    if (showEditMedicationDialog && medicationToEdit != null) {
        EditMedicationDialog(
            medication = medicationToEdit!!,
            onDismiss = {
                showEditMedicationDialog = false
                medicationToEdit = null
            },
            onSave = { updatedMedication ->
                handleMedicationEdit(updatedMedication)
                showEditMedicationDialog = false
                medicationToEdit = null
            }
        )
    }
}

@Composable
fun WeeklyCalendar(
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    dateProgressMap: Map<LocalDate, Float?>
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = 40.dp
    val visibleItems = 1000
    val centerIndex = visibleItems / 2

    val listState = rememberLazyListState(centerIndex)

    LaunchedEffect(Unit) {
        listState.scrollToItem(centerIndex)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = screenWidth / 2 - itemWidth / 2),
            horizontalArrangement = Arrangement.Center
        ) {
            items(
                count = visibleItems,
                key = { it }
            ) { index ->
                val offset = index - centerIndex
                val date = today.plusDays(offset.toLong())
                val isToday = date == today
                val isSelected = date == selectedDate
                val progress = dateProgressMap[date]

                DayItem(
                    date = date,
                    isToday = isToday,
                    isSelected = isSelected,
                    progressPercentage = progress,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    progressPercentage: Float? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = Color(0xFF66AFE9) // Brighter blue for better visibility
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
        Box(
            modifier = Modifier.size(25.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle with border
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(1.dp, Color.Black.copy(alpha = 0.6f)),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isDark)
                            Color.DarkGray.copy(alpha = 0.1f)
                        else
                            Color.LightGray.copy(alpha = 0.1f)
                    )
            )

            // Progress fill with segments - only show if we have medications for this day
            if (progressPercentage != null && progressPercentage > 0f) {
                // Calculate number of segments (assuming each take is 20% for visual clarity)
                val numberOfSegments = 5
                val segmentHeight = 30f / numberOfSegments
                val filledSegments = (progressPercentage * numberOfSegments).toInt()
                
                // Draw filled segments
                for (i in 0 until filledSegments) {
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        fillColor.copy(alpha = 0.4f),
                                        fillColor
                                    ),
                                    startY = 30f - (i + 1) * segmentHeight,
                                    endY = 30f - i * segmentHeight
                                )
                            )
                    )
                }
            }

            // Selected or today indicator with enhanced border
            if (isSelected || isToday) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                )
            }

            // Content (number or checkmark)
            if (progressPercentage != null && progressPercentage >= 1f) {
                // Show checkmark with green background for completed days
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Show day number
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun MedicationSummarySection(
    medications: List<Medication>,
    onEditMedication: (Medication) -> Unit,
    onDeleteMedication: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        medications.forEach { medication ->
            MedicationSummaryCard(
                medication = medication,
                onEdit = { onEditMedication(medication) },
                onDelete = { onDeleteMedication(medication.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit
) {
    var name by remember { mutableStateOf(medication.name) }
    var dosage by remember { mutableStateOf(medication.dosage) }
    var frequency by remember { mutableStateOf(medication.frequency) }
    var shape by remember { mutableStateOf(medication.shape) }
    var timeOfDay by remember { mutableStateOf(medication.timeOfDay) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    
    val frequencies = listOf("Daily", "As Needed")
    val shapes = listOf("Pill", "Capsule", "Liquid", "Injection", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Medication") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = { expandedFrequency = !expandedFrequency }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        frequencies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    frequency = option
                                    expandedFrequency = false
                                    // Clear scheduled times if switching to As Needed
                                    if (option == "As Needed") {
                                        timeOfDay = emptyList()
                                    }
                                }
                            )
                        }
                    }
                }

                // Scheduled Times section (only show for Daily frequency)
                if (frequency == "Daily") {
                    Text(
                        text = "Scheduled Times",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Display current scheduled times
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = 3
                    ) {
                        timeOfDay.forEach { time ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text(time.format(DateTimeFormatter.ofPattern("h:mm a"))) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            timeOfDay = timeOfDay.filter { it != time }
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove time",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            )
                        }

                        // Add time button
                        FilledTonalButton(
                            onClick = { showTimePickerDialog = true },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add time")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Time")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        medication.copy(
                            name = name,
                            dosage = dosage,
                            frequency = frequency,
                            shape = shape,
                            timeOfDay = timeOfDay
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Time picker dialog
    if (showTimePickerDialog) {
        TimePickerDialog(
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { hour, minute ->
                // Create LocalDateTime for today with the selected time
                val newTime = LocalDateTime.now()
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0)
                timeOfDay = (timeOfDay.toList() + newTime).sortedBy { it.toLocalTime() }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(
                    value = hour,
                    onValueChange = { hour = it },
                    range = 0..23
                )
                Text(":")
                NumberPicker(
                    value = minute,
                    onValueChange = { minute = it },
                    range = 0..59
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(hour, minute) }) {
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicationCard(
    medication: Medication,
    currentTakes: Int,
    selectedDate: LocalDate,
    onTaken: (MedicationLog?) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Calculate total required takes
    val totalRequired = if (medication.frequency == "Daily") medication.timeOfDay.size else 1
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with medication name and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (medication.frequency == "Daily") {
                        Text(
                            text = "Takes: $currentTakes of $totalRequired",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit medication",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete medication",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Medication details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Dosage: ${medication.dosage}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Type: ${medication.shape}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Frequency: ${medication.frequency}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Take medication button with counter
                if (medication.frequency == "Daily" || medication.frequency == "As Needed") {
                    Button(
                        onClick = { onTaken(null) },
                        enabled = when {
                            medication.frequency == "As Needed" -> true
                            else -> currentTakes < totalRequired
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                currentTakes >= totalRequired -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.primary
                            },
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (medication.frequency == "Daily") {
                                Text(
                                    text = "$currentTakes/$totalRequired",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                imageVector = if (currentTakes >= totalRequired) 
                                    Icons.Default.Check 
                                else 
                                    Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    medication.frequency == "As Needed" -> "Take"
                                    currentTakes >= totalRequired -> "Done"
                                    else -> "Take"
                                }
                            )
                        }
                    }
                }
            }

            if (medication.frequency == "Daily") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scheduled times:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    medication.timeOfDay.forEach { time ->
                        FilterChip(
                            selected = false,
                            onClick = { },
                            label = {
                                try {
                                    time.format(DateTimeFormatter.ofPattern("h:mm a"))
                                } catch (e: Exception) {
                                    null
                                }?.let {
                                    Text(it)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Medication") },
            text = { Text("Are you sure you want to delete ${medication.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) }
        ) {
            Icon(Icons.Default.ExpandLess, "Increment")
        }
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) }
        ) {
            Icon(Icons.Default.ExpandMore, "Decrement")
        }
    }
}





















































