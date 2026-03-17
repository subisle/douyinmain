package com.example.myapplication.data.remote

import com.example.myapplication.data.model.SoundData
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("auth/check")
    suspend fun checkAuth(): Response<AuthResponse>
    
    @GET("sound-data")
    suspend fun getSoundData(@Query("date") date: String): Response<SoundDataResponse>
    
    @POST("sound-data/import")
    suspend fun importData(@Body data: List<SoundData>): Response<ImportResponse>
    
    @GET("sound-data/sync")
    suspend fun syncData(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<SyncResponse>
}

data class AuthResponse(
    val authorized: Boolean
)

data class SoundDataResponse(
    val success: Boolean,
    val data: List<SoundData>
)

data class ImportResponse(
    val success: Boolean,
    val imported: Int,
    val failed: Int
)

data class SyncResponse(
    val success: Boolean,
    val data: List<SoundData>,
    val count: Int
)
