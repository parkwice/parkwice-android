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
import org.webrtc.PeerConnection

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_DEBUG", "New FCM Token Generated: $token")

        AppLogger.logEvent("fcm_token_generated")

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

        val data = remoteMessage.data
        Log.d("FCM_DEBUG", "🚨 Push received! Raw Data: $data")

        val callerId = data["callerId"] ?: data["id"]
        if (callerId == null) {
            Log.e("FCM_DEBUG", "❌ Aborting: No callerId or id found in payload!")
            return
        }

        val status = data["status"]
        if (status == "CANCEL_CALL") {
            AppLogger.logEvent("incoming_call_push_cancelled")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(callerId.hashCode())
            sendBroadcast(Intent("CANCEL_CALL_ACTION"))
            Log.d("FCM_DEBUG", "✅ Call cancelled, ringing dismissed.")
            return
        }

        AppLogger.logEvent("incoming_call_push_received")
        val licensePlate = data["licensePlate"] ?: data["handle"] ?: "Vehicle Alert"

        // 🚨 NEW: Parse the dynamic TURN credentials sent by backend
        val turnCredsJson = data["turnCredentials"]
        if (turnCredsJson != null) {
            try {
                val jsonArray = org.json.JSONArray(turnCredsJson)
                val servers = mutableListOf<PeerConnection.IceServer>()
                for (i in 0 until jsonArray.length()) {
                    val serverObj = jsonArray.getJSONObject(i)
                    val urlsArray = serverObj.optJSONArray("urls")
                    val username = serverObj.optString("username", "")
                    val credential = serverObj.optString("credential", "")

                    if (urlsArray != null) {
                        for (j in 0 until urlsArray.length()) {
                            val url = urlsArray.getString(j)
                            val builder = PeerConnection.IceServer.builder(url)
                            if (username.isNotEmpty()) builder.setUsername(username)
                            if (credential.isNotEmpty()) builder.setPassword(credential)
                            servers.add(builder.createIceServer())
                        }
                    }
                }
                if (servers.isNotEmpty()) {
                    SignalingClient.getInstance(applicationContext).currentIceServers = servers
                }
            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "Failed to parse TURN credentials", e)
            }
        }

        SignalingClient.getInstance(applicationContext).notifyCallDelivered(callerId)

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CALLER_ID", callerId)
            putExtra("LICENSE_PLATE", licensePlate)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CALLER_ID", callerId)
            putExtra("LICENSE_PLATE", licensePlate)
            putExtra("AUTO_ACCEPT", true)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, callerId.hashCode() + 2, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, CallRejectReceiver::class.java).apply {
            action = "REJECT_CALL_ACTION"
            putExtra("CALLER_ID", callerId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, callerId.hashCode() + 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "voip_call_channel_v3"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val channel = NotificationChannel(
                channelId, "Incoming Emergency Calls", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rings for incoming Parkwice calls"
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
            .setColor(android.graphics.Color.parseColor("#62C554"))
            .setContentTitle("Incoming Secure Call")
            .setContentText("Vehicle: $licensePlate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "Decline", dismissPendingIntent)
            .addAction(0, "Accept", acceptPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))

        try {
            notificationManager.notify(callerId.hashCode(), notificationBuilder.build())
        } catch (e: SecurityException) {
            AppLogger.logEvent("notification_blocked_by_os")
            Log.e("FCM_DEBUG", "❌ OS blocked notification!", e)
        }
    }
}