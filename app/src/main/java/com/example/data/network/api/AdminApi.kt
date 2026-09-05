package com.example.data.network.api

import com.example.data.network.dto.*
import retrofit2.http.*

interface AdminApi {

    // Buildings
    @GET("admin/buildings")
    suspend fun getBuildings(): ApiResponse<List<BuildingDto>>

    @GET("admin/buildings/{id}")
    suspend fun getBuildingById(@Path("id") id: String): ApiResponse<BuildingDto>

    @POST("admin/buildings")
    suspend fun createBuilding(@Body building: BuildingDto): ApiResponse<BuildingDto>

    @PUT("admin/buildings/{id}")
    suspend fun updateBuilding(
        @Path("id") id: String,
        @Body building: BuildingDto
    ): ApiResponse<BuildingDto>

    @DELETE("admin/buildings/{id}")
    suspend fun deleteBuilding(@Path("id") id: String): ApiResponse<Map<String, Any>>

    // Floors
    @GET("admin/buildings/{id}/floors")
    suspend fun getFloors(@Path("id") buildingId: String): ApiResponse<List<FloorDto>>

    @POST("admin/buildings/{id}/floors")
    suspend fun createFloor(
        @Path("id") buildingId: String,
        @Body floor: FloorDto
    ): ApiResponse<FloorDto>

    @PUT("admin/floors/{id}")
    suspend fun updateFloor(
        @Path("id") floorId: String,
        @Body floor: FloorDto
    ): ApiResponse<FloorDto>

    @DELETE("admin/floors/{id}")
    suspend fun deleteFloor(@Path("id") floorId: String): ApiResponse<Map<String, Any>>

    // Locations
    @POST("admin/locations")
    suspend fun createLocation(@Body location: LocationDto): ApiResponse<LocationDto>

    @PUT("admin/locations/{id}")
    suspend fun updateLocation(
        @Path("id") locationId: String,
        @Body location: LocationDto
    ): ApiResponse<LocationDto>

    @DELETE("admin/locations/{id}")
    suspend fun deleteLocation(@Path("id") locationId: String): ApiResponse<Map<String, Any>>

    // Nodes
    @POST("admin/nodes")
    suspend fun createNode(@Body node: NavNodeDto): ApiResponse<NavNodeDto>

    @PUT("admin/nodes/{id}")
    suspend fun updateNode(
        @Path("id") nodeId: String,
        @Body node: NavNodeDto
    ): ApiResponse<NavNodeDto>

    @DELETE("admin/nodes/{id}")
    suspend fun deleteNode(@Path("id") nodeId: String): ApiResponse<Map<String, Any>>

    // Edges
    @POST("admin/edges")
    suspend fun createEdge(@Body edge: NavEdgeDto): ApiResponse<NavEdgeDto>

    @PUT("admin/edges/{id}")
    suspend fun updateEdge(
        @Path("id") edgeId: String,
        @Body edge: NavEdgeDto
    ): ApiResponse<NavEdgeDto>

    @DELETE("admin/edges/{id}")
    suspend fun deleteEdge(@Path("id") edgeId: String): ApiResponse<Map<String, Any>>

    // Beacons
    @GET("admin/beacons/building/{id}")
    suspend fun getBeacons(@Path("id") buildingId: String): ApiResponse<List<BeaconDto>>

    @POST("admin/beacons")
    suspend fun createBeacon(@Body beacon: BeaconDto): ApiResponse<BeaconDto>

    @PUT("admin/beacons/{id}")
    suspend fun updateBeacon(
        @Path("id") beaconId: String,
        @Body beacon: BeaconDto
    ): ApiResponse<BeaconDto>

    @DELETE("admin/beacons/{id}")
    suspend fun deleteBeacon(@Path("id") beaconId: String): ApiResponse<Map<String, Any>>

    // QR Calibration Points
    @GET("admin/qr-points/building/{id}")
    suspend fun getQrPoints(@Path("id") buildingId: String): ApiResponse<List<QrPointDto>>

    @POST("admin/qr-points")
    suspend fun createQrPoint(@Body qrPoint: QrPointDto): ApiResponse<QrPointDto>

    @PUT("admin/qr-points/{id}")
    suspend fun updateQrPoint(
        @Path("id") qrId: String,
        @Body qrPoint: QrPointDto
    ): ApiResponse<QrPointDto>

    @DELETE("admin/qr-points/{id}")
    suspend fun deleteQrPoint(@Path("id") qrId: String): ApiResponse<Map<String, Any>>

    // Graph Validation
    @POST("admin/buildings/{id}/validate-graph")
    suspend fun validateGraph(@Path("id") buildingId: String): ApiResponse<GraphValidationResponseDto>
}
