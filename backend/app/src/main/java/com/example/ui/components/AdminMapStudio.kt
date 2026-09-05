package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class AdminMapStudioMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    INSPECT("Inspect", Icons.Default.TouchApp),
    ADD_LOCATION("Add Room", Icons.Default.AddLocationAlt),
    ADD_NODE("Add Nav Node", Icons.Default.AddCircleOutline),
    CONNECT_EDGE("Connect Edge", Icons.Default.Timeline),
    ADD_BEACON("Add Beacon", Icons.Default.Sensors),
    ADD_QR("Add QR Point", Icons.Default.QrCodeScanner)
}

@Composable
fun AdminMapStudio(
    building: Building,
    activeFloor: Int,
    onSelectFloor: (Int) -> Unit,
    beacons: List<Beacon>,
    qrPoints: List<QrCalibrationPoint>,
    onAddLocationAtCoordinate: (Float, Float) -> Unit,
    onAddNodeAtCoordinate: (Float, Float) -> Unit,
    onAddBeaconAtCoordinate: (Float, Float) -> Unit,
    onAddQrPointAtCoordinate: (Float, Float) -> Unit,
    onConnectNodes: (fromId: String, toId: String, distanceMeters: Float, edgeType: EdgeType) -> Unit,
    onEditNode: (NavNode) -> Unit,
    onDeleteNode: (String) -> Unit,
    onDeleteEdge: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var studioMode by remember { mutableStateOf(AdminMapStudioMode.INSPECT) }
    var selectedNode by remember { mutableStateOf<NavNode?>(null) }
    var edgeSourceNode by remember { mutableStateOf<NavNode?>(null) }

    // Dialog state for confirming interactive edge creation
    var pendingEdgeDialog by remember { mutableStateOf<Pair<NavNode, NavNode>?>(null) }
    var pendingEdgeType by remember { mutableStateOf(EdgeType.CORRIDOR) }
    var pendingEdgeAccessible by remember { mutableStateOf(true) }

    // Pan & Zoom
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3.5f)
        offset += offsetChange
    }

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

    val floorQrPoints = remember(qrPoints, activeFloor) {
        qrPoints.filter { it.floorId == activeFloor }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Floor Selector & Mode Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .border(1.dp, Slate800, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Floors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FLOOR LEVEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${floorNodes.size} Nodes • ${floorEdges.size} Edges • ${floorBeacons.size} Beacons",
                        fontSize = 11.sp,
                        color = Cyan400
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(building.floors.sortedBy { it.floorNumber }) { floor ->
                        val isSelected = floor.floorNumber == activeFloor
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectFloor(floor.floorNumber)
                                selectedNode = null
                                edgeSourceNode = null
                            },
                            label = { Text(floor.name.ifBlank { "Floor ${floor.floorNumber}" }) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Slate950 else Cyan400
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan400,
                                selectedLabelColor = Slate950,
                                containerColor = Slate800,
                                labelColor = Slate200
                            )
                        )
                    }
                }

                Divider(color = Slate800, thickness = 1.dp)

                // Tool Modes
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AdminMapStudioMode.values()) { mode ->
                        val isModeSelected = studioMode == mode
                        Button(
                            onClick = {
                                studioMode = mode
                                if (mode != AdminMapStudioMode.CONNECT_EDGE) {
                                    edgeSourceNode = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isModeSelected) Cyan400 else Slate800,
                                contentColor = if (isModeSelected) Slate950 else Slate200
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = mode.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action Instruction Banner
        Surface(
            color = when (studioMode) {
                AdminMapStudioMode.INSPECT -> Slate900
                AdminMapStudioMode.ADD_LOCATION, AdminMapStudioMode.ADD_NODE -> Indigo500.copy(alpha = 0.25f)
                AdminMapStudioMode.CONNECT_EDGE -> Amber500.copy(alpha = 0.2f)
                AdminMapStudioMode.ADD_BEACON -> Emerald500.copy(alpha = 0.2f)
                AdminMapStudioMode.ADD_QR -> Purple500.copy(alpha = 0.2f)
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .border(1.dp, Slate800, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = studioMode.icon,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when (studioMode) {
                        AdminMapStudioMode.INSPECT -> "Tap any node to view connections, coordinates, and options."
                        AdminMapStudioMode.ADD_LOCATION -> "Tap anywhere on the floor map to place a new Room / Destination."
                        AdminMapStudioMode.ADD_NODE -> "Tap on map corridor to place a Navigation Waypoint."
                        AdminMapStudioMode.CONNECT_EDGE -> if (edgeSourceNode == null) "Select Source Node (Start of edge)" else "Select Target Node to connect from '${edgeSourceNode?.name}'"
                        AdminMapStudioMode.ADD_BEACON -> "Tap anywhere on the floor map to deploy a BLE Anchor Beacon."
                        AdminMapStudioMode.ADD_QR -> "Tap on floor map to deploy a QR Calibration Landmark."
                    },
                    fontSize = 11.sp,
                    color = Slate200,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Interactive Studio Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF070B14))
                .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                .transformable(state = transformState)
                .pointerInput(floorNodes, studioMode, edgeSourceNode, scale, offset) {
                    detectTapGestures { tapOffset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        if (canvasWidth > 0 && canvasHeight > 0) {
                            val localX = (tapOffset.x - offset.x - canvasWidth / 2f) / scale + canvasWidth / 2f
                            val localY = (tapOffset.y - offset.y - canvasHeight / 2f) / scale + canvasHeight / 2f

                            val normX = ((localX / canvasWidth) * 1000f).coerceIn(0f, 1000f)
                            val normY = ((localY / canvasHeight) * 1000f).coerceIn(0f, 1000f)

                            // Check if tapped on existing node
                            var tappedNode: NavNode? = null
                            var minDistance = 75f
                            for (node in floorNodes) {
                                val dist = hypot(node.x - normX, node.y - normY)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    tappedNode = node
                                }
                            }

                            when (studioMode) {
                                AdminMapStudioMode.INSPECT -> {
                                    selectedNode = tappedNode
                                }
                                AdminMapStudioMode.ADD_LOCATION -> {
                                    onAddLocationAtCoordinate(normX, normY)
                                }
                                AdminMapStudioMode.ADD_NODE -> {
                                    onAddNodeAtCoordinate(normX, normY)
                                }
                                AdminMapStudioMode.CONNECT_EDGE -> {
                                    if (tappedNode != null) {
                                        if (edgeSourceNode == null) {
                                            edgeSourceNode = tappedNode
                                        } else if (edgeSourceNode?.id != tappedNode.id) {
                                            // Open Edge Confirmation Dialog
                                            pendingEdgeDialog = Pair(edgeSourceNode!!, tappedNode)
                                            edgeSourceNode = null
                                        }
                                    }
                                }
                                AdminMapStudioMode.ADD_BEACON -> {
                                    onAddBeaconAtCoordinate(normX, normY)
                                }
                                AdminMapStudioMode.ADD_QR -> {
                                    onAddQrPointAtCoordinate(normX, normY)
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw Studio Grid
                val gridSpacing = 50f * scale
                val startX = (offset.x % gridSpacing)
                val startY = (offset.y % gridSpacing)

                var curX = startX
                while (curX < canvasWidth) {
                    drawLine(
                        color = Slate800.copy(alpha = 0.25f),
                        start = Offset(curX, 0f),
                        end = Offset(curX, canvasHeight),
                        strokeWidth = 1f
                    )
                    curX += gridSpacing
                }

                var curY = startY
                while (curY < canvasHeight) {
                    drawLine(
                        color = Slate800.copy(alpha = 0.25f),
                        start = Offset(0f, curY),
                        end = Offset(canvasWidth, curY),
                        strokeWidth = 1f
                    )
                    curY += gridSpacing
                }

                fun toCanvasOffset(nx: Float, ny: Float): Offset {
                    val px = (nx / 1000f) * canvasWidth
                    val py = (ny / 1000f) * canvasHeight
                    val screenX = (px - canvasWidth / 2f) * scale + canvasWidth / 2f + offset.x
                    val screenY = (py - canvasHeight / 2f) * scale + canvasHeight / 2f + offset.y
                    return Offset(screenX, screenY)
                }

                val nodeCoordMap = floorNodes.associateBy({ it.id }, { toCanvasOffset(it.x, it.y) })

                // 1. Draw Edges
                for (edge in floorEdges) {
                    val p1 = nodeCoordMap[edge.fromId]
                    val p2 = nodeCoordMap[edge.toId]
                    if (p1 != null && p2 != null) {
                        val edgeColor = when (edge.edgeType) {
                            EdgeType.CORRIDOR -> Color(0xFF00E5FF)
                            EdgeType.ELEVATOR -> Amber400
                            EdgeType.STAIRS -> Color(0xFFFF7043)
                            EdgeType.RAMP -> Emerald400
                            EdgeType.ESCALATOR -> Color(0xFFAB47BC)
                            EdgeType.EMERGENCY_EXIT -> Red400
                        }

                        drawLine(
                            color = edgeColor.copy(alpha = 0.7f),
                            start = p1,
                            end = p2,
                            strokeWidth = 3f * scale,
                            pathEffect = if (!edge.bidirectional) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null
                        )
                    }
                }

                // 2. Draw Beacons
                for (beacon in floorBeacons) {
                    val p = toCanvasOffset(beacon.x, beacon.y)
                    drawCircle(
                        color = Emerald400.copy(alpha = 0.25f),
                        radius = 20f * scale,
                        center = p
                    )
                    drawCircle(
                        color = Emerald400,
                        radius = 6f * scale,
                        center = p
                    )
                }

                // 3. Draw QR Points
                for (qr in floorQrPoints) {
                    val p = toCanvasOffset(qr.x, qr.y)
                    drawCircle(
                        color = Color(0xFF9C27B0).copy(alpha = 0.25f),
                        radius = 18f * scale,
                        center = p
                    )
                    drawCircle(
                        color = Color(0xFFBA68C8),
                        radius = 5f * scale,
                        center = p
                    )
                }

                // 4. Draw Nodes
                for (node in floorNodes) {
                    val p = nodeCoordMap[node.id] ?: continue
                    val isSelected = selectedNode?.id == node.id
                    val isEdgeSource = edgeSourceNode?.id == node.id

                    val nodeColor = when (node.category) {
                        NodeCategory.ENTRANCE -> Emerald400
                        NodeCategory.ELEVATOR -> Amber400
                        NodeCategory.STAIRS -> Color(0xFFFF7043)
                        NodeCategory.EMERGENCY_EXIT -> Red400
                        NodeCategory.LABORATORY, NodeCategory.CLASSROOM -> Cyan400
                        NodeCategory.RECEPTION, NodeCategory.FACULTY -> Color(0xFF7C4DFF)
                        NodeCategory.RESTROOM -> Color(0xFF42A5F5)
                        else -> Slate200
                    }

                    if (isSelected || isEdgeSource) {
                        drawCircle(
                            color = if (isEdgeSource) Amber400 else Cyan400,
                            radius = 20f * scale,
                            center = p,
                            style = Stroke(width = 3f * scale)
                        )
                    }

                    drawCircle(
                        color = nodeColor,
                        radius = 10f * scale,
                        center = p
                    )

                    drawCircle(
                        color = Slate950,
                        radius = 5f * scale,
                        center = p
                    )
                }
            }

            // Legend Overlay (Top Right)
            Surface(
                color = Slate900.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Cyan400, CircleShape))
                        Text("Corridor/Room", fontSize = 9.sp, color = Slate300)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Amber400, CircleShape))
                        Text("Elevator", fontSize = 9.sp, color = Slate300)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Color(0xFFFF7043), CircleShape))
                        Text("Stairs", fontSize = 9.sp, color = Slate300)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Emerald400, CircleShape))
                        Text("Beacon / Entrance", fontSize = 9.sp, color = Slate300)
                    }
                }
            }
        }

        // Bottom Selected Node Inspector Card
        if (selectedNode != null) {
            val node = selectedNode!!
            val connections = remember(node, building.edges) {
                building.edges.filter { it.fromId == node.id || it.toId == node.id }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, Cyan400.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(node.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Surface(color = Slate800, shape = RoundedCornerShape(4.dp)) {
                                    Text(node.code, fontSize = 10.sp, color = Cyan400, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("ID: ${node.id} • Floor ${node.floor} • (${node.x.roundToInt()}, ${node.y.roundToInt()})", fontSize = 11.sp, color = Slate400)
                        }

                        IconButton(onClick = { selectedNode = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${connections.size} Connected Route(s)",
                            fontSize = 12.sp,
                            color = Slate300
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    studioMode = AdminMapStudioMode.CONNECT_EDGE
                                    edgeSourceNode = node
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Connect", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onDeleteNode(node.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Red500),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Connect Edge Dialog
    if (pendingEdgeDialog != null) {
        val (nodeA, nodeB) = pendingEdgeDialog!!
        val calculatedDistance = (hypot(nodeA.x - nodeB.x, nodeA.y - nodeB.y) / 10f).coerceAtLeast(1f)

        AlertDialog(
            onDismissRequest = { pendingEdgeDialog = null },
            title = { Text("Connect Route Edge", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connecting: ${nodeA.name} ↔ ${nodeB.name}", fontSize = 13.sp, color = Cyan400)
                    Text("Calculated Distance: ${"%.1f".format(calculatedDistance)} meters", fontSize = 12.sp, color = Slate300)

                    Text("Edge Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EdgeType.values()) { type ->
                            FilterChip(
                                selected = pendingEdgeType == type,
                                onClick = {
                                    pendingEdgeType = type
                                    pendingEdgeAccessible = (type != EdgeType.STAIRS && type != EdgeType.ESCALATOR)
                                },
                                label = { Text(type.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pendingEdgeAccessible,
                            onCheckedChange = { pendingEdgeAccessible = it }
                        )
                        Text("Wheelchair Accessible Route", fontSize = 12.sp, color = Slate200)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConnectNodes(nodeA.id, nodeB.id, calculatedDistance, pendingEdgeType)
                        pendingEdgeDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Slate950)
                ) {
                    Text("Create Edge")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEdgeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
