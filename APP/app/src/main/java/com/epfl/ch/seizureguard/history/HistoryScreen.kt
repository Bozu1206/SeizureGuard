package com.epfl.ch.seizureguard.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.DefaultState
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel
) {
    var editingSeizure by remember { mutableStateOf<SeizureEvent?>(null) }
    val profile by profileViewModel.profileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Seizure History",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            text = "No seizures recorded yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SeizureCard(
    seizure: SeizureEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val severityColor = when (seizure.severity) {
        1 -> Color(0xFF4CAF50)  // Vert - Léger
        2 -> Color(0xFF2196F3)  // Bleu - Modéré
        3 -> Color(0xFFFFA726)  // Orange - Sévère
        4 -> Color(0xFFE53935)  // Rouge - Très sévère
        else -> Color(0xFF9C27B0)  // Violet - Critique
    }

    val cardColor = if (isDark) {
        when (seizure.severity) {
            1 -> Color(0xFF1B2D1B)  // Fond vert sombre
            2 -> Color(0xFF1B2632)  // Fond bleu sombre
            3 -> Color(0xFF2D2319)  // Fond orange sombre
            4 -> Color(0xFF2D1B1B)  // Fond rouge sombre
            else -> Color(0xFF2D1B2D)  // Fond violet sombre
        }
    } else {
        when (seizure.severity) {
            1 -> Color(0xFFE8F5E9)  // Fond vert clair
            2 -> Color(0xFFE3F2FD)  // Fond bleu clair
            3 -> Color(0xFFFFF3E0)  // Fond orange clair
            4 -> Color(0xFFFFEBEE)  // Fond rouge clair
            else -> Color(0xFFF3E5F5)  // Fond violet clair
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 2.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = severityColor.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = if (isDark) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SeizureCardHeader(
                type = seizure.type,
                textColor = if (isDark) White.copy(0.9f) else Black.copy(0.8f),
                onEdit = onEdit,
                onDelete = onDelete,
                severityColor = severityColor
            )

            Divider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .alpha(if (isDark) 0.2f else 0.1f),
                color = severityColor
            )

            SeizureCardContent(
                seizure = seizure,
                textColor = if (isDark) White.copy(0.9f) else Black.copy(0.8f),
                severityColor = severityColor
            )
        }
    }
}

@Composable
private fun SeizureCardHeader(
    type: String,
    textColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    severityColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$type Seizure",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onEdit,
                icon = Icons.Default.Edit,
                tint = textColor.copy(0.5f)
            )
            IconButton(
                onClick = onDelete,
                icon = Icons.Default.RemoveCircle,
                tint = textColor.copy(0.5f)
            )
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
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(0.8f),
        )

        Text(
            text = "${seizure.duration} minutes long",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(0.8f),
        )

        Spacer(modifier = Modifier.height(12.dp))

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
private fun IconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color,
    contentDescription: String? = null
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .padding(4.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun Pill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = if (isSystemInDarkTheme()) 0.15f else 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = if (isSystemInDarkTheme()) 0.3f else 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.7f)
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EditSeizureDialog(
    seizure: SeizureEvent,
    onDismiss: () -> Unit,
    onEdit: (SeizureEvent) -> Unit
) {
    LogSeizureEventModal(
        onDismiss = onDismiss,
        onClick = { updatedSeizure ->
            onEdit(updatedSeizure.copy(timestamp = seizure.timestamp))
        },
        label = "Edit Seizure",
        default = DefaultState(
            type = seizure.type,
            duration = seizure.duration,
            severity = seizure.severity,
            triggers = seizure.triggers
        )
    )
}



