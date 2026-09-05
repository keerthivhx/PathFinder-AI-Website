package com.example

import com.example.data.model.*
import com.example.positioning.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BlePositioningUnitTest {

    private lateinit var bleEngine: BlePositionEngine
    private lateinit var testBuilding: Building

    @Before
    fun setUp() {
        bleEngine = BlePositionEngine()
        testBuilding = Building(
            id = "engineering-block",
            name = "Engineering Block",
            type = "University",
            description = "Main Academic Building",
            floors = listOf(
                FloorInfo(floorNumber = 0, name = "Ground Floor", shortName = "G"),
                FloorInfo(floorNumber = 1, name = "First Floor", shortName = "F1")
            ),
            nodes = listOf(
                NavNode(id = "node-gf-01", buildingId = "engineering-block", floor = 0, x = 100f, y = 100f, name = "Main Entrance", code = "ENTR-01", category = NodeCategory.ENTRANCE, description = "Main Entry"),
                NavNode(id = "node-gf-02", buildingId = "engineering-block", floor = 0, x = 300f, y = 100f, name = "Central Atrium", code = "ATR-01", category = NodeCategory.RECEPTION, description = "Atrium"),
                NavNode(id = "node-gf-03", buildingId = "engineering-block", floor = 0, x = 600f, y = 100f, name = "AI Lab", code = "CS-204", category = NodeCategory.CLASSROOM, description = "AI Lab"),
                NavNode(id = "node-f1-01", buildingId = "engineering-block", floor = 1, x = 100f, y = 100f, name = "Floor 1 Landing", code = "L1-LOB", category = NodeCategory.RECEPTION, description = "Landing")
            ),
            edges = listOf(
                NavEdge(fromId = "node-gf-01", toId = "node-gf-02", distanceMeters = 20f),
                NavEdge(fromId = "node-gf-02", toId = "node-gf-03", distanceMeters = 30f)
            )
        )
    }

    @Test
    fun testRssiSmoothingEMA() {
        val beaconId = "BEACON-GF-01"
        // 1st reading
        val smoothed1 = bleEngine.updateRssiReading(beaconId, -60)
        assertEquals(-60f, smoothed1, 0.1f)

        // 2nd reading: EMA = 0.35 * -80 + 0.65 * -60 = -28 + -39 = -67
        val smoothed2 = bleEngine.updateRssiReading(beaconId, -80)
        assertEquals(-67f, smoothed2, 0.5f)
    }

    @Test
    fun testRssiToDistanceEstimation() {
        // At txPower (-59 dBm) distance is 1.0 meter
        val dist1m = bleEngine.estimateDistance(rssi = -59f, txPower = -59)
        assertEquals(1.0f, dist1m, 0.1f)

        // Higher attenuation = greater distance
        val distFar = bleEngine.estimateDistance(rssi = -80f, txPower = -59)
        assertTrue("Distance should be greater than 1m for -80dBm", distFar > 1.0f)
    }

    @Test
    fun testMultiBeaconTrilateration() {
        val beacons = listOf(
            Beacon(beaconId = "B1", buildingId = "engineering-block", floorId = 0, x = 100f, y = 100f, txPower = -59),
            Beacon(beaconId = "B2", buildingId = "engineering-block", floorId = 0, x = 300f, y = 100f, txPower = -59),
            Beacon(beaconId = "B3", buildingId = "engineering-block", floorId = 0, x = 200f, y = 300f, txPower = -59)
        )

        // Simulate strong signal near B2
        bleEngine.simulateBeaconReading("B1", -75)
        bleEngine.simulateBeaconReading("B2", -58)
        bleEngine.simulateBeaconReading("B3", -78)

        val position = bleEngine.calculateIndoorPosition(beacons, "engineering-block", 0)
        assertNotNull("Position should be computed from 3 beacons", position)
        assertEquals(0, position?.floorId)
        // Computed position should be closest to B2 (300, 100)
        assertTrue("X coordinate should be near B2", position!!.x > 200f && position.x < 350f)
        assertTrue("Confidence score should be reasonable", position.confidenceScore > 0.4f)
    }

    @Test
    fun testFloorIdentificationFromDominantBeacons() {
        val beacons = listOf(
            Beacon(beaconId = "B-GF-1", buildingId = "engineering-block", floorId = 0, x = 100f, y = 100f),
            Beacon(beaconId = "B-F1-1", buildingId = "engineering-block", floorId = 1, x = 100f, y = 100f),
            Beacon(beaconId = "B-F1-2", buildingId = "engineering-block", floorId = 1, x = 300f, y = 100f)
        )

        // Strong signals on Floor 1
        bleEngine.simulateBeaconReading("B-GF-1", -95)
        bleEngine.simulateBeaconReading("B-F1-1", -55)
        bleEngine.simulateBeaconReading("B-F1-2", -60)

        val position = bleEngine.calculateIndoorPosition(beacons, "engineering-block", 0)
        assertNotNull(position)
        assertEquals("Should identify floor 1 based on dominant signals", 1, position?.floorId)
    }

    @Test
    fun testGraphSnappingUtility() {
        val snapped = GraphSnapper.snapToFloorGraph(
            rawX = 310f,
            rawY = 115f,
            floor = 0,
            building = testBuilding,
            maxSnapDistanceThreshold = 50f
        )
        assertNotNull("Should snap to edge near (300, 100)", snapped)
    }

    @Test
    fun testSimulationMode() {
        val mockBeaconId = "SIM-BEACON-01"
        val smoothed = bleEngine.simulateBeaconReading(mockBeaconId, -52)
        assertEquals(-52f, smoothed, 0.1f)
    }
}
