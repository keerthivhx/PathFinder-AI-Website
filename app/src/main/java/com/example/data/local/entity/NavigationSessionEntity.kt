package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.NavigationSessionResult
import com.example.data.model.NavigationSessionStatus

@Entity(
    tableName = "navigation_sessions",
    indices = [
        Index(value = ["buildingId"]),
        Index(value = ["destinationNodeId"]),
        Index(value = ["startTimestamp"]),
        Index(value = ["navigationResult"]),
        Index(value = ["isDemoSession"])
    ]
)
data class NavigationSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val buildingId: String,
    val buildingName: String,
    val destinationNodeId: String,
    val destinationName: String,
    val destinationFloor: Int,
    val startNodeId: String?,
    val startNodeName: String?,
    val startingFloor: Int,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val plannedDistanceMeters: Float = 0f,
    val estimatedDurationSeconds: Int = 0,
    val actualDurationSeconds: Int? = null,
    val routeRecalculationCount: Int = 0,
    val offRouteEventCount: Int = 0,
    val floorTransitionsCount: Int = 0,
    val floorsCrossedSummary: String = "",
    val elevatorUsageCount: Int = 0,
    val stairsUsageCount: Int = 0,
    val isWheelchairMode: Boolean = false,
    val primaryPositioningSource: String = "SENSOR_FUSION",
    val navigationResult: String = NavigationSessionResult.IN_PROGRESS.name,
    val status: String = NavigationSessionStatus.NAVIGATING.name,
    val isSynced: Boolean = false,
    val isDemoSession: Boolean = false
)
