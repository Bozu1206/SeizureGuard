package com.epfl.ch.seizureguard

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.epfl.ch.seizureguard.seizure_detection.SeizureDetectionViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModelFactory
import com.epfl.ch.seizureguard.bluetooth.BluetoothViewModel

class RunningApp : Application(), ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore by lazy { ViewModelStore() }
    lateinit var seizureDetectionViewModel: SeizureDetectionViewModel
    lateinit var profileViewModel: ProfileViewModel
    lateinit var bluetoothViewModel: BluetoothViewModel

    val appLifecycleObserver = AppLifecycleObserver()

    override fun onCreate() {
        super.onCreate()
        
        seizureDetectionViewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[SeizureDetectionViewModel::class.java]
        
        profileViewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(applicationContext, this)
        )[ProfileViewModel::class.java]

        bluetoothViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        )[BluetoothViewModel::class.java]
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        val channel = NotificationChannel(
            getString(R.string.fixed_foregroung_notification_channel_id),
            "Inference Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Permanent notification during foreground operation"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Use alarm sound
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                getString(R.string.seizure_notification_id),
                "Inference Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for inference status"
                setSound(soundUri, audioAttributes) // Set the custom sound
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 500)
            }

            // Register the channel with the system
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        fun getInstance(context: Context): RunningApp {
            return context.applicationContext as RunningApp
        }
    }
}

class AppLifecycleObserver : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    var isAppInForeground = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
    }

    // Implémentation des méthodes requises de ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}