package com.example.data.network.api

import com.example.data.network.dto.ApiResponse
import com.example.data.network.dto.RouteRequestDto
import com.example.data.network.dto.RouteResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface NavigationApi {
    @POST("navigation/route")
    suspend fun calculateRoute(
        @Body request: RouteRequestDto
    ): ApiResponse<RouteResponseDto>
}
