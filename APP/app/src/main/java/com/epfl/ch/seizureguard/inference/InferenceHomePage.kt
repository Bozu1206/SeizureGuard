package com.epfl.ch.seizureguard.inference

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

@Composable
fun InferenceHomePage(
    metrics: Metrics,
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
        } else if (bluetoothViewModel.bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
        }
    }

    val metricsBefore by metricsViewModel.metricsBeforeTraining.collectAsState()
    val metricsAfter by metricsViewModel.metricsAfterTraining.collectAsState()

    LaunchedEffect(Unit) { metricsViewModel.loadMetrics() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Inference Dashboard",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
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

@Composable
fun BentoMetricsCard(metrics: Metrics) {
    var showExtra by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Metrics",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Accuracy box
                BentoBox(
                    title = "Accuracy",
                    value = "%.4f".format(metrics.accuracy),
                    onClick = { /* No action on Accuracy box */ }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // F1 Score box
                BentoBox(
                    title = "F1-Score",
                    value = "%.4f".format(metrics.f1),
                    onClick = { showExtra = !showExtra }
                )
            }

            if (showExtra) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text(
                        text = "Precision: ${"%.4f".format(metrics.precision)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Recall: ${"%.4f".format(metrics.recall)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "FPR: ${"%.4f".format(metrics.fpr)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
                .fillMaxSize()
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
    metrics: Metrics,
    onRunInference: () -> Unit,
    metricsViewModel: MetricsViewModel
) {
    InferenceHomePage(
        metrics = metrics,
        onPerformInference = onRunInference,
        metricsViewModel = metricsViewModel
    )
}
