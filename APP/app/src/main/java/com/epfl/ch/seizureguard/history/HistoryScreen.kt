package com.epfl.ch.seizureguard.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.DefaultState
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import com.epfl.ch.seizureguard.seizure_event.SeizureLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    navController: NavController
) {
    var editingSeizure by remember { mutableStateOf<SeizureEvent?>(null) }
    val profile by profileViewModel.profileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.history_screen_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )

                        androidx.compose.material3.IconButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .size(48.dp),
                            onClick = { navController.navigate("seizure_stats") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = stringResource(R.string.view_statistics),
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        val sortedSeizures by remember(profile.pastSeizures) {
            derivedStateOf {
                profile.pastSeizures.sortedByDescending { it.timestamp }
            }
        }

        if (sortedSeizures.isEmpty()) {
            EmptyHistoryMessage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = sortedSeizures,
                    key = { it.timestamp }
                ) { seizure ->
                    SeizureCard(
                        seizure = seizure,
                        onEdit = { editingSeizure = seizure },
                        onDelete = { profileViewModel.removeSeizure(seizure) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }

        editingSeizure?.let { seizure ->
            EditSeizureDialog(
                seizure = seizure,
                onDismiss = { editingSeizure = null },
                onEdit = { updatedSeizure: SeizureEvent ->
                    profileViewModel.editSeizure(updatedSeizure, seizure)
                    editingSeizure = null
                }
            )
        }
    }
}

@Composable
private fun EmptyHistoryMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_seizures_recorded_yet),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SeizureCard(
    seizure: SeizureEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var showActions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showActions = true },
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) 
        else 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 0.9.dp,
            color = if (isDark) 
                Color.Gray.copy(alpha = 0.2f)
            else 
                Color.Gray.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark)
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Map section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Create a unique key that includes all relevant seizure data
                val mapKey = remember(seizure) {
                    "${seizure.timestamp}_${seizure.location.latitude}_${seizure.location.longitude}_${seizure.type}"
                }

                key(mapKey) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.Builder()
                            .target(LatLng(seizure.location.latitude, seizure.location.longitude))
                            .zoom(12f)
                            .build()
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = false,
                            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                                context,
                                R.raw.map_style_no_poi
                            ),
                            minZoomPreference = 3f,
                            maxZoomPreference = 21f,
                            isBuildingEnabled = false,
                            isIndoorEnabled = false,

                            ),
                        uiSettings = MapUiSettings(
                            compassEnabled = false,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false,
                            rotationGesturesEnabled = false,
                            scrollGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            zoomControlsEnabled = false,
                            zoomGesturesEnabled = false,
                            indoorLevelPickerEnabled = false
                        )
                    )
                }
            }

            // Content section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${seizure.type} Seizure",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        // Severity badge
                        val severityColor = when (seizure.severity) {
                            1 -> Color(0xFF4CAF50) // Green
                            2 -> Color(0xFF8BC34A) // Light Green
                            3 -> Color(0xFFFFC107) // Amber
                            4 -> Color(0xFFFF9800) // Orange
                            else -> Color(0xFFF44336) // Red
                        }
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = severityColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, severityColor)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${seizure.severity}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = severityColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = formatDateTime(seizure.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) White.copy(0.9f) else Black.copy(0.8f),
                    )
                }

                // Pills at the bottom
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    seizure.triggers.forEach { trigger ->
                        val pillColor =
                            if (isSystemInDarkTheme()) Color(0xFF9B82DB) else Color(0xFF6750A4)
                        Pill(
                            text = trigger,
                            color = pillColor
                        )
                    }
                }
            }
        }
    }

    if (showActions) {
        ModalBottomSheet(
            onDismissRequest = { showActions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { }  // Removes the default drag handle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Title
                Text(
                    text = "Seizure Actions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                // Edit option
                ListItem(
                    headlineContent = {
                        Text(
                            "Edit Seizure",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            "Modify details of this seizure event",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            onEdit()
                            showActions = false
                        }
                        .fillMaxWidth()
                )

                // Delete option
                ListItem(
                    headlineContent = {
                        Text(
                            "Delete",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE57373)
                        )
                    },
                    supportingContent = {
                        Text(
                            "Remove this seizure event permanently",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            onDelete()
                            showActions = false
                        }
                        .fillMaxWidth()
                )

                // Add some bottom padding
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeizureCardContent(
    seizure: SeizureEvent,
    textColor: Color,
    severityColor: Color
) {
    Column {
        Text(
            text = formatDateTime(seizure.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(0.8f),
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                seizure.triggers.forEach { trigger ->
                    Pill(
                        text = trigger,
                        color = severityColor
                    )
                }
            }
        }
    }
}

private fun formatDateTime(
    time: Long,
    zone: String = "Europe/Paris",
    format: String = "EEE, MMMM d K:mm a"
): String {
    val zoneId = ZoneId.of(zone)
    val instant = Instant.ofEpochMilli(time)
    val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
    return instant.atZone(zoneId).format(formatter)
}

@Composable
private fun Pill(
    text: String,
    color: Color
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        color = color.copy(alpha = if (isDark) 0.15f else 0.08f),
        shape = RoundedCornerShape(16.dp),
//        border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.5f else 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDark) color.copy(alpha = 0.9f) else color.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EditSeizureDialog(
    seizure: SeizureEvent,
    onDismiss: () -> Unit,
    onEdit: (SeizureEvent) -> Unit
) {
    val ctx = LocalContext.current

    LogSeizureEventModal(
        context = ctx,
        onDismiss = onDismiss,
        onClick = { updatedSeizure ->
            onEdit(
                seizure.copy(
                    type = updatedSeizure.type,
                    duration = updatedSeizure.duration,
                    severity = updatedSeizure.severity,
                    triggers = updatedSeizure.triggers,
                    location = updatedSeizure.location,
                    timestamp = seizure.timestamp
                )
            )
        },
        label = "Edit Seizure",
        default = DefaultState(
            type = seizure.type,
            duration = seizure.duration,
            severity = seizure.severity,
            triggers = seizure.triggers,
            location = seizure.location
        )
    )
}