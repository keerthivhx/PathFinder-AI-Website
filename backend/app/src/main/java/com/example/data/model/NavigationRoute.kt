package com.example.data.model

enum class TurnType {
    START,
    STRAIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    ELEVATOR_UP,
    ELEVATOR_DOWN,
    STAIRS_UP,
    STAIRS_DOWN,
    RAMP_UP,
    RAMP_DOWN,
    ARRIVAL,
    EMERGENCY_EVACUATE
}

data class RouteStep(
    val stepIndex: Int,
    val instruction: String,
    val detail: String,
    val turnType: TurnType,
    val distanceMeters: Float,
    val targetNode: NavNode,
    val floor: Int,
    val icon: String = "straight"
) {
    val floorChange: Int? get() = (targetNode.floor - floor).takeIf { it != 0 }
}

data class NavigationRoute(
    val pathNodes: List<NavNode>,
    val steps: List<RouteStep>,
    val totalDistanceMeters: Float,
    val estimatedTimeSeconds: Int,
    val transferFloors: List<Int> = emptyList(),
    val hasStairs: Boolean = false,
    val hasElevators: Boolean = false,
    val isWheelchairSafe: Boolean = true,
    val isEmergencyRoute: Boolean = false
) {
    val startNode: NavNode get() = pathNodes.first()
    val targetNode: NavNode get() = pathNodes.last()
    val destinationNode: NavNode get() = pathNodes.last()
    val estimatedMinutes: Int get() = ((estimatedTimeSeconds + 59) / 60).coerceAtLeast(1)
}

data class AIIntentResult(
    val targetNodeId: String?,
    val confidence: Float,
    val explanation: String,
    val suggestedVoiceResponse: String
) {
    val reasoning: String get() = explanation
}

data class AILandmarkResult(
    val matchedNodeId: String?,
    val landmarkName: String,
    val confidence: Float,
    val explanation: String
)
