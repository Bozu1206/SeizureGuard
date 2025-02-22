package com.epfl.ch.seizureguard.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeizureStatsScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val profile by profileViewModel.profileState.collectAsState()
    val seizures = profile.pastSeizures

    // Capture localized strings for "N/A" and "Unknown"
    val noDataString = stringResource(R.string.no_data_string)
    val unknownTriggerString = stringResource(R.string.unknown_trigger)

    // Compute stats with localized strings passed in
    val stats = remember(seizures, noDataString, unknownTriggerString) {
        calculateSeizureStats(
            seizures = seizures,
            noDataString = noDataString,
            unknownTriggerString = unknownTriggerString
        )
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
                IconButton(onClick = onBack) {
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
                    text = stringResource(R.string.seizure_statistics),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                // Placeholder to maintain spacing
                Box(modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        // Strings for stat titles
        val totalSeizuresText = stringResource(R.string.total_seizures)
        val averageDurationText = stringResource(R.string.average_duration)
        val averageSeverityText = stringResource(R.string.average_severity)
        val mostCommonTypeText = stringResource(R.string.most_common_type)
        val mostCommonTriggerText = stringResource(R.string.most_common_trigger)
        val noneTriggerText = stringResource(R.string.none_trigger)
        val minutesAbbrev = stringResource(R.string.minutes_abbrev) // "min"
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard(
                    title = totalSeizuresText,
                    value = stats.totalSeizures.toString(),
                    icon = Icons.Default.Numbers,
                    iconTint = Color(0xFF2196F3)
                )
            }

            item {
                StatCard(
                    title = averageDurationText,
                    value = "${String.format("%.1f", stats.averageDuration)} $minutesAbbrev",
                    icon = Icons.Default.Timer,
                    iconTint = Color(0xFF4CAF50)
                )
            }

            item {
                StatCard(
                    title = averageSeverityText,
                    value = String.format("%.1f", stats.averageSeverity),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = Color(0xFFF44336)
                )
            }

            item {
                StatCard(
                    title = mostCommonTypeText,
                    value = stats.mostCommonType,
                    icon = Icons.Default.Category,
                    iconTint = Color(0xFF9C27B0)
                )
            }

            item {
                val triggerValue = stats.mostCommonTrigger ?: noneTriggerText

                StatCard(
                    title = mostCommonTriggerText,
                    value = triggerValue,
                    icon = Icons.Default.Warning,
                    iconTint = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Data class to hold computed stats about seizures.
 */
private data class SeizureStats(
    val totalSeizures: Int,
    val averageDuration: Double,
    val averageSeverity: Double,
    val mostCommonType: String,
    val mostCommonTrigger: String?
)

/**
 * Non-composable function to calculate stats. We pass in localized
 * fallback strings instead of calling `stringResource(...)` here.
 */
private fun calculateSeizureStats(
    seizures: List<SeizureEvent>,
    noDataString: String,        // e.g. "N/A"
    unknownTriggerString: String // e.g. "Unknown"
): SeizureStats {
    if (seizures.isEmpty()) {
        return SeizureStats(
            totalSeizures = 0,
            averageDuration = 0.0,
            averageSeverity = 0.0,
            mostCommonType = noDataString,    // "N/A"
            mostCommonTrigger = null
        )
    }

    val totalSeizures = seizures.size
    val averageDuration = seizures.map { it.duration }.average()
    val averageSeverity = seizures.map { it.severity }.average()

    val typeFrequency = seizures.groupBy { it.type }
    val mostCommonType = typeFrequency.maxByOrNull { it.value.size }?.key ?: unknownTriggerString

    val allTriggers = seizures.flatMap { it.triggers }
    val mostCommonTrigger = allTriggers
        .groupBy { it }
        .maxByOrNull { it.value.size }
        ?.key

    return SeizureStats(
        totalSeizures = totalSeizures,
        averageDuration = averageDuration,
        averageSeverity = averageSeverity,
        mostCommonType = mostCommonType,
        mostCommonTrigger = mostCommonTrigger
    )
}