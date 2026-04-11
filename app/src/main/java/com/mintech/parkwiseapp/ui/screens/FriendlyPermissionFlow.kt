package com.mintech.parkwiseapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mintech.parkwiseapp.services.AppLogger
import com.mintech.parkwiseapp.ui.theme.*

@Composable
fun FriendlyPermissionFlow(
    onPermissionsGranted: () -> Unit,
    onCancel: () -> Unit
) {
    var showCustomExplanation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "PermissionFlow"))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val pushGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (audioGranted && pushGranted) {
            AppLogger.logEvent("permissions_granted")
            onPermissionsGranted()
        } else {
            AppLogger.logEvent("permissions_denied")
            onCancel()
        }
    }

    if (showCustomExplanation) {
        AlertDialog(
            onDismissRequest = {
                AppLogger.logEvent("permission_flow_dismissed")
                onCancel()
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = SurfaceHigh, 
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { 
                Text("We need your help!", fontWeight = FontWeight.Bold, color = Color.White) 
            },
            text = {
                Column {
                    Text(
                        "To safely connect you with vehicle owners, Parkwise needs:", 
                        color = OnSurfaceVariant 
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🎤 Microphone: To speak during emergency calls.", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🔔 Notifications: To ring your phone when someone needs you.", color = Color.White)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppLogger.logEvent("permission_flow_continue_clicked")
                        showCustomExplanation = false
                        val permsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permsToRequest.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryApp) 
                ) {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    AppLogger.logEvent("permission_flow_cancel_clicked")
                    onCancel()
                }) { 
                    Text("Maybe Later", color = OnSurfaceVariant) 
                }
            }
        )
    }
}