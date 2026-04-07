package com.mintech.parkwiseapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mintech.parkwiseapp.IncomingCallActivity
import com.mintech.parkwiseapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send this token to MongoDB! This is the Android equivalent of the voip_token
        val jwtToken = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("jwt_token", null)
        if (jwtToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiService.api.syncDeviceToken("Bearer $jwtToken", TokenSyncRequest(token))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("ServiceCast")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extract the payload from Node.js
        val data = remoteMessage.data
        val callerId = data["callerId"] ?: return
        val licensePlate = data["licensePlate"] ?: "Unknown"

        // 2. Prepare the Intent that points to your Call Screen
        val fullScreenIntent =
                Intent(this, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("CALLER_ID", callerId)
                    putExtra("LICENSE_PLATE", licensePlate)
                }

        // 3. Wrap it in a PendingIntent (Required for Notifications)
        val fullScreenPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "voip_call_channel"

        // 4. Create a High-Priority Notification Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri =
                    android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_RINGTONE
                    )
            val channel =
                    NotificationChannel(
                                    channelId,
                                    "Incoming Calls",
                                    NotificationManager
                                            .IMPORTANCE_HIGH // MUST be HIGH to wake the screen
                            )
                            .apply {
                                description = "Rings for incoming Parkwise emergency calls"
                                // Optional: You can set a custom looping ringtone here
                                setSound(
                                        ringtoneUri,
                                        android.media.AudioAttributes.Builder()
                                                .setContentType(
                                                        android.media.AudioAttributes
                                                                .CONTENT_TYPE_SONIFICATION
                                                )
                                                .setUsage(
                                                        android.media.AudioAttributes
                                                                .USAGE_NOTIFICATION_RINGTONE
                                                )
                                                .build()
                                )
                                enableLights(true)
                                lightColor = android.graphics.Color.GREEN
                                enableVibration(true)
                            }
            notificationManager.createNotificationChannel(channel)
        }

        // 5. Build the Notification with the Magic "FullScreenIntent"
        val notificationBuilder =
                NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(
                                R.drawable.ic_launcher_foreground
                        ) // Replace with your actual app icon
                        .setContentTitle("Incoming Secure Call")
                        .setContentText(licensePlate)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        // 🚨 THIS IS THE MAGIC BULLET FOR BACKGROUND WAKEOUTS:
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)) // Makes it hard to swipe away accidentally

        // 6. Fire it!
        // Using a unique ID (like the callerId hash) ensures multiple calls don't overwrite each
        // other weirdly
        notificationManager.notify(callerId.hashCode(), notificationBuilder.build())
    }
}
