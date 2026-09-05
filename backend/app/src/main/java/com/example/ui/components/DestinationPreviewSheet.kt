package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Building
import com.example.data.model.NavNode
import com.example.data.model.NodeCategory
import com.example.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun DestinationPreviewSheet(
    building: Building,
    destination: NavNode,
    currentFloor: Int,
    userPositionX: Float?,
    userPositionY: Float?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStartNavigation: () -> Unit,
    onViewRoute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDifferentFloor = destination.floor != currentFloor
    val floorName = building.floors.firstOrNull { it.floorNumber == destination.floor }?.name ?: "Level ${destination.floor}"
    val currentFloorName = building.floors.firstOrNull { it.floorNumber == currentFloor }?.shortName ?: "L$currentFloor"
    val destFloorName = building.floors.firstOrNull { it.floorNumber == destination.floor }?.shortName ?: "L${destination.floor}"

    val distanceMeters = if (userPositionX != null && userPositionY != null) {
        val dx = destination.x - userPositionX
        val dy = destination.y - userPositionY
        (hypot(dx, dy) / 10f).roundToInt()
    } else {
        null
    }

    Surface(
        color = Slate900.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("destination_preview_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Slate700)
                    .align(Alignment.CenterHorizontally)
            )

            // Header: Category Icon, Name, Room Code, Favorite & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (destination.category) {
                                NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Red500.copy(alpha = 0.2f)
                                NodeCategory.PHARMACY -> Emerald500.copy(alpha = 0.2f)
                                NodeCategory.LIBRARY, NodeCategory.CLASSROOM, NodeCategory.LABORATORY -> Cyan500.copy(alpha = 0.2f)
                                else -> Purple500.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (destination.category) {
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
                        tint = when (destination.category) {
                            NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Red400
                            NodeCategory.PHARMACY -> Emerald400
                            NodeCategory.LIBRARY, NodeCategory.CLASSROOM, NodeCategory.LABORATORY -> Cyan400
                            else -> Purple400
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destination.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = destination.code,
                                color = Cyan400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "•",
                            color = Slate500,
                            fontSize = 12.sp
                        )
                        Text(
                            text = building.name,
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("preview_fav_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Amber400 else Slate400
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("preview_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate400
                    )
                }
            }

            HorizontalDivider(color = Slate800)

            // Info Grid: Floor location + Multi-floor transfer badge + Accessibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Floor indicator
                Surface(
                    color = Slate800.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text("Floor", color = Slate400, fontSize = 10.sp)
                            Text(floorName, color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Multi-floor badge or Distance
                Surface(
                    color = if (isDifferentFloor) Amber500.copy(alpha = 0.15f) else Slate800.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isDifferentFloor) androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDifferentFloor) Icons.Default.AltRoute else Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isDifferentFloor) Amber400 else Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isDifferentFloor) "Floor Transition" else "Distance",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (isDifferentFloor) "$currentFloorName → $destFloorName" else "${distanceMeters ?: "~50"} m away",
                                color = if (isDifferentFloor) Amber400 else Slate100,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Description / Keywords / Accessibility
            if (destination.description.isNotBlank() || destination.isAccessible) {
                Surface(
                    color = Slate950.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (destination.description.isNotBlank()) {
                            Text(
                                text = destination.description,
                                color = Slate300,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (destination.isAccessible) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Accessible,
                                    contentDescription = "Wheelchair Accessible",
                                    tint = Cyan400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Wheelchair accessible facility",
                                    color = Cyan400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Actions: START NAVIGATION & VIEW ROUTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewRoute,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("preview_view_route_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Route", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("preview_start_nav_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
