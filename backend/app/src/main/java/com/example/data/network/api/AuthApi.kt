package com.example.data.network.api

import com.example.data.network.dto.ApiResponse
import com.example.data.network.dto.HealthDto
import retrofit2.http.GET

interface AuthApi {
    @GET("health")
    suspend fun checkHealth(): ApiResponse<HealthDto>
}
