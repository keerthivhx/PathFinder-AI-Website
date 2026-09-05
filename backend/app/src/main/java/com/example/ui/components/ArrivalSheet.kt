package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.theme.*

@Composable
fun ArrivalSheet(
    building: Building,
    destination: NavNode,
    totalDistanceMeters: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val floorName = building.floors.firstOrNull { it.floorNumber == destination.floor }?.name ?: "Level ${destination.floor}"

    Surface(
        color = Slate900.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald400.copy(alpha = 0.6f)),
        shadowElevation = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("arrival_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Big Animated Checkmark badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = 0.2f))
                    .border(2.dp, Emerald400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Arrived",
                    tint = Emerald400,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = "You have arrived!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = destination.name,
                color = Emerald300,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // Location details card
            Surface(
                color = Slate950.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Room Code", color = Slate400, fontSize = 10.sp)
                        Text(destination.code, color = Cyan400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Slate800)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Floor", color = Slate400, fontSize = 10.sp)
                        Text(floorName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Slate800)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Building", color = Slate400, fontSize = 10.sp)
                        Text(building.name.take(12), color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exit Navigation Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Slate950),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("arrival_done_btn")
            ) {
                Text("Done / Exit Navigation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
