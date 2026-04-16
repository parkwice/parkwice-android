package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.CallInitiateRequest
import com.mintech.parkwiseapp.services.CallRecord
import com.mintech.parkwiseapp.services.GroupedCall
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import com.mintech.parkwiseapp.services.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    var isLoggingOut by remember { mutableStateOf(false) }
    
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
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow)
            ) { Text("Log Out", color = ErrorApp) }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Advanced", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow)
            ) { Text("Delete Account", color = ErrorApp) }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        enabled = !isLoggingOut,
                        onClick = {
                            isLoggingOut = true
                            scope.launch {
                                try { ApiService.api.logout("Bearer $jwtToken") } catch (e: Exception) {} 
                                finally {
                                    AppLogger.clearUser()
                                    prefs.edit().clear().apply()
                                    isLoggingOut = false
                                    onLogout()
                                }
                            }
                        }
                    ) { 
                        if (isLoggingOut) CircularProgressIndicator(color = ErrorApp, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Log Out", color = ErrorApp) 
                    }
                },
                dismissButton = { TextButton(enabled = !isLoggingOut, onClick = { showLogoutDialog = false }) { Text("Cancel") } },
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
                            if (ApiService.api.deleteAccount("Bearer $jwtToken").isSuccessful) {
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

// 🚨 Shimmer Placeholder for Loading States
@Composable
fun ShimmerHistoryItem() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = alpha), CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.5f).background(Color.White.copy(alpha = alpha), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.3f).background(Color.White.copy(alpha = alpha), RoundedCornerShape(4.dp)))
        }
    }
}

fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return "Unknown Time"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString)
        val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        date?.let { formatter.format(it) } ?: isoString
    } catch (e: Exception) { isoString }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(onBack: () -> Unit, onNavigateToDetail: (String, String) -> Unit) {
    val context = LocalContext.current
    val jwtToken = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: ""
    
    var groupedCalls by remember { mutableStateOf<List<GroupedCall>>(emptyList()) }
    var currentPage by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMoreData by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()

    fun loadMore() {
        if (isLoading || !hasMoreData) return
        isLoading = true
        scope.launch {
            try {
                // 1. Fetch exactly what the backend grouped for us
                val res = ApiService.api.getGroupedCalls("Bearer $jwtToken", currentPage, 15)
                if (res.isSuccessful) {
                    val newCalls = res.body() ?: emptyList()
                    if (newCalls.size < 15) hasMoreData = false
                    groupedCalls = groupedCalls + newCalls
                    currentPage++
                } else {
                    Toast.makeText(context, "Could not load history.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                AppLogger.recordError(e, "History fetch failed")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "CallHistoryScreen"))
        loadMore()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recents", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
        containerColor = Background
    ) { padding ->
        
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (groupedCalls.isEmpty() && !isLoading) {
                item { 
                    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(48.dp), tint = OnSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No recent calls.", color = OnSurfaceVariant) 
                    }
                }
            } else {
                itemsIndexed(groupedCalls) { index, group ->
                    if (index == groupedCalls.lastIndex && hasMoreData && !isLoading) {
                        LaunchedEffect(group._id) { loadMore() }
                    }
                    
                    val displayPlate = group.latestCall.licensePlate ?: "Unknown Vehicle"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onNavigateToDetail(group._id, displayPlate) } // Pass otherUserId & Plate
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).background(SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = displayPlate, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (group.totalCalls > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "(${group.totalCalls})", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Tap for detailed history", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Details", tint = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                    HorizontalDivider(color = SurfaceHigh)
                }
            }
            if (isLoading) {
                items(3) { ShimmerHistoryItem() }
            }
        }
    }
}

// 🚨 Call Detail Screen cleanly fetches by otherUserId
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(otherUserId: String, licensePlate: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val jwtToken = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: ""
    val myUserId = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("user_id", "") ?: ""
    
    var detailedCalls by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isBlocked by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "CallDetailScreen"))
        try {
            // 1. Fetch user's calls directly using the simple endpoint
            val callsRes = ApiService.api.getCallDetails("Bearer $jwtToken", otherUserId)
            if (callsRes.isSuccessful) detailedCalls = callsRes.body() ?: emptyList()
            
            // 2. See if we have blocked them
            val blockedRes = ApiService.api.getBlockedUsers("Bearer $jwtToken")
            if (blockedRes.isSuccessful) isBlocked = blockedRes.body()?.contains(otherUserId) == true
        } catch (e: Exception) {
            AppLogger.recordError(e, "Call details fetch failed")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(licensePlate, color = Color.White) }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = PrimaryApp) }
                        
                        // 🚨 Styled Top-Right Popup Menu
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(SurfaceHigh, RoundedCornerShape(12.dp)).border(1.dp, OnSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(4.dp)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (isBlocked) Icons.Filled.CheckCircle else Icons.Filled.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(if (isBlocked) "Unblock User" else "Block User", color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        if (isBlocked) {
                                            if (ApiService.api.unblockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful) isBlocked = false
                                        } else {
                                            if (ApiService.api.blockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful) isBlocked = true
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Report, contentDescription = null, tint = ErrorApp, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Report User", color = ErrorApp, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = { showMenu = false; showReportDialog = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            ) 
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                items(6) { ShimmerHistoryItem() }
            } else if (detailedCalls.isEmpty()) {
                item { Text("No history found.", color = OnSurfaceVariant, modifier = Modifier.padding(24.dp)) }
            } else {
                item {
                    Text("CALL HISTORY", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp))
                }
                
                // Detailed items
                items(detailedCalls) { call ->
                    val isOutgoing = call.callerId == myUserId
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SurfaceLow).padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isOutgoing) "Outgoing Call" else "Incoming Call", color = if (isOutgoing) PrimaryApp else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatIsoDate(call.createdAt), color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = if (isOutgoing) Icons.Filled.CallMade else Icons.Filled.CallReceived, 
                            contentDescription = null, 
                            tint = if (isOutgoing) PrimaryApp else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(color = Background)
                }
            }
        }
        
        if (showReportDialog) {
            ReportUserDialog(userId = otherUserId, token = jwtToken, onDismiss = { showReportDialog = false })
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
                    if (ApiService.api.reportUser("Bearer $token", mapOf("targetId" to userId, "reason" to selectedReason)).isSuccessful) {
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