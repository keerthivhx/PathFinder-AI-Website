package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Building
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIIntentSearchBar(
    building: Building,
    queryText: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    isAILoading: Boolean,
    isWheelchairMode: Boolean,
    onToggleWheelchair: () -> Unit,
    isEmergencyMode: Boolean,
    onToggleEmergency: () -> Unit,
    onOpenBuildingSelector: () -> Unit,
    onOpenLandmarkScanner: () -> Unit,
    onOpenQRCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val quickIntents = remember(building.id) {
        when (building.id) {
            "hosp" -> listOf(
                "My head hurts & fever" to "headache fever emergency",
                "Where is the X-Ray room?" to "xray ct scan radiology",
                "Heart specialist / ECG" to "cardiology heart check",
                "Get prescription medicines" to "pharmacy medicine store",
                "Emergency Exit" to "emergency fire exit"
            )
            "univ" -> listOf(
                "Pay my tuition fee" to "pay tuition fee accounts",
                "Computer Science Lab 204" to "computer science ai lab",
                "Central Library" to "library quiet study room",
                "Admissions office" to "admissions office 101",
                "Dean faculty offices" to "faculty lounge"
            )
            "air" -> listOf(
                "Tokyo Flight Gate B4" to "gate b4 flight to tokyo",
                "Where is Security Screening?" to "security checkpoint screening",
                "Baggage Claim Carousel 3" to "baggage claim 3 luggage",
                "Grab a hot coffee" to "coffee skyline food court",
                "Star VIP Lounge" to "vip business lounge"
            )
            "mall" -> listOf(
                "IMAX Cinema Multiplex" to "imax cinema movie tickets",
                "Food Court & Burger" to "food court restaurant",
                "VR & Arcade Games" to "arcade games vr",
                "Electronics & Gadgets" to "electronics phones gadgets",
                "Family Restroom" to "restroom toilet"
            )
            else -> listOf(
                "Nearest Elevator" to "elevator",
                "Emergency Exit" to "emergency exit",
                "Restroom" to "restroom"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Main Search Bar with Gemini AI Pill
        Surface(
            color = Slate900.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergencyMode) Red500 else Slate700),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Building Switcher Chip
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clickable(onClick = onOpenBuildingSelector)
                        .testTag("building_selector_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (building.type) {
                                "Hospital" -> Icons.Default.LocalHospital
                                "University" -> Icons.Default.School
                                "Airport" -> Icons.Default.Flight
                                "Mall" -> Icons.Default.Storefront
                                else -> Icons.Default.Business
                            },
                            contentDescription = "Building",
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = building.name.take(14) + if (building.name.length > 14) "…" else "",
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Input Field
                TextField(
                    value = queryText,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Ask AI (e.g. 'headache', 'Gate B4', 'pay fee')...",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate100
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearchSubmit(queryText)
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_search_input")
                )

                // Search or Loading Action
                if (isAILoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Cyan400,
                        strokeWidth = 2.5.dp
                    )
                } else if (queryText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onSearchSubmit(queryText)
                        },
                        modifier = Modifier
                            .background(Cyan500, CircleShape)
                            .size(36.dp)
                            .testTag("ai_submit_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Search", tint = Slate950, modifier = Modifier.size(20.dp))
                    }
                } else {
                    // Quick Landmark Vision camera trigger
                    IconButton(
                        onClick = onOpenLandmarkScanner,
                        modifier = Modifier
                            .background(Slate800, CircleShape)
                            .size(36.dp)
                            .testTag("landmark_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Vision Scan", tint = Cyan400, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Horizontal Actions Strip (Symptom Chips + Mode Filters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // One-Tap Emergency Evacuation Toggle
            Surface(
                color = if (isEmergencyMode) Red600 else Slate900,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergencyMode) Red500 else Red500.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable(onClick = onToggleEmergency)
                    .testTag("emergency_evac_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency",
                        tint = if (isEmergencyMode) Color.White else Red500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEmergencyMode) "EVACUATION ACTIVE" else "Emergency Exit",
                        color = if (isEmergencyMode) Color.White else Red500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Wheelchair / Accessible Route Toggle
            Surface(
                color = if (isWheelchairMode) Emerald900 else Slate900,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isWheelchairMode) Emerald400 else Slate700),
                modifier = Modifier
                    .clickable(onClick = onToggleWheelchair)
                    .testTag("wheelchair_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Accessible,
                        contentDescription = "Wheelchair",
                        tint = if (isWheelchairMode) Emerald400 else Slate300,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWheelchairMode) "Step-Free Only" else "Accessible",
                        color = if (isWheelchairMode) Emerald400 else Slate300,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Zero-Hardware QR Calibration Button
            Surface(
                color = Slate900,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .clickable(onClick = onOpenQRCalibration)
                    .testTag("qr_calibrate_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Anchor", tint = Cyan400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QR Anchor", color = Slate200, fontSize = 12.sp)
                }
            }

            // Quick Intent Chips for Current Building
            for ((label, query) in quickIntents) {
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier.clickable {
                        onQueryChange(label)
                        onSearchSubmit(query)
                    }
                ) {
                    Text(
                        text = label,
                        color = Slate300,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
