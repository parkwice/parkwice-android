package com.mintech.parkwiseapp.services

import com.mintech.parkwiseapp.core.ApiConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Models
data class Vehicle(val _id: String, val licensePlate: String)
data class CallInitiateRequest(val licensePlate: String)
data class CallInitiateResponse(val targetUserId: String?)
data class VehicleRequest(val licensePlate: String)
data class TokenSyncRequest(val fcmToken: String)

data class GoogleLoginRequest(
    val email: String,
    val name: String,
    val googleId: String,
    val fcmToken: String,
    val voipToken: String,
    val photoUrl: String
)

data class User(val _id: String, val email: String)
data class GoogleLoginResponse(val token: String, val user: User)

interface ParkwiseApi {
    @GET("vehicles")
    suspend fun getVehicles(@Header("Authorization") token: String): List<Vehicle>

    @POST("vehicles")
    suspend fun addVehicle(@Header("Authorization") token: String, @Body req: VehicleRequest)

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Header("Authorization") token: String, @Path("id") id: String)

    @POST("call/initiate")
    suspend fun initiateCall(@Header("Authorization") token: String, @Body req: CallInitiateRequest): CallInitiateResponse

    @POST("auth/fcm")
    suspend fun syncDeviceToken(@Header("Authorization") token: String, @Body req: TokenSyncRequest)

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body req: GoogleLoginRequest): GoogleLoginResponse
}

object ApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ParkwiseApi = retrofit.create(ParkwiseApi::class.java)
}