package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IndoorMapCanvas(
    building: Building,
    activeFloor: Int,
    startNode: NavNode?,
    destinationNode: NavNode?,
    activeRoute: NavigationRoute?,
    activeStepIndex: Int,
    onNodeClicked: (NavNode) -> Unit,
    indoorPosition: IndoorPosition? = null,
    deviceHeadingDegrees: Float = 0f,
    beacons: List<Beacon> = emptyList(),
    showBeacons: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.6f, 3.5f)
        offset += offsetChange
    }

    // Animation for pulsing navigation path and beacons
    val infiniteTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    val floorNodes = remember(building, activeFloor) {
        building.nodes.filter { it.floor == activeFloor }
    }

    val floorEdges = remember(building, activeFloor) {
        val floorNodeIds = floorNodes.map { it.id }.toSet()
        building.edges.filter { it.fromId in floorNodeIds && it.toId in floorNodeIds }
    }

    val floorBeacons = remember(beacons, activeFloor) {
        beacons.filter { it.floor == activeFloor }
    }

    // Filter active route path for current floor
    val activeFloorPathNodes = remember(activeRoute, activeFloor) {
        activeRoute?.pathNodes?.filter { it.floor == activeFloor } ?: emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("indoor_map_canvas")
            .transformable(state = transformState)
            .pointerInput(floorNodes, scale, offset) {
                detectTapGestures { tapOffset ->
                    // Convert screen tap offset to normalized 0..1000 coordinate space
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    if (canvasWidth > 0 && canvasHeight > 0) {
                        val localX = (tapOffset.x - offset.x - canvasWidth / 2f) / scale + canvasWidth / 2f
                        val localY = (tapOffset.y - offset.y - canvasHeight / 2f) / scale + canvasHeight / 2f

                        val normX = (localX / canvasWidth) * 1000f
                        val normY = (localY / canvasHeight) * 1000f

                        // Find closest node within 48dp touch radius
                        var closest: NavNode? = null
                        var minDistance = 70f // Threshold in coordinate space
                        for (node in floorNodes) {
                            val dist = kotlin.math.hypot(node.x - normX, node.y - normY)
                            if (dist < minDistance) {
                                minDistance = dist
                                closest = node
                            }
                        }
                        if (closest != null) {
                            onNodeClicked(closest)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Coordinate mapping lambda
            fun toScreenOffset(normX: Float, normY: Float): Offset {
                val baseScreenX = (normX / 1000f) * canvasW
                val baseScreenY = (normY / 1000f) * canvasH
                val centeredX = (baseScreenX - canvasW / 2f) * scale + canvasW / 2f + offset.x
                val centeredY = (baseScreenY - canvasH / 2f) * scale + canvasH / 2f + offset.y
                return Offset(centeredX, centeredY)
            }

            // 1. Draw Architectural Floor Background Grid & Blueprint boundary
            drawFloorBlueprint(canvasW, canvasH, scale, offset, activeFloor)

            // 2. Draw Floor Edges / Hallway Corridors
            for (edge in floorEdges) {
                val fromNode = floorNodes.firstOrNull { it.id == edge.fromId }
                val toNode = floorNodes.firstOrNull { it.id == edge.toId }
                if (fromNode != null && toNode != null) {
                    val p1 = toScreenOffset(fromNode.x, fromNode.y)
                    val p2 = toScreenOffset(toNode.x, toNode.y)

                    // Corridor base track
                    drawLine(
                        color = if (edge.edgeType == EdgeType.EMERGENCY_EXIT) Red500.copy(alpha = 0.4f) else Slate800,
                        start = p1,
                        end = p2,
                        strokeWidth = 14f * scale,
                        cap = StrokeCap.Round
                    )
                    // Corridor inner lane
                    drawLine(
                        color = if (edge.edgeType == EdgeType.EMERGENCY_EXIT) Red500.copy(alpha = 0.7f) else Slate700.copy(alpha = 0.5f),
                        start = p1,
                        end = p2,
                        strokeWidth = 4f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Draw Active Navigation Route Polyline (Glow + Animated Chevrons)
            if (activeRoute != null && activeRoute.pathNodes.size > 1) {
                drawActiveRoutePath(
                    route = activeRoute,
                    activeFloor = activeFloor,
                    pulsePhase = pulsePhase,
                    scale = scale,
                    toScreen = ::toScreenOffset,
                    isEmergency = activeRoute.isEmergencyRoute
                )
            }

            // 4. Draw BLE Beacons on Current Floor (Phase 7 BLE Hardware Anchors)
            if (showBeacons) {
                for (b in floorBeacons) {
                    val bp = toScreenOffset(b.x, b.y)
                    // BLE signal ripple waves
                    drawCircle(
                        color = Indigo400.copy(alpha = 0.2f),
                        radius = (16f + (beaconPulse * 0.5f)) * scale,
                        center = bp,
                        style = Stroke(width = 1.5f * scale)
                    )
                    // Inner Beacon Dot
                    drawCircle(
                        color = Indigo500,
                        radius = 6f * scale,
                        center = bp
                    )
                    drawCircle(
                        color = Cyan300,
                        radius = 2.5f * scale,
                        center = bp
                    )

                    // Beacon ID Text
                    drawContext.canvas.nativeCanvas.apply {
                        val bPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#818CF8")
                            textSize = (8.5f * scale).coerceIn(14f, 22f)
                            textAlign = android.graphics.Paint.Align.CENTER
                            setShadowLayer(2f, 0f, 1f, android.graphics.Color.BLACK)
                        }
                        drawText(b.beaconId.takeLast(5), bp.x, bp.y - (10f * scale), bPaint)
                    }
                }
            }

            // 5. Draw Room Envelopes & Nodes
            for (node in floorNodes) {
                val p = toScreenOffset(node.x, node.y)
                val isStart = node.id == startNode?.id
                val isDest = node.id == destinationNode?.id
                val isEmergency = node.isEmergencyExit || node.category == NodeCategory.EMERGENCY_EXIT
                val isTransfer = node.category == NodeCategory.ELEVATOR || node.category == NodeCategory.STAIRS

                // Room background pill
                val roomRadius = if (isStart || isDest) 22f * scale else 16f * scale
                val nodeColor = when {
                    isDest && isEmergency -> Red500
                    isDest -> Emerald400
                    isStart -> Cyan400
                    isEmergency -> Red500.copy(alpha = 0.9f)
                    isTransfer -> Amber500.copy(alpha = 0.85f)
                    node.category == NodeCategory.EMERGENCY -> Red500
                    node.category == NodeCategory.PHARMACY -> Emerald500
                    node.category == NodeCategory.RESTROOM -> Purple500
                    else -> Slate700
                }

                // Outer border aura
                drawCircle(
                    color = nodeColor.copy(alpha = 0.25f),
                    radius = roomRadius + 6f * scale,
                    center = p
                )

                // Main Node circle
                drawCircle(
                    color = Slate900,
                    radius = roomRadius,
                    center = p
                )
                drawCircle(
                    color = nodeColor,
                    radius = roomRadius,
                    center = p,
                    style = Stroke(width = 3.5f * scale)
                )

                // Category indicator inner dot
                drawCircle(
                    color = nodeColor,
                    radius = 5.5f * scale,
                    center = p
                )

                // Draw Text Label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isDest) android.graphics.Color.parseColor("#34D399")
                        else if (isStart) android.graphics.Color.parseColor("#22D3EE")
                        else if (isEmergency) android.graphics.Color.parseColor("#EF4444")
                        else android.graphics.Color.parseColor("#CBD5E1")
                        textSize = (11f * scale).coerceIn(18f, 32f)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = isDest || isStart || isEmergency
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                    }
                    drawText(node.code, p.x, p.y + roomRadius + (13f * scale), paint)
                }
            }

            // 6. Draw Live Real-time User Position & Heading Flashlight Cone (Phases 6 & 7)
            val liveX = indoorPosition?.x ?: startNode?.takeIf { it.floor == activeFloor }?.x
            val liveY = indoorPosition?.y ?: startNode?.takeIf { it.floor == activeFloor }?.y
            val liveFloor = indoorPosition?.floor ?: startNode?.floor

            if (liveX != null && liveY != null && liveFloor == activeFloor) {
                val userPos = toScreenOffset(liveX, liveY)

                // A. Heading Flashlight Cone / Field of view
                val headingRad = Math.toRadians((deviceHeadingDegrees - 90.0)).toFloat()
                val coneAngleRad = Math.toRadians(35.0).toFloat()
                val coneLength = 55f * scale

                val conePath = Path().apply {
                    moveTo(userPos.x, userPos.y)
                    lineTo(
                        userPos.x + (cos(headingRad - coneAngleRad) * coneLength),
                        userPos.y + (sin(headingRad - coneAngleRad) * coneLength)
                    )
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = userPos.x - coneLength,
                            top = userPos.y - coneLength,
                            right = userPos.x + coneLength,
                            bottom = userPos.y + coneLength
                        ),
                        startAngleDegrees = Math.toDegrees((headingRad - coneAngleRad).toDouble()).toFloat(),
                        sweepAngleDegrees = 70f,
                        forceMoveTo = false
                    )
                    close()
                }

                drawPath(
                    path = conePath,
                    brush = Brush.radialGradient(
                        colors = listOf(Cyan400.copy(alpha = 0.45f), Cyan400.copy(alpha = 0.0f)),
                        center = userPos,
                        radius = coneLength
                    )
                )

                // B. Accuracy / Uncertainty Circle
                val accRadius = ((indoorPosition?.accuracyMeters ?: 4.0f) * 6.5f * scale).coerceIn(16f * scale, 60f * scale)
                drawCircle(
                    color = Cyan500.copy(alpha = 0.18f),
                    radius = accRadius,
                    center = userPos
                )
                drawCircle(
                    color = Cyan400.copy(alpha = 0.45f),
                    radius = accRadius,
                    center = userPos,
                    style = Stroke(width = 1.2f * scale)
                )

                // C. Outer Pulse Ring
                drawCircle(
                    color = Cyan400.copy(alpha = (1f - (beaconPulse - 12f) / 20f).coerceIn(0f, 0.7f)),
                    radius = beaconPulse * scale,
                    center = userPos,
                    style = Stroke(width = 2.5f * scale)
                )

                // D. Main User Core Dot
                drawCircle(
                    color = Color.White,
                    radius = 12f * scale,
                    center = userPos
                )
                drawCircle(
                    color = Cyan500,
                    radius = 9f * scale,
                    center = userPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * scale,
                    center = userPos
                )

                // Direction pointer tip
                val tipX = userPos.x + (cos(headingRad) * 14f * scale)
                val tipY = userPos.y + (sin(headingRad) * 14f * scale)
                drawCircle(
                    color = Cyan300,
                    radius = 3.5f * scale,
                    center = Offset(tipX, tipY)
                )
            }

            // 7. Draw Destination Beacon (Pulsing Emerald / Red Emergency Target)
            if (destinationNode != null && destinationNode.floor == activeFloor) {
                val destPos = toScreenOffset(destinationNode.x, destinationNode.y)
                val destColor = if (activeRoute?.isEmergencyRoute == true) Red500 else Emerald400

                // Destination Radar rings
                drawCircle(
                    color = destColor.copy(alpha = (1f - (beaconPulse - 12f) / 20f).coerceIn(0f, 0.8f)),
                    radius = (beaconPulse + 8f) * scale,
                    center = destPos,
                    style = Stroke(width = 3f * scale)
                )

                // Flag / Pin Center
                drawCircle(
                    color = destColor,
                    radius = 12f * scale,
                    center = destPos
                )
                drawCircle(
                    color = Slate950,
                    radius = 4f * scale,
                    center = destPos
                )
            }
        }
    }
}

private fun DrawScope.drawFloorBlueprint(
    canvasW: Float,
    canvasH: Float,
    scale: Float,
    offset: Offset,
    floor: Int
) {
    // Subtle architectural grid
    val step = 50f * scale
    val startX = (offset.x % step)
    val startY = (offset.y % step)

    var x = startX
    while (x < canvasW) {
        drawLine(
            color = Slate800.copy(alpha = 0.35f),
            start = Offset(x, 0f),
            end = Offset(x, canvasH),
            strokeWidth = 0.8f
        )
        x += step
    }

    var y = startY
    while (y < canvasH) {
        drawLine(
            color = Slate800.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(canvasW, y),
            strokeWidth = 0.8f
        )
        y += step
    }
}

private fun DrawScope.drawActiveRoutePath(
    route: NavigationRoute,
    activeFloor: Int,
    pulsePhase: Float,
    scale: Float,
    toScreen: (Float, Float) -> Offset,
    isEmergency: Boolean
) {
    val nodes = route.pathNodes
    val pathSegments = mutableListOf<Pair<Offset, Offset>>()

    for (i in 0 until nodes.size - 1) {
        val n1 = nodes[i]
        val n2 = nodes[i + 1]
        // If both nodes are on this floor, draw direct line
        if (n1.floor == activeFloor && n2.floor == activeFloor) {
            val p1 = toScreen(n1.x, n1.y)
            val p2 = toScreen(n2.x, n2.y)
            pathSegments.add(Pair(p1, p2))
        }
    }

    val primaryColor = if (isEmergency) Red500 else Cyan400
    val secondaryColor = if (isEmergency) Amber500 else Emerald400

    for ((p1, p2) in pathSegments) {
        // Outer Blur Glow
        drawLine(
            brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.35f), secondaryColor.copy(alpha = 0.35f))),
            start = p1,
            end = p2,
            strokeWidth = 16f * scale,
            cap = StrokeCap.Round
        )

        // Core Neon Path
        drawLine(
            brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
            start = p1,
            end = p2,
            strokeWidth = 5.5f * scale,
            cap = StrokeCap.Round
        )

        // Marching Pulse Chevron Dots
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dist = kotlin.math.hypot(dx, dy)
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        val numChevrons = (dist / (45f * scale)).toInt().coerceAtLeast(1)
        for (k in 0..numChevrons) {
            val fraction = ((k.toFloat() / numChevrons) + pulsePhase) % 1.0f
            val cx = p1.x + dx * fraction
            val cy = p1.y + dy * fraction

            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = Color.White,
                    radius = 3.5f * scale,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
