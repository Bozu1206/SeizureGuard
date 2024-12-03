package com.example.seizureguard.homescreen

import android.content.Context
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seizureguard.guidelines.GuidelinesModal
import com.example.seizureguard.seizure_event.LogSeizureEventModal
import com.example.seizureguard.onEmergencyCall
import com.example.seizureguard.ui.theme.AppTheme

@Composable
fun HomeScreen(  homeScreenViewModel: HomeScreenViewModel = viewModel()) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome Section
            WelcomeSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Metrics
            HealthMetricsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Graph Section
            // GraphSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            QuickActionsSection(context)
        }
    }
}

@Composable
fun WelcomeSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Text(
                text = "Welcome, François!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Here's an overview of your health status",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun HealthMetricsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricCard(title = "Seizures", value = "2", unit = "last week")
            MetricCard(title = "Heart Rate", value = "76", unit = "bpm")
            MetricCard(title = "Sleep", value = "6.5", unit = "hrs/night")
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .background(
                colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun QuickActionsSection(context: Context) {
    var showLogEventModal by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false)}

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionButton(icon = Icons.Default.Call, label = "Emergency") {
                onEmergencyCall(context)
            }
            QuickActionButton(icon = Icons.Default.Add, label = "Log Event") {
                showLogEventModal = true
            }
            QuickActionButton(icon = Icons.Default.Info, label = "Guidelines") {
                showGuidelines = true
            }

            if (showGuidelines) {
                GuidelinesModal(
                    onDismiss = { showGuidelines = false }
                )
            }

            if (showLogEventModal) {
                LogSeizureEventModal(
                    onDismiss = { showLogEventModal = false }
                )
            }
        }
    }
}




@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(48.dp)
                .background(
                    colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GraphSection() {
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
            text = "Seizure Trends",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SeizureTrendGraph(
            dataPoints = listOf(3, 2, 4, 5, 1, 0, 2), // Dummy example
            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AppTheme {
        //HomeScreen({})
    }
}

