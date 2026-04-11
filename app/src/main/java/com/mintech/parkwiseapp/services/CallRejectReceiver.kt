package com.mintech.parkwiseapp.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallRejectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "REJECT_CALL_ACTION") {
            val callerId = intent.getStringExtra("CALLER_ID")
            
            AppLogger.logEvent("call_rejected_from_notification")

            if (callerId != null) {
                // 1. Clear the notification from the system tray
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(callerId.hashCode())
                
                // 🚨 2. Kill the IncomingCallActivity if it is hiding in the recent apps
                context.sendBroadcast(Intent("CANCEL_CALL_ACTION"))

                // 3. Inform OS to stay awake, send the network packet
                val pendingResult = goAsync()
                SignalingClient.getInstance(context).endCall(callerId) {
                    pendingResult.finish() 
                }
            }
        }
    }
}