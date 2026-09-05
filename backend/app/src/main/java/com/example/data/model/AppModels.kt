package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val message: String,
    val suggestedNodeId: String? = null,
    val suggestedFloor: Int? = null,
    val distanceMeters: Float? = null,
    val estimatedTimeMinutes: Int? = null,
    val multipleMatchNodeIds: List<String> = emptyList(),
    val isEmergency: Boolean = false,
    val intent: String? = null,
    val action: AIAssistantAction = AIAssistantAction.NONE,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserAccount(
    val id: String = "usr_demo_01",
    val name: String = "Alex Rivera",
    val email: String = "alex.rivera@university.edu",
    val role: String = "USER", // "USER" or "ADMIN"
    val token: String = "jwt_sec_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    val favoriteNodeIds: Set<String> = setOf("univ_cslab", "univ_library", "hosp_triage_er"),
    val recentNodeIds: List<String> = listOf("univ_cslab", "univ_cafeteria", "univ_seminar")
)

enum class AppTab(val label: String, val icon: String) {
    NAVIGATION("Navigation", "explore"),
    AI_ASSISTANT("AI Assistant", "psychology"),
    LANDING("Overview", "dashboard"),
    ADMIN("Admin Dashboard", "admin_panel_settings")
}

sealed class NetworkSyncState {
    object Idle : NetworkSyncState()
    object Loading : NetworkSyncState()
    data class Synced(val buildingCount: Int, val nodeCount: Int, val timestamp: Long = System.currentTimeMillis()) : NetworkSyncState()
    data class OfflineFallback(val message: String, val buildingCount: Int) : NetworkSyncState()
    data class Error(val message: String) : NetworkSyncState()
}

data class AnalyticsData(
    val totalBuildings: Int = 4,
    val totalFloors: Int = 11,
    val totalLocations: Int = 48,
    val totalRoutes: Int = 74,
    val dailyNavigationRequests: Int = 1840,
    val avgNavigationTimeMinutes: Float = 3.2f,
    val accessibleRoutePercentage: Int = 28,
    val popularDestinations: List<Pair<String, Int>> = listOf(
        "AI Laboratory (CS-204)" to 412,
        "Central Library" to 345,
        "Campus Cafeteria" to 289,
        "Emergency Triage (ER-101)" to 210,
        "Grand Auditorium" to 198,
        "Duty Free Plaza" to 175
    ),
    val dailyTrafficSeries: List<Pair<String, Int>> = listOf(
        "Mon" to 1240,
        "Tue" to 1580,
        "Wed" to 1840,
        "Thu" to 1720,
        "Fri" to 1950,
        "Sat" to 980,
        "Sun" to 720
    )
)
