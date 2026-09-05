package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapBuilderDialog(
    building: Building,
    activeFloor: Int,
    onAddNode: (name: String, code: String, category: NodeCategory, description: String, x: Float, y: Float) -> Unit,
    onAddEdge: (fromId: String, toId: String, dist: Float, edgeType: EdgeType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Add Room/Node, 1 = Connect Corridor/Edge

    // Node Form State
    var nodeName by remember { mutableStateOf("") }
    var nodeCode by remember { mutableStateOf("") }
    var nodeDesc by remember { mutableStateOf("") }
    var nodeX by remember { mutableFloatStateOf(500f) }
    var nodeY by remember { mutableFloatStateOf(500f) }
    var selectedCategory by remember { mutableStateOf(NodeCategory.GENERAL) }

    // Edge Form State
    val floorNodes = remember(building, activeFloor) {
        building.nodes.filter { it.floor == activeFloor }
    }
    var edgeFromId by remember { mutableStateOf(floorNodes.firstOrNull()?.id ?: "") }
    var edgeToId by remember { mutableStateOf(floorNodes.getOrNull(1)?.id ?: "") }
    var edgeDistance by remember { mutableStateOf("15") }
    var edgeType by remember { mutableStateOf(EdgeType.CORRIDOR) }

    var expandedCategory by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("map_builder_dialog")
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
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Slate950, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Graph Builder",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Level $activeFloor  •  ${building.name}",
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

                // Tabs: Add Room vs Connect Hallway
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate800,
                    contentColor = Cyan400,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Add Room / Node", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Connect Corridor", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // ADD NODE FORM
                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = { nodeName = it },
                        label = { Text("Room / Landmark Name") },
                        placeholder = { Text("e.g. Ophthalmology Clinic") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("builder_node_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nodeCode,
                        onValueChange = { nodeCode = it },
                        label = { Text("Room Code / Sign ID") },
                        placeholder = { Text("e.g. EYE-208") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("builder_node_code")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nodeDesc,
                        onValueChange = { nodeDesc = it },
                        label = { Text("Description & Symptoms") },
                        placeholder = { Text("Eye exams, vision testing, lenses") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("builder_node_desc")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Coordinate Sliders
                    Text("Canvas Position X: ${nodeX.toInt()} / 1000", color = Slate300, fontSize = 11.sp)
                    Slider(
                        value = nodeX,
                        onValueChange = { nodeX = it },
                        valueRange = 50f..950f,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan500)
                    )

                    Text("Canvas Position Y: ${nodeY.toInt()} / 1000", color = Slate300, fontSize = 11.sp)
                    Slider(
                        value = nodeY,
                        onValueChange = { nodeY = it },
                        valueRange = 50f..950f,
                        colors = SliderDefaults.colors(thumbColor = Cyan400, activeTrackColor = Cyan500)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nodeName.isNotBlank() && nodeCode.isNotBlank()) {
                                onAddNode(nodeName, nodeCode, selectedCategory, nodeDesc, nodeX, nodeY)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500, contentColor = Slate950),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("builder_save_node_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Node to Spatial Graph", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // CONNECT CORRIDOR FORM
                    Text("Select Start Node:", color = Slate300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            floorNodes.take(6).forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { edgeFromId = node.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = edgeFromId == node.id,
                                        onClick = { edgeFromId = node.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = Cyan400)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${node.name} (${node.code})", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Target Node:", color = Slate300, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            floorNodes.take(6).forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { edgeToId = node.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = edgeToId == node.id,
                                        onClick = { edgeToId = node.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = Emerald400)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${node.name} (${node.code})", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = edgeDistance,
                        onValueChange = { edgeDistance = it },
                        label = { Text("Distance (Meters)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("builder_edge_dist")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (edgeFromId.isNotEmpty() && edgeToId.isNotEmpty() && edgeFromId != edgeToId) {
                                val dist = edgeDistance.toFloatOrNull() ?: 15f
                                onAddEdge(edgeFromId, edgeToId, dist, edgeType)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Slate950),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("builder_save_edge_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Bidirectional Corridor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
