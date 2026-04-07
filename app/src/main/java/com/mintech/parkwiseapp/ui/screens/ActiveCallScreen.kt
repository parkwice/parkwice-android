package com.mintech.parkwiseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintech.parkwiseapp.ui.theme.*

@Composable
fun ActiveCallScreen(onEndCall: () -> Unit) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SurfaceHigh, Background)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text("SECURE CONNECTION", color = PrimaryApp, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting...", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            // Shield Graphic
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(260.dp).border(2.dp, PrimaryApp.copy(alpha = 0.1f), CircleShape))
                Box(modifier = Modifier.size(210.dp).border(2.dp, PrimaryApp.copy(alpha = 0.3f), CircleShape))
                Box(modifier = Modifier.size(160.dp).background(SurfaceHigh, CircleShape).border(2.dp, PrimaryApp.copy(alpha = 0.5f), CircleShape))
                Icon(Icons.Filled.Shield, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Hardware Controls
            Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                ControlButton(icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, label = "MUTE", isActive = isMuted) { isMuted = !isMuted }
                ControlButton(icon = if (isSpeaker) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown, label = "SPEAKER", isActive = isSpeaker) { isSpeaker = !isSpeaker }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // End Call Button
            Row(
                modifier = Modifier
                    .background(ErrorApp, CircleShape)
                    .clickable { onEndCall() }
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).padding(12.dp)) {
                    Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("End Call", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(64.dp).background(if (isActive) Color.White else SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isActive) SurfaceLowest else OnSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}