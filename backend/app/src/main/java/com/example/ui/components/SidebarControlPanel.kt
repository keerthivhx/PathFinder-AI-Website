package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun SidebarControlPanel(
    building: Building,
    activeFloor: Int,
    startNode: NavNode?,
    destinationNode: NavNode?,
    isWheelchairMode: Boolean,
    isEmergencyMode: Boolean,
    favoriteNodeIds: Set<String>,
    recentNodeIds: List<String>,
    onSelectBuilding: (String) -> Unit,
    onSelectFloor: (Int) -> Unit,
    onSetStartNode: (NavNode) -> Unit,
    onSetDestinationNode: (NavNode?) -> Unit,
    onToggleWheelchair: () -> Unit,
    onToggleEmergency: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenQRScanner: () -> Unit,
    onOpenLandmarkScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<NodeCategory?>(null) }
    var showStartDropdown by remember { mutableStateOf(false) }

    val categoryFilters = listOf(
        null to "All",
        NodeCategory.LABORATORY to "Labs",
        NodeCategory.FACULTY to "Offices",
        NodeCategory.FOOD_COURT to "Cafeteria",
        NodeCategory.LIBRARY to "Library",
        NodeCategory.RESTROOM to "Restrooms",
        NodeCategory.ELEVATOR to "Elevators",
        NodeCategory.EMERGENCY_EXIT to "Exits"
    )

    val searchResults = remember(searchQuery, selectedCategoryFilter, building) {
        building.nodes.filter { node ->
            val matchesCategory = selectedCategoryFilter == null || node.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() ||
                node.name.contains(searchQuery, ignoreCase = true) ||
                node.code.contains(searchQuery, ignoreCase = true) ||
                node.keywords.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(20.dp))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. LOCATION ROUTING INPUTS (START -> DESTINATION)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate800.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Node
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Cyan500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Location / Start", color = Slate400, fontSize = 10.sp)
                            Text(
                                text = startNode?.let { "${it.name} (L${it.floor})" } ?: "Select start point",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showStartDropdown = !showStartDropdown },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (showStartDropdown) Icons.Default.ExpandLess else Icons.Default.Edit,
                                contentDescription = "Change start",
                                tint = Cyan400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showStartDropdown) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(building.nodes) { n ->
                                val isSelected = startNode?.id == n.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onSetStartNode(n)
                                        showStartDropdown = false
                                    },
                                    label = { Text(n.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Cyan500,
                                        selectedLabelColor = Slate950,
                                        containerColor = Slate900,
                                        labelColor = Slate300
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Slate700, thickness = 0.5.dp)

                    // Destination Node
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Purple500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Purple500, modifier = Modifier.size(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Destination", color = Slate400, fontSize = 10.sp)
                            Text(
                                text = destinationNode?.let { "${it.name} (L${it.floor})" } ?: "Search destination below...",
                                color = if (destinationNode != null) Color.White else Slate500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (destinationNode != null) {
                            IconButton(
                                onClick = { onSetDestinationNode(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 2. SEARCH DESTINATION INPUT
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search room, lab, department, facility...", color = Slate500, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate800,
                        unfocusedContainerColor = Slate800,
                        focusedBorderColor = Cyan500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_search_input")
                )
            }

            // 3. CATEGORY FILTERS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryFilters) { (cat, label) ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan500,
                                selectedLabelColor = Slate950,
                                containerColor = Slate800,
                                labelColor = Slate300
                            )
                        )
                    }
                }
            }

            // 4. ACCESSIBILITY & EMERGENCY TOGGLES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wheelchair toggle
                    Surface(
                        color = if (isWheelchairMode) Amber500.copy(alpha = 0.2f) else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isWheelchairMode) Amber500 else Slate700
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleWheelchair() }
                            .testTag("toggle_wheelchair_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Accessible,
                                contentDescription = null,
                                tint = if (isWheelchairMode) Amber500 else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isWheelchairMode) "Step-Free ON" else "Accessible",
                                color = if (isWheelchairMode) Amber500 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Emergency 1-tap Evacuation
                    Surface(
                        color = if (isEmergencyMode) Rose500.copy(alpha = 0.3f) else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEmergencyMode) Rose500 else Slate700
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleEmergency() }
                            .testTag("toggle_emergency_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isEmergencyMode) Rose500 else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isEmergencyMode) "EVACUATION" else "Emergency",
                                color = if (isEmergencyMode) Rose500 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. SEARCH RESULTS LIST (AUTOCOMPLETE)
            item {
                Text(
                    text = "Locations (${searchResults.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            }

            items(searchResults.take(6)) { node ->
                val isDest = destinationNode?.id == node.id
                val isFav = favoriteNodeIds.contains(node.id)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDest) Purple500.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isDest) Purple500 else Slate700,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onSetDestinationNode(node)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Cyan900.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "L${node.floor}",
                                    color = Cyan400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                    text = "${node.code} • ${node.category.displayName}",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleFavorite(node.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Amber500 else Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 6. SENSORS & VISION QUICK ACTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenQRScanner,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Anchor", color = Slate200, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenLandmarkScanner,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Purple500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Vision", color = Slate200, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
