package com.mintech.parkwiseapp.services

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AppLogger {
    // 🚨 Use lazy initialization so it waits until Firebase is 100% ready
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }
    private var analytics: FirebaseAnalytics? = null

    fun init(firebaseAnalytics: FirebaseAnalytics) {
        analytics = firebaseAnalytics
    }

    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
        analytics?.setUserId(userId)
    }

    fun clearUser() {
        crashlytics.setUserId("")
        analytics?.setUserId(null)
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        analytics?.logEvent(eventName, bundle)
        crashlytics.log("Event: $eventName | Params: $params")
        Log.d("AppLogger", "Event: $eventName")
    }

    fun recordError(error: Throwable, contextMsg: String = "") {
        if (contextMsg.isNotEmpty()) crashlytics.log(contextMsg)
        crashlytics.recordException(error)
        Log.e("AppLogger", contextMsg, error)
    }
}