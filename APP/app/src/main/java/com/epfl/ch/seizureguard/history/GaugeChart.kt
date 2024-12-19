package com.epfl.ch.seizureguard.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GaugeChart(
    modifier: Modifier = Modifier,
    value: Int,
    maxValue: Int = 5,
    severityColors: List<Color> = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Dark Orange
        Color(0xFFF44336)  // Red
    )
) {
    val segmentAngle = 180f / maxValue
    val sweepAngle = value * segmentAngle

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Text(text = "Severity: $value", style = MaterialTheme.typography.labelSmall)
        Canvas(
            modifier = modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val arcSize = Size(size.width, size.width)
            val startAngle = 180f // Start from the left edge of the circle

            // Draw the full background arc
            drawArc(
                color = Color.LightGray,
                startAngle = startAngle,
                sweepAngle = 180f,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = 20f)
            )

            // Draw the active arc based on the value
            drawArc(
                color = severityColors[value - 1],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = 20f)
            )
        }
    }
}
