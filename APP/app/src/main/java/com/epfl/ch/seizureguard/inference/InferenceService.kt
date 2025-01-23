package com.epfl.ch.seizureguard.inference

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

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

    private lateinit var ongoingNotification: Notification

    private lateinit var seizureDetectionViewModel: SeizureDetectionViewModel

    var location : Location? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.create().apply {
        interval = 5000 // 5 seconds
        fastestInterval = 2000 // 2 seconds
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            location = locationResult.lastLocation
            Log.d("locationCallback", "Location Updated")
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { // triggered when this service gets connected
            modelService = (service as ModelService.LocalBinder).getService()

            Log.d("InferenceService", "Pending action is: $pendingAction")

            when (pendingAction) { // parse what action we have to perform
                Actions.START.toString() -> start()             // safe now
                Actions.STOP.toString()  -> stopSelf()          // or stop
                else  -> doTraining()        // or train
            }
//            pendingAction = null
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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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

        // 2) Bind to ModelService (Android will create it)
        val modelServiceIntent = Intent(this, ModelService::class.java).apply {
            putExtra("IS_DEBUG_ENABLED", profile.isDebugEnabled)
        }
        bindService(modelServiceIntent, serviceConnection, BIND_AUTO_CREATE)

    }

    private fun doTraining() {
        Thread {
            samples.shuffle()
            Log.d("InferenceService", "Training on ${samples.size} samples")
            modelService.getModelManager().let { modelManager ->
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

        pendingAction = intent?.action

        if (pendingAction == Actions.STOP.toString()) { // when  we receive the stop command from the ongoing notification
            bluetoothViewModel.stopBLE()
            profileViewModel.setInferenceRunning(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        else if (pendingAction == "ACTION_START_TRAINING") {
            Log.d("InferenceService", "Training requested")
            if (::modelService.isInitialized) {
                doTraining()
            }
            // If modelService is not initialized yet, pendingAction will be handled in onServiceConnected
            return START_STICKY
        }
        else{
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationProviderClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { initialLocation ->
                    if (initialLocation != null) {
                        location = initialLocation
                    }
                }.addOnFailureListener { e ->
                    Log.e("InferenceService", "Failed to get initial location", e)
                }
                // Start continuous location updates
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
            sendOngoingInferenceNotification()
            profileViewModel.setInferenceRunning(true)
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
        loadCustomModel()
        if (isDebugEnabled) {
            registerSampleReceiver()
        } else {
            //bluetoothViewModel.enableEEGNotifications() // show the fixed notification for the service
            startObservingLiveData() // starts observing the values read from BLE
        }
        profileViewModel.setInferenceRunning(true)
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
    }

    private fun stopObservingLiveData() {
        bluetoothViewModel.dataSample.removeObserver(bleDataSampleObserver)
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
    }

    /**
     * Unregister things & unbind service
     */
    override fun onDestroy() {
        super.onDestroy()

        unbindService(serviceConnection)

        if (isDebugEnabled) {
            unregisterReceiver(sampleReceiver)
        } else {
            stopObservingLiveData()
        }

        unregisterReceiver(trainingCompleteReceiver)
    }

    /**
     * Common logic to handle each new sample (either from broadcast or from LiveData)
     */
    private fun handleSample(sample: DataSample, context: Context) {
        repository.incrementSampleCount()

        if (isPaused) {
            Log.d("InferenceService", "Inference paused, training in progress")
            return
        }

        samples.add(sample)
        Log.d("InferenceService", "Collected (${samples.size} samples)")

        // Example trigger: If we want to train after 100 samples
        if (samples.size >= 100 && isTrainingEnabled) {
            isPaused = true
            profileViewModel.setInferenceRunning(false)
            // doTraining()
        }

        // Inference
        modelService.getModelManager().let { modelManager ->
            val prediction = modelManager.performInference(sample)
            Log.d("InferenceService",
                "Predictions: $prediction (${repository.sampleCount} | Ground Truth: ${sample.label})"
            )

            if (prediction == 1) {
                val app = context.applicationContext as RunningApp
                if (app.appLifecycleObserver.isAppInForeground) {
                    seizureDetectionViewModel.onSeizureDetected()
                    profileViewModel.sendNotificationToMyDevices("Seizure Detected!", location)
                } else {
                    sendSeizureDetectedNotification()
                    profileViewModel.sendNotificationToMyDevices("Seizure Detected!", location)
                }
            }
        }
    }

    private fun loadCustomModel() {
        profileViewModel.loadLatestModelFromFirebase { modelFile ->
            if (modelFile != null) {
                if (::modelService.isInitialized) {
                    modelService.getModelManager()?.updateInferenceModel(modelFile)
                } else {
                    Log.e("InferenceService", "ModelService not initialized, retrying later")
                    Handler(mainLooper).postDelayed({ loadCustomModel() }, 100)
                }
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
        ongoingNotification = NotificationCompat.Builder(
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

        startForeground(FIXED_NOTIFICATION_ID, ongoingNotification)
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
        START, STOP, TRAINING
    }

    companion object {
        private const val FIXED_NOTIFICATION_ID = 1
        private const val SEIZURE_NOTIFICATION_ID = 2
    }
}

