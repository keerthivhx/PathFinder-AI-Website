package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun AdminAnalyticsDashboard(
    buildings: List<Building>,
    currentBuilding: Building,
    overviewMetrics: AnalyticsOverviewMetrics,
    topDestinations: List<TopDestinationMetric>,
    searchDemand: List<SearchMetric>,
    zeroResultSearches: List<SearchMetric>,
    positioningDistribution: List<PositioningSourceMetric>,
    floorActivity: List<FloorActivityMetric>,
    problematicLocations: List<ProblematicLocationAlert>,
    adminAlerts: List<ProblematicLocationAlert>,
    recentSessions: List<NavigationSessionEntity>,
    dateTrends: List<DateTrendMetric>,
    selectedBuildingId: String?,
    onSelectBuilding: (String?) -> Unit,
    dateRange: AnalyticsDateRange,
    onSelectDateRange: (AnalyticsDateRange) -> Unit,
    resultFilter: ResultFilter,
    onSelectResultFilter: (ResultFilter) -> Unit,
    accessibilityFilter: AccessibilityFilter,
    onSelectAccessibilityFilter: (AccessibilityFilter) -> Unit,
    includeDemoSessions: Boolean,
    onToggleIncludeDemoSessions: (Boolean) -> Unit,
    lastUpdatedTimestamp: Long,
    onRefresh: () -> Unit,
    onClearDemoData: () -> Unit,
    onReviewInMapStudio: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedAnalyticsTab by remember { mutableIntStateOf(0) }
    val analyticsTabs = listOf("Overview", "Destinations", "Search & Gaps", "Sensors & Routes", "Session Log")

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(lastUpdatedTimestamp) { timeFormat.format(Date(lastUpdatedTimestamp)) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Controls & Filter Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Refresh & Last Updated
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Navigation Analytics & Insights",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Text(
                                text = "Last updated: $formattedTime • Privacy-first aggregation",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Slate800, CircleShape)
                                .testTag("refresh_analytics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Cyan300,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate800)

                    // Building Selector Filter
                    Text("Building Filter", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedBuildingId == null,
                                onClick = { onSelectBuilding(null) },
                                label = { Text("All Buildings") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                                    selectedLabelColor = Cyan300,
                                    containerColor = Slate950,
                                    labelColor = Slate300
                                )
                            )
                        }
                        items(buildings) { b ->
                            FilterChip(
                                selected = selectedBuildingId == b.id,
                                onClick = { onSelectBuilding(b.id) },
                                label = { Text(b.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                                    selectedLabelColor = Cyan300,
                                    containerColor = Slate950,
                                    labelColor = Slate300
                                )
                            )
                        }
                    }

                    // Date Range Filters
                    Text("Date Range", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AnalyticsDateRange.values()) { range ->
                            FilterChip(
                                selected = dateRange == range,
                                onClick = { onSelectDateRange(range) },
                                label = { Text(range.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Purple500.copy(alpha = 0.25f),
                                    selectedLabelColor = Purple300,
                                    containerColor = Slate950,
                                    labelColor = Slate300
                                )
                            )
                        }
                    }

                    // Outcome & Accessibility Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Outcome filter
                        var expandedOutcome by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedOutcome = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(resultFilter.label, fontSize = 12.sp, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = expandedOutcome,
                                onDismissRequest = { expandedOutcome = false },
                                modifier = Modifier.background(Slate900)
                            ) {
                                ResultFilter.values().forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label, color = Color.White) },
                                        onClick = {
                                            onSelectResultFilter(filter)
                                            expandedOutcome = false
                                        }
                                    )
                                }
                            }
                        }

                        // Accessibility filter
                        var expandedAcc by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedAcc = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(accessibilityFilter.label, fontSize = 12.sp, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = expandedAcc,
                                onDismissRequest = { expandedAcc = false },
                                modifier = Modifier.background(Slate900)
                            ) {
                                AccessibilityFilter.values().forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label, color = Color.White) },
                                        onClick = {
                                            onSelectAccessibilityFilter(filter)
                                            expandedAcc = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Demo / Simulation Data Toggle (Strict separation mandated in Section 33)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Include Simulation / Demo Sessions",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (includeDemoSessions) Amber500.copy(alpha = 0.2f) else Slate800,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (includeDemoSessions) "DEMO ON" else "REAL DATA ONLY",
                                        fontSize = 9.sp,
                                        color = if (includeDemoSessions) Amber400 else Emerald400,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Keep off to view pure authentic user wayfinding telemetry",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        Switch(
                            checked = includeDemoSessions,
                            onCheckedChange = onToggleIncludeDemoSessions,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Amber400,
                                checkedTrackColor = Amber500.copy(alpha = 0.3f),
                                uncheckedThumbColor = Slate500,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }
                }
            }
        }

        // 2. Sub-tab navigation
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedAnalyticsTab,
                containerColor = Slate900,
                contentColor = Cyan400,
                edgePadding = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            ) {
                analyticsTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedAnalyticsTab == index,
                        onClick = { selectedAnalyticsTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedAnalyticsTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // Sub-tab Contents
        when (selectedAnalyticsTab) {
            0 -> {
                // Overview Tab
                item {
                    AnalyticsOverviewSection(
                        metrics = overviewMetrics,
                        dateTrends = dateTrends,
                        alerts = adminAlerts,
                        onReviewInMapStudio = onReviewInMapStudio
                    )
                }
            }
            1 -> {
                // Destinations Tab
                item {
                    AnalyticsDestinationsSection(
                        topDestinations = topDestinations,
                        floorActivity = floorActivity,
                        onReviewInMapStudio = onReviewInMapStudio
                    )
                }
            }
            2 -> {
                // Search & Gaps Tab (Unmapped & zero-result discovery)
                item {
                    AnalyticsSearchGapsSection(
                        searchDemand = searchDemand,
                        zeroResultSearches = zeroResultSearches
                    )
                }
            }
            3 -> {
                // Sensors & Route Performance Tab
                item {
                    AnalyticsSensorsSection(
                        positioningDistribution = positioningDistribution,
                        metrics = overviewMetrics,
                        problematicLocations = problematicLocations,
                        onReviewInMapStudio = onReviewInMapStudio
                    )
                }
            }
            4 -> {
                // Session Log Tab
                item {
                    AnalyticsSessionLogSection(
                        sessions = recentSessions,
                        onClearDemo = onClearDemoData
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. OVERVIEW SECTION
// -------------------------------------------------------------
@Composable
private fun AnalyticsOverviewSection(
    metrics: AnalyticsOverviewMetrics,
    dateTrends: List<DateTrendMetric>,
    alerts: List<ProblematicLocationAlert>,
    onReviewInMapStudio: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // KPI Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsStatCard(
                    title = "Total Sessions",
                    value = "${metrics.totalSessions}",
                    icon = Icons.Default.DirectionsWalk,
                    accentColor = Cyan400,
                    subtitle = "${metrics.completedSessions} completed",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Success Rate",
                    value = "${metrics.successRatePercent}%",
                    icon = Icons.Default.CheckCircle,
                    accentColor = if (metrics.successRatePercent >= 75f) Emerald400 else Amber400,
                    subtitle = "${metrics.cancelledSessions} cancelled",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsStatCard(
                    title = "Avg Duration",
                    value = formatDuration(metrics.averageDurationSeconds),
                    icon = Icons.Default.Timer,
                    accentColor = Purple400,
                    subtitle = "Avg ${metrics.averageDistanceMeters.toInt()}m route",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Avg Recalculations",
                    value = "${metrics.averageRerouteCount}",
                    icon = Icons.Default.AltRoute,
                    accentColor = if (metrics.averageRerouteCount > 1.5f) Rose400 else Indigo400,
                    subtitle = "Off-routes: ${metrics.averageOffRouteCount} avg",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsStatCard(
                    title = "Accessible Journeys",
                    value = "${metrics.accessibleSessionsCount}",
                    icon = Icons.AutoMirrored.Filled.Accessible,
                    accentColor = Cyan300,
                    subtitle = "${metrics.accessibleSuccessRatePercent}% success rate",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Floor Transitions",
                    value = "${metrics.averageFloorTransitions}",
                    icon = Icons.Default.Layers,
                    accentColor = Amber400,
                    subtitle = "Vertical route avg",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sessions Trend Chart
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
                    Text("Sessions Activity Trend", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Cyan400, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("Completed", fontSize = 10.sp, color = Slate400)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Rose400, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("Cancelled", fontSize = 10.sp, color = Slate400)
                        }
                    }
                }

                if (dateTrends.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No session trend data available yet", color = Slate500, fontSize = 12.sp)
                    }
                } else {
                    TrendBarChart(trends = dateTrends, modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            }
        }

        // Actionable Real-Time Admin Alerts (Section 19)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Data-Driven Building Alerts", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Surface(
                        color = if (alerts.isNotEmpty()) Amber500.copy(alpha = 0.2f) else Emerald500.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (alerts.isNotEmpty()) "${alerts.size} Issues" else "All Clear",
                            fontSize = 10.sp,
                            color = if (alerts.isNotEmpty()) Amber300 else Emerald300,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (alerts.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Navigation graph is healthy. No excessive rerouting or unmapped search surges detected.",
                            color = Slate300,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alerts.take(4).forEach { alert ->
                            AlertCardItem(alert = alert, onReview = { onReviewInMapStudio(alert.locationId) })
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. DESTINATIONS SECTION
// -------------------------------------------------------------
@Composable
private fun AnalyticsDestinationsSection(
    topDestinations: List<TopDestinationMetric>,
    floorActivity: List<FloorActivityMetric>,
    onReviewInMapStudio: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Top Destinations Ranking Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Visited Destinations", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("By Journey Volume", fontSize = 11.sp, color = Slate400)
                }

                if (topDestinations.isEmpty()) {
                    Text("No destination journeys recorded yet.", color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    val maxSessions = max(1, topDestinations.maxOf { it.sessionCount })
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topDestinations.forEach { dest ->
                            DestinationRankRow(
                                metric = dest,
                                maxSessions = maxSessions,
                                onReview = { onReviewInMapStudio(dest.destinationNodeId) }
                            )
                        }
                    }
                }
            }
        }

        // Floor Activity Breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Activity by Floor Level", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                if (floorActivity.isEmpty()) {
                    Text("No floor activity recorded yet.", color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        floorActivity.forEach { floor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Cyan500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("F${floor.floor}", color = Cyan300, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Floor ${floor.floor}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            if (floor.topLocations.isNotEmpty()) "Top: ${floor.topLocations.joinToString(", ")}" else "No distinct nodes",
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${floor.sessionCount} journeys", color = Cyan400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${floor.transitionCount} vertical transitions", color = Slate400, fontSize = 10.sp)
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
// 3. SEARCH & GAPS SECTION (Section 11 & 12)
// -------------------------------------------------------------
@Composable
private fun AnalyticsSearchGapsSection(
    searchDemand: List<SearchMetric>,
    zeroResultSearches: List<SearchMetric>
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Zero-Result Searches (Missing Destinations Discovery)
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Amber500.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zero-Result Searches (Missing Map Data)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Surface(color = Amber500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "${zeroResultSearches.size} Gaps",
                            color = Amber300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    "Queries entered by users that returned 0 results. Use these insights to add missing rooms, offices, or facilities to the building floor plan.",
                    fontSize = 12.sp,
                    color = Slate300
                )

                if (zeroResultSearches.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("No missing destination search failures recorded.", color = Slate400, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        zeroResultSearches.forEach { zero ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber400, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("\"${zero.query}\"", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Surface(color = Rose500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "${zero.searchCount} attempts (0 found)",
                                        color = Rose300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Popular Search Demand
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Popular Search Queries", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                if (searchDemand.isEmpty()) {
                    Text("No search queries recorded yet.", color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchDemand.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.query, color = Slate200, fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${item.searchCount} searches", color = Slate400, fontSize = 11.sp)
                                    Surface(
                                        color = if (item.isZeroResult) Rose500.copy(alpha = 0.2f) else Emerald500.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (item.isZeroResult) "No results" else "${item.successfulCount} matched",
                                            color = if (item.isZeroResult) Rose300 else Emerald300,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
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
// 4. SENSORS & ROUTE PERFORMANCE (Section 8 & 21)
// -------------------------------------------------------------
@Composable
private fun AnalyticsSensorsSection(
    positioningDistribution: List<PositioningSourceMetric>,
    metrics: AnalyticsOverviewMetrics,
    problematicLocations: List<ProblematicLocationAlert>,
    onReviewInMapStudio: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Positioning Method Distribution
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Indoor Positioning Method Distribution", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(
                    "Breakdown of primary positioning providers utilized during navigation sessions.",
                    fontSize = 11.sp,
                    color = Slate400
                )

                if (positioningDistribution.isEmpty()) {
                    Text("No positioning data available yet.", color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        positioningDistribution.forEach { item ->
                            val color = when (item.source) {
                                "BLE" -> Cyan400
                                "QR" -> Emerald400
                                "GPS" -> Amber400
                                "SENSOR_FUSION" -> Purple400
                                "MANUAL" -> Slate400
                                else -> Indigo400
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.source, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${item.count} sessions (${item.percentage}%)", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Slate950, CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (item.percentage / 100f).coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .background(color, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Problematic Locations (High Reroute / Divergence Hotspots)
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Problematic Locations & Reroute Hotspots", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Surface(
                        color = if (problematicLocations.isNotEmpty()) Rose500.copy(alpha = 0.2f) else Emerald500.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${problematicLocations.size} Detected",
                            color = if (problematicLocations.isNotEmpty()) Rose300 else Emerald300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (problematicLocations.isEmpty()) {
                    Text("No problematic locations detected. User navigation paths have low rerouting overhead.", color = Slate400, fontSize = 12.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        problematicLocations.forEach { prob ->
                            AlertCardItem(alert = prob, onReview = { onReviewInMapStudio(prob.locationId) })
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. SESSION LOG SECTION (Section 28)
// -------------------------------------------------------------
@Composable
private fun AnalyticsSessionLogSection(
    sessions: List<NavigationSessionEntity>,
    onClearDemo: () -> Unit
) {
    var sessionToView by remember { mutableStateOf<NavigationSessionEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Navigation Journeys (${sessions.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            TextButton(onClick = onClearDemo) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear Demo Data", color = Slate400, fontSize = 11.sp)
            }
        }

        if (sessions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                    Text("No Navigation Sessions Yet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Start a navigation session on the map to record real journey analytics.", color = Slate400, fontSize = 12.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessions.take(20).forEach { session ->
                    val resultColor = when (session.navigationResult) {
                        "COMPLETED" -> Emerald400
                        "CANCELLED" -> Amber400
                        "FAILED", "ABANDONED" -> Rose400
                        else -> Cyan400
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                            .clickable { sessionToView = session }
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.destinationName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Surface(color = Slate800, shape = RoundedCornerShape(4.dp)) {
                                        Text("Floor ${session.destinationFloor}", color = Slate300, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    if (session.isWheelchairMode) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.Accessible, contentDescription = "Wheelchair", tint = Cyan300, modifier = Modifier.size(14.dp))
                                    }
                                    if (session.isDemoSession) {
                                        Spacer(Modifier.width(4.dp))
                                        Surface(color = Amber500.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text("DEMO", color = Amber400, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                        }
                                    }
                                }

                                Surface(color = resultColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = session.navigationResult,
                                        color = resultColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${dateFormat.format(Date(session.startTimestamp))} • ${session.primaryPositioningSource}",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = "Duration: ${formatDuration(session.actualDurationSeconds?.toFloat() ?: 0f)} • Reroutes: ${session.routeRecalculationCount}",
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Session Detail Dialog
    sessionToView?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToView = null },
            containerColor = Slate900,
            title = {
                Text("Session Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session ID: ${session.sessionId}", fontSize = 10.sp, color = Slate500, fontFamily = FontFamily.Monospace)
                    HorizontalDivider(color = Slate800)
                    DetailItem("Building", session.buildingName)
                    DetailItem("Destination", "${session.destinationName} (Floor ${session.destinationFloor})")
                    DetailItem("Start Location", "${session.startNodeName ?: "Origin"} (Floor ${session.startingFloor})")
                    DetailItem("Outcome", session.navigationResult)
                    DetailItem("Planned Distance", "${session.plannedDistanceMeters.toInt()} meters")
                    DetailItem("Actual Duration", "${session.actualDurationSeconds ?: 0} seconds")
                    DetailItem("Reroute Events", "${session.routeRecalculationCount}")
                    DetailItem("Off-Route Divergences", "${session.offRouteEventCount}")
                    DetailItem("Floor Transitions", "${session.floorTransitionsCount} (Elevators: ${session.elevatorUsageCount}, Stairs: ${session.stairsUsageCount})")
                    DetailItem("Positioning Source", session.primaryPositioningSource)
                    DetailItem("Wheelchair Mode", if (session.isWheelchairMode) "Yes" else "No")
                    if (session.floorsCrossedSummary.isNotBlank()) {
                        DetailItem("Transition Log", session.floorsCrossedSummary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sessionToView = null }) {
                    Text("Close", color = Cyan400)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// HELPER COMPONENTS & CARDS
// -------------------------------------------------------------
@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate400, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, Slate800, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Slate400
            )
        }
    }
}

@Composable
private fun DestinationRankRow(
    metric: TopDestinationMetric,
    maxSessions: Int,
    onReview: () -> Unit
) {
    val progress = (metric.sessionCount.toFloat() / maxSessions).coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Purple500.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("F${metric.floor}", color = Purple300, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(metric.destinationName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${metric.sessionCount} journeys", color = Cyan300, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(
                    onClick = onReview,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.EditLocationAlt, contentDescription = "View in Map Studio", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Slate800, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .background(Cyan400, CircleShape)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${metric.completedCount} completed", fontSize = 10.sp, color = Slate400)
            Text("Avg reroutes: ${metric.rerouteAverage}", fontSize = 10.sp, color = if (metric.rerouteAverage > 1.5f) Amber400 else Slate400)
        }
    }
}

@Composable
private fun AlertCardItem(
    alert: ProblematicLocationAlert,
    onReview: () -> Unit
) {
    val borderColor = when (alert.severity) {
        "CRITICAL" -> Rose500
        "WARNING" -> Amber500
        else -> Cyan500
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (alert.severity == "CRITICAL") Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(alert.locationName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Surface(color = borderColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(alert.severity, color = borderColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                Text(alert.description, color = Slate300, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }

        if (alert.issueType != "UNMAPPED_SEARCH") {
            IconButton(
                onClick = onReview,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Inspect Node", tint = Cyan400, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TrendBarChart(
    trends: List<DateTrendMetric>,
    modifier: Modifier = Modifier
) {
    val maxVal = max(1, trends.maxOf { it.totalSessions })

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barCount = trends.size
        val gap = 12f
        val barWidth = ((w - (barCount + 1) * gap) / barCount).coerceAtLeast(16f)

        trends.forEachIndexed { i, item ->
            val x = gap + i * (barWidth + gap)
            val totalHeight = (item.totalSessions.toFloat() / maxVal) * (h - 24f)
            val completedHeight = (item.completedSessions.toFloat() / maxVal) * (h - 24f)
            val cancelledHeight = totalHeight - completedHeight

            // Background / cancelled portion
            if (totalHeight > 0) {
                drawRoundRect(
                    color = Rose400.copy(alpha = 0.6f),
                    topLeft = Offset(x, h - totalHeight),
                    size = Size(barWidth, totalHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Completed portion
            if (completedHeight > 0) {
                drawRoundRect(
                    color = Cyan400,
                    topLeft = Offset(x, h - completedHeight),
                    size = Size(barWidth, completedHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Float): String {
    val secInt = seconds.toInt()
    val mins = secInt / 60
    val remSec = secInt % 60
    return if (mins > 0) "${mins}m ${remSec}s" else "${remSec}s"
}
