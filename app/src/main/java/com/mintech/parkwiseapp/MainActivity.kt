package com.mintech.parkwiseapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.ui.screens.ActiveCallScreen
import com.mintech.parkwiseapp.ui.screens.DashboardScreen
import com.mintech.parkwiseapp.ui.screens.LoginScreen
import com.mintech.parkwiseapp.ui.screens.VehicleSetupScreen

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check Android 14+ Full Screen Intent Permission
        checkAndRequestFullScreenPermission()

        // Initialize WebRTC Signaling Client
        val signalingClient = SignalingClient.getInstance(applicationContext)

        setContent {
            // 2. Setup Runtime Permissions (Mic & Notifications)
            val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

            // Request permissions on boot if not all are granted
            LaunchedEffect(Unit) {
                if (!permissionsState.allPermissionsGranted) {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }

            val navController = rememberNavController()
            val isCallActive by signalingClient.isCallActive.collectAsState()

            // --- AUTO-LOGIN LOGIC ---
            // Check if we already have a JWT token saved from a previous session
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val jwtToken = prefs.getString("jwt_token", null)

            // If a token exists, skip the login screen and go straight to dashboard
            val startScreen = if (!jwtToken.isNullOrEmpty()) "dashboard" else "login"

            // 3. Navigation / Screen Routing
            if (isCallActive) {
                // If a call is active, bypass navigation and show the Call UI directly
                ActiveCallScreen(onEndCall = { signalingClient.endCall() })
            } else {
                NavHost(navController = navController, startDestination = startScreen) {

                    composable("login") {
                        LoginScreen(onLoginSuccess = {
                            // Navigate to dashboard and remove login from the backstack
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            onLogout = {
                                // Clear session and return to login
                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },
                            onAddVehicle = { navController.navigate("setup") }
                        )
                    }

                    composable("setup") {
                        VehicleSetupScreen(
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() }
                        )
                    }

                }
            }
        }
    }

    /**
     * Android 14 (API 34+) requires explicit user consent to launch Full-Screen Intents
     * from the background (like our IncomingCallActivity). This redirects them to Settings.
     */
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