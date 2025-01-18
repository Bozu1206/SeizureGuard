package com.epfl.ch.seizureguard.history

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.format.DateTimeFormatter
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeizureStatsScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val profile by profileViewModel.profileState.collectAsState()
    val seizures = profile.pastSeizures

    val stats = remember(seizures) {
        calculateSeizureStats(seizures)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seizure Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard(
                    title = "Total Seizures",
                    value = stats.totalSeizures.toString(),
                    icon = Icons.Default.Numbers,
                    iconTint = Color(0xFF2196F3)
                )
            }

            item {
                StatCard(
                    title = "Average Duration",
                    value = "${String.format("%.1f", stats.averageDuration)} min",
                    icon = Icons.Default.Timer,
                    iconTint = Color(0xFF4CAF50)
                )
            }

            item {
                StatCard(
                    title = "Average Severity",
                    value = String.format("%.1f", stats.averageSeverity),
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFFF44336)
                )
            }

            item {
                StatCard(
                    title = "Most Common Type",
                    value = stats.mostCommonType,
                    icon = Icons.Default.Category,
                    iconTint = Color(0xFF9C27B0)
                )
            }

            item {
                StatCard(
                    title = "Most Common Trigger",
                    value = stats.mostCommonTrigger ?: "None",
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

private data class SeizureStats(
    val totalSeizures: Int,
    val averageDuration: Double,
    val averageSeverity: Double,
    val mostCommonType: String,
    val mostCommonTrigger: String?
)

private fun calculateSeizureStats(seizures: List<SeizureEvent>): SeizureStats {
    if (seizures.isEmpty()) {
        return SeizureStats(0, 0.0, 0.0, "N/A", null)
    }

    val totalSeizures = seizures.size
    val averageDuration = seizures.map { it.duration }.average()
    val averageSeverity = seizures.map { it.severity }.average()

    val typeFrequency = seizures.groupBy { it.type }
    val mostCommonType = typeFrequency.maxByOrNull { it.value.size }?.key ?: "Unknown"

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

@Composable 
private fun SeizureChart(
    seizures: List<SeizureEvent>,
    modifier: Modifier = Modifier
) {


    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seizure Frequency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
            }

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}
