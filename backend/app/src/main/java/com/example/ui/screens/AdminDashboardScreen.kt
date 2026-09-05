package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.model.*
import com.example.positioning.*
import com.example.routing.GraphValidator
import com.example.ui.components.AdminAnalyticsDashboard
import com.example.ui.components.AdminMapStudio
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class AdminSubTab(val label: String, val icon: ImageVector) {
    ANALYTICS("Analytics", Icons.Default.Analytics),
    MAP_STUDIO("Map Studio", Icons.Default.EditLocationAlt),
    BUILDINGS("Buildings", Icons.Default.Apartment),
    LOCATIONS("Nodes & Rooms", Icons.Default.Place),
    EDGES("Route Edges", Icons.Default.AltRoute),
    BEACONS("BLE Beacons", Icons.Default.Sensors),
    QR_CALIBRATION("QR Calibration", Icons.Default.QrCodeScanner),
    GRAPH_VALIDATE("Validate & Publish", Icons.Default.FactCheck),
    POSITIONING("Sensors & Demo", Icons.Default.MyLocation)
}

@Composable
fun AdminDashboardScreen(
    buildings: List<Building>,
    currentBuilding: Building,
    analyticsData: AnalyticsData,
    onSelectBuilding: (String) -> Unit,
    onCreateBuilding: (name: String, code: String, type: String, description: String, address: String, lat: Double, lng: Double) -> Unit,
    onUpdateBuilding: (Building) -> Unit = {},
    onDeleteBuilding: (String, Boolean) -> DeleteResult,
    onAddLocation: (name: String, code: String, cat: NodeCategory, floor: Int, desc: String, x: Float, y: Float, isAccessible: Boolean, isEmergency: Boolean) -> Unit,
    onUpdateLocation: (NavNode) -> Unit = {},
    onDeleteLocation: (String, Boolean) -> DeleteResult,
    onAddEdge: (fromId: String, toId: String, dist: Float, type: EdgeType, isWheelchairAccessible: Boolean, bidirectional: Boolean) -> Unit,
    onDeleteEdge: (fromId: String, toId: String) -> Unit,
    onAddFloor: (floorNum: Int, name: String, shortLabel: String, mapWidth: Float, mapHeight: Float, mapAsset: String) -> Unit,
    onUpdateFloor: (FloorInfo) -> Unit = {},
    onDeleteFloor: (Int, Boolean) -> DeleteResult = { _, _ -> DeleteResult.Success },
    beacons: List<Beacon> = emptyList(),
    qrPoints: List<QrCalibrationPoint> = emptyList(),
    onAddBeacon: (Beacon) -> Unit = {},
    onUpdateBeacon: (Beacon) -> Unit = {},
    onDeleteBeacon: (String) -> Unit = {},
    onAddQrPoint: (QrCalibrationPoint) -> Unit = {},
    onUpdateQrPoint: (QrCalibrationPoint) -> Unit = {},
    onDeleteQrPoint: (String) -> Unit = {},
    onRunGraphValidation: (String) -> GraphValidationReport = { GraphValidator.validateGraph(currentBuilding) },
    onPublishBuilding: (String, (Result<GraphValidationReport>) -> Unit) -> Unit = { _, cb -> cb(Result.success(GraphValidator.validateGraph(currentBuilding))) },
    validationReport: GraphValidationReport? = null,
    positioningMode: PositioningMode = PositioningMode.HYBRID_AUTO,
    onSetPositioningMode: (PositioningMode) -> Unit = {},
    onSimulateBeacon: (String, Int) -> Unit = { _, _ -> },
    onTriggerSimulatedStep: () -> Unit = {},
    onSimulateGps: (Double, Double) -> Unit = { _, _ -> },
    locationState: NavigationLocationState? = null,
    currentIndoorPosition: IndoorPosition? = null,
    deviceHeadingDegrees: Float = 0f,
    // Phase 12 Analytics Parameters
    analyticsOverview: AnalyticsOverviewMetrics = AnalyticsOverviewMetrics(),
    topDestinations: List<TopDestinationMetric> = emptyList(),
    searchDemand: List<SearchMetric> = emptyList(),
    zeroResultSearches: List<SearchMetric> = emptyList(),
    positioningDistribution: List<PositioningSourceMetric> = emptyList(),
    floorActivity: List<FloorActivityMetric> = emptyList(),
    problematicLocations: List<ProblematicLocationAlert> = emptyList(),
    adminAlerts: List<ProblematicLocationAlert> = emptyList(),
    recentSessions: List<NavigationSessionEntity> = emptyList(),
    dateTrends: List<DateTrendMetric> = emptyList(),
    selectedAnalyticsBuildingId: String? = null,
    onSelectAnalyticsBuilding: (String?) -> Unit = {},
    analyticsDateRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
    onSelectAnalyticsDateRange: (AnalyticsDateRange) -> Unit = {},
    analyticsResultFilter: ResultFilter = ResultFilter.ALL,
    onSelectAnalyticsResultFilter: (ResultFilter) -> Unit = {},
    analyticsAccessibilityFilter: AccessibilityFilter = AccessibilityFilter.ALL,
    onSelectAnalyticsAccessibilityFilter: (AccessibilityFilter) -> Unit = {},
    includeDemoSessions: Boolean = false,
    onToggleIncludeDemoSessions: (Boolean) -> Unit = {},
    analyticsLastUpdated: Long = System.currentTimeMillis(),
    onRefreshAnalytics: () -> Unit = {},
    onClearDemoData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(AdminSubTab.MAP_STUDIO) }
    var activeStudioFloor by remember { mutableIntStateOf(0) }

    // Dialog states
    var showAddBuildingDialog by remember { mutableStateOf(false) }
    var showEditBuildingDialog by remember { mutableStateOf<Building?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showEditLocationDialog by remember { mutableStateOf<NavNode?>(null) }
    var showAddEdgeDialog by remember { mutableStateOf(false) }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var showAddBeaconDialog by remember { mutableStateOf(false) }
    var showAddQrDialog by remember { mutableStateOf(false) }
    var showQrPreviewDialog by remember { mutableStateOf<QrCalibrationPoint?>(null) }

    // Prefilled coordinates for map-based placement
    var pendingPlacementX by remember { mutableFloatStateOf(500f) }
    var pendingPlacementY by remember { mutableFloatStateOf(500f) }

    // Delete Dependency Safety Dialog
    var pendingDeleteDependency by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(14.dp)
    ) {
        // Admin Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Amber500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Admin Map Management",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Facility: ${currentBuilding.name} (v${currentBuilding.graphVersion})",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                // Building Switcher dropdown chip
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Text(
                        text = currentBuilding.type,
                        color = Cyan400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub Tabs Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminSubTab.values().toList()) { tab ->
                val isSelected = tab == selectedSubTab
                Surface(
                    color = if (isSelected) Cyan500 else Slate900,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Cyan400 else Slate800
                    ),
                    modifier = Modifier.clickable { selectedSubTab = tab }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) Slate950 else Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = tab.label,
                            color = if (isSelected) Slate950 else Slate200,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub Tab Content
        when (selectedSubTab) {
            AdminSubTab.MAP_STUDIO -> {
                AdminMapStudio(
                    building = currentBuilding,
                    activeFloor = activeStudioFloor,
                    onSelectFloor = { activeStudioFloor = it },
                    beacons = beacons,
                    qrPoints = qrPoints,
                    onAddLocationAtCoordinate = { x, y ->
                        pendingPlacementX = x
                        pendingPlacementY = y
                        showAddLocationDialog = true
                    },
                    onAddNodeAtCoordinate = { x, y ->
                        pendingPlacementX = x
                        pendingPlacementY = y
                        showAddLocationDialog = true
                    },
                    onAddBeaconAtCoordinate = { x, y ->
                        pendingPlacementX = x
                        pendingPlacementY = y
                        showAddBeaconDialog = true
                    },
                    onAddQrPointAtCoordinate = { x, y ->
                        pendingPlacementX = x
                        pendingPlacementY = y
                        showAddQrDialog = true
                    },
                    onConnectNodes = { from, to, dist, type ->
                        onAddEdge(from, to, dist, type, type != EdgeType.STAIRS && type != EdgeType.ESCALATOR, true)
                    },
                    onEditNode = { node -> showEditLocationDialog = node },
                    onDeleteNode = { nodeId ->
                        val result = onDeleteLocation(nodeId, false)
                        if (result is DeleteResult.HasDependencies) {
                            pendingDeleteDependency = Triple("Delete Navigation Node", result.message) {
                                onDeleteLocation(nodeId, true)
                            }
                        }
                    },
                    onDeleteEdge = onDeleteEdge
                )
            }

            AdminSubTab.ANALYTICS -> {
                AdminAnalyticsDashboard(
                    buildings = buildings,
                    currentBuilding = currentBuilding,
                    overviewMetrics = analyticsOverview,
                    topDestinations = topDestinations,
                    searchDemand = searchDemand,
                    zeroResultSearches = zeroResultSearches,
                    positioningDistribution = positioningDistribution,
                    floorActivity = floorActivity,
                    problematicLocations = problematicLocations,
                    adminAlerts = adminAlerts,
                    recentSessions = recentSessions,
                    dateTrends = dateTrends,
                    selectedBuildingId = selectedAnalyticsBuildingId,
                    onSelectBuilding = onSelectAnalyticsBuilding,
                    dateRange = analyticsDateRange,
                    onSelectDateRange = onSelectAnalyticsDateRange,
                    resultFilter = analyticsResultFilter,
                    onSelectResultFilter = onSelectAnalyticsResultFilter,
                    accessibilityFilter = analyticsAccessibilityFilter,
                    onSelectAccessibilityFilter = onSelectAnalyticsAccessibilityFilter,
                    includeDemoSessions = includeDemoSessions,
                    onToggleIncludeDemoSessions = onToggleIncludeDemoSessions,
                    lastUpdatedTimestamp = analyticsLastUpdated,
                    onRefresh = onRefreshAnalytics,
                    onClearDemoData = onClearDemoData,
                    onReviewInMapStudio = {
                        selectedSubTab = AdminSubTab.MAP_STUDIO
                    }
                )
            }

            AdminSubTab.BUILDINGS -> {
                AdminBuildingsSection(
                    buildings = buildings,
                    currentBuilding = currentBuilding,
                    onSelectBuilding = onSelectBuilding,
                    onOpenAddBuilding = { showAddBuildingDialog = true },
                    onEditBuilding = { b -> showEditBuildingDialog = b },
                    onDeleteBuilding = { id ->
                        val result = onDeleteBuilding(id, false)
                        if (result is DeleteResult.HasDependencies) {
                            pendingDeleteDependency = Triple("Delete Building & All Data", result.message) {
                                onDeleteBuilding(id, true)
                            }
                        }
                    },
                    onOpenAddFloor = { showAddFloorDialog = true },
                    onDeleteFloor = { floorNum ->
                        val result = onDeleteFloor(floorNum, false)
                        if (result is DeleteResult.HasDependencies) {
                            pendingDeleteDependency = Triple("Delete Floor", result.message) {
                                onDeleteFloor(floorNum, true)
                            }
                        }
                    }
                )
            }

            AdminSubTab.LOCATIONS -> {
                AdminLocationsSection(
                    building = currentBuilding,
                    onOpenAddLocation = {
                        pendingPlacementX = 500f
                        pendingPlacementY = 500f
                        showAddLocationDialog = true
                    },
                    onEditLocation = { node -> showEditLocationDialog = node },
                    onDeleteLocation = { nodeId ->
                        val result = onDeleteLocation(nodeId, false)
                        if (result is DeleteResult.HasDependencies) {
                            pendingDeleteDependency = Triple("Delete Navigation Node", result.message) {
                                onDeleteLocation(nodeId, true)
                            }
                        }
                    }
                )
            }

            AdminSubTab.EDGES -> {
                AdminEdgesSection(
                    building = currentBuilding,
                    onOpenAddEdge = { showAddEdgeDialog = true },
                    onDeleteEdge = onDeleteEdge
                )
            }

            AdminSubTab.BEACONS -> {
                AdminBeaconsSection(
                    building = currentBuilding,
                    beacons = beacons,
                    onOpenAddBeacon = {
                        pendingPlacementX = 500f
                        pendingPlacementY = 500f
                        showAddBeaconDialog = true
                    },
                    onDeleteBeacon = onDeleteBeacon,
                    onSimulateBeacon = onSimulateBeacon
                )
            }

            AdminSubTab.QR_CALIBRATION -> {
                AdminQrSection(
                    building = currentBuilding,
                    qrPoints = qrPoints,
                    onOpenAddQr = {
                        pendingPlacementX = 500f
                        pendingPlacementY = 500f
                        showAddQrDialog = true
                    },
                    onPreviewQr = { qr -> showQrPreviewDialog = qr },
                    onDeleteQr = onDeleteQrPoint
                )
            }

            AdminSubTab.GRAPH_VALIDATE -> {
                AdminGraphValidationSection(
                    building = currentBuilding,
                    onRunValidation = { onRunGraphValidation(currentBuilding.id) },
                    onPublishBuilding = { cb -> onPublishBuilding(currentBuilding.id, cb) },
                    report = validationReport
                )
            }

            AdminSubTab.POSITIONING -> {
                AdminPositioningSection(
                    building = currentBuilding,
                    positioningMode = positioningMode,
                    onSetPositioningMode = onSetPositioningMode,
                    onTriggerSimulatedStep = onTriggerSimulatedStep,
                    onSimulateGps = onSimulateGps,
                    locationState = locationState,
                    currentIndoorPosition = currentIndoorPosition,
                    deviceHeadingDegrees = deviceHeadingDegrees
                )
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS & ACTION PROMPTS
    // -------------------------------------------------------------

    // Dependency Confirmation Alert
    if (pendingDeleteDependency != null) {
        val (title, message, action) = pendingDeleteDependency!!
        AlertDialog(
            onDismissRequest = { pendingDeleteDependency = null },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Amber400)
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, color = Slate200, fontSize = 13.sp)
                    Text(
                        "Deleting this item will cascade delete connected edges, beacons, and calibration points. Do you wish to proceed?",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        action()
                        pendingDeleteDependency = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red500)
                ) {
                    Text("Cascade Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDependency = null }) {
                    Text("Cancel", color = Slate300)
                }
            }
        )
    }

    if (showAddBuildingDialog) {
        AddBuildingDialog(
            onDismiss = { showAddBuildingDialog = false },
            onConfirm = { name, code, type, desc, address, lat, lng ->
                onCreateBuilding(name, code, type, desc, address, lat, lng)
                showAddBuildingDialog = false
            }
        )
    }

    if (showEditBuildingDialog != null) {
        EditBuildingDialog(
            building = showEditBuildingDialog!!,
            onDismiss = { showEditBuildingDialog = null },
            onConfirm = { updated ->
                onUpdateBuilding(updated)
                showEditBuildingDialog = null
            }
        )
    }

    if (showAddLocationDialog) {
        AddLocationDialog(
            building = currentBuilding,
            defaultFloor = activeStudioFloor,
            defaultX = pendingPlacementX,
            defaultY = pendingPlacementY,
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { name, code, cat, floor, desc, x, y, isAcc, isEmerg ->
                onAddLocation(name, code, cat, floor, desc, x, y, isAcc, isEmerg)
                showAddLocationDialog = false
            }
        )
    }

    if (showEditLocationDialog != null) {
        EditLocationDialog(
            node = showEditLocationDialog!!,
            building = currentBuilding,
            onDismiss = { showEditLocationDialog = null },
            onConfirm = { updated ->
                onUpdateLocation(updated)
                showEditLocationDialog = null
            }
        )
    }

    if (showAddEdgeDialog) {
        AddEdgeDialog(
            building = currentBuilding,
            onDismiss = { showAddEdgeDialog = false },
            onConfirm = { fromId, toId, dist, type ->
                onAddEdge(fromId, toId, dist, type, type != EdgeType.STAIRS && type != EdgeType.ESCALATOR, true)
                showAddEdgeDialog = false
            }
        )
    }

    if (showAddFloorDialog) {
        AddFloorDialog(
            currentMaxFloor = currentBuilding.floors.maxOfOrNull { it.floorNumber } ?: 0,
            onDismiss = { showAddFloorDialog = false },
            onConfirm = { floorNum, name, label, width, height, asset ->
                onAddFloor(floorNum, name, label, width, height, asset)
                showAddFloorDialog = false
            }
        )
    }

    if (showAddBeaconDialog) {
        AddBeaconDialog(
            building = currentBuilding,
            defaultFloor = activeStudioFloor,
            defaultX = pendingPlacementX,
            defaultY = pendingPlacementY,
            onDismiss = { showAddBeaconDialog = false },
            onConfirm = { beacon ->
                onAddBeacon(beacon)
                showAddBeaconDialog = false
            }
        )
    }

    if (showAddQrDialog) {
        AddQrPointDialog(
            building = currentBuilding,
            defaultFloor = activeStudioFloor,
            defaultX = pendingPlacementX,
            defaultY = pendingPlacementY,
            onDismiss = { showAddQrDialog = false },
            onConfirm = { qr ->
                onAddQrPoint(qr)
                showAddQrDialog = false
            }
        )
    }

    if (showQrPreviewDialog != null) {
        QrCodePreviewDialog(
            qrPoint = showQrPreviewDialog!!,
            onDismiss = { showQrPreviewDialog = null }
        )
    }
}

// -------------------------------------------------------------
// 1. ANALYTICS SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminAnalyticsSection(
    analyticsData: AnalyticsData,
    building: Building
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Total Buildings", "${analyticsData.totalBuildings}", Icons.Default.Apartment, Cyan400, Modifier.weight(1f))
                    StatCard("Total Floors", "${building.floors.size} Floors", Icons.Default.Layers, Purple500, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Total Locations", "${building.nodes.size} Nodes", Icons.Default.Place, Emerald400, Modifier.weight(1f))
                    StatCard("Active Graph Edges", "${building.edges.size} Paths", Icons.Default.AltRoute, Amber500, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Daily Requests", "${analyticsData.dailyNavigationRequests}", Icons.Default.TrendingUp, Cyan300, Modifier.weight(1f))
                    StatCard("Accessible Usage", "${analyticsData.accessibleRoutePercentage}%", Icons.Default.Accessible, Indigo400, Modifier.weight(1f))
                }
            }
        }

        // Graph Version & Architecture Health Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backend Graph Synchronization", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Surface(color = Emerald500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("ACTIVE v${building.graphVersion}", color = Emerald400, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        "Source of truth: Live MongoDB via Pathfinder REST API. All administrative modifications sync automatically across mobile clients.",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, Slate800, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Slate400
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. BUILDINGS SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminBuildingsSection(
    buildings: List<Building>,
    currentBuilding: Building,
    onSelectBuilding: (String) -> Unit,
    onOpenAddBuilding: () -> Unit,
    onEditBuilding: (Building) -> Unit,
    onDeleteBuilding: (String) -> Unit,
    onOpenAddFloor: () -> Unit,
    onDeleteFloor: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Managed Facilities (${buildings.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onOpenAddBuilding,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_add_building_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Slate950, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Building", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(buildings) { b ->
            val isActive = b.id == currentBuilding.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) Slate900 else Slate900.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isActive) Cyan500 else Slate800,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectBuilding(b.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = b.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isActive) {
                                Surface(
                                    color = Cyan500.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Cyan400,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEditBuilding(b) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate300, modifier = Modifier.size(16.dp))
                            }
                            if (buildings.size > 1) {
                                IconButton(
                                    onClick = { onDeleteBuilding(b.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Rose500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${b.address} • Graph v${b.graphVersion}",
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Floors: ${b.floors.size}", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.Medium)
                        Text("Rooms/Nodes: ${b.nodes.size}", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.Medium)
                        Text("Edges: ${b.edges.size}", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Active building floor management
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Floors in ${currentBuilding.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        OutlinedButton(
                            onClick = onOpenAddFloor,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Cyan400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Floor", color = Slate200, fontSize = 11.sp)
                        }
                    }

                    currentBuilding.floors.sortedBy { it.floorNumber }.forEach { floor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate800, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = Cyan400.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        floor.shortName.ifBlank { "L${floor.floorNumber}" },
                                        color = Cyan400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Column {
                                    Text(floor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("Floor Level ${floor.floorNumber}", fontSize = 11.sp, color = Slate400)
                                }
                            }

                            if (currentBuilding.floors.size > 1) {
                                IconButton(
                                    onClick = { onDeleteFloor(floor.floorNumber) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Floor", tint = Rose500, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. LOCATIONS / NODES SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminLocationsSection(
    building: Building,
    onOpenAddLocation: () -> Unit,
    onEditLocation: (NavNode) -> Unit,
    onDeleteLocation: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Navigation Nodes & Rooms (${building.nodes.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onOpenAddLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Slate950, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(building.nodes) { node ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = node.code,
                                color = Cyan400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column {
                            Text(
                                text = node.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Floor ${node.floor} • (${node.x.roundToInt()}, ${node.y.roundToInt()}) • ${node.category.name}",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onEditLocation(node) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate300, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDeleteLocation(node.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. EDGES SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminEdgesSection(
    building: Building,
    onOpenAddEdge: () -> Unit,
    onDeleteEdge: (String, String) -> Unit
) {
    val nodeMap = remember(building) { building.nodes.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Navigation Graph Edges (${building.edges.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onOpenAddEdge,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Slate950, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Edge", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(building.edges) { edge ->
            val fromNode = nodeMap[edge.fromId]
            val toNode = nodeMap[edge.toId]

            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(fromNode?.name ?: edge.fromId, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Cyan400, modifier = Modifier.size(14.dp))
                            Text(toNode?.name ?: edge.toId, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Text(
                            text = "${edge.distanceMeters}m • ${edge.edgeType.name} • ${if (edge.isWheelchairAccessible) "Accessible" else "Not Accessible"}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    IconButton(
                        onClick = { onDeleteEdge(edge.fromId, edge.toId) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. BEACONS SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminBeaconsSection(
    building: Building,
    beacons: List<Beacon>,
    onOpenAddBeacon: () -> Unit,
    onDeleteBeacon: (String) -> Unit,
    onSimulateBeacon: (String, Int) -> Unit
) {
    val buildingBeacons = remember(building, beacons) {
        beacons.filter { it.buildingId == building.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deployed BLE Beacons (${buildingBeacons.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onOpenAddBeacon,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deploy Beacon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(buildingBeacons) { beacon ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Indigo500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = Indigo400, modifier = Modifier.padding(8.dp).size(18.dp))
                        }
                        Column {
                            Text(beacon.name.ifBlank { beacon.beaconId }, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Floor ${beacon.floor} • (${beacon.x.roundToInt()}, ${beacon.y.roundToInt()}) • Tx: ${beacon.txPower} dBm", fontSize = 11.sp, color = Slate400)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onSimulateBeacon(beacon.beaconId, -62) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Simulate", tint = Emerald400, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteBeacon(beacon.beaconId) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. QR CALIBRATION SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminQrSection(
    building: Building,
    qrPoints: List<QrCalibrationPoint>,
    onOpenAddQr: () -> Unit,
    onPreviewQr: (QrCalibrationPoint) -> Unit,
    onDeleteQr: (String) -> Unit
) {
    val buildingQr = remember(building, qrPoints) {
        qrPoints.filter { it.buildingId == building.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QR Calibration Anchors (${buildingQr.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = onOpenAddQr,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple500),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add QR Anchor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(buildingQr) { qr ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Purple500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Purple400, modifier = Modifier.padding(8.dp).size(18.dp))
                        }
                        Column {
                            Text(qr.calibrationId, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Floor ${qr.floorId} • Node: ${qr.nodeId} • (${qr.x.roundToInt()}, ${qr.y.roundToInt()})", fontSize = 11.sp, color = Slate400)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onPreviewQr(qr) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "View QR", tint = Cyan400, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDeleteQr(qr.calibrationId) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. GRAPH VALIDATION & PUBLISH SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminGraphValidationSection(
    building: Building,
    onRunValidation: () -> GraphValidationReport,
    onPublishBuilding: ((Result<GraphValidationReport>) -> Unit) -> Unit,
    report: GraphValidationReport?
) {
    var validationReport by remember { mutableStateOf(report ?: onRunValidation()) }
    var publishStatus by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (validationReport.isValid) Emerald500.copy(alpha = 0.5f) else Red500.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (validationReport.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (validationReport.isValid) Emerald400 else Red400,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (validationReport.isValid) "Graph Integrity Valid" else "Graph Validation Errors",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                validationReport = onRunValidation()
                                publishStatus = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Cyan400)
                            Spacer(Modifier.width(4.dp))
                            Text("Re-validate", fontSize = 11.sp, color = Slate200)
                        }
                    }

                    // Metrics Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Slate800, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Nodes", fontSize = 10.sp, color = Slate400)
                                Text("${validationReport.totalNodes}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Surface(color = Slate800, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Edges", fontSize = 10.sp, color = Slate400)
                                Text("${validationReport.totalEdges}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Surface(color = Slate800, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Accessible", fontSize = 10.sp, color = Slate400)
                                Text("${validationReport.accessibleEdgesCount}", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 14.sp)
                            }
                        }
                        Surface(color = Slate800, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Elevators", fontSize = 10.sp, color = Slate400)
                                Text("${validationReport.elevatorTransitionsCount}", fontWeight = FontWeight.Bold, color = Amber400, fontSize = 14.sp)
                            }
                        }
                    }

                    if (validationReport.errors.isNotEmpty()) {
                        Divider(color = Slate800)
                        Text("Blocking Errors:", fontWeight = FontWeight.Bold, color = Red400, fontSize = 12.sp)
                        validationReport.errors.forEach { err ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(6.dp).background(Red400, CircleShape))
                                Text(err, color = Slate200, fontSize = 11.sp)
                            }
                        }
                    }

                    if (validationReport.warnings.isNotEmpty()) {
                        Divider(color = Slate800)
                        Text("Warnings:", fontWeight = FontWeight.Bold, color = Amber400, fontSize = 12.sp)
                        validationReport.warnings.forEach { warn ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(6.dp).background(Amber400, CircleShape))
                                Text(warn, color = Slate300, fontSize = 11.sp)
                            }
                        }
                    }

                    Divider(color = Slate800)

                    // Publish Button
                    Button(
                        onClick = {
                            isPublishing = true
                            onPublishBuilding { res ->
                                isPublishing = false
                                if (res.isSuccess) {
                                    publishStatus = "Successfully published and synchronized graph v${building.graphVersion + 1} to MongoDB."
                                } else {
                                    publishStatus = "Publish error: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = validationReport.isValid && !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPublishing) "Publishing..." else "Publish to Live Backend (MongoDB)", fontWeight = FontWeight.Bold)
                    }

                    if (publishStatus != null) {
                        Text(publishStatus!!, color = if (publishStatus!!.startsWith("Success")) Emerald400 else Amber400, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. POSITIONING & SENSORS SUB-TAB
// -------------------------------------------------------------
@Composable
fun AdminPositioningSection(
    building: Building,
    positioningMode: PositioningMode,
    onSetPositioningMode: (PositioningMode) -> Unit,
    onTriggerSimulatedStep: () -> Unit,
    onSimulateGps: (Double, Double) -> Unit,
    locationState: NavigationLocationState?,
    currentIndoorPosition: IndoorPosition?,
    deviceHeadingDegrees: Float
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Positioning Engine Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PositioningMode.values()) { mode ->
                            FilterChip(
                                selected = positioningMode == mode,
                                onClick = { onSetPositioningMode(mode) },
                                label = { Text(mode.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Telemetry & Live Status", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Heading: ${"%.1f".format(deviceHeadingDegrees)}°", fontSize = 12.sp, color = Cyan400)
                    Text(
                        text = if (currentIndoorPosition != null) "Fused: (${"%.1f".format(currentIndoorPosition.x)}, ${"%.1f".format(currentIndoorPosition.y)}) Floor ${currentIndoorPosition.floorId}" else "Indoor position: Uncalibrated",
                        fontSize = 12.sp,
                        color = Slate200
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Live Simulation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Button(
                        onClick = onTriggerSimulatedStep,
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Step Forward (PDR)", color = Slate950, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onSimulateGps(building.latitude, building.longitude) },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("GPS at Entrance", fontSize = 11.sp, color = Cyan300)
                        }
                        Button(
                            onClick = { onSimulateGps(building.latitude + 0.005, building.longitude + 0.005) },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("GPS 500m Away", fontSize = 11.sp, color = Slate300)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG IMPLEMENTATIONS
// -------------------------------------------------------------

@Composable
fun AddBuildingDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, type: String, desc: String, address: String, lat: Double, lng: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("University") }
    var desc by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Campus Main Blvd") }
    var latStr by remember { mutableStateOf("12.9716") }
    var lngStr by remember { mutableStateOf("77.5946") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Add New Facility", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Building Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Facility Code (e.g. BLD_ENG)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. University, Hospital, Airport)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull() ?: 12.9716
                    val lng = lngStr.toDoubleOrNull() ?: 77.5946
                    if (name.isNotBlank()) {
                        onConfirm(name, code, type, desc, address, lat, lng)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Create Building")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun EditBuildingDialog(
    building: Building,
    onDismiss: () -> Unit,
    onConfirm: (Building) -> Unit
) {
    var name by remember { mutableStateOf(building.name) }
    var type by remember { mutableStateOf(building.type) }
    var desc by remember { mutableStateOf(building.description) }
    var address by remember { mutableStateOf(building.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Edit Facility Details", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Building Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(building.copy(name = name, type = type, description = desc, address = address))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun AddLocationDialog(
    building: Building,
    defaultFloor: Int = 0,
    defaultX: Float = 500f,
    defaultY: Float = 500f,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, cat: NodeCategory, floor: Int, desc: String, x: Float, y: Float, isAccessible: Boolean, isEmergency: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NodeCategory.CLASSROOM) }
    var floorStr by remember { mutableStateOf(defaultFloor.toString()) }
    var desc by remember { mutableStateOf("") }
    var xStr by remember { mutableStateOf(defaultX.roundToInt().toString()) }
    var yStr by remember { mutableStateOf(defaultY.roundToInt().toString()) }
    var isAccessible by remember { mutableStateOf(true) }
    var isEmergency by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Add Room or Node", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Node / Room Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g. LAB-201)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = floorStr, onValueChange = { floorStr = it }, label = { Text("Floor") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = xStr, onValueChange = { xStr = it }, label = { Text("X Coordinate (0-1000)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = yStr, onValueChange = { yStr = it }, label = { Text("Y Coordinate (0-1000)") }, modifier = Modifier.weight(1f))
                }
                Text("Category", fontSize = 12.sp, color = Slate400, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NodeCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name, fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAccessible, onCheckedChange = { isAccessible = it })
                    Text("Wheelchair Accessible", fontSize = 12.sp, color = Slate200)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = floorStr.toIntOrNull() ?: 0
                    val x = xStr.toFloatOrNull() ?: defaultX
                    val y = yStr.toFloatOrNull() ?: defaultY
                    if (name.isNotBlank()) {
                        onConfirm(name, code, selectedCategory, f, desc, x, y, isAccessible, isEmergency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Add Node")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun EditLocationDialog(
    node: NavNode,
    building: Building,
    onDismiss: () -> Unit,
    onConfirm: (NavNode) -> Unit
) {
    var name by remember { mutableStateOf(node.name) }
    var code by remember { mutableStateOf(node.code) }
    var desc by remember { mutableStateOf(node.description) }
    var xStr by remember { mutableStateOf(node.x.roundToInt().toString()) }
    var yStr by remember { mutableStateOf(node.y.roundToInt().toString()) }
    var isAccessible by remember { mutableStateOf(node.isAccessible) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Edit Navigation Node", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = xStr, onValueChange = { xStr = it }, label = { Text("X (0-1000)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = yStr, onValueChange = { yStr = it }, label = { Text("Y (0-1000)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAccessible, onCheckedChange = { isAccessible = it })
                    Text("Wheelchair Accessible", fontSize = 12.sp, color = Slate200)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val x = xStr.toFloatOrNull() ?: node.x
                    val y = yStr.toFloatOrNull() ?: node.y
                    onConfirm(node.copy(name = name, code = code, description = desc, x = x, y = y, isAccessible = isAccessible))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun AddEdgeDialog(
    building: Building,
    onDismiss: () -> Unit,
    onConfirm: (fromId: String, toId: String, dist: Float, type: EdgeType) -> Unit
) {
    var fromId by remember { mutableStateOf(building.nodes.firstOrNull()?.id ?: "") }
    var toId by remember { mutableStateOf(building.nodes.getOrNull(1)?.id ?: "") }
    var distStr by remember { mutableStateOf("15.0") }
    var edgeType by remember { mutableStateOf(EdgeType.CORRIDOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Create Navigation Edge", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("From Node", fontSize = 12.sp, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(building.nodes) { n ->
                        FilterChip(selected = fromId == n.id, onClick = { fromId = n.id }, label = { Text(n.name, fontSize = 11.sp) })
                    }
                }
                Text("To Node", fontSize = 12.sp, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(building.nodes) { n ->
                        FilterChip(selected = toId == n.id, onClick = { toId = n.id }, label = { Text(n.name, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = distStr, onValueChange = { distStr = it }, label = { Text("Distance (meters)") }, modifier = Modifier.fillMaxWidth())
                Text("Edge Type", fontSize = 12.sp, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EdgeType.values()) { t ->
                        FilterChip(selected = edgeType == t, onClick = { edgeType = t }, label = { Text(t.name, fontSize = 10.sp) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = distStr.toFloatOrNull() ?: 10f
                    if (fromId.isNotBlank() && toId.isNotBlank() && fromId != toId) {
                        onConfirm(fromId, toId, d, edgeType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Create Edge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun AddFloorDialog(
    currentMaxFloor: Int,
    onDismiss: () -> Unit,
    onConfirm: (floorNum: Int, name: String, label: String, width: Float, height: Float, asset: String) -> Unit
) {
    var floorNumStr by remember { mutableStateOf((currentMaxFloor + 1).toString()) }
    var name by remember { mutableStateOf("Floor ${currentMaxFloor + 1}") }
    var label by remember { mutableStateOf("L${currentMaxFloor + 1}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Add Floor Level", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = floorNumStr, onValueChange = { floorNumStr = it }, label = { Text("Floor Number (e.g. 0, 1, 2, -1)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Floor Display Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Short Label (e.g. G, L1, B1)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fn = floorNumStr.toIntOrNull() ?: (currentMaxFloor + 1)
                    onConfirm(fn, name, label, 1000f, 1000f, "blueprint_vector")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Add Floor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun AddBeaconDialog(
    building: Building,
    defaultFloor: Int = 0,
    defaultX: Float = 500f,
    defaultY: Float = 500f,
    onDismiss: () -> Unit,
    onConfirm: (Beacon) -> Unit
) {
    var beaconId by remember { mutableStateOf("BEACON-${building.id.take(4).uppercase()}-${System.currentTimeMillis() % 1000}") }
    var name by remember { mutableStateOf("Anchor Beacon") }
    var floorStr by remember { mutableStateOf(defaultFloor.toString()) }
    var xStr by remember { mutableStateOf(defaultX.roundToInt().toString()) }
    var yStr by remember { mutableStateOf(defaultY.roundToInt().toString()) }
    var txPowerStr by remember { mutableStateOf("-59") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Deploy BLE Anchor Beacon", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = beaconId, onValueChange = { beaconId = it }, label = { Text("Beacon ID (UUID/MAC)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = floorStr, onValueChange = { floorStr = it }, label = { Text("Floor") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = txPowerStr, onValueChange = { txPowerStr = it }, label = { Text("TxPower (dBm)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = xStr, onValueChange = { xStr = it }, label = { Text("X (0-1000)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = yStr, onValueChange = { yStr = it }, label = { Text("Y (0-1000)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = floorStr.toIntOrNull() ?: defaultFloor
                    val x = xStr.toFloatOrNull() ?: defaultX
                    val y = yStr.toFloatOrNull() ?: defaultY
                    val tx = txPowerStr.toIntOrNull() ?: -59
                    if (beaconId.isNotBlank()) {
                        onConfirm(Beacon(beaconId = beaconId.trim(), buildingId = building.id, floorId = f, x = x, y = y, name = name.trim(), txPower = tx))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Deploy Anchor", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun AddQrPointDialog(
    building: Building,
    defaultFloor: Int = 0,
    defaultX: Float = 500f,
    defaultY: Float = 500f,
    onDismiss: () -> Unit,
    onConfirm: (QrCalibrationPoint) -> Unit
) {
    var calId by remember { mutableStateOf("QR-${building.id.take(4).uppercase()}-${System.currentTimeMillis() % 1000}") }
    var floorStr by remember { mutableStateOf(defaultFloor.toString()) }
    var xStr by remember { mutableStateOf(defaultX.roundToInt().toString()) }
    var yStr by remember { mutableStateOf(defaultY.roundToInt().toString()) }
    var selectedNodeId by remember { mutableStateOf(building.nodes.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Add QR Calibration Anchor", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = calId, onValueChange = { calId = it }, label = { Text("QR Code ID / Payload") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = floorStr, onValueChange = { floorStr = it }, label = { Text("Floor") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = xStr, onValueChange = { xStr = it }, label = { Text("X (0-1000)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = yStr, onValueChange = { yStr = it }, label = { Text("Y (0-1000)") }, modifier = Modifier.weight(1f))
                }
                Text("Anchor Node Reference", fontSize = 12.sp, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(building.nodes) { n ->
                        FilterChip(selected = selectedNodeId == n.id, onClick = { selectedNodeId = n.id }, label = { Text(n.name, fontSize = 11.sp) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = floorStr.toIntOrNull() ?: defaultFloor
                    val x = xStr.toFloatOrNull() ?: defaultX
                    val y = yStr.toFloatOrNull() ?: defaultY
                    if (calId.isNotBlank()) {
                        onConfirm(QrCalibrationPoint(calibrationId = calId.trim(), buildingId = building.id, floorId = f, nodeId = selectedNodeId, x = x, y = y))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Purple500)
            ) {
                Text("Create QR Anchor", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate300) }
        }
    )
}

@Composable
fun QrCodePreviewDialog(
    qrPoint: QrCalibrationPoint,
    onDismiss: () -> Unit
) {
    val payload = remember(qrPoint) {
        "PATHFINDER_CALIBRATION\n${qrPoint.buildingId}\n${qrPoint.floorId}\n${qrPoint.nodeId}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Cyan400)
                Text("QR Calibration Card", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simulated High-Contrast QR Code Visual Pattern
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(160.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Box(Modifier.size(32.dp).background(Color.Black))
                                Box(Modifier.size(16.dp).background(Color.Black))
                                Box(Modifier.size(32.dp).background(Color.Black))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                Box(Modifier.size(14.dp).background(Color.Black))
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                Box(Modifier.size(14.dp).background(Color.Black))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Box(Modifier.size(32.dp).background(Color.Black))
                                Box(Modifier.size(16.dp).background(Color.Black))
                                Box(Modifier.size(32.dp).background(Color.Black))
                            }
                        }
                    }
                }

                Surface(color = Slate800, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("QR Payload Format:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Text(payload, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Cyan300)
                    }
                }

                Text("Anchor Node: ${qrPoint.nodeId} (Floor ${qrPoint.floorId})", fontSize = 12.sp, color = Slate300)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
            ) {
                Text("Done")
            }
        }
    )
}
