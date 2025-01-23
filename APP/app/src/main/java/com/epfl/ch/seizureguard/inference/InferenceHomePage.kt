package com.epfl.ch.seizureguard.inference

import EEGChart
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.bluetooth.BluetoothViewModel
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.profile.ProfileViewModelFactory
import kotlin.math.abs
import kotlin.math.round

private val CardShape = RoundedCornerShape(16.dp)
private val CardElevation = 6.dp
private val DefaultPadding = 16.dp
private val SmallPadding = 8.dp
private val CardHeight = 120.dp

@Composable
fun InferenceHomePage(
    onPerformInference: () -> Unit,
    onPauseInference: () -> Unit,
    modifier: Modifier = Modifier,
    metricsViewModel: MetricsViewModel,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            LocalContext.current,
            (LocalContext.current.applicationContext as RunningApp)
        )
    )
) {
    val profile by profileViewModel.profileState.collectAsState()
    val isInferenceRunning by profileViewModel.isInferenceRunning.collectAsState()

    val context = LocalContext.current
    val runningApp = context.applicationContext as RunningApp
    val bluetoothViewModel = runningApp.bluetoothViewModel

    val debugMode = profile.isDebugEnabled
    // Is the BLE device connected?
    val isConnected by bluetoothViewModel.isConnected.observeAsState(initial = false)

    // Retrieve power-mode value from strings
    val powerModeValue = when (profile.powerMode) {
        context.getString(R.string.low_power_mode) ->
            context.resources.getInteger(R.integer.low_power_config)

        context.getString(R.string.normal_power_mode) ->
            context.resources.getInteger(R.integer.normal_power_config)

        context.getString(R.string.high_performance_mode) ->
            context.resources.getInteger(R.integer.high_performance_config)

        else ->
            context.resources.getInteger(R.integer.normal_power_config)
    }

    bluetoothViewModel.setPowerMode(powerModeValue.toByte())

    // Handle Bluetooth setup
    HandleBluetoothSetup(bluetoothViewModel)

    // Load metrics
    LaunchedEffect(Unit) {
        metricsViewModel.loadMetrics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DefaultPadding)
    ) {
        DashboardHeader()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MetricsSection(profileViewModel)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EEGChart(
                        debugMode,
                        isInferenceRunning = isInferenceRunning,
                    )
                    InferenceOverlay(
                        debugMode,
                        isConnected,
                        context,
                        isInferenceRunning = isInferenceRunning,
                        onStartInference = {
                            onPerformInference()
                        }
                    )
                }

                ActionButtonsSection(
                    debugMode = debugMode,
                    isConnected = isConnected,
                    isInferenceRunning = isInferenceRunning,
                    onPerformInference = { onPerformInference() },
                    onPauseInference = { onPauseInference() },
                    onScanDevices = { bluetoothViewModel.scanLeDevice() },
                    bluetoothViewModel = bluetoothViewModel
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Text(
        text = stringResource(R.string.dashboard_title),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = DefaultPadding),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun MetricsSection(profileViewModel: ProfileViewModel) {
    val metrics by profileViewModel.latestMetrics.collectAsState()
    BentoMetricsCard(
        metrics = if (metrics.f1 > -1.0) metrics else Metrics.defaultsModelMetrics(),
        profileViewModel = profileViewModel
    )
}

@Composable
private fun ActionButtonsSection(
    debugMode: Boolean,
    isConnected: Boolean,
    isInferenceRunning: Boolean,
    onPerformInference: () -> Unit,
    onPauseInference: () -> Unit,
    onScanDevices: () -> Unit,
    bluetoothViewModel: BluetoothViewModel
) {
    val context = LocalContext.current

    // Pre-capture string resources that will be used inside lambdas:
    val noDeviceConnectedText = stringResource(R.string.no_device_connected)
    val inferenceRunningText = stringResource(R.string.inference_running)
    val performInferenceText = stringResource(R.string.perform_inference)
    val scanningForDevicesText = stringResource(R.string.scanning_for_devices)
    val connectedDeviceFormat = stringResource(R.string.connected_device)
    val lookForDevicesText = stringResource(R.string.look_for_devices)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonHeight = 48.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SmallPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val inferenceButtonText = if (isInferenceRunning) {
                    inferenceRunningText
                } else {
                    performInferenceText
                }

                ActionButtonInference(
                    onClick = {
                        if (isInferenceRunning) {
                            onPauseInference()
                        } else {
                            if (!debugMode && !isConnected) {
                                Toast.makeText(
                                    context,
                                    noDeviceConnectedText,
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                onPerformInference()
                            }
                        }
                    },
                    icon = if (isInferenceRunning) Icons.Default.Autorenew else Icons.Default.PlayArrow,
                    text = inferenceButtonText,
                    isInferenceRunning = isInferenceRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                )
                Spacer(modifier = Modifier.width(8.dp))
                InferenceRunningIcon(
                    isInferenceRunning = isInferenceRunning,
                    buttonColors = ButtonDefaults.buttonColors(),
                    onClick = {
                        if (isInferenceRunning) {
                            onPauseInference()
                        } else {
                            if (!debugMode && !isConnected) {
                                Toast.makeText(
                                    context,
                                    noDeviceConnectedText,
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                onPerformInference()
                            }
                        }
                    },
                    modifier = Modifier.height(buttonHeight)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SmallPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scanButtonText = if (isConnected) {
                    // "Connected to %1$s"
                    String.format(connectedDeviceFormat, bluetoothViewModel.myDeviceName)
                } else {
                    lookForDevicesText
                }
                val scanButtonColors = if (isConnected) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF3EFD46))
                } else {
                    ButtonDefaults.buttonColors()
                }
                ActionButtonBLE(
                    onClick = {
                        if (!isConnected) {
                            onScanDevices()
                            Toast.makeText(context, scanningForDevicesText, Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    icon = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    text = scanButtonText,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    buttonColors = scanButtonColors
                )
                Spacer(modifier = Modifier.width(8.dp))

                DeviceConnectionIcon(
                    isConnected = isConnected,
                    buttonColors = scanButtonColors,
                    modifier = Modifier.height(buttonHeight)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonInference(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    isInferenceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rotating arrow")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotating arrow"
    )

    Button(
        onClick = onClick,
        shape = CardShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer(
                    rotationZ = if (isInferenceRunning) rotationAngle else 0f
                )
        )
        Spacer(modifier = Modifier.width(SmallPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionButtonBLE(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        shape = CardShape,
        colors = buttonColors,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(SmallPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BentoMetricsCard(
    metrics: Metrics,
    profileViewModel: ProfileViewModel
) {
    var showExtra by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sampleCount by profileViewModel.sampleCount.collectAsState()
    val isTrainReady by profileViewModel.isTrainReady.collectAsState()

    // Pre-capture string resource for training toast
    val trainingHasStartedText = stringResource(R.string.training_has_started)

    val canTrain by remember(sampleCount, isTrainReady) {
        derivedStateOf { sampleCount >= 100 && isTrainReady }
    }

    MetricsCardContent(
        metrics = metrics,
        canTrain = canTrain,
        onTrainClick = {
            profileViewModel.requestTraining()
            Toast.makeText(context, trainingHasStartedText, Toast.LENGTH_SHORT).show()
        },
        onShowDetails = { showExtra = true },
        profileViewModel = profileViewModel
    )

    if (showExtra) {
        MetricsDetailsSheet(
            metrics = metrics,
            onDismiss = { showExtra = false }
        )
    }
}

@Composable
private fun MetricsCardContent(
    metrics: Metrics,
    canTrain: Boolean,
    onTrainClick: () -> Unit,
    onShowDetails: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    val profile by profileViewModel.profileState.collectAsState()
    val sampleCount by profileViewModel.sampleCount.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(CardElevation, shape = RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFf2994a),
                        Color(0xFFf2c94c)
                    ),
                    startX = 0f,
                    endX = 900f
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(DefaultPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    title = stringResource(R.string.accuracy),
                    value = metrics.accuracy.toString(),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Adjust
                )
                Spacer(modifier = Modifier.width(24.dp))
                MetricItem(
                    title = stringResource(R.string.f1_score),
                    value = metrics.f1.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onShowDetails),
                    icon = Icons.Default.TaskAlt
                )
            }

            if (profile.isTrainingEnabled) {
                Spacer(modifier = Modifier.height(DefaultPadding))
                Button(
                    onClick = onTrainClick,
                    enabled = canTrain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                ) {
                    Text(
                        text = stringResource(R.string.train_model),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.samples_collected, sampleCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val eps = 0.1
    val tint: Color = if (abs(value.toDouble() - 1) <= eps) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFDB3535)
    }

    Card(
        shape = CardShape,
        modifier = modifier
            .height(CardHeight)
            .shadow(4.dp, shape = CardShape),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultPadding)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(icon, contentDescription = null, tint = tint)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = (round(value.toDouble() * 1000) / 1000).toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricsDetailsSheet(
    metrics: Metrics,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultPadding)
        ) {
            Text(
                text = stringResource(R.string.metrics_details),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = SmallPadding)
            )

            MetricDetailRow(stringResource(R.string.precision), metrics.precision.toString())
            MetricDetailRow(stringResource(R.string.recall), metrics.recall.toString())

            Spacer(modifier = Modifier.height(SmallPadding))

            Text(
                text = stringResource(R.string.f1_score_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HandleBluetoothSetup(bluetoothViewModel: BluetoothViewModel) {
    val context = LocalContext.current

    // Capture strings for lambdas
    val bluetoothNotEnabledText = stringResource(R.string.bluetooth_not_enabled)
    val bluetoothNotSupportedText = stringResource(R.string.bluetooth_not_supported)

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, bluetoothNotEnabledText, Toast.LENGTH_SHORT).show()
        }
    }

    RequestBluetoothPermissions()

    LaunchedEffect(Unit) {
        when {
            bluetoothViewModel.bluetoothAdapter == null -> {
                Toast.makeText(context, bluetoothNotSupportedText, Toast.LENGTH_SHORT).show()
            }

            !bluetoothViewModel.bluetoothAdapter.isEnabled -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                resultLauncher.launch(enableBtIntent)
            }
        }
    }
}

@Composable
private fun RequestBluetoothPermissions() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                    permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        if (!isGranted) {
            Toast.makeText(context, "Bluetooth Permissions Denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val needsPermissions = requiredPermissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsPermissions) {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }
}

@Composable
fun InferenceOverlay(
    debugMode: Boolean,
    isConnected: Boolean,
    context: Context,
    isInferenceRunning: Boolean,
    onStartInference: () -> Unit
) {
    // Pre-capture string resources for the overlay
    val noDeviceConnectedText = stringResource(R.string.no_device_connected)
    val startInferenceText = stringResource(R.string.start_inference)
    val tapToBeginText = stringResource(R.string.tap_anywhere_to_begin_monitoring)

    if (!isInferenceRunning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .clickable {
                    if (!debugMode && !isConnected) {
                        Toast
                            .makeText(context, noDeviceConnectedText, Toast.LENGTH_LONG)
                            .show()
                    } else {
                        onStartInference()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = startInferenceText,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = startInferenceText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tapToBeginText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DeviceConnectionIcon(
    isConnected: Boolean,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors(),
    modifier: Modifier = Modifier
) {
    val icon =
        if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled
    // Pre-capture the contentDescription
    val contentDescription = if (isConnected) {
        stringResource(R.string.device_connected)
    } else {
        stringResource(R.string.device_disconnected)
    }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.aspectRatio(1f),
        tint = buttonColors.containerColor
    )
}

@Composable
fun InferenceRunningIcon(
    isInferenceRunning: Boolean,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isInferenceRunning) Icons.Default.Pause else Icons.Default.PlayArrow
    // Pre-capture the contentDescription
    val contentDescription = if (isInferenceRunning) {
        stringResource(R.string.inference_running_cd)
    } else {
        stringResource(R.string.inference_not_running_cd)
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = buttonColors.containerColor,
            modifier = modifier.fillMaxSize()
        )
    }
}
