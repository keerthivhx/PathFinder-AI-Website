package com.example.data.model

data class Beacon(
    val beaconId: String,
    val buildingId: String,
    val floorId: Int = 0,
    val nodeId: String = "",
    val x: Float = 0f, // Coordinates on normalized 0..1000 map canvas
    val y: Float = 0f,
    val major: Int = 1,
    val minor: Int = 1,
    val txPower: Int = -59, // Calibrated RSSI at 1 meter in dBm
    val activeStatus: Boolean = true,
    val macAddress: String = "",
    val description: String = "",
    val name: String = "",
    val pathLossExponent: Float = 2.2f
) {
    val floor: Int get() = floorId
}

enum class PositionConfidence {
    HIGH,
    MEDIUM,
    LOW;

    val label: String
        get() = name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

enum class PositionSource(val label: String) {
    GPS("GPS Outdoor"),
    QR_CALIBRATION("QR Ground Truth"),
    BLE_BEACON("BLE Trilateration"),
    PDR_SENSOR("PDR Step & Compass"),
    MANUAL("Manual Anchor"),
    SIMULATION("Simulation Mode")
}

data class IndoorPosition(
    val buildingId: String,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val nearestNodeId: String? = null,
    val headingDegrees: Float = 0f,
    val confidence: PositionConfidence = PositionConfidence.HIGH,
    val confidenceScore: Float = 1.0f, // 0.0 .. 1.0
    val source: PositionSource = PositionSource.QR_CALIBRATION,
    val activeBeaconsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracyMeters: Float = 1.5f,
    val isSnapped: Boolean = false,
    val snappedNodeId: String? = null
) {
    val floor: Int get() = floorId
    val provider: PositionSource get() = source
}
