package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_analytics",
    indices = [
        Index(value = ["buildingId"]),
        Index(value = ["query"]),
        Index(value = ["timestamp"]),
        Index(value = ["isSuccessful"])
    ]
)
data class SearchAnalyticsEntity(
    @PrimaryKey
    val searchId: String,
    val buildingId: String,
    val query: String,
    val resultCount: Int,
    val isSuccessful: Boolean,
    val selectedNodeId: String? = null,
    val selectedCategory: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)
