package com.example.seizureguard.inference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.ui.theme.AppTheme
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seizureguard.bluetooth.BluetoothViewModel



@Composable
fun InferenceHomePage(
    metrics: Metrics,
    onPerformInference: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {

    val context = LocalContext.current
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result->
            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    context, "Bluetooth is not enabled!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    @Composable
    fun RequestBluetoothPermissions(onPermissionGranted: () -> Unit) {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
                } else {
                    // For API levels < 31, no need to request these permissions
                    true
                }
                if (isGranted) {
                    onPermissionGranted()
                } else {
                    Toast.makeText(context, "Bluetooth Permissions Denied", Toast.LENGTH_SHORT).show()
                }
            }
        )
        LaunchedEffect(Unit) {// triggers everytime Unit changes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scanPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
                val connectPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                val adminPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
                val fineLocationPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!scanPermissionGranted || !connectPermissionGranted || !adminPermissionGranted || !fineLocationPermissionGranted || !coarseLocationPermissionGranted) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    onPermissionGranted()
                }
            }
        }
    }

    RequestBluetoothPermissions {Log.d("RequestBluetoothPermissions", "permissions granted")}
    LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {// at onResume (when we open this screen) we look for past sessions
        Log.d("Inference home page", "Inference home page OnCreate")
        if(bluetoothViewModel.bluetoothAdapter==null){ // if a bluetooth adapter is not available
            Toast.makeText(context,"Bluetooth not supported", Toast.LENGTH_SHORT).show() // tell the user the bluetooth is not supported
            Log.d("InferenceHomePage","Bluetooth not supported")
        }
        if (bluetoothViewModel.bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val (titleText, performInferenceButton, lookForDevicesButton, metricsColumn, notComputedText) = createRefs()

        Text(text = "Inference Dashboard", modifier = Modifier.constrainAs(titleText) {
            top.linkTo(parent.top, margin = 16.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        },
            style = MaterialTheme.typography.headlineLarge,
        )

        Button(
            onClick = onPerformInference,
            modifier = Modifier
                .constrainAs(performInferenceButton) {
                    top.linkTo(titleText.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(0.8f)  // Button fills 80% of the width
        ) {
            Text(text = "Perform Inference")
        }
        Button(
            onClick = { bluetoothViewModel.scanLeDevice() },
            modifier = Modifier
                .constrainAs(lookForDevicesButton) {
                    top.linkTo(performInferenceButton.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(0.8f)  // Button fills 80% of the width
        ) {
            Text(text = "Look for devices")
        }

        if (metrics.f1 != -1.0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.constrainAs(metricsColumn) {
                    top.linkTo(lookForDevicesButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                Text(text = "Accuracy: ${"%.4f".format(metrics.accuracy)}")
                Text(text = "F1 Score: ${"%.4f".format(metrics.f1)}")
                Text(text = "Precision: ${"%.4f".format(metrics.precision)}")
                Text(text = "Recall: ${"%.4f".format(metrics.recall)}")
                Text(text = "FPR: ${"%.4f".format(metrics.fpr)}")
            }
        } else {
            Text(
                text = "Not yet computed",
                modifier = Modifier.constrainAs(notComputedText) {
                    top.linkTo(lookForDevicesButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Composable
fun InferenceScreen(
    metrics: Metrics,
    onRunInference: () -> Unit,
) {
    InferenceHomePage(metrics = metrics, onPerformInference = onRunInference)
}

@Preview(showBackground = true)
@Composable
fun InferenceHomePagePreview() {
    AppTheme {
        InferenceHomePage( Metrics(0.0,0.0,0.0,0.0, 0.0), {})
    }
}
