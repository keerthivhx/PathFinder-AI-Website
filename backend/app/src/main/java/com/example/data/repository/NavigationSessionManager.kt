package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.NavigationSessionDao
import com.example.data.local.entity.NavigationEventEntity
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NavigationSessionManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val sessionDao: NavigationSessionDao = AppDatabase.getDatabase(context).navigationSessionDao()

    private val _activeSession = MutableStateFlow<NavigationSessionEntity?>(null)
    val activeSession: StateFlow<NavigationSessionEntity?> = _activeSession.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            // Clean up any unclosed sessions from previous unexpected app kills
            sessionDao.markOrphanedSessionsAbandoned(System.currentTimeMillis())
        }
    }

    fun startSession(
        building: Building,
        startNode: NavNode?,
        destinationNode: NavNode,
        route: NavigationRoute?,
        isWheelchair: Boolean,
        positioningProvider: PositionSource,
        isDemo: Boolean = false
    ): NavigationSessionEntity {
        // If an active session already exists, close it before starting a new one
        _activeSession.value?.let { existing ->
            cancelSession()
        }

        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val session = NavigationSessionEntity(
            sessionId = sessionId,
            buildingId = building.id,
            buildingName = building.name,
            destinationNodeId = destinationNode.id,
            destinationName = destinationNode.name,
            destinationFloor = destinationNode.floor,
            startNodeId = startNode?.id,
            startNodeName = startNode?.name,
            startingFloor = startNode?.floor ?: building.floors.firstOrNull()?.floorNumber ?: 0,
            startTimestamp = now,
            plannedDistanceMeters = route?.totalDistanceMeters ?: 0f,
            estimatedDurationSeconds = route?.estimatedTimeSeconds ?: 0,
            routeRecalculationCount = 0,
            offRouteEventCount = 0,
            floorTransitionsCount = 0,
            floorsCrossedSummary = "",
            elevatorUsageCount = 0,
            stairsUsageCount = 0,
            isWheelchairMode = isWheelchair,
            primaryPositioningSource = positioningProvider.name,
            navigationResult = NavigationSessionResult.IN_PROGRESS.name,
            status = NavigationSessionStatus.NAVIGATING.name,
            isSynced = false,
            isDemoSession = isDemo
        )

        _activeSession.value = session

        scope.launch(Dispatchers.IO) {
            sessionDao.insertSession(session)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    buildingId = building.id,
                    eventType = NavigationEventType.SESSION_STARTED.name,
                    floor = session.startingFloor,
                    targetNodeId = destinationNode.id,
                    reason = "User initiated indoor navigation to ${destinationNode.name}",
                    details = "Provider: ${positioningProvider.name}, Accessible: $isWheelchair",
                    timestamp = now,
                    isDemo = isDemo
                )
            )
        }

        return session
    }

    fun pauseSession() {
        val current = _activeSession.value ?: return
        val updated = current.copy(status = NavigationSessionStatus.PAUSED.name)
        _activeSession.value = updated

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.PAUSED.name,
                    floor = current.destinationFloor,
                    reason = "User paused navigation",
                    timestamp = System.currentTimeMillis(),
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun resumeSession() {
        val current = _activeSession.value ?: return
        val updated = current.copy(status = NavigationSessionStatus.NAVIGATING.name)
        _activeSession.value = updated

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.RESUMED.name,
                    floor = current.destinationFloor,
                    reason = "User resumed navigation",
                    timestamp = System.currentTimeMillis(),
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun recordReroute(reason: String, floor: Int) {
        val current = _activeSession.value ?: return
        val updated = current.copy(
            routeRecalculationCount = current.routeRecalculationCount + 1
        )
        _activeSession.value = updated

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.ROUTE_RECALCULATED.name,
                    floor = floor,
                    targetNodeId = current.destinationNodeId,
                    reason = reason,
                    timestamp = System.currentTimeMillis(),
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun recordOffRoute(reason: String, floor: Int) {
        val current = _activeSession.value ?: return
        val updated = current.copy(
            offRouteEventCount = current.offRouteEventCount + 1
        )
        _activeSession.value = updated

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.OFF_ROUTE_DETECTED.name,
                    floor = floor,
                    targetNodeId = current.destinationNodeId,
                    reason = reason,
                    timestamp = System.currentTimeMillis(),
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun recordFloorTransition(fromFloor: Int, toFloor: Int, isElevator: Boolean) {
        val current = _activeSession.value ?: return
        val transitionNote = "F$fromFloor->F$toFloor (${if (isElevator) "Elevator" else "Stairs"})"
        val newSummary = if (current.floorsCrossedSummary.isBlank()) {
            transitionNote
        } else {
            "${current.floorsCrossedSummary} | $transitionNote"
        }

        val updated = current.copy(
            floorTransitionsCount = current.floorTransitionsCount + 1,
            elevatorUsageCount = if (isElevator) current.elevatorUsageCount + 1 else current.elevatorUsageCount,
            stairsUsageCount = if (!isElevator) current.stairsUsageCount + 1 else current.stairsUsageCount,
            floorsCrossedSummary = newSummary
        )
        _activeSession.value = updated

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.FLOOR_TRANSITION.name,
                    floor = toFloor,
                    targetNodeId = current.destinationNodeId,
                    reason = "Transition from Floor $fromFloor to Floor $toFloor",
                    details = if (isElevator) "ELEVATOR" else "STAIRS",
                    timestamp = System.currentTimeMillis(),
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun completeSession(arrivedNode: NavNode) {
        val current = _activeSession.value ?: return
        val now = System.currentTimeMillis()
        val durationSec = ((now - current.startTimestamp) / 1000).toInt().coerceAtLeast(1)

        val updated = current.copy(
            endTimestamp = now,
            actualDurationSeconds = durationSec,
            navigationResult = NavigationSessionResult.COMPLETED.name,
            status = NavigationSessionStatus.COMPLETED.name
        )
        _activeSession.value = null

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.SESSION_COMPLETED.name,
                    floor = arrivedNode.floor,
                    targetNodeId = arrivedNode.id,
                    reason = "User safely arrived at ${arrivedNode.name}",
                    details = "Duration: ${durationSec}s, Reroutes: ${current.routeRecalculationCount}",
                    timestamp = now,
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun cancelSession() {
        val current = _activeSession.value ?: return
        val now = System.currentTimeMillis()
        val durationSec = ((now - current.startTimestamp) / 1000).toInt().coerceAtLeast(1)

        val updated = current.copy(
            endTimestamp = now,
            actualDurationSeconds = durationSec,
            navigationResult = NavigationSessionResult.CANCELLED.name,
            status = NavigationSessionStatus.CANCELLED.name
        )
        _activeSession.value = null

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.SESSION_CANCELLED.name,
                    floor = current.destinationFloor,
                    targetNodeId = current.destinationNodeId,
                    reason = "User stopped / cancelled navigation",
                    details = "Duration before cancel: ${durationSec}s",
                    timestamp = now,
                    isDemo = current.isDemoSession
                )
            )
        }
    }

    fun failSession(error: String) {
        val current = _activeSession.value ?: return
        val now = System.currentTimeMillis()
        val durationSec = ((now - current.startTimestamp) / 1000).toInt().coerceAtLeast(1)

        val updated = current.copy(
            endTimestamp = now,
            actualDurationSeconds = durationSec,
            navigationResult = NavigationSessionResult.FAILED.name,
            status = NavigationSessionStatus.FAILED.name
        )
        _activeSession.value = null

        scope.launch(Dispatchers.IO) {
            sessionDao.updateSession(updated)
            sessionDao.insertEvent(
                NavigationEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    buildingId = current.buildingId,
                    eventType = NavigationEventType.SESSION_FAILED.name,
                    floor = current.destinationFloor,
                    targetNodeId = current.destinationNodeId,
                    reason = error,
                    timestamp = now,
                    isDemo = current.isDemoSession
                )
            )
        }
    }
}
