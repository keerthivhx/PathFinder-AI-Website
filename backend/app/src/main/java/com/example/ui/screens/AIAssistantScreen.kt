package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AIAssistantScreen(
    building: Building,
    chatMessages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onNavigateToNode: (NavNode) -> Unit,
    onSpeakText: (String) -> Unit,
    navigationContext: AINavigationContext? = null,
    onScanQr: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isNavigating = navigationContext?.isNavigating == true

    // Context-sensitive quick suggestions grounded in current navigation state
    val quickQuestions = remember(isNavigating) {
        if (isNavigating) {
            listOf(
                "What should I do next?",
                "How far is my destination?",
                "Is this route wheelchair accessible?",
                "Where is the nearest emergency exit?",
                "Where am I?"
            )
        } else {
            listOf(
                "Where am I?",
                "Take me to the AI Lab",
                "Where is the nearest restroom?",
                "Which floor is the library on?",
                "Where is the nearest emergency exit?",
                "How do I get to the cafeteria?"
            )
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
    ) {
        // -------------------------------------------------------------
        // GROUNDED NAVIGATION CONTEXT HEADER
        // -------------------------------------------------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple500, Cyan500)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Slate950,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PathFinder Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                color = Emerald900.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Grounded AI Active",
                                    color = Emerald400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Authoritative guidance inside ${building.name}",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                // Live Navigation Context Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Pill
                    if (navigationContext != null && navigationContext.isLocationUncertain) {
                        Surface(
                            color = Amber500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onScanQr() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Amber400,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Location Uncertain • Scan QR",
                                    color = Amber400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        val floorNum = navigationContext?.currentFloor ?: 1
                        val nearName = navigationContext?.currentNodeName ?: "Floor $floorNum Area"
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Level $floorNum • $nearName",
                                    color = Slate200,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Navigation Status Pill
                    if (isNavigating) {
                        Surface(
                            color = Emerald900.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "To ${navigationContext?.destinationName ?: "Destination"}",
                                    color = Emerald300,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Accessibility Pill
                    if (navigationContext?.isWheelchairMode == true) {
                        Surface(
                            color = Purple900.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Purple400.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Accessible,
                                    contentDescription = null,
                                    tint = Purple400,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Accessible",
                                    color = Purple400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Context-Sensitive Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickQuestions) { question ->
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(100.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier.clickable {
                        onSendMessage(question)
                    }
                ) {
                    Text(
                        text = question,
                        color = Cyan300,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Grounded Indoor Navigation Assistant",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Every answer is strictly verified against actual building map data, active routes, and positioning sensors. Ask where to go, what turn to make next, or request emergency exits.",
                                color = Slate400,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(chatMessages) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isUser -> Purple500.copy(alpha = 0.25f)
                                msg.isEmergency -> Red600.copy(alpha = 0.2f)
                                else -> Slate900
                            }
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        modifier = Modifier
                            .widthIn(max = 330.dp)
                            .border(
                                1.dp,
                                when {
                                    isUser -> Purple500.copy(alpha = 0.5f)
                                    msg.isEmergency -> Red500
                                    else -> Slate800
                                },
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Message Header with Intent Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isUser) "You" else "PathFinder AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) Purple500 else if (msg.isEmergency) Red400 else Cyan400
                                    )

                                    if (!isUser && msg.intent != null) {
                                        Surface(
                                            color = Slate800,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = msg.intent.replace("_", " "),
                                                color = Slate400,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (!isUser) {
                                    IconButton(
                                        onClick = { onSpeakText(msg.message) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Read aloud",
                                            tint = Slate400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Emergency Warning Banner
                            if (msg.isEmergency) {
                                Surface(
                                    color = Red600.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Red500)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Red400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "EMERGENCY EVACUATION MODE",
                                            color = Red400,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = msg.message,
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 20.sp
                            )

                            // Actionable Destination Card (if single destination resolved)
                            if (msg.suggestedNodeId != null) {
                                val matchedNode = building.nodes.firstOrNull { it.id == msg.suggestedNodeId }
                                if (matchedNode != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Slate950),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = matchedNode.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Surface(
                                                    color = Cyan900.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Floor ${matchedNode.floor}",
                                                        color = Cyan300,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (msg.distanceMeters != null && msg.distanceMeters > 0) {
                                                Text(
                                                    text = "Distance: ${msg.distanceMeters.toInt()}m (~${msg.estimatedTimeMinutes ?: 1} min walk)",
                                                    color = Slate400,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Button(
                                                onClick = { onNavigateToNode(matchedNode) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (msg.isEmergency) Red500 else Cyan500
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("ai_navigate_btn_${matchedNode.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (msg.isEmergency) Icons.Default.DirectionsRun else Icons.Default.Navigation,
                                                    contentDescription = null,
                                                    tint = Slate950,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (msg.isEmergency) "Evacuate to ${matchedNode.name}" else "Start Navigation to ${matchedNode.name}",
                                                    color = Slate950,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Multiple Matches Disambiguation (if query matched multiple nodes)
                            if (msg.multipleMatchNodeIds.isNotEmpty()) {
                                Text(
                                    text = "Select your target destination:",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    msg.multipleMatchNodeIds.forEach { nodeId ->
                                        val node = building.nodes.firstOrNull { it.id == nodeId }
                                        if (node != null) {
                                            Surface(
                                                color = Slate800,
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onNavigateToNode(node) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = node.name,
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = "${node.code} • Floor ${node.floor}",
                                                            color = Slate400,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowForward,
                                                        contentDescription = "Select",
                                                        tint = Cyan400,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.border(1.dp, Slate800, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Cyan400,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "PathFinder AI is reasoning...",
                                    color = Slate300,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Field & Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Ask how to reach any room, lab, exit, or restroom...", color = Slate500, fontSize = 13.sp)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = Cyan500,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_assistant_input_field")
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText
                        inputText = ""
                        onSendMessage(textToSend)
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (inputText.isNotBlank()) Cyan500 else Slate800)
                    .testTag("ai_assistant_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Slate950 else Slate500,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
