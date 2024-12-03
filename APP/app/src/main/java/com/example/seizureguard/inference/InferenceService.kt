package com.example.seizureguard.inference

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.seizureguard.R
import com.example.seizureguard.dl.InferenceProcessor
import com.example.seizureguard.dl.OnnxHelper
import com.example.seizureguard.dl.DataLoader
import com.example.seizureguard.MainActivity
import com.example.seizureguard.RunningApp

class InferenceService : Service() { // foreground service for launching inference in background

    private lateinit var inferenceProcessor: InferenceProcessor
    override fun onCreate() {
        super.onCreate()
        Log.d("InferenceService", "onCreate called")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { // triggered when MainActivity sends the intent
        Log.d("InferenceService", "onStartCommand called")
        when(intent?.action){
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    private fun start(){
        sendOngoingInferenceNotification()
        inferenceProcessor = InferenceProcessor(
            context = this,
            dataLoader = DataLoader(),
            onnxHelper = OnnxHelper(),
            onSeizureDetected = {
                val app = applicationContext as RunningApp
                if (app.appLifecycleObserver.isAppInForeground) { // App is open; navigate using NavController
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("EXTRA_SEIZURE_DETECTED", true)
                    }
                    startActivity(intent)
                } else {  // App is not open; send a notification
                    sendSeizureDetectedNotification()
                }
                inferenceProcessor.cancelCoroutine()
            }
        )
        inferenceProcessor.runInference {newMetrics ->
            var metrics = newMetrics
            Log.d(
                "ValidationMetrics",
                "F1 Score: ${metrics.f1}, Precision: ${metrics.precision}, " +
                        "Recall: ${metrics.recall}, FPR: ${metrics.fpr}"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inferenceProcessor.cancelCoroutine()
        Log.d("InferenceService", "onDestroy called")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendSeizureDetectedNotification() {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SEIZURE_DETECTED", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            SEIZURE_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.seizure_notification_id))
            .setContentTitle("SEIZURE DETECTED!")
            .setContentText("Click to open")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(SEIZURE_NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun sendOngoingInferenceNotification() {
        val stopServiceIntent = Intent(this, InferenceService::class.java).apply {
            action = Actions.STOP.toString()
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, getString(R.string.fixed_foregroung_notification_channel_id) )
            .setContentTitle("Inference Running")
            .setContentText("Performing inference in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop", // Text for the button
                stopServicePendingIntent // Intent triggered when the button is clicked
            )
            .build()
        startForeground(FIXED_NOTIFICATION_ID, notification)
    }

    enum class Actions{
        START, STOP
    }

    companion object {
        private const val FIXED_NOTIFICATION_ID = 1
        private const val SEIZURE_NOTIFICATION_ID = 2
    }
}
