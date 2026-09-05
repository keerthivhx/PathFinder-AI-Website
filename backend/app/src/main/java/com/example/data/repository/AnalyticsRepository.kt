package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.NavigationSessionDao
import com.example.data.local.dao.SearchAnalyticsDao
import com.example.data.local.entity.NavigationEventEntity
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.local.entity.SearchAnalyticsEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val sessionDao: NavigationSessionDao = db.navigationSessionDao()
    private val searchDao: SearchAnalyticsDao = db.searchAnalyticsDao()

    suspend fun recordSearch(
        buildingId: String,
        query: String,
        resultCount: Int,
        selectedNodeId: String? = null,
        selectedCategory: String? = null,
        isDemo: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val entity = SearchAnalyticsEntity(
            searchId = UUID.randomUUID().toString(),
            buildingId = buildingId,
            query = query.trim(),
            resultCount = resultCount,
            isSuccessful = resultCount > 0,
            selectedNodeId = selectedNodeId,
            selectedCategory = selectedCategory,
            timestamp = System.currentTimeMillis(),
            isDemo = isDemo
        )
        searchDao.insertSearch(entity)
    }

    private fun getTimestampForDateRange(range: AnalyticsDateRange): Long {
        val cal = Calendar.getInstance()
        return when (range) {
            AnalyticsDateRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            AnalyticsDateRange.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            AnalyticsDateRange.LAST_30_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            AnalyticsDateRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            AnalyticsDateRange.ALL_TIME -> 0L
        }
    }

    suspend fun getFilteredSessions(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        resultFilter: ResultFilter = ResultFilter.ALL,
        accessibilityFilter: AccessibilityFilter = AccessibilityFilter.ALL,
        includeDemo: Boolean = false
    ): List<NavigationSessionEntity> = withContext(Dispatchers.IO) {
        val minTime = getTimestampForDateRange(dateRange)
        val all = if (buildingId.isNullOrBlank()) {
            sessionDao.getAllSessions()
        } else {
            sessionDao.getSessionsForBuilding(buildingId)
        }

        all.filter { session ->
            (includeDemo || !session.isDemoSession) &&
            session.startTimestamp >= minTime &&
            (resultFilter == ResultFilter.ALL || session.navigationResult.equals(resultFilter.name, ignoreCase = true)) &&
            (accessibilityFilter == AccessibilityFilter.ALL ||
                    (accessibilityFilter == AccessibilityFilter.ACCESSIBLE_ONLY && session.isWheelchairMode) ||
                    (accessibilityFilter == AccessibilityFilter.STANDARD_ONLY && !session.isWheelchairMode))
        }
    }

    suspend fun calculateOverviewMetrics(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        accessibilityFilter: AccessibilityFilter = AccessibilityFilter.ALL,
        includeDemo: Boolean = false
    ): AnalyticsOverviewMetrics = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            resultFilter = ResultFilter.ALL,
            accessibilityFilter = accessibilityFilter,
            includeDemo = includeDemo
        )

        if (sessions.isEmpty()) {
            return@withContext AnalyticsOverviewMetrics()
        }

        val total = sessions.size
        val completed = sessions.count { it.navigationResult == NavigationSessionResult.COMPLETED.name }
        val cancelled = sessions.count { it.navigationResult == NavigationSessionResult.CANCELLED.name }
        val failed = sessions.count { it.navigationResult == NavigationSessionResult.FAILED.name || it.navigationResult == NavigationSessionResult.ABANDONED.name }

        val successRate = if (total > 0) (completed.toFloat() / total * 100f) else 0f

        val completedSessions = sessions.filter { it.navigationResult == NavigationSessionResult.COMPLETED.name }
        val avgDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.mapNotNull { it.actualDurationSeconds }.average().toFloat()
        } else 0f

        val avgDistance = if (sessions.isNotEmpty()) {
            sessions.map { it.plannedDistanceMeters }.average().toFloat()
        } else 0f

        val avgReroutes = if (sessions.isNotEmpty()) {
            sessions.map { it.routeRecalculationCount }.average().toFloat()
        } else 0f

        val avgOffRoutes = if (sessions.isNotEmpty()) {
            sessions.map { it.offRouteEventCount }.average().toFloat()
        } else 0f

        val avgTransitions = if (sessions.isNotEmpty()) {
            sessions.map { it.floorTransitionsCount }.average().toFloat()
        } else 0f

        val accessibleSessions = sessions.filter { it.isWheelchairMode }
        val accessibleCompleted = accessibleSessions.count { it.navigationResult == NavigationSessionResult.COMPLETED.name }
        val accessibleSuccessRate = if (accessibleSessions.isNotEmpty()) {
            (accessibleCompleted.toFloat() / accessibleSessions.size * 100f)
        } else 0f

        AnalyticsOverviewMetrics(
            totalSessions = total,
            completedSessions = completed,
            cancelledSessions = cancelled,
            failedSessions = failed,
            successRatePercent = (successRate * 10).toInt() / 10f,
            averageDurationSeconds = avgDuration,
            averageDistanceMeters = avgDistance,
            averageRerouteCount = (avgReroutes * 10).toInt() / 10f,
            averageOffRouteCount = (avgOffRoutes * 10).toInt() / 10f,
            averageFloorTransitions = (avgTransitions * 10).toInt() / 10f,
            accessibleSessionsCount = accessibleSessions.size,
            accessibleSuccessRatePercent = (accessibleSuccessRate * 10).toInt() / 10f
        )
    }

    suspend fun getTopDestinations(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false,
        limit: Int = 6
    ): List<TopDestinationMetric> = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            resultFilter = ResultFilter.ALL,
            accessibilityFilter = AccessibilityFilter.ALL,
            includeDemo = includeDemo
        )

        sessions.groupBy { it.destinationNodeId }
            .map { (destId, group) ->
                val sample = group.first()
                val completed = group.count { it.navigationResult == NavigationSessionResult.COMPLETED.name }
                val reroutes = group.map { it.routeRecalculationCount }.average().toFloat()
                TopDestinationMetric(
                    destinationNodeId = destId,
                    destinationName = sample.destinationName,
                    floor = sample.destinationFloor,
                    sessionCount = group.size,
                    completedCount = completed,
                    rerouteAverage = (reroutes * 10).toInt() / 10f
                )
            }
            .sortedByDescending { it.sessionCount }
            .take(limit)
    }

    suspend fun getSearchDemand(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false,
        limit: Int = 10
    ): List<SearchMetric> = withContext(Dispatchers.IO) {
        val minTime = getTimestampForDateRange(dateRange)
        val raw = if (buildingId.isNullOrBlank()) {
            searchDao.getAllSearches(minTime)
        } else {
            searchDao.getSearchesForBuilding(buildingId, minTime)
        }.filter { includeDemo || !it.isDemo }

        raw.groupBy { it.query.lowercase() }
            .map { (query, group) ->
                val successful = group.count { it.isSuccessful }
                val isZero = group.all { !it.isSuccessful }
                SearchMetric(
                    query = group.first().query,
                    searchCount = group.size,
                    successfulCount = successful,
                    isZeroResult = isZero
                )
            }
            .sortedByDescending { it.searchCount }
            .take(limit)
    }

    suspend fun getZeroResultSearches(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false
    ): List<SearchMetric> = withContext(Dispatchers.IO) {
        getSearchDemand(buildingId, dateRange, includeDemo).filter { it.isZeroResult }
    }

    suspend fun getPositioningDistribution(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false
    ): List<PositioningSourceMetric> = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            resultFilter = ResultFilter.ALL,
            accessibilityFilter = AccessibilityFilter.ALL,
            includeDemo = includeDemo
        )

        if (sessions.isEmpty()) return@withContext emptyList()

        val total = sessions.size
        sessions.groupBy { it.primaryPositioningSource }
            .map { (source, group) ->
                val pct = (group.size.toFloat() / total * 100f)
                PositioningSourceMetric(
                    source = source,
                    count = group.size,
                    percentage = (pct * 10).toInt() / 10f
                )
            }
            .sortedByDescending { it.count }
    }

    suspend fun getFloorActivity(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false
    ): List<FloorActivityMetric> = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            includeDemo = includeDemo
        )

        if (sessions.isEmpty()) return@withContext emptyList()

        sessions.groupBy { it.destinationFloor }
            .map { (floor, group) ->
                val topLocs = group.groupBy { it.destinationName }
                    .entries.sortedByDescending { it.value.size }
                    .take(3)
                    .map { it.key }
                val transitions = group.sumOf { it.floorTransitionsCount }
                FloorActivityMetric(
                    floor = floor,
                    sessionCount = group.size,
                    topLocations = topLocs,
                    transitionCount = transitions
                )
            }
            .sortedBy { it.floor }
    }

    suspend fun getDateTrends(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.LAST_7_DAYS,
        includeDemo: Boolean = false
    ): List<DateTrendMetric> = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            includeDemo = includeDemo
        )

        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        sessions.groupBy { sdf.format(Date(it.startTimestamp)) }
            .map { (dateLabel, group) ->
                DateTrendMetric(
                    dateLabel = dateLabel,
                    totalSessions = group.size,
                    completedSessions = group.count { it.navigationResult == NavigationSessionResult.COMPLETED.name },
                    cancelledSessions = group.count { it.navigationResult == NavigationSessionResult.CANCELLED.name }
                )
            }
    }

    suspend fun getProblematicLocations(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false
    ): List<ProblematicLocationAlert> = withContext(Dispatchers.IO) {
        val sessions = getFilteredSessions(
            buildingId = buildingId,
            dateRange = dateRange,
            includeDemo = includeDemo
        )

        val alerts = mutableListOf<ProblematicLocationAlert>()

        // Analyze per destination
        sessions.groupBy { it.destinationNodeId }.forEach { (destId, group) ->
            if (group.size >= 2) {
                val sample = group.first()
                val avgReroutes = group.map { it.routeRecalculationCount }.average().toFloat()
                val avgOffRoutes = group.map { it.offRouteEventCount }.average().toFloat()
                val completed = group.count { it.navigationResult == NavigationSessionResult.COMPLETED.name }
                val completionRate = completed.toFloat() / group.size

                if (avgReroutes >= 1.5f) {
                    alerts.add(
                        ProblematicLocationAlert(
                            locationId = destId,
                            locationName = sample.destinationName,
                            floor = sample.destinationFloor,
                            issueType = "HIGH_REROUTING",
                            description = "${sample.destinationName} has an unusually high rerouting frequency (avg ${(avgReroutes * 10).toInt() / 10f} reroutes/session). Possible ambiguous corridor or doorway placement.",
                            severity = if (avgReroutes >= 2.5f) "CRITICAL" else "WARNING",
                            metricValue = avgReroutes
                        )
                    )
                }

                if (avgOffRoutes >= 2.0f) {
                    alerts.add(
                        ProblematicLocationAlert(
                            locationId = destId,
                            locationName = sample.destinationName,
                            floor = sample.destinationFloor,
                            issueType = "HIGH_OFF_ROUTE",
                            description = "Frequent off-route divergence detected near ${sample.destinationName} (avg ${(avgOffRoutes * 10).toInt() / 10f} events/session). Check node snapping or BLE beacon coverage.",
                            severity = "WARNING",
                            metricValue = avgOffRoutes
                        )
                    )
                }

                if (completionRate < 0.5f) {
                    alerts.add(
                        ProblematicLocationAlert(
                            locationId = destId,
                            locationName = sample.destinationName,
                            floor = sample.destinationFloor,
                            issueType = "LOW_COMPLETION",
                            description = "Only ${(completionRate * 100).toInt()}% of navigation journeys to ${sample.destinationName} are completed. Users frequently cancel before arriving.",
                            severity = "WARNING",
                            metricValue = completionRate
                        )
                    )
                }
            }
        }

        alerts
    }

    suspend fun getAdminAlerts(
        buildingId: String?,
        dateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
        includeDemo: Boolean = false
    ): List<ProblematicLocationAlert> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ProblematicLocationAlert>()
        result.addAll(getProblematicLocations(buildingId, dateRange, includeDemo))

        // Check unmapped search queries (Section 12: No-result analytics)
        val zeroSearches = getZeroResultSearches(buildingId, dateRange, includeDemo)
        zeroSearches.filter { it.searchCount >= 2 }.forEach { zero ->
            result.add(
                ProblematicLocationAlert(
                    locationId = "search_${zero.query.hashCode()}",
                    locationName = zero.query,
                    floor = 0,
                    issueType = "UNMAPPED_SEARCH",
                    description = "Users searched for \"${zero.query}\" ${zero.searchCount} times with 0 results found. Location may be missing from the building map.",
                    severity = "INFO",
                    metricValue = zero.searchCount.toFloat()
                )
            )
        }

        result
    }

    suspend fun clearDemoData() = withContext(Dispatchers.IO) {
        sessionDao.deleteDemoSessions()
        sessionDao.deleteDemoEvents()
        searchDao.deleteDemoSearches()
    }
}
