package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.data.network.ApiClient
import com.example.data.network.api.AdminApi
import com.example.data.network.api.BuildingApi
import com.example.data.network.dto.*
import com.example.data.network.mapper.NetworkModelMappers.toDomain
import com.example.data.network.mapper.NetworkModelMappers.toDto
import com.example.data.network.mapper.NetworkModelMappers.toLocationDto
import com.example.routing.AStarRouter
import com.example.routing.GraphValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildingRepository(
    private val context: Context? = null,
    private val buildingApi: BuildingApi = ApiClient.buildingApi,
    private val adminApi: AdminApi = ApiClient.adminApi
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("pathfinder_building_cache", Context.MODE_PRIVATE)
    }

    private val _buildings = MutableStateFlow<List<Building>>(loadCachedOrInitialBuildings())
    val buildings: StateFlow<List<Building>> = _buildings.asStateFlow()

    private val _beacons = MutableStateFlow<List<Beacon>>(loadCachedOrInitialBeacons())
    val beacons: StateFlow<List<Beacon>> = _beacons.asStateFlow()

    private val _qrPoints = MutableStateFlow<List<QrCalibrationPoint>>(loadCachedOrInitialQrPoints())
    val qrPoints: StateFlow<List<QrCalibrationPoint>> = _qrPoints.asStateFlow()

    private val _syncState = MutableStateFlow<NetworkSyncState>(NetworkSyncState.Idle)
    val syncState: StateFlow<NetworkSyncState> = _syncState.asStateFlow()

    private fun loadCachedOrInitialBeacons(): List<Beacon> {
        return getInitialBeacons()
    }

    private fun loadCachedOrInitialQrPoints(): List<QrCalibrationPoint> {
        return listOf(
            QrCalibrationPoint("QR-UNIV-GF", "univ", 0, "univ_gate", 150f, 500f),
            QrCalibrationPoint("QR-UNIV-F1", "univ", 1, "univ_elevator_1", 500f, 500f),
            QrCalibrationPoint("QR-UNIV-F2", "univ", 2, "univ_elevator_2", 500f, 500f),
            QrCalibrationPoint("QR-HOSP-GF", "hosp", 0, "hosp_entrance", 150f, 500f),
            QrCalibrationPoint("QR-AIR-GF", "air", 0, "air_dropoff", 100f, 500f)
        )
    }

    fun getBeaconsForBuilding(buildingId: String): List<Beacon> {
        return _beacons.value.filter { it.buildingId == buildingId && it.activeStatus }
    }

    fun getQrPointsForBuilding(buildingId: String): List<QrCalibrationPoint> {
        return _qrPoints.value.filter { it.buildingId == buildingId && it.active }
    }

    // -------------------------------------------------------------
    // REAL ADMIN MUTATION & REST API PERSISTENCE (PHASE 9)
    // -------------------------------------------------------------

    /**
     * Creates a new Building, updates local graph, increments graph version, and persists to backend MongoDB.
     */
    fun createBuilding(building: Building) {
        val versioned = building.copy(
            graphVersion = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _buildings.value = _buildings.value + versioned
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.createBuilding(versioned.toDto())
                Log.d("BuildingRepository", "Building ${versioned.name} successfully created on backend.")
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend persistence notice for createBuilding: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing building's metadata.
     */
    fun updateBuilding(updated: Building) {
        val versioned = updated.copy(
            graphVersion = updated.graphVersion + 1,
            updatedAt = System.currentTimeMillis()
        )
        val current = _buildings.value.toMutableList()
        val index = current.indexOfFirst { it.id == versioned.id }
        if (index >= 0) {
            current[index] = versioned
        } else {
            current.add(versioned)
        }
        _buildings.value = current
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.updateBuilding(versioned.id, versioned.toDto())
                Log.d("BuildingRepository", "Building ${versioned.name} updated on backend.")
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for updateBuilding: ${e.message}")
            }
        }
    }

    /**
     * Checks building dependencies before deleting.
     */
    fun checkBuildingDependencies(buildingId: String): Int {
        val b = getBuildingById(buildingId) ?: return 0
        val beaconCount = _beacons.value.count { it.buildingId == buildingId }
        val qrCount = _qrPoints.value.count { it.buildingId == buildingId }
        return b.floors.size + b.nodes.size + b.edges.size + beaconCount + qrCount
    }

    /**
     * Safely deletes a building with optional cascading cleanup of associated beacons and QR calibration points.
     */
    fun deleteBuilding(buildingId: String, cascade: Boolean = false): DeleteResult {
        val b = getBuildingById(buildingId) ?: return DeleteResult.Error("Building not found")
        val depCount = checkBuildingDependencies(buildingId)
        if (depCount > 0 && !cascade) {
            return DeleteResult.HasDependencies(
                message = "Building '${b.name}' contains ${b.floors.size} floor(s), ${b.nodes.size} node(s), ${b.edges.size} edge(s), and associated beacons/QR points.",
                dependencyCount = depCount,
                dependencyType = "Building Data & Hardware Anchors"
            )
        }

        _buildings.value = _buildings.value.filterNot { it.id == buildingId }
        if (cascade) {
            _beacons.value = _beacons.value.filterNot { it.buildingId == buildingId }
            _qrPoints.value = _qrPoints.value.filterNot { it.buildingId == buildingId }
        }
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.deleteBuilding(buildingId)
                Log.d("BuildingRepository", "Building $buildingId deleted from backend.")
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteBuilding: ${e.message}")
            }
        }
        return DeleteResult.Success
    }

    /**
     * Adds a new floor to a building.
     */
    fun createFloor(buildingId: String, floor: FloorInfo) {
        val b = getBuildingById(buildingId) ?: return
        if (b.floors.none { it.floorNumber == floor.floorNumber }) {
            val updatedFloors = (b.floors + floor.copy(buildingId = buildingId)).sortedBy { it.floorNumber }
            updateBuilding(b.copy(floors = updatedFloors))

            scope.launch {
                try {
                    adminApi.createFloor(buildingId, floor.toDto(buildingId))
                } catch (e: Exception) {
                    Log.w("BuildingRepository", "Backend notice for createFloor: ${e.message}")
                }
            }
        }
    }

    /**
     * Updates floor metadata.
     */
    fun updateFloor(buildingId: String, floor: FloorInfo) {
        val b = getBuildingById(buildingId) ?: return
        val updatedFloors = b.floors.map {
            if (it.floorNumber == floor.floorNumber) floor.copy(buildingId = buildingId) else it
        }
        updateBuilding(b.copy(floors = updatedFloors))

        scope.launch {
            try {
                val floorId = "${buildingId}_fl_${floor.floorNumber}"
                adminApi.updateFloor(floorId, floor.toDto(buildingId))
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for updateFloor: ${e.message}")
            }
        }
    }

    /**
     * Checks if a floor has dependent nodes or hardware anchors before deletion.
     */
    fun checkFloorDependencies(buildingId: String, floorNumber: Int): Int {
        val b = getBuildingById(buildingId) ?: return 0
        val nodeCount = b.nodes.count { it.floor == floorNumber }
        val beaconCount = _beacons.value.count { it.buildingId == buildingId && it.floor == floorNumber }
        val qrCount = _qrPoints.value.count { it.buildingId == buildingId && it.floorId == floorNumber }
        return nodeCount + beaconCount + qrCount
    }

    /**
     * Deletes a floor from a building with safety dependency checks.
     */
    fun deleteFloor(buildingId: String, floorNumber: Int, cascade: Boolean = false): DeleteResult {
        val b = getBuildingById(buildingId) ?: return DeleteResult.Error("Building not found")
        val floor = b.floors.firstOrNull { it.floorNumber == floorNumber } ?: return DeleteResult.Error("Floor not found")
        val depCount = checkFloorDependencies(buildingId, floorNumber)

        if (depCount > 0 && !cascade) {
            return DeleteResult.HasDependencies(
                message = "Floor '${floor.name}' has $depCount dependent item(s) (nodes, beacons, QR points).",
                dependencyCount = depCount,
                dependencyType = "Floor Content"
            )
        }

        val updatedFloors = b.floors.filterNot { it.floorNumber == floorNumber }
        var updatedNodes = b.nodes
        var updatedEdges = b.edges

        if (cascade) {
            val floorNodeIds = b.nodes.filter { it.floor == floorNumber }.map { it.id }.toSet()
            updatedNodes = b.nodes.filterNot { it.floor == floorNumber }
            updatedEdges = b.edges.filterNot { it.fromId in floorNodeIds || it.toId in floorNodeIds }
            _beacons.value = _beacons.value.filterNot { it.buildingId == buildingId && it.floor == floorNumber }
            _qrPoints.value = _qrPoints.value.filterNot { it.buildingId == buildingId && it.floorId == floorNumber }
        }

        updateBuilding(b.copy(floors = updatedFloors, nodes = updatedNodes, edges = updatedEdges))

        scope.launch {
            try {
                val floorId = "${buildingId}_fl_${floorNumber}"
                adminApi.deleteFloor(floorId)
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteFloor: ${e.message}")
            }
        }
        return DeleteResult.Success
    }

    /**
     * Creates or places a navigation node / location on the floor map.
     */
    fun createNode(buildingId: String, node: NavNode) {
        val b = getBuildingById(buildingId) ?: return
        val updatedNode = node.copy(buildingId = buildingId)
        val updatedNodes = b.nodes.filterNot { it.id == node.id } + updatedNode
        updateBuilding(b.copy(nodes = updatedNodes))

        scope.launch {
            try {
                adminApi.createNode(updatedNode.toDto())
                adminApi.createLocation(updatedNode.toLocationDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for createNode: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing node.
     */
    fun updateNode(buildingId: String, node: NavNode) {
        val b = getBuildingById(buildingId) ?: return
        val updatedNode = node.copy(buildingId = buildingId, updatedAt = System.currentTimeMillis())
        val updatedNodes = b.nodes.map { if (it.id == node.id) updatedNode else it }
        updateBuilding(b.copy(nodes = updatedNodes))

        scope.launch {
            try {
                adminApi.updateNode(node.id, updatedNode.toDto())
                adminApi.updateLocation("loc_${node.id}", updatedNode.toLocationDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for updateNode: ${e.message}")
            }
        }
    }

    /**
     * Checks if a node has connected edges or anchors before deletion.
     */
    fun checkNodeDependencies(buildingId: String, nodeId: String): Int {
        val b = getBuildingById(buildingId) ?: return 0
        val edgeCount = b.edges.count { it.fromId == nodeId || it.toId == nodeId }
        val beaconCount = _beacons.value.count { it.nodeId == nodeId }
        val qrCount = _qrPoints.value.count { it.nodeId == nodeId }
        return edgeCount + beaconCount + qrCount
    }

    /**
     * Deletes a node and cleans up connected edges.
     */
    fun deleteNode(buildingId: String, nodeId: String, cascade: Boolean = true): DeleteResult {
        val b = getBuildingById(buildingId) ?: return DeleteResult.Error("Building not found")
        val node = b.nodes.firstOrNull { it.id == nodeId } ?: return DeleteResult.Error("Node not found")
        val depCount = checkNodeDependencies(buildingId, nodeId)

        if (depCount > 0 && !cascade) {
            return DeleteResult.HasDependencies(
                message = "Node '${node.name}' has $depCount connection(s) or hardware anchors.",
                dependencyCount = depCount,
                dependencyType = "Edges & Anchors"
            )
        }

        val updatedNodes = b.nodes.filterNot { it.id == nodeId }
        val updatedEdges = b.edges.filterNot { it.fromId == nodeId || it.toId == nodeId }
        updateBuilding(b.copy(nodes = updatedNodes, edges = updatedEdges))

        scope.launch {
            try {
                adminApi.deleteNode(nodeId)
                adminApi.deleteLocation("loc_$nodeId")
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteNode: ${e.message}")
            }
        }
        return DeleteResult.Success
    }

    /**
     * Creates an edge connection between two nodes.
     */
    fun createEdge(buildingId: String, edge: NavEdge) {
        val b = getBuildingById(buildingId) ?: return
        val updatedEdges = b.edges.filterNot {
            (it.fromId == edge.fromId && it.toId == edge.toId) ||
            (it.fromId == edge.toId && it.toId == edge.fromId)
        } + edge
        updateBuilding(b.copy(edges = updatedEdges))

        scope.launch {
            try {
                adminApi.createEdge(edge.toDto(buildingId))
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for createEdge: ${e.message}")
            }
        }
    }

    /**
     * Updates an edge connection.
     */
    fun updateEdge(buildingId: String, edge: NavEdge) {
        createEdge(buildingId, edge)
    }

    /**
     * Deletes an edge connection.
     */
    fun deleteEdge(buildingId: String, fromId: String, toId: String) {
        val b = getBuildingById(buildingId) ?: return
        val updatedEdges = b.edges.filterNot {
            (it.fromId == fromId && it.toId == toId) ||
            (it.fromId == toId && it.toId == fromId)
        }
        updateBuilding(b.copy(edges = updatedEdges))

        scope.launch {
            try {
                val edgeId = "edge_${fromId}_${toId}"
                adminApi.deleteEdge(edgeId)
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteEdge: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // BEACONS & QR CALIBRATION POINTS CRUD (PHASE 9)
    // -------------------------------------------------------------

    fun createBeacon(beacon: Beacon) {
        val current = _beacons.value.filterNot { it.beaconId == beacon.beaconId }
        _beacons.value = current + beacon
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.createBeacon(beacon.toDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for createBeacon: ${e.message}")
            }
        }
    }

    fun updateBeacon(beacon: Beacon) {
        val current = _beacons.value.map { if (it.beaconId == beacon.beaconId) beacon else it }
        _beacons.value = current
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.updateBeacon(beacon.beaconId, beacon.toDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for updateBeacon: ${e.message}")
            }
        }
    }

    fun deleteBeacon(beaconId: String) {
        _beacons.value = _beacons.value.filterNot { it.beaconId == beaconId }
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.deleteBeacon(beaconId)
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteBeacon: ${e.message}")
            }
        }
    }

    fun createQrPoint(qrPoint: QrCalibrationPoint) {
        val current = _qrPoints.value.filterNot { it.calibrationId == qrPoint.calibrationId }
        _qrPoints.value = current + qrPoint
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.createQrPoint(qrPoint.toDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for createQrPoint: ${e.message}")
            }
        }
    }

    fun updateQrPoint(qrPoint: QrCalibrationPoint) {
        val current = _qrPoints.value.map { if (it.calibrationId == qrPoint.calibrationId) qrPoint else it }
        _qrPoints.value = current
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.updateQrPoint(qrPoint.calibrationId, qrPoint.toDto())
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for updateQrPoint: ${e.message}")
            }
        }
    }

    fun deleteQrPoint(calibrationId: String) {
        _qrPoints.value = _qrPoints.value.filterNot { it.calibrationId == calibrationId }
        saveToDiskCache()

        scope.launch {
            try {
                adminApi.deleteQrPoint(calibrationId)
            } catch (e: Exception) {
                Log.w("BuildingRepository", "Backend notice for deleteQrPoint: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // GRAPH VALIDATION & PUBLISH / SYNC (PHASE 9)
    // -------------------------------------------------------------

    /**
     * Validates a building's navigation graph using complete structural and accessibility rules.
     */
    fun validateBuildingGraph(buildingId: String): GraphValidationReport {
        val b = getBuildingById(buildingId) ?: return GraphValidationReport(
            isValid = false,
            buildingId = buildingId,
            buildingName = "Unknown",
            totalNodes = 0,
            totalEdges = 0,
            totalFloors = 0,
            accessibleEdgesCount = 0,
            elevatorTransitionsCount = 0,
            stairTransitionsCount = 0,
            rampTransitionsCount = 0,
            errors = listOf("Building '$buildingId' not found.")
        )
        return GraphValidator.validateGraph(b)
    }

    /**
     * Validates and publishes the building graph to backend MongoDB, updating version and synchronizing.
     */
    suspend fun publishAndSynchronizeBuilding(buildingId: String): Result<GraphValidationReport> = withContext(Dispatchers.IO) {
        val b = getBuildingById(buildingId) ?: return@withContext Result.failure(IllegalArgumentException("Building not found"))
        val report = GraphValidator.validateGraph(b)
        if (!report.isValid) {
            return@withContext Result.failure(IllegalStateException("Graph contains ${report.errors.size} blocking error(s)"))
        }

        // Increment graph version and save
        val published = b.copy(
            graphVersion = b.graphVersion + 1,
            updatedAt = System.currentTimeMillis()
        )
        updateBuilding(published)

        try {
            adminApi.updateBuilding(published.id, published.toDto())
            adminApi.validateGraph(published.id)
        } catch (e: Exception) {
            Log.w("BuildingRepository", "Backend sync notice during publish: ${e.message}")
        }
        Result.success(report)
    }

    // -------------------------------------------------------------
    // LOCAL CACHE & SYNC
    // -------------------------------------------------------------

    fun saveToDiskCache() {
        try {
            val rawDtos = _buildings.value.map { b ->
                CachedBuildingDto(
                    building = b.toDto(),
                    floors = b.floors.map { it.toDto(b.id) },
                    nodes = b.nodes.map { it.toDto() },
                    edges = b.edges.map { it.toDto(b.id) },
                    beacons = _beacons.value.filter { it.buildingId == b.id }.map { it.toDto() },
                    qrPoints = _qrPoints.value.filter { it.buildingId == b.id }.map { it.toDto() }
                )
            }
            saveBuildingsToCache(_buildings.value, rawDtos)
        } catch (e: Exception) {
            Log.w("BuildingRepository", "Could not save disk cache: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // BACKWARD COMPATIBILITY HELPERS
    // -------------------------------------------------------------
    fun addBuilding(building: Building) = createBuilding(building)
    fun addCustomNode(buildingId: String, node: NavNode) = createNode(buildingId, node)
    fun addCustomEdge(buildingId: String, edge: NavEdge) = createEdge(buildingId, edge)
    fun addCustomFloor(buildingId: String, floor: FloorInfo) = createFloor(buildingId, floor)
    fun addCustomBeacon(beacon: Beacon) = createBeacon(beacon)
    fun updateCustomBeacon(beacon: Beacon) = updateBeacon(beacon)
    fun deleteCustomBeacon(beaconId: String) = deleteBeacon(beaconId)

    private fun getInitialBeacons(): List<Beacon> {
        return listOf(
            // University Engineering Block Beacons (Phase 7)
            Beacon("BEACON-GF-01", "univ", 0, "univ_gate", 150f, 500f, 101, 1, -59, true, "E2:C5:11:9B:01:A1", "Main Entrance BLE Anchor"),
            Beacon("BEACON-GF-02", "univ", 0, "univ_reception", 300f, 500f, 101, 2, -59, true, "E2:C5:11:9B:01:A2", "Reception & Information Foyer Beacon"),
            Beacon("BEACON-GF-03", "univ", 0, "univ_cafeteria", 300f, 250f, 101, 3, -59, true, "E2:C5:11:9B:01:A3", "Cafeteria Entrance Beacon"),
            Beacon("BEACON-GF-04", "univ", 0, "univ_seminar", 750f, 250f, 101, 4, -59, true, "E2:C5:11:9B:01:A4", "Seminar Hall Foyer Beacon"),
            Beacon("BEACON-GF-05", "univ", 0, "univ_elevator_0", 500f, 500f, 101, 5, -59, true, "E2:C5:11:9B:01:A5", "Ground Floor Elevator Lobby Beacon"),

            // Floor 1 Beacons
            Beacon("BEACON-F1-01", "univ", 1, "univ_elevator_1", 500f, 500f, 102, 1, -59, true, "E2:C5:11:9B:02:B1", "Floor 1 Elevator Lobby Beacon"),
            Beacon("BEACON-F1-02", "univ", 1, "univ_cs_dept", 300f, 300f, 102, 2, -59, true, "E2:C5:11:9B:02:B2", "Computer Science Dept Wing Beacon"),
            Beacon("BEACON-F1-03", "univ", 1, "univ_datascience_lab", 750f, 300f, 102, 3, -59, true, "E2:C5:11:9B:02:B3", "Data Science Laboratory Beacon"),

            // Floor 2 Beacons
            Beacon("BEACON-F2-01", "univ", 2, "univ_elevator_2", 500f, 500f, 103, 1, -59, true, "E2:C5:11:9B:03:C1", "Floor 2 Elevator Lobby Beacon"),
            Beacon("BEACON-F2-02", "univ", 2, "univ_cslab", 300f, 300f, 103, 2, -59, true, "E2:C5:11:9B:03:C2", "AI Laboratory Room 204 BLE Anchor"),
            Beacon("BEACON-F2-03", "univ", 2, "univ_library", 750f, 300f, 103, 3, -59, true, "E2:C5:11:9B:03:C3", "Central Library Entrance Beacon"),

            // Floor 3 Beacons
            Beacon("BEACON-F3-01", "univ", 3, "univ_elevator_3", 500f, 500f, 104, 1, -59, true, "E2:C5:11:9B:04:D1", "Floor 3 Penthouse Elevator Beacon"),
            Beacon("BEACON-F3-02", "univ", 3, "univ_robotics", 300f, 300f, 104, 2, -59, true, "E2:C5:11:9B:04:D2", "Robotics Research Arena Beacon"),

            // Hospital Beacons
            Beacon("BEACON-HOSP-01", "hosp", 0, "hosp_entrance", 150f, 500f, 201, 1, -59, true, "D1:A4:22:8C:01:A1", "Main Hospital Entrance Beacon"),
            Beacon("BEACON-HOSP-02", "hosp", 0, "hosp_reception", 300f, 500f, 201, 2, -59, true, "D1:A4:22:8C:01:A2", "Hospital Triage Reception Beacon"),
            Beacon("BEACON-HOSP-03", "hosp", 0, "hosp_elevator_g", 500f, 500f, 201, 3, -59, true, "D1:A4:22:8C:01:A3", "Hospital Ground Elevator Beacon"),

            // Airport Beacons
            Beacon("BEACON-AIR-01", "air", 0, "air_dropoff", 100f, 500f, 301, 1, -59, true, "C3:B2:44:7E:01:A1", "Terminal 3 Drop-Off Beacon"),
            Beacon("BEACON-AIR-02", "air", 0, "air_checkin", 250f, 500f, 301, 2, -59, true, "C3:B2:44:7E:01:A2", "Check-in Desks Beacon"),
            Beacon("BEACON-AIR-03", "air", 0, "air_security", 420f, 500f, 301, 3, -59, true, "C3:B2:44:7E:01:A3", "Security Checkpoint Beacon"),
            Beacon("BEACON-AIR-04", "air", 0, "air_gate_a1", 750f, 250f, 301, 4, -59, true, "C3:B2:44:7E:01:A4", "Gate A1 Concourse Beacon")
        )
    }

    private fun loadCachedOrInitialBuildings(): List<Building> {
        try {
            val json = prefs?.getString("cached_buildings_json", null)
            if (!json.isNullOrBlank()) {
                val adapter = ApiClient.moshi.adapter(CachedBuildingContainerDto::class.java)
                val container = adapter.fromJson(json)
                if (container != null && container.buildings.isNotEmpty()) {
                    val loaded = container.buildings.map { cb ->
                        val floors = cb.floors.map { it.toDomain() }
                        val nodes = cb.nodes.map { it.toDomain() }
                        val edges = cb.edges.map { it.toDomain() }
                        cb.building.toDomain(floors, nodes, edges)
                    }
                    if (loaded.isNotEmpty()) {
                        Log.d("BuildingRepository", "Loaded ${loaded.size} cached buildings from local offline storage.")
                        return loaded
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("BuildingRepository", "Failed to deserialize cached buildings: ${e.message}")
        }
        return getInitialBuildings()
    }

    private fun saveBuildingsToCache(buildingsList: List<Building>, rawDtos: List<CachedBuildingDto>) {
        try {
            val container = CachedBuildingContainerDto(
                buildings = rawDtos,
                lastCachedTimestamp = System.currentTimeMillis()
            )
            val adapter = ApiClient.moshi.adapter(CachedBuildingContainerDto::class.java)
            val json = adapter.toJson(container)
            prefs?.edit()?.putString("cached_buildings_json", json)?.apply()
            Log.d("BuildingRepository", "Successfully persisted ${rawDtos.size} buildings to local offline cache.")
        } catch (e: Exception) {
            Log.e("BuildingRepository", "Error saving buildings to local cache: ${e.message}", e)
        }
    }

    suspend fun syncWithBackend(buildingId: String? = null): Result<List<Building>> = withContext(Dispatchers.IO) {
        _syncState.value = NetworkSyncState.Loading
        try {
            val response = buildingApi.getBuildings()
            if (response.success && response.data != null && response.data.isNotEmpty()) {
                val buildingDtos = response.data
                val loadedBuildings = mutableListOf<Building>()
                val cachedDtos = mutableListOf<CachedBuildingDto>()

                for (bDto in buildingDtos) {
                    try {
                        // Fetch floors, nodes, and edges for each building
                        val floorsResp = buildingApi.getFloors(bDto.buildingId)
                        val nodesResp = buildingApi.getNodes(bDto.buildingId)
                        val edgesResp = buildingApi.getEdges(bDto.buildingId)

                        val rawFloors = floorsResp.data ?: emptyList()
                        val rawNodes = nodesResp.data ?: emptyList()
                        val rawEdges = edgesResp.data ?: emptyList()

                        val floors = rawFloors.map { it.toDomain() }
                        val nodes = rawNodes.map { it.toDomain() }
                        val edges = rawEdges.map { it.toDomain() }

                        val domainBuilding = bDto.toDomain(floors, nodes, edges)
                        
                        // Validate graph integrity
                        val validation = AStarRouter.validateGraph(domainBuilding)
                        if (!validation.isValid) {
                            Log.w("BuildingRepository", "Graph validation warnings for ${domainBuilding.name}: ${validation.errors}")
                        }

                        loadedBuildings.add(domainBuilding)
                        cachedDtos.add(CachedBuildingDto(bDto, rawFloors, rawNodes, rawEdges))
                    } catch (e: Exception) {
                        Log.w("BuildingRepository", "Failed loading sub-elements for building ${bDto.buildingId}: ${e.message}")
                    }
                }

                if (loadedBuildings.isNotEmpty()) {
                    // Update buildings with live backend graph
                    _buildings.value = loadedBuildings
                    saveBuildingsToCache(loadedBuildings, cachedDtos)

                    val totalNodes = loadedBuildings.sumOf { it.nodes.size }
                    _syncState.value = NetworkSyncState.Synced(
                        buildingCount = loadedBuildings.size,
                        nodeCount = totalNodes
                    )
                    return@withContext Result.success(loadedBuildings)
                }
            }
            // Fallback if backend returned empty or unsuccessful
            _syncState.value = NetworkSyncState.OfflineFallback(
                message = "Live backend returned empty data. Using high-precision local graph.",
                buildingCount = _buildings.value.size
            )
            Result.success(_buildings.value)
        } catch (e: Exception) {
            Log.e("BuildingRepository", "Backend sync error: ${e.message}", e)
            _syncState.value = NetworkSyncState.OfflineFallback(
                message = "Backend connection unavailable (${e.localizedMessage ?: "timeout"}). Using cached graph.",
                buildingCount = _buildings.value.size
            )
            Result.failure(e)
        }
    }

    fun searchLocations(
        query: String,
        buildingId: String? = null,
        category: NodeCategory? = null,
        accessibleOnly: Boolean = false
    ): List<NavNode> {
        val targetBuildings = if (buildingId != null) {
            _buildings.value.filter { it.id == buildingId }
        } else {
            _buildings.value
        }

        val allNodesWithBuilding = targetBuildings.flatMap { bld ->
            bld.nodes.map { node -> Pair(bld, node) }
        }

        val cleanQuery = query.trim().lowercase()

        return allNodesWithBuilding.filter { (bld, node) ->
            if (accessibleOnly && !node.isAccessible) {
                return@filter false
            }

            val matchesCategory = category == null || node.category == category
            if (!matchesCategory) return@filter false

            if (cleanQuery.isEmpty()) return@filter true

            // Floor search terms: "floor 2", "level 2", "f2", "l2", "ground"
            val matchesFloor = when {
                cleanQuery.contains("floor ${node.floor}") || cleanQuery.contains("level ${node.floor}") -> true
                cleanQuery.contains("f${node.floor}") || cleanQuery.contains("l${node.floor}") -> true
                (node.floor == 0 && (cleanQuery.contains("ground") || cleanQuery == "g")) -> true
                else -> false
            }

            // Category name matching: "laboratory", "labs", "library", "stairs", etc.
            val matchesCategoryName = node.category.name.lowercase().contains(cleanQuery) ||
                node.category.name.replace("_", " ").lowercase().contains(cleanQuery)

            // Building name matching
            val matchesBuilding = bld.name.contains(cleanQuery, ignoreCase = true)

            // Landmark matching
            val matchesLandmark = node.visualSignageHint.contains(cleanQuery, ignoreCase = true) ||
                bld.presetLandmarks.any { it.nodeId == node.id && (it.title.contains(cleanQuery, ignoreCase = true) || it.description.contains(cleanQuery, ignoreCase = true)) }

            matchesFloor ||
                matchesCategoryName ||
                matchesBuilding ||
                matchesLandmark ||
                node.name.contains(cleanQuery, ignoreCase = true) ||
                node.code.contains(cleanQuery, ignoreCase = true) ||
                node.keywords.any { it.contains(cleanQuery, ignoreCase = true) } ||
                node.description.contains(cleanQuery, ignoreCase = true)
        }.map { it.second }
    }

    fun getBuildingById(id: String): Building? {
        return _buildings.value.firstOrNull { it.id == id }
    }

    private fun getInitialBuildings(): List<Building> {
        return listOf(
            createHospitalBuilding(),
            createUniversityBuilding(),
            createAirportBuilding(),
            createMallBuilding()
        )
    }

    // -------------------------------------------------------------
    // 1. METROPOLITAN CITY HOSPITAL
    // -------------------------------------------------------------
    private fun createHospitalBuilding(): Building {
        val floors = listOf(
            FloorInfo(0, "Ground Floor (Triage & ER)", "G"),
            FloorInfo(1, "Floor 1 (Radiology & Diagnostics)", "L1"),
            FloorInfo(2, "Floor 2 (Surgery & Inpatient)", "L2")
        )

        val nodes = listOf(
            // Floor 0
            NavNode("hosp_entrance", "hosp", 0, 150f, 500f, "Main Hospital Entrance", "ENT-01", NodeCategory.ENTRANCE, "Pedestrian street entrance with automatic sliding doors", listOf("entrance", "main gate", "outside", "lobby", "in"), true, false, "Main Entrance Plaque", "door_front"),
            NavNode("hosp_reception", "hosp", 0, 300f, 500f, "Central Reception & Info", "REC-100", NodeCategory.RECEPTION, "Patient registration and enquiry desk", listOf("reception", "help", "information", "register", "admit", "desk", "check in"), true, false, "Information & Reception Desk", "desk"),
            NavNode("hosp_triage_er", "hosp", 0, 300f, 250f, "Emergency & Triage", "ER-101", NodeCategory.EMERGENCY, "24/7 Trauma, acute care, immediate treatment", listOf("emergency", "er", "triage", "accident", "trauma", "headache", "bleeding", "broken bone", "chest pain", "fever", "heart attack", "injury", "sick", "urgent", "pain"), true, false, "EMERGENCY & TRIAGE - ROOM 101", "emergency"),
            NavNode("hosp_pharmacy", "hosp", 0, 500f, 750f, "Main Pharmacy", "PH-102", NodeCategory.PHARMACY, "Prescription medication dispensary and surgical supplies", listOf("pharmacy", "medicine", "drug", "prescription", "pills", "syrup", "medical store", "chemist"), true, false, "24/7 Pharmacy Dispenser", "local_pharmacy"),
            NavNode("hosp_restroom_g", "hosp", 0, 700f, 750f, "Ground Restrooms", "WC-01", NodeCategory.RESTROOM, "Wheelchair accessible gender-neutral restrooms", listOf("restroom", "washroom", "toilet", "bathroom", "wc", "lavatory", "accessible"), true, false, "Accessible Restroom Sign", "wc"),
            NavNode("hosp_elevator_g", "hosp", 0, 500f, 500f, "Elevator Lobby A (Ground)", "ELV-G", NodeCategory.ELEVATOR, "Central passenger elevators to all specialty floors", listOf("elevator", "lift", "vertical", "up", "level 1", "level 2"), true, false, "Elevator Shaft A", "elevator"),
            NavNode("hosp_stairs_g", "hosp", 0, 700f, 500f, "Stairwell East (Ground)", "STR-G", NodeCategory.STAIRS, "Reinforced concrete stairwell to upper floors", listOf("stairs", "steps", "walk up"), false, false, "Stairs East", "stairs"),
            NavNode("hosp_exit_east", "hosp", 0, 900f, 250f, "Emergency Fire Exit East", "EXIT-E", NodeCategory.EMERGENCY_EXIT, "Direct fire door to exterior evacuation parking", listOf("fire exit", "evacuation", "emergency exit", "fire escape", "out"), true, true, "EMERGENCY FIRE EXIT", "exit_to_app"),

            // Floor 1
            NavNode("hosp_elevator_1", "hosp", 1, 500f, 500f, "Elevator Lobby A (Floor 1)", "ELV-1", NodeCategory.ELEVATOR, "Level 1 elevator landing with braille signage", listOf("elevator", "lift"), true, false, "Elevator Lobby Level 1", "elevator"),
            NavNode("hosp_stairs_1", "hosp", 1, 700f, 500f, "Stairwell East (Floor 1)", "STR-1", NodeCategory.STAIRS, "Level 1 stair landing", listOf("stairs", "steps"), false, false, "Stairs East Level 1", "stairs"),
            NavNode("hosp_radiology", "hosp", 1, 300f, 300f, "Radiology & CT Scan", "RAD-201", NodeCategory.RADIOLOGY, "MRI, X-Ray, CT Scan imaging suites", listOf("radiology", "xray", "x-ray", "ct scan", "mri", "ultrasound", "scan", "bone scan", "imaging", "chest xray"), true, false, "Radiology & Imaging Suite 201", "biotech"),
            NavNode("hosp_cardiology", "hosp", 1, 300f, 700f, "Cardiology & ECG Wing", "CARD-202", NodeCategory.CARDIOLOGY, "Heart specialist clinic, echocardiogram & stress testing", listOf("cardiology", "heart", "ecg", "cardiac", "chest", "pulse", "blood pressure", "cardiologist"), true, false, "Cardiology Dept Room 202", "favorite"),
            NavNode("hosp_blood_lab", "hosp", 1, 700f, 300f, "Pathology & Blood Lab", "LAB-203", NodeCategory.LABORATORY, "Blood tests, pathology, sample collection", listOf("lab", "blood test", "pathology", "sample", "urine test", "culture", "laboratory"), true, false, "Pathology Laboratory 203", "science"),
            NavNode("hosp_restroom_1", "hosp", 1, 850f, 700f, "Floor 1 Restrooms", "WC-02", NodeCategory.RESTROOM, "Accessible restrooms with baby changing station", listOf("toilet", "washroom", "restroom"), true, false, "Restrooms L1", "wc"),

            // Floor 2
            NavNode("hosp_elevator_2", "hosp", 2, 500f, 500f, "Elevator Lobby A (Floor 2)", "ELV-2", NodeCategory.ELEVATOR, "Level 2 surgical wing elevator landing", listOf("elevator", "lift"), true, false, "Elevator Lobby Level 2", "elevator"),
            NavNode("hosp_stairs_2", "hosp", 2, 700f, 500f, "Stairwell East (Floor 2)", "STR-2", NodeCategory.STAIRS, "Level 2 stair landing", listOf("stairs"), false, false, "Stairs East Level 2", "stairs"),
            NavNode("hosp_surgery", "hosp", 2, 300f, 300f, "Operating Theatres & Surgery", "SURG-301", NodeCategory.SURGERY, "Sterile surgical suites & pre-op prep", listOf("surgery", "operation", "surgeon", "theatre", "pre-op", "anesthesia", "surgical"), true, false, "Operating Theatres 301", "medical_services"),
            NavNode("hosp_icu", "hosp", 2, 300f, 700f, "Intensive Care Unit (ICU)", "ICU-302", NodeCategory.DOCTOR_OFFICE, "Critical care patient monitoring", listOf("icu", "intensive care", "critical care", "ventilator"), true, false, "ICU Sterile Wing 302", "health_and_safety"),
            NavNode("hosp_inpatient", "hosp", 2, 750f, 300f, "Inpatient Patient Rooms", "IP-303", NodeCategory.DOCTOR_OFFICE, "Private & shared recovery patient rooms 301-320", listOf("inpatient", "room", "bed", "ward", "admitted", "visit", "patient room"), true, false, "Inpatient Wards 303", "hotel"),
            NavNode("hosp_exit_roof", "hosp", 2, 850f, 700f, "Helipad & Emergency Roof Exit", "EXIT-R", NodeCategory.EMERGENCY_EXIT, "Emergency rooftop evacuation access point", listOf("helipad", "roof", "evacuation"), true, true, "Emergency Roof Access", "flight_takeoff")
        )

        val edges = listOf(
            // Ground Floor Edges
            NavEdge("hosp_entrance", "hosp_reception", 15f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_reception", "hosp_triage_er", 20f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_reception", "hosp_elevator_g", 18f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_g", "hosp_pharmacy", 22f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_pharmacy", "hosp_restroom_g", 16f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_g", "hosp_stairs_g", 15f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_triage_er", "hosp_exit_east", 45f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_stairs_g", "hosp_exit_east", 25f, EdgeType.CORRIDOR, true),

            // Vertical Transfers (Ground <-> Floor 1)
            NavEdge("hosp_elevator_g", "hosp_elevator_1", 10f, EdgeType.ELEVATOR, true),
            NavEdge("hosp_stairs_g", "hosp_stairs_1", 12f, EdgeType.STAIRS, false),

            // Floor 1 Edges
            NavEdge("hosp_elevator_1", "hosp_radiology", 22f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_1", "hosp_cardiology", 20f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_1", "hosp_blood_lab", 20f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_1", "hosp_stairs_1", 15f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_cardiology", "hosp_restroom_1", 35f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_blood_lab", "hosp_restroom_1", 30f, EdgeType.CORRIDOR, true),

            // Vertical Transfers (Floor 1 <-> Floor 2)
            NavEdge("hosp_elevator_1", "hosp_elevator_2", 10f, EdgeType.ELEVATOR, true),
            NavEdge("hosp_stairs_1", "hosp_stairs_2", 12f, EdgeType.STAIRS, false),

            // Floor 2 Edges
            NavEdge("hosp_elevator_2", "hosp_surgery", 24f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_2", "hosp_icu", 22f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_2", "hosp_inpatient", 26f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_elevator_2", "hosp_stairs_2", 15f, EdgeType.CORRIDOR, true),
            NavEdge("hosp_inpatient", "hosp_exit_roof", 28f, EdgeType.CORRIDOR, true)
        )

        val landmarks = listOf(
            LandmarkSample(
                "lm_hosp_er",
                "hosp_triage_er",
                "Hospital Emergency Signboard",
                R.drawable.landmark_hospital_er,
                "EMERGENCY & TRIAGE - ROOM 101",
                "Sign mounted near hallway intersection pointing to Room 101 Emergency Triage"
            )
        )

        val entrances = listOf(
            BuildingEntrance("hosp_ent_main", "Hospital Main Entrance", 12.97192, 77.59372, EntranceType.MAIN, "hosp_entrance", true, "Automatic sliding glass door on Health Blvd"),
            BuildingEntrance("hosp_ent_er", "Emergency Trauma Ambulance Bay", 12.97198, 77.59365, EntranceType.EMERGENCY, "hosp_triage_er", true, "Direct emergency red-light drop off bay"),
            BuildingEntrance("hosp_ent_east", "East Evacuation Door", 12.97210, 77.59390, EntranceType.EAST, "hosp_exit_east", true, "East parking lot ground fire exit")
        )

        return Building(
            id = "hosp",
            name = "Metropolitan City Hospital",
            type = "Hospital",
            description = "3-floor tertiary medical care center with emergency trauma center, cardiology, radiology and surgical suites",
            icon = "local_hospital",
            latitude = 12.97190,
            longitude = 77.59375,
            address = "84 Health Boulevard, Metro Medical Center",
            entrances = entrances,
            floors = floors,
            nodes = nodes,
            edges = edges,
            presetLandmarks = landmarks
        )
    }

    // -------------------------------------------------------------
    // 2. ENGINEERING BLOCK (TECH UNIVERSITY)
    // -------------------------------------------------------------
    private fun createUniversityBuilding(): Building {
        val floors = listOf(
            FloorInfo(0, "Ground Floor (Entrance, Cafeteria & Seminar Hall)", "G"),
            FloorInfo(1, "Floor 1 (CS Dept, Data Science & Principal Office)", "L1"),
            FloorInfo(2, "Floor 2 (AI Laboratory, Library & Placement Cell)", "L2"),
            FloorInfo(3, "Floor 3 (Robotics, Terrace & Research Hall)", "L3")
        )

        val nodes = listOf(
            // Floor 0: Ground Floor
            NavNode("univ_gate", "univ", 0, 150f, 500f, "Main Entrance", "ENT-01", NodeCategory.ENTRANCE, "Main architectural glass entrance to the Engineering Block", listOf("entrance", "gate", "main entrance", "door", "outside", "quad"), true, false, "Main Entrance Portal", "door_front"),
            NavNode("univ_reception", "univ", 0, 300f, 500f, "Reception & Help Desk", "REC-01", NodeCategory.RECEPTION, "Student inquiries, visitor registration, information desk", listOf("reception", "help", "information", "inquiry", "register", "desk"), true, false, "Reception & Information Desk", "desk"),
            NavNode("univ_cafeteria", "univ", 0, 300f, 250f, "Cafeteria & Food Court", "CAF-01", NodeCategory.FOOD_COURT, "Student dining hall, hot meals, bakery, coffee bar", listOf("cafeteria", "canteen", "food", "lunch", "eat", "coffee", "snacks", "tea", "meals"), true, false, "Campus Cafeteria Entrance", "restaurant"),
            NavNode("univ_seminar", "univ", 0, 750f, 250f, "Seminar Hall", "SEM-100", NodeCategory.AUDITORIUM, "500-seat multi-purpose hall for guest lectures & conferences", listOf("seminar hall", "seminar", "auditorium", "hall", "event", "talk", "conference"), true, false, "Seminar Hall Entrance", "theater_comedy"),
            NavNode("univ_elevator_0", "univ", 0, 500f, 500f, "Elevator A (Ground Floor)", "ELV-G", NodeCategory.ELEVATOR, "Passenger elevator servicing Floors Ground through 3", listOf("elevator", "lift", "elevator a"), true, false, "Elevator A Ground", "elevator"),
            NavNode("univ_stairs_0", "univ", 0, 650f, 500f, "Stairwell A (Ground Floor)", "STR-G", NodeCategory.STAIRS, "Reinforced central stairwell", listOf("stairs", "steps"), false, false, "Stairwell A", "stairs"),
            NavNode("univ_fire_exit_0", "univ", 0, 850f, 750f, "Emergency Exit (Ground)", "EXIT-G", NodeCategory.EMERGENCY_EXIT, "Direct fire door to exterior evacuation lawn", listOf("emergency exit", "fire exit", "evacuation"), true, true, "Emergency Fire Exit Ground", "exit_to_app"),
            NavNode("univ_restroom_0", "univ", 0, 300f, 750f, "Ground Restrooms", "WC-G", NodeCategory.RESTROOM, "Accessible restrooms with handrails", listOf("restroom", "toilet", "washroom"), true, false, "Restrooms Ground", "wc"),

            // Floor 1
            NavNode("univ_elevator_1", "univ", 1, 500f, 500f, "Elevator A (Floor 1)", "ELV-1", NodeCategory.ELEVATOR, "Floor 1 elevator lobby", listOf("elevator", "lift"), true, false, "Elevator A Level 1", "elevator"),
            NavNode("univ_stairs_1", "univ", 1, 650f, 500f, "Stairwell A (Floor 1)", "STR-1", NodeCategory.STAIRS, "Floor 1 stairs landing", listOf("stairs"), false, false, "Stairwell A Level 1", "stairs"),
            NavNode("univ_cs_dept", "univ", 1, 300f, 300f, "Computer Science Department", "CS-101", NodeCategory.FACULTY, "CS faculty offices, department head cabin, meeting room", listOf("computer science department", "cs department", "computer science", "cs dept", "cs faculty"), true, false, "Computer Science Department Head Office", "school"),
            NavNode("univ_datascience_lab", "univ", 1, 750f, 300f, "Data Science Laboratory", "DS-102", NodeCategory.LABORATORY, "Big Data analytics clusters, GPU workstations, statistical modeling", listOf("data science laboratory", "data science lab", "data science", "analytics lab", "gpu"), true, false, "Data Science Laboratory DS-102", "computer"),
            NavNode("univ_principal", "univ", 1, 300f, 700f, "Principal Office", "OFF-105", NodeCategory.FACULTY, "Principal & Executive Dean administrative suite", listOf("principal office", "principal", "director", "dean office", "administration"), true, false, "Principal Executive Suite 105", "account_balance"),
            NavNode("univ_restroom_1", "univ", 1, 750f, 700f, "Floor 1 Restrooms", "WC-1", NodeCategory.RESTROOM, "Male, female, and all-gender accessible restrooms", listOf("restroom", "toilet", "washroom"), true, false, "Restrooms Floor 1", "wc"),
            NavNode("univ_fire_exit_1", "univ", 1, 850f, 500f, "Emergency Exit (Floor 1)", "EXIT-1", NodeCategory.EMERGENCY_EXIT, "Floor 1 external emergency fire escape", listOf("emergency exit", "fire exit"), true, true, "Emergency Exit Floor 1", "exit_to_app"),

            // Floor 2
            NavNode("univ_elevator_2", "univ", 2, 500f, 500f, "Elevator A (Floor 2)", "ELV-2", NodeCategory.ELEVATOR, "Floor 2 elevator lobby", listOf("elevator", "lift"), true, false, "Elevator A Level 2", "elevator"),
            NavNode("univ_stairs_2", "univ", 2, 650f, 500f, "Stairwell A (Floor 2)", "STR-2", NodeCategory.STAIRS, "Floor 2 stairs landing", listOf("stairs"), false, false, "Stairwell A Level 2", "stairs"),
            NavNode("univ_cslab", "univ", 2, 300f, 300f, "AI Laboratory", "AI-204", NodeCategory.LABORATORY, "Artificial Intelligence & Neural Networks Research Lab, Room 204", listOf("ai laboratory", "ai lab", "artificial intelligence", "deep learning", "machine learning", "room 204", "python", "ai"), true, false, "AI LABORATORY - ROOM 204", "psychology"),
            NavNode("univ_library", "univ", 2, 750f, 300f, "Library", "LIB-200", NodeCategory.LIBRARY, "Central research library, quiet study carrels, digital books archive", listOf("library", "books", "reading", "study", "quiet", "journals", "research"), true, false, "Central University Library", "menu_book"),
            NavNode("univ_placement", "univ", 2, 300f, 700f, "Placement Cell", "PLC-210", NodeCategory.RECEPTION, "Corporate relations, campus interview rooms, career counseling", listOf("placement cell", "placement", "jobs", "careers", "internships", "interviews", "recruitment"), true, false, "Placement Cell & Career Center", "work"),
            NavNode("univ_faculty", "univ", 2, 500f, 750f, "Dean & Faculty Lounge", "FAC-215", NodeCategory.FACULTY, "Professors offices, academic advisor cabins", listOf("faculty", "professor", "dean", "teacher", "advisor", "office"), true, false, "Faculty Offices Wing", "groups"),
            NavNode("univ_restroom_2", "univ", 2, 750f, 700f, "Floor 2 Restrooms", "WC-2", NodeCategory.RESTROOM, "Accessible restrooms with touchless fixtures", listOf("restroom", "toilet", "washroom"), true, false, "Restrooms Floor 2", "wc"),

            // Floor 3
            NavNode("univ_elevator_3", "univ", 3, 500f, 500f, "Elevator A (Floor 3)", "ELV-3", NodeCategory.ELEVATOR, "Floor 3 penthouse & lab elevator landing", listOf("elevator", "lift"), true, false, "Elevator A Level 3", "elevator"),
            NavNode("univ_stairs_3", "univ", 3, 650f, 500f, "Stairwell A (Floor 3)", "STR-3", NodeCategory.STAIRS, "Floor 3 stair landing", listOf("stairs"), false, false, "Stairwell A Level 3", "stairs"),
            NavNode("univ_robotics", "univ", 3, 300f, 300f, "Robotics & IoT Research Arena", "ROB-301", NodeCategory.LABORATORY, "Autonomous drone testing arena, micro-controllers, humanoid robotics", listOf("robotics", "iot", "drone", "hardware", "microcontroller", "embedded"), true, false, "Robotics Research Arena", "smart_toy"),
            NavNode("univ_terrace_hall", "univ", 3, 750f, 300f, "Terrace Conference Hall", "TER-305", NodeCategory.AUDITORIUM, "Scenic sky-deck conference hall & reception foyer", listOf("terrace", "conference", "sky hall", "meeting", "events"), true, false, "Terrace Conference Hall", "meeting_room"),
            NavNode("univ_fire_exit_roof", "univ", 3, 850f, 700f, "Emergency Roof Exit", "EXIT-R", NodeCategory.EMERGENCY_EXIT, "Emergency rooftop helipad access door", listOf("emergency exit", "roof exit", "fire escape"), true, true, "Emergency Roof Exit", "exit_to_app")
        )

        val edges = listOf(
            // Floor 0
            NavEdge("univ_gate", "univ_reception", 15f, EdgeType.CORRIDOR, true),
            NavEdge("univ_reception", "univ_cafeteria", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_reception", "univ_elevator_0", 18f, EdgeType.CORRIDOR, true),
            NavEdge("univ_reception", "univ_restroom_0", 22f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_0", "univ_seminar", 28f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_0", "univ_stairs_0", 12f, EdgeType.CORRIDOR, true),
            NavEdge("univ_stairs_0", "univ_fire_exit_0", 25f, EdgeType.CORRIDOR, true),

            // Floor 0 <-> Floor 1
            NavEdge("univ_elevator_0", "univ_elevator_1", 10f, EdgeType.ELEVATOR, true),
            NavEdge("univ_stairs_0", "univ_stairs_1", 12f, EdgeType.STAIRS, false),

            // Floor 1
            NavEdge("univ_elevator_1", "univ_cs_dept", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_1", "univ_datascience_lab", 22f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_1", "univ_principal", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_1", "univ_stairs_1", 12f, EdgeType.CORRIDOR, true),
            NavEdge("univ_datascience_lab", "univ_restroom_1", 25f, EdgeType.CORRIDOR, true),
            NavEdge("univ_datascience_lab", "univ_fire_exit_1", 20f, EdgeType.CORRIDOR, true),

            // Floor 1 <-> Floor 2
            NavEdge("univ_elevator_1", "univ_elevator_2", 10f, EdgeType.ELEVATOR, true),
            NavEdge("univ_stairs_1", "univ_stairs_2", 12f, EdgeType.STAIRS, false),

            // Floor 2
            NavEdge("univ_elevator_2", "univ_cslab", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_2", "univ_library", 25f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_2", "univ_placement", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_2", "univ_faculty", 18f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_2", "univ_stairs_2", 12f, EdgeType.CORRIDOR, true),
            NavEdge("univ_library", "univ_restroom_2", 25f, EdgeType.CORRIDOR, true),

            // Floor 2 <-> Floor 3
            NavEdge("univ_elevator_2", "univ_elevator_3", 10f, EdgeType.ELEVATOR, true),
            NavEdge("univ_stairs_2", "univ_stairs_3", 12f, EdgeType.STAIRS, false),

            // Floor 3
            NavEdge("univ_elevator_3", "univ_robotics", 20f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_3", "univ_terrace_hall", 25f, EdgeType.CORRIDOR, true),
            NavEdge("univ_elevator_3", "univ_stairs_3", 12f, EdgeType.CORRIDOR, true),
            NavEdge("univ_terrace_hall", "univ_fire_exit_roof", 22f, EdgeType.CORRIDOR, true)
        )

        val landmarks = listOf(
            LandmarkSample(
                "lm_univ_cs",
                "univ_cslab",
                "AI Laboratory Wall Plaque",
                R.drawable.landmark_univ_cslab,
                "AI LABORATORY - ROOM 204",
                "Wall mounted plaque outside Room 204 glass entryway"
            )
        )

        val entrances = listOf(
            BuildingEntrance("univ_ent_main", "Main Architectural Entrance", 12.97162, 77.59462, EntranceType.MAIN, "univ_gate", true, "Central glass double-door campus entrance"),
            BuildingEntrance("univ_ent_ramp", "Accessible Ramp Portal", 12.97155, 77.59468, EntranceType.ACCESSIBLE, "univ_reception", true, "Wheelchair ramp entry leading to reception"),
            BuildingEntrance("univ_ent_fire", "Lawn Evacuation Gate", 12.97175, 77.59480, EntranceType.EMERGENCY, "univ_fire_exit_0", true, "Ground floor fire evacuation door")
        )

        return Building(
            id = "univ",
            name = "Engineering Block",
            type = "University",
            description = "4-floor modern engineering campus with AI Laboratory, Data Science Lab, Library, Placement Cell, Cafeteria, and Seminar Hall",
            icon = "school",
            latitude = 12.97160,
            longitude = 77.59465,
            address = "Campus North Wing, Tech Park Avenue",
            entrances = entrances,
            floors = floors,
            nodes = nodes,
            edges = edges,
            presetLandmarks = landmarks
        )
    }

    // -------------------------------------------------------------
    // 3. INTERNATIONAL AIRPORT TERMINAL
    // -------------------------------------------------------------
    private fun createAirportBuilding(): Building {
        val floors = listOf(
            FloorInfo(0, "Concourse Level (Check-In & Gates A1-B4)", "L1"),
            FloorInfo(1, "Mezzanine Level (Airline Lounges & Sky Dining)", "L2")
        )

        val nodes = listOf(
            NavNode("air_dropoff", "air", 0, 100f, 500f, "Terminal Departures Drop-off", "AIR-01", NodeCategory.ENTRANCE, "Curbside passenger vehicle drop-off", listOf("entrance", "drop off", "taxi", "departures"), true, false, "Terminal 3 Departures", "flight_takeoff"),
            NavNode("air_checkin", "air", 0, 250f, 500f, "Airline Check-in & Bag Drop", "CHK-ALL", NodeCategory.CHECK_IN, "Self-service kiosks and baggage drop desks", listOf("check in", "baggage drop", "boarding pass", "luggage", "ticket", "counter"), true, false, "Check-in Desks 1-20", "luggage"),
            NavNode("air_security", "air", 0, 420f, 500f, "Security Screening Checkpoint", "SEC-01", NodeCategory.SECURITY_CHECK, "TSA body scanners, carry-on x-ray lanes", listOf("security", "screening", "xray", "metal detector", "customs", "passport"), true, false, "Security Screening Lanes", "security"),
            NavNode("air_dutyfree", "air", 0, 580f, 500f, "Duty Free Plaza", "DUTY-01", NodeCategory.DUTY_FREE, "Cosmetics, perfumes, electronics, travel retail", listOf("duty free", "shopping", "perfume", "gifts", "souvenirs", "store", "buy"), true, false, "World Duty Free Plaza", "shopping_bag"),
            NavNode("air_gate_a1", "air", 0, 750f, 250f, "Flight Gate A1 - A4", "GATE-A1", NodeCategory.AIRPORT_GATE, "Domestic departure boarding gates A1-A4", listOf("gate a1", "gate a2", "gate a3", "gate a4", "gate a", "boarding", "flight"), true, false, "Gates A1 - A4 Signboard", "airplane_ticket"),
            NavNode("air_gate_b4", "air", 0, 750f, 750f, "Flight Gate B4 (Tokyo / London)", "GATE-B4", NodeCategory.AIRPORT_GATE, "International long-haul departure gate B4", listOf("gate b4", "gate b", "tokyo", "london", "flight b4", "gate b1", "gate b2", "gate b3", "gate b4"), true, false, "GATE B4 - DEPARTURES", "flight"),
            NavNode("air_baggage", "air", 0, 420f, 800f, "Baggage Claim Carousel 3", "BAG-03", NodeCategory.BAGGAGE_CLAIM, "Arrivals baggage carousel", listOf("baggage claim", "luggage", "carousel", "arrivals"), true, false, "Baggage Claim 3", "luggage"),
            NavNode("air_emergency_runway", "air", 0, 920f, 500f, "Tarmac Emergency Evacuation Gate", "EXIT-T", NodeCategory.EMERGENCY_EXIT, "Emergency pressurized crash gate leading directly to runway buffer", listOf("emergency exit", "fire exit", "runway exit", "evacuation"), true, true, "Emergency Runway Crash Exit", "exit_to_app"),
            NavNode("air_elevator_0", "air", 0, 580f, 320f, "Terminal Elevator Bank", "ELV-A0", NodeCategory.ELEVATOR, "Concourse elevator to Mezzanine VIP Lounges", listOf("elevator", "lift", "lounge"), true, false, "Mezzanine Elevator", "elevator"),

            // Floor 1
            NavNode("air_elevator_1", "air", 1, 580f, 320f, "Mezzanine Elevator Bank (Floor 2)", "ELV-A1", NodeCategory.ELEVATOR, "Mezzanine lounge level elevator lobby", listOf("elevator"), true, false, "Mezzanine Lobby", "elevator"),
            NavNode("air_vip_lounge", "air", 1, 400f, 320f, "Star Alliance VIP Lounge", "LNG-01", NodeCategory.GENERAL, "First & Business class lounge with showers & hot buffet", listOf("vip lounge", "lounge", "first class", "business class", "showers", "relax", "quiet"), true, false, "Star VIP Lounge Portal", "airline_seat_recline_extra"),
            NavNode("air_food_court", "air", 1, 750f, 320f, "Skyline Dining & Coffee Bar", "DINE-01", NodeCategory.FOOD_COURT, "Artisan cafes, noodle bar, burger lounge", listOf("coffee", "food", "eat", "cafe", "restaurant", "dining", "espresso", "burger", "water", "tea"), true, false, "Skyline Food & Coffee", "restaurant")
        )

        val edges = listOf(
            NavEdge("air_dropoff", "air_checkin", 25f, EdgeType.CORRIDOR, true),
            NavEdge("air_checkin", "air_security", 30f, EdgeType.CORRIDOR, true),
            NavEdge("air_security", "air_dutyfree", 25f, EdgeType.CORRIDOR, true),
            NavEdge("air_dutyfree", "air_gate_a1", 35f, EdgeType.CORRIDOR, true),
            NavEdge("air_dutyfree", "air_gate_b4", 35f, EdgeType.CORRIDOR, true),
            NavEdge("air_dutyfree", "air_elevator_0", 15f, EdgeType.CORRIDOR, true),
            NavEdge("air_dutyfree", "air_emergency_runway", 50f, EdgeType.CORRIDOR, true),
            NavEdge("air_gate_a1", "air_emergency_runway", 40f, EdgeType.CORRIDOR, true),
            NavEdge("air_gate_b4", "air_emergency_runway", 40f, EdgeType.CORRIDOR, true),
            NavEdge("air_security", "air_baggage", 35f, EdgeType.CORRIDOR, true),

            NavEdge("air_elevator_0", "air_elevator_1", 12f, EdgeType.ELEVATOR, true),
            NavEdge("air_elevator_1", "air_vip_lounge", 20f, EdgeType.CORRIDOR, true),
            NavEdge("air_elevator_1", "air_food_court", 20f, EdgeType.CORRIDOR, true)
        )

        val landmarks = listOf(
            LandmarkSample(
                "lm_air_gate_b4",
                "air_gate_b4",
                "Gate B4 Overhead Signboard",
                R.drawable.landmark_airport_gate,
                "GATE B4 - DEPARTURES",
                "High-contrast overhead illuminated gate direction signboard"
            )
        )

        val entrances = listOf(
            BuildingEntrance("air_ent_dropoff", "Terminal 3 Departures Curbside", 12.98002, 77.60002, EntranceType.MAIN, "air_dropoff", true, "Curbside passenger vehicle drop-off door"),
            BuildingEntrance("air_ent_crash", "Tarmac Emergency Crash Gate", 12.98015, 77.60020, EntranceType.EMERGENCY, "air_emergency_runway", true, "Pressurized runway access gate")
        )

        return Building(
            id = "air",
            name = "International Airport Terminal 3",
            type = "Airport",
            description = "Multi-concourse aviation terminal featuring fast-track security, departure gates A/B, duty free retail, and VIP mezzanine lounges",
            icon = "local_airport",
            latitude = 12.98000,
            longitude = 77.60005,
            address = "International Terminal 3, Airport Expressway",
            entrances = entrances,
            floors = floors,
            nodes = nodes,
            edges = edges,
            presetLandmarks = landmarks
        )
    }

    // -------------------------------------------------------------
    // 4. GRAND CENTRAL SHOPPING MALL
    // -------------------------------------------------------------
    private fun createMallBuilding(): Building {
        val floors = listOf(
            FloorInfo(0, "Level 1 (Fashion & Dining)", "L1"),
            FloorInfo(1, "Level 2 (Cinema & Entertainment)", "L2")
        )

        val nodes = listOf(
            NavNode("mall_entrance", "mall", 0, 150f, 500f, "Main Promenade Entrance", "ENT-M", NodeCategory.ENTRANCE, "Grand atrium plaza entrance with revolving glass doors", listOf("entrance", "mall gate", "door", "plaza"), true, false, "Promenade Gate", "door_front"),
            NavNode("mall_info", "mall", 0, 320f, 500f, "Information & Concierge", "INF-01", NodeCategory.RECEPTION, "Gift cards, wheelchair loan, directory kiosks", listOf("info", "concierge", "wheelchair rental", "help", "lost and found"), true, false, "Mall Information Hub", "info"),
            NavNode("mall_foodcourt", "mall", 0, 320f, 250f, "Gourmet Food Court", "FOOD-01", NodeCategory.FOOD_COURT, "15 food stalls, sushi bar, pizza, bubble tea", listOf("food court", "eat", "lunch", "coffee", "pizza", "sushi", "snack", "burger", "drinks"), true, false, "Food Court Atrium", "fastfood"),
            NavNode("mall_stores", "mall", 0, 320f, 750f, "Fashion & Apparel Wing", "STR-01", NodeCategory.STORE, "Apparel brands, shoes, sportswear, jewelry", listOf("clothes", "shopping", "shoes", "fashion", "apparel", "stores", "buy clothes"), true, false, "Fashion Avenue", "checkroom"),
            NavNode("mall_restroom_0", "mall", 0, 750f, 750f, "Family Restrooms & Nursery", "WC-M0", NodeCategory.RESTROOM, "Family restroom, nursing lounge, handicap accessible stall", listOf("restroom", "toilet", "washroom", "baby", "nursery"), true, false, "Family Restroom Sign", "wc"),
            NavNode("mall_elevator_0", "mall", 0, 550f, 500f, "Panoramic Glass Elevator", "ELV-M0", NodeCategory.ELEVATOR, "360-degree glass scenic elevator", listOf("elevator", "lift", "cinema"), true, false, "Panoramic Elevator", "elevator"),
            NavNode("mall_stairs_0", "mall", 0, 700f, 500f, "Central Grand Staircase", "STR-M0", NodeCategory.STAIRS, "Illuminated step staircase", listOf("stairs", "steps"), false, false, "Grand Staircase", "stairs"),
            NavNode("mall_fire_exit", "mall", 0, 900f, 250f, "Emergency Fire Exit North", "EXIT-MN", NodeCategory.EMERGENCY_EXIT, "Emergency exit leading directly to open avenue", listOf("fire exit", "emergency exit", "evacuation"), true, true, "Emergency North Exit", "exit_to_app"),

            // Floor 1
            NavNode("mall_elevator_1", "mall", 1, 550f, 500f, "Panoramic Glass Elevator (Level 2)", "ELV-M1", NodeCategory.ELEVATOR, "Level 2 entertainment deck elevator landing", listOf("elevator"), true, false, "Elevator Landing Level 2", "elevator"),
            NavNode("mall_stairs_1", "mall", 1, 700f, 500f, "Central Grand Staircase (Level 2)", "STR-M1", NodeCategory.STAIRS, "Level 2 stairs landing", listOf("stairs"), false, false, "Grand Stairs L2", "stairs"),
            NavNode("mall_cinema", "mall", 1, 350f, 300f, "IMAX Cinema & Box Office", "CIN-01", NodeCategory.GENERAL, "10-screen multiplex with IMAX 3D & popcorn counter", listOf("cinema", "movie", "imax", "film", "popcorn", "theater", "tickets", "show"), true, false, "IMAX Multiplex Entrance", "movie"),
            NavNode("mall_arcade", "mall", 1, 350f, 700f, "VR & Game Arcade Zone", "ARC-01", NodeCategory.GENERAL, "Bowling, arcade racing, VR simulators, laser tag", listOf("arcade", "games", "vr", "bowling", "play", "fun", "gaming"), true, false, "VR & Arcade Arena", "sports_esports"),
            NavNode("mall_tech", "mall", 1, 750f, 300f, "Electronics & Gadgets Hall", "TECH-01", NodeCategory.STORE, "Smartphones, laptops, smart home devices, headphones", listOf("electronics", "phones", "laptop", "gadgets", "apple", "samsung", "tech"), true, false, "Electronics Superstore", "devices")
        )

        val edges = listOf(
            NavEdge("mall_entrance", "mall_info", 18f, EdgeType.CORRIDOR, true),
            NavEdge("mall_info", "mall_foodcourt", 22f, EdgeType.CORRIDOR, true),
            NavEdge("mall_info", "mall_stores", 22f, EdgeType.CORRIDOR, true),
            NavEdge("mall_info", "mall_elevator_0", 20f, EdgeType.CORRIDOR, true),
            NavEdge("mall_elevator_0", "mall_stairs_0", 15f, EdgeType.CORRIDOR, true),
            NavEdge("mall_foodcourt", "mall_fire_exit", 40f, EdgeType.CORRIDOR, true),
            NavEdge("mall_stores", "mall_restroom_0", 25f, EdgeType.CORRIDOR, true),
            NavEdge("mall_stairs_0", "mall_restroom_0", 20f, EdgeType.CORRIDOR, true),

            NavEdge("mall_elevator_0", "mall_elevator_1", 12f, EdgeType.ELEVATOR, true),
            NavEdge("mall_stairs_0", "mall_stairs_1", 14f, EdgeType.STAIRS, false),

            NavEdge("mall_elevator_1", "mall_cinema", 25f, EdgeType.CORRIDOR, true),
            NavEdge("mall_elevator_1", "mall_arcade", 25f, EdgeType.CORRIDOR, true),
            NavEdge("mall_elevator_1", "mall_tech", 22f, EdgeType.CORRIDOR, true),
            NavEdge("mall_elevator_1", "mall_stairs_1", 15f, EdgeType.CORRIDOR, true)
        )

        val mallEntrances = listOf(
            BuildingEntrance("mall_ent_main", "Main Promenade Entrance", 12.96505, 77.59005, EntranceType.MAIN, "mall_entrance", true, "Grand atrium revolving glass doors"),
            BuildingEntrance("mall_ent_north", "North Fire Evacuation Exit", 12.96520, 77.59025, EntranceType.NORTH, "mall_fire_exit", true, "North pedestrian avenue exit door")
        )

        return Building(
            id = "mall",
            name = "Grand Central Shopping Mall",
            type = "Mall",
            description = "Multi-level modern retail & entertainment complex with IMAX theatre, VR arcade, international food court, and fashion department stores",
            icon = "storefront",
            latitude = 12.96500,
            longitude = 77.59000,
            address = "700 Grand Promenade Boulevard, City Center",
            entrances = mallEntrances,
            floors = floors,
            nodes = nodes,
            edges = edges
        )
    }
}
