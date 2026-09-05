package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun PitchHackathonDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("pitch_hackathon_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Cyan500, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Slate950, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PathFounder AI",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero-Hardware Indoor Navigation",
                                color = Cyan400,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Problem vs The Solution Card
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "THE PROBLEM",
                            color = Red500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GPS fails indoors. Traditional indoor navigation requires thousands of dollars in Bluetooth beacons, complex WiFi fingerprinting, and ongoing battery maintenance.",
                            color = Slate300,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "THE ZERO-HARDWARE SOLUTION",
                            color = Emerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PathFounder AI combines Google Gemini LLMs, Gemini Vision photo scanning, QR spatial anchors, and dead-reckoning graph algorithms for $0 hardware cost and 100% building coverage.",
                            color = Slate200,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature Highlights Grid
                Text(
                    text = "Core Architectural Innovations:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PitchPill(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Natural Language & Symptom Intent",
                    desc = "Users speak naturally (\"My ankle is broken\", \"Where is Gate B4?\") and Gemini routes directly to the exact department.",
                    color = Cyan400
                )

                Spacer(modifier = Modifier.height(8.dp))

                PitchPill(
                    icon = Icons.Default.CameraAlt,
                    title = "Gemini Vision Landmark Scanner",
                    desc = "Photograph any door plaque or room sign to instantly calibrate indoor position with sub-meter spatial accuracy.",
                    color = Emerald400
                )

                Spacer(modifier = Modifier.height(8.dp))

                PitchPill(
                    icon = Icons.Default.Accessible,
                    title = "Universal Accessibility & Fire Safety",
                    desc = "Step-free wheelchair routing avoids stairs. 1-tap emergency evacuation routes to the nearest fire exit.",
                    color = Amber500
                )

                Spacer(modifier = Modifier.height(8.dp))

                PitchPill(
                    icon = Icons.Default.ViewInAr,
                    title = "Spatial AR HUD & Voice Engine",
                    desc = "Turn-by-turn spoken guidance synced with 3D perspective AR waypoints.",
                    color = Purple500
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Explore Live App", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PitchPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color
) {
    Surface(
        color = Slate800,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, color = Slate400, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}
