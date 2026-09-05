package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Building
import com.example.data.model.NavNode
import com.example.data.model.NodeCategory
import com.example.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealDestinationSearchView(
    building: Building,
    currentFloor: Int,
    userPositionX: Float?,
    userPositionY: Float?,
    favoriteNodeIds: Set<String>,
    recentNodeIds: List<String>,
    onSelectDestination: (NavNode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClose: () -> Unit,
    onSearchPerformed: ((query: String, count: Int, category: NodeCategory?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<NodeCategory?>(null) }
    var accessibleOnly by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Request focus on open
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val categoryFilters = listOf(
        null to "All",
        NodeCategory.CLASSROOM to "Classrooms",
        NodeCategory.LABORATORY to "Laboratories",
        NodeCategory.LIBRARY to "Library",
        NodeCategory.FACULTY to "Faculty",
        NodeCategory.RESTROOM to "Restrooms",
        NodeCategory.EMERGENCY to "Emergency",
        NodeCategory.RECEPTION to "Reception",
        NodeCategory.PHARMACY to "Pharmacy",
        NodeCategory.FOOD_COURT to "Cafeteria",
        NodeCategory.AUDITORIUM to "Auditorium",
        NodeCategory.ELEVATOR to "Elevators",
        NodeCategory.STAIRS to "Stairs"
    )

    // Filtered search results
    val searchResults = remember(searchQuery, selectedCategory, accessibleOnly, building) {
        val cleanQuery = searchQuery.trim().lowercase()

        building.nodes.filter { node ->
            if (accessibleOnly && !node.isAccessible) return@filter false

            val matchesCategory = selectedCategory == null || node.category == selectedCategory
            if (!matchesCategory) return@filter false

            if (cleanQuery.isEmpty()) return@filter true

            val matchesFloor = cleanQuery.contains("floor ${node.floor}") ||
                cleanQuery.contains("level ${node.floor}") ||
                cleanQuery.contains("f${node.floor}") ||
                cleanQuery.contains("l${node.floor}") ||
                (node.floor == 0 && (cleanQuery.contains("ground") || cleanQuery == "g"))

            val matchesCategoryName = node.category.name.lowercase().contains(cleanQuery) ||
                node.category.name.replace("_", " ").lowercase().contains(cleanQuery)

            matchesFloor ||
                matchesCategoryName ||
                node.name.contains(cleanQuery, ignoreCase = true) ||
                node.code.contains(cleanQuery, ignoreCase = true) ||
                node.keywords.any { it.contains(cleanQuery, ignoreCase = true) } ||
                node.description.contains(cleanQuery, ignoreCase = true)
        }
    }

    // Autocomplete quick suggestions (when user types >= 2 chars)
    val autocompleteSuggestions = remember(searchQuery, building) {
        if (searchQuery.trim().length >= 2) {
            val q = searchQuery.trim().lowercase()
            building.nodes.filter {
                it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            }.take(3)
        } else {
            emptyList()
        }
    }

    // Recent destinations
    val recentNodes = remember(recentNodeIds, building) {
        recentNodeIds.mapNotNull { id -> building.nodes.firstOrNull { it.id == id } }.distinctBy { it.id }
    }

    // Favorite destinations
    val favoriteNodes = remember(favoriteNodeIds, building) {
        favoriteNodeIds.mapNotNull { id -> building.nodes.firstOrNull { it.id == id } }.distinctBy { it.id }
    }

    // Popular campus destinations
    val popularNodes = remember(building) {
        building.nodes.filter {
            it.category == NodeCategory.LABORATORY ||
                it.category == NodeCategory.LIBRARY ||
                it.category == NodeCategory.FOOD_COURT ||
                it.category == NodeCategory.TRIAGE ||
                it.category == NodeCategory.AUDITORIUM
        }.take(4)
    }

    Surface(
        color = Slate950,
        modifier = modifier
            .fillMaxSize()
            .testTag("real_destination_search_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            // 1. Search Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("search_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Slate200
                    )
                }

                // Main Search Input
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Where to? Room, lab, stairs, floor...",
                            color = Slate400,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Cyan400)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    // Voice search prompt suggestion
                                    searchQuery = "AI Laboratory"
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = Cyan400, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        if (searchQuery.isNotBlank()) {
                            onSearchPerformed?.invoke(searchQuery, searchResults.size, selectedCategory)
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate900,
                        unfocusedContainerColor = Slate900,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("search_input_field")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Category Filter Chips + Accessible Routes Toggle Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accessible Routes chip
                FilterChip(
                    selected = accessibleOnly,
                    onClick = { accessibleOnly = !accessibleOnly },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Accessible, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Accessible Routes", fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Cyan500,
                        selectedLabelColor = Slate950,
                        containerColor = Slate900,
                        labelColor = Slate300
                    ),
                    shape = RoundedCornerShape(18.dp)
                )

                // Category chips
                categoryFilters.forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else cat },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple500,
                            selectedLabelColor = Color.White,
                            containerColor = Slate900,
                            labelColor = Slate300
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }

            // 3. Autocomplete Suggestions (instant chips)
            if (autocompleteSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Suggestions:", color = Slate400, fontSize = 11.sp)
                    autocompleteSuggestions.forEach { node ->
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { onSelectDestination(node) }
                        ) {
                            Text(
                                text = node.name,
                                color = Cyan300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate800.copy(alpha = 0.6f), modifier = Modifier.padding(top = 6.dp))

            // 4. Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // When no search query is typed and no category is selected: show Recents, Favorites, Popular
                if (searchQuery.isBlank() && selectedCategory == null && !accessibleOnly) {
                    // Recent Searches
                    if (recentNodes.isNotEmpty()) {
                        item {
                            Text(
                                text = "RECENT DESTINATIONS",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(recentNodes) { node ->
                            SearchResultItem(
                                node = node,
                                building = building,
                                currentFloor = currentFloor,
                                userPositionX = userPositionX,
                                userPositionY = userPositionY,
                                isFavorite = favoriteNodeIds.contains(node.id),
                                onToggleFavorite = { onToggleFavorite(node.id) },
                                onSelect = { onSelectDestination(node) }
                            )
                        }
                    }

                    // Favorites
                    if (favoriteNodes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "STARRED & FAVORITES",
                                color = Amber400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(favoriteNodes) { node ->
                            SearchResultItem(
                                node = node,
                                building = building,
                                currentFloor = currentFloor,
                                userPositionX = userPositionX,
                                userPositionY = userPositionY,
                                isFavorite = true,
                                onToggleFavorite = { onToggleFavorite(node.id) },
                                onSelect = { onSelectDestination(node) }
                            )
                        }
                    }

                    // Popular Campus Destinations
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "POPULAR IN ${building.name.uppercase()}",
                            color = Cyan400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(popularNodes) { node ->
                        SearchResultItem(
                            node = node,
                            building = building,
                            currentFloor = currentFloor,
                            userPositionX = userPositionX,
                            userPositionY = userPositionY,
                            isFavorite = favoriteNodeIds.contains(node.id),
                            onToggleFavorite = { onToggleFavorite(node.id) },
                            onSelect = { onSelectDestination(node) }
                        )
                    }
                } else {
                    // Filtered Results
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${searchResults.size} LOCATIONS FOUND",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (searchQuery.isNotBlank() || selectedCategory != null || accessibleOnly) {
                                Text(
                                    text = "Clear filters",
                                    color = Cyan400,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable {
                                        searchQuery = ""
                                        selectedCategory = null
                                        accessibleOnly = false
                                    }
                                )
                            }
                        }
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            Surface(
                                color = Slate900,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                                    Text("No locations found", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Try searching for another room name, code, department, or select a category filter.",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(searchResults) { node ->
                            SearchResultItem(
                                node = node,
                                building = building,
                                currentFloor = currentFloor,
                                userPositionX = userPositionX,
                                userPositionY = userPositionY,
                                isFavorite = favoriteNodeIds.contains(node.id),
                                onToggleFavorite = { onToggleFavorite(node.id) },
                                onSelect = { onSelectDestination(node) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    node: NavNode,
    building: Building,
    currentFloor: Int,
    userPositionX: Float?,
    userPositionY: Float?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val floorName = building.floors.firstOrNull { it.floorNumber == node.floor }?.shortName ?: "L${node.floor}"
    val isDifferentFloor = node.floor != currentFloor

    val distanceMeters = if (userPositionX != null && userPositionY != null) {
        (hypot(node.x - userPositionX, node.y - userPositionY) / 10f).roundToInt()
    } else {
        null
    }

    Surface(
        color = Slate900,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("search_result_item_${node.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (node.category) {
                            NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Red500.copy(alpha = 0.2f)
                            NodeCategory.PHARMACY -> Emerald500.copy(alpha = 0.2f)
                            NodeCategory.LIBRARY, NodeCategory.CLASSROOM, NodeCategory.LABORATORY -> Cyan500.copy(alpha = 0.2f)
                            else -> Purple500.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (node.category) {
                        NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Icons.Default.Warning
                        NodeCategory.PHARMACY -> Icons.Default.LocalPharmacy
                        NodeCategory.LABORATORY -> Icons.Default.Science
                        NodeCategory.LIBRARY -> Icons.Default.MenuBook
                        NodeCategory.RESTROOM -> Icons.Default.Wc
                        NodeCategory.FOOD_COURT -> Icons.Default.Restaurant
                        NodeCategory.ELEVATOR -> Icons.Default.Elevator
                        NodeCategory.STAIRS -> Icons.Default.Stairs
                        else -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = when (node.category) {
                        NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Red400
                        NodeCategory.PHARMACY -> Emerald400
                        NodeCategory.LIBRARY, NodeCategory.CLASSROOM, NodeCategory.LABORATORY -> Cyan400
                        else -> Purple400
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Name, Floor, Code & Description
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = node.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = node.code,
                            color = Cyan400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Floor Badge
                    Text(
                        text = "Level ${node.floor} ($floorName)",
                        color = if (isDifferentFloor) Amber400 else Slate300,
                        fontSize = 11.sp,
                        fontWeight = if (isDifferentFloor) FontWeight.Bold else FontWeight.Normal
                    )

                    if (distanceMeters != null) {
                        Text("•", color = Slate500, fontSize = 11.sp)
                        Text("$distanceMeters m away", color = Slate400, fontSize = 11.sp)
                    }

                    if (node.isAccessible) {
                        Text("•", color = Slate500, fontSize = 11.sp)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Accessible,
                            contentDescription = "Accessible",
                            tint = Cyan400,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (node.description.isNotBlank()) {
                    Text(
                        text = node.description,
                        color = Slate400,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Star Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Amber400 else Slate600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
