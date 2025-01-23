package com.epfl.ch.seizureguard.homescreen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.guidelines.GuidelinesModal
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.res.stringResource
import com.epfl.ch.seizureguard.R
import androidx.navigation.NavController

@Composable
private fun formatDate(timestamp: Long): String {
    // Retrieve the pattern from resources in a non-Composable function:
    val pattern = LocalContext.current.getString(R.string.date_time_pattern)
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun HomeScreen(
    seizureEventViewModel: SeizureEventViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavController
) {
    Scaffold(
        bottomBar = {
            QuickActionsSection(
                context = LocalContext.current,
                profileViewModel = profileViewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeSection(profileViewModel = profileViewModel)
                Spacer(modifier = Modifier.height(8.dp))
                DailyTipsSection()
                Spacer(modifier = Modifier.height(8.dp))
                HealthMetricsSection(profileViewModel = profileViewModel)
                Spacer(modifier = Modifier.height(8.dp))
                RecentActivitySection(profileViewModel = profileViewModel)
            }
        }
    }

    val showSeizureLogging by seizureEventViewModel.showSeizureLoggingDialog.collectAsState()
    val showGuidelines by seizureEventViewModel.showGuidelinesDialog.collectAsState()

    if (showSeizureLogging) {
        LogSeizureEventModal(
            onDismiss = { seizureEventViewModel.hideSeizureLoggingDialog() },
            onClick = {
                seizureEventViewModel.hideSeizureLoggingDialog()
                profileViewModel.addSeizure(it)
            }
        )
    }

    if (showGuidelines) {
        GuidelinesModal(
            onDismiss = {
                seizureEventViewModel.hideGuidelinesDialog()
            }
        )
    }
}

@Composable
fun WelcomeSection(
    profileViewModel: ProfileViewModel
) {
    val profile = profileViewModel.profileState.collectAsState()

    // Capture string resources:
    val welcomeText = stringResource(R.string.welcome_text)
    val overviewText = stringResource(R.string.overview_of_health)

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF5722),
            Color(0xFFFFC107),
            Color(0xFFFF9800),
        ),
        startX = 0f,
        endX = 900f
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        // "Welcome, " + userName
                        append(welcomeText)
                        withStyle(
                            style = SpanStyle(
                                brush = gradient,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append(" ${profile.value.name.split(" ")[0]}!")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = overviewText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun HealthMetricsSection(profileViewModel: ProfileViewModel) {
    // Capture string resources:
    val seizuresLabel = stringResource(R.string.seizures_label)
    val lastWeekText = stringResource(R.string.last_week)
    val heartRateText = stringResource(R.string.heart_rate)
    val bpmText = stringResource(R.string.bpm)
    val sleepLabel = stringResource(R.string.sleep_label)
    val hoursPerNight = stringResource(R.string.hours_per_night)

    val profile = profileViewModel.profileState.collectAsState()
    val numbersOfSeizureLastWeek =
        profile.value.pastSeizures.count {
            // Count number of seizures in the past week
            it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Example: "Seizures" / "last week"
            MetricCard(
                title = seizuresLabel,
                value = numbersOfSeizureLastWeek.toString(),
                unit = lastWeekText
            )

            // Not real data; just placeholders -- idea is to show different metrics
            MetricCard(title = heartRateText, value = "76", unit = bpmText)
            MetricCard(title = sleepLabel, value = "6.5", unit = hoursPerNight)
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, unit: String) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(colorScheme.surface, RoundedCornerShape(16.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(100.dp)
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    // Typically we handle coloring based on label; let's localize the label logic as well:
    // We'll keep the color logic but also map them to stringResource usage externally.
    val buttonColors = when (label) {
        // "Emergency"
        stringResource(R.string.emergency) -> Color(0xFFFF5252)
        // "Log Event"
        stringResource(R.string.log_event) -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = buttonColors.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp),
                tint = buttonColors
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuickActionsSection(
    context: Context,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    // Capture string resources:
    val emergencyText = stringResource(R.string.emergency)
    val logEventText = stringResource(R.string.log_event)
    val guidelinesText = stringResource(R.string.guidelines_label)

    var showLogEventModal by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionButton(icon = Icons.Default.Call, label = emergencyText) {
                onEmergencyCall(context)
            }
            QuickActionButton(icon = Icons.Default.Add, label = logEventText) {
                showLogEventModal = true
            }
            QuickActionButton(icon = Icons.Default.Info, label = guidelinesText) {
                showGuidelines = true
            }

            if (showGuidelines) {
                GuidelinesModal(onDismiss = { showGuidelines = false })
            }

            if (showLogEventModal) {
                LogSeizureEventModal(
                    onDismiss = { showLogEventModal = false },
                    onClick = { seizureEvent ->
                        showLogEventModal = false
                        profileViewModel.addSeizure(seizureEvent)
                    }
                )
            }
        }
    }
}

@Composable
fun GraphSection() {
    val seizureTrendsText = stringResource(R.string.seizure_trends)
    // Example day labels (no string-array; define individually):
    val monLabel = stringResource(R.string.mon)
    val tueLabel = stringResource(R.string.tue)
    val wedLabel = stringResource(R.string.wed)
    val thuLabel = stringResource(R.string.thu)
    val friLabel = stringResource(R.string.fri)
    val satLabel = stringResource(R.string.sat)
    val sunLabel = stringResource(R.string.sun)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = seizureTrendsText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Pass the localized day labels to the graph:
        SeizureTrendGraph(
            dataPoints = listOf(3, 2, 4, 5, 1, 0, 2),
            labels = listOf(monLabel, tueLabel, wedLabel, thuLabel, friLabel, satLabel, sunLabel)
        )
    }
}

@Composable
fun SeizureTrendGraph(dataPoints: List<Int>, labels: List<String>) {
    val maxY = (dataPoints.maxOrNull() ?: 1) + 1

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val color = Color(0xFF678840)
        val graphWidth = size.width
        val graphHeight = size.height
        val xStep = graphWidth / (dataPoints.size - 1)
        val yStep = graphHeight / maxY

        // Draw background lines
        for (i in 0..maxY) {
            val y = graphHeight - (i * yStep)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(graphWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw data lines
        dataPoints.forEachIndexed { index, value ->
            if (index < dataPoints.size - 1) {
                val startX = index * xStep
                val startY = graphHeight - (value * yStep)
                val endX = (index + 1) * xStep
                val endY = graphHeight - (dataPoints[index + 1] * yStep)

                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = index * xStep
            val y = graphHeight - (value * yStep)
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun RecentActivitySection(profileViewModel: ProfileViewModel) {
    val lastSeizuresText = stringResource(R.string.last_seizures)
    val profile = profileViewModel.profileState.collectAsState()
    val recentSeizures = profile.value.pastSeizures
        .sortedByDescending { it.timestamp }
        .take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = lastSeizuresText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (recentSeizures.isEmpty()) {
            EmptyStateCard()
        } else {
            recentSeizures.forEach { seizure ->
                RecentActivityCard(seizure = seizure)
            }
        }
    }
}

@Composable
fun RecentActivityCard(seizure: SeizureEvent) {
    // Localize string for "Seizure Event"
    val seizureEventText = stringResource(R.string.seizure_event)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .shadow(
                elevation = 8.dp,
                shape = CardDefaults.shape,
                spotColor = Color.Black.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = seizureEventText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(seizure.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = seizure.type,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary
                )
                Text(
                    text = "${seizure.duration} minutes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DailyTipsSection() {
    // Localize all tips individually
    val tip1 = stringResource(R.string.tip_1)
    val tip2 = stringResource(R.string.tip_2)
    val tip3 = stringResource(R.string.tip_3)
    val tip4 = stringResource(R.string.tip_4)
    val dailyTipText = stringResource(R.string.daily_tip)

    // Build a list from them
    val tips = listOf(tip1, tip2, tip3, tip4)
    val currentTip = remember { tips.random() }
    val yellow = Color(0xFFFFC107)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = yellow.copy(0.1f)
        ),
        border = BorderStroke(0.5.dp, yellow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = yellow,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = dailyTipText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentTip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    val noActivityText = stringResource(R.string.no_recent_activity)
    val appearHereText = stringResource(R.string.recent_seizure_events_will_appear_here)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = noActivityText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = appearHereText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

