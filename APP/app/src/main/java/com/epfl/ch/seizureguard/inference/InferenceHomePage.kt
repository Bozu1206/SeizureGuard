package com.epfl.ch.seizureguard.inference

import EEGChart
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
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
import com.epfl.ch.seizureguard.profile.ProfileRepository
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.profile.ProfileViewModelFactory
import org.tensorflow.op.math.Round
import kotlin.math.round

private val CardShape = RoundedCornerShape(12.dp)
private val CardElevation = 6.dp
private val DefaultPadding = 16.dp
private val SmallPadding = 8.dp
private val CardHeight = 120.dp

@Composable
fun InferenceHomePage(
    onPerformInference: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    metricsViewModel: MetricsViewModel,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(
                                                LocalContext.current,
                                                (LocalContext.current.applicationContext as RunningApp)))
) {
    val profile by profileViewModel.profileState.collectAsState()

    val context = LocalContext.current

    val runningApp = context.applicationContext as RunningApp
    val bluetoothViewModel = runningApp.bluetoothViewModel // Retrieve the singleton instance

    val debugMode = profile.isDebugEnabled
    val isConnected by bluetoothViewModel.isConnected.observeAsState(initial = false) // is the BLE device connected?


    // Handle Bluetooth setup
    HandleBluetoothSetup(bluetoothViewModel)

    // Load metrics
    LaunchedEffect(Unit) {
        metricsViewModel.loadMetrics()
    }

    var isInferenceRunning by remember { mutableStateOf(false) }

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
                    EEGChart()
                    InferenceOverlay(
                        debugMode,
                        isConnected,
                        context,
                        isInferenceRunning = isInferenceRunning,
                        onStartInference = {
                            isInferenceRunning = true
                            onPerformInference()
                        }
                    )
                }
                
                ActionButtonsSection(
                    debugMode,
                    isConnected,
                    onPerformInference = {
                        isInferenceRunning = true
                        onPerformInference()
                    },
                    onScanDevices = { bluetoothViewModel.scanLeDevice() }
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Text(
        text = "Dashboard",
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
    onPerformInference: () -> Unit,
    onScanDevices: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SmallPadding)
        ) {
            ActionButton(
                onClick = {
                    Log.e(context::class.java.toString(), "debugMode: $debugMode")
                    Log.e(context::class.java.toString(), "isConnected: $isConnected")
                     if(!debugMode  && !isConnected){ // if no EEG device connected
                        Toast.makeText(context,"No device connected!", Toast.LENGTH_LONG).show()
                    }else{
                        onPerformInference()
                    }
                },
                icon = Icons.Default.PlayArrow,
                text = "Perform Inference"
            )

            Spacer(modifier = Modifier.height(SmallPadding / 4))

            ActionButton(
                onClick = {
                    onScanDevices()
                    Toast.makeText(context, "Scanning for devices", Toast.LENGTH_SHORT).show()
                },
                icon = Icons.Default.Bluetooth,
                text = "Look for Devices"
            )
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = CardShape,
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

    val canTrain by remember(sampleCount, isTrainReady) {
        derivedStateOf { sampleCount >= 100 && isTrainReady }
    }

    MetricsCardContent(
        metrics = metrics,
        canTrain = canTrain,
        onTrainClick = {
            profileViewModel.requestTraining()
            Toast.makeText(context, "Training has started", Toast.LENGTH_SHORT).show()
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

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(CardElevation, shape = RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(DefaultPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    title = "Accuracy",
                    value = metrics.accuracy.toString(),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(24.dp))

                MetricItem(
                    title = "F1 Score",
                    value = metrics.f1.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onShowDetails)
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
                    Text("Train Model", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )

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
                text = "Metrics Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = SmallPadding)
            )
            
            MetricDetailRow("Precision", metrics.precision.toString())
            MetricDetailRow("Recall", metrics.recall.toString())
            
            Spacer(modifier = Modifier.height(SmallPadding))
            
            Text(
                text = "The F1-Score is computed as the harmonic mean of precision and recall: " +
                    "f1 = 2 * (precision * recall) / (precision + recall)",
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

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "Bluetooth is not enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    RequestBluetoothPermissions()

    LaunchedEffect(Unit) {
        when {
            bluetoothViewModel.bluetoothAdapter == null -> {
                Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
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
fun InferenceOverlay( // big clickable box with the plots
    debugMode: Boolean,
    isConnected: Boolean,
    context : Context,

    isInferenceRunning: Boolean,
    onStartInference: () -> Unit
) {
    if (!isInferenceRunning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .clickable {
                    Log.e("InferenceOverlay", "debugMode: $debugMode")
                    Log.e("InferenceOverlay", "isConnected: $isConnected")
                    if(!debugMode  && !isConnected){ // if no EEG device connected
                        Toast.makeText(context,"No device connected!", Toast.LENGTH_LONG).show()
                    }else{
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
                    contentDescription = "Start Inference",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Start Inference",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tap anywhere to begin monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
