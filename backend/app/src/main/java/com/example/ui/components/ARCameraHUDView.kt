package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ARCameraHUDView(
    building: Building,
    activeRoute: NavigationRoute?,
    activeStepIndex: Int,
    onExitAR: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_anim")
    val chevronPulse by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_pulse"
    )

    val gridSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_sweep"
    )

    val currentStep = activeRoute?.steps?.getOrNull(activeStepIndex)
    val nextStep = activeRoute?.steps?.getOrNull(activeStepIndex + 1)
    val isEmergency = activeRoute?.isEmergencyRoute == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("ar_camera_hud_view")
    ) {
        // 1. Spatial AR Grid & Virtual Viewport Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cyber Horizon Grid
            val horizonY = h * 0.55f
            val vpX = w * 0.5f

            // Perspective Ground Grid
            val primaryColor = if (isEmergency) Red500 else Cyan400
            for (i in -4..4) {
                val bottomX = vpX + (i * w * 0.22f)
                drawLine(
                    color = primaryColor.copy(alpha = 0.2f),
                    start = Offset(vpX, horizonY),
                    end = Offset(bottomX, h),
                    strokeWidth = 1.5f
                )
            }

            for (row in 1..6) {
                val rowY = horizonY + (h - horizonY) * (row * 0.16f)
                drawLine(
                    color = primaryColor.copy(alpha = 0.18f),
                    start = Offset(0f, rowY),
                    end = Offset(w, rowY),
                    strokeWidth = 1.2f
                )
            }

            // Radar Scan Sweep Line
            val sweepY = h * gridSweep
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, sweepY),
                end = Offset(w, sweepY),
                strokeWidth = 3f
            )

            // Center HUD Crosshair & Reticle
            val cx = w / 2f
            val cy = h * 0.45f

            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 60f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = 6f,
                center = Offset(cx, cy)
            )

            // Crosshair brackets
            val bracketLen = 24f
            val bracketDist = 75f
            // Top-left
            drawLine(primaryColor, Offset(cx - bracketDist, cy - bracketDist + bracketLen), Offset(cx - bracketDist, cy - bracketDist), 3f)
            drawLine(primaryColor, Offset(cx - bracketDist, cy - bracketDist), Offset(cx - bracketDist + bracketLen, cy - bracketDist), 3f)
            // Top-right
            drawLine(primaryColor, Offset(cx + bracketDist, cy - bracketDist + bracketLen), Offset(cx + bracketDist, cy - bracketDist), 3f)
            drawLine(primaryColor, Offset(cx + bracketDist, cy - bracketDist), Offset(cx + bracketDist - bracketLen, cy - bracketDist), 3f)
            // Bottom-left
            drawLine(primaryColor, Offset(cx - bracketDist, cy + bracketDist - bracketLen), Offset(cx - bracketDist, cy + bracketDist), 3f)
            drawLine(primaryColor, Offset(cx - bracketDist, cy + bracketDist), Offset(cx - bracketDist + bracketLen, cy + bracketDist), 3f)
            // Bottom-right
            drawLine(primaryColor, Offset(cx + bracketDist, cy + bracketDist - bracketLen), Offset(cx + bracketDist, cy + bracketDist), 3f)
            drawLine(primaryColor, Offset(cx + bracketDist, cy + bracketDist), Offset(cx + bracketDist - bracketLen, cy + bracketDist), 3f)

            // 3D Floating Spatial Directional Arrow / Chevron
            val arrowBaseY = cy + 120f + chevronPulse
            val arrowColor = if (isEmergency) Red500 else Emerald400

            // Floating Arrow Polygon
            val path = Path().apply {
                moveTo(cx, arrowBaseY - 35f)
                lineTo(cx + 40f, arrowBaseY + 25f)
                lineTo(cx + 15f, arrowBaseY + 15f)
                lineTo(cx + 15f, arrowBaseY + 45f)
                lineTo(cx - 15f, arrowBaseY + 45f)
                lineTo(cx - 15f, arrowBaseY + 15f)
                lineTo(cx - 40f, arrowBaseY + 25f)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(arrowColor, arrowColor.copy(alpha = 0.6f))
                )
            )
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2.5f)
            )
        }

        // 2. Top Header HUD Bar with Step Instruction
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                color = Slate900.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergency) Red500 else Cyan500.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isEmergency) Red500 else Cyan500, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentStep?.turnType) {
                                TurnType.LEFT, TurnType.SHARP_LEFT, TurnType.SLIGHT_LEFT -> Icons.Default.ArrowBack
                                TurnType.RIGHT, TurnType.SHARP_RIGHT, TurnType.SLIGHT_RIGHT -> Icons.Default.ArrowForward
                                TurnType.ELEVATOR_UP, TurnType.ELEVATOR_DOWN -> Icons.Default.Elevator
                                TurnType.STAIRS_UP, TurnType.STAIRS_DOWN -> Icons.Default.Stairs
                                TurnType.EMERGENCY_EVACUATE -> Icons.Default.Warning
                                TurnType.ARRIVAL -> Icons.Default.CheckCircle
                                else -> Icons.Default.Straight
                            },
                            contentDescription = "Direction",
                            tint = Slate950,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStep?.instruction ?: "Spatial AR Guidance Calibrated",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = currentStep?.detail ?: "Point phone toward hallway corridor",
                            color = Slate300,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onExitAR,
                        modifier = Modifier
                            .background(Slate800, CircleShape)
                            .size(40.dp)
                            .testTag("exit_ar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Switch to 2D Map",
                            tint = Cyan400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AR Telemetry Strip (Distance, Floor, Bearing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Slate900.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Cyan400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HEADING: 042° NE", color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Slate900.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LEVEL ${currentStep?.floor ?: 0}", color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Slate900.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergency) Red500 else Emerald400)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${currentStep?.distanceMeters?.roundToInt() ?: 0}m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Floating Spatial Anchor Card in bottom center
        Surface(
            color = Slate900.copy(alpha = 0.95f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Cyan400)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPATIAL AR WAYPOINT HUD", color = Cyan300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "ZERO-HARDWARE AI",
                        color = Emerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Next: ${nextStep?.instruction ?: "Destination Point"}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (currentStep?.targetNode?.visualSignageHint?.isNotEmpty() == true) {
                    Text(
                        text = "Signage Plaque: \"${currentStep.targetNode.visualSignageHint}\"",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
