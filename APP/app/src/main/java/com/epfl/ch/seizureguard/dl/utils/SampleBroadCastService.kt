package com.epfl.ch.seizureguard.dl.utils

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.dl.DataLoader
import com.epfl.ch.seizureguard.dl.DataSample
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// This handles broadcasting values from the known dataset to the model (only in debug mode)
class SampleBroadcastService : LifecycleService() {
    private val handler = Handler()
    private val interval: Long = 4000

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profile : Profile
    private var isInferenceRunning: Boolean = false
    private var inferenceCollectorJob: Job? = null

    private val bluetoothViewModel by lazy {
        val appContext = applicationContext ?: throw IllegalStateException("Application context is null")
        RunningApp.getInstance(appContext).bluetoothViewModel
    }

    private var isDebugEnabled : Boolean = false

    private lateinit var data: Array<DataSample>

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            if (isInferenceRunning && data.isNotEmpty()) {
                sendSampleBroadcast()
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun handleInferenceStateChange(isRunning: Boolean) {
        isInferenceRunning = isRunning
        if (isDebugEnabled) {
            if (isRunning && ::data.isInitialized && data.isNotEmpty()) {
                handler.post(broadcastRunnable)
            } else {
                handler.removeCallbacks(broadcastRunnable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        profileViewModel = RunningApp.getInstance(application as RunningApp).profileViewModel
        profile = profileViewModel.profileState.value
        isDebugEnabled = profile.isDebugEnabled
        
        // Start collecting inference state
        inferenceCollectorJob = lifecycleScope.launch {
            profileViewModel.isInferenceRunning.collect { isRunning ->
                handleInferenceStateChange(isRunning)
            }
        }
        
        if(isDebugEnabled){ // DEBUG mode
            val d = DataLoader().loadDataAndLabels(applicationContext, "data_20.bin")
            data = d.slice(0..100).toTypedArray()
            
            // Only start broadcasting if inference is already running
            if (profileViewModel.isInferenceRunning.value) {
                handler.post(broadcastRunnable)
            }
            System.gc()
        }
        else{ // BLE mode
            startObservingLiveData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the inference collector job
        inferenceCollectorJob?.cancel()
        
        if(isDebugEnabled){ // DEBUG mode
            handler.removeCallbacks(broadcastRunnable)
        }
        else{ // BLE mode
            stopObservingLiveData()
        }
    }

    private fun sendSampleBroadcast() { // for DEBUG mode
        if (data.isEmpty()) return
        
        val sample = data.random()
        data = data.filter { it != sample }.toTypedArray()

        val intent = Intent("com.example.seizureguard.NEW_SAMPLE").apply {
            putExtra("EXTRA_SAMPLE", sample)
        }


        sendBroadcast(intent)
    }

    private fun sendSampleBroadcastBLE(dataSample: DataSample) { // for BLE
        val intent = Intent("com.example.seizureguard.NEW_SAMPLE").apply {
            putExtra("EXTRA_SAMPLE", dataSample)
        }
        sendBroadcast(intent)
    }

    /**
     * Non-debug: Observe LiveData from BluetoothViewModel
     */
    private val bleDataSampleObserver = Observer<DataSample> { sample ->
        if(sample.data.isNotEmpty() && isInferenceRunning){
            sendSampleBroadcastBLE(sample)
        }
    }

    private fun startObservingLiveData() {
        bluetoothViewModel.lastValues.observeForever(bleDataSampleObserver)
    }

    private fun stopObservingLiveData() {
        bluetoothViewModel.lastValues.removeObserver(bleDataSampleObserver)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
