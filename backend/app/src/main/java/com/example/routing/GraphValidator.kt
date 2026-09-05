package com.example.routing

import com.example.data.model.*
import java.util.*

object GraphValidator {

    /**
     * Runs complete structural, logical, and accessibility validations on a Building navigation graph.
     */
    fun validateGraph(building: Building): GraphValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val nodeMap = building.nodes.associateBy { it.id }
        val floorNumbers = building.floors.map { it.floorNumber }.toSet()

        if (building.floors.isEmpty()) {
            errors.add("Building has no defined floors.")
        }

        if (building.nodes.isEmpty()) {
            errors.add("Building has no navigation nodes.")
            return GraphValidationReport(
                isValid = false,
                buildingId = building.id,
                buildingName = building.name,
                totalNodes = 0,
                totalEdges = 0,
                totalFloors = building.floors.size,
                accessibleEdgesCount = 0,
                elevatorTransitionsCount = 0,
                stairTransitionsCount = 0,
                rampTransitionsCount = 0,
                errors = errors,
                warnings = warnings
            )
        }

        // 1. Validate Nodes
        val duplicateNodeIds = building.nodes.groupBy { it.id }.filter { it.value.size > 1 }.keys
        if (duplicateNodeIds.isNotEmpty()) {
            errors.add("Duplicate Node IDs detected: ${duplicateNodeIds.joinToString(", ")}")
        }

        for (node in building.nodes) {
            if (node.buildingId.isNotBlank() && node.buildingId != building.id) {
                warnings.add("Node '${node.name}' (${node.id}) has buildingId '${node.buildingId}' mismatching '${building.id}'.")
            }
            if (floorNumbers.isNotEmpty() && node.floor !in floorNumbers) {
                errors.add("Node '${node.name}' references invalid Floor ${node.floor}. Defined floors: $floorNumbers.")
            }
            if (node.x < 0f || node.x > 1000f || node.y < 0f || node.y > 1000f) {
                warnings.add("Node '${node.name}' coordinates (${node.x}, ${node.y}) are outside standard [0..1000] viewport bounds.")
            }
        }

        // 2. Validate Edges & Connectivity
        val seenEdgePairs = mutableSetOf<String>()
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (node in building.nodes) {
            adjacency[node.id] = mutableListOf()
        }

        var accessibleEdges = 0
        var elevatorTransitions = 0
        var stairTransitions = 0
        var rampTransitions = 0

        for (edge in building.edges) {
            val fromNode = nodeMap[edge.fromId]
            val toNode = nodeMap[edge.toId]

            if (fromNode == null || toNode == null) {
                val missing = if (fromNode == null && toNode == null) "${edge.fromId} & ${edge.toId}"
                else if (fromNode == null) edge.fromId else edge.toId
                errors.add("Dangling edge detected: references non-existent node(s) [$missing].")
                continue
            }

            if (edge.fromId == edge.toId) {
                errors.add("Self-loop edge detected on node '${fromNode.name}' (${edge.fromId}).")
            }

            // Duplicate edge check (normalized undirected key)
            val pairKey = if (edge.fromId < edge.toId) "${edge.fromId}__${edge.toId}" else "${edge.toId}__${edge.fromId}"
            if (seenEdgePairs.contains(pairKey)) {
                warnings.add("Duplicate or redundant edge between '${fromNode.name}' and '${toNode.name}'.")
            } else {
                seenEdgePairs.add(pairKey)
            }

            if (edge.distanceMeters <= 0f) {
                errors.add("Invalid edge distance (${edge.distanceMeters}m) between '${fromNode.name}' and '${toNode.name}'. Must be > 0.")
            }

            if (edge.isWheelchairAccessible) {
                accessibleEdges++
            }

            // Multi-floor vertical transition validation
            if (fromNode.floor != toNode.floor) {
                when (edge.edgeType) {
                    EdgeType.ELEVATOR -> elevatorTransitions++
                    EdgeType.STAIRS -> {
                        stairTransitions++
                        if (edge.isWheelchairAccessible) {
                            warnings.add("Stair transition between '${fromNode.name}' (F${fromNode.floor}) and '${toNode.name}' (F${toNode.floor}) is marked wheelchair accessible.")
                        }
                    }
                    EdgeType.RAMP -> rampTransitions++
                    EdgeType.ESCALATOR -> {}
                    else -> {
                        warnings.add("Inter-floor edge between Floor ${fromNode.floor} and Floor ${toNode.floor} is typed as '${edge.edgeType}'. Recommended: ELEVATOR, STAIRS, or RAMP.")
                    }
                }
            }

            adjacency[edge.fromId]?.add(edge.toId)
            if (edge.bidirectional) {
                adjacency[edge.toId]?.add(edge.fromId)
            }
        }

        // 3. Graph Connectivity / Reachability Check (BFS)
        val visited = mutableSetOf<String>()
        val isolatedNodes = mutableListOf<String>()

        for (node in building.nodes) {
            val neighbors = adjacency[node.id] ?: emptyList()
            if (neighbors.isEmpty()) {
                isolatedNodes.add("${node.name} (Floor ${node.floor})")
            }
        }

        if (isolatedNodes.isNotEmpty()) {
            warnings.add("Found ${isolatedNodes.size} isolated node(s) with 0 connections: ${isolatedNodes.take(5).joinToString(", ")}${if (isolatedNodes.size > 5) "..." else ""}")
        }

        // Check overall connected components
        if (building.nodes.isNotEmpty()) {
            val queue: Queue<String> = LinkedList()
            val startNode = building.nodes.first()
            queue.add(startNode.id)
            visited.add(startNode.id)

            while (queue.isNotEmpty()) {
                val current = queue.poll()
                val neighbors = adjacency[current] ?: emptyList()
                for (nbr in neighbors) {
                    if (nbr !in visited) {
                        visited.add(nbr)
                        queue.add(nbr)
                    }
                }
            }

            val unreachableCount = building.nodes.size - visited.size
            if (unreachableCount > 0 && unreachableCount != isolatedNodes.size) {
                warnings.add("$unreachableCount node(s) reside in disconnected graph partitions.")
            }
        }

        val isValid = errors.isEmpty() && building.nodes.isNotEmpty()

        return GraphValidationReport(
            isValid = isValid,
            buildingId = building.id,
            buildingName = building.name,
            totalNodes = building.nodes.size,
            totalEdges = building.edges.size,
            totalFloors = building.floors.size,
            accessibleEdgesCount = accessibleEdges,
            elevatorTransitionsCount = elevatorTransitions,
            stairTransitionsCount = stairTransitions,
            rampTransitionsCount = rampTransitions,
            errors = errors,
            warnings = warnings,
            isolatedNodes = isolatedNodes,
            validatedAt = System.currentTimeMillis()
        )
    }
}
