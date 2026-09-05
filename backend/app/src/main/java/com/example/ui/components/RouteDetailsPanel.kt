package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.model.NavigationRoute
import com.example.ui.theme.*

@Composable
fun RouteDetailsPanel(
    route: NavigationRoute?,
    activeStepIndex: Int,
    isWheelchairMode: Boolean,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onReplayVoice: () -> Unit,
    onClearRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (route == null) return

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
            // Header & Origin-Destination
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROUTE DETAILS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400,
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = onClearRoute,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Route",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Origin -> Destination row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Cyan900.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "L${route.startNode.floor}",
                                color = Cyan400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = route.startNode.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(16.dp)
                        )

                        Surface(
                            color = Purple900.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "L${route.targetNode.floor}",
                                color = Purple400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = route.targetNode.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Key Metrics Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricItem(
                            label = "Distance",
                            value = "${route.totalDistanceMeters.toInt()} m",
                            icon = Icons.Default.Straighten,
                            tint = Cyan400
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Slate700)
                        MetricItem(
                            label = "Est. Time",
                            value = "${route.estimatedMinutes} min",
                            icon = Icons.Default.Timer,
                            tint = Purple500
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Slate700)
                        MetricItem(
                            label = "Steps",
                            value = "${route.steps.size}",
                            icon = Icons.Default.DirectionsWalk,
                            tint = Emerald400
                        )
                    }
                }
            }

            // Route Summary string
            item {
                Surface(
                    color = Slate800.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Summary: ${route.pathNodes.joinToString(" → ") { it.name }}",
                        color = Slate300,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Turn-by-Turn Navigation Steps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step-by-Step Instructions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onReplayVoice,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Voice Readout",
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            itemsIndexed(route.steps) { index, step ->
                val isActive = index == activeStepIndex
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) Cyan500.copy(alpha = 0.15f) else Slate800.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isActive) Cyan500 else Slate800,
                            RoundedCornerShape(12.dp)
                        )
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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Cyan500 else Slate700),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (isActive) Slate950 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.instruction,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Floor ${step.floor} • Target: ${step.targetNode.name}",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }

                        val change = step.floorChange
                        if (change != null && change != 0) {
                            Surface(
                                color = Purple500.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (change > 0) "▲ Up" else "▼ Down",
                                    color = Purple500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevStep,
                        enabled = activeStepIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous Step", color = if (activeStepIndex > 0) Slate200 else Slate600, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onNextStep,
                        enabled = activeStepIndex < route.steps.size - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next Step", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = label, color = Slate400, fontSize = 10.sp)
    }
}
