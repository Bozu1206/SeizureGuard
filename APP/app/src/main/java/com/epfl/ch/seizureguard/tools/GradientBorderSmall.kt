package com.epfl.ch.seizureguard.tools

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GradientBorderSmall(modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFFF11C4A), // 241, 28, 74
        Color(0xFFFF8001), // 255, 128, 1
        Color(0xFFFFF500), // 255, 245, 0
        Color(0xFFB4C51A), // 255, 245, 0
        Color(0xFF65FF6B), // 101, 255, 107
        Color(0xFF50D5FF), // 80, 213, 255
        Color(0xFF2250FB), // 34, 80, 251
        Color(0xFF7A0CFC), // 122, 12, 252
        Color(0xFFFE19C3), // 254, 25, 195
        Color(0xFFEE0D1E),  // 238, 13, 30
    )

    val overlayColors = listOf(
        Color(0xFF608BE6), // 72, 126, 233
        Color(0xFFDA4141)  // 219, 36, 36
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 45f,
        targetValue = 75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val alphaAnimation by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .blur(12.dp)
            .drawBehind {
                drawRoundRect(
                    brush = getAngularGradient(colors, size),
                    style = Stroke(width = pulseSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f)
                )

                drawRoundRect(
                    brush = Brush.verticalGradient(overlayColors),
                    style = Stroke(width = pulseSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(26f),
                    alpha = alphaAnimation
                )
            }
    )
}

private fun getAngularGradient(colors: List<Color>, size: androidx.compose.ui.geometry.Size): Brush {
    return Brush.sweepGradient(
        colors = colors,
        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    )
}

@Composable
@Preview(showBackground = true)
fun GradientBorderSmallPreview() {
    GradientBorderSmall(
        modifier = Modifier.size(200.dp)
    )
}

