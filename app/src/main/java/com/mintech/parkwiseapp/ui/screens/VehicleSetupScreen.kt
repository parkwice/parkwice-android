package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.VehicleRequest
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import com.mintech.parkwiseapp.services.AppLogger

@Composable
fun VehicleSetupScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    var plate by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Secure your\nvehicle", color = PrimaryApp, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp)
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth().background(SurfaceLowest, RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = 1f, color = PrimaryApp.copy(alpha = 0.3f), modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp)
                    Box(modifier = Modifier.size(80.dp).background(SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("LICENSE PLATE NUMBER", color = PrimaryApp, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().background(SurfaceHigh, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = plate,
                            onValueChange = { plate = it.uppercase() },
                            placeholder = { Text("e.g. UK22M1234", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, textColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = PrimaryApp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isSaving = true
                            AppLogger.logEvent("add_vehicle_attempt")
                            coroutineScope.launch {
                                try {
                                    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                                    val token = prefs.getString("jwt_token", "") ?: ""

                                    val response = ApiService.api.addVehicle("Bearer $token", VehicleRequest(plate.trim()))
                                    
                                    if (response.isSuccessful) {
                                        AppLogger.logEvent("add_vehicle_success")
                                        isSaving = false
                                        onSaved() 
                                    } else {
                                        val errorMsg = ApiService.extractErrorMessage(response.errorBody())
                                        AppLogger.logEvent("add_vehicle_failed", mapOf("reason" to errorMsg))
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        isSaving = false
                                    }
                                } catch (e: Exception) {
                                    AppLogger.recordError(e, "Failed to save vehicle")
                                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryApp, disabledBackgroundColor = PrimaryApp.copy(alpha = 0.5f)),
                        enabled = !isSaving && plate.isNotEmpty()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Save Vehicle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}