package com.example.seizureguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.epfl.ch.seizureguard.R

class RunningApp : Application() { // Application class
    // If the notification channel is used in a foreground service, it is better to declare it in the Application class to ensure availability.

    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        appLifecycleObserver = AppLifecycleObserver()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.fixed_foregroung_notification_channel_id),
                "Inference Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Permanent notification during foreground operation"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
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
}

class AppLifecycleObserver :
    DefaultLifecycleObserver { // used to allow the foreground service to be aware of the app's lifecycle state
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
}