package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiNavService
import com.example.data.model.EdgeType
import com.example.data.model.NodeCategory
import com.example.data.repository.BuildingRepository
import com.example.routing.AStarRouter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PathFinder AI", appName)
    }

    @Test
    fun testBuildingRepositoryInitialization() {
        val repo = BuildingRepository()
        val buildings = repo.buildings.value
        assertTrue("Buildings list should not be empty", buildings.isNotEmpty())

        val hosp = repo.getBuildingById("hosp")
        assertNotNull("Hospital building should exist", hosp)
        assertEquals("Metropolitan City Hospital", hosp?.name)
        assertTrue("Hospital should have multiple floors", (hosp?.floors?.size ?: 0) >= 3)
    }

    @Test
    fun testAStarPathfindingHospitalGroundToRadiology() {
        val repo = BuildingRepository()
        val hosp = repo.getBuildingById("hosp")!!

        // Find path from Main Entrance (Level 0) to Radiology (Level 1)
        val result = AStarRouter.findPath(
            building = hosp,
            startNodeId = "hosp_entrance",
            targetNodeId = "hosp_radiology",
            wheelchairOnly = false,
            isEmergency = false
        )

        assertNotNull("Path should be found", result.route)
        val route = result.route!!
        assertTrue("Route must contain steps", route.steps.isNotEmpty())
        assertEquals("First node should be entrance", "hosp_entrance", route.pathNodes.first().id)
        assertEquals("Last node should be radiology", "hosp_radiology", route.pathNodes.last().id)
        assertTrue("Total distance should be positive", route.totalDistanceMeters > 0f)
    }

    @Test
    fun testWheelchairAccessibilityModeAvoidsStairs() {
        val repo = BuildingRepository()
        val hosp = repo.getBuildingById("hosp")!!

        // Find path from Ground Floor to Level 1 with Wheelchair Mode enabled
        val result = AStarRouter.findPath(
            building = hosp,
            startNodeId = "hosp_reception",
            targetNodeId = "hosp_surgery",
            wheelchairOnly = true,
            isEmergency = false
        )

        assertNotNull("Accessible path should be found", result.route)
        val route = result.route!!

        // Must NOT use stairs
        val usesStairs = route.steps.any { it.turnType == com.example.data.model.TurnType.STAIRS_UP || it.turnType == com.example.data.model.TurnType.STAIRS_DOWN }
        assertFalse("Wheelchair path must NOT use stairs", usesStairs)

        // Must use elevator
        val usesElevator = route.steps.any { it.turnType == com.example.data.model.TurnType.ELEVATOR_UP || it.turnType == com.example.data.model.TurnType.ELEVATOR_DOWN }
        assertTrue("Wheelchair path should use elevator for multi-floor transition", usesElevator)
    }

    @Test
    fun testEmergencyEvacuationNearestFireExit() {
        val repo = BuildingRepository()
        val hosp = repo.getBuildingById("hosp")!!

        val result = AStarRouter.findEmergencyEvacuationPath(
            building = hosp,
            startNodeId = "hosp_triage_er",
            wheelchairOnly = false
        )

        assertNotNull("Emergency evacuation route should be found", result.route)
        val route = result.route!!
        assertTrue("Route must be flagged as emergency", route.isEmergencyRoute)
        val exitNode = route.pathNodes.last()
        assertTrue("Destination must be an emergency exit", exitNode.isEmergencyExit || exitNode.category == NodeCategory.EMERGENCY_EXIT)
    }

    @Test
    fun testGeminiIntentLocalFallback() = runBlocking {
        val repo = BuildingRepository()
        val hosp = repo.getBuildingById("hosp")!!
        val service = GeminiNavService()

        val intent = service.resolveUserIntent("my head hurts and fever", hosp)
        assertNotNull("Intent result should not be null", intent)
        assertEquals("hosp_triage_er", intent.targetNodeId)
        assertTrue("Confidence should be high for emergency", intent.confidence >= 0.7f)
    }
}

