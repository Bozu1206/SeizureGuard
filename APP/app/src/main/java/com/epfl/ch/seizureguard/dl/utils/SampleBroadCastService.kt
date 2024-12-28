package com.epfl.ch.seizureguard.dl.utils

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.epfl.ch.seizureguard.dl.DataLoader
import com.epfl.ch.seizureguard.dl.DataSample

class SampleBroadcastService : Service() {
    private val handler = Handler()
    private val interval: Long = 100
    private var counter: Int = 0

    private lateinit var data: Array<DataSample>

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            sendSampleBroadcast()
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SampleBroadcastService", "Service created, starting broadcast loop")
        handler.post(broadcastRunnable)
        var d = DataLoader().loadDataAndLabels(applicationContext, "data_20.bin")
        data = d.slice(200..800).toTypedArray()
        d = emptyArray()
        System.gc()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(broadcastRunnable)
        Log.d("SampleBroadcastService", "Service destroyed, broadcast loop stopped")
    }

    private fun sendSampleBroadcast() {
        val sample = data.random()
        data = data.filter { it != sample }.toTypedArray()

        val intent = Intent("com.example.seizureguard.NEW_SAMPLE").apply {
            putExtra("EXTRA_SAMPLE", sample)
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
