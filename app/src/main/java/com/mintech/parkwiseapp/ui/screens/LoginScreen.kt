package com.mintech.parkwiseapp.ui.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import com.mintech.parkwiseapp.R
import com.mintech.parkwiseapp.services.ApiService
import com.mintech.parkwiseapp.services.GoogleLoginRequest
import com.mintech.parkwiseapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mintech.parkwiseapp.services.AppLogger

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppLogger.logEvent("screen_view", mapOf("screen_name" to "LoginScreen"))
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .requestProfile()
        .build()

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)

            coroutineScope.launch {
                try {
                    val fcmToken = FirebaseMessaging.getInstance().token.await()

                    val payload = GoogleLoginRequest(
                        email = account.email ?: "",
                        name = account.displayName ?: "Parkwise User",
                        googleId = account.id ?: "",
                        fcmToken = fcmToken,
                        voipToken = "", 
                        photoUrl = account.photoUrl?.toString() ?: ""
                    )

                    val response = ApiService.api.loginWithGoogle(payload)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("jwt_token", body.token)
                                .putString("user_id", body.user._id)
                                .putString("user_email", body.user.email)
                                .apply()

                            AppLogger.logEvent("login_success", mapOf("method" to "google"))
                            AppLogger.setUserId(body.user._id)

                            isLoading = false
                            onLoginSuccess() 
                        }
                    } else {
                        AppLogger.logEvent("login_failed", mapOf("reason" to "backend_rejected"))
                        Log.e("AuthError", "Backend rejected Google Auth payload: ${response.errorBody()?.string()}")
                        isLoading = false
                    }

                } catch (e: Exception) {
                    AppLogger.logEvent("login_failed", mapOf("reason" to "network_error"))
                    Log.e("AuthError", "Failed to login with backend", e)
                    isLoading = false
                }
            }
        } catch (e: ApiException) {
            AppLogger.logEvent("login_failed", mapOf("reason" to "google_sign_in_cancelled_or_failed"))
            Log.e("AuthError", "Google Sign-In failed", e)
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 200.dp, y = (-100).dp)
                .background(Color(0xFF234720).copy(alpha = 0.5f), CircleShape)
                .blur(80.dp)
        )

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
            Text("Parkwise", color = PrimaryApp, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 48.dp, start = 24.dp))
            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Row(modifier = Modifier.background(SurfaceHigh, RoundedCornerShape(20.dp)).border(1.dp, OnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = PrimaryApp, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURE ACCESS", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }

                Text("Hassle Free\nCommunication", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 48.sp)
                Text("Sign in to manage your vehicle emergency contacts and secure your roadside response.", color = OnSurfaceVariant, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.background(SurfaceLowest, RoundedCornerShape(28.dp)).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isLoading) {
                                AppLogger.logEvent("login_button_clicked", mapOf("method" to "google"))
                                isLoading = true
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Sign in with Google", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().background(SurfaceLow, RoundedCornerShape(12.dp)).border(1.dp, OnSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFF8B9BA5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Data Privacy", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Your vehicle data and contact list are encrypted.", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}