package com.mintech.parkwiseapp.services

import com.mintech.parkwiseapp.core.ApiConstants
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import org.json.JSONObject

// Models
data class Vehicle(val _id: String, val licensePlate: String)
data class VehicleRequest(val licensePlate: String)
data class CallInitiateRequest(val licensePlate: String)
data class CallInitiateResponse(val targetUserId: String?, val error: String?)

data class CallRecord(
    val _id: String,
    val licensePlate: String?,
    val callerId: String?,
    val receiverId: String?,
    val otherUserId: String?,
    val createdAt: String?
)

data class TokenSyncRequest(val fcmToken: String, val voipToken: String = "")
data class GoogleLoginRequest(val email: String, val name: String, val googleId: String, val fcmToken: String, val voipToken: String, val photoUrl: String)
data class User(val _id: String, val email: String)
data class GoogleLoginResponse(val token: String, val user: User)

interface ParkwiseApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body req: GoogleLoginRequest): Response<GoogleLoginResponse>

    @POST("auth/fcm")
    suspend fun syncDeviceToken(@Header("Authorization") token: String, @Body req: TokenSyncRequest): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @GET("vehicles")
    suspend fun getVehicles(@Header("Authorization") token: String): Response<List<Vehicle>>

    @POST("vehicles")
    suspend fun addVehicle(@Header("Authorization") token: String, @Body req: VehicleRequest): Response<Vehicle>

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Header("Authorization") token: String, @Path("id") id: String): Response<Unit>

    @POST("call/initiate")
    suspend fun initiateCall(@Header("Authorization") token: String, @Body req: CallInitiateRequest): Response<CallInitiateResponse>

    @GET("call/history/list")
    suspend fun getCallHistoryList(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<CallRecord>>

    @POST("users/block")
    suspend fun blockUser(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<Unit>

    @POST("users/unblock")
    suspend fun unblockUser(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<Unit>

    @GET("users/blocked")
    suspend fun getBlockedUsers(@Header("Authorization") token: String): Response<List<String>>

    @POST("users/report")
    suspend fun reportUser(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<Unit>

    @DELETE("users/account")
    suspend fun deleteAccount(@Header("Authorization") token: String): Response<Unit>
}

object ApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ParkwiseApi = retrofit.create(ParkwiseApi::class.java)

    fun extractErrorMessage(errorBody: okhttp3.ResponseBody?): String {
        return try {
            val json = JSONObject(errorBody?.string() ?: "")
            json.getString("error")
        } catch (e: Exception) {
            "An unknown error occurred."
        }
    }
}