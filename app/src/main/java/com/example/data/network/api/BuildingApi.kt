package com.example.data.network.api

import com.example.data.network.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BuildingApi {
    @GET("buildings")
    suspend fun getBuildings(): ApiResponse<List<BuildingDto>>

    @GET("buildings/{id}")
    suspend fun getBuildingById(@Path("id") id: String): ApiResponse<BuildingDto>

    @GET("buildings/{id}/floors")
    suspend fun getFloors(@Path("id") id: String): ApiResponse<List<FloorDto>>

    @GET("buildings/{id}/locations")
    suspend fun getLocations(
        @Path("id") id: String,
        @Query("category") category: String? = null,
        @Query("floor") floor: Int? = null
    ): ApiResponse<List<LocationDto>>

    @GET("buildings/{id}/nodes")
    suspend fun getNodes(
        @Path("id") id: String,
        @Query("floor") floor: Int? = null
    ): ApiResponse<List<NavNodeDto>>

    @GET("buildings/{id}/edges")
    suspend fun getEdges(@Path("id") id: String): ApiResponse<List<NavEdgeDto>>

    @GET("beacons/building/{id}")
    suspend fun getBeacons(@Path("id") id: String): ApiResponse<List<BeaconDto>>

    @GET("beacons/{id}")
    suspend fun getBeaconById(@Path("id") id: String): ApiResponse<BeaconDto>
}
