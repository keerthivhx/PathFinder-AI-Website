package com.example

import com.example.data.model.*
import com.example.positioning.IndoorPositionFusionEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IndoorPositionFusionEngineTest {

    private lateinit var fusionEngine: IndoorPositionFusionEngine
    private lateinit var sampleBuilding: Building

    @Before
    fun setup() {
        fusionEngine = IndoorPositionFusionEngine(
            positionSmoothingFactor = 0.4f,
            maxPositionJumpMeters = 10f,
            headingSmoothingFactor = 0.3f,
            graphSnappingThresholdUnits = 60f
        )

        sampleBuilding = Building(
            id = "bld-tech-hub",
            name = "Innovation Hub",
            type = "University",
            description = "Main Academic Tech Center",
            floors = listOf(
                FloorInfo(floorNumber = 0, name = "Ground Floor", shortName = "G"),
                FloorInfo(floorNumber = 1, name = "Level 1", shortName = "L1")
            ),
            nodes = listOf(
                NavNode(id = "n_entry", buildingId = "bld-tech-hub", floor = 0, x = 100f, y = 100f, name = "Main Entrance", code = "ENTR-01", category = NodeCategory.ENTRANCE, description = "Main Entry"),
                NavNode(id = "n_atrium", buildingId = "bld-tech-hub", floor = 0, x = 100f, y = 250f, name = "Central Atrium", code = "ATR-01", category = NodeCategory.RECEPTION, description = "Atrium"),
                NavNode(id = "n_cafe", buildingId = "bld-tech-hub", floor = 0, x = 200f, y = 250f, name = "Innovation Café", code = "CAF-01", category = NodeCategory.FOOD_COURT, description = "Cafeteria"),
                NavNode(id = "n_l1_lobby", buildingId = "bld-tech-hub", floor = 1, x = 100f, y = 100f, name = "Level 1 Lobby", code = "L1-LOB", category = NodeCategory.RECEPTION, description = "Lobby")
            ),
            edges = listOf(
                NavEdge(fromId = "n_entry", toId = "n_atrium", distanceMeters = 15f, edgeType = EdgeType.CORRIDOR),
                NavEdge(fromId = "n_atrium", toId = "n_cafe", distanceMeters = 10f, edgeType = EdgeType.CORRIDOR)
            )
        )
    }

    @Test
    fun testQrCalibrationInitializesHighConfidenceAnchor() {
        val entryNode = sampleBuilding.nodes.first { it.id == "n_entry" }
        val position = fusionEngine.processQrCalibration(entryNode, sampleBuilding)

        assertEquals("bld-tech-hub", position.buildingId)
        assertEquals(0, position.floorId)
        assertEquals(100f, position.x, 0.01f)
        assertEquals(100f, position.y, 0.01f)
        assertEquals(PositionConfidence.HIGH, position.confidence)
        assertEquals(1.0f, position.confidenceScore, 0.01f)
        assertEquals(PositionSource.QR_CALIBRATION, position.source)
    }

    @Test
    fun testPdrStepUpdatesPositionAndDecaysConfidence() {
        val entryNode = sampleBuilding.nodes.first { it.id == "n_entry" }
        fusionEngine.processQrCalibration(entryNode, sampleBuilding)

        // Take a step in positive Y direction (towards atrium)
        val posAfterStep = fusionEngine.processPdrStep(deltaX = 0f, deltaY = 15f, headingDeg = 90f, building = sampleBuilding)

        assertNotNull(posAfterStep)
        assertEquals(0, posAfterStep!!.floorId)
        assertTrue(posAfterStep.y > 100f)
        assertEquals(PositionSource.PDR_SENSOR, posAfterStep.source)
        assertEquals(1, fusionEngine.diagnostics.value.pdrStepCount)
    }

    @Test
    fun testBleMeasurementSmoothsPosition() {
        val entryNode = sampleBuilding.nodes.first { it.id == "n_entry" }
        fusionEngine.processQrCalibration(entryNode, sampleBuilding)

        val blePos = IndoorPosition(
            buildingId = "bld-tech-hub",
            floorId = 0,
            x = 110f,
            y = 120f,
            confidence = PositionConfidence.MEDIUM,
            confidenceScore = 0.8f,
            source = PositionSource.BLE_BEACON
        )

        val fused = fusionEngine.processBleMeasurement(blePos, sampleBuilding)
        assertNotNull(fused)
        // Check that smoothed position is between previous 100 and new 110/120
        assertTrue(fused!!.x >= 100f && fused.x <= 110f)
        assertTrue(fused.y >= 100f && fused.y <= 120f)
    }

    @Test
    fun testKinematicOutlierRejectionRejectsMassiveInstantJump() {
        val entryNode = sampleBuilding.nodes.first { it.id == "n_entry" }
        fusionEngine.processQrCalibration(entryNode, sampleBuilding)

        // Simulate instant jump of 500 units (~50 meters) in 50ms
        val extremeJump = IndoorPosition(
            buildingId = "bld-tech-hub",
            floorId = 0,
            x = 600f,
            y = 600f,
            confidence = PositionConfidence.LOW,
            confidenceScore = 0.4f,
            source = PositionSource.BLE_BEACON
        )

        val initialRejections = fusionEngine.diagnostics.value.outlierRejectionsCount
        val fused = fusionEngine.processBleMeasurement(extremeJump, sampleBuilding)

        // Should reject extreme outlier and increment counter
        assertEquals(initialRejections + 1, fusionEngine.diagnostics.value.outlierRejectionsCount)
        assertNotNull(fused)
        // Position should not jump to 600
        assertTrue(fused!!.x < 300f)
    }

    @Test
    fun testCircularHeadingSmoothingAcrossBoundary() {
        fusionEngine.processHeadingUpdate(355f)
        val smoothed1 = fusionEngine.smoothedHeadingDegrees.value
        assertEquals(355f, smoothed1, 1f)

        // Transition from 355° across 0° to 5° (short path of +10°)
        val smoothed2 = fusionEngine.processHeadingUpdate(5f)
        assertTrue("Heading across 0° boundary should be smooth near 0° or 360°", smoothed2 >= 350f || smoothed2 <= 10f)
    }

    @Test
    fun testFloorArbitrationRequiresSustainedEvidence() {
        val entryNode = sampleBuilding.nodes.first { it.id == "n_entry" }
        fusionEngine.processQrCalibration(entryNode, sampleBuilding)
        assertEquals(0, fusionEngine.fusedPosition.value?.floorId)

        // Single momentary BLE reading on Floor 1 should not instantly flip floor without elevator transition
        val strayL1Ble = IndoorPosition(
            buildingId = "bld-tech-hub",
            floorId = 1,
            x = 100f,
            y = 100f,
            confidence = PositionConfidence.LOW,
            confidenceScore = 0.3f,
            source = PositionSource.BLE_BEACON
        )

        val fused = fusionEngine.processBleMeasurement(strayL1Ble, sampleBuilding, isElevatorTransitionActive = false)
        assertNotNull(fused)
        // Should remain on Floor 0 due to historical evidence window
        assertEquals(0, fused!!.floorId)
    }
}
