package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

data class StepItem(
    val stepNumber: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class UseCaseItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
    val buildingId: String
)

@Composable
fun LandingPageScreen(
    onStartNavigation: () -> Unit,
    onOpenAIAssistant: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSelectBuilding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val features = listOf(
        FeatureItem(
            title = "Smart Indoor Navigation",
            description = "Sub-meter indoor graph pathfinding using precise spatial waypoints and topological maps.",
            icon = Icons.Default.Explore,
            accentColor = Cyan400
        ),
        FeatureItem(
            title = "AI Route Optimization",
            description = "Powered by Google Gemini to parse natural language queries, clinical symptoms, and room numbers.",
            icon = Icons.Default.Psychology,
            accentColor = Purple500
        ),
        FeatureItem(
            title = "Multi-Floor Navigation",
            description = "Seamless multi-level transitions with intelligent elevator, ramp, and stairwell guidance.",
            icon = Icons.Default.Layers,
            accentColor = Indigo500
        ),
        FeatureItem(
            title = "Accessibility-Friendly Routes",
            description = "Step-free wheelchair routes that strictly avoid stairs, narrow passages, and steep grades.",
            icon = Icons.AutoMirrored.Filled.Accessible,
            accentColor = Amber500
        ),
        FeatureItem(
            title = "Real-Time Directions",
            description = "Live turn-by-turn instruction cards with voice speech guidance and distance countdowns.",
            icon = Icons.Default.NearMe,
            accentColor = Emerald400
        ),
        FeatureItem(
            title = "Intelligent Search",
            description = "Instant autocomplete across classrooms, laboratories, departments, offices, and facilities.",
            icon = Icons.Default.Search,
            accentColor = Cyan300
        )
    )

    val steps = listOf(
        StepItem(
            stepNumber = "01",
            title = "Search Destination",
            description = "Type any department, room code, faculty name, or plain-English intent.",
            icon = Icons.Default.Search
        ),
        StepItem(
            stepNumber = "02",
            title = "Select Starting Point",
            description = "Scan an entrance QR code, take a photo of a door sign, or pick from the map.",
            icon = Icons.Default.Place
        ),
        StepItem(
            stepNumber = "03",
            title = "AI Calculates Optimal Route",
            description = "A* graph engine computes the fastest path with distance, turns, and floor changes.",
            icon = Icons.Default.Route
        ),
        StepItem(
            stepNumber = "04",
            title = "Follow Step-by-Step Guidance",
            description = "Experience turn-by-turn navigation cards and audio voice narration.",
            icon = Icons.Default.DirectionsWalk
        )
    )

    val useCases = listOf(
        UseCaseItem(
            title = "Universities & Campuses",
            subtitle = "Engineering blocks, research labs, lecture halls, and admissions.",
            icon = Icons.Default.School,
            tag = "4 Floors",
            buildingId = "univ"
        ),
        UseCaseItem(
            title = "Hospitals & Medical",
            subtitle = "Emergency trauma ER, radiology, cardiology, surgery, and pharmacies.",
            icon = Icons.Default.LocalHospital,
            tag = "3 Floors",
            buildingId = "hosp"
        ),
        UseCaseItem(
            title = "Airports & Terminals",
            subtitle = "Check-in desks, security screening, departure gates A1-B4, and lounges.",
            icon = Icons.Default.FlightTakeoff,
            tag = "2 Levels",
            buildingId = "air"
        ),
        UseCaseItem(
            title = "Shopping Malls",
            subtitle = "Food courts, retail fashion stores, IMAX cinemas, and family restrooms.",
            icon = Icons.Default.Storefront,
            tag = "2 Levels",
            buildingId = "mall"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 1. HERO SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Brush.horizontalGradient(listOf(Cyan500.copy(alpha = 0.5f), Purple500.copy(alpha = 0.5f))), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge
                    Surface(
                        color = Cyan900.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(100.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Text(
                                text = "PATHFINDER AI • INDOOR WAYFINDING",
                                color = Cyan300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Navigate Smarter.\nFind Your Way.",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AI-powered indoor navigation that helps you reach any destination quickly, easily, and intelligently.",
                        fontSize = 15.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onStartNavigation,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hero_start_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Slate950,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Navigation",
                                color = Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenAIAssistant,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate800.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hero_ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Purple500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ask AI Assistant",
                                color = Slate100,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. FEATURES GRID
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CORE CAPABILITIES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Everything You Need to Navigate Indoors",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                features.forEach { feature ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(feature.accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, feature.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = feature.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = feature.description,
                                    fontSize = 13.sp,
                                    color = Slate400,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. HOW IT WORKS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "HOW IT WORKS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple500,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "4 Simple Steps to Your Destination",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate800.copy(alpha = 0.5f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = Purple500.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Purple500.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = step.stepNumber,
                                    color = Purple500,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = step.description,
                                    fontSize = 12.sp,
                                    color = Slate300,
                                    lineHeight = 16.sp
                                )
                            }

                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. USE CASES
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "PRE-CONFIGURED FACILITIES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald400,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Ready-to-Explore Multi-Floor Environments",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                useCases.forEach { useCase ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectBuilding(useCase.buildingId)
                                onStartNavigation()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate800)
                                    .border(1.dp, Slate700, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = useCase.icon,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = useCase.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Surface(
                                        color = Slate800,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = useCase.tag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate300,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = useCase.subtitle,
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. BOTTOM CTA BANNER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Slate900,
                                Slate800
                            )
                        )
                    )
                    .border(1.dp, Cyan500.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Find Your Path with PathFinder AI",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Zero expensive beacons. Instant setup. Precise indoor graph wayfinding for your campus or facility.",
                        fontSize = 13.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Button(
                            onClick = onStartNavigation,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Launch Live Map", color = Slate950, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenAdmin,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate600)
                        ) {
                            Text("Admin Portal", color = Slate200)
                        }
                    }
                }
            }
        }
    }
}
