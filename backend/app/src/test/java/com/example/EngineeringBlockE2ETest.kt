package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiNavService
import com.example.data.model.*
import com.example.data.repository.BuildingRepository
import com.example.positioning.*
import com.example.routing.AStarRouter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EngineeringBlockE2ETest {

    private lateinit var context: Context
    private lateinit var repository: BuildingRepository
    private lateinit var engineeringBuilding: Building

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = BuildingRepository(context)
        val building = repository.getBuildingById("univ")
        assertNotNull("Engineering Block ('univ') must exist in repository", building)
        engineeringBuilding = building!!
    }

    /**
     * Test 1: Search "AI Lab" -> Select AI Lab destination
     */
    @Test
    fun test1_searchAndSelectAILab() {
        val searchResults = repository.searchLocations("AI Lab", buildingId = "univ")
        assertTrue("Searching 'AI Lab' in Engineering Block should yield results", searchResults.isNotEmpty())
        
        val aiLabNode = searchResults.firstOrNull { it.id == "univ_cslab" }
        assertNotNull("AI Lab node 'univ_cslab' (Room 204) must be found", aiLabNode)
        assertEquals("AI Laboratory", aiLabNode?.name)
        assertEquals("AI-204", aiLabNode?.code)
        assertEquals(2, aiLabNode?.floor)
        assertEquals(NodeCategory.LABORATORY, aiLabNode?.category)
    }

    /**
     * Test 2: Calculate A* Route from Entrance (Floor 0) to AI Lab (Floor 2)
     */
    @Test
    fun test2_calculateAStarRouteToAILab() {
        val startNodeId = "univ_gate" // Ground floor entrance
        val targetNodeId = "univ_cslab" // Floor 2 AI Lab

        val pathResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = startNodeId,
            targetNodeId = targetNodeId,
            wheelchairOnly = false,
            isEmergency = false
        )

        assertNotNull("A* router must find a valid route to AI Lab", pathResult.route)
        val route = pathResult.route!!

        assertEquals("Route must start at Main Entrance", "univ_gate", route.pathNodes.first().id)
        assertEquals("Route must terminate at AI Laboratory", "univ_cslab", route.pathNodes.last().id)
        assertTrue("Total distance must be positive", route.totalDistanceMeters > 0f)
        assertTrue("Estimated time must be positive", route.estimatedTimeSeconds > 0)
        assertTrue("Steps must be generated", route.steps.isNotEmpty())

        // Verify floor span across 0 -> 1 -> 2
        val floorsVisited = route.pathNodes.map { it.floor }.distinct()
        assertTrue("Route must visit Ground Floor (0)", floorsVisited.contains(0))
        assertTrue("Route must arrive at Floor 2", floorsVisited.contains(2))
    }

    /**
     * Test 3: Navigation Instructions and Voice Guidance Steps
     */
    @Test
    fun test3_navigationInstructionsAndTurnTypes() {
        val pathResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_gate",
            targetNodeId = "univ_cslab",
            wheelchairOnly = false,
            isEmergency = false
        )
        val route = pathResult.route!!

        // Must have initial departure instruction
        val firstStep = route.steps.first()
        assertTrue("First step instruction should not be empty", firstStep.instruction.isNotBlank())

        // Must have vertical transition step (Elevator or Stairs)
        val hasVerticalTransition = route.steps.any {
            it.turnType == TurnType.ELEVATOR_UP || it.turnType == TurnType.STAIRS_UP
        }
        assertTrue("Multi-floor route must contain ELEVATOR_UP or STAIRS_UP turn type", hasVerticalTransition)

        // Must have final arrival step
        val arrivalStep = route.steps.last()
        assertEquals("Final step must be ARRIVAL", TurnType.ARRIVAL, arrivalStep.turnType)
        assertTrue("Arrival step should mention arrival or destination name", 
            arrivalStep.instruction.contains("Arrived", ignoreCase = true) || 
            arrivalStep.instruction.contains("AI", ignoreCase = true))
    }

    /**
     * Test 4: Floor Transition (Floor 0 -> Floor 2 via Elevator vs Stairs)
     */
    @Test
    fun test4_multiFloorTransitions() {
        // Test Elevator path
        val elevatorResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_elevator_0",
            targetNodeId = "univ_elevator_2",
            wheelchairOnly = false
        )
        assertNotNull(elevatorResult.route)
        val elevRoute = elevatorResult.route!!
        assertTrue("Elevator route must contain elevator transition step", 
            elevRoute.steps.any { it.turnType == TurnType.ELEVATOR_UP })

        // Test Stairs path
        val stairsResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_stairs_0",
            targetNodeId = "univ_stairs_2",
            wheelchairOnly = false
        )
        assertNotNull(stairsResult.route)
        val stairsRoute = stairsResult.route!!
        assertTrue("Stairs route must contain stairs transition step",
            stairsRoute.steps.any { it.turnType == TurnType.STAIRS_UP })
    }

    /**
     * Test 5: Wheelchair / Accessibility Mode (Excludes Stairs, Mandates Elevators)
     */
    @Test
    fun test5_wheelchairAccessibilityMode() {
        val accessibleResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_gate",
            targetNodeId = "univ_cslab",
            wheelchairOnly = true,
            isEmergency = false
        )

        assertNotNull("Accessible path to AI Lab must exist", accessibleResult.route)
        val route = accessibleResult.route!!

        // Must NOT take stairs
        val usesStairs = route.steps.any { 
            it.turnType == TurnType.STAIRS_UP || it.turnType == TurnType.STAIRS_DOWN 
        }
        assertFalse("Wheelchair accessible route must strictly avoid stairs", usesStairs)

        // Must use elevator
        val usesElevator = route.steps.any { 
            it.turnType == TurnType.ELEVATOR_UP || it.turnType == TurnType.ELEVATOR_DOWN 
        }
        assertTrue("Wheelchair accessible multi-floor route must use elevator", usesElevator)

        // All nodes along path must be accessible
        val allNodesAccessible = route.pathNodes.all { it.isAccessible }
        assertTrue("All traversed nodes in wheelchair mode must be marked accessible", allNodesAccessible)
    }

    /**
     * Test 6: Off-Route Detection & Recalculation
     */
    @Test
    fun test6_offRouteDetectionAndRecalculation() {
        // Initial route to AI Lab
        val initialResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_gate",
            targetNodeId = "univ_cslab"
        )
        val initialRoute = initialResult.route!!
        assertEquals("univ_gate", initialRoute.pathNodes.first().id)

        // User deviated to Cafeteria (univ_cafeteria) which is off the direct gate->reception->elevator path
        val offRouteNodeId = "univ_cafeteria"
        val recalculatedResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = offRouteNodeId,
            targetNodeId = "univ_cslab"
        )

        assertNotNull("A* must successfully recalculate route from deviated location", recalculatedResult.route)
        val newRoute = recalculatedResult.route!!
        assertEquals("Recalculated route must start at current deviated location (Cafeteria)", "univ_cafeteria", newRoute.pathNodes.first().id)
        assertEquals("Recalculated route must still terminate at AI Laboratory", "univ_cslab", newRoute.pathNodes.last().id)
        assertTrue("Recalculated distance should be valid", newRoute.totalDistanceMeters > 0f)
    }

    /**
     * Test 7: Positioning Fallbacks & Sensor Fusion
     */
    @Test
    fun test7_positioningFallbacks() {
        val fusionEngine = IndoorPositionFusionEngine()
        
        // 1. Process QR calibration anchor point at univ_gate
        val gateNode = engineeringBuilding.nodes.first { it.id == "univ_gate" }
        val qrPos = fusionEngine.processQrCalibration(gateNode, engineeringBuilding)
        assertEquals("univ", qrPos.buildingId)
        assertEquals(0, qrPos.floorId)
        assertEquals(150f, qrPos.x, 0.01f)
        assertEquals(500f, qrPos.y, 0.01f)
        assertEquals(PositionSource.QR_CALIBRATION, qrPos.source)
        assertEquals(PositionConfidence.HIGH, qrPos.confidence)

        // 2. PDR step update
        val pdrPos = fusionEngine.processPdrStep(deltaX = 10f, deltaY = 0f, headingDeg = 0f, building = engineeringBuilding)
        assertNotNull("PDR step should update position", pdrPos)
        assertEquals(PositionSource.PDR_SENSOR, pdrPos!!.source)
        assertTrue("X coordinate should have incremented with forward step", pdrPos.x > 150f)

        // 3. Ble position measurement fusion
        val bleMeasurement = IndoorPosition(
            buildingId = "univ",
            floorId = 0,
            x = 180f,
            y = 500f,
            confidence = PositionConfidence.MEDIUM,
            confidenceScore = 0.8f,
            source = PositionSource.BLE_BEACON
        )
        val fusedBle = fusionEngine.processBleMeasurement(bleMeasurement, engineeringBuilding)
        assertNotNull("BLE measurement should be fused", fusedBle)
        assertEquals("univ", fusedBle!!.buildingId)
        assertEquals(0, fusedBle.floorId)
    }

    /**
     * Test 8: AI Assistant Grounding & Intent Resolution
     */
    @Test
    fun test8_aiAssistantIntentResolution() = runBlocking {
        val aiService = GeminiNavService()

        // "Where is the AI Lab?" query
        val aiLabIntent = aiService.resolveUserIntent("Where is the AI Lab?", engineeringBuilding)
        assertNotNull("AI Service must resolve AI Lab intent", aiLabIntent)
        assertEquals("Intent should target AI Lab node", "univ_cslab", aiLabIntent.targetNodeId)
        assertTrue("Confidence should be high for exact alias match", aiLabIntent.confidence >= 0.7f)

        // "Where is the nearest restroom?" query
        val restroomIntent = aiService.resolveUserIntent("Where is the nearest restroom?", engineeringBuilding)
        assertNotNull("AI Service must resolve Restroom intent", restroomIntent)
        assertTrue("Target node must be a restroom", restroomIntent.targetNodeId?.contains("restroom") == true)

        // "Take me to the library" query
        val libraryIntent = aiService.resolveUserIntent("Take me to the library", engineeringBuilding)
        assertNotNull(libraryIntent)
        assertEquals("univ_library", libraryIntent.targetNodeId)
    }

    /**
     * Test 9: Admin -> User Synchronization
     */
    @Test
    fun test9_adminToUserSync() {
        val newNode = NavNode(
            id = "univ_vr_lab",
            buildingId = "univ",
            floor = 2,
            x = 450f,
            y = 300f,
            name = "Virtual Reality Studio",
            code = "VR-208",
            category = NodeCategory.LABORATORY,
            description = "Immersive VR and spatial computing lab",
            keywords = listOf("vr studio", "vr lab", "virtual reality"),
            isAccessible = true
        )

        // Admin adds new location to Engineering Block
        repository.createNode("univ", newNode)

        // Verify updated building immediately reflects new node
        val updatedBuilding = repository.getBuildingById("univ")
        assertNotNull(updatedBuilding)
        val foundNode = updatedBuilding?.nodes?.find { it.id == "univ_vr_lab" }
        assertNotNull("New node must be present in building nodes list", foundNode)

        // User searches for the new location
        val searchResults = repository.searchLocations("Virtual Reality", buildingId = "univ")
        assertTrue("User search must find newly published Admin node", searchResults.any { it.id == "univ_vr_lab" })

        // Routing to the newly added location works
        val newEdge = NavEdge("univ_elevator_2", "univ_vr_lab", 15f, EdgeType.CORRIDOR, true)
        repository.createEdge("univ", newEdge)

        val routeResult = AStarRouter.findPath(
            building = repository.getBuildingById("univ")!!,
            startNodeId = "univ_gate",
            targetNodeId = "univ_vr_lab"
        )
        assertNotNull("Routing to new Admin node must succeed", routeResult.route)
        assertEquals("univ_vr_lab", routeResult.route?.pathNodes?.last()?.id)
    }

    /**
     * Test 10: Graceful Failure Handling
     */
    @Test
    fun test10_gracefulFailureHandling() {
        // Disconnected or invalid target
        val invalidResult = AStarRouter.findPath(
            building = engineeringBuilding,
            startNodeId = "univ_gate",
            targetNodeId = "non_existent_node_xyz"
        )
        assertNull("Path to non-existent node must be null", invalidResult.route)
        assertNotNull("Must provide error message without crashing", invalidResult.error)

        // Search with empty or non-matching query returns empty list, does not crash
        val emptyResults = repository.searchLocations("xyzabc987654321nonexistent", buildingId = "univ")
        assertTrue("Non-matching search should yield empty list", emptyResults.isEmpty())
    }
}
