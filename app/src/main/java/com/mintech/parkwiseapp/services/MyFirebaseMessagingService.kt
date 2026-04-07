package com.mintech.parkwiseapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
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
        Log.d("FCM_DEBUG", "New FCM Token Generated: $token")
        val jwtToken = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("jwt_token", null)
        if (jwtToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiService.api.syncDeviceToken("Bearer $jwtToken", TokenSyncRequest(token))
                } catch (e: Exception) {
                    Log.e("FCM_DEBUG", "Failed to sync token", e)
                }
            }
        }
    }

    @SuppressLint("ServiceCast")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 🚨 1. LOG THE PAYLOAD TO CATCH SILENT ERRORS
        Log.d("FCM_DEBUG", "🚨 Push received! Raw Data: ${remoteMessage.data}")

        val data = remoteMessage.data
        
        // 🚨 2. MIRROR iOS LOGIC: Check for both possible keys!
        val callerId = data["callerId"] ?: data["id"]
        if (callerId == null) {
            Log.e("FCM_DEBUG", "❌ Aborting: No callerId or id found in payload!")
            return
        }
        
        val licensePlate = data["licensePlate"] ?: data["handle"] ?: "Vehicle Alert"

        Log.d("FCM_DEBUG", "✅ Building Notification for Caller: $callerId, Plate: $licensePlate")

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CALLER_ID", callerId)
            putExtra("LICENSE_PLATE", licensePlate)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "voip_call_channel_v3" // Incremented to force Android to apply Ringtone settings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val channel = NotificationChannel(
                channelId,
                "Incoming Emergency Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rings for incoming Parkwise calls"
                setSound(
                    ringtoneUri,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle("Incoming Secure Call")
            .setContentText("Vehicle: $licensePlate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))

        try {
            notificationManager.notify(callerId.hashCode(), notificationBuilder.build())
            Log.d("FCM_DEBUG", "✅ Notification fired to Android OS successfully!")
        } catch (e: SecurityException) {
            Log.e("FCM_DEBUG", "❌ OS blocked notification! Missing POST_NOTIFICATIONS permission.", e)
        }
    }
}