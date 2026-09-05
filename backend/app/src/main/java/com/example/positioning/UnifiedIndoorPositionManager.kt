package com.example.positioning

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.example.routing.AStarRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

class UnifiedIndoorPositionManager(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : LocationProvider {

    // Sub-providers & Fusion Engine
    val gpsProvider = GpsLocationProvider(context)
    val bleEngine = BlePositionEngine(context)
    val inertialProvider = InertialPositionProvider(context)
    val fusionEngine = IndoorPositionFusionEngine()

    // Configurable parameters
    var offRouteDistanceThresholdMeters: Float = 15f // Threshold before triggering OFF_ROUTE
    var arrivalDistanceThresholdMeters: Float = 4.0f // Threshold for ARRIVED
    var snappingThresholdUnits: Float = 80f // Threshold in canvas coordinates (~8m)

    // State flows
    private val _locationState = MutableStateFlow<NavigationLocationState>(NavigationLocationState.Idle)
    override val locationState: StateFlow<NavigationLocationState> = _locationState.asStateFlow()

    private val _currentIndoorPosition = MutableStateFlow<IndoorPosition?>(null)
    override val currentIndoorPosition: StateFlow<IndoorPosition?> = _currentIndoorPosition.asStateFlow()

    private val _deviceHeadingDegrees = MutableStateFlow(0f)
    override val deviceHeadingDegrees: StateFlow<Float> = _deviceHeadingDegrees.asStateFlow()

    private val _positioningMode = MutableStateFlow(PositioningMode.HYBRID_FUSION)
    override val positioningMode: StateFlow<PositioningMode> = _positioningMode.asStateFlow()

    private val _registeredBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    val registeredBeacons: StateFlow<List<Beacon>> = _registeredBeacons.asStateFlow()

    val diagnostics: StateFlow<FusionDiagnostics> = fusionEngine.diagnostics

    // Navigation tracking references
    private var activeBuilding: Building? = null
    private var activeRoute: NavigationRoute? = null
    private var currentFloor: Int = 0
    private var offRouteViolationCount: Int = 0

    init {
        // Collect sensor heading updates and pass through circular smoothing filter
        scope.launch {
            inertialProvider.headingDegrees.collect { deg ->
                val smoothed = fusionEngine.processHeadingUpdate(deg)
                _deviceHeadingDegrees.value = smoothed
            }
        }
    }

    fun setRegisteredBeacons(beacons: List<Beacon>) {
        _registeredBeacons.value = beacons
    }

    fun setActiveBuilding(building: Building) {
        activeBuilding = building
    }

    fun setActiveFloor(floor: Int) {
        currentFloor = floor
    }

    override fun updateActiveRoute(route: NavigationRoute?) {
        activeRoute = route
        offRouteViolationCount = 0
        if (route == null) {
            if (_locationState.value is NavigationLocationState.IndoorNavigating ||
                _locationState.value is NavigationLocationState.OffRoute ||
                _locationState.value is NavigationLocationState.Arrived) {
                _locationState.value = NavigationLocationState.Idle
            }
        }
    }

    override fun setPositioningMode(mode: PositioningMode) {
        _positioningMode.value = mode
    }

    override fun startTracking() {
        gpsProvider.startListening()

        if (_positioningMode.value == PositioningMode.HYBRID_FUSION || _positioningMode.value == PositioningMode.REAL_BLE_ONLY) {
            bleEngine.startBleScanning { beaconId, rssi ->
                onBleBeaconRead(beaconId, rssi)
            }
        }

        inertialProvider.startListening { deltaX, deltaY, headingDeg ->
            onStepPdrDelta(deltaX, deltaY, headingDeg)
        }
    }

    override fun stopTracking() {
        gpsProvider.stopListening()
        bleEngine.stopBleScanning()
        inertialProvider.stopListening()
    }

    fun calibrateWithQr(qrText: String): Result<NavNode> {
        val bld = activeBuilding ?: return Result.failure(Exception("No active building set"))
        return calibrateWithQr(qrText, bld)
    }

    override fun calibrateWithQr(qrText: String, building: Building): Result<NavNode> {
        val result = QrLocationProvider.parseAndValidateQr(qrText, building)
        if (result.isSuccess) {
            val node = result.getOrThrow()
            activeBuilding = building
            currentFloor = node.floor

            val fusedPos = fusionEngine.processQrCalibration(node, building)
            _currentIndoorPosition.value = fusedPos
            updateNavigationProgress(fusedPos)
        }
        return result
    }

    override fun calibrateManual(node: NavNode) {
        currentFloor = node.floor
        val fusedPos = fusionEngine.processManualAnchor(node)
        _currentIndoorPosition.value = fusedPos
        updateNavigationProgress(fusedPos)
    }

    fun confirmFloorTransition(targetFloor: Int) {
        currentFloor = targetFloor
        val bld = activeBuilding
        val current = _currentIndoorPosition.value
        val nodeOnFloor = bld?.nodes?.firstOrNull { it.floor == targetFloor }
        if (nodeOnFloor != null && current != null) {
            val updatedPos = current.copy(
                floorId = targetFloor,
                x = nodeOnFloor.x,
                y = nodeOnFloor.y,
                nearestNodeId = nodeOnFloor.id
            )
            _currentIndoorPosition.value = updatedPos
            updateNavigationProgress(updatedPos)
        }
    }

    fun triggerSimulatedStep(headingOverride: Float? = null) {
        val heading = headingOverride ?: _deviceHeadingDegrees.value
        val stepLengthUnits = 14f // ~1.4 meters per step in map units
        val rad = Math.toRadians(heading.toDouble())
        val dx = (Math.cos(rad) * stepLengthUnits).toFloat()
        val dy = (Math.sin(rad) * stepLengthUnits).toFloat()
        onStepPdrDelta(dx, dy, heading)
    }

    fun simulateBeaconSignal(beaconId: String, rssi: Int) {
        bleEngine.simulateBeaconReading(beaconId, rssi)
        onBleBeaconRead(beaconId, rssi)
    }

    fun simulateGpsLocation(latitude: Double, longitude: Double) {
        gpsProvider.simulateGpsLocation(latitude, longitude)
    }

    private fun onBleBeaconRead(beaconId: String, rssi: Int) {
        val bld = activeBuilding ?: return
        val calculated = bleEngine.calculateIndoorPosition(_registeredBeacons.value, bld.id, currentFloor)
        if (calculated != null) {
            val isElevatorActive = _locationState.value is NavigationLocationState.FloorTransition ||
                    _locationState.value is NavigationLocationState.AtFloorTransition
            val fusedPos = fusionEngine.processBleMeasurement(calculated, bld, isElevatorTransitionActive = isElevatorActive)
            if (fusedPos != null) {
                _currentIndoorPosition.value = fusedPos
                currentFloor = fusedPos.floorId
                updateNavigationProgress(fusedPos)
            }
        }
    }

    private fun onStepPdrDelta(deltaX: Float, deltaY: Float, headingDeg: Float) {
        val fusedPos = fusionEngine.processPdrStep(deltaX, deltaY, headingDeg, activeBuilding)
        if (fusedPos != null) {
            _currentIndoorPosition.value = fusedPos
            updateNavigationProgress(fusedPos)
        }
    }

    /**
     * Updates active navigation progress, off-route detection, floor transition, and arrival.
     */
    fun updateNavigationProgress(pos: IndoorPosition) {
        val route = activeRoute ?: return
        val bld = activeBuilding ?: return
        val destNode = route.destinationNode

        // Canvas coordinates conversion factor (~10 units ~ 1 meter)
        val unitsPerMeter = 10f

        // 1. Check Arrival
        if (destNode != null && pos.floorId == destNode.floor) {
            val distToDestMeters = hypot(pos.x - destNode.x, pos.y - destNode.y) / unitsPerMeter
            if (distToDestMeters <= arrivalDistanceThresholdMeters) {
                _locationState.value = NavigationLocationState.Arrived(destNode, route.totalDistanceMeters)
                return
            }
        }

        // 2. Check Route Proximity & Step Index
        val floorPathNodes = route.pathNodes.filter { it.floor == pos.floorId }
        if (floorPathNodes.isEmpty()) {
            // User is on a different floor than route current path
            val targetFloorNode = route.pathNodes.firstOrNull()
            if (targetFloorNode != null && targetFloorNode.floor != pos.floorId) {
                val transferNode = route.pathNodes.firstOrNull { it.category == NodeCategory.ELEVATOR || it.category == NodeCategory.STAIRS }
                val isElevator = transferNode?.category == NodeCategory.ELEVATOR
                _locationState.value = NavigationLocationState.FloorTransition(
                    fromFloor = pos.floorId,
                    toFloor = targetFloorNode.floor,
                    viaNodeName = transferNode?.name ?: (if (isElevator) "Elevator" else "Stairs"),
                    isElevator = isElevator
                )
                return
            }
        }

        // Find closest point on active route path
        var minDistanceToRouteUnits = Float.MAX_VALUE
        var closestStepIndex = 0

        for ((idx, step) in route.steps.withIndex()) {
            val targetN = step.targetNode
            if (targetN != null && targetN.floor == pos.floorId) {
                val d = hypot(pos.x - targetN.x, pos.y - targetN.y)
                if (d < minDistanceToRouteUnits) {
                    minDistanceToRouteUnits = d
                    closestStepIndex = idx
                }
            }
        }

        val distanceOffRouteMeters = minDistanceToRouteUnits / unitsPerMeter

        // 3. Off-Route Detection (requires multiple consecutive violations to prevent noise flicker)
        if (distanceOffRouteMeters > offRouteDistanceThresholdMeters) {
            offRouteViolationCount++
            if (offRouteViolationCount >= 3) {
                _locationState.value = NavigationLocationState.OffRoute(distanceOffRouteMeters, pos)
                return
            }
        } else {
            offRouteViolationCount = 0
        }

        // 4. Floor Transition Detection on active route
        val currentStep = route.steps.getOrNull(closestStepIndex)
        if (currentStep != null && (currentStep.turnType == TurnType.ELEVATOR_UP || currentStep.turnType == TurnType.ELEVATOR_DOWN ||
            currentStep.turnType == TurnType.STAIRS_UP || currentStep.turnType == TurnType.STAIRS_DOWN)) {
            val isElevator = currentStep.turnType == TurnType.ELEVATOR_UP || currentStep.turnType == TurnType.ELEVATOR_DOWN
            val nextStep = route.steps.getOrNull(closestStepIndex + 1)
            val toFloor = nextStep?.floor ?: (pos.floorId + 1)
            _locationState.value = NavigationLocationState.FloorTransition(
                fromFloor = pos.floorId,
                toFloor = toFloor,
                viaNodeName = currentStep.targetNode?.name ?: (if (isElevator) "Elevator" else "Stairwell"),
                isElevator = isElevator
            )
            return
        }

        // 5. Normal Indoor Navigation State
        val remainingSteps = route.steps.drop(closestStepIndex)
        val remainingDistance = remainingSteps.sumOf { it.distanceMeters.toDouble() }.toFloat()
        val remainingMinutes = (remainingDistance / (1.4f * 60f)).toInt().coerceAtLeast(1)
        val nearestGraphNode = bld.nodes.filter { it.floor == pos.floorId }.minByOrNull { hypot(it.x - pos.x, it.y - pos.y) }

        _locationState.value = NavigationLocationState.IndoorNavigating(
            position = pos,
            nearestNode = nearestGraphNode,
            currentFloor = pos.floorId,
            stepIndex = closestStepIndex,
            remainingDistanceMeters = remainingDistance,
            remainingTimeMinutes = remainingMinutes
        )
    }

    /**
     * Executes automatic A* recalculation from current indoor position to destination node.
     */
    fun performReroute(wheelchairOnly: Boolean = false, isEmergency: Boolean = false): NavigationRoute? {
        val bld = activeBuilding ?: return null
        val currentPos = _currentIndoorPosition.value ?: return null
        val currentDest = activeRoute?.destinationNode ?: return null

        val nearestNodeOnCurrentFloor = bld.nodes.filter { it.floor == currentPos.floorId }
            .minByOrNull { hypot(it.x - currentPos.x, it.y - currentPos.y) } ?: return null

        _locationState.value = NavigationLocationState.Rerouting("Calculating new route from current position…")

        val result = AStarRouter.findPath(
            building = bld,
            startNodeId = nearestNodeOnCurrentFloor.id,
            targetNodeId = currentDest.id,
            wheelchairOnly = wheelchairOnly,
            isEmergency = isEmergency
        )

        val newRoute = result.route
        if (newRoute != null) {
            activeRoute = newRoute
            offRouteViolationCount = 0
            updateNavigationProgress(currentPos)
        }
        return newRoute
    }

    /**
     * Simulates walking along a route step by step (Simulation / Demo Mode).
     */
    fun stepSimulationAlongRoute() {
        val route = activeRoute ?: return
        val currentPos = _currentIndoorPosition.value
        val path = route.pathNodes

        if (path.isEmpty()) return

        val currentIndex = if (currentPos != null) {
            val idx = path.indexOfFirst { it.id == currentPos.nearestNodeId }
            if (idx >= 0 && idx + 1 < path.size) idx + 1 else 0
        } else {
            0
        }

        val targetNode = path[currentIndex]
        currentFloor = targetNode.floor

        val simulatedPos = IndoorPosition(
            buildingId = targetNode.buildingId,
            floorId = targetNode.floor,
            x = targetNode.x,
            y = targetNode.y,
            nearestNodeId = targetNode.id,
            headingDegrees = _deviceHeadingDegrees.value,
            confidence = PositionConfidence.HIGH,
            confidenceScore = 0.95f,
            source = PositionSource.SIMULATION,
            isSnapped = true,
            snappedNodeId = targetNode.id
        )

        val fused = fusionEngine.processSimulationPosition(simulatedPos)
        _currentIndoorPosition.value = fused
        updateNavigationProgress(fused)
    }
}

