package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.ui.theme.*

@Composable
fun AuthProfileDialog(
    userAccount: UserAccount,
    onRoleToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isAdmin = userAccount.role == "ADMIN"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isAdmin) Amber500 else Cyan500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = userAccount.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = userAccount.email,
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Role Switcher Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Role",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                            Text(
                                text = if (isAdmin) "Administrator (Full Access)" else "Standard User",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) Amber500 else Cyan400
                            )
                        }

                        Button(
                            onClick = {
                                onRoleToggle(if (isAdmin) "USER" else "ADMIN")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdmin) Slate700 else Amber500
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("toggle_role_button")
                        ) {
                            Text(
                                text = if (isAdmin) "Switch to User" else "Switch to Admin",
                                color = if (isAdmin) Slate200 else Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Security & Auth details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Session Authentication Token:",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = userAccount.token,
                            color = Slate300,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Quick stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Favorites", color = Slate400, fontSize = 11.sp)
                            Text("${userAccount.favoriteNodeIds.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Recent Searches", color = Slate400, fontSize = 11.sp)
                            Text("${userAccount.recentNodeIds.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
            ) {
                Text("Close", color = Slate950, fontWeight = FontWeight.Bold)
            }
        }
    )
}
