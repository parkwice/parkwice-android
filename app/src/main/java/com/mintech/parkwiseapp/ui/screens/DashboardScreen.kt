package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.CallInitiateRequest
import com.mintech.parkwiseapp.services.SignalingClient
import com.mintech.parkwiseapp.services.Vehicle
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(onLogout: () -> Unit, onAddVehicle: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val userEmail = prefs.getString("user_email", "Loading...") ?: ""
    val jwtToken = prefs.getString("jwt_token", "") ?: ""

    var searchPlate by remember { mutableStateOf("") }
    var isCalling by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

    // Fetch vehicles when the screen loads
    fun loadVehicles() {
        coroutineScope.launch {
            try {
                vehicles = ApiService.api.getVehicles("Bearer $jwtToken")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) { loadVehicles() }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out of your account?") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showLogoutDialog = false
                                prefs.edit().clear().apply() // Clear session
                                onLogout()
                            }
                    ) { Text("Logout", color = ErrorApp, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = OnSurfaceVariant)
                    }
                },
                backgroundColor = SurfaceHigh,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {

        // --- CUSTOM HEADER ---
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier.size(44.dp).background(SurfaceHigh, CircleShape),
                    contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryApp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Parkwise", color = PrimaryApp, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(userEmail, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = PrimaryApp)
            }
        }

        // --- MAIN SCROLLABLE CONTENT ---
        Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    "Hassle-Free\nCommunication",
                    color = PrimaryApp,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Calls are completely encrypted; zero data shared.", color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // Plate Number Input Card
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(SurfaceLow, RoundedCornerShape(32.dp))
                                    .border(
                                            1.dp,
                                            PrimaryApp.copy(alpha = 0.1f),
                                            RoundedCornerShape(32.dp)
                                    )
                                    .padding(24.dp)
            ) {
                Text("Plate Number", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(SurfaceLowest, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                            value = searchPlate,
                            onValueChange = { searchPlate = it.uppercase() },
                            colors =
                                    TextFieldDefaults.textFieldColors(
                                            backgroundColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            textColor = PrimaryApp,
                                            cursorColor = PrimaryApp
                                    ),
                            placeholder = {
                                Text(
                                        "ABC-1234",
                                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                )
                            },
                            textStyle =
                                    LocalTextStyle.current.copy(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                    ),
                            modifier = Modifier.weight(1f)
                    )
                    Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Initiate Call Button
                // Initiate Call Button
                Button(
                        onClick = {
                            isCalling = true
                            coroutineScope.launch {
                                try {
                                    val response =
                                            ApiService.api.initiateCall(
                                                    "Bearer $jwtToken",
                                                    CallInitiateRequest(searchPlate.trim())
                                            )
                                    // Check if we actually got an ID instead of checking for a fake
                                    // 'success' boolean
                                    if (!response.targetUserId.isNullOrEmpty()) {
                                        SignalingClient.getInstance(context)
                                                .initiateCall(response.targetUserId)
                                    } else {
                                        android.widget.Toast.makeText(
                                                        context,
                                                        "Vehicle not found",
                                                        android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                                } catch (e: Exception) {
                                    // Network failure or Server threw an error (like a 404)
                                    android.util.Log.e("CallError", "Failed to initiate call", e)
                                    android.widget.Toast.makeText(
                                                    context,
                                                    "Server Error: Check your connection",
                                                    android.widget.Toast.LENGTH_SHORT
                                            )
                                            .show()
                                } finally {
                                    isCalling = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        backgroundColor = PrimaryApp,
                                        disabledBackgroundColor = PrimaryApp.copy(alpha = 0.5f)
                                ),
                        enabled = searchPlate.isNotEmpty() && !isCalling
                ) {
                    if (isCalling) {
                        CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = SurfaceLowest)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                "Contact Owner",
                                color = SurfaceLowest,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active Vehicles Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        "Active Vehicles",
                        color = PrimaryApp,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                        modifier =
                                Modifier.background(SurfaceHigh, RoundedCornerShape(8.dp))
                                        .clickable { onAddVehicle() }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = PrimaryApp,
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            "Add Vehicle",
                            color = PrimaryApp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicles Grid
            if (vehicles.isEmpty()) {
                Text(
                        "No vehicles registered yet.",
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(vehicles) { vehicle ->
                        VehicleCard(
                                vehicle = vehicle,
                                onDelete = {
                                    coroutineScope.launch {
                                        try {
                                            ApiService.api.deleteVehicle(
                                                    "Bearer $jwtToken",
                                                    vehicle._id
                                            )
                                            loadVehicles() // Refresh list after delete
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

// Sub-component for the Grid
@Composable
fun VehicleCard(vehicle: Vehicle, onDelete: () -> Unit) {
    Column(
            modifier =
                    Modifier.background(SurfaceLow, RoundedCornerShape(16.dp))
                            .border(
                                    1.dp,
                                    OnSurfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                            .height(120.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                    vehicle.licensePlate,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = ErrorApp,
                        modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.Bottom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryApp,
                        modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Active", color = OnSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = PrimaryApp.copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
            )
        }
    }
}
