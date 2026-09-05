package com.example.data.model

/**
 * Centralized navigation lifecycle state for PathFinder AI (Phase 10).
 */
enum class NavSessionState {
    IDLE,
    SEARCHING,
    DESTINATION_SELECTED,
    CALCULATING_ROUTE,
    ROUTE_READY,
    NAVIGATING,
    OFF_ROUTE,
    RECALCULATING,
    FLOOR_TRANSITION,
    ARRIVED,
    PAUSED,
    ERROR
}
