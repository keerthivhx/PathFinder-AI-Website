package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiNavService
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.model.*
import com.example.data.repository.AnalyticsRepository
import com.example.data.repository.BuildingRepository
import com.example.data.repository.NavigationSessionManager
import com.example.positioning.*
import com.example.routing.AStarRouter
import com.example.voice.VoiceGuidanceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NavViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BuildingRepository(application.applicationContext)
    private val geminiService = GeminiNavService()
    val voiceEngine = VoiceGuidanceEngine(application)

    // Phase 12: Navigation Session Manager & Analytics Repository
    val sessionManager = NavigationSessionManager(application.applicationContext, viewModelScope)
    val analyticsRepository = AnalyticsRepository(application.applicationContext)

    val activeNavigationSession: StateFlow<NavigationSessionEntity?> = sessionManager.activeSession

    // Unified Indoor & Outdoor Positioning Engine (Phases 5, 6, 7)
    val positionManager = UnifiedIndoorPositionManager(application.applicationContext, viewModelScope)

    val locationState: StateFlow<NavigationLocationState> = positionManager.locationState
    val currentIndoorPosition: StateFlow<IndoorPosition?> = positionManager.currentIndoorPosition
    val deviceHeadingDegrees: StateFlow<Float> = positionManager.deviceHeadingDegrees
    val positioningMode: StateFlow<PositioningMode> = positionManager.positioningMode
    val registeredBeacons: StateFlow<List<Beacon>> = repository.beacons
    val registeredQrPoints: StateFlow<List<QrCalibrationPoint>> = repository.qrPoints

    val buildings: StateFlow<List<Building>> = repository.buildings
    val syncState: StateFlow<NetworkSyncState> = repository.syncState

    private val _graphValidationReport = MutableStateFlow<GraphValidationReport?>(null)
    val graphValidationReport: StateFlow<GraphValidationReport?> = _graphValidationReport.asStateFlow()

    // Configurable walking speed (default 1.4 m/s ~ 5.0 km/h)
    private val _walkingSpeedMetersPerSec = MutableStateFlow(1.4f)
    val walkingSpeedMetersPerSec: StateFlow<Float> = _walkingSpeedMetersPerSec.asStateFlow()

    // Navigation Tab state
    private val _currentTab = MutableStateFlow(AppTab.NAVIGATION)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentBuilding = MutableStateFlow(
        repository.buildings.value.firstOrNull { it.id == "univ" } ?: repository.buildings.value.first()
    )
    val currentBuilding: StateFlow<Building> = _currentBuilding.asStateFlow()

    private val _activeFloor = MutableStateFlow(0)
    val activeFloor: StateFlow<Int> = _activeFloor.asStateFlow()

    private val _startNode = MutableStateFlow<NavNode?>(_currentBuilding.value.nodes.firstOrNull())
    val startNode: StateFlow<NavNode?> = _startNode.asStateFlow()

    private val _destinationNode = MutableStateFlow<NavNode?>(null)
    val destinationNode: StateFlow<NavNode?> = _destinationNode.asStateFlow()

    private val _activeRoute = MutableStateFlow<NavigationRoute?>(null)
    val activeRoute: StateFlow<NavigationRoute?> = _activeRoute.asStateFlow()

    private val _navSessionState = MutableStateFlow(NavSessionState.IDLE)
    val navSessionState: StateFlow<NavSessionState> = _navSessionState.asStateFlow()

    private val _isNorthUp = MutableStateFlow(true)
    val isNorthUp: StateFlow<Boolean> = _isNorthUp.asStateFlow()

    private val _activeStepIndex = MutableStateFlow(0)
    val activeStepIndex: StateFlow<Int> = _activeStepIndex.asStateFlow()

    private val _isWheelchairMode = MutableStateFlow(false)
    val isWheelchairMode: StateFlow<Boolean> = _isWheelchairMode.asStateFlow()

    private val _isEmergencyMode = MutableStateFlow(false)
    val isEmergencyMode: StateFlow<Boolean> = _isEmergencyMode.asStateFlow()

    private val _isARMode = MutableStateFlow(false)
    val isARMode: StateFlow<Boolean> = _isARMode.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    private val _lastIntentResult = MutableStateFlow<AIIntentResult?>(null)
    val lastIntentResult: StateFlow<AIIntentResult?> = _lastIntentResult.asStateFlow()

    private val _lastLandmarkResult = MutableStateFlow<AILandmarkResult?>(null)
    val lastLandmarkResult: StateFlow<AILandmarkResult?> = _lastLandmarkResult.asStateFlow()

    // AI Assistant Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                message = "Hello! I am PathFinder Assistant. Ask me how to find any room, department, or facility inside this campus."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // User Account & Role
    private val _userAccount = MutableStateFlow(UserAccount())
    val userAccount: StateFlow<UserAccount> = _userAccount.asStateFlow()

    // Analytics Data (Legacy and Phase 12)
    private val _analyticsData = MutableStateFlow(AnalyticsData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()

    // Phase 12 Analytics StateFlows
    private val _analyticsOverview = MutableStateFlow(AnalyticsOverviewMetrics())
    val analyticsOverview: StateFlow<AnalyticsOverviewMetrics> = _analyticsOverview.asStateFlow()

    private val _topDestinations = MutableStateFlow<List<TopDestinationMetric>>(emptyList())
    val topDestinations: StateFlow<List<TopDestinationMetric>> = _topDestinations.asStateFlow()

    private val _searchDemand = MutableStateFlow<List<SearchMetric>>(emptyList())
    val searchDemand: StateFlow<List<SearchMetric>> = _searchDemand.asStateFlow()

    private val _zeroResultSearches = MutableStateFlow<List<SearchMetric>>(emptyList())
    val zeroResultSearches: StateFlow<List<SearchMetric>> = _zeroResultSearches.asStateFlow()

    private val _positioningDistribution = MutableStateFlow<List<PositioningSourceMetric>>(emptyList())
    val positioningDistribution: StateFlow<List<PositioningSourceMetric>> = _positioningDistribution.asStateFlow()

    private val _floorActivity = MutableStateFlow<List<FloorActivityMetric>>(emptyList())
    val floorActivity: StateFlow<List<FloorActivityMetric>> = _floorActivity.asStateFlow()

    private val _problematicLocations = MutableStateFlow<List<ProblematicLocationAlert>>(emptyList())
    val problematicLocations: StateFlow<List<ProblematicLocationAlert>> = _problematicLocations.asStateFlow()

    private val _adminAlerts = MutableStateFlow<List<ProblematicLocationAlert>>(emptyList())
    val adminAlerts: StateFlow<List<ProblematicLocationAlert>> = _adminAlerts.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<NavigationSessionEntity>>(emptyList())
    val recentSessions: StateFlow<List<NavigationSessionEntity>> = _recentSessions.asStateFlow()

    private val _dateTrends = MutableStateFlow<List<DateTrendMetric>>(emptyList())
    val dateTrends: StateFlow<List<DateTrendMetric>> = _dateTrends.asStateFlow()

    // Analytics Filters
    private val _selectedAnalyticsBuildingId = MutableStateFlow<String?>(null)
    val selectedAnalyticsBuildingId: StateFlow<String?> = _selectedAnalyticsBuildingId.asStateFlow()

    private val _analyticsDateRange = MutableStateFlow(AnalyticsDateRange.ALL_TIME)
    val analyticsDateRange: StateFlow<AnalyticsDateRange> = _analyticsDateRange.asStateFlow()

    private val _analyticsResultFilter = MutableStateFlow(ResultFilter.ALL)
    val analyticsResultFilter: StateFlow<ResultFilter> = _analyticsResultFilter.asStateFlow()

    private val _analyticsAccessibilityFilter = MutableStateFlow(AccessibilityFilter.ALL)
    val analyticsAccessibilityFilter: StateFlow<AccessibilityFilter> = _analyticsAccessibilityFilter.asStateFlow()

    private val _includeDemoSessions = MutableStateFlow(false)
    val includeDemoSessions: StateFlow<Boolean> = _includeDemoSessions.asStateFlow()

    private val _analyticsLastUpdated = MutableStateFlow(System.currentTimeMillis())
    val analyticsLastUpdated: StateFlow<Long> = _analyticsLastUpdated.asStateFlow()

    // Dialog & Sheet Controls
    private val _selectedNodeDetail = MutableStateFlow<NavNode?>(null)
    val selectedNodeDetail: StateFlow<NavNode?> = _selectedNodeDetail.asStateFlow()

    private val _showBuildingSheet = MutableStateFlow(false)
    val showBuildingSheet: StateFlow<Boolean> = _showBuildingSheet.asStateFlow()

    private val _showLandmarkDialog = MutableStateFlow(false)
    val showLandmarkDialog: StateFlow<Boolean> = _showLandmarkDialog.asStateFlow()

    private val _showQRDialog = MutableStateFlow(false)
    val showQRDialog: StateFlow<Boolean> = _showQRDialog.asStateFlow()

    private val _showPitchDialog = MutableStateFlow(false)
    val showPitchDialog: StateFlow<Boolean> = _showPitchDialog.asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    private val _showMapBuilder = MutableStateFlow(false)
    val showMapBuilder: StateFlow<Boolean> = _showMapBuilder.asStateFlow()

    private val _showPositionDebugOverlay = MutableStateFlow(false)
    val showPositionDebugOverlay: StateFlow<Boolean> = _showPositionDebugOverlay.asStateFlow()

    val fusionDiagnostics: StateFlow<FusionDiagnostics> = positionManager.diagnostics

    private var lastSpokenStateMessage: String? = null
    private var lastSpokenTime: Long = 0L

    fun togglePositionDebugOverlay() {
        _showPositionDebugOverlay.value = !_showPositionDebugOverlay.value
    }

    fun confirmFloorTransition(targetFloor: Int) {
        positionManager.confirmFloorTransition(targetFloor)
        _activeFloor.value = targetFloor
        voiceEngine.speak("Floor updated to Level $targetFloor.")
    }

    init {
        val initialBuilding = _currentBuilding.value
        _startNode.value = initialBuilding.nodes.firstOrNull { it.category == NodeCategory.ENTRANCE }
            ?: initialBuilding.nodes.firstOrNull()

        positionManager.setActiveBuilding(initialBuilding)
        positionManager.setRegisteredBeacons(repository.beacons.value)
        positionManager.startTracking()

        // Sync beacons flow
        viewModelScope.launch {
            repository.beacons.collect { bList ->
                positionManager.setRegisteredBeacons(bList)
            }
        }

        // Voice Guidance feedback on state changes
        viewModelScope.launch {
            positionManager.locationState.collect { state ->
                handleLocationStateVoiceFeedback(state)
            }
        }

        syncBackendData()
        refreshAnalytics()
    }

    private fun handleLocationStateVoiceFeedback(state: NavigationLocationState) {
        val now = System.currentTimeMillis()
        when (state) {
            is NavigationLocationState.OffRoute -> {
                if (_navSessionState.value == NavSessionState.NAVIGATING) {
                    _navSessionState.value = NavSessionState.OFF_ROUTE
                }
                sessionManager.recordOffRoute("Off route: ${state.distanceOffMeters.toInt()}m", _activeFloor.value)
                val msg = "You are ${state.distanceOffMeters.toInt()} meters off route. Recalculating path."
                if (msg != lastSpokenStateMessage || (now - lastSpokenTime) > 5000L) {
                    voiceEngine.speak(msg)
                    lastSpokenStateMessage = msg
                    lastSpokenTime = now
                    _navSessionState.value = NavSessionState.RECALCULATING
                    sessionManager.recordReroute("Off route divergence recalculation", _activeFloor.value)
                    positionManager.performReroute(_isWheelchairMode.value, _isEmergencyMode.value)?.let {
                        _activeRoute.value = it
                        _activeStepIndex.value = 0
                        _navSessionState.value = NavSessionState.NAVIGATING
                    }
                }
            }
            is NavigationLocationState.FloorTransition -> {
                if (_navSessionState.value == NavSessionState.NAVIGATING) {
                    _navSessionState.value = NavSessionState.FLOOR_TRANSITION
                }
                sessionManager.recordFloorTransition(state.fromFloor, state.toFloor, state.isElevator)
                val action = if (state.isElevator) "Take elevator" else "Use stairs"
                val dir = if (state.toFloor > state.fromFloor) "up to Floor ${state.toFloor}" else "down to Floor ${state.toFloor}"
                val msg = "$action via ${state.viaNodeName} $dir."
                if (msg != lastSpokenStateMessage || (now - lastSpokenTime) > 8000L) {
                    voiceEngine.speak(msg)
                    lastSpokenStateMessage = msg
                    lastSpokenTime = now
                    _activeFloor.value = state.toFloor
                }
            }
            is NavigationLocationState.Arrived -> {
                if (_navSessionState.value == NavSessionState.NAVIGATING || _navSessionState.value == NavSessionState.PAUSED) {
                    _navSessionState.value = NavSessionState.ARRIVED
                }
                sessionManager.completeSession(state.destinationNode)
                refreshAnalytics()
                val msg = "You have arrived at ${state.destinationNode.name}."
                if (msg != lastSpokenStateMessage) {
                    voiceEngine.speak(msg, force = true)
                    lastSpokenStateMessage = msg
                    lastSpokenTime = now
                }
            }
            is NavigationLocationState.IndoorNavigating -> {
                _activeStepIndex.value = state.stepIndex
                if (_navSessionState.value == NavSessionState.OFF_ROUTE || _navSessionState.value == NavSessionState.RECALCULATING) {
                    _navSessionState.value = NavSessionState.NAVIGATING
                }
            }
            is NavigationLocationState.AtEntrance -> {
                val msg = "You have reached ${state.entranceName}. Scan QR code to begin indoor guidance."
                if (msg != lastSpokenStateMessage || (now - lastSpokenTime) > 10000L) {
                    voiceEngine.speak(msg)
                    lastSpokenStateMessage = msg
                    lastSpokenTime = now
                }
            }
            else -> {}
        }
    }

    fun syncBackendData() {
        viewModelScope.launch {
            val currentId = _currentBuilding.value.id
            val result = repository.syncWithBackend(currentId)
            if (result.isSuccess) {
                val updatedBuilding = repository.getBuildingById(currentId)
                    ?: repository.buildings.value.firstOrNull()
                if (updatedBuilding != null) {
                    _currentBuilding.value = updatedBuilding
                    if (_startNode.value == null || updatedBuilding.nodes.none { it.id == _startNode.value?.id }) {
                        _startNode.value = updatedBuilding.nodes.firstOrNull { it.category == NodeCategory.ENTRANCE }
                            ?: updatedBuilding.nodes.firstOrNull()
                    }
                    if (_destinationNode.value != null && updatedBuilding.nodes.none { it.id == _destinationNode.value?.id }) {
                        _destinationNode.value = null
                        _activeRoute.value = null
                    } else if (_destinationNode.value != null) {
                        recalculateRoute()
                    }
                }
            }
        }
    }

    fun setBackendBaseUrl(newUrl: String) {
        if (newUrl.isNotBlank()) {
            com.example.data.network.ApiClient.setBaseUrl(newUrl)
            syncBackendData()
        }
    }

    fun getBackendBaseUrl(): String {
        return com.example.data.network.ApiClient.getBaseUrl()
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectBuilding(buildingId: String) {
        val b = repository.getBuildingById(buildingId) ?: return
        _currentBuilding.value = b
        _activeFloor.value = 0
        val entranceNode = b.nodes.firstOrNull { it.category == NodeCategory.ENTRANCE } ?: b.nodes.firstOrNull()
        _startNode.value = entranceNode
        _destinationNode.value = null
        _activeRoute.value = null
        _activeStepIndex.value = 0
        _isEmergencyMode.value = false
        _lastIntentResult.value = null
        _lastLandmarkResult.value = null
        _showBuildingSheet.value = false

        positionManager.setActiveBuilding(b)
        positionManager.setRegisteredBeacons(repository.getBeaconsForBuilding(buildingId))

        // Update initial chat message for new building
        _chatMessages.value = listOf(
            ChatMessage(
                isUser = false,
                message = "Switched to ${b.name} (${b.type}). Ask me how to navigate to any of the ${b.nodes.size} locations across ${b.floors.size} floors."
            )
        )
    }

    fun setFloor(floorNumber: Int) {
        _activeFloor.value = floorNumber
    }

    fun setStartNode(node: NavNode) {
        _startNode.value = node
        _activeFloor.value = node.floor
        recalculateRoute()
    }

    fun setDestinationNode(node: NavNode?) {
        _destinationNode.value = node
        if (node != null) {
            // Add to recent searches
            val recents = (_userAccount.value.recentNodeIds.filterNot { it == node.id } + node.id).takeLast(6)
            _userAccount.value = _userAccount.value.copy(recentNodeIds = recents)
            recalculateRoute()
        } else {
            _activeRoute.value = null
            _activeStepIndex.value = 0
        }
    }

    fun selectNodeForDetail(node: NavNode?) {
        _selectedNodeDetail.value = node
    }

    fun toggleWheelchairMode() {
        val newValue = !_isWheelchairMode.value
        _isWheelchairMode.value = newValue
        if (_activeRoute.value != null) {
            recalculateRoute()
        }
    }

    fun toggleARMode() {
        _isARMode.value = !_isARMode.value
    }

    fun toggleFavorite(nodeId: String) {
        val currentFavs = _userAccount.value.favoriteNodeIds.toMutableSet()
        if (currentFavs.contains(nodeId)) {
            currentFavs.remove(nodeId)
        } else {
            currentFavs.add(nodeId)
        }
        _userAccount.value = _userAccount.value.copy(favoriteNodeIds = currentFavs)
    }

    fun toggleUserRole(role: String) {
        _userAccount.value = _userAccount.value.copy(role = role)
    }

    fun toggleEmergencyEvacuation() {
        val newEmergency = !_isEmergencyMode.value
        _isEmergencyMode.value = newEmergency

        val currentStart = _startNode.value ?: _currentBuilding.value.nodes.first()

        if (newEmergency) {
            val result = AStarRouter.findEmergencyEvacuationPath(
                building = _currentBuilding.value,
                startNodeId = currentStart.id,
                wheelchairOnly = _isWheelchairMode.value
            )
            if (result.route != null) {
                _activeRoute.value = result.route
                _destinationNode.value = result.route.pathNodes.lastOrNull()
                _activeStepIndex.value = 0
                val firstStep = result.route.steps.firstOrNull()
                voiceEngine.speak("EMERGENCY EVACUATION ACTIVE! ${firstStep?.instruction ?: "Proceed to nearest emergency exit."}", force = true)
            }
        } else {
            _activeRoute.value = null
            _destinationNode.value = null
            _activeStepIndex.value = 0
            voiceEngine.stop()
        }
    }

    fun setWalkingSpeed(speedMetersPerSec: Float) {
        _walkingSpeedMetersPerSec.value = speedMetersPerSec.coerceIn(0.5f, 3.0f)
        if (_activeRoute.value != null) {
            recalculateRoute()
        }
    }

    fun useCurrentLocation() {
        val entrance = _currentBuilding.value.nodes.firstOrNull { it.category == NodeCategory.ENTRANCE }
            ?: _currentBuilding.value.nodes.firstOrNull()
        if (entrance != null) {
            _startNode.value = entrance
            _activeFloor.value = entrance.floor
            voiceEngine.speak("Current location set to ${entrance.name}")
            if (_destinationNode.value != null) {
                recalculateRoute()
            }
        }
    }

    fun searchLocations(query: String, category: NodeCategory? = null): List<NavNode> {
        val results = repository.searchLocations(query, _currentBuilding.value.id, category)
        recordSearchEvent(query, results.size, category = category?.name)
        return results
    }

    fun recordSearchEvent(query: String, resultCount: Int, selectedNodeId: String? = null, category: String? = null) {
        if (query.trim().length >= 2) {
            viewModelScope.launch {
                analyticsRepository.recordSearch(
                    buildingId = _currentBuilding.value.id,
                    query = query,
                    resultCount = resultCount,
                    selectedNodeId = selectedNodeId,
                    selectedCategory = category,
                    isDemo = positioningMode.value == PositioningMode.SIMULATION_DEMO
                )
                refreshAnalytics()
            }
        }
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            val bId = _selectedAnalyticsBuildingId.value
            val range = _analyticsDateRange.value
            val rFilter = _analyticsResultFilter.value
            val aFilter = _analyticsAccessibilityFilter.value
            val demo = _includeDemoSessions.value

            _analyticsOverview.value = analyticsRepository.calculateOverviewMetrics(bId, range, aFilter, demo)
            _topDestinations.value = analyticsRepository.getTopDestinations(bId, range, demo)
            _searchDemand.value = analyticsRepository.getSearchDemand(bId, range, demo)
            _zeroResultSearches.value = analyticsRepository.getZeroResultSearches(bId, range, demo)
            _positioningDistribution.value = analyticsRepository.getPositioningDistribution(bId, range, demo)
            _floorActivity.value = analyticsRepository.getFloorActivity(bId, range, demo)
            _problematicLocations.value = analyticsRepository.getProblematicLocations(bId, range, demo)
            _adminAlerts.value = analyticsRepository.getAdminAlerts(bId, range, demo)
            _recentSessions.value = analyticsRepository.getFilteredSessions(bId, range, rFilter, aFilter, demo)
            _dateTrends.value = analyticsRepository.getDateTrends(bId, range, demo)
            _analyticsLastUpdated.value = System.currentTimeMillis()
        }
    }

    fun setAnalyticsBuilding(buildingId: String?) {
        _selectedAnalyticsBuildingId.value = buildingId
        refreshAnalytics()
    }

    fun setAnalyticsDateRange(range: AnalyticsDateRange) {
        _analyticsDateRange.value = range
        refreshAnalytics()
    }

    fun setAnalyticsResultFilter(filter: ResultFilter) {
        _analyticsResultFilter.value = filter
        refreshAnalytics()
    }

    fun setAnalyticsAccessibilityFilter(filter: AccessibilityFilter) {
        _analyticsAccessibilityFilter.value = filter
        refreshAnalytics()
    }

    fun setIncludeDemoSessions(include: Boolean) {
        _includeDemoSessions.value = include
        refreshAnalytics()
    }

    fun clearDemoAnalytics() {
        viewModelScope.launch {
            analyticsRepository.clearDemoData()
            refreshAnalytics()
        }
    }

    fun recalculateRoute() {
        // Prefer starting from user's current indoor position if available
        val effectiveStart = if (currentIndoorPosition.value != null) {
            val userPos = currentIndoorPosition.value!!
            val nearest = _currentBuilding.value.nodes
                .filter { it.floor == userPos.floorId }
                .minByOrNull { kotlin.math.hypot(it.x - userPos.x, it.y - userPos.y) }
            nearest ?: _startNode.value
        } else {
            _startNode.value
        } ?: return

        val dest = _destinationNode.value ?: return

        val result = AStarRouter.findPath(
            building = _currentBuilding.value,
            startNodeId = effectiveStart.id,
            targetNodeId = dest.id,
            wheelchairOnly = _isWheelchairMode.value,
            isEmergency = _isEmergencyMode.value,
            walkingSpeedMetersPerSec = _walkingSpeedMetersPerSec.value
        )

        if (result.route != null) {
            _activeRoute.value = result.route
            _activeStepIndex.value = 0
            positionManager.updateActiveRoute(result.route)
            val initialStep = result.route.steps.firstOrNull()
            if (initialStep != null && _navSessionState.value == NavSessionState.NAVIGATING) {
                voiceEngine.speak(initialStep.instruction)
            }
        } else {
            _activeRoute.value = null
            positionManager.updateActiveRoute(null)
            _aiStatusMessage.value = result.error ?: "No valid path found"
        }
    }

    // -------------------------------------------------------------
    // PHASE 10 NAVIGATION LIFECYCLE & STATE MACHINE
    // -------------------------------------------------------------
    fun startSearching() {
        _navSessionState.value = NavSessionState.SEARCHING
    }

    fun cancelSearching() {
        _navSessionState.value = if (_activeRoute.value != null) {
            NavSessionState.NAVIGATING
        } else if (_destinationNode.value != null) {
            NavSessionState.DESTINATION_SELECTED
        } else {
            NavSessionState.IDLE
        }
    }

    fun selectDestination(node: NavNode) {
        _destinationNode.value = node
        val recents = (_userAccount.value.recentNodeIds.filterNot { it == node.id } + node.id).takeLast(8)
        _userAccount.value = _userAccount.value.copy(recentNodeIds = recents)
        _navSessionState.value = NavSessionState.DESTINATION_SELECTED
        _activeFloor.value = node.floor
    }

    fun viewRoute() {
        _navSessionState.value = NavSessionState.CALCULATING_ROUTE
        recalculateRoute()
        if (_activeRoute.value != null) {
            _navSessionState.value = NavSessionState.ROUTE_READY
        } else {
            _navSessionState.value = NavSessionState.ERROR
        }
    }

    fun startNavigation() {
        if (_activeRoute.value == null || _destinationNode.value?.id != _activeRoute.value?.destinationNode?.id) {
            _navSessionState.value = NavSessionState.CALCULATING_ROUTE
            recalculateRoute()
        }

        val route = _activeRoute.value
        val dest = _destinationNode.value
        if (route != null && dest != null) {
            _navSessionState.value = NavSessionState.NAVIGATING
            positionManager.updateActiveRoute(route)
            positionManager.startTracking()
            _activeStepIndex.value = 0
            val firstNode = route.pathNodes.firstOrNull()
            if (firstNode != null) {
                _activeFloor.value = firstNode.floor
            }
            val firstStep = route.steps.firstOrNull()
            voiceEngine.speak("Starting indoor navigation to ${dest.name}. ${firstStep?.instruction ?: ""}", force = true)

            // Start Phase 12 Navigation Session
            val isDemo = positioningMode.value == PositioningMode.SIMULATION_DEMO
            val posSource = currentIndoorPosition.value?.source ?: if (isDemo) PositionSource.SIMULATION else PositionSource.BLE_BEACON
            sessionManager.startSession(
                building = _currentBuilding.value,
                startNode = _startNode.value,
                destinationNode = dest,
                route = route,
                isWheelchair = _isWheelchairMode.value,
                positioningProvider = posSource,
                isDemo = isDemo
            )
            refreshAnalytics()
        } else {
            _navSessionState.value = NavSessionState.ERROR
            sessionManager.failSession("Failed to compute valid route to destination")
            refreshAnalytics()
        }
    }

    fun pauseNavigation() {
        if (_navSessionState.value == NavSessionState.NAVIGATING) {
            _navSessionState.value = NavSessionState.PAUSED
            voiceEngine.stop()
            voiceEngine.speak("Navigation paused")
            sessionManager.pauseSession()
        }
    }

    fun resumeNavigation() {
        if (_navSessionState.value == NavSessionState.PAUSED) {
            _navSessionState.value = NavSessionState.NAVIGATING
            voiceEngine.speak("Resuming navigation")
            val currentStep = _activeRoute.value?.steps?.getOrNull(_activeStepIndex.value)
            if (currentStep != null) {
                voiceEngine.speak(currentStep.instruction)
            }
            sessionManager.resumeSession()
        }
    }

    fun stopNavigation() {
        _navSessionState.value = NavSessionState.IDLE
        _activeRoute.value = null
        _destinationNode.value = null
        _activeStepIndex.value = 0
        positionManager.updateActiveRoute(null)
        voiceEngine.stop()
        voiceEngine.speak("Navigation stopped")
        sessionManager.cancelSession()
        refreshAnalytics()
    }

    fun dismissArrival() {
        _navSessionState.value = NavSessionState.IDLE
        val dest = _destinationNode.value ?: _currentBuilding.value.nodes.firstOrNull()
        if (dest != null) {
            sessionManager.completeSession(dest)
        }
        _activeRoute.value = null
        _destinationNode.value = null
        _activeStepIndex.value = 0
        positionManager.updateActiveRoute(null)
        refreshAnalytics()
    }

    fun dismissRoutePreview() {
        _navSessionState.value = NavSessionState.DESTINATION_SELECTED
    }

    fun dismissDestinationPreview() {
        _navSessionState.value = NavSessionState.IDLE
        _destinationNode.value = null
        _activeRoute.value = null
    }

    fun recenterMapOnUser() {
        val userPos = currentIndoorPosition.value
        if (userPos != null) {
            _activeFloor.value = userPos.floorId
        } else {
            val start = _startNode.value
            if (start != null) {
                _activeFloor.value = start.floor
            }
        }
    }

    fun toggleCompassMode() {
        _isNorthUp.value = !_isNorthUp.value
    }

    fun getDynamicDistanceToNextAction(): Int? {
        val route = _activeRoute.value ?: return null
        val step = route.steps.getOrNull(_activeStepIndex.value) ?: return null
        val targetNode = step.targetNode ?: return step.distanceMeters.toInt()

        val userPos = currentIndoorPosition.value
        return if (userPos != null && userPos.floorId == targetNode.floor) {
            val dx = targetNode.x - userPos.x
            val dy = targetNode.y - userPos.y
            (kotlin.math.hypot(dx, dy) / 10f).toInt().coerceAtLeast(0)
        } else {
            step.distanceMeters.toInt()
        }
    }

    fun getRemainingDistanceMeters(): Float {
        val route = _activeRoute.value ?: return 0f
        val remainingSteps = route.steps.drop(_activeStepIndex.value)
        return remainingSteps.sumOf { it.distanceMeters.toDouble() }.toFloat()
    }

    fun getRemainingTimeMinutes(): Int {
        val dist = getRemainingDistanceMeters()
        val speed = _walkingSpeedMetersPerSec.value.coerceAtLeast(0.5f)
        return (dist / (speed * 60f)).toInt().coerceAtLeast(1)
    }

    // -------------------------------------------------------------
    // POSITIONING & CALIBRATION (Phases 5, 6, 7)
    // -------------------------------------------------------------
    fun setPositioningMode(mode: PositioningMode) {
        positionManager.setPositioningMode(mode)
    }

    fun calibrateWithQR(nodeId: String) {
        val node = _currentBuilding.value.nodes.firstOrNull { it.id == nodeId } ?: return
        _startNode.value = node
        _activeFloor.value = node.floor
        _showQRDialog.value = false
        positionManager.calibrateManual(node)
        voiceEngine.speak("QR calibrated. Starting at ${node.name} on Level ${node.floor}.")
        if (_destinationNode.value != null) {
            recalculateRoute()
        }
    }

    fun calibrateWithQrPayload(payload: String): Boolean {
        val result = positionManager.calibrateWithQr(payload)
        return if (result.isSuccess) {
            val node = result.getOrNull()
            if (node != null) {
                _startNode.value = node
                _activeFloor.value = node.floor
                _showQRDialog.value = false
                voiceEngine.speak("Calibrated to ${node.name} on Level ${node.floor}.")
                if (_destinationNode.value != null) {
                    recalculateRoute()
                }
                true
            } else false
        } else {
            false
        }
    }

    fun calibrateManual(node: NavNode) {
        _startNode.value = node
        _activeFloor.value = node.floor
        positionManager.calibrateManual(node)
        voiceEngine.speak("Location set to ${node.name}.")
        if (_destinationNode.value != null) {
            recalculateRoute()
        }
    }

    fun triggerSimulatedStep(headingOverride: Float? = null) {
        positionManager.triggerSimulatedStep(headingOverride)
    }

    fun stepSimulationAlongRoute() {
        val route = _activeRoute.value ?: return
        val currentPos = currentIndoorPosition.value
        val pathNodes = route.pathNodes
        if (pathNodes.isEmpty()) return

        // Find next target node in route
        val currentFloorNodes = pathNodes.filter { it.floor == _activeFloor.value }
        if (currentFloorNodes.isEmpty()) return

        val targetNode = if (currentPos == null) {
            currentFloorNodes.first()
        } else {
            val closestIdx = currentFloorNodes.indexOfFirst { it.id == currentPos.snappedNodeId }
            if (closestIdx >= 0 && closestIdx < currentFloorNodes.size - 1) {
                currentFloorNodes[closestIdx + 1]
            } else {
                currentFloorNodes.last()
            }
        }

        // Calculate heading to target node
        val curX = currentPos?.x ?: _startNode.value?.x ?: 150f
        val curY = currentPos?.y ?: _startNode.value?.y ?: 500f
        val dx = targetNode.x - curX
        val dy = targetNode.y - curY
        val heading = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        positionManager.triggerSimulatedStep((heading + 360f) % 360f)
    }

    fun simulateBeaconSignal(beaconId: String, rssi: Int) {
        positionManager.simulateBeaconSignal(beaconId, rssi)
    }

    fun simulateGpsLocation(latitude: Double, longitude: Double) {
        positionManager.simulateGpsLocation(latitude, longitude)
    }

    // Beacon Management (Phase 7 CRUD)
    fun addCustomBeacon(beacon: Beacon) {
        repository.addCustomBeacon(beacon)
        positionManager.setRegisteredBeacons(repository.beacons.value)
    }

    fun updateCustomBeacon(beacon: Beacon) {
        repository.updateCustomBeacon(beacon)
        positionManager.setRegisteredBeacons(repository.beacons.value)
    }

    fun deleteCustomBeacon(beaconId: String) {
        repository.deleteCustomBeacon(beaconId)
        positionManager.setRegisteredBeacons(repository.beacons.value)
    }

    fun buildAINavigationContext(): AINavigationContext {
        val building = _currentBuilding.value
        val pos = currentIndoorPosition.value
        val nearestNode = building.nodes.firstOrNull { it.id == pos?.nearestNodeId }
            ?: building.nodes.filter { it.floor == (pos?.floorId ?: _activeFloor.value) }.minByOrNull {
                val px = pos?.x ?: 0f
                val py = pos?.y ?: 0f
                kotlin.math.hypot(it.x - px, it.y - py)
            }
            ?: building.nodes.firstOrNull()

        val dest = _destinationNode.value
        val route = _activeRoute.value
        val stepIdx = _activeStepIndex.value
        val currentStep = route?.steps?.getOrNull(stepIdx)
        val isNavigating = _navSessionState.value == NavSessionState.NAVIGATING ||
                _navSessionState.value == NavSessionState.FLOOR_TRANSITION

        val confidenceStr = when (pos?.confidence) {
            PositionConfidence.HIGH -> "HIGH"
            PositionConfidence.MEDIUM -> "MEDIUM"
            PositionConfidence.LOW -> "LOW"
            null -> "UNCERTAIN"
        }

        val remainingDist = if (route != null && stepIdx in route.steps.indices) {
            route.steps.drop(stepIdx).sumOf { it.distanceMeters.toDouble() }.toFloat()
        } else route?.totalDistanceMeters

        val remainingMinutes = if (remainingDist != null) {
            ((remainingDist / 1.3f) / 60f).toInt().coerceAtLeast(1)
        } else null

        val floorTransfers = route?.steps?.filter { it.floorChange != null }?.map {
            "Floor transition from Floor ${it.floor} via ${it.instruction}"
        } ?: emptyList()

        return AINavigationContext(
            buildingId = building.id,
            buildingName = building.name,
            currentFloor = pos?.floorId ?: _activeFloor.value,
            currentNodeId = nearestNode?.id,
            currentNodeName = nearestNode?.name,
            currentNodeCode = nearestNode?.code,
            currentX = pos?.x,
            currentY = pos?.y,
            locationConfidence = confidenceStr,
            isLocationUncertain = pos == null || pos.confidence == PositionConfidence.LOW || pos.confidenceScore < 0.4f,
            destinationNodeId = dest?.id,
            destinationName = dest?.name,
            destinationFloor = dest?.floor,
            isDestinationSelected = dest != null,
            isNavigating = isNavigating,
            navigationState = _navSessionState.value,
            currentRouteStepInstruction = currentStep?.instruction,
            distanceToNextActionMeters = currentStep?.distanceMeters,
            remainingDistanceMeters = remainingDist,
            estimatedMinutes = remainingMinutes,
            floorTransfers = floorTransfers,
            isWheelchairMode = _isWheelchairMode.value,
            isRouteAccessible = route?.isWheelchairSafe ?: true,
            routeStepCount = route?.steps?.size ?: 0,
            currentStepIndex = stepIdx
        )
    }

    fun startNavigationWithDestination(node: NavNode) {
        setDestinationNode(node)
        _navSessionState.value = NavSessionState.NAVIGATING
        setTab(AppTab.NAVIGATION)
    }

    fun sendAssistantMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return

        val userMsg = ChatMessage(isUser = true, message = userPrompt)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val context = buildAINavigationContext()
                val response = geminiService.processNavigationQuery(userPrompt, context, _currentBuilding.value)

                val targetNode = response.resolvedTargetNode
                val start = _startNode.value ?: _currentBuilding.value.nodes.first()

                val routeCalc = if (targetNode != null) {
                    AStarRouter.findPath(
                        building = _currentBuilding.value,
                        startNodeId = start.id,
                        targetNodeId = targetNode.id,
                        wheelchairOnly = _isWheelchairMode.value,
                        isEmergency = response.isEmergency
                    ).route
                } else null

                if (response.isEmergency) {
                    _isEmergencyMode.value = true
                    if (targetNode != null) {
                        setDestinationNode(targetNode)
                        _navSessionState.value = NavSessionState.NAVIGATING
                    }
                }

                val aiMsg = ChatMessage(
                    isUser = false,
                    message = response.textResponse,
                    suggestedNodeId = targetNode?.id,
                    suggestedFloor = targetNode?.floor,
                    distanceMeters = routeCalc?.totalDistanceMeters ?: context.remainingDistanceMeters,
                    estimatedTimeMinutes = routeCalc?.estimatedMinutes ?: context.estimatedMinutes,
                    multipleMatchNodeIds = response.multipleMatches.map { it.id },
                    isEmergency = response.isEmergency,
                    intent = response.intent.name,
                    action = response.action
                )

                _chatMessages.value = _chatMessages.value + aiMsg

                if (response.suggestedVoiceResponse.isNotEmpty()) {
                    voiceEngine.speak(response.suggestedVoiceResponse, force = response.isEmergency)
                }
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    isUser = false,
                    message = "I encountered an error processing your query: ${e.message}"
                )
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun resolveAIIntent(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isAILoading.value = true
            _aiStatusMessage.value = "Gemini AI analyzing navigation intent..."
            try {
                val context = buildAINavigationContext()
                val response = geminiService.processNavigationQuery(query, context, _currentBuilding.value)
                val matchedNode = response.resolvedTargetNode

                val intentResult = AIIntentResult(
                    targetNodeId = matchedNode?.id,
                    confidence = response.confidence,
                    explanation = response.textResponse,
                    suggestedVoiceResponse = response.suggestedVoiceResponse
                )
                _lastIntentResult.value = intentResult

                if (matchedNode != null) {
                    _destinationNode.value = matchedNode
                    recalculateRoute()
                    if (response.suggestedVoiceResponse.isNotEmpty()) {
                        voiceEngine.speak(response.suggestedVoiceResponse, force = response.isEmergency)
                    }
                } else {
                    _aiStatusMessage.value = "Could not find a specific destination."
                }
            } catch (e: Exception) {
                _aiStatusMessage.value = "Error resolving intent: ${e.message}"
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun scanLandmarkImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAILoading.value = true
            _aiStatusMessage.value = "Gemini Vision scanning landmark photo..."
            try {
                val result = geminiService.identifyLandmark(bitmap, _currentBuilding.value)
                _lastLandmarkResult.value = result
                val matchedNode = _currentBuilding.value.nodes.firstOrNull { it.id == result.matchedNodeId }

                if (matchedNode != null) {
                    _startNode.value = matchedNode
                    _activeFloor.value = matchedNode.floor
                    voiceEngine.speak("Location anchored to ${matchedNode.name} on Floor ${matchedNode.floor}")
                    if (_destinationNode.value != null) {
                        recalculateRoute()
                    }
                }
            } catch (e: Exception) {
                _aiStatusMessage.value = "Landmark scan error: ${e.message}"
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun nextStep() {
        val route = _activeRoute.value ?: return
        val currentIdx = _activeStepIndex.value
        if (currentIdx < route.steps.size - 1) {
            val newIdx = currentIdx + 1
            _activeStepIndex.value = newIdx
            val step = route.steps[newIdx]
            _activeFloor.value = step.floor
            _startNode.value = step.targetNode
            voiceEngine.speak(step.instruction)
        }
    }

    fun prevStep() {
        val route = _activeRoute.value ?: return
        val currentIdx = _activeStepIndex.value
        if (currentIdx > 0) {
            val newIdx = currentIdx - 1
            _activeStepIndex.value = newIdx
            val step = route.steps[newIdx]
            _activeFloor.value = step.floor
            _startNode.value = step.targetNode
            voiceEngine.speak(step.instruction)
        }
    }

    fun replayVoiceInstruction() {
        val route = _activeRoute.value ?: return
        val step = route.steps.getOrNull(_activeStepIndex.value)
        if (step != null) {
            voiceEngine.speak(step.instruction, force = true)
        }
    }

    // -------------------------------------------------------------
    // ADMIN ACTIONS (PHASE 9)
    // -------------------------------------------------------------
    fun createBuilding(
        name: String,
        type: String,
        description: String,
        code: String = "",
        address: String = "Campus Ground",
        latitude: Double = 12.9716,
        longitude: Double = 77.5946
    ) {
        val newId = if (code.isNotBlank()) code.lowercase().replace(" ", "_") else "bld_${System.currentTimeMillis() % 10000}"
        val initialFloors = listOf(
            FloorInfo(0, "Ground Floor", "G", buildingId = newId),
            FloorInfo(1, "Floor 1", "L1", buildingId = newId)
        )
        val initialNodes = listOf(
            NavNode(
                id = "${newId}_ent",
                buildingId = newId,
                floor = 0,
                x = 200f,
                y = 500f,
                name = "Main Entrance",
                code = "ENT-01",
                category = NodeCategory.ENTRANCE,
                description = "Primary entrance gate",
                keywords = listOf("entrance", "door", "gate"),
                isAccessible = true,
                isEmergencyExit = false,
                visualSignageHint = "Main Entrance Gate",
                iconName = "door_front"
            ),
            NavNode(
                id = "${newId}_rec",
                buildingId = newId,
                floor = 0,
                x = 500f,
                y = 500f,
                name = "Reception & Info Desk",
                code = "REC-01",
                category = NodeCategory.RECEPTION,
                description = "Main reception counter",
                keywords = listOf("reception", "desk", "info"),
                isAccessible = true,
                isEmergencyExit = false,
                visualSignageHint = "Information Desk",
                iconName = "desk"
            )
        )
        val initialEdges = listOf(
            NavEdge("${newId}_ent", "${newId}_rec", 20f, EdgeType.CORRIDOR, isWheelchairAccessible = true, bidirectional = true)
        )
        val newBuilding = Building(
            id = newId,
            name = name,
            code = if (code.isNotBlank()) code else newId,
            type = type,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            floors = initialFloors,
            nodes = initialNodes,
            edges = initialEdges
        )
        repository.createBuilding(newBuilding)
        selectBuilding(newId)
    }

    fun updateBuilding(building: Building) {
        repository.updateBuilding(building)
        _currentBuilding.value = building
    }

    fun deleteBuilding(buildingId: String, cascade: Boolean = false): DeleteResult {
        val result = repository.deleteBuilding(buildingId, cascade)
        if (result is DeleteResult.Success) {
            val remaining = repository.buildings.value
            if (remaining.isNotEmpty()) {
                selectBuilding(remaining.first().id)
            }
        }
        return result
    }

    fun addFloor(
        floorNumber: Int,
        name: String,
        shortLabel: String,
        mapWidth: Float = 1000f,
        mapHeight: Float = 1000f,
        mapScale: Float = 1.0f,
        mapAsset: String = "blueprint_vector"
    ) {
        val building = _currentBuilding.value
        val floor = FloorInfo(
            floorNumber = floorNumber,
            name = name,
            shortName = shortLabel,
            buildingId = building.id,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapScale = mapScale,
            mapAsset = mapAsset
        )
        repository.createFloor(building.id, floor)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
    }

    fun updateFloor(floor: FloorInfo) {
        val building = _currentBuilding.value
        repository.updateFloor(building.id, floor)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
    }

    fun deleteFloor(floorNumber: Int, cascade: Boolean = false): DeleteResult {
        val building = _currentBuilding.value
        val result = repository.deleteFloor(building.id, floorNumber, cascade)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
        if (_activeFloor.value == floorNumber) {
            _activeFloor.value = _currentBuilding.value.floors.firstOrNull()?.floorNumber ?: 0
        }
        return result
    }

    fun addLocation(
        name: String,
        code: String,
        category: NodeCategory,
        floor: Int,
        description: String,
        x: Float = 500f,
        y: Float = 500f,
        isAccessible: Boolean = true,
        isEmergencyExit: Boolean = false
    ) {
        val building = _currentBuilding.value
        val newNodeId = if (code.isNotBlank()) "${building.id}_${code.lowercase().replace("-", "_").replace(" ", "_")}" else "node_${System.currentTimeMillis() % 10000}"
        val newNode = NavNode(
            id = newNodeId,
            buildingId = building.id,
            floor = floor,
            x = x,
            y = y,
            name = name,
            code = code,
            category = category,
            description = description,
            keywords = listOf(name.lowercase(), code.lowercase()),
            isAccessible = isAccessible,
            isEmergencyExit = isEmergencyExit || category == NodeCategory.EMERGENCY_EXIT,
            visualSignageHint = "$name Sign",
            iconName = "place"
        )
        repository.createNode(building.id, newNode)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
    }

    fun updateNavNode(node: NavNode) {
        val building = _currentBuilding.value
        repository.updateNode(building.id, node)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
        if (_startNode.value?.id == node.id) _startNode.value = node
        if (_destinationNode.value?.id == node.id) _destinationNode.value = node
    }

    fun deleteLocation(nodeId: String, cascade: Boolean = true): DeleteResult {
        val building = _currentBuilding.value
        val result = repository.deleteNode(building.id, nodeId, cascade)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
        if (_startNode.value?.id == nodeId) {
            _startNode.value = _currentBuilding.value.nodes.firstOrNull()
        }
        if (_destinationNode.value?.id == nodeId) {
            _destinationNode.value = null
            _activeRoute.value = null
        }
        return result
    }

    fun addEdge(
        fromId: String,
        toId: String,
        distance: Float,
        edgeType: EdgeType,
        isWheelchairAccessible: Boolean = (edgeType != EdgeType.STAIRS && edgeType != EdgeType.ESCALATOR),
        bidirectional: Boolean = true
    ) {
        val building = _currentBuilding.value
        val edge = NavEdge(
            fromId = fromId,
            toId = toId,
            distanceMeters = distance,
            edgeType = edgeType,
            isWheelchairAccessible = isWheelchairAccessible,
            bidirectional = bidirectional
        )
        repository.createEdge(building.id, edge)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
        recalculateRoute()
    }

    fun deleteEdge(fromId: String, toId: String) {
        val building = _currentBuilding.value
        repository.deleteEdge(building.id, fromId, toId)
        _currentBuilding.value = repository.getBuildingById(building.id) ?: building
        recalculateRoute()
    }

    fun addCustomNodeToMap(name: String, code: String, category: NodeCategory, description: String, x: Float, y: Float) {
        addLocation(name, code, category, _activeFloor.value, description, x, y)
    }

    fun addCustomEdgeToMap(fromId: String, toId: String, distance: Float, edgeType: EdgeType) {
        addEdge(fromId, toId, distance, edgeType)
    }

    // Beacons & QR
    fun addBeacon(beacon: Beacon) {
        repository.createBeacon(beacon)
    }

    fun updateBeacon(beacon: Beacon) {
        repository.updateBeacon(beacon)
    }

    fun deleteBeacon(beaconId: String) {
        repository.deleteBeacon(beaconId)
    }

    fun addQrPoint(qrPoint: QrCalibrationPoint) {
        repository.createQrPoint(qrPoint)
    }

    fun updateQrPoint(qrPoint: QrCalibrationPoint) {
        repository.updateQrPoint(qrPoint)
    }

    fun deleteQrPoint(calibrationId: String) {
        repository.deleteQrPoint(calibrationId)
    }

    // Graph Validation & Publishing
    fun runGraphValidation(buildingId: String? = null): GraphValidationReport {
        val targetId = buildingId ?: _currentBuilding.value.id
        val report = repository.validateBuildingGraph(targetId)
        _graphValidationReport.value = report
        return report
    }

    fun publishAndSynchronizeBuilding(buildingId: String? = null, onComplete: ((Result<GraphValidationReport>) -> Unit)? = null) {
        val targetId = buildingId ?: _currentBuilding.value.id
        viewModelScope.launch {
            val result = repository.publishAndSynchronizeBuilding(targetId)
            if (result.isSuccess) {
                _graphValidationReport.value = result.getOrNull()
                _currentBuilding.value = repository.getBuildingById(targetId) ?: _currentBuilding.value
            }
            onComplete?.invoke(result)
        }
    }

    // UI Dialog state mutators
    fun setShowBuildingSheet(show: Boolean) { _showBuildingSheet.value = show }
    fun setShowLandmarkDialog(show: Boolean) { _showLandmarkDialog.value = show }
    fun setShowQRDialog(show: Boolean) { _showQRDialog.value = show }
    fun setShowPitchDialog(show: Boolean) { _showPitchDialog.value = show }
    fun setShowAuthDialog(show: Boolean) { _showAuthDialog.value = show }
    fun setShowMapBuilder(show: Boolean) { _showMapBuilder.value = show }
    fun clearAIStatus() { _aiStatusMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
    }
}
