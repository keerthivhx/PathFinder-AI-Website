package com.example

import com.example.data.model.*
import com.example.data.network.dto.*
import com.example.data.network.mapper.NetworkModelMappers.toDomain
import com.example.data.network.mapper.NetworkModelMappers.toNavNode
import com.example.routing.AStarRouter
import org.junit.Assert.*
import org.junit.Test

class NavigationBackendIntegrationTest {

    @Test
    fun testDtoMappingAndDomainConversion() {
        val bDto = BuildingDto(
            buildingId = "test_build",
            name = "Science Complex",
            type = "University",
            description = "Science and Tech Labs",
            address = "400 Campus Ave",
            totalFloors = 2
        )

        val fDtos = listOf(
            FloorDto(buildingId = "test_build", floorNumber = 0, name = "Level 0", shortName = "G"),
            FloorDto(buildingId = "test_build", floorNumber = 1, name = "Level 1", shortName = "L1")
        )

        val nDtos = listOf(
            NavNodeDto(
                nodeId = "n_entry",
                buildingId = "test_build",
                floor = 0,
                x = 100f,
                y = 100f,
                name = "Main Entrance",
                code = "ENTR-01",
                category = "ENTRANCE"
            ),
            NavNodeDto(
                nodeId = "n_hall_0",
                buildingId = "test_build",
                floor = 0,
                x = 200f,
                y = 100f,
                name = "Ground Corridor",
                code = "HALL-01",
                category = "GENERAL"
            ),
            NavNodeDto(
                nodeId = "n_elevator_0",
                buildingId = "test_build",
                floor = 0,
                x = 300f,
                y = 100f,
                name = "Central Elevator G",
                code = "ELEV-0",
                category = "ELEVATOR"
            ),
            NavNodeDto(
                nodeId = "n_elevator_1",
                buildingId = "test_build",
                floor = 1,
                x = 300f,
                y = 100f,
                name = "Central Elevator L1",
                code = "ELEV-1",
                category = "ELEVATOR"
            ),
            NavNodeDto(
                nodeId = "n_lab_1",
                buildingId = "test_build",
                floor = 1,
                x = 450f,
                y = 100f,
                name = "Robotics Lab",
                code = "LAB-102",
                category = "LABORATORY"
            )
        )

        val eDtos = listOf(
            NavEdgeDto(fromId = "n_entry", toId = "n_hall_0", distanceMeters = 15f, edgeType = "CORRIDOR"),
            NavEdgeDto(fromId = "n_hall_0", toId = "n_elevator_0", distanceMeters = 12f, edgeType = "CORRIDOR"),
            NavEdgeDto(fromId = "n_elevator_0", toId = "n_elevator_1", distanceMeters = 8f, edgeType = "ELEVATOR"),
            NavEdgeDto(fromId = "n_elevator_1", toId = "n_lab_1", distanceMeters = 20f, edgeType = "CORRIDOR")
        )

        val floors = fDtos.map { it.toDomain() }
        val nodes = nDtos.map { it.toDomain() }
        val edges = eDtos.map { it.toDomain() }
        val building = bDto.toDomain(floors, nodes, edges)

        assertEquals("test_build", building.id)
        assertEquals(2, building.floors.size)
        assertEquals(5, building.nodes.size)
        assertEquals(4, building.edges.size)

        // Test A* Route Calculation from Entrance (Floor 0) to Robotics Lab (Floor 1)
        val routeResult = AStarRouter.findPath(
            building = building,
            startNodeId = "n_entry",
            targetNodeId = "n_lab_1",
            wheelchairOnly = true
        )

        assertNotNull("Route should be calculated successfully", routeResult.route)
        val route = routeResult.route!!

        assertEquals(5, route.pathNodes.size)
        assertEquals("n_entry", route.pathNodes.first().id)
        assertEquals("n_lab_1", route.pathNodes.last().id)
        assertEquals(55f, route.totalDistanceMeters, 0.1f)
        val routeFloors = route.pathNodes.map { it.floor }.distinct()
        assertTrue("Route spans across 2 floors", routeFloors.size == 2)
        assertTrue("Route has clear navigational instructions", route.steps.isNotEmpty())
    }

    @Test
    fun testEngineeringBlockEndToEndNavigationFlow() {
        val repo = com.example.data.repository.BuildingRepository()
        val univ = repo.getBuildingById("univ")
        assertNotNull("Engineering Block must be available in repository", univ)

        // Step 1: Validate Graph
        val validation = AStarRouter.validateGraph(univ!!)
        assertTrue("Engineering Block graph must be valid", validation.isValid)
        assertTrue("Engineering Block must have at least 15 nodes", validation.validNodeCount >= 15)
        assertTrue("Engineering Block must have at least 15 edges", validation.validEdgeCount >= 15)

        // Step 2: Search Destination - "AI Lab" / "Room 204"
        val searchResults = repo.searchLocations("AI Lab", buildingId = "univ")
        assertTrue("Search for 'AI Lab' must return the node", searchResults.isNotEmpty())
        val aiLabNode = searchResults.firstOrNull { it.id == "univ_cslab" }
        assertNotNull("AI Lab node (univ_cslab) must exist", aiLabNode)

        // Step 3: Select Start Location as Main Entrance (univ_gate)
        val startNode = univ.nodes.firstOrNull { it.id == "univ_gate" }
        assertNotNull("Main Entrance node must exist", startNode)

        // Step 4: Calculate Route with standard walking speed (1.4 m/s)
        val routeResult = AStarRouter.findPath(
            building = univ,
            startNodeId = startNode!!.id,
            targetNodeId = aiLabNode!!.id,
            wheelchairOnly = false,
            walkingSpeedMetersPerSec = 1.4f
        )
        assertNotNull("Route calculation to AI Lab must succeed", routeResult.route)
        val route = routeResult.route!!
        assertEquals("Start must be Main Entrance", "univ_gate", route.pathNodes.first().id)
        assertEquals("Target must be AI Lab", "univ_cslab", route.pathNodes.last().id)
        val univFloors = route.pathNodes.map { it.floor }.distinct()
        assertTrue("Route must traverse Floor 0 and Floor 2", univFloors.containsAll(listOf(0, 2)))
        assertTrue("Steps must contain turn-by-turn instructions", route.steps.size >= 3)
        assertTrue("Estimated time must be positive", route.estimatedTimeSeconds > 0)

        // Step 5: Test Configurable Walking Speed (e.g. 1.0 m/s vs 2.0 m/s)
        val slowRoute = AStarRouter.findPath(univ, startNode.id, aiLabNode.id, walkingSpeedMetersPerSec = 1.0f)
        val fastRoute = AStarRouter.findPath(univ, startNode.id, aiLabNode.id, walkingSpeedMetersPerSec = 2.0f)
        assertNotNull(slowRoute.route)
        assertNotNull(fastRoute.route)
        assertTrue("Slower speed must produce larger estimated duration", slowRoute.route!!.estimatedTimeSeconds > fastRoute.route!!.estimatedTimeSeconds)

        // Step 6: Test Routing to other key destinations
        val targets = listOf("univ_library", "univ_cafeteria", "univ_placement", "univ_restroom_0", "univ_fire_exit_0")
        for (targetId in targets) {
            val res = AStarRouter.findPath(univ, startNode.id, targetId)
            assertNotNull("Route to $targetId must succeed", res.route)
            assertEquals("Destination must match $targetId", targetId, res.route!!.pathNodes.last().id)
        }

        // Step 7: Test Emergency Evacuation
        val evacResult = AStarRouter.findEmergencyEvacuationPath(univ, "univ_cslab")
        assertNotNull("Emergency evacuation route must succeed", evacResult.route)
        assertTrue("Evacuation route must end at an emergency exit", evacResult.route!!.pathNodes.last().isEmergencyExit)
    }

    @Test
    fun testLocationSearchDtoMapping() {
        val locationDto = LocationDto(
            locationId = "loc_99",
            nodeId = "node_99",
            buildingId = "test_build",
            floorNumber = 1,
            name = "Microbiology Lab",
            code = "MBI-105",
            category = "LABORATORY",
            coordinates = CoordinatesDto(x = 550f, y = 300f)
        )

        val node = locationDto.toNavNode()
        assertEquals("node_99", node.id)
        assertEquals(NodeCategory.LABORATORY, node.category)
        assertEquals(550f, node.x, 0.01f)
        assertEquals(300f, node.y, 0.01f)
    }
}
