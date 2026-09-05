package com.example.data.model

enum class NodeCategory {
    ENTRANCE,
    RECEPTION,
    TRIAGE,
    EMERGENCY,
    DOCTOR_OFFICE,
    RADIOLOGY,
    CARDIOLOGY,
    SURGERY,
    PHARMACY,
    LABORATORY,
    CLASSROOM,
    AUDITORIUM,
    ACCOUNTS,
    FACULTY,
    LIBRARY,
    AIRPORT_GATE,
    SECURITY_CHECK,
    BAGGAGE_CLAIM,
    CHECK_IN,
    DUTY_FREE,
    FOOD_COURT,
    STORE,
    RESTROOM,
    ELEVATOR,
    STAIRS,
    ESCALATOR,
    EMERGENCY_EXIT,
    KIOSK,
    GENERAL;

    val displayName: String
        get() = name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
}

data class NavNode(
    val id: String,
    val buildingId: String,
    val floor: Int, // 0 = Ground/Level 1, 1 = Level 2, etc.
    val x: Float, // Normalized 0f..1000f coordinates for vector scaling
    val y: Float,
    val name: String,
    val code: String, // e.g. "ER-101", "CS-204", "GATE-B4"
    val category: NodeCategory,
    val description: String,
    val keywords: List<String> = emptyList(),
    val isAccessible: Boolean = true,
    val isEmergencyExit: Boolean = false,
    val visualSignageHint: String = "",
    val iconName: String = "place",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class EdgeType {
    CORRIDOR,
    ELEVATOR,
    STAIRS,
    ESCALATOR,
    RAMP,
    EMERGENCY_EXIT
}

data class NavEdge(
    val fromId: String,
    val toId: String,
    val distanceMeters: Float,
    val edgeType: EdgeType = EdgeType.CORRIDOR,
    val isWheelchairAccessible: Boolean = true,
    val bidirectional: Boolean = true,
    val restricted: Boolean = false,
    val stairs: Boolean = (edgeType == EdgeType.STAIRS),
    val elevator: Boolean = (edgeType == EdgeType.ELEVATOR),
    val ramp: Boolean = (edgeType == EdgeType.RAMP),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FloorInfo(
    val floorNumber: Int,
    val name: String, // "Ground Floor (Level 1)", "Floor 2 (Specialties)", etc.
    val shortName: String, // "G", "L1", "L2", "L3"
    val buildingId: String = "",
    val mapWidth: Float = 1000f,
    val mapHeight: Float = 1000f,
    val mapScale: Float = 1.0f,
    val mapAsset: String = "blueprint_vector",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val shortLabel: String get() = shortName
    val displayLabel: String get() = shortName
}

data class QrCalibrationPoint(
    val calibrationId: String,
    val buildingId: String,
    val floorId: Int,
    val nodeId: String,
    val x: Float,
    val y: Float,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val qrPayload: String
        get() = "PATHFINDER_CALIBRATION\n$buildingId\n$floorId\n$nodeId"
}

data class GraphValidationReport(
    val isValid: Boolean,
    val buildingId: String,
    val buildingName: String,
    val totalNodes: Int,
    val totalEdges: Int,
    val totalFloors: Int,
    val accessibleEdgesCount: Int,
    val elevatorTransitionsCount: Int,
    val stairTransitionsCount: Int,
    val rampTransitionsCount: Int,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val isolatedNodes: List<String> = emptyList(),
    val validatedAt: Long = System.currentTimeMillis()
)

sealed class DeleteResult {
    object Success : DeleteResult()
    data class HasDependencies(
        val message: String,
        val dependencyCount: Int,
        val dependencyType: String
    ) : DeleteResult()
    data class Error(val error: String) : DeleteResult()
}

data class LandmarkSample(
    val id: String,
    val nodeId: String,
    val title: String,
    val drawableRes: Int?,
    val expectedText: String,
    val description: String
)

enum class EntranceType {
    MAIN,
    ACCESSIBLE,
    GATE,
    EMERGENCY,
    NORTH,
    SOUTH,
    EAST,
    WEST;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Entrance"
}

data class BuildingEntrance(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: EntranceType = EntranceType.MAIN,
    val linkedNodeId: String, // Corresponding NavNode.id inside indoor graph (e.g. "univ_gate")
    val isWheelchairAccessible: Boolean = true,
    val description: String = ""
)

data class Building(
    val id: String,
    val name: String,
    val type: String, // "Hospital", "University", "Airport", "Mall", "Custom"
    val description: String,
    val icon: String = "apartment",
    val latitude: Double = 12.9716, // Geographic Latitude
    val longitude: Double = 77.5946, // Geographic Longitude
    val address: String = "Engineering Campus, Tech Park Drive",
    val entrances: List<BuildingEntrance> = emptyList(),
    val floors: List<FloorInfo>,
    val nodes: List<NavNode>,
    val edges: List<NavEdge>,
    val presetLandmarks: List<LandmarkSample> = emptyList(),
    val code: String = id,
    val activeStatus: Boolean = true,
    val graphVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "admin",
    val updatedBy: String = "admin"
)
