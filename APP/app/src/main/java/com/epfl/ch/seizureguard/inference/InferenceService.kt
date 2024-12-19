package com.example.seizureguard.inference

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.seizureguard.MainActivity
import com.example.seizureguard.dl.ModelService
import com.example.seizureguard.R
import com.example.seizureguard.RunningApp
import com.example.seizureguard.dl.DataSample
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.dl.metrics.MetricsUtils

// Foreground service for launching inference in background
class InferenceService : Service() {
    private var modelService: ModelService? = null
    private val samples = mutableListOf<DataSample>()
    private var isPaused = false
    private var isTrainingEnabled = false
    private var counter = 0


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            modelService = (service as ModelService.LocalBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            modelService = null
        }
    }

    private val trainingCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.seizureguard.TRAINING_COMPLETE") {
                Log.d("InferenceService", "Training complete. Resuming inference.")
                isPaused = false
            }
        }
    }

    private val sampleReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context, intent: Intent) {
            val sample = intent.getParcelableExtra("EXTRA_SAMPLE", DataSample::class.java)
            if (sample != null) {
                counter++
                if (!isTrainingEnabled) {
                    Log.d("InferenceService", "Training is disabled")
                } else {
                    Log.d("InferenceService", "Training is enabled")
                }

                if (isPaused) {
                    Log.d("InferenceService", "Inference paused, training in progress")
                    return
                }

                samples.add(sample)

                if (samples.size >= 100 && isTrainingEnabled) {
                    Log.d("InferenceService", "Training triggered")
                    isPaused = true
                    Thread {
                        samples.shuffle()
                        modelService?.getModelManager().let { modelManager ->
                            for (i in 0..20) {
                                modelManager?.performTrainingEpoch(samples.toTypedArray())
                            }

                            samples.clear()

                            val metricsBeforeTraining = modelManager?.validate(context = applicationContext) ?: Metrics()
                            MetricsUtils.saveMetrics(applicationContext, "metrics_before_training", metricsBeforeTraining)

                            modelManager?.saveModel(context = applicationContext)

                            val metricsAfterTraining = modelManager?.validate(context = applicationContext) ?: Metrics()
                            MetricsUtils.saveMetrics(applicationContext, "metrics_after_training", metricsAfterTraining)
                        }

                        isPaused = false
                    }.start()
                }

                modelService?.getModelManager()?.let { modelManager ->
                    val prediction = modelManager.performInference(sample)
                    Log.d("InferenceService", "Predictions: $prediction (${counter})")

                    if (prediction == 1) {
                        val app = context.applicationContext as RunningApp
                        if (app.appLifecycleObserver.isAppInForeground) {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("EXTRA_SEIZURE_DETECTED", true)
                            }
                            context.startActivity(intent)
                        } else {
                            Log.d("InferenceService", "app is in background, sending notification")
                            sendSeizureDetectedNotification()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d("InferenceService", "onCreate called")
        var filter = IntentFilter("com.example.seizureguard.TRAINING_COMPLETE")
        // registerReceiver(trainingCompleteReceiver, filter)
        ContextCompat.registerReceiver(
            this,
            trainingCompleteReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        bindService(Intent(this, ModelService::class.java), serviceConnection, BIND_AUTO_CREATE)
        filter = IntentFilter("com.example.seizureguard.NEW_SAMPLE")
        // registerReceiver(sampleReceiver, filter)
        ContextCompat.registerReceiver(
            this,
            sampleReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d("InferenceService", "SampleReceiver registered")
    }

    // Triggered when MainActivity sends the intent
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("InferenceService", "onStartCommand called")

        this.isTrainingEnabled = intent?.getBooleanExtra("IS_TRAINING_ENABLED", false) ?: false

        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        sendOngoingInferenceNotification()
        val modelManager = modelService?.getModelManager()

        if (modelManager == null) {
            Handler(mainLooper).postDelayed({ start() }, 1000)
            return
        }
    }

    override fun onDestroy() {
        Log.d("InferenceService", "onDestroy called")
        super.onDestroy()
        unbindService(serviceConnection)
        unregisterReceiver(sampleReceiver)
        unregisterReceiver(trainingCompleteReceiver)
        Log.d("InferenceService", "SampleReceiver unregistered")
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
        val notificationBuilder =
            NotificationCompat.Builder(this, getString(R.string.seizure_notification_id))
                .setContentTitle("SEIZURE DETECTED!")
                .setContentText("Click to open")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(SEIZURE_NOTIFICATION_ID, notificationBuilder.build())
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
        val notification = NotificationCompat.Builder(
            this,
            getString(R.string.fixed_foregroung_notification_channel_id)
        )
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

    enum class Actions {
        START, STOP
    }

    companion object {
        private const val FIXED_NOTIFICATION_ID = 1
        private const val SEIZURE_NOTIFICATION_ID = 2
    }
}
