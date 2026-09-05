package com.example.positioning

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Diagnostics information exposed for dev/debug overlays and telemetry.
 */
data class FusionDiagnostics(
    val lastSource: PositionSource = PositionSource.QR_CALIBRATION,
    val rawBleX: Float? = null,
    val rawBleY: Float? = null,
    val rawBleFloor: Int? = null,
    val pdrStepCount: Int = 0,
    val activeBeaconsCount: Int = 0,
    val smoothedHeadingDegrees: Float = 0f,
    val outlierRejectedCount: Int = 0,
    val positionSmoothFactor: Float = 0.35f,
    val confidenceScore: Float = 1.0f,
    val isSnappedToGraph: Boolean = false,
    val floorArbitrated: Int = 0,
    val stateTransitionReason: String = "Stable",
    val lastUpdateTimeMs: Long = System.currentTimeMillis()
) {
    val activeBeaconCount: Int get() = activeBeaconsCount
    val outlierRejectionsCount: Int get() = outlierRejectedCount
}

/**
 * Phase 8 Indoor Position Fusion Engine:
 * Combines BLE trilateration, QR ground-truth, PDR inertial sensors, and Graph Snapping
 * with outlier rejection, low-pass smoothing, circular heading filtering, and floor arbitration.
 */
class IndoorPositionFusionEngine(
    var positionSmoothingFactor: Float = 0.35f,
    var maxPositionJumpMeters: Float = 12.0f,
    var maxSpeedUnitsPerSec: Float = 40.0f, // ~4m/s maximum realistic indoor traversal
    var headingSmoothingFactor: Float = 0.25f,
    var graphSnappingThresholdUnits: Float = 80f // ~8 meters in canvas space
) {
    companion object {
        const val CANVAS_UNITS_PER_METER = 10f
    }

    private val _fusedPosition = MutableStateFlow<IndoorPosition?>(null)
    val fusedPosition: StateFlow<IndoorPosition?> = _fusedPosition.asStateFlow()

    private val _smoothedHeadingDegrees = MutableStateFlow(0f)
    val smoothedHeadingDegrees: StateFlow<Float> = _smoothedHeadingDegrees.asStateFlow()

    private val _diagnostics = MutableStateFlow(FusionDiagnostics())
    val diagnostics: StateFlow<FusionDiagnostics> = _diagnostics.asStateFlow()

    // Internal tracker state
    private var lastRawBlePosition: IndoorPosition? = null
    private var lastTimestampMs: Long = System.currentTimeMillis()
    private var consecutiveOutlierCount: Int = 0
    private var totalOutliersRejected: Int = 0
    private var pdrTotalSteps: Int = 0
    private var hasInitialHeading: Boolean = false

    // Floor transition arbitration
    private val floorEvidenceHistory = mutableListOf<Pair<Int, Long>>() // Floor to Timestamp

    /**
     * Resets fusion state when starting a new session or building.
     */
    fun reset() {
        _fusedPosition.value = null
        _smoothedHeadingDegrees.value = 0f
        hasInitialHeading = false
        lastRawBlePosition = null
        consecutiveOutlierCount = 0
        pdrTotalSteps = 0
        floorEvidenceHistory.clear()
        _diagnostics.value = FusionDiagnostics()
    }

    /**
     * 1. QR Calibration: Hard ground-truth anchor (Confidence = 1.0, Source = QR).
     * Instantly resets outlier baseline and establishes current floor.
     */
    fun processQrCalibration(node: NavNode, building: Building): IndoorPosition {
        val now = System.currentTimeMillis()
        val pos = IndoorPosition(
            buildingId = building.id,
            floorId = node.floor,
            x = node.x,
            y = node.y,
            nearestNodeId = node.id,
            headingDegrees = _smoothedHeadingDegrees.value,
            confidence = PositionConfidence.HIGH,
            confidenceScore = 1.0f,
            source = PositionSource.QR_CALIBRATION,
            activeBeaconsCount = 0,
            timestamp = now,
            accuracyMeters = 0.5f,
            isSnapped = true,
            snappedNodeId = node.id
        )

        lastTimestampMs = now
        consecutiveOutlierCount = 0
        _fusedPosition.value = pos
        recordFloorEvidence(node.floor)

        updateDiagnostics(
            lastSource = PositionSource.QR_CALIBRATION,
            confidence = 1.0f,
            isSnapped = true
        )
        return pos
    }

    /**
     * 2. Manual Anchor: User manually tapped a node on the map.
     */
    fun processManualAnchor(node: NavNode): IndoorPosition {
        val now = System.currentTimeMillis()
        val pos = IndoorPosition(
            buildingId = node.buildingId,
            floorId = node.floor,
            x = node.x,
            y = node.y,
            nearestNodeId = node.id,
            headingDegrees = _smoothedHeadingDegrees.value,
            confidence = PositionConfidence.HIGH,
            confidenceScore = 1.0f,
            source = PositionSource.MANUAL,
            timestamp = now,
            accuracyMeters = 0.8f,
            isSnapped = true,
            snappedNodeId = node.id
        )

        lastTimestampMs = now
        consecutiveOutlierCount = 0
        _fusedPosition.value = pos
        recordFloorEvidence(node.floor)

        updateDiagnostics(
            lastSource = PositionSource.MANUAL,
            confidence = 1.0f,
            isSnapped = true
        )
        return pos
    }

    /**
     * 3. BLE Reading Fusion: Evaluates trilaterated raw BLE position against kinematics
     * and performs exponential smoothing + graph snapping.
     */
    fun processBleMeasurement(
        rawBlePos: IndoorPosition,
        building: Building?,
        isElevatorTransitionActive: Boolean = false
    ): IndoorPosition? {
        val now = System.currentTimeMillis()
        val current = _fusedPosition.value
        lastRawBlePosition = rawBlePos

        recordFloorEvidence(rawBlePos.floorId)

        // If no prior position, accept first BLE fix with caution
        if (current == null) {
            val snapped = if (building != null) {
                GraphSnapper.snapToFloorGraph(rawBlePos.x, rawBlePos.y, rawBlePos.floorId, building, graphSnappingThresholdUnits)
            } else null

            val initialPos = rawBlePos.copy(
                x = snapped?.snappedX ?: rawBlePos.x,
                y = snapped?.snappedY ?: rawBlePos.y,
                nearestNodeId = snapped?.nearestNode?.id ?: rawBlePos.nearestNodeId,
                headingDegrees = _smoothedHeadingDegrees.value,
                source = PositionSource.BLE_BEACON,
                isSnapped = snapped != null,
                snappedNodeId = snapped?.nearestNode?.id
            )
            _fusedPosition.value = initialPos
            lastTimestampMs = now
            updateDiagnostics(
                lastSource = PositionSource.BLE_BEACON,
                rawX = rawBlePos.x,
                rawY = rawBlePos.y,
                rawFloor = rawBlePos.floorId,
                beacons = rawBlePos.activeBeaconsCount,
                confidence = initialPos.confidenceScore,
                isSnapped = snapped != null
            )
            return initialPos
        }

        // Check Floor Consistency
        val targetFloor = determineConsistentFloor(current.floorId, isElevatorTransitionActive)
        val floorChanged = targetFloor != current.floorId

        // Kinematic Outlier Check
        val timeDeltaSec = max(0.1f, (now - lastTimestampMs) / 1000f)
        val distanceJumpUnits = hypot(rawBlePos.x - current.x, rawBlePos.y - current.y)
        val impliedSpeedUnitsPerSec = distanceJumpUnits / timeDeltaSec
        val maxAllowedJumpUnits = maxPositionJumpMeters * CANVAS_UNITS_PER_METER

        val isOutlier = !floorChanged && (distanceJumpUnits > maxAllowedJumpUnits || impliedSpeedUnitsPerSec > maxSpeedUnitsPerSec)

        if (isOutlier) {
            consecutiveOutlierCount++
            totalOutliersRejected++
            // If outlier persists for >= 4 consecutive readings, user likely teleported or environment altered -> accept with warning
            if (consecutiveOutlierCount < 4) {
                updateDiagnostics(
                    lastSource = PositionSource.BLE_BEACON,
                    rawX = rawBlePos.x,
                    rawY = rawBlePos.y,
                    rawFloor = rawBlePos.floorId,
                    beacons = rawBlePos.activeBeaconsCount,
                    confidence = current.confidenceScore * 0.9f,
                    isSnapped = current.isSnapped
                )
                return current
            }
        }

        consecutiveOutlierCount = 0
        lastTimestampMs = now

        // Exponential smoothing on (X, Y)
        val alpha = if (floorChanged) 1.0f else positionSmoothingFactor.coerceIn(0.1f, 0.9f)
        val smoothedX = (1f - alpha) * current.x + alpha * rawBlePos.x
        val smoothedY = (1f - alpha) * current.y + alpha * rawBlePos.y

        // Graph Snapping
        val snapped = if (building != null) {
            GraphSnapper.snapToFloorGraph(smoothedX, smoothedY, targetFloor, building, graphSnappingThresholdUnits)
        } else null

        val finalX = snapped?.snappedX ?: smoothedX
        val finalY = snapped?.snappedY ?: smoothedY

        // Compute Fused Confidence
        val beaconFactor = min(1.0f, rawBlePos.activeBeaconsCount / 3.0f)
        val distancePenalty = min(0.3f, (distanceJumpUnits / maxAllowedJumpUnits) * 0.3f)
        val fusedConfidenceScore = (0.5f + (beaconFactor * 0.4f) - distancePenalty).coerceIn(0.4f, 0.98f)

        val fusedPos = IndoorPosition(
            buildingId = current.buildingId,
            floorId = targetFloor,
            x = finalX,
            y = finalY,
            nearestNodeId = snapped?.nearestNode?.id ?: current.nearestNodeId,
            headingDegrees = _smoothedHeadingDegrees.value,
            confidence = when {
                fusedConfidenceScore >= 0.75f -> PositionConfidence.HIGH
                fusedConfidenceScore >= 0.50f -> PositionConfidence.MEDIUM
                else -> PositionConfidence.LOW
            },
            confidenceScore = fusedConfidenceScore,
            source = PositionSource.BLE_BEACON,
            activeBeaconsCount = rawBlePos.activeBeaconsCount,
            timestamp = now,
            accuracyMeters = (1.2f + (1.0f - fusedConfidenceScore) * 3.5f),
            isSnapped = snapped != null,
            snappedNodeId = snapped?.nearestNode?.id
        )

        _fusedPosition.value = fusedPos
        updateDiagnostics(
            lastSource = PositionSource.BLE_BEACON,
            rawX = rawBlePos.x,
            rawY = rawBlePos.y,
            rawFloor = rawBlePos.floorId,
            beacons = rawBlePos.activeBeaconsCount,
            confidence = fusedConfidenceScore,
            isSnapped = snapped != null
        )
        return fusedPos
    }

    /**
     * 4. PDR Step Sensor Delta: Dead-reckoning integration between BLE scan intervals.
     */
    fun processPdrStep(
        deltaX: Float,
        deltaY: Float,
        headingDeg: Float,
        building: Building?
    ): IndoorPosition? {
        val current = _fusedPosition.value ?: return null
        val now = System.currentTimeMillis()
        pdrTotalSteps++

        // Update smoothed heading
        processHeadingUpdate(headingDeg)

        // Raw Dead-Reckoned position
        val rawX = (current.x + deltaX).coerceIn(20f, 980f)
        val rawY = (current.y + deltaY).coerceIn(20f, 980f)

        // Graph Snapping along corridors
        val snapped = if (building != null) {
            GraphSnapper.snapToFloorGraph(rawX, rawY, current.floorId, building, graphSnappingThresholdUnits)
        } else null

        val finalX = snapped?.snappedX ?: rawX
        val finalY = snapped?.snappedY ?: rawY

        // Gradual confidence decay when purely on PDR
        val decayedConfidence = (current.confidenceScore * 0.992f).coerceAtLeast(0.45f)

        val updatedPos = IndoorPosition(
            buildingId = current.buildingId,
            floorId = current.floorId,
            x = finalX,
            y = finalY,
            nearestNodeId = snapped?.nearestNode?.id ?: current.nearestNodeId,
            headingDegrees = _smoothedHeadingDegrees.value,
            confidence = when {
                decayedConfidence >= 0.75f -> PositionConfidence.HIGH
                decayedConfidence >= 0.50f -> PositionConfidence.MEDIUM
                else -> PositionConfidence.LOW
            },
            confidenceScore = decayedConfidence,
            source = if (current.source == PositionSource.BLE_BEACON) {
                PositionSource.BLE_BEACON
            } else {
                PositionSource.PDR_SENSOR
            },
            activeBeaconsCount = current.activeBeaconsCount,
            timestamp = now,
            accuracyMeters = current.accuracyMeters + 0.05f,
            isSnapped = snapped != null,
            snappedNodeId = snapped?.nearestNode?.id
        )

        _fusedPosition.value = updatedPos
        lastTimestampMs = now

        updateDiagnostics(
            lastSource = PositionSource.PDR_SENSOR,
            confidence = decayedConfidence,
            isSnapped = snapped != null
        )
        return updatedPos
    }

    /**
     * 5. Circular Heading Filter: Low-pass filter with angular boundary wrapping ($0^\circ \leftrightarrow 360^\circ$).
     */
    fun processHeadingUpdate(rawHeadingDeg: Float): Float {
        if (!hasInitialHeading) {
            hasInitialHeading = true
            val initial = (rawHeadingDeg % 360f + 360f) % 360f
            _smoothedHeadingDegrees.value = initial
            val currentPos = _fusedPosition.value
            if (currentPos != null) {
                _fusedPosition.value = currentPos.copy(headingDegrees = initial)
            }
            return initial
        }

        val curr = _smoothedHeadingDegrees.value
        var diff = (rawHeadingDeg - curr) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f

        val smoothed = (curr + diff * headingSmoothingFactor.coerceIn(0.05f, 0.8f) + 360f) % 360f
        _smoothedHeadingDegrees.value = smoothed

        val currentPos = _fusedPosition.value
        if (currentPos != null) {
            _fusedPosition.value = currentPos.copy(headingDegrees = smoothed)
        }
        return smoothed
    }

    /**
     * 6. Simulation Position Injection: Direct mock update for hardware-free simulation.
     */
    fun processSimulationPosition(simPos: IndoorPosition): IndoorPosition {
        _fusedPosition.value = simPos
        _smoothedHeadingDegrees.value = simPos.headingDegrees
        lastTimestampMs = System.currentTimeMillis()
        recordFloorEvidence(simPos.floorId)
        updateDiagnostics(
            lastSource = PositionSource.SIMULATION,
            confidence = simPos.confidenceScore,
            isSnapped = simPos.isSnapped
        )
        return simPos
    }

    private fun recordFloorEvidence(floor: Int) {
        val now = System.currentTimeMillis()
        floorEvidenceHistory.add(Pair(floor, now))
        // Keep last 10 seconds of evidence
        floorEvidenceHistory.removeAll { (now - it.second) > 10000L }
    }

    /**
     * Floor arbitration: Requires strong consistent evidence before switching floor to prevent vertical RF jitter.
     */
    private fun determineConsistentFloor(currentFloor: Int, isElevatorActive: Boolean): Int {
        if (floorEvidenceHistory.isEmpty()) return currentFloor
        if (isElevatorActive) {
            // In elevator transition, respond promptly to latest beacon evidence
            return floorEvidenceHistory.last().first
        }

        val counts = floorEvidenceHistory.groupingBy { it.first }.eachCount()
        val candidate = counts.maxByOrNull { it.value } ?: return currentFloor

        // Require at least 3 consistent readings before floor switch
        return if (candidate.value >= 3) {
            candidate.key
        } else {
            currentFloor
        }
    }

    private fun updateDiagnostics(
        lastSource: PositionSource,
        rawX: Float? = _diagnostics.value.rawBleX,
        rawY: Float? = _diagnostics.value.rawBleY,
        rawFloor: Int? = _diagnostics.value.rawBleFloor,
        beacons: Int = _diagnostics.value.activeBeaconsCount,
        confidence: Float = _diagnostics.value.confidenceScore,
        isSnapped: Boolean = _diagnostics.value.isSnappedToGraph
    ) {
        _diagnostics.value = FusionDiagnostics(
            lastSource = lastSource,
            rawBleX = rawX,
            rawBleY = rawY,
            rawBleFloor = rawFloor,
            pdrStepCount = pdrTotalSteps,
            activeBeaconsCount = beacons,
            smoothedHeadingDegrees = _smoothedHeadingDegrees.value,
            outlierRejectedCount = totalOutliersRejected,
            positionSmoothFactor = positionSmoothingFactor,
            confidenceScore = confidence,
            isSnappedToGraph = isSnapped,
            lastUpdateTimeMs = System.currentTimeMillis()
        )
    }
}
