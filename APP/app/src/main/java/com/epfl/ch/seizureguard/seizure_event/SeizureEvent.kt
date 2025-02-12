package com.epfl.ch.seizureguard.seizure_event

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

/**
 * Represent the default state for the SeizureEvent modal.
 */
data class DefaultState(
    val type: String = "Focal",
    val duration: Int = 0,
    val severity: Int = 2,
    val triggers: List<String> = emptyList(),
    val timestamp: Long = -1,
    val location: SeizureLocation? = null
)

data class SeizureLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toPair(): Pair<Double, Double> = Pair(latitude, longitude)

    companion object {
        fun fromPair(pair: Pair<Double, Double>): SeizureLocation =
            SeizureLocation(pair.first, pair.second)

        fun fromAndroidLocation(location: Location): SeizureLocation =
            SeizureLocation(location.latitude, location.longitude)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSeizureEventModal(
    context: Context,
    onDismiss: () -> Unit,
    onClick: (SeizureEvent) -> Unit,
    label: String = "Log a Seizure",
    default: DefaultState = DefaultState()
) {
    val type by remember { mutableStateOf(default.type) }
    val duration by remember { mutableStateOf(default.duration) }
    val severity by remember { mutableStateOf(default.severity) }
    val selectedTriggers =
        remember { mutableStateListOf<String>().apply { addAll(default.triggers) } }

    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var isInitialLocationSet by remember { mutableStateOf(default.location != null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            )
        )
    }
    var mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false
            )
        )
    }

    var selectedOption by remember { mutableStateOf(type) }
    var durationStr by remember { mutableStateOf(duration.toString()) }
    var severityFloat by remember { mutableStateOf(severity.toFloat()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (default.location != null) {
            location = default.location.toPair()
            isInitialLocationSet = true
        }

        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocation == PackageManager.PERMISSION_GRANTED ||
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = true
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Get device location if no location is set
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !isInitialLocationSet) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (LocationManagerCompat.isLocationEnabled(locationManager)) {
                try {
                    val lastKnownLocation =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    lastKnownLocation?.let {
                        location = Pair(it.latitude, it.longitude)
                        isInitialLocationSet = true
                    }
                } catch (e: SecurityException) {
                    // Handle permission denial
                }
            }
        }
        mapProperties = mapProperties.copy(isMyLocationEnabled = hasLocationPermission)
    }


    val options = listOf("Focal", "Generalized", "Unknown")
    val triggerOptions = listOf("Stress", "Lack of Sleep", "Flashing Lights", "Other")

    val sliderColor = when {
        severityFloat <= 2f -> Color(0xFF4CAF50)
        severityFloat <= 3f -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }

    suspend fun updateLocationAndCamera(
        newLocation: Pair<Double, Double>,
        cameraPositionState: com.google.maps.android.compose.CameraPositionState
    ) {
        location = newLocation
        val newCameraPosition = CameraPosition.Builder()
            .target(LatLng(newLocation.first, newLocation.second))
            .zoom(15f)
            .build()
        cameraPositionState.animate(
            update = CameraUpdateFactory.newCameraPosition(newCameraPosition),
            durationMs = 500
        )
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val previewCameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.Builder()
                        .target(LatLng(46.5191, 6.5668))
                        .zoom(15f)
                        .build()
                }

                LaunchedEffect(location) {
                    val targetLocation = location
                    val newCameraPosition =
                        targetLocation?.let { LatLng(it.first, targetLocation.second) }?.let {
                            CameraPosition.Builder()
                                .target(it)
                                .zoom(15f)
                                .build()
                        }
                    newCameraPosition?.let { CameraUpdateFactory.newCameraPosition(it) }?.let {
                        previewCameraPositionState.animate(
                            update = it,
                            durationMs = 500
                        )
                    }
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(5.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    cameraPositionState = previewCameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapClick = { latLng ->
                        scope.launch {
                            updateLocationAndCamera(
                                Pair(latLng.latitude, latLng.longitude),
                                previewCameraPositionState
                            )
                        }
                    }
                ) {
                    location?.let { loc ->
                        Marker(
                            state = MarkerState(position = LatLng(loc.first, loc.second)),
                            title = "Seizure Location"
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Location button
                    Card(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            scope.launch {
                                val locationManager =
                                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                if (hasLocationPermission && LocationManagerCompat.isLocationEnabled(
                                        locationManager
                                    )
                                ) {
                                    try {
                                        val lastKnownLocation =
                                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                                ?: locationManager.getLastKnownLocation(
                                                    LocationManager.NETWORK_PROVIDER
                                                )

                                        lastKnownLocation?.let {
                                            val newPos = LatLng(it.latitude, it.longitude)
                                            previewCameraPositionState.animate(
                                                update = CameraUpdateFactory.newLatLngZoom(
                                                    newPos,
                                                    15f
                                                ),
                                                durationMs = 500
                                            )
                                        }
                                    } catch (e: SecurityException) {
                                        // Handle permission denial
                                    }
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Center on my location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Fullscreen button
                    Card(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = { showMap = true }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Expand Map",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom in button
                    Card(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            scope.launch {
                                previewCameraPositionState.animate(
                                    update = CameraUpdateFactory.zoomIn(),
                                    durationMs = 300
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Zoom out button
                    Card(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            scope.launch {
                                previewCameraPositionState.animate(
                                    update = CameraUpdateFactory.zoomOut(),
                                    durationMs = 300
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            DropdownField(
                label = "Select Seizure Type",
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it }
            )

            OutlinedTextField(
                value = durationStr,
                onValueChange = { durationStr = it },
                label = { Text("Duration (minutes)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Severity")
                Slider(
                    value = severityFloat,
                    onValueChange = { severityFloat = it },
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
                    text = "Selected Severity: ${severityFloat.toInt()}",
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
                duration = durationStr.toIntOrNull() ?: 0,
                severity = severityFloat.toInt(),
                triggers = selectedTriggers.toList(),
                timestamp = System.currentTimeMillis(),
                location = location?.let { SeizureLocation.fromPair(it) }
                    ?: SeizureLocation(46.5191, 6.5668)
            )

            Button(
                onClick = {
                    onClick(seizureEvent)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Event")
            }
        }
    }

    if (showMap) {
        FullScreenMapDialog(
            onDismiss = { showMap = false },
            onLocationSelected = { lat, lon ->
                location = Pair(lat, lon)
            },
            initialLocation = location
        )
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
    var timestamp: Long = 0,
    val location: SeizureLocation = SeizureLocation()
)

@Composable
fun FullScreenMapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    initialLocation: Pair<Double, Double>?
) {
    val scope = rememberCoroutineScope()
    var currentPosition by remember {
        mutableStateOf<LatLng?>(initialLocation?.let {
            LatLng(
                it.first,
                it.second
            )
        })
    }
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            )
        )
    }
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = true
            )
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(LatLng(46.5191, 6.5668))
            .zoom(15f)
            .build()
    }

    fun animateCamera(latLng: LatLng) {
        scope.launch {
            val newPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(17f)
                .build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(newPosition),
                durationMs = 700
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = { latLng ->
                    currentPosition = latLng
                    onLocationSelected(latLng.latitude, latLng.longitude)
                    animateCamera(latLng)
                }
            ) {
                currentPosition?.let { pos ->
                    Marker(
                        state = MarkerState(position = pos),
                        title = "Selected Location"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Location button
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    onClick = {
                        scope.launch {
                            val lastLocation = currentPosition ?: LatLng(46.5191, 6.5668)
                            animateCamera(lastLocation)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.LocationOn,
                            contentDescription = "Center on my location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom in button
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.zoomIn(),
                                durationMs = 300
                            )
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Zoom out button
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.zoomOut(),
                                durationMs = 300
                            )
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .shadow(
                        elevation = 2.dp,
                        spotColor = Color.Black.copy(alpha = 0.15f),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Done")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewLogSeizureEventModal() {
    LogSeizureEventModal(
        context = androidx.compose.ui.platform.LocalContext.current,
        onDismiss = {},
        onClick = { /* Handle SeizureEvent */ },
        label = "Preview Log Seizure",
        default = DefaultState()
    )
}