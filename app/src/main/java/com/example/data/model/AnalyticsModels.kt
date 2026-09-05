package com.example.data.model

enum class NavigationSessionResult(val label: String) {
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    ABANDONED("Abandoned")
}

enum class NavigationSessionStatus(val label: String) {
    CREATED("Created"),
    STARTED("Started"),
    NAVIGATING("Navigating"),
    PAUSED("Paused"),
    RESUMED("Resumed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    FAILED("Failed")
}

enum class NavigationEventType(val label: String) {
    SESSION_STARTED("Session Started"),
    NAVIGATING("Navigating"),
    PAUSED("Navigation Paused"),
    RESUMED("Navigation Resumed"),
    ROUTE_RECALCULATED("Route Recalculated"),
    OFF_ROUTE_DETECTED("Off Route Detected"),
    FLOOR_TRANSITION("Floor Transition"),
    SESSION_COMPLETED("Arrived / Completed"),
    SESSION_CANCELLED("Cancelled by User"),
    SESSION_FAILED("Navigation Failed")
}

enum class AnalyticsDateRange(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

enum class ResultFilter(val label: String) {
    ALL("All Outcomes"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    FAILED("Failed")
}

enum class AccessibilityFilter(val label: String) {
    ALL("All Modes"),
    ACCESSIBLE_ONLY("Accessible Only"),
    STANDARD_ONLY("Standard")
}

data class AnalyticsOverviewMetrics(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val cancelledSessions: Int = 0,
    val failedSessions: Int = 0,
    val successRatePercent: Float = 0f,
    val averageDurationSeconds: Float = 0f,
    val averageDistanceMeters: Float = 0f,
    val averageRerouteCount: Float = 0f,
    val averageOffRouteCount: Float = 0f,
    val averageFloorTransitions: Float = 0f,
    val accessibleSessionsCount: Int = 0,
    val accessibleSuccessRatePercent: Float = 0f
)

data class TopDestinationMetric(
    val destinationNodeId: String,
    val destinationName: String,
    val floor: Int,
    val sessionCount: Int,
    val completedCount: Int,
    val rerouteAverage: Float
)

data class SearchMetric(
    val query: String,
    val searchCount: Int,
    val successfulCount: Int,
    val isZeroResult: Boolean
)

data class PositioningSourceMetric(
    val source: String,
    val count: Int,
    val percentage: Float
)

data class ProblematicLocationAlert(
    val locationId: String,
    val locationName: String,
    val floor: Int,
    val issueType: String,
    val description: String,
    val severity: String, // "WARNING", "CRITICAL", "INFO"
    val metricValue: Float
)

data class FloorActivityMetric(
    val floor: Int,
    val sessionCount: Int,
    val topLocations: List<String>,
    val transitionCount: Int
)

data class DateTrendMetric(
    val dateLabel: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val cancelledSessions: Int
)
