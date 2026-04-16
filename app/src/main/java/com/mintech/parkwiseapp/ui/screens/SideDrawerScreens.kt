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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
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
import com.mintech.parkwiseapp.services.CallRecord
import com.mintech.parkwiseapp.services.GroupedCall
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
                onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        enabled = !isLoggingOut,
                        onClick = {
                            isLoggingOut = true
                            scope.launch {
                                try {
                                    ApiService.api.logout("Bearer $jwtToken")
                                } catch (e: Exception) {
                                    AppLogger.recordError(e, "Logout API failed to clear tokens")
                                } finally {
                                    AppLogger.logEvent("logout_success")
                                    AppLogger.clearUser()
                                    prefs.edit().clear().apply()
                                    isLoggingOut = false
                                    onLogout()
                                }
                            }
                        }
                    ) { 
                        if (isLoggingOut) {
                            CircularProgressIndicator(color = ErrorApp, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Log Out", color = ErrorApp) 
                        }
                    }
                },
                dismissButton = { 
                    TextButton(
                        enabled = !isLoggingOut,
                        onClick = { showLogoutDialog = false }
                    ) { Text("Cancel") } 
                },
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

// 🚨 Safe helper to format backend ISO strings to human-readable dates
fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return "Unknown Time"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString)
        val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        date?.let { formatter.format(it) } ?: isoString
    } catch (e: Exception) {
        isoString
    }
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
        topBar = { TopAppBar(title = { Text("Call History", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
        containerColor = Background
    ) { padding ->
        
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (groupedCalls.isEmpty() && !isLoading) {
                item { Text("No calls yet.", color = OnSurfaceVariant, modifier = Modifier.padding(24.dp)) }
            }
            
            itemsIndexed(groupedCalls) { index, group ->
                if (index == groupedCalls.size - 1 && hasMoreData && !isLoading) {
                    LaunchedEffect(Unit) { loadMore() }
                }
                
                val record = group.latestCall
                val displayPlate = record.licensePlate ?: "Unknown Vehicle"
                
                ListItem(
                    modifier = Modifier.clickable { 
                        onNavigateToDetail(group._id, displayPlate) 
                    },
                    headlineContent = { 
                        Text(
                            text = if (group.totalCalls > 1) "$displayPlate (${group.totalCalls})" else displayPlate, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    supportingContent = { Text("Tap for detailed history", color = OnSurfaceVariant) },
                    colors = ListItemDefaults.colors(containerColor = SurfaceLow),
                    trailingContent = {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Details", tint = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                )
                HorizontalDivider(color = Background)
            }
            
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryApp, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// 🚨 NEW: The iOS-style detail screen that opens when you tap a grouped item
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
            val callsRes = ApiService.api.getCallDetails("Bearer $jwtToken", otherUserId)
            if (callsRes.isSuccessful) detailedCalls = callsRes.body() ?: emptyList()
            
            val blockedRes = ApiService.api.getBlockedUsers("Bearer $jwtToken")
            if (blockedRes.isSuccessful) {
                isBlocked = blockedRes.body()?.contains(otherUserId) == true
            }
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
                navigationIcon = { 
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } 
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = PrimaryApp)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(SurfaceHigh)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock Caller" else "Block Caller", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        if (isBlocked) {
                                            if (ApiService.api.unblockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful) {
                                                AppLogger.logEvent("user_unblocked")
                                                isBlocked = false
                                            }
                                        } else {
                                            if (ApiService.api.blockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful) {
                                                AppLogger.logEvent("user_blocked")
                                                isBlocked = true
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report to Safety Team", color = ErrorApp) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
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
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryApp)
                    }
                }
            } else if (detailedCalls.isEmpty()) {
                item { Text("No history found.", color = OnSurfaceVariant, modifier = Modifier.padding(24.dp)) }
            } else {
                items(detailedCalls) { call ->
                    val isOutgoing = call.callerId == myUserId
                    
                    ListItem(
                        headlineContent = { 
                            Text(if (isOutgoing) "Outgoing Call" else "Incoming Call", color = if (isOutgoing) PrimaryApp else Color.White) 
                        },
                        supportingContent = { Text(formatIsoDate(call.createdAt), color = OnSurfaceVariant) },
                        colors = ListItemDefaults.colors(containerColor = SurfaceLow),
                        trailingContent = {
                            Icon(
                                imageVector = if (isOutgoing) Icons.Filled.CallMade else Icons.Filled.CallReceived, 
                                contentDescription = null, 
                                tint = if (isOutgoing) PrimaryApp else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    HorizontalDivider(color = Background)
                }
            }
        }
        
        if (showReportDialog) {
            ReportUserDialog(
                userId = otherUserId,
                token = jwtToken,
                onDismiss = { showReportDialog = false }
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