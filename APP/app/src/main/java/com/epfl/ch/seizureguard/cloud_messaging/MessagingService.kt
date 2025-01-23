package com.epfl.ch.seizureguard.cloud_messaging

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.epfl.ch.seizureguard.MainActivity
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileRepository
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessagingService : FirebaseMessagingService() {

    private val repository: ProfileRepository by lazy {
        val appContext = applicationContext ?: throw IllegalStateException("Application context is null")
        ProfileRepository.getInstance(context = appContext)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profile : Profile

    override fun onCreate() {
        super.onCreate()
        profileViewModel = RunningApp.getInstance(application as RunningApp).profileViewModel
        profile = profileViewModel.profileState.value
    }

    /**
     * Called whenever a new FCM registration token is generated.
     * This happens the first time the app starts and
     * whenever the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                repository.storeFcmToken(profile.uid, token)
            } catch (e: Exception) {
                Log.e("MyFirebaseMsgService", "Error storing FCM token", e)
            }
        }
    }

    /**
     * Called whenever an FCM message is received.
     * This is where you'll handle the data or display a notification.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (!profileViewModel.parentMode.value) {// Only handle notifications in parent mode
            return
        }
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: "No title"
            // Extract latitude and longitude from data
            val latStr = data["latitude"]
            val lonStr = data["longitude"]
            var extractedLocation: Location? = null
            if (latStr != null && lonStr != null) {
                val latitude = latStr.toDoubleOrNull()
                val longitude = lonStr.toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    extractedLocation = Location(LocationManager.GPS_PROVIDER).apply {
                        this.latitude = latitude
                        this.longitude = longitude
                    }
                } else {
                    Log.d("MyFirebaseMsgService", "Invalid lat/long data in message")
                }
            } else {
                Log.d("MyFirebaseMsgService", "No lat/long data found in message")
            }
            val displayMessage = if (extractedLocation != null) {
                "Location received from device"
            } else {
                "Location unavailable"
            }
            showNotification(title, displayMessage, extractedLocation)
        }
    }

    @SuppressLint("ServiceCast")
    private fun showNotification(title: String, message: String, location: Location?) {
        val channelId = "seizureguard_channel_id"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SEIZURE_DETECTED_PARENT", true)
        }
        profileViewModel.setLatestLocation(location)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(
            channelId,
            "SeizureGuard Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        val locationString = if (location != null) {
            "Lat: ${location.latitude}, Long: ${location.longitude}"
        } else {
            "Location unavailable"
        }
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(locationString)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }
}
