package com.example.positioning

import com.example.data.model.*
import kotlinx.coroutines.flow.StateFlow

sealed class NavigationLocationState {
    object Idle : NavigationLocationState()
    data class Outside(val distanceMeters: Float, val userLat: Double, val userLng: Double) : NavigationLocationState()
    data class ApproachingBuilding(val buildingName: String, val distanceMeters: Float) : NavigationLocationState()
    data class ApproachingEntrance(val entrance: BuildingEntrance, val distanceMeters: Float) : NavigationLocationState()
    data class AtEntrance(val entranceName: String, val buildingName: String, val linkedNodeId: String) : NavigationLocationState()
    data class IndoorCalibrationRequired(val entrance: BuildingEntrance) : NavigationLocationState()
    data class IndoorNavigating(
        val position: IndoorPosition,
        val nearestNode: NavNode?,
        val currentFloor: Int,
        val stepIndex: Int,
        val remainingDistanceMeters: Float,
        val remainingTimeMinutes: Int
    ) : NavigationLocationState()
    data class FloorTransition(
        val fromFloor: Int,
        val toFloor: Int,
        val viaNodeName: String = "Transfer",
        val isElevator: Boolean = false,
        val targetFloor: Int = toFloor,
        val transitionType: String = if (isElevator) "Elevator" else "Stairs"
    ) : NavigationLocationState()
    data class AtFloorTransition(
        val fromFloor: Int,
        val toFloor: Int,
        val targetFloor: Int = toFloor,
        val transitionType: String = "Elevator"
    ) : NavigationLocationState()
    data class OffRoute(val distanceOffMeters: Float, val currentPosition: IndoorPosition) : NavigationLocationState()
    data class Rerouting(val reason: String) : NavigationLocationState()
    data class Arrived(val destinationNode: NavNode, val totalDistanceTraversed: Float) : NavigationLocationState()
    data class Error(val message: String) : NavigationLocationState()
}

enum class PositioningMode(val label: String) {
    HYBRID_AUTO("Hybrid Sensor Fusion"),
    HYBRID_FUSION("Hybrid Sensor Fusion"),
    BLE_ONLY("BLE Beacons Only"),
    REAL_BLE_ONLY("Real BLE Beacons Only"),
    PDR_ONLY("PDR Step & Compass"),
    QR_PDR_ONLY("QR + PDR Sensor"),
    GPS_ONLY("GPS Outdoor Only"),
    GPS_OUTDOOR_ONLY("GPS Outdoor Only"),
    QR_ONLY("QR Landmarks Only"),
    MANUAL("Manual Tap Navigation"),
    SIMULATION_DEMO("Demo / Simulation Mode")
}

interface LocationProvider {
    val locationState: StateFlow<NavigationLocationState>
    val currentIndoorPosition: StateFlow<IndoorPosition?>
    val deviceHeadingDegrees: StateFlow<Float>
    val positioningMode: StateFlow<PositioningMode>

    fun startTracking()
    fun stopTracking()
    fun setPositioningMode(mode: PositioningMode)
    fun calibrateWithQr(qrText: String, building: Building): Result<NavNode>
    fun calibrateManual(node: NavNode)
    fun updateActiveRoute(route: NavigationRoute?)
}
