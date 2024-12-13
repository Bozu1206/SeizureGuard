package com.example.seizureguard.dl.utils

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.example.seizureguard.dl.DataLoader
import com.example.seizureguard.dl.DataSample

class SampleBroadcastService() : Service() {
    private val handler = Handler()
    private val interval: Long = 4000

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
        var d = DataLoader().loadDataAndLabels(applicationContext, "data.bin")
        data = d.slice(400..800).toTypedArray()
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
        // remove sample
        data = data.filter { it != sample }.toTypedArray()
        val intent = Intent("com.example.seizureguard.NEW_SAMPLE").apply {
            putExtra("EXTRA_SAMPLE", sample)
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
