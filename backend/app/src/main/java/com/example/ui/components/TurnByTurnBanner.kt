package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TurnByTurnBanner(
    route: NavigationRoute,
    activeStepIndex: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onReplayVoice: () -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onCancelRoute: () -> Unit,
    onToggleAR: () -> Unit,
    isARActive: Boolean,
    distanceToNextActionMeters: Int? = null,
    modifier: Modifier = Modifier
) {
    val currentStep = route.steps.getOrNull(activeStepIndex) ?: return
    val nextStep = route.steps.getOrNull(activeStepIndex + 1)
    val totalSteps = route.steps.size
    val isEmergency = route.isEmergencyRoute

    val displayDistance = distanceToNextActionMeters ?: currentStep.distanceMeters.roundToInt()

    Surface(
        color = if (isEmergency) Red950() else Slate900.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isEmergency) Red500 else Cyan500.copy(alpha = 0.6f)
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("turn_by_turn_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Step icon, Instruction text, Replay / Mute / AR buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Turn Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (isEmergency) Red500 else Cyan500, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentStep.turnType) {
                            TurnType.LEFT, TurnType.SHARP_LEFT, TurnType.SLIGHT_LEFT -> Icons.Default.TurnLeft
                            TurnType.RIGHT, TurnType.SHARP_RIGHT, TurnType.SLIGHT_RIGHT -> Icons.Default.TurnRight
                            TurnType.ELEVATOR_UP, TurnType.ELEVATOR_DOWN -> Icons.Default.Elevator
                            TurnType.STAIRS_UP, TurnType.STAIRS_DOWN -> Icons.Default.Stairs
                            TurnType.RAMP_UP, TurnType.RAMP_DOWN -> Icons.Default.Accessible
                            TurnType.EMERGENCY_EVACUATE -> Icons.Default.Warning
                            TurnType.ARRIVAL -> Icons.Default.CheckCircle
                            else -> Icons.Default.Straight
                        },
                        contentDescription = "Turn Action",
                        tint = Slate950,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Instruction & Detail
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isEmergency) Red500.copy(alpha = 0.3f) else Cyan500.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (displayDistance <= 3) "Now" else "$displayDistance m",
                                color = if (isEmergency) Red400 else Cyan300,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = currentStep.instruction,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (currentStep.detail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentStep.detail,
                            color = if (isEmergency) Amber500 else Slate300,
                            fontSize = 12.sp
                        )
                    }
                    if (nextStep != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Then:", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${nextStep.instruction} (${nextStep.distanceMeters.roundToInt()}m)",
                                color = Slate300,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // AR Mode Toggle
                IconButton(
                    onClick = onToggleAR,
                    modifier = Modifier
                        .background(if (isARActive) Cyan500 else Slate800, CircleShape)
                        .size(36.dp)
                        .testTag("toggle_ar_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "AR View",
                        tint = if (isARActive) Slate950 else Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Voice Replay / Mute Button
                IconButton(
                    onClick = onReplayVoice,
                    modifier = Modifier
                        .background(Slate800, CircleShape)
                        .size(36.dp)
                        .testTag("replay_voice_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Replay Voice Guidance",
                        tint = if (isMuted) Slate400 else Emerald400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Metric & Navigation Step Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Route Info Tags (Remaining meters, Est Time, Steps)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${route.totalDistanceMeters.roundToInt()}m  •  ~${(route.estimatedTimeSeconds / 60).coerceAtLeast(1)} min",
                            color = Cyan300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Step ${activeStepIndex + 1}/$totalSteps",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                // Step Forward / Backward Controls & Exit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevStep,
                        enabled = activeStepIndex > 0,
                        modifier = Modifier.size(32.dp).testTag("prev_step_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Step",
                            tint = if (activeStepIndex > 0) Color.White else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextStep,
                        enabled = activeStepIndex < totalSteps - 1,
                        modifier = Modifier.size(32.dp).testTag("next_step_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Step",
                            tint = if (activeStepIndex < totalSteps - 1) Color.White else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onCancelRoute,
                        modifier = Modifier.size(32.dp).testTag("cancel_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Navigation",
                            tint = Red500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Red950() = Color(0xFF450A0A)
