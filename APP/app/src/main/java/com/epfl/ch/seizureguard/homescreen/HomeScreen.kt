package com.epfl.ch.seizureguard.homescreen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.guidelines.GuidelinesModal
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.tools.onEmergencyCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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
    val haptic = LocalHapticFeedback.current
    var showSuccess by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (showSuccess) 16.dp else 0.dp, animationSpec = tween(durationMillis = 300),
        label = ""
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {
            Scaffold(
                bottomBar = {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp, bottom = 8.dp)
                        ) {
                            MonitoringPill(
                                isMonitoring = profileViewModel.isInferenceRunning.collectAsState().value,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 16.dp,
                        ) {
                            QuickActionsSection(
                                context = LocalContext.current,
                                profileViewModel = profileViewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.background),
                                onLogSuccess = { showSuccess = true }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WelcomeSection(
                            profileViewModel = profileViewModel,
                        )
                        DailyTipsSection()
                        HealthMetricsSection(profileViewModel = profileViewModel)
                        WeeklyOverviewSection(profileViewModel = profileViewModel)
                        // RecentActivitySection(profileViewModel = profileViewModel)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                    scaleIn(initialScale = 0.5f, animationSpec = tween(durationMillis = 300)) +
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 300)
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                    scaleOut(targetScale = 1.5f, animationSpec = tween(durationMillis = 300)) +
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 300)
                    ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Draw particle effect in the background
                ParticleEffect(Modifier.fillMaxSize())
                // Then draw the check icon and text on top
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Event Logged Successfully",
                        tint = Color(0xff4CAF50),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Event Logged Successfully",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xff4CAF50),
                    )
                }
            }
        }
    }

    if (showSuccess) {
        LaunchedEffect(showSuccess) {
            kotlinx.coroutines.delay(1500)
            showSuccess = false
        }
    }

    val showSeizureLogging by seizureEventViewModel.showSeizureLoggingDialog.collectAsState()
    val showGuidelines by seizureEventViewModel.showGuidelinesDialog.collectAsState()

    val ctx = LocalContext.current
    if (showSeizureLogging) {
        LogSeizureEventModal(
            context = ctx,
            onDismiss = { seizureEventViewModel.hideSeizureLoggingDialog() },
            onClick = { seizureEvent ->
                seizureEventViewModel.hideSeizureLoggingDialog()
                profileViewModel.addSeizure(seizureEvent)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showSuccess = true
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
    profileViewModel: ProfileViewModel,
) {
    val profile = profileViewModel.profileState.collectAsState()
    val welcomeText = stringResource(R.string.welcome_text)
    val overviewText = stringResource(R.string.overview_of_health)

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFB306),
            Color(0xFFFFA726),
            Color(0xFFFF9800),
        ),
        startX = 0f,
        endX = 900f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Text(
            text = buildAnnotatedString {
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = overviewText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun HealthMetricsSection(profileViewModel: ProfileViewModel) {
    val seizuresLabel = stringResource(R.string.seizures_label)
    val lastWeekText = stringResource(R.string.last_week)
    val heartRateText = stringResource(R.string.heart_rate)
    val bpmText = stringResource(R.string.bpm)
    val sleepLabel = stringResource(R.string.sleep_label)
    val hoursPerNight = stringResource(R.string.hours_per_night)

    val profile = profileViewModel.profileState.collectAsState()
    val numbersOfSeizureLastWeek =
        profile.value.pastSeizures.count {
            it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        }

    val primaryOrange = Color(0xFFFF9800)

    // Simulate changing heart rate between 65-85 bpm
    var heartRate by remember { mutableStateOf(75) }
    // Fixed sleep duration with slight variation
    val sleepDuration = 7.2f

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            heartRate = (65..105).random()
        }
    }

    var showHeartRateChart by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnhancedMetricCard(
                title = seizuresLabel,
                value = numbersOfSeizureLastWeek.toString(),
                unit = lastWeekText,
                trend = if (numbersOfSeizureLastWeek > 3) "↑" else "↓",
                trendColor = if (numbersOfSeizureLastWeek > 3) Color(0xFF757575) else primaryOrange,
                modifier = Modifier
                    .weight(1f)
            )

            EnhancedMetricCard(
                title = heartRateText,
                value = heartRate.toString(),
                unit = bpmText,
                trend = if (heartRate > 80) "↑" else if (heartRate < 70) "↓" else "→",
                trendColor = when {
                    heartRate > 80 -> Color(0xFF757575)
                    heartRate < 70 -> Color(0xFF4CAF50)
                    else -> primaryOrange
                },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showHeartRateChart = true }
            )

            EnhancedMetricCard(
                title = sleepLabel,
                value = String.format("%.1f", sleepDuration),
                unit = hoursPerNight,
                trend = "→",
                trendColor = primaryOrange,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }

    if (showHeartRateChart) {
        MetricDetailSheet(
            title = heartRateText,
            onDismiss = { showHeartRateChart = false }
        ) {
            HeartRateChart()
        }
    }
}

@Composable
fun MonitoringPill(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val activeColor = Color(0xFFFF9800)
    val inactiveColor = Color(0xFF757575)

    Surface(
        modifier = modifier
            .padding(1.dp)  // Add padding to prevent shadow clipping
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .border(
                width = 1.dp,
                color = if (isMonitoring) activeColor else inactiveColor,
                shape = CircleShape
            )
            .clip(CircleShape),
        color = if (isMonitoring) activeColor.copy(0.08f) else inactiveColor.copy(0.08f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isMonitoring) activeColor.copy(alpha = if (isMonitoring) dotAlpha else 1f) else inactiveColor,
                        CircleShape
                    )
            )
            Text(
                text = if (isMonitoring) "Monitoring" else "Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMonitoring) activeColor else inactiveColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EnhancedMetricCard(
    title: String,
    value: String,
    unit: String,
    trend: String,
    trendColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = trend,
                    style = MaterialTheme.typography.titleMedium,
                    color = trendColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    context: Context,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    onLogSuccess: () -> Unit
) {
    val emergencyText = stringResource(R.string.emergency)
    val logEventText = stringResource(R.string.log_event)
    val guidelinesText = stringResource(R.string.guidelines_label)

    val haptic = LocalHapticFeedback.current
    var showLogEventModal by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEmergencyCall(context)
                },
            color = Color(0xFFC43D3D),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = emergencyText,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Log Event button with darker color
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showLogEventModal = true },
                color = Color(0xFF424242)  // Dark gray background
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = logEventText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Guidelines button with border
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp)  // Add padding to prevent border clipping
                    .height(40.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showGuidelines = true },
                color = colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = guidelinesText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Keep existing modal visibility handlers
        AnimatedVisibility(
            visible = showLogEventModal,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            LogSeizureEventModal(
                context = context,
                onDismiss = { showLogEventModal = false },
                onClick = { seizureEvent ->
                    showLogEventModal = false
                    profileViewModel.addSeizure(seizureEvent)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLogSuccess()
                }
            )
        }

        AnimatedVisibility(
            visible = showGuidelines,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            GuidelinesModal(onDismiss = { showGuidelines = false })
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
                elevation = 4.dp,
                shape = CardDefaults.shape,
                spotColor = Color.Black.copy(alpha = 0.2f)
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
    val primaryOrange = Color(0xFFFF9800)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = primaryOrange.copy(0.08f)
        ),
        border = BorderStroke(0.5.dp, primaryOrange)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = primaryOrange,
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

@Composable
fun ParticleEffect(modifier: Modifier = Modifier) {
    data class Particle(
        val startAngle: Float,
        val speed: Float,
        val radius: Float,
        val color: Color
    )

    val particleCount = 30
    val random = remember { java.util.Random() }
    val particles = remember {
        List(particleCount) {
            Particle(
                startAngle = random.nextFloat() * 360f,
                speed = 200f + random.nextFloat() * 200f,
                radius = 4f + random.nextFloat() * 4f,
                color = Color(
                    red = random.nextInt(256),
                    green = random.nextInt(256),
                    blue = random.nextInt(256)
                )
            )
        }
    }

    val animationDuration = 1000L
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = elapsed / animationDuration.toFloat()
            if (elapsed > animationDuration) break
            kotlinx.coroutines.delay(16L)
        }
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        particles.forEach { particle ->
            val distance = particle.speed * progress
            val angleRad = Math.toRadians(particle.startAngle.toDouble())
            val offsetX = distance * kotlin.math.cos(angleRad).toFloat()
            val offsetY = distance * kotlin.math.sin(angleRad).toFloat()
            val alpha = 1f - progress
            drawCircle(
                color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = particle.radius,
                center = Offset(centerX + offsetX, centerY + offsetY)
            )
        }
    }
}

@Composable
fun WeeklyOverviewSection(
    profileViewModel: ProfileViewModel
) {
    val profile = profileViewModel.profileState.collectAsState()
    val currentTime = System.currentTimeMillis()
    val oneWeek = 7 * 24 * 60 * 60 * 1000L

    // Calculate stats
    val recentSeizures = profile.value.pastSeizures.filter {
        it.timestamp > currentTime - oneWeek
    }
    val lastSeizure = profile.value.pastSeizures.maxByOrNull { it.timestamp }
    val averageDuration =
        recentSeizures.map { it.duration }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val commonTriggers = recentSeizures
        .flatMap { it.triggers }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f),
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Last Seizure Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Last Seizure",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = lastSeizure?.let {
                            val days = (currentTime - it.timestamp) / (24 * 60 * 60 * 1000)
                            if (days == 0L) "Today" else "$days days ago"
                        } ?: "No data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Average Duration
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Avg Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (averageDuration > 0) "$averageDuration min" else "No data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            if (commonTriggers != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Most Common Trigger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = commonTriggers,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)
            ) {
                content()
            }
        }
    }
}

@Composable
fun HeartRateChart() {
    var selectedPoint by remember { mutableStateOf<Int?>(null) }

    val heartRateData = remember {
        mutableStateListOf<Int>().apply {
            addAll(List(24) { hour ->
                val baseRate = 70
                val dayVariation = if (hour in 8..20) 5 else -2
                val randomVariation = (-3..3).random()
                baseRate + dayVariation + randomVariation
            })
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val lastRate = heartRateData.last()
            val newRate = (lastRate + (-5..5).random()).coerceIn(65, 100)
            heartRateData.apply {
                removeAt(0)
                add(newRate)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 32.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = ((offset.x / size.width) * (heartRateData.size - 1))
                                .toInt()
                                .coerceIn(0, heartRateData.size - 1)
                            selectedPoint = index
                        },
                        onDrag = { change, _ ->
                            val index =
                                ((change.position.x / size.width) * (heartRateData.size - 1))
                                    .toInt()
                                    .coerceIn(0, heartRateData.size - 1)
                            selectedPoint = index
                        },
                        onDragEnd = {
                            selectedPoint = null
                        }
                    )
                }
        ) {
            val minRate = heartRateData.minOrNull() ?: 65
            val maxRate = heartRateData.maxOrNull() ?: 100
            val range = if (maxRate - minRate == 0) 1f else (maxRate - minRate).toFloat()
            val points = heartRateData.mapIndexed { index, rate ->
                val normalized = (rate - minRate) / range
                Offset(
                    x = (index.toFloat() / (heartRateData.size - 1)) * size.width,
                    y = size.height - normalized * size.height
                )
            }

            // Draw gradient under the line
            val gradientPath = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, size.height)
                close()
            }

            drawPath(
                path = gradientPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF9800).copy(alpha = 0.15f),
                        Color(0xFFFF9800).copy(alpha = 0.0f)
                    )
                )
            )

            // Draw the smooth line
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val current = points[i]
                    val next = points[i + 1]
                    val controlPoint1 = Offset(
                        x = current.x + (next.x - current.x) / 2f,
                        y = current.y
                    )
                    val controlPoint2 = Offset(
                        x = current.x + (next.x - current.x) / 2f,
                        y = next.y
                    )
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        next.x, next.y
                    )
                }
            }

            drawPath(
                path = path,
                color = Color(0xFFFF9800),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw points and highlight selected point
            points.forEachIndexed { index, point ->
                if (index % 4 == 0 || index == selectedPoint) {
                    val isSelected = index == selectedPoint
                    val radius = if (isSelected) 8.dp else 5.dp

                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = radius.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = (radius - 2.dp).toPx(),
                        center = point
                    )
                }
            }

            // Draw selected point value
            selectedPoint?.let { index ->
                val point = points[index]
                val value = heartRateData[index]
                val text = "$value bpm"
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FF9800")
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 14.sp.toPx()
                    isFakeBoldText = true
                }

                val bounds = android.graphics.Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                val bubblePadding = 8.dp.toPx()

                // Draw background bubble
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        point.x - bounds.width() / 2f - bubblePadding,
                        point.y - bounds.height() - 2 * bubblePadding
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        bounds.width().toFloat() + 2 * bubblePadding,
                        bounds.height().toFloat() + 2 * bubblePadding
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )

                // Draw text
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    point.x,
                    point.y - bubblePadding,
                    textPaint
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..5) {
                Text(
                    text = "${i * 4}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
