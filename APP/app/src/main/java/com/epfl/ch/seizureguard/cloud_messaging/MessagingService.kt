package com.epfl.ch.seizureguard.cloud_messaging

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.epfl.ch.seizureguard.MainActivity
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.RunningApp
import com.epfl.ch.seizureguard.inference.InferenceService
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
        Log.d("MyFirebaseMsgService", "Refreshed token: $token")
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
        if (!profileViewModel.parentMode.value) { // do not listen for notifications in non-parent mode
            Log.d("MyFirebaseMsgService", "Ignoring notification due to parentMode being false.")
            return
        }
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: "No title"
            val body = data["body"] ?: "No body"
            // --- PARSE LOCATION FROM BODY ---
            val locationRegex = Regex("""Location:\s*Lat:\s*([-0-9.]+),\s*Long:\s*([-0-9.]+)""")
            val matchResult = locationRegex.find(body)
            var extractedLocation: Location? = null
            if (matchResult != null) {
                // groupValues[1] = the latitude string
                // groupValues[2] = the longitude string
                val latStr = matchResult.groupValues[1]
                val lonStr = matchResult.groupValues[2]
                val latitude = latStr.toDoubleOrNull()
                val longitude = lonStr.toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    // Construct a Location object using the parsed coords
                    extractedLocation = Location(LocationManager.GPS_PROVIDER).apply {
                        this.latitude = latitude
                        this.longitude = longitude
                    }
                    Log.d("MyFirebaseMsgService", "Parsed location from notification: Latitude${extractedLocation.latitude}, Longitude: ${extractedLocation.longitude}")
                }else{
                    Log.d("MyFirebaseMsgService", "Impossible to parse location")

                }
            }

            showNotification(title, body, extractedLocation)
        }
    }

    @SuppressLint("ServiceCast")
    private fun showNotification(title: String, message: String, location: Location?) {
        // A simple example of building and showing a notification:
        val channelId = "seizureguard_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SEIZURE_DETECTED_PARENT", true)
            if(location != null){
                putExtra("EXTRA_LATITUDE", location.latitude)
                putExtra("EXTRA_LONGITUDE", location.longitude)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create a notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SeizureGuard Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon) // Replace with your icon
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        // Issue the notification
        notificationManager.notify(0, notificationBuilder.build())
    }
}
