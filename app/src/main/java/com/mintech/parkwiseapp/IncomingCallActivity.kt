package com.mintech.parkwiseapp

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent // 🚨 THE FIX: Added the missing Intent import
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.ui.theme.*

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force screen to wake up and bypass lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val callerId = intent.getStringExtra("CALLER_ID") ?: ""
        val licensePlate = intent.getStringExtra("LICENSE_PLATE") ?: "Vehicle Alert"

        setContent {
            IncomingCallScreen(
                licensePlate = licensePlate,
                onAccept = {
                    // 1. Tell WebRTC to connect
                    SignalingClient.getInstance(applicationContext).acceptCallBackground(callerId)
                    
                    // 2. Launch MainActivity to show the Active Call UI
                    val mainIntent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                        // Clear the backstack so the user doesn't hit "back" into a dead call screen
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(mainIntent)
                    
                    // 3. Now close the ringing screen
                    finish()
                },
                onDecline = {
                    SignalingClient.getInstance(applicationContext).endCall(callerId)
                    finish()
                }
            )
        }
    }
}

@Composable
fun IncomingCallScreen(licensePlate: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    // Infinite pulse animation for the ringing effect
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SurfaceHigh, Background)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // App Branding
            Text("Parkwise", color = OnSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Spacer(modifier = Modifier.height(32.dp))

            Text("Incoming Secure Call", color = PrimaryApp, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(licensePlate, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Text("Vehicle Owner", color = OnSurfaceVariant, fontSize = 16.sp)

            Spacer(modifier = Modifier.weight(1f))

            // Animated Center Avatar
            Box(contentAlignment = Alignment.Center) {
                // The expanding/fading pulse ring
                Box(modifier = Modifier.size(140.dp).scale(pulseScale).background(PrimaryApp.copy(alpha = pulseAlpha), CircleShape))
                // The solid inner circle
                Box(modifier = Modifier.size(120.dp).background(SurfaceLow, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Decline Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier.size(72.dp).background(ErrorApp, CircleShape)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Decline", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Accept Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier.size(72.dp).background(PrimaryApp, CircleShape)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Accept", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}