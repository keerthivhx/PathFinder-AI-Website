package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.positioning.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentBuilding by viewModel.currentBuilding.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    val activeFloor by viewModel.activeFloor.collectAsStateWithLifecycle()
    val startNode by viewModel.startNode.collectAsStateWithLifecycle()
    val destinationNode by viewModel.destinationNode.collectAsStateWithLifecycle()
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val activeStepIndex by viewModel.activeStepIndex.collectAsStateWithLifecycle()
    val navSessionState by viewModel.navSessionState.collectAsStateWithLifecycle()
    val isNorthUp by viewModel.isNorthUp.collectAsStateWithLifecycle()

    val isWheelchairMode by viewModel.isWheelchairMode.collectAsStateWithLifecycle()
    val isEmergencyMode by viewModel.isEmergencyMode.collectAsStateWithLifecycle()
    val isARMode by viewModel.isARMode.collectAsStateWithLifecycle()
    val isAILoading by viewModel.isAILoading.collectAsStateWithLifecycle()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsStateWithLifecycle()
    val lastLandmarkResult by viewModel.lastLandmarkResult.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // Phase 12 Analytics Flows
    val analyticsOverview by viewModel.analyticsOverview.collectAsStateWithLifecycle()
    val topDestinations by viewModel.topDestinations.collectAsStateWithLifecycle()
    val searchDemand by viewModel.searchDemand.collectAsStateWithLifecycle()
    val zeroResultSearches by viewModel.zeroResultSearches.collectAsStateWithLifecycle()
    val positioningDistribution by viewModel.positioningDistribution.collectAsStateWithLifecycle()
    val floorActivity by viewModel.floorActivity.collectAsStateWithLifecycle()
    val problematicLocations by viewModel.problematicLocations.collectAsStateWithLifecycle()
    val adminAlerts by viewModel.adminAlerts.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val dateTrends by viewModel.dateTrends.collectAsStateWithLifecycle()
    val selectedAnalyticsBuildingId by viewModel.selectedAnalyticsBuildingId.collectAsStateWithLifecycle()
    val analyticsDateRange by viewModel.analyticsDateRange.collectAsStateWithLifecycle()
    val analyticsResultFilter by viewModel.analyticsResultFilter.collectAsStateWithLifecycle()
    val analyticsAccessibilityFilter by viewModel.analyticsAccessibilityFilter.collectAsStateWithLifecycle()
    val includeDemoSessions by viewModel.includeDemoSessions.collectAsStateWithLifecycle()
    val analyticsLastUpdated by viewModel.analyticsLastUpdated.collectAsStateWithLifecycle()

    // Positioning and Sensors flows (Phases 5, 6, 7, 8)
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val currentIndoorPosition by viewModel.currentIndoorPosition.collectAsStateWithLifecycle()
    val deviceHeadingDegrees by viewModel.deviceHeadingDegrees.collectAsStateWithLifecycle()
    val positioningMode by viewModel.positioningMode.collectAsStateWithLifecycle()
    val registeredBeacons by viewModel.registeredBeacons.collectAsStateWithLifecycle()
    val registeredQrPoints by viewModel.registeredQrPoints.collectAsStateWithLifecycle()
    val graphValidationReport by viewModel.graphValidationReport.collectAsStateWithLifecycle()
    val showPositionDebugOverlay by viewModel.showPositionDebugOverlay.collectAsStateWithLifecycle()
    val fusionDiagnostics by viewModel.fusionDiagnostics.collectAsStateWithLifecycle()

    // Dialog state
    val selectedNodeDetail by viewModel.selectedNodeDetail.collectAsStateWithLifecycle()
    val showBuildingSheet by viewModel.showBuildingSheet.collectAsStateWithLifecycle()
    val showLandmarkDialog by viewModel.showLandmarkDialog.collectAsStateWithLifecycle()
    val showQRDialog by viewModel.showQRDialog.collectAsStateWithLifecycle()
    val showPitchDialog by viewModel.showPitchDialog.collectAsStateWithLifecycle()
    val showAuthDialog by viewModel.showAuthDialog.collectAsStateWithLifecycle()
    val showMapBuilder by viewModel.showMapBuilder.collectAsStateWithLifecycle()

    val isMuted by viewModel.voiceEngine.isMuted.collectAsStateWithLifecycle()

    var showSidebarDrawer by remember { mutableStateOf(false) }
    var showRouteDetailsDrawer by remember { mutableStateOf(false) }
    var showBackendConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // -------------------------------------------------------------
        // TOP APPLICATION BAR & NAVIGATION TABS
        // -------------------------------------------------------------
        Surface(
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo & Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { viewModel.setTab(AppTab.LANDING) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Cyan500, Purple500)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Logo",
                                tint = Slate950,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PATHFINDER",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "AI",
                                    color = Cyan400,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "Smart Indoor Wayfinding",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Facility Selector & Role / Auth Profile button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Facility picker chip
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                            modifier = Modifier
                                .clickable { viewModel.setShowBuildingSheet(true) }
                                .testTag("top_building_selector_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = currentBuilding.name.take(14) + if (currentBuilding.name.length > 14) "…" else "",
                                    color = Slate200,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Backend Live Sync Status Chip
                        Surface(
                            color = when (syncState) {
                                is NetworkSyncState.Synced -> Emerald500.copy(alpha = 0.15f)
                                is NetworkSyncState.Loading -> Cyan500.copy(alpha = 0.15f)
                                else -> Slate800
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (syncState) {
                                    is NetworkSyncState.Synced -> Emerald500.copy(alpha = 0.5f)
                                    is NetworkSyncState.Loading -> Cyan500.copy(alpha = 0.5f)
                                    else -> Slate700
                                }
                            ),
                            modifier = Modifier
                                .clickable {
                                    if (syncState is NetworkSyncState.OfflineFallback || syncState is NetworkSyncState.Error) {
                                        showBackendConfigDialog = true
                                    } else {
                                        viewModel.syncBackendData()
                                    }
                                }
                                .testTag("backend_sync_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                when (syncState) {
                                    is NetworkSyncState.Loading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = Cyan400
                                        )
                                    }
                                    is NetworkSyncState.Synced -> {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Backend Live",
                                            tint = Emerald400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Sync Backend",
                                            tint = Slate400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when (syncState) {
                                        is NetworkSyncState.Loading -> "Syncing…"
                                        is NetworkSyncState.Synced -> "Live"
                                        is NetworkSyncState.OfflineFallback -> "Offline"
                                        else -> "Sync"
                                    },
                                    color = when (syncState) {
                                        is NetworkSyncState.Synced -> Emerald300
                                        is NetworkSyncState.Loading -> Cyan300
                                        else -> Slate400
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure Backend URL",
                                    tint = Slate400,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { showBackendConfigDialog = true }
                                )
                            }
                        }

                        // Voice Mute Button
                        IconButton(
                            onClick = { viewModel.voiceEngine.toggleMute() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Toggle Mute",
                                tint = if (isMuted) Slate500 else Cyan400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Auth & Profile Chip
                        Surface(
                            color = if (userAccount.role == "ADMIN") Amber500.copy(alpha = 0.2f) else Slate800,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (userAccount.role == "ADMIN") Amber500 else Slate700
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setShowAuthDialog(true) }
                                .testTag("auth_profile_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (userAccount.role == "ADMIN") Amber500 else Cyan500),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (userAccount.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Slate950,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = userAccount.role,
                                    color = if (userAccount.role == "ADMIN") Amber400 else Slate200,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Tab Bar
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = Slate900,
                    contentColor = Cyan400,
                    divider = { HorizontalDivider(color = Slate800) },
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                            color = Cyan400,
                            height = 3.dp
                        )
                    }
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            modifier = Modifier.testTag("app_tab_${tab.name.lowercase()}"),
                            text = {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Cyan400 else Slate400
                                )
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // MAIN BODY (TAB SWITCHER)
        // -------------------------------------------------------------
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                AppTab.LANDING -> {
                    LandingPageScreen(
                        onStartNavigation = { viewModel.setTab(AppTab.NAVIGATION) },
                        onOpenAIAssistant = { viewModel.setTab(AppTab.AI_ASSISTANT) },
                        onOpenAdmin = { viewModel.setTab(AppTab.ADMIN) },
                        onSelectBuilding = { id -> viewModel.selectBuilding(id) }
                    )
                }

                AppTab.AI_ASSISTANT -> {
                    AIAssistantScreen(
                        building = currentBuilding,
                        chatMessages = chatMessages,
                        isLoading = isAILoading,
                        navigationContext = viewModel.buildAINavigationContext(),
                        onSendMessage = { prompt -> viewModel.sendAssistantMessage(prompt) },
                        onNavigateToNode = { targetNode ->
                            viewModel.startNavigationWithDestination(targetNode)
                        },
                        onSpeakText = { text -> viewModel.voiceEngine.speak(text, force = true) },
                        onScanQr = { viewModel.setTab(AppTab.NAVIGATION) }
                    )
                }

                AppTab.ADMIN -> {
                    AdminDashboardScreen(
                        buildings = buildings,
                        currentBuilding = currentBuilding,
                        analyticsData = analyticsData,
                        onSelectBuilding = { id -> viewModel.selectBuilding(id) },
                        onCreateBuilding = { name, code, type, desc, address, lat, lng ->
                            viewModel.createBuilding(name, type, desc, code, address, lat, lng)
                        },
                        onUpdateBuilding = { b -> viewModel.updateBuilding(b) },
                        onDeleteBuilding = { id, cascade -> viewModel.deleteBuilding(id, cascade) },
                        onAddLocation = { name, code, cat, floor, desc, x, y, isAcc, isEmerg ->
                            viewModel.addLocation(name, code, cat, floor, desc, x, y, isAcc, isEmerg)
                        },
                        onUpdateLocation = { node -> viewModel.updateNavNode(node) },
                        onDeleteLocation = { id, cascade -> viewModel.deleteLocation(id, cascade) },
                        onAddEdge = { from, to, dist, type, isAcc, bi ->
                            viewModel.addEdge(from, to, dist, type, isAcc, bi)
                        },
                        onDeleteEdge = { from, to -> viewModel.deleteEdge(from, to) },
                        onAddFloor = { fNum, name, label, width, height, asset ->
                            viewModel.addFloor(fNum, name, label, width, height, 1.0f, asset)
                        },
                        onUpdateFloor = { floor -> viewModel.updateFloor(floor) },
                        onDeleteFloor = { floorNum, cascade -> viewModel.deleteFloor(floorNum, cascade) },
                        beacons = registeredBeacons,
                        qrPoints = registeredQrPoints,
                        onAddBeacon = { b -> viewModel.addBeacon(b) },
                        onUpdateBeacon = { b -> viewModel.updateBeacon(b) },
                        onDeleteBeacon = { id -> viewModel.deleteBeacon(id) },
                        onAddQrPoint = { qr -> viewModel.addQrPoint(qr) },
                        onUpdateQrPoint = { qr -> viewModel.updateQrPoint(qr) },
                        onDeleteQrPoint = { id -> viewModel.deleteQrPoint(id) },
                        onRunGraphValidation = { id -> viewModel.runGraphValidation(id) },
                        onPublishBuilding = { id, cb -> viewModel.publishAndSynchronizeBuilding(id, cb) },
                        validationReport = graphValidationReport,
                        positioningMode = positioningMode,
                        onSetPositioningMode = { mode -> viewModel.setPositioningMode(mode) },
                        onSimulateBeacon = { id, rssi -> viewModel.simulateBeaconSignal(id, rssi) },
                        onTriggerSimulatedStep = { viewModel.triggerSimulatedStep() },
                        onSimulateGps = { lat, lng -> viewModel.simulateGpsLocation(lat, lng) },
                        locationState = locationState,
                        currentIndoorPosition = currentIndoorPosition,
                        deviceHeadingDegrees = deviceHeadingDegrees,
                        // Phase 12 Analytics
                        analyticsOverview = analyticsOverview,
                        topDestinations = topDestinations,
                        searchDemand = searchDemand,
                        zeroResultSearches = zeroResultSearches,
                        positioningDistribution = positioningDistribution,
                        floorActivity = floorActivity,
                        problematicLocations = problematicLocations,
                        adminAlerts = adminAlerts,
                        recentSessions = recentSessions,
                        dateTrends = dateTrends,
                        selectedAnalyticsBuildingId = selectedAnalyticsBuildingId,
                        onSelectAnalyticsBuilding = { id -> viewModel.setAnalyticsBuilding(id) },
                        analyticsDateRange = analyticsDateRange,
                        onSelectAnalyticsDateRange = { range -> viewModel.setAnalyticsDateRange(range) },
                        analyticsResultFilter = analyticsResultFilter,
                        onSelectAnalyticsResultFilter = { f -> viewModel.setAnalyticsResultFilter(f) },
                        analyticsAccessibilityFilter = analyticsAccessibilityFilter,
                        onSelectAnalyticsAccessibilityFilter = { af -> viewModel.setAnalyticsAccessibilityFilter(af) },
                        includeDemoSessions = includeDemoSessions,
                        onToggleIncludeDemoSessions = { inc -> viewModel.setIncludeDemoSessions(inc) },
                        analyticsLastUpdated = analyticsLastUpdated,
                        onRefreshAnalytics = { viewModel.refreshAnalytics() },
                        onClearDemoData = { viewModel.clearDemoAnalytics() }
                    )
                }

                AppTab.NAVIGATION -> {
                    if (isARMode) {
                        // Fullscreen Spatial AR Camera HUD View
                        ARCameraHUDView(
                            building = currentBuilding,
                            activeRoute = activeRoute,
                            activeStepIndex = activeStepIndex,
                            onExitAR = { viewModel.toggleARMode() }
                        )
                    } else if (navSessionState == NavSessionState.SEARCHING) {
                        // Phase 10 Full-Featured Destination Search View
                        RealDestinationSearchView(
                            building = currentBuilding,
                            currentFloor = activeFloor,
                            userPositionX = currentIndoorPosition?.x,
                            userPositionY = currentIndoorPosition?.y,
                            favoriteNodeIds = userAccount.favoriteNodeIds,
                            recentNodeIds = userAccount.recentNodeIds,
                            onSelectDestination = { node ->
                                viewModel.selectDestination(node)
                            },
                            onToggleFavorite = { nodeId ->
                                viewModel.toggleFavorite(nodeId)
                            },
                            onClose = {
                                viewModel.cancelSearching()
                            },
                            onSearchPerformed = { query, count, cat ->
                                viewModel.recordSearchEvent(query, count, category = cat?.name)
                            }
                        )
                    } else {
                        // 2D Interactive Multi-Floor Map Canvas & Navigation Lifecycle
                        Box(modifier = Modifier.fillMaxSize()) {
                            IndoorMapCanvas(
                                building = currentBuilding,
                                activeFloor = activeFloor,
                                startNode = startNode,
                                destinationNode = destinationNode,
                                activeRoute = activeRoute,
                                activeStepIndex = activeStepIndex,
                                onNodeClicked = { node ->
                                    viewModel.selectDestination(node)
                                },
                                indoorPosition = currentIndoorPosition,
                                deviceHeadingDegrees = deviceHeadingDegrees,
                                beacons = registeredBeacons,
                                showBeacons = true,
                                modifier = Modifier.fillMaxSize()
                            )

                            // -------------------------------------------------------------
                            // TOP OVERLAY: SEARCH BAR OR TURN-BY-TURN BANNER
                            // -------------------------------------------------------------
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            ) {
                                val isNavigatingActive = navSessionState in listOf(
                                    NavSessionState.NAVIGATING,
                                    NavSessionState.PAUSED,
                                    NavSessionState.OFF_ROUTE,
                                    NavSessionState.RECALCULATING,
                                    NavSessionState.FLOOR_TRANSITION
                                )

                                if (isNavigatingActive && activeRoute != null) {
                                    TurnByTurnBanner(
                                        route = activeRoute!!,
                                        activeStepIndex = activeStepIndex,
                                        isMuted = isMuted,
                                        onToggleMute = { viewModel.voiceEngine.toggleMute() },
                                        onReplayVoice = { viewModel.replayVoiceInstruction() },
                                        onNextStep = { viewModel.nextStep() },
                                        onPrevStep = { viewModel.prevStep() },
                                        onCancelRoute = { viewModel.stopNavigation() },
                                        onToggleAR = { viewModel.toggleARMode() },
                                        isARActive = isARMode,
                                        distanceToNextActionMeters = viewModel.getDynamicDistanceToNextAction()
                                    )
                                } else {
                                    // Google Maps-Style Top Floating Search Bar
                                    Surface(
                                        color = Slate900.copy(alpha = 0.94f),
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clickable { viewModel.startSearching() }
                                            .testTag("top_maps_search_bar")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = Cyan400,
                                                modifier = Modifier.size(20.dp)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (destinationNode != null) destinationNode!!.name else "Search destination, room, lab…",
                                                    color = if (destinationNode != null) Color.White else Slate300,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (destinationNode != null) FontWeight.Bold else FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${currentBuilding.name} • Floor $activeFloor",
                                                    color = Slate400,
                                                    fontSize = 10.sp
                                                )
                                            }

                                            // Quick Wheelchair Filter Icon
                                            IconButton(
                                                onClick = { viewModel.toggleWheelchairMode() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Accessible,
                                                    contentDescription = "Accessible Path",
                                                    tint = if (isWheelchairMode) Cyan400 else Slate500,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Quick Voice Search Icon
                                            IconButton(
                                                onClick = { viewModel.startSearching() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice Search",
                                                    tint = Cyan400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Live Positioning Status Pill with Debug Toggle
                                Surface(
                                    color = Slate900.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when (locationState) {
                                            is NavigationLocationState.OffRoute -> Red500.copy(alpha = 0.6f)
                                            is NavigationLocationState.FloorTransition,
                                            is NavigationLocationState.AtFloorTransition -> Amber500.copy(alpha = 0.6f)
                                            is NavigationLocationState.Arrived -> Emerald400.copy(alpha = 0.6f)
                                            else -> Cyan500.copy(alpha = 0.45f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 4.dp)
                                        .clickable { viewModel.togglePositionDebugOverlay() }
                                        .testTag("positioning_status_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (locationState) {
                                                        is NavigationLocationState.OffRoute -> Red500
                                                        is NavigationLocationState.FloorTransition,
                                                        is NavigationLocationState.AtFloorTransition -> Amber500
                                                        is NavigationLocationState.Arrived -> Emerald400
                                                        else -> Cyan400
                                                    }
                                                )
                                        )
                                        Text(
                                            text = when (locationState) {
                                                is NavigationLocationState.OffRoute -> "Off Route • Recalculating"
                                                is NavigationLocationState.FloorTransition -> "Change floor to L${(locationState as NavigationLocationState.FloorTransition).toFloor}"
                                                is NavigationLocationState.AtFloorTransition -> "Take ${(locationState as NavigationLocationState.AtFloorTransition).transitionType.lowercase()} to Floor ${(locationState as NavigationLocationState.AtFloorTransition).targetFloor}"
                                                is NavigationLocationState.ApproachingEntrance -> "Approaching ${(locationState as NavigationLocationState.ApproachingEntrance).entrance.name} (Switching Indoor)"
                                                is NavigationLocationState.Arrived -> "Arrived at Destination"
                                                else -> {
                                                    val prov = currentIndoorPosition?.source?.label ?: currentIndoorPosition?.provider?.name ?: positioningMode.name
                                                    val acc = currentIndoorPosition?.accuracyMeters ?: 2.5f
                                                    "$prov • ±${"%.1f".format(acc)}m • ${deviceHeadingDegrees.toInt()}°"
                                                }
                                            },
                                            color = Slate200,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (showPositionDebugOverlay) Icons.Default.BugReport else Icons.Default.Analytics,
                                            contentDescription = "Debug Fusion",
                                            tint = if (showPositionDebugOverlay) Cyan400 else Slate400,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // DEVELOPER SENSOR FUSION & POSITIONING DEBUG OVERLAY
                            // -------------------------------------------------------------
                            if (showPositionDebugOverlay) {
                                Surface(
                                    color = Slate950.copy(alpha = 0.94f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.6f)),
                                    shadowElevation = 10.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 12.dp, top = 80.dp)
                                        .widthIn(max = 240.dp)
                                        .testTag("fusion_debug_overlay")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("FUSION TELEMETRY", color = Cyan400, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Debug",
                                                tint = Slate400,
                                                modifier = Modifier.size(14.dp).clickable { viewModel.togglePositionDebugOverlay() }
                                            )
                                        }
                                        HorizontalDivider(color = Slate800, thickness = 1.dp)

                                        val cur = currentIndoorPosition
                                        Text("Source: ${cur?.source?.label ?: "NONE"}", color = Slate300, fontSize = 9.sp)
                                        Text("Pos: (${cur?.x?.toInt() ?: 0}, ${cur?.y?.toInt() ?: 0}) L${cur?.floorId ?: activeFloor}", color = Slate300, fontSize = 9.sp)
                                        Text("Heading: ${deviceHeadingDegrees.toInt()}° (Smoothed)", color = Slate300, fontSize = 9.sp)
                                        Text("Confidence: ${cur?.confidence?.name ?: "LOW"} (${"%.2f".format(cur?.confidenceScore ?: 0f)})", color = Slate300, fontSize = 9.sp)
                                        Text("Beacons in range: ${fusionDiagnostics.activeBeaconCount}", color = Slate300, fontSize = 9.sp)
                                        Text("PDR Steps: ${fusionDiagnostics.pdrStepCount} (Outliers: ${fusionDiagnostics.outlierRejectionsCount})", color = Slate300, fontSize = 9.sp)
                                        Text("Floor stable: L${fusionDiagnostics.floorArbitrated} (${fusionDiagnostics.stateTransitionReason})", color = Slate300, fontSize = 9.sp)
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // FLOOR TRANSITION PROMPT CONFIRMATION CARD
                            // -------------------------------------------------------------
                            if (locationState is NavigationLocationState.FloorTransition) {
                                val transState = locationState as NavigationLocationState.FloorTransition
                                Surface(
                                    color = Slate900.copy(alpha = 0.96f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Amber500),
                                    shadowElevation = 12.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 120.dp, start = 20.dp, end = 20.dp)
                                        .testTag("floor_transition_prompt_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (transState.isElevator) Icons.Default.Elevator else Icons.Default.Stairs,
                                                contentDescription = "Floor Transition",
                                                tint = Amber400,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Floor Transition Required",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "Take ${if (transState.isElevator) "elevator" else "stairs"} to Level ${transState.toFloor}. Have you arrived at Level ${transState.toFloor}?",
                                            color = Slate300,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.confirmFloorTransition(transState.toFloor) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("Yes, on Level ${transState.toFloor}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { /* Stay in transit */ },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("In Transit", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // FLOATING CONTROLS (RIGHT EDGE: FLOOR SELECTOR, RECENTER, COMPASS, STEP)
                            // -------------------------------------------------------------
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Floor Switcher
                                Surface(
                                    color = Slate900.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.testTag("floor_switcher")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "FLOOR",
                                            color = Slate400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                        currentBuilding.floors.reversed().forEach { floorInfo ->
                                            val isSelected = floorInfo.floorNumber == activeFloor
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Cyan500 else Slate800)
                                                    .clickable {
                                                        viewModel.setFloor(floorInfo.floorNumber)
                                                    }
                                                    .testTag("floor_button_${floorInfo.floorNumber}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = floorInfo.shortName,
                                                    color = if (isSelected) Slate950 else Slate200,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Compass / North-Up Toggle FAB
                                Surface(
                                    color = Slate900.copy(alpha = 0.92f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clickable { viewModel.toggleCompassMode() }
                                        .testTag("compass_mode_fab")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Explore,
                                            contentDescription = "Compass",
                                            tint = if (isNorthUp) Cyan400 else Amber400,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Recenter Map on User Location FAB
                                Surface(
                                    color = Slate900.copy(alpha = 0.92f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clickable { viewModel.recenterMapOnUser() }
                                        .testTag("recenter_map_fab")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Recenter",
                                            tint = Cyan400,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Step Simulator FAB (Advances along route or triggers simulated step)
                                Surface(
                                    color = Cyan500.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .clickable {
                                            if (activeRoute != null) {
                                                viewModel.stepSimulationAlongRoute()
                                            } else {
                                                viewModel.triggerSimulatedStep()
                                            }
                                        }
                                        .testTag("step_simulation_fab")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.DirectionsWalk, contentDescription = "Step", tint = Slate950, modifier = Modifier.size(20.dp))
                                        Text("Step", color = Slate950, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // BOTTOM NAVIGATION SHEETS BY SESSION STATE
                            // -------------------------------------------------------------
                            when (navSessionState) {
                                NavSessionState.DESTINATION_SELECTED -> {
                                    destinationNode?.let { dest ->
                                        DestinationPreviewSheet(
                                            building = currentBuilding,
                                            destination = dest,
                                            currentFloor = activeFloor,
                                            userPositionX = currentIndoorPosition?.x,
                                            userPositionY = currentIndoorPosition?.y,
                                            isFavorite = userAccount.favoriteNodeIds.contains(dest.id),
                                            onToggleFavorite = { viewModel.toggleFavorite(dest.id) },
                                            onStartNavigation = { viewModel.startNavigation() },
                                            onViewRoute = { viewModel.viewRoute() },
                                            onDismiss = { viewModel.dismissDestinationPreview() },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                NavSessionState.ROUTE_READY -> {
                                    activeRoute?.let { route ->
                                        RoutePreviewSheet(
                                            building = currentBuilding,
                                            route = route,
                                            isWheelchairMode = isWheelchairMode,
                                            onToggleWheelchair = { viewModel.toggleWheelchairMode() },
                                            onStartNavigation = { viewModel.startNavigation() },
                                            onChangeRoute = { viewModel.dismissRoutePreview() },
                                            onDismiss = { viewModel.dismissRoutePreview() },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                NavSessionState.NAVIGATING,
                                NavSessionState.PAUSED,
                                NavSessionState.OFF_ROUTE,
                                NavSessionState.RECALCULATING,
                                NavSessionState.FLOOR_TRANSITION -> {
                                    activeRoute?.let { route ->
                                        ActiveNavigationBottomBar(
                                            route = route,
                                            navSessionState = navSessionState,
                                            currentFloor = activeFloor,
                                            remainingDistanceMeters = viewModel.getRemainingDistanceMeters(),
                                            remainingTimeMinutes = viewModel.getRemainingTimeMinutes(),
                                            isMuted = isMuted,
                                            onToggleMute = { viewModel.voiceEngine.toggleMute() },
                                            onPauseNavigation = { viewModel.pauseNavigation() },
                                            onResumeNavigation = { viewModel.resumeNavigation() },
                                            onStopNavigation = { viewModel.stopNavigation() },
                                            onOpenRouteDetails = { showRouteDetailsDrawer = true },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                NavSessionState.ARRIVED -> {
                                    destinationNode?.let { dest ->
                                        ArrivalSheet(
                                            building = currentBuilding,
                                            destination = dest,
                                            totalDistanceMeters = activeRoute?.totalDistanceMeters ?: 0f,
                                            onDismiss = { viewModel.dismissArrival() },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                else -> {
                                    // IDLE / DEFAULT ACTION BAR
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Search & Filter Panel trigger button (Left)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                color = Slate900.copy(alpha = 0.94f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .clickable { viewModel.startSearching() }
                                                    .testTag("open_search_drawer_btn")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Cyan400, modifier = Modifier.size(18.dp))
                                                    Text("Search Destinations", color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Surface(
                                                color = Slate900.copy(alpha = 0.94f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                                modifier = Modifier
                                                    .clickable { showSidebarDrawer = true }
                                                    .testTag("open_sidebar_options_btn")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Tune, contentDescription = "Options", tint = Slate300, modifier = Modifier.size(18.dp))
                                                    Text("Options", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }

                                        // Spatial AR Button (Right)
                                        FloatingActionButton(
                                            onClick = { viewModel.toggleARMode() },
                                            containerColor = Cyan500,
                                            contentColor = Slate950,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.testTag("ar_fab_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ViewInAr, contentDescription = "AR Mode")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Spatial AR", fontWeight = FontWeight.Bold)
                                            }
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
        // SIDEBAR DRAWER / MODAL SHEET FOR SEARCH & WAYPOINTS
        // -------------------------------------------------------------
        if (showSidebarDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showSidebarDrawer = false },
                containerColor = Slate900,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                SidebarControlPanel(
                    building = currentBuilding,
                    activeFloor = activeFloor,
                    startNode = startNode,
                    destinationNode = destinationNode,
                    isWheelchairMode = isWheelchairMode,
                    isEmergencyMode = isEmergencyMode,
                    favoriteNodeIds = userAccount.favoriteNodeIds,
                    recentNodeIds = userAccount.recentNodeIds,
                    onSelectBuilding = { id -> viewModel.selectBuilding(id) },
                    onSelectFloor = { f -> viewModel.setFloor(f) },
                    onSetStartNode = { node -> viewModel.setStartNode(node) },
                    onSetDestinationNode = { node ->
                        viewModel.setDestinationNode(node)
                        showSidebarDrawer = false
                    },
                    onToggleWheelchair = { viewModel.toggleWheelchairMode() },
                    onToggleEmergency = {
                        viewModel.toggleEmergencyEvacuation()
                        showSidebarDrawer = false
                    },
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onOpenQRScanner = {
                        showSidebarDrawer = false
                        viewModel.setShowQRDialog(true)
                    },
                    onOpenLandmarkScanner = {
                        showSidebarDrawer = false
                        viewModel.setShowLandmarkDialog(true)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // -------------------------------------------------------------
        // ROUTE DETAILS DRAWER / MODAL SHEET
        // -------------------------------------------------------------
        if (showRouteDetailsDrawer && activeRoute != null) {
            ModalBottomSheet(
                onDismissRequest = { showRouteDetailsDrawer = false },
                containerColor = Slate900,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                RouteDetailsPanel(
                    route = activeRoute,
                    activeStepIndex = activeStepIndex,
                    isWheelchairMode = isWheelchairMode,
                    onNextStep = { viewModel.nextStep() },
                    onPrevStep = { viewModel.prevStep() },
                    onReplayVoice = { viewModel.replayVoiceInstruction() },
                    onClearRoute = {
                        viewModel.setDestinationNode(null)
                        showRouteDetailsDrawer = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // -------------------------------------------------------------
        // AUTH & PROFILE DIALOG
        // -------------------------------------------------------------
        if (showAuthDialog) {
            AuthProfileDialog(
                userAccount = userAccount,
                onRoleToggle = { newRole ->
                    viewModel.toggleUserRole(newRole)
                    viewModel.setShowAuthDialog(false)
                },
                onDismiss = { viewModel.setShowAuthDialog(false) }
            )
        }

        // -------------------------------------------------------------
        // FACILITY SELECTOR SHEET
        // -------------------------------------------------------------
        if (showBuildingSheet) {
            BuildingSelectorSheet(
                buildings = buildings,
                currentBuildingId = currentBuilding.id,
                onSelectBuilding = { id -> viewModel.selectBuilding(id) },
                onOpenMapBuilder = { viewModel.setShowMapBuilder(true) },
                onDismiss = { viewModel.setShowBuildingSheet(false) }
            )
        }

        // -------------------------------------------------------------
        // GEMINI VISION LANDMARK DIALOG
        // -------------------------------------------------------------
        if (showLandmarkDialog) {
            LandmarkScannerDialog(
                building = currentBuilding,
                lastResult = lastLandmarkResult,
                isScanning = isAILoading,
                onScanPresetLandmark = { sample ->
                    sample.drawableRes?.let { resId ->
                        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                        if (bitmap != null) {
                            viewModel.scanLandmarkImage(bitmap)
                        }
                    }
                },
                onConfirmAnchoring = {
                    viewModel.setShowLandmarkDialog(false)
                },
                onDismiss = { viewModel.setShowLandmarkDialog(false) }
            )
        }

        // -------------------------------------------------------------
        // QR CALIBRATION DIALOG
        // -------------------------------------------------------------
        if (showQRDialog) {
            QRCalibrationDialog(
                building = currentBuilding,
                onNodeCalibrated = { nodeId -> viewModel.calibrateWithQR(nodeId) },
                onDismiss = { viewModel.setShowQRDialog(false) }
            )
        }

        // -------------------------------------------------------------
        // INVESTOR PITCH DIALOG
        // -------------------------------------------------------------
        if (showPitchDialog) {
            PitchHackathonDialog(onDismiss = { viewModel.setShowPitchDialog(false) })
        }

        // -------------------------------------------------------------
        // MAP BUILDER DIALOG
        // -------------------------------------------------------------
        if (showMapBuilder) {
            MapBuilderDialog(
                building = currentBuilding,
                activeFloor = activeFloor,
                onAddNode = { name, code, cat, desc, x, y ->
                    viewModel.addCustomNodeToMap(name, code, cat, desc, x, y)
                },
                onAddEdge = { from, to, dist, type ->
                    viewModel.addCustomEdgeToMap(from, to, dist, type)
                },
                onDismiss = { viewModel.setShowMapBuilder(false) }
            )
        }

        // -------------------------------------------------------------
        // NODE DETAIL BOTTOM SHEET
        // -------------------------------------------------------------
        selectedNodeDetail?.let { node ->
            NodeDetailBottomSheet(
                node = node,
                onSetAsStart = { start -> viewModel.setStartNode(start) },
                onNavigateHere = { dest -> viewModel.setDestinationNode(dest) },
                onDismiss = { viewModel.selectNodeForDetail(null) }
            )
        }

        // -------------------------------------------------------------
        // BACKEND API CONFIGURATION DIALOG
        // -------------------------------------------------------------
        if (showBackendConfigDialog) {
            var urlInput by remember { mutableStateOf(viewModel.getBackendBaseUrl()) }
            AlertDialog(
                onDismissRequest = { showBackendConfigDialog = false },
                containerColor = Slate900,
                title = {
                    Text("Backend API Endpoint", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "For real-phone testing, enter your computer's Wi-Fi IP address (e.g. http://192.168.1.50:5000/api/) or your public backend URL.",
                            fontSize = 12.sp,
                            color = Slate300
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Base API URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Presets:", fontSize = 11.sp, color = Slate400)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = urlInput.contains("10.0.2.2"),
                                onClick = { urlInput = "http://10.0.2.2:5000/api/" },
                                label = { Text("Emulator", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = urlInput.contains("127.0.0.1"),
                                onClick = { urlInput = "http://127.0.0.1:5000/api/" },
                                label = { Text("Localhost", fontSize = 10.sp) }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = urlInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.setBackendBaseUrl(trimmed)
                            }
                            showBackendConfigDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                    ) {
                        Text("Save & Sync", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackendConfigDialog = false }) {
                        Text("Cancel", color = Slate300)
                    }
                }
            )
        }
    }
}
