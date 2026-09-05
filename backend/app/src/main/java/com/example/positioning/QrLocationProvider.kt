package com.example.positioning

import android.util.Log
import com.example.data.model.Building
import com.example.data.model.NavNode
import org.json.JSONObject

object QrLocationProvider {

    data class QrCalibrationPayload(
        val type: String?,
        val buildingId: String?,
        val floorId: Int?,
        val nodeId: String?
    )

    /**
     * Validates and parses a scanned QR string.
     * Supports JSON payload format:
     * {
     *   "type": "PATHFINDER_CALIBRATION",
     *   "buildingId": "univ",
     *   "floorId": 0,
     *   "nodeId": "univ_gate"
     * }
     * Also supports raw anchor nodeId strings (e.g. "univ_gate").
     */
    fun parseAndValidateQr(
        qrRawText: String,
        currentBuilding: Building
    ): Result<NavNode> {
        val trimmed = qrRawText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid PathFinder calibration QR: Empty code."))
        }

        var targetNodeId: String = trimmed
        var targetBuildingId: String? = null
        var targetFloor: Int? = null

        // Try JSON parsing
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val type = json.optString("type")
                if (type.isNotEmpty() && type != "PATHFINDER_CALIBRATION" && type != "CALIBRATION_ANCHOR") {
                    return Result.failure(IllegalArgumentException("Invalid PathFinder calibration QR: Unknown type '$type'."))
                }
                targetBuildingId = json.optString("buildingId").takeIf { it.isNotEmpty() }
                if (json.has("floorId")) {
                    targetFloor = json.optInt("floorId")
                }
                targetNodeId = json.optString("nodeId")
                if (targetNodeId.isEmpty()) {
                    return Result.failure(IllegalArgumentException("Invalid PathFinder calibration QR: Missing nodeId."))
                }
            } catch (e: Exception) {
                Log.w("QrLocationProvider", "Failed to parse JSON QR: ${e.message}")
                return Result.failure(IllegalArgumentException("Invalid PathFinder calibration QR format."))
            }
        }

        // Validate Building Existence & Match
        if (targetBuildingId != null && targetBuildingId != currentBuilding.id) {
            return Result.failure(IllegalArgumentException("This QR belongs to another building ($targetBuildingId). Please select the correct building."))
        }

        // Validate Node Existence in current building
        val matchingNode = currentBuilding.nodes.firstOrNull { it.id.equals(targetNodeId, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Calibration point is unavailable in ${currentBuilding.name}."))

        // Validate Floor Existence if specified
        if (targetFloor != null && matchingNode.floor != targetFloor) {
            Log.w("QrLocationProvider", "Node floor mismatch: QR says $targetFloor, node is on ${matchingNode.floor}")
        }

        return Result.success(matchingNode)
    }

    /**
     * Generates a standard PathFinder calibration JSON string for a given node.
     */
    fun generateCalibrationQrPayload(node: NavNode, buildingId: String): String {
        return JSONObject().apply {
            put("type", "PATHFINDER_CALIBRATION")
            put("buildingId", buildingId)
            put("floorId", node.floor)
            put("nodeId", node.id)
            put("nodeName", node.name)
            put("code", node.code)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
