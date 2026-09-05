package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NavNode
import com.example.data.model.NodeCategory
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailBottomSheet(
    node: NavNode,
    onSetAsStart: (NavNode) -> Unit,
    onNavigateHere: (NavNode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Slate700
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("node_detail_bottom_sheet")
        ) {
            // Header with Node Code and Category Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                when (node.category) {
                                    NodeCategory.EMERGENCY, NodeCategory.EMERGENCY_EXIT -> Red500
                                    NodeCategory.PHARMACY -> Emerald500
                                    NodeCategory.ELEVATOR, NodeCategory.STAIRS -> Amber500
                                    NodeCategory.RESTROOM -> Purple500
                                    else -> Cyan500
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (node.category) {
                                NodeCategory.ENTRANCE -> Icons.Default.DoorFront
                                NodeCategory.EMERGENCY, NodeCategory.TRIAGE -> Icons.Default.Emergency
                                NodeCategory.PHARMACY -> Icons.Default.LocalPharmacy
                                NodeCategory.RADIOLOGY, NodeCategory.LABORATORY -> Icons.Default.Biotech
                                NodeCategory.CARDIOLOGY -> Icons.Default.Favorite
                                NodeCategory.SURGERY -> Icons.Default.MedicalServices
                                NodeCategory.ELEVATOR -> Icons.Default.Elevator
                                NodeCategory.STAIRS -> Icons.Default.Stairs
                                NodeCategory.RESTROOM -> Icons.Default.Wc
                                NodeCategory.AIRPORT_GATE -> Icons.Default.Flight
                                NodeCategory.SECURITY_CHECK -> Icons.Default.Security
                                NodeCategory.CHECK_IN -> Icons.Default.Luggage
                                NodeCategory.DUTY_FREE, NodeCategory.STORE -> Icons.Default.ShoppingBag
                                NodeCategory.FOOD_COURT -> Icons.Default.Fastfood
                                NodeCategory.CLASSROOM, NodeCategory.AUDITORIUM -> Icons.Default.School
                                NodeCategory.ACCOUNTS -> Icons.Default.Payments
                                NodeCategory.LIBRARY -> Icons.Default.MenuBook
                                NodeCategory.EMERGENCY_EXIT -> Icons.Default.ExitToApp
                                else -> Icons.Default.Place
                            },
                            contentDescription = null,
                            tint = Slate950,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = node.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${node.code}  •  Level ${node.floor}",
                            color = Cyan300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (node.isAccessible) {
                    Surface(
                        color = Emerald900,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Accessible, contentDescription = null, tint = Emerald400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accessible", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            Text(
                text = node.description,
                color = Slate300,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (node.visualSignageHint.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Signpost, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visual Landmark: \"${node.visualSignageHint}\"",
                            color = Slate200,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Set as Start / Navigate Here)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onSetAsStart(node)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Cyan300),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_as_start_button")
                ) {
                    Icon(Icons.Default.TripOrigin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set as Start", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onNavigateHere(node)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("navigate_here_button")
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate Here", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
