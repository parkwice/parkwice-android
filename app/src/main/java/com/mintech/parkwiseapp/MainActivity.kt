package com.mintech.parkwiseapp

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.ui.screens.*
import com.mintech.parkwiseapp.services.AppLogger

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Analytics
        AppLogger.init(FirebaseAnalytics.getInstance(this))

        val darkAppColor = android.graphics.Color.parseColor("#1A232A")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkAppColor),
            navigationBarStyle = SystemBarStyle.dark(darkAppColor)
        )

        checkAndRequestFullScreenPermission()
        val signalingClient = SignalingClient.getInstance(applicationContext)

        setContent {
            val navController = rememberNavController()
            val isCallActive by signalingClient.isCallActive.collectAsState()

            val context = LocalContext.current
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val jwtToken = prefs.getString("jwt_token", null)

            val startScreen = if (!jwtToken.isNullOrEmpty()) "dashboard" else "login"

            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                if (isCallActive) {
                    ActiveCallScreen(onEndCall = { signalingClient.endCall() })
                } else {
                    NavHost(navController = navController, startDestination = startScreen) {

                        composable("login") {
                            LoginScreen(onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            })
                        }

                        composable("dashboard") {
                            DashboardScreen(navController = navController)
                        }

                        composable("setup") {
                            VehicleSetupScreen(
                                onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() }
                            )
                        }
                        
                        composable("history") {
                            CallHistoryScreen(onBack = { navController.popBackStack() })
                        }
                        
                        composable("account") {
                            AccountScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestFullScreenPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}