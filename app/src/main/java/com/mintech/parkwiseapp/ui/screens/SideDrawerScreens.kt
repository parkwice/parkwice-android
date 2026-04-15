package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.CallInitiateRequest
import com.mintech.parkwiseapp.services.CallRecord
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import com.mintech.parkwiseapp.services.AppLogger

@Composable
fun SideDrawerContent(
    userEmail: String,
    photoUrl: String,
    onNavigateToAccount: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = SurfaceHigh
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            AsyncImage(
                model = photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${userEmail}" },
                contentDescription = "Profile Photo",
                modifier = Modifier.size(64.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Parkwise", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = OnSurfaceVariant.copy(alpha = 0.2f))

            NavigationDrawerItem(
                label = { Text("Account", color = Color.White) },
                selected = false,
                onClick = onNavigateToAccount,
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
            NavigationDrawerItem(
                label = { Text("Call History", color = Color.White) },
                selected = false,
                onClick = onNavigateToHistory,
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = OnSurfaceVariant.copy(alpha = 0.2f))

            val openUrl = { url: String, eventName: String -> 
                AppLogger.logEvent(eventName)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) 
            }
            
            NavigationDrawerItem(label = { Text("Terms and Conditions", color = Color.White) }, selected = false, onClick = { openUrl("https://parkwice.com/terms.html", "terms_clicked") }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
            NavigationDrawerItem(label = { Text("Privacy Policy", color = Color.White) }, selected = false, onClick = { openUrl("https://parkwice.com/privacy.html", "privacy_clicked") }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
            NavigationDrawerItem(label = { Text("Contact Support", color = Color.White) }, selected = false, onClick = { openUrl("https://parkwice.com/contact.html", "contact_support_clicked") }, colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val jwtToken = prefs.getString("jwt_token", "") ?: ""

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "AccountScreen"))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Account", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
        containerColor = Background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            
            Button(
                onClick = { 
                    AppLogger.logEvent("logout_clicked")
                    showLogoutDialog = true 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow)
            ) { Text("Log Out", color = ErrorApp) }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Advanced", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { 
                    AppLogger.logEvent("delete_account_clicked")
                    showDeleteDialog = true 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow)
            ) { Text("Delete Account", color = ErrorApp) }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(onClick = {
                        AppLogger.logEvent("logout_success")
                        AppLogger.clearUser()
                        prefs.edit().clear().apply()
                        onLogout()
                    }) { Text("Log Out", color = ErrorApp) }
                },
                dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
                containerColor = SurfaceHigh, textContentColor = Color.White, titleContentColor = Color.White
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account?") },
                text = { Text("This will permanently delete your vehicles, history, and account. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val res = ApiService.api.deleteAccount("Bearer $jwtToken")
                            if (res.isSuccessful) {
                                AppLogger.logEvent("delete_account_success")
                                AppLogger.clearUser()
                                prefs.edit().clear().apply()
                                onLogout()
                            }
                        }
                    }) { Text("Delete", color = ErrorApp) }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
                containerColor = SurfaceHigh, textContentColor = Color.White, titleContentColor = Color.White
            )
        }
    }
}

// 🚨 NEW: Data class and algorithm to group consecutive calls
data class GroupedCallRecord(
    val licensePlate: String,
    val callerId: String?,
    val receiverId: String?,
    val lastCallTime: String?,
    val callCount: Int
)

fun groupHistory(history: List<CallRecord>): List<GroupedCallRecord> {
    if (history.isEmpty()) return emptyList()
    val grouped = mutableListOf<GroupedCallRecord>()
    var currentGroup = mutableListOf(history[0])

    for (i in 1 until history.size) {
        val record = history[i]
        // Group consecutive records with the same license plate
        if (record.licensePlate == currentGroup.first().licensePlate) {
            currentGroup.add(record)
        } else {
            grouped.add(
                GroupedCallRecord(
                    licensePlate = currentGroup.first().licensePlate,
                    callerId = currentGroup.first().callerId,
                    receiverId = currentGroup.first().receiverId,
                    lastCallTime = currentGroup.first().createdAt,
                    callCount = currentGroup.size
                )
            )
            currentGroup = mutableListOf(record)
        }
    }
    if (currentGroup.isNotEmpty()) {
        grouped.add(
            GroupedCallRecord(
                licensePlate = currentGroup.first().licensePlate,
                callerId = currentGroup.first().callerId,
                receiverId = currentGroup.first().receiverId,
                lastCallTime = currentGroup.first().createdAt,
                callCount = currentGroup.size
            )
        )
    }
    return grouped
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val jwtToken = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: ""
    
    var history by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var blockedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedUserIdForReport by remember { mutableStateOf<String?>(null) }
    
    // 🚨 NEW: Track loading state for inline calling
    var callingPlate by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "CallHistoryScreen"))
        val historyRes = ApiService.api.getCallHistory("Bearer $jwtToken")
        val blockedRes = ApiService.api.getBlockedUsers("Bearer $jwtToken")
        if (historyRes.isSuccessful) history = historyRes.body() ?: emptyList()
        if (blockedRes.isSuccessful) blockedIds = blockedRes.body()?.toSet() ?: emptySet()
    }

    val groupedHistory = remember(history) { groupHistory(history) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Call History", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
        containerColor = Background
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (groupedHistory.isEmpty()) {
                item { Text("No calls yet.", color = OnSurfaceVariant, modifier = Modifier.padding(24.dp)) }
            }
            items(groupedHistory) { record ->
                val isBlocked = blockedIds.contains(record.callerId)
                
                ListItem(
                    headlineContent = { 
                        Text(
                            text = if (record.callCount > 1) "${record.licensePlate} (${record.callCount})" else record.licensePlate, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    supportingContent = { Text(record.lastCallTime ?: "Unknown time", color = OnSurfaceVariant) },
                    colors = ListItemDefaults.colors(containerColor = SurfaceLow),
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            
                            // 🚨 NEW: The Call Button
                            if (callingPlate == record.licensePlate) {
                                CircularProgressIndicator(
                                    color = PrimaryApp,
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        callingPlate = record.licensePlate
                                        AppLogger.logEvent("call_attempted_from_history")
                                        scope.launch {
                                            try {
                                                val response = ApiService.api.initiateCall("Bearer $jwtToken", CallInitiateRequest(record.licensePlate))
                                                if (response.isSuccessful && !response.body()?.targetUserId.isNullOrEmpty()) {
                                                    AppLogger.logEvent("call_connected")
                                                    SignalingClient.getInstance(context).initiateCall(response.body()!!.targetUserId!!)
                                                } else {
                                                    val errorMsg = ApiService.extractErrorMessage(response.errorBody())
                                                    AppLogger.logEvent("call_failed", mapOf("reason" to errorMsg))
                                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                AppLogger.recordError(e, "Call init failed from history")
                                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                callingPlate = null
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call", tint = PrimaryApp)
                                }
                            }

                            TextButton(onClick = {
                                scope.launch {
                                    val id = record.callerId ?: return@launch
                                    if (isBlocked) {
                                        if (ApiService.api.unblockUser("Bearer $jwtToken", mapOf("targetId" to id)).isSuccessful) {
                                            AppLogger.logEvent("user_unblocked")
                                            blockedIds = blockedIds - id
                                        }
                                    } else {
                                        if (ApiService.api.blockUser("Bearer $jwtToken", mapOf("targetId" to id)).isSuccessful) {
                                            AppLogger.logEvent("user_blocked")
                                            blockedIds = blockedIds + id
                                        }
                                    }
                                }
                            }) { Text(if (isBlocked) "Unblock" else "Block", color = if (isBlocked) Color.Green else ErrorApp, fontWeight = FontWeight.Bold) }
                            
                            TextButton(onClick = { 
                                AppLogger.logEvent("report_user_clicked")
                                selectedUserIdForReport = record.callerId 
                            }) {
                                Text("Report", color = Color(0xFFFFA500), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
                HorizontalDivider(color = Background)
            }
        }

        if (selectedUserIdForReport != null) {
            ReportUserDialog(
                userId = selectedUserIdForReport!!,
                token = jwtToken,
                onDismiss = { selectedUserIdForReport = null }
            )
        }
    }
}

@Composable
fun ReportUserDialog(userId: String, token: String, onDismiss: () -> Unit) {
    var selectedReason by remember { mutableStateOf("Harassment / Abuse") }
    val reasons = listOf("Harassment / Abuse", "Spam", "Inappropriate Behavior", "Other")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report User") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = (reason == selectedReason), onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryApp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val res = ApiService.api.reportUser("Bearer $token", mapOf("targetId" to userId, "reason" to selectedReason))
                    if (res.isSuccessful) {
                        AppLogger.logEvent("user_reported", mapOf("reason" to selectedReason))
                        Toast.makeText(context, "Report Submitted", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            }) { Text("Submit", color = ErrorApp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceVariant) } },
        containerColor = SurfaceHigh, textContentColor = Color.White, titleContentColor = Color.White
    )
}