package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "navigation_events",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["buildingId"]),
        Index(value = ["eventType"]),
        Index(value = ["timestamp"])
    ]
)
data class NavigationEventEntity(
    @PrimaryKey
    val eventId: String,
    val sessionId: String,
    val buildingId: String,
    val eventType: String,
    val floor: Int,
    val targetNodeId: String? = null,
    val reason: String? = null,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)
