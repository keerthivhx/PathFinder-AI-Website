package com.example.data.model

/**
 * Phase 11: Structured AI Intent model for grounded navigation reasoning.
 */
enum class AINavigationIntent(val label: String) {
    NAVIGATE_TO("Navigate To"),
    SEARCH_LOCATION("Search Location"),
    CURRENT_LOCATION("Current Location"),
    NEXT_INSTRUCTION("Next Instruction"),
    ROUTE_SUMMARY("Route Summary"),
    ACCESSIBILITY("Accessibility"),
    NEAREST_LOCATION("Nearest Location"),
    BUILDING_INFORMATION("Building Information"),
    FLOOR_INFORMATION("Floor Information"),
    EMERGENCY("Emergency"),
    UNKNOWN("General Inquiry")
}

/**
 * Actions that the UI or ViewModel can trigger from AI guidance.
 */
enum class AIAssistantAction {
    START_NAVIGATION,
    VIEW_ROUTE,
    RECALCULATE,
    SHOW_NEAREST,
    CANCEL_ROUTE,
    SCAN_QR,
    NONE
}

/**
 * Phase 11: Structured navigation context model grounded in real-time application data.
 * Sensitive location history is excluded; only minimum current navigation state is provided.
 */
data class AINavigationContext(
    val buildingId: String,
    val buildingName: String,
    val currentFloor: Int,
    val currentNodeId: String? = null,
    val currentNodeName: String? = null,
    val currentNodeCode: String? = null,
    val currentX: Float? = null,
    val currentY: Float? = null,
    val locationConfidence: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "UNCERTAIN"
    val isLocationUncertain: Boolean = false,
    val destinationNodeId: String? = null,
    val destinationName: String? = null,
    val destinationFloor: Int? = null,
    val isDestinationSelected: Boolean = false,
    val isNavigating: Boolean = false,
    val navigationState: NavSessionState = NavSessionState.IDLE,
    val currentRouteStepInstruction: String? = null,
    val distanceToNextActionMeters: Float? = null,
    val remainingDistanceMeters: Float? = null,
    val estimatedMinutes: Int? = null,
    val floorTransfers: List<String> = emptyList(),
    val isWheelchairMode: Boolean = false,
    val isRouteAccessible: Boolean = true,
    val routeStepCount: Int = 0,
    val currentStepIndex: Int = 0
)

/**
 * Grounded response from the AI Navigation Assistant.
 */
data class AIAssistantResponse(
    val intent: AINavigationIntent,
    val textResponse: String,
    val suggestedVoiceResponse: String,
    val action: AIAssistantAction = AIAssistantAction.NONE,
    val resolvedTargetNode: NavNode? = null,
    val multipleMatches: List<NavNode> = emptyList(),
    val isEmergency: Boolean = false,
    val confidence: Float = 0.95f
)
