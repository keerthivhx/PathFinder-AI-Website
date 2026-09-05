package com.example.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "errors") val errors: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class BuildingEntranceDto(
    @Json(name = "entranceId") val entranceId: String,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "type") val type: String? = "MAIN",
    @Json(name = "linkedNodeId") val linkedNodeId: String,
    @Json(name = "isWheelchairAccessible") val isWheelchairAccessible: Boolean = true,
    @Json(name = "description") val description: String? = ""
)

@JsonClass(generateAdapter = true)
data class BuildingDto(
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String? = null,
    @Json(name = "type") val type: String? = "University",
    @Json(name = "description") val description: String? = "",
    @Json(name = "address") val address: String? = "",
    @Json(name = "latitude") val latitude: Double? = 12.9716,
    @Json(name = "longitude") val longitude: Double? = 77.5946,
    @Json(name = "entrances") val entrances: List<BuildingEntranceDto>? = emptyList(),
    @Json(name = "totalFloors") val totalFloors: Int? = 1,
    @Json(name = "activeStatus") val activeStatus: Boolean? = true,
    @Json(name = "graphVersion") val graphVersion: Int? = 1,
    @Json(name = "createdAt") val createdAt: Long? = null,
    @Json(name = "updatedAt") val updatedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class FloorDto(
    @Json(name = "floorId") val floorId: String? = null,
    @Json(name = "buildingId") val buildingId: String? = null,
    @Json(name = "floorNumber") val floorNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "displayLabel") val displayLabel: String? = "G",
    @Json(name = "shortName") val shortName: String? = "G",
    @Json(name = "mapWidth") val mapWidth: Float? = 1000f,
    @Json(name = "mapHeight") val mapHeight: Float? = 1000f,
    @Json(name = "mapScale") val mapScale: Float? = 1.0f,
    @Json(name = "mapAsset") val mapAsset: String? = "blueprint_vector",
    @Json(name = "blueprintUrl") val blueprintUrl: String? = null,
    @Json(name = "elevationMeters") val elevationMeters: Double? = 0.0,
    @Json(name = "active") val active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class CoordinatesDto(
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float
)

@JsonClass(generateAdapter = true)
data class LocationDto(
    @Json(name = "locationId") val locationId: String,
    @Json(name = "nodeId") val nodeId: String,
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "floorNumber") val floorNumber: Int,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "category") val category: String = "GENERAL",
    @Json(name = "description") val description: String? = "",
    @Json(name = "keywords") val keywords: List<String>? = emptyList(),
    @Json(name = "isAccessible") val isAccessible: Boolean? = true,
    @Json(name = "isEmergencyExit") val isEmergencyExit: Boolean? = false,
    @Json(name = "coordinates") val coordinates: CoordinatesDto? = null,
    @Json(name = "active") val active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class NavNodeDto(
    @Json(name = "nodeId") val nodeId: String,
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "floor") val floor: Int,
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "category") val category: String = "GENERAL",
    @Json(name = "description") val description: String? = "",
    @Json(name = "keywords") val keywords: List<String>? = emptyList(),
    @Json(name = "isAccessible") val isAccessible: Boolean? = true,
    @Json(name = "isEmergencyExit") val isEmergencyExit: Boolean? = false,
    @Json(name = "visualSignageHint") val visualSignageHint: String? = "",
    @Json(name = "iconName") val iconName: String? = "place",
    @Json(name = "active") val active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class NavEdgeDto(
    @Json(name = "edgeId") val edgeId: String? = null,
    @Json(name = "buildingId") val buildingId: String? = null,
    @Json(name = "fromId") val fromId: String,
    @Json(name = "toId") val toId: String,
    @Json(name = "distanceMeters") val distanceMeters: Float,
    @Json(name = "edgeType") val edgeType: String? = "CORRIDOR",
    @Json(name = "isWheelchairAccessible") val isWheelchairAccessible: Boolean? = true,
    @Json(name = "bidirectional") val bidirectional: Boolean? = true,
    @Json(name = "restricted") val restricted: Boolean? = false,
    @Json(name = "stairs") val stairs: Boolean? = false,
    @Json(name = "elevator") val elevator: Boolean? = false,
    @Json(name = "ramp") val ramp: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class QrPointDto(
    @Json(name = "calibrationId") val calibrationId: String,
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "floorId") val floorId: Int,
    @Json(name = "nodeId") val nodeId: String,
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "qrPayload") val qrPayload: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphValidationResponseDto(
    @Json(name = "isValid") val isValid: Boolean,
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "buildingName") val buildingName: String? = "",
    @Json(name = "totalNodes") val totalNodes: Int = 0,
    @Json(name = "totalEdges") val totalEdges: Int = 0,
    @Json(name = "totalFloors") val totalFloors: Int = 0,
    @Json(name = "accessibleEdgesCount") val accessibleEdgesCount: Int = 0,
    @Json(name = "elevatorTransitionsCount") val elevatorTransitionsCount: Int = 0,
    @Json(name = "stairTransitionsCount") val stairTransitionsCount: Int = 0,
    @Json(name = "rampTransitionsCount") val rampTransitionsCount: Int = 0,
    @Json(name = "errors") val errors: List<String> = emptyList(),
    @Json(name = "warnings") val warnings: List<String> = emptyList(),
    @Json(name = "isolatedNodes") val isolatedNodes: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RouteRequestDto(
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "startNodeId") val startNodeId: String,
    @Json(name = "targetNodeId") val targetNodeId: String,
    @Json(name = "isWheelchairMode") val isWheelchairMode: Boolean = false,
    @Json(name = "isEmergencyMode") val isEmergencyMode: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RouteInstructionDto(
    @Json(name = "stepNumber") val stepNumber: Int,
    @Json(name = "instruction") val instruction: String,
    @Json(name = "direction") val direction: String = "STRAIGHT",
    @Json(name = "nodeId") val nodeId: String? = null,
    @Json(name = "floor") val floor: Int? = 0,
    @Json(name = "locationName") val locationName: String? = null,
    @Json(name = "locationCode") val locationCode: String? = null,
    @Json(name = "visualSignageHint") val visualSignageHint: String? = null
)

@JsonClass(generateAdapter = true)
data class RouteResponseDto(
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "totalDistanceMeters") val totalDistanceMeters: Float,
    @Json(name = "estimatedTimeMinutes") val estimatedTimeMinutes: Int,
    @Json(name = "floorsTraversed") val floorsTraversed: List<Int> = emptyList(),
    @Json(name = "stepCount") val stepCount: Int = 0,
    @Json(name = "pathNodeIds") val pathNodeIds: List<String> = emptyList(),
    @Json(name = "pathNodes") val pathNodes: List<NavNodeDto>? = null,
    @Json(name = "instructions") val instructions: List<RouteInstructionDto>? = null
)

@JsonClass(generateAdapter = true)
data class HealthDto(
    @Json(name = "status") val status: String,
    @Json(name = "service") val service: String,
    @Json(name = "version") val version: String,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String = "USER"
)

@JsonClass(generateAdapter = true)
data class BeaconDto(
    @Json(name = "beaconId") val beaconId: String,
    @Json(name = "buildingId") val buildingId: String,
    @Json(name = "floorId") val floorId: Int = 0,
    @Json(name = "nodeId") val nodeId: String,
    @Json(name = "x") val x: Float = 500f,
    @Json(name = "y") val y: Float = 500f,
    @Json(name = "major") val major: Int? = 1,
    @Json(name = "minor") val minor: Int? = 1,
    @Json(name = "txPower") val txPower: Int? = -59,
    @Json(name = "activeStatus") val activeStatus: Boolean? = true,
    @Json(name = "metadata") val metadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class CachedBuildingDto(
    @Json(name = "building") val building: BuildingDto,
    @Json(name = "floors") val floors: List<FloorDto> = emptyList(),
    @Json(name = "nodes") val nodes: List<NavNodeDto> = emptyList(),
    @Json(name = "edges") val edges: List<NavEdgeDto> = emptyList(),
    @Json(name = "beacons") val beacons: List<BeaconDto> = emptyList(),
    @Json(name = "qrPoints") val qrPoints: List<QrPointDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CachedBuildingContainerDto(
    @Json(name = "buildings") val buildings: List<CachedBuildingDto> = emptyList(),
    @Json(name = "lastCachedTimestamp") val lastCachedTimestamp: Long = System.currentTimeMillis()
)
