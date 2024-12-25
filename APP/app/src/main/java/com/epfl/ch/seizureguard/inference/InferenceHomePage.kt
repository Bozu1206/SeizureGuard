package com.epfl.ch.seizureguard.inference

import MockEEGChart
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import kotlin.math.round

@Composable
fun InferenceHomePage(
    profileViewModel: ProfileViewModel,
    onPerformInference: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    metricsViewModel: MetricsViewModel,
) {
    val context = LocalContext.current

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "Bluetooth is not enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun RequestBluetoothPermissions(onPermissionGranted: () -> Unit) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                        permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if (isGranted) onPermissionGranted() else {
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
                if (requiredPermissions.any {
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) != PackageManager.PERMISSION_GRANTED
                    }) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    onPermissionGranted()
                }
            }
        }
    }

    RequestBluetoothPermissions { /* Permissions Granted */ }

    LaunchedEffect(Unit) {
        if (bluetoothViewModel.bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        } else if (!bluetoothViewModel.bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
        }
    }

    LaunchedEffect(Unit) { metricsViewModel.loadMetrics() }





    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Start
        )

        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            // ---------------------
            // 1) METRICS CARD EN HAUT
            // ---------------------
            val metrics = profileViewModel.latestMetrics.collectAsState()
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
            ) {
                BentoMetricsCard(
                    metrics = metrics.value,
                    profileViewModel = profileViewModel
                )
            }

            // ---------------------
            // 2) GRAPHE AU CENTRE
            // ---------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 120.dp),
                contentAlignment = Alignment.Center  // Centre l'ensemble de la Column dans la Box
            ) {
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.Start
                ) {
                    MockEEGChart()
                }
            }


            // ---------------------
            // 3) BOUTONS EN BAS
            // ---------------------
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // colle les boutons en bas
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onPerformInference()
                        Toast.makeText(
                            context,
                            "Performing Inference",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Perform Inference",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Perform Inference",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        bluetoothViewModel.scanLeDevice()
                        Toast.makeText(context, "Scanning for devices", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Look for Devices",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Look for Devices",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))


        // Show only one metrics card using the Bento style
//        if (metrics.f1 != -1.0) {
//            BentoMetricsCard(metrics = metrics)
//        } else {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
//                elevation = CardDefaults.cardElevation(4.dp)
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "Metrics",
//                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                    Text(text = "Not yet computed", fontSize = 16.sp, color = Color.Gray)
//                }
//            }
//        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text(text = "Metrics Before Training: Accuracy = ${metricsBefore.accuracy}, F1 = ${metricsBefore.f1}")
        // Text(text = "Metrics After Training: Accuracy = ${metricsAfter.accuracy}, F1 = ${metricsAfter.f1}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoMetricsCard(metrics: Metrics, profileViewModel: ProfileViewModel) {
    var showExtra by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sampleCount by profileViewModel.sampleCount.collectAsState()
    val isTrainReady by profileViewModel.isTrainReady.collectAsState()
    val metrics by profileViewModel.latestMetrics.collectAsState()


    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )

                    Spacer(modifier = Modifier.fillMaxWidth())

                    Text(
                        text = (round(metrics.accuracy * 1000) / 1000).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clickable(onClick = { showExtra = !showExtra })
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Text(
                        text = "F1 Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )

                    Spacer(modifier = Modifier.fillMaxWidth())

                    Text(
                        text = (round(metrics.f1 * 1000) / 1000).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.End)
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    profileViewModel.requestTraining()
                    Toast.makeText(context, "Training has started", Toast.LENGTH_SHORT).show()
                },
                enabled = sampleCount >= 100 && isTrainReady,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Train ? ", color = Color.Black)
            }

        }
    }

    if (showExtra) {
        ModalBottomSheet(onDismissRequest = { showExtra = !showExtra }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Precision: ${metrics.precision}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Recall: ${round(metrics.recall * 1000) / 1000}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Text(
                    text = "The F1-Score is computed as the harmonic mean of precision and recall: f1 = 2 * (precision * recall) / (precision + recall)",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BentoBox(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F0F0),
        modifier = Modifier
            .aspectRatio(1.5f)
            .clickable { onClick() },
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .background(Color(0xFFF0F0F0)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun InferenceScreen(
    profileViewModel: ProfileViewModel,
    onRunInference: () -> Unit,
    metricsViewModel: MetricsViewModel
) {
    InferenceHomePage(
        profileViewModel = profileViewModel,
        onPerformInference = onRunInference,
        metricsViewModel = metricsViewModel
    )
}
