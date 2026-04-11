package com.mintech.parkwiseapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.CallInitiateRequest
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.services.Vehicle
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import com.mintech.parkwiseapp.services.AppLogger

fun arePermissionsGranted(context: Context): Boolean {
    val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val push = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
    return audio && push
}

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    
    val userEmail = prefs.getString("user_email", "Loading...") ?: ""
    val userPhoto = prefs.getString("user_photo", "") ?: ""
    val jwtToken = prefs.getString("jwt_token", "") ?: ""

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var searchPlate by remember { mutableStateOf("") }
    var isCalling by remember { mutableStateOf(false) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

    var showPermissionFlow by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "DashboardScreen"))
    }

    fun loadVehicles() {
        coroutineScope.launch {
            try {
                val response = ApiService.api.getVehicles("Bearer $jwtToken")
                if (response.isSuccessful) {
                    vehicles = response.body() ?: emptyList()
                    if (vehicles.isNotEmpty() && !arePermissionsGranted(context)) {
                        showPermissionFlow = true
                    }
                }
            } catch (e: Exception) {
                AppLogger.recordError(e, "Failed to load vehicles")
            }
        }
    }

    LaunchedEffect(Unit) { loadVehicles() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawerContent(
                userEmail = userEmail,
                photoUrl = userPhoto,
                onNavigateToAccount = { 
                    coroutineScope.launch { drawerState.close() }
                    navController.navigate("account") 
                },
                onNavigateToHistory = { 
                    coroutineScope.launch { drawerState.close() }
                    navController.navigate("history") 
                }
            )
        }
    ) {
        if (showPermissionFlow) {
            FriendlyPermissionFlow(
                onPermissionsGranted = {
                    showPermissionFlow = false
                    pendingAction?.invoke()
                    pendingAction = null
                },
                onCancel = {
                    showPermissionFlow = false
                    pendingAction = null
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().background(Background)) {
            // --- HEADER (Sticky at Top) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = userPhoto.ifEmpty { "https://ui-avatars.com/api/?name=${userEmail}" },
                    contentDescription = "Profile",
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable {
                        AppLogger.logEvent("drawer_opened")
                        coroutineScope.launch { drawerState.open() }
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Parkwise", color = PrimaryApp, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                }
            }

            // --- FULL SCREEN SCROLLABLE GRID ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Texts
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hassle-Free\nCommunication", color = PrimaryApp, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Calls are completely encrypted; zero data shared.", color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Plate Input Block
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(SurfaceLow, RoundedCornerShape(32.dp))
                            .border(1.dp, PrimaryApp.copy(alpha = 0.1f), RoundedCornerShape(32.dp)).padding(24.dp)
                    ) {
                        Text("Plate Number", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().background(SurfaceLowest, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = searchPlate,
                                onValueChange = { searchPlate = it.uppercase() },
                                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, textColor = PrimaryApp, cursorColor = PrimaryApp),
                                placeholder = { Text("ABC-1234", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isCalling = true
                                AppLogger.logEvent("call_attempted")
                                coroutineScope.launch {
                                    try {
                                        val response = ApiService.api.initiateCall("Bearer $jwtToken", CallInitiateRequest(searchPlate.trim()))
                                        
                                        if (response.isSuccessful && !response.body()?.targetUserId.isNullOrEmpty()) {
                                            AppLogger.logEvent("call_connected")
                                            SignalingClient.getInstance(context).initiateCall(response.body()!!.targetUserId!!)
                                        } else {
                                            val errorMsg = ApiService.extractErrorMessage(response.errorBody())
                                            AppLogger.logEvent("call_failed", mapOf("reason" to errorMsg))
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.recordError(e, "Call init failed")
                                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isCalling = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryApp, disabledBackgroundColor = PrimaryApp.copy(alpha = 0.5f)),
                            enabled = searchPlate.isNotEmpty() && !isCalling
                        ) {
                            if (isCalling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Text("Contact Owner", color = SurfaceLowest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Active Vehicles Title Row
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Active Vehicles", color = PrimaryApp, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.background(SurfaceHigh, RoundedCornerShape(8.dp)).clickable { 
                                    AppLogger.logEvent("add_vehicle_clicked")
                                    if (!arePermissionsGranted(context)) {
                                        showPermissionFlow = true
                                        pendingAction = { navController.navigate("setup") }
                                    } else {
                                        navController.navigate("setup")
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Vehicle", color = PrimaryApp, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // The Vehicles or Empty State
                if (vehicles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No vehicles registered yet.", color = OnSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    }
                } else {
                    items(vehicles) { vehicle ->
                        VehicleCard(vehicle = vehicle) {
                            AppLogger.logEvent("delete_vehicle_clicked")
                            coroutineScope.launch {
                                try {
                                    val res = ApiService.api.deleteVehicle("Bearer $jwtToken", vehicle._id)
                                    if (res.isSuccessful) {
                                        AppLogger.logEvent("delete_vehicle_success")
                                        loadVehicles()
                                    }
                                } catch (e: Exception) {
                                    AppLogger.recordError(e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleCard(vehicle: Vehicle, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.background(SurfaceLow, RoundedCornerShape(16.dp))
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp).height(120.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vehicle.licensePlate, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorApp, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Active", color = OnSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = PrimaryApp.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
        }
    }
}