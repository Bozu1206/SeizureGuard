package com.epfl.ch.seizureguard.inference

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
import androidx.lifecycle.Observer
import com.epfl.ch.seizureguard.MainActivity
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.dl.ModelService
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.dl.DataSample
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileRepository
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_detection.SeizureDetectionViewModel

class InferenceService : Service() {

    private lateinit var modelService: ModelService

    private var pendingAction: String? = null

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profile : Profile

    private val bluetoothViewModel by lazy {
        val appContext = applicationContext ?: throw IllegalStateException("Application context is null")
        RunningApp.getInstance(appContext).bluetoothViewModel
    }

    private val repository: ProfileRepository by lazy {
        val appContext = applicationContext ?: throw IllegalStateException("Application context is null")
        ProfileRepository.getInstance(context = appContext)
    }

    private val samples = mutableListOf<DataSample>()
    private var isPaused = false
    private var isTrainingEnabled = false
    private var isDebugEnabled = false

    private lateinit var seizureDetectionViewModel: SeizureDetectionViewModel

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { // triggered when this service gets connected
            Log.d("onServiceConnected","Connecting ModelService, isDebugEnabled: $isDebugEnabled")
            modelService = (service as ModelService.LocalBinder).getService()

            when (pendingAction) { // parse what action we have to perform
                Actions.START.toString() -> start()             // safe now
                Actions.STOP.toString()  -> stopSelf()          // or stop
                "ACTION_START_TRAINING"  -> doTraining()        // or train
            }
            pendingAction = null

            loadCustomModel()
            if (isDebugEnabled) {
                registerSampleReceiver()
            } else {
                //bluetoothViewModel.enableEEGNotifications() // show the fixed notification for the service
                startObservingLiveData() // starts observing the values read from BLE
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            modelService.onDestroy()
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

    /**
     * In DEBUG mode, receives samples from SampleBroadcastService.
     */
    private val sampleReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context, intent: Intent) {
            val sample = intent.getParcelableExtra("EXTRA_SAMPLE", DataSample::class.java)
            if (sample != null) {
                handleSample(sample, context)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        seizureDetectionViewModel = (application as RunningApp).seizureDetectionViewModel
        profileViewModel = RunningApp.getInstance(application as RunningApp).profileViewModel
        profile = profileViewModel.profileState.value

        Log.d("InferenceService", "onCreate called")

        // 1) Load any custom model from Firebase
        loadCustomModel()

        // 2) Register for "TRAINING_COMPLETE" broadcasts
        val trainingFilter = IntentFilter("com.example.seizureguard.TRAINING_COMPLETE")
        ContextCompat.registerReceiver(
            this,
            trainingCompleteReceiver,
            trainingFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // 3) Bind to ModelService
        // 2) Bind to ModelService (Android will create it)
        val modelServiceIntent = Intent(this, ModelService::class.java).apply {
            putExtra("IS_DEBUG_ENABLED", profile.isDebugEnabled)
        }
        bindService(modelServiceIntent, serviceConnection, BIND_AUTO_CREATE)

        Log.d("InferenceService", "onCreate finished")
    }

    private fun doTraining() {
        Thread {
            samples.shuffle()
            modelService?.getModelManager()?.let { modelManager ->
                for (i in 0..20) {
                    modelManager.performTrainingEpoch(samples.toTypedArray())
                }

                samples.clear()

                modelManager.saveModel(
                    context = applicationContext,
                    profileViewModel = profileViewModel
                )

                repository.resetSamples()
            }
            isPaused = false
        }.start()
    }

    /**
     * Triggered when MainActivity sends the intent (startForegroundService(...)).
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("InferenceService", "onStartCommand called")

        sendOngoingInferenceNotification()

        pendingAction = intent?.action

        // Extract the action
        val action = intent?.action
        if (action == Actions.STOP.toString()) { // when  we receive the stop command from the ongoing notification
            Log.d("InferenceService", "Stopping service and BLE from onStartCommand")
            bluetoothViewModel.stopBLE()
            stopSelf()
            return START_NOT_STICKY
        }

        // Extract flags from Intent
        intent?.extras?.let {
            isTrainingEnabled = it.getBoolean("IS_TRAINING_ENABLED", false)
            isDebugEnabled    = it.getBoolean("IS_DEBUG_ENABLED", false)
        }

        // Return START_STICKY or START_NOT_STICKY as appropriate
        return START_STICKY
    }

    /**
     * Start "ongoing inference" - show the foreground notification, etc.
     */
    private fun start() {
        // Optionally, ensure the model is loaded or create a repeated check
        val modelManager = modelService?.getModelManager()
        if (modelManager == null) {
            Handler(mainLooper).postDelayed({ start() }, 1000)
        }
    }

    /**
     * Non-debug: Observe LiveData from BluetoothViewModel
     */
    private val bleDataSampleObserver = Observer<DataSample> { sample ->
        if (sample != null) {
            val context = this@InferenceService.applicationContext
            handleSample(sample, context)
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

    /**
     * Debug mode: register sampleReceiver for the "com.example.seizureguard.NEW_SAMPLE" broadcast
     */
    private fun registerSampleReceiver() {
        val filter = IntentFilter("com.example.seizureguard.NEW_SAMPLE")
        ContextCompat.registerReceiver(
            this,
            sampleReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d("InferenceService", "SampleReceiver registered (DEBUG MODE)")
    }

    /**
     * Unregister things & unbind service
     */
    override fun onDestroy() {
        Log.d("InferenceService", "onDestroy called")
        super.onDestroy()

        unbindService(serviceConnection)

        if (isDebugEnabled) {
            unregisterReceiver(sampleReceiver)
            Log.d("InferenceService", "SampleReceiver unregistered (DEBUG MODE)")
        } else {
            stopObservingLiveData()
        }

        unregisterReceiver(trainingCompleteReceiver)
        Log.d("InferenceService", "trainingCompleteReceiver unregistered")
    }

    /**
     * Common logic to handle each new sample (either from broadcast or from LiveData)
     */
    private fun handleSample(sample: DataSample, context: Context) {
        repository.incrementSampleCount()

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

        // Example trigger: If we want to train after 100 samples
        if (samples.size >= 100 && isTrainingEnabled) {
            Log.d("InferenceService", "Training triggered")
            isPaused = true
            doTraining()
        }

        // Inference
        modelService?.getModelManager()?.let { modelManager ->
            val prediction = modelManager.performInference(sample)
            Log.d("InferenceService",
                "Predictions: $prediction (${repository.sampleCount} | Ground Truth: ${sample.label})"
            )

            if (prediction == 1) {
                val app = context.applicationContext as RunningApp
                if (app.appLifecycleObserver.isAppInForeground) {
                    seizureDetectionViewModel.onSeizureDetected()
                } else {
                    Log.d("InferenceService", "app is in background, sending notification")
                    sendSeizureDetectedNotification()
                }
            }
        }
    }

    private fun loadCustomModel() {
        profileViewModel.loadLatestModelFromFirebase { modelFile ->
            if (modelFile != null) {
                Log.d("InferenceService", "Custom model loaded successfully")
                modelService?.getModelManager()?.updateInferenceModel(modelFile)
            } else {
                Log.d("InferenceService", "No custom model found, using default")
            }
        }
    }

    private fun bindToModelService() {
        val intent = Intent(this, ModelService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Show a high-priority foreground notification to keep the service alive
     */
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(SEIZURE_NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    enum class Actions {
        START, STOP
    }

    companion object {
        private const val FIXED_NOTIFICATION_ID = 1
        private const val SEIZURE_NOTIFICATION_ID = 2
    }
}

