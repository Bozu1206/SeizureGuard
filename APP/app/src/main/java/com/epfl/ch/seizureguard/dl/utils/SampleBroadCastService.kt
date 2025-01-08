package com.epfl.ch.seizureguard.dl.utils

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Observer
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.dl.DataLoader
import com.epfl.ch.seizureguard.dl.DataSample
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileViewModel

// this handles broadcasting values from the known dataset to the model (only in debug mode)
class SampleBroadcastService : Service() {
    private val handler = Handler()
    private val interval: Long = 4000

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profile : Profile

    private val bluetoothViewModel by lazy {
        val appContext = applicationContext ?: throw IllegalStateException("Application context is null")
        RunningApp.getInstance(appContext).bluetoothViewModel
    }

    private var isDebugEnabled : Boolean = false

    private lateinit var data: Array<DataSample>

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            sendSampleBroadcast()
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        profileViewModel = RunningApp.getInstance(application as RunningApp).profileViewModel
        profile = profileViewModel.profileState.value
        isDebugEnabled = profile.isDebugEnabled
        if(isDebugEnabled){ // DEBUG mode
            Log.d("SampleBroadcastService", "Service created. isDebugEnabled = $isDebugEnabled")
            handler.post(broadcastRunnable)
            var d = DataLoader().loadDataAndLabels(applicationContext, "data_20.bin")
            data = d.slice(200..800).toTypedArray()
            d = emptyArray()
            System.gc()
        }
        else{ // BLE mode
            startObservingLiveData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isDebugEnabled){ // DEBUG mode
            handler.removeCallbacks(broadcastRunnable)
        }
        else{ // BLE mode
            stopObservingLiveData()
        }
        Log.d("SampleBroadcastService", "Service destroyed, broadcast loop stopped")
    }

    private fun sendSampleBroadcast() { // for DEBUG mode
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
        if (sample != null) {
            val context = this@SampleBroadcastService.applicationContext
            sendSampleBroadcastBLE(sample)
        }
    }

    private fun startObservingLiveData() {
        bluetoothViewModel.dataSample.observeForever(bleDataSampleObserver)
        Log.d("InferenceService", "Started observing bluetoothViewModel.dataSample")
    }

    private fun stopObservingLiveData() {
        bluetoothViewModel.dataSample.removeObserver(bleDataSampleObserver)
        Log.d("InferenceService", "Stopped observing bluetoothViewModel.dataSample")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
