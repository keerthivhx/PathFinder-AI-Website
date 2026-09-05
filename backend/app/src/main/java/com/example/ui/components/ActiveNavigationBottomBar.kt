package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.example.data.model.NavSessionState
import com.example.data.model.NavigationRoute
import com.example.data.model.TurnType
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ActiveNavigationBottomBar(
    route: NavigationRoute,
    navSessionState: NavSessionState,
    currentFloor: Int,
    remainingDistanceMeters: Float,
    remainingTimeMinutes: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onPauseNavigation: () -> Unit,
    onResumeNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onOpenRouteDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = navSessionState == NavSessionState.PAUSED
    val isOffRoute = navSessionState == NavSessionState.OFF_ROUTE || navSessionState == NavSessionState.RECALCULATING
    val destNode = route.destinationNode

    val hasElevator = route.steps.any { it.turnType == TurnType.ELEVATOR_UP || it.turnType == TurnType.ELEVATOR_DOWN }
    val isDifferentFloor = destNode.floor != currentFloor

    Surface(
        color = Slate900.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOffRoute) Red500.copy(alpha = 0.5f) else Cyan500.copy(alpha = 0.3f)
        ),
        shadowElevation = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_nav_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Remaining time, Distance, Destination, Floor summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ETA & Distance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ETA Time badge
                    Text(
                        text = "$remainingTimeMinutes min",
                        color = if (isPaused) Amber400 else if (isOffRoute) Red400 else Emerald400,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )

                    Column {
                        Text(
                            text = "${remainingDistanceMeters.roundToInt()} m remaining",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isDifferentFloor) {
                                "Level $currentFloor → Level ${destNode.floor} (${if (hasElevator) "Elevator" else "Stairs"})"
                            } else {
                                "To ${destNode.name}"
                            },
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                // Stop Navigation Button (Red circular button, Google Maps style)
                Surface(
                    color = Red500.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onStopNavigation() }
                        .testTag("stop_nav_btn")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Navigation",
                            tint = Red400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800, thickness = 1.dp)

            // Bottom Controls Row: Pause/Resume, Voice Toggle, Directions Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Voice Toggle (🔊 / 🔇)
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { onToggleMute() }
                        .testTag("nav_voice_toggle_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Voice Guidance",
                            tint = if (isMuted) Slate400 else Cyan400,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isMuted) "Muted" else "Voice On",
                            color = if (isMuted) Slate400 else Cyan300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Center: Pause / Resume Button
                Surface(
                    color = if (isPaused) Amber500 else Slate800,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable {
                            if (isPaused) onResumeNavigation() else onPauseNavigation()
                        }
                        .testTag("nav_pause_resume_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) Slate950 else Slate200,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            color = if (isPaused) Slate950 else Slate200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right: Route Steps List Button
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { onOpenRouteDetails() }
                        .testTag("nav_route_steps_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Directions List",
                            tint = Purple400,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Steps",
                            color = Purple400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
