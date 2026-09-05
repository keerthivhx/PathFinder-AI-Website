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
import com.example.data.model.NavigationRoute
import com.example.data.model.TurnType
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun RoutePreviewSheet(
    building: Building,
    route: NavigationRoute,
    isWheelchairMode: Boolean,
    onToggleWheelchair: () -> Unit,
    onStartNavigation: () -> Unit,
    onChangeRoute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startNode = route.startNode
    val destNode = route.destinationNode

    // Floor transition analysis
    val hasFloorChange = route.pathNodes.map { it.floor }.distinct().size > 1
    val hasElevator = route.steps.any { it.turnType == TurnType.ELEVATOR_UP || it.turnType == TurnType.ELEVATOR_DOWN }
    val hasStairs = route.steps.any { it.turnType == TurnType.STAIRS_UP || it.turnType == TurnType.STAIRS_DOWN }

    val totalDistanceMeters = route.totalDistanceMeters.roundToInt()
    val estMinutes = (totalDistanceMeters / (1.2f * 60f)).roundToInt().coerceAtLeast(1)

    Surface(
        color = Slate900.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.3f)),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("route_preview_sheet")
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

            // Header: Route Preview Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Route Preview",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${building.name} Indoor Waypoint",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("route_preview_close_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }

            // Origin -> Destination card
            Surface(
                color = Slate950.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Cyan400)
                        )
                        Column {
                            Text("Current Location", color = Slate400, fontSize = 10.sp)
                            Text(
                                text = "${startNode.name} (Level ${startNode.floor})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Vertical connecting dots
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(2.dp)
                            .height(14.dp)
                            .background(Slate700)
                    )

                    // Destination
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Emerald400)
                        )
                        Column {
                            Text("Destination", color = Slate400, fontSize = 10.sp)
                            Text(
                                text = "${destNode.name} (Level ${destNode.floor})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Summary stats: Distance • ETA • Floor Changes • Transfer Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Distance
                Surface(
                    color = Slate800.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Distance", color = Slate400, fontSize = 10.sp)
                        Text("$totalDistanceMeters m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Estimated Time
                Surface(
                    color = Slate800.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Est. Time", color = Slate400, fontSize = 10.sp)
                        Text("$estMinutes min", color = Emerald400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Floor changes
                Surface(
                    color = Slate800.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Floors", color = Slate400, fontSize = 10.sp)
                        Text(
                            text = if (hasFloorChange) {
                                if (hasElevator) "Elevator" else "Stairs"
                            } else "Same Floor",
                            color = if (hasFloorChange) Amber400 else Cyan400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Route Settings: Wheelchair Accessible Route Toggle
            Surface(
                color = if (isWheelchairMode) Cyan500.copy(alpha = 0.12f) else Slate950.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isWheelchairMode) Cyan500.copy(alpha = 0.4f) else Slate800),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWheelchair() }
                    .testTag("route_preview_wheelchair_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Accessible,
                            contentDescription = "Accessible Mode",
                            tint = if (isWheelchairMode) Cyan400 else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Wheelchair Accessible Route",
                                color = if (isWheelchairMode) Cyan300 else Slate200,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isWheelchairMode) "Ramps & elevators only (avoid stairs)" else "Standard shortest path",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = isWheelchairMode,
                        onCheckedChange = { onToggleWheelchair() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = Cyan400,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }
            }

            // Action Buttons: Change Route & START NAVIGATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeRoute,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("route_preview_change_btn")
                ) {
                    Text("Change", fontSize = 13.sp)
                }

                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("route_preview_start_btn")
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
