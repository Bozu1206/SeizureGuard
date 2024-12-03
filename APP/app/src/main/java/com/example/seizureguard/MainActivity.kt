package com.example.seizureguard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.example.seizureguard.wallet_manager.generateToken
import com.example.seizureguard.dl.DataLoader
import com.example.seizureguard.dl.InferenceProcessor
import com.example.seizureguard.dl.OnnxHelper
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.seizure_event.SeizureDao
import com.example.seizureguard.seizure_event.SeizureDatabase
import com.google.android.gms.samples.wallet.viewmodel.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0, -1.0))
    private var isSeizureDetected by mutableStateOf(false)

    private val walletViewModel: WalletViewModel by viewModels()
    private val addToGoogleWalletRequestCode = 1000

    private lateinit var databaseRoom: SeizureDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseRoom = initializeDatabase()
        val inferenceProcessor = initializeInferenceProcessor()

        setContent {
            val payState by walletViewModel.walletUiState.collectAsStateWithLifecycle()

            MainScreen(
                isSeizureDetected = isSeizureDetected,
                onDismissSeizure = { isSeizureDetected = false },
                onEmergencyCall = { onEmergencyCall(context = this) },
                onRunInference = {
                    inferenceProcessor.runInference { newMetrics ->
                        updateMetrics(newMetrics)
                    }
                },
                metrics = metrics,
                payState = payState,
                requestSavePass = ::requestSavePass
            )
        }
    }

    private fun initializeDatabase(): SeizureDao {
        val application = requireNotNull(this).application
        return SeizureDatabase.getInstance(application).seizureDao
    }

    private fun initializeInferenceProcessor(): InferenceProcessor {
        val context = this
        val dataLoader = DataLoader()
        val onnxHelper = OnnxHelper()

        return InferenceProcessor(context, dataLoader, onnxHelper) {
            isSeizureDetected = true
        }
    }

    private fun updateMetrics(newMetrics: Metrics) {
        metrics = newMetrics
        Log.d(
            "ValidationMetrics", """
            F1 Score: ${metrics.f1}, Precision: ${metrics.precision}, 
            Recall: ${metrics.recall}, FPR: ${metrics.fpr}
        """.trimIndent()
        )
    }

    private fun requestSavePass(request: GoogleWalletToken.PassRequest) {
        lifecycleScope.launch {
            val token = generateToken(request)
            walletViewModel.savePassesJwt(token, this@MainActivity, addToGoogleWalletRequestCode)
        }
    }
}


fun logAllPastSeizures(seizureDao: SeizureDao) { // temporary, for debug purpose
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val seizures = seizureDao.getAllSeizureEvents()
            seizures.forEach { seizure ->
                Log.d(
                    "SeizureLog", """
                        Seizure ID: ${seizure.seizureKey}
                        Type: ${seizure.type}
                        Duration: ${seizure.duration} minutes
                        Severity: ${seizure.severity}
                        Triggers: ${seizure.triggers.joinToString(", ")}
                        Timestamp: ${seizure.timestamp}
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            Log.e("SeizureLogError", "Failed to fetch seizures: ${e.message}")
        }
    }
}



