package com.example.data.network.api

import com.example.data.network.dto.ApiResponse
import com.example.data.network.dto.LocationDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApi {
    @GET("locations/search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("buildingId") buildingId: String? = null,
        @Query("category") category: String? = null
    ): ApiResponse<List<LocationDto>>
}
