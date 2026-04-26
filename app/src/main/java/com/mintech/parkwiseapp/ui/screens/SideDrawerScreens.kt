package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.CallRecord
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import com.mintech.parkwiseapp.services.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SideDrawerContent(
    userEmail: String,
    photoUrl: String,
    onNavigateToAccount: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToWebView: (title: String, url: String) -> Unit
) {
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

            NavigationDrawerItem(
                label = { Text("Terms and Conditions", color = Color.White) },
                selected = false,
                onClick = {
                    AppLogger.logEvent("terms_clicked")
                    onNavigateToWebView("Terms and Conditions", "https://parkwice.com/terms.html")
                },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
            NavigationDrawerItem(
                label = { Text("Privacy Policy", color = Color.White) },
                selected = false,
                onClick = {
                    AppLogger.logEvent("privacy_clicked")
                    onNavigateToWebView("Privacy Policy", "https://parkwice.com/privacy.html")
                },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
            NavigationDrawerItem(
                label = { Text("Contact Support", color = Color.White) },
                selected = false,
                onClick = {
                    AppLogger.logEvent("contact_support_clicked")
                    onNavigateToWebView("Contact Support", "https://parkwice.com/contact.html")
                },
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
            )
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
        val versionName = remember {
            try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "" }
        }

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

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Version $versionName",
                color = OnSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "WebViewScreen", "url" to url))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            if (uri.scheme == "mailto") {
                                ctx.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(url)
                }
            }
        )
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

private sealed class HistoryListItem {
    data class Header(val label: String) : HistoryListItem()
    data class Item(val call: CallRecord) : HistoryListItem()
}

private fun buildDaySections(calls: List<CallRecord>): List<HistoryListItem> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displaySdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val todayStr = sdf.format(java.util.Date())
    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val yesterdayStr = sdf.format(cal.time)

    val result = mutableListOf<HistoryListItem>()
    var lastDay = ""
    for (call in calls) {
        val dayStr = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = parser.parse(call.createdAt ?: "") ?: continue
            sdf.format(date)
        } catch (e: Exception) { continue }

        if (dayStr != lastDay) {
            val label = when (dayStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> try { displaySdf.format(sdf.parse(dayStr)!!) } catch (e: Exception) { dayStr }
            }
            result.add(HistoryListItem.Header(label))
            lastDay = dayStr
        }
        result.add(HistoryListItem.Item(call))
    }
    return result
}

private fun formatCallTime(isoString: String?): String {
    if (isoString.isNullOrEmpty()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString) ?: return ""
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) { "" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val jwtToken = prefs.getString("jwt_token", "") ?: ""
    val myUserId = prefs.getString("user_id", "") ?: ""

    var calls by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var currentPage by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMoreData by remember { mutableStateOf(true) }
    var blockedUserIds by remember { mutableStateOf(setOf<String>()) }
    var reportDialogUserId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadMore() {
        if (isLoading || !hasMoreData) return
        isLoading = true
        scope.launch {
            try {
                val res = ApiService.api.getCallHistoryList("Bearer $jwtToken", currentPage, 20)
                if (res.isSuccessful) {
                    val newCalls = res.body() ?: emptyList()
                    if (newCalls.size < 20) hasMoreData = false
                    calls = calls + newCalls
                    currentPage++
                }
            } catch (e: Exception) {
                AppLogger.recordError(e, "History list fetch failed")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "CallHistoryScreen"))
        scope.launch {
            try {
                val res = ApiService.api.getBlockedUsers("Bearer $jwtToken")
                if (res.isSuccessful) blockedUserIds = res.body()?.toSet() ?: emptySet()
            } catch (e: Exception) {}
        }
        loadMore()
    }

    val listItems = remember(calls) { buildDaySections(calls) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recents", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (listItems.isEmpty() && !isLoading) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(48.dp), tint = OnSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No recent calls.", color = OnSurfaceVariant)
                    }
                }
            }

            items(listItems, key = { item ->
                when (item) {
                    is HistoryListItem.Header -> "header_${item.label}"
                    is HistoryListItem.Item -> item.call._id
                }
            }) { item ->
                when (item) {
                    is HistoryListItem.Header -> {
                        Text(
                            text = item.label.uppercase(),
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                    is HistoryListItem.Item -> {
                        val call = item.call
                        val isOutgoing = call.callerId == myUserId
                        val otherUserId = call.otherUserId
                            ?: if (isOutgoing) call.receiverId else call.callerId
                        val isBlocked = otherUserId != null && blockedUserIds.contains(otherUserId)

                        // Trigger next page load when we reach the last call item
                        val callItems = listItems.filterIsInstance<HistoryListItem.Item>()
                        if (call._id == callItems.lastOrNull()?.call?._id && hasMoreData && !isLoading) {
                            LaunchedEffect(call._id) { loadMore() }
                        }

                        CallHistoryListItem(
                            call = call,
                            isOutgoing = isOutgoing,
                            isBlocked = isBlocked,
                            onBlock = {
                                if (otherUserId != null) scope.launch {
                                    if (ApiService.api.blockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful)
                                        blockedUserIds = blockedUserIds + otherUserId
                                }
                            },
                            onUnblock = {
                                if (otherUserId != null) scope.launch {
                                    if (ApiService.api.unblockUser("Bearer $jwtToken", mapOf("targetId" to otherUserId)).isSuccessful)
                                        blockedUserIds = blockedUserIds - otherUserId
                                }
                            },
                            onReport = { if (otherUserId != null) reportDialogUserId = otherUserId }
                        )
                        HorizontalDivider(color = SurfaceHigh.copy(alpha = 0.6f))
                    }
                }
            }

            if (isLoading) {
                items(3) { ShimmerHistoryItem() }
            }
        }

        reportDialogUserId?.let { userId ->
            ReportUserDialog(userId = userId, token = jwtToken, onDismiss = { reportDialogUserId = null })
        }
    }
}

@Composable
private fun CallHistoryListItem(
    call: CallRecord,
    isOutgoing: Boolean,
    isBlocked: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onReport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().background(Background).padding(start = 20.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction icon
        Icon(
            imageVector = if (isOutgoing) Icons.Filled.CallMade else Icons.Filled.CallReceived,
            contentDescription = if (isOutgoing) "Outgoing" else "Incoming",
            tint = if (isOutgoing) PrimaryApp else OnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))

        // Main content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = call.licensePlate ?: "Unknown Vehicle",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isBlocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BLOCKED",
                        color = ErrorApp,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(ErrorApp.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${if (isOutgoing) "Outgoing" else "Incoming"}  ·  ${formatCallTime(call.createdAt)}",
                color = OnSurfaceVariant,
                fontSize = 13.sp
            )
        }

        // Three-dot menu
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = OnSurfaceVariant)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(SurfaceHigh, RoundedCornerShape(12.dp))
                    .border(1.dp, OnSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isBlocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isBlocked) "Unblock User" else "Block User", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    },
                    onClick = {
                        showMenu = false
                        if (isBlocked) onUnblock() else onBlock()
                    }
                )
                HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 8.dp))
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Report, contentDescription = null, tint = ErrorApp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Report User", color = ErrorApp, fontWeight = FontWeight.Medium)
                        }
                    },
                    onClick = { showMenu = false; onReport() }
                )
            }
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