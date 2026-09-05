package com.example.routing

import com.example.data.model.*
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

object AStarRouter {

    data class PathfindingResult(
        val route: NavigationRoute?,
        val error: String? = null
    )

    data class GraphValidationResult(
        val isValid: Boolean,
        val validNodeCount: Int,
        val validEdgeCount: Int,
        val danglingEdges: List<NavEdge> = emptyList(),
        val isolatedNodes: List<NavNode> = emptyList(),
        val errors: List<String> = emptyList()
    )

    private data class NodeRecord(
        val nodeId: String,
        val gCost: Float,
        val fCost: Float,
        val parentId: String? = null,
        val edgeUsed: NavEdge? = null
    ) : Comparable<NodeRecord> {
        override fun compareTo(other: NodeRecord): Int = this.fCost.compareTo(other.fCost)
    }

    /**
     * Validates a building's navigation graph integrity without crashing.
     */
    fun validateGraph(building: Building): GraphValidationResult {
        val errors = mutableListOf<String>()
        if (building.nodes.isEmpty()) {
            errors.add("Building contains 0 navigation nodes.")
            return GraphValidationResult(false, 0, 0, errors = errors)
        }

        val nodeMap = building.nodes.associateBy { it.id }
        val danglingEdges = mutableListOf<NavEdge>()
        val connectedNodeIds = mutableSetOf<String>()

        for (edge in building.edges) {
            val fromExists = nodeMap.containsKey(edge.fromId)
            val toExists = nodeMap.containsKey(edge.toId)
            if (!fromExists || !toExists) {
                danglingEdges.add(edge)
            } else {
                connectedNodeIds.add(edge.fromId)
                connectedNodeIds.add(edge.toId)
            }
        }

        val isolatedNodes = building.nodes.filterNot { it.id in connectedNodeIds }
        if (danglingEdges.isNotEmpty()) {
            errors.add("Found ${danglingEdges.size} dangling edge(s) with invalid node references.")
        }

        return GraphValidationResult(
            isValid = building.nodes.isNotEmpty() && errors.isEmpty(),
            validNodeCount = building.nodes.size,
            validEdgeCount = building.edges.size - danglingEdges.size,
            danglingEdges = danglingEdges,
            isolatedNodes = isolatedNodes,
            errors = errors
        )
    }

    /**
     * Finds the shortest path between startNode and targetNode using A* graph search.
     */
    fun findPath(
        building: Building,
        startNodeId: String,
        targetNodeId: String,
        wheelchairOnly: Boolean = false,
        isEmergency: Boolean = false,
        walkingSpeedMetersPerSec: Float = 1.4f
    ): PathfindingResult {
        val nodeMap = building.nodes.associateBy { it.id }
        val startNode = nodeMap[startNodeId] ?: return PathfindingResult(null, "Start node '$startNodeId' not found in building graph")
        val targetNode = nodeMap[targetNodeId] ?: return PathfindingResult(null, "Destination node '$targetNodeId' not found in building graph")

        if (startNodeId == targetNodeId) {
            val step = RouteStep(
                stepIndex = 0,
                instruction = "You are already at ${targetNode.name}",
                detail = "Current location matches destination (${targetNode.code})",
                turnType = TurnType.ARRIVAL,
                distanceMeters = 0f,
                targetNode = targetNode,
                floor = targetNode.floor
            )
            return PathfindingResult(
                NavigationRoute(
                    pathNodes = listOf(targetNode),
                    steps = listOf(step),
                    totalDistanceMeters = 0f,
                    estimatedTimeSeconds = 0,
                    transferFloors = listOf(targetNode.floor),
                    hasStairs = false,
                    hasElevators = false,
                    isWheelchairSafe = true,
                    isEmergencyRoute = isEmergency
                )
            )
        }

        // Build adjacency map (safely skipping invalid/dangling edges)
        val adjacency = mutableMapOf<String, MutableList<Pair<NavEdge, NavNode>>>()
        for (edge in building.edges) {
            val from = nodeMap[edge.fromId]
            val to = nodeMap[edge.toId]
            if (from != null && to != null) {
                // Filter wheelchair non-accessible paths
                if (wheelchairOnly) {
                    if (edge.edgeType == EdgeType.STAIRS || edge.edgeType == EdgeType.ESCALATOR || !edge.isWheelchairAccessible) {
                        continue
                    }
                }
                adjacency.getOrPut(edge.fromId) { mutableListOf() }.add(Pair(edge, to))
                if (edge.bidirectional) {
                    adjacency.getOrPut(edge.toId) { mutableListOf() }.add(Pair(edge, from))
                }
            }
        }

        val openSet = PriorityQueue<NodeRecord>()
        val closedSet = mutableSetOf<String>()
        val gCosts = mutableMapOf<String, Float>()
        val parentRecords = mutableMapOf<String, NodeRecord>()

        val initialH = heuristic(startNode, targetNode)
        val startRecord = NodeRecord(startNodeId, 0f, initialH, null, null)
        openSet.add(startRecord)
        gCosts[startNodeId] = 0f
        parentRecords[startNodeId] = startRecord

        var destinationFound = false
        var lastRecord: NodeRecord? = null

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.nodeId == targetNodeId) {
                destinationFound = true
                lastRecord = current
                break
            }

            if (current.nodeId in closedSet) continue
            closedSet.add(current.nodeId)

            val currentNode = nodeMap[current.nodeId] ?: continue
            val neighbors = adjacency[current.nodeId] ?: emptyList()

            for ((edge, neighborNode) in neighbors) {
                if (neighborNode.id in closedSet) continue

                var edgeCost = edge.distanceMeters
                // Add vertical transfer cost if different floors
                if (currentNode.floor != neighborNode.floor) {
                    edgeCost += if (edge.edgeType == EdgeType.ELEVATOR) 15f else 25f
                }

                val tentativeG = current.gCost + edgeCost
                val currentBestG = gCosts.getOrDefault(neighborNode.id, Float.MAX_VALUE)

                if (tentativeG < currentBestG) {
                    gCosts[neighborNode.id] = tentativeG
                    val h = heuristic(neighborNode, targetNode)
                    val record = NodeRecord(neighborNode.id, tentativeG, tentativeG + h, current.nodeId, edge)
                    parentRecords[neighborNode.id] = record
                    openSet.add(record)
                }
            }
        }

        if (!destinationFound || lastRecord == null) {
            return PathfindingResult(
                null,
                if (wheelchairOnly) "No accessible route is available from this location." else "No route available to destination."
            )
        }

        // Reconstruct path
        val pathNodes = mutableListOf<NavNode>()
        val edgesUsed = mutableListOf<NavEdge>()
        var curr: NodeRecord? = lastRecord

        while (curr != null) {
            val node = nodeMap[curr.nodeId]
            if (node != null) {
                pathNodes.add(0, node)
            }
            if (curr.edgeUsed != null) {
                edgesUsed.add(0, curr.edgeUsed!!)
            }
            curr = curr.parentId?.let { parentRecords[it] }
        }

        if (pathNodes.isEmpty()) {
            return PathfindingResult(null, "Could not trace route")
        }

        // Build turn-by-turn navigation steps
        val steps = generateSteps(pathNodes, edgesUsed, isEmergency)
        var totalDist = 0f
        for (edge in edgesUsed) {
            totalDist += edge.distanceMeters
        }
        if (totalDist == 0f && pathNodes.size > 1) {
            for (i in 0 until pathNodes.size - 1) {
                totalDist += spatialDistanceMeters(pathNodes[i], pathNodes[i + 1])
            }
        }

        val hasStairs = edgesUsed.any { it.edgeType == EdgeType.STAIRS }
        val hasElevators = edgesUsed.any { it.edgeType == EdgeType.ELEVATOR }
        val transferFloors = pathNodes.map { it.floor }.distinct()
        val effectiveSpeed = if (walkingSpeedMetersPerSec > 0.1f) walkingSpeedMetersPerSec else 1.4f
        val walkingSeconds = totalDist / effectiveSpeed
        val elevatorDelay = if (hasElevators) 40 else 0
        val estimatedSeconds = (walkingSeconds + elevatorDelay).roundToInt()

        val route = NavigationRoute(
            pathNodes = pathNodes,
            steps = steps,
            totalDistanceMeters = totalDist,
            estimatedTimeSeconds = estimatedSeconds,
            transferFloors = transferFloors,
            hasStairs = hasStairs,
            hasElevators = hasElevators,
            isWheelchairSafe = !hasStairs && (!edgesUsed.any { !it.isWheelchairAccessible }),
            isEmergencyRoute = isEmergency
        )

        return PathfindingResult(route)
    }

    /**
     * One-Tap Emergency Evacuation: finds the closest emergency exit from user position.
     */
    fun findEmergencyEvacuationPath(
        building: Building,
        startNodeId: String,
        wheelchairOnly: Boolean = false,
        walkingSpeedMetersPerSec: Float = 1.4f
    ): PathfindingResult {
        val exits = building.nodes.filter { it.isEmergencyExit || it.category == NodeCategory.EMERGENCY_EXIT }
        if (exits.isEmpty()) {
            // Fallback to entrance
            val entrance = building.nodes.firstOrNull { it.category == NodeCategory.ENTRANCE }
                ?: return PathfindingResult(null, "No emergency exits designated in building")
            return findPath(building, startNodeId, entrance.id, wheelchairOnly, isEmergency = true)
        }

        var bestRoute: NavigationRoute? = null
        var minDistance = Float.MAX_VALUE

        for (exit in exits) {
            val result = findPath(building, startNodeId, exit.id, wheelchairOnly, isEmergency = true)
            if (result.route != null && result.route.totalDistanceMeters < minDistance) {
                minDistance = result.route.totalDistanceMeters
                bestRoute = result.route
            }
        }

        if (bestRoute != null) {
            return PathfindingResult(bestRoute)
        }

        return PathfindingResult(null, "Unable to compute evacuation path from current location")
    }

    private fun heuristic(a: NavNode, b: NavNode): Float {
        val dx = (a.x - b.x) * 0.1f // 1000 coordinate scale -> ~100m
        val dy = (a.y - b.y) * 0.1f
        val floorDiff = kotlin.math.abs(a.floor - b.floor) * 20f
        return hypot(dx, dy) + floorDiff
    }

    private fun spatialDistanceMeters(a: NavNode, b: NavNode): Float {
        val dx = (a.x - b.x) * 0.12f
        val dy = (a.y - b.y) * 0.12f
        val floorPenalty = kotlin.math.abs(a.floor - b.floor) * 12f
        return hypot(dx, dy) + floorPenalty
    }

    private fun generateSteps(nodes: List<NavNode>, edges: List<NavEdge>, isEmergency: Boolean): List<RouteStep> {
        val steps = mutableListOf<RouteStep>()
        if (nodes.isEmpty()) return steps

        if (nodes.size == 1) {
            val n = nodes[0]
            steps.add(
                RouteStep(
                    stepIndex = 0,
                    instruction = if (isEmergency) "EVACUATION: Exit safely at ${n.name}" else "Arrive at ${n.name}",
                    detail = n.description.ifEmpty { "Floor ${n.floor}" },
                    turnType = if (isEmergency) TurnType.EMERGENCY_EVACUATE else TurnType.ARRIVAL,
                    distanceMeters = 0f,
                    targetNode = n,
                    floor = n.floor
                )
            )
            return steps
        }

        // Initial Start Step
        val first = nodes[0]
        val second = nodes[1]
        val initialDist = edges.firstOrNull()?.distanceMeters ?: spatialDistanceMeters(first, second)

        val firstEdge = edges.firstOrNull()
        val initialTurn = when {
            isEmergency -> TurnType.EMERGENCY_EVACUATE
            firstEdge?.edgeType == EdgeType.ELEVATOR && second.floor > first.floor -> TurnType.ELEVATOR_UP
            firstEdge?.edgeType == EdgeType.ELEVATOR && second.floor < first.floor -> TurnType.ELEVATOR_DOWN
            firstEdge?.edgeType == EdgeType.STAIRS && second.floor > first.floor -> TurnType.STAIRS_UP
            firstEdge?.edgeType == EdgeType.STAIRS && second.floor < first.floor -> TurnType.STAIRS_DOWN
            else -> TurnType.START
        }

        steps.add(
            RouteStep(
                stepIndex = 0,
                instruction = if (isEmergency) "EMERGENCY: Proceed toward ${second.name}" else "Start at ${first.name}, head toward ${second.name}",
                detail = if (first.floor != second.floor) "Floor transition: Level ${first.floor} -> Level ${second.floor}" else "Walk straight along corridor (${initialDist.roundToInt()}m)",
                turnType = initialTurn,
                distanceMeters = initialDist,
                targetNode = second,
                floor = first.floor
            )
        )

        for (i in 1 until nodes.size - 1) {
            val prev = nodes[i - 1]
            val curr = nodes[i]
            val next = nodes[i + 1]
            val edge = edges.getOrNull(i)
            val dist = edge?.distanceMeters ?: spatialDistanceMeters(curr, next)

            val turnType: TurnType
            val instruction: String
            val detail: String

            if (curr.floor != next.floor) {
                if (edge?.edgeType == EdgeType.ELEVATOR) {
                    turnType = if (next.floor > curr.floor) TurnType.ELEVATOR_UP else TurnType.ELEVATOR_DOWN
                    instruction = "Take Elevator to Floor ${next.floor}"
                    detail = "Exit elevator on Level ${next.floor} toward ${next.name}"
                } else if (edge?.edgeType == EdgeType.STAIRS) {
                    turnType = if (next.floor > curr.floor) TurnType.STAIRS_UP else TurnType.STAIRS_DOWN
                    instruction = "Take Stairs to Floor ${next.floor}"
                    detail = "Ascend/Descend stairwell to Level ${next.floor}"
                } else {
                    turnType = if (next.floor > curr.floor) TurnType.RAMP_UP else TurnType.RAMP_DOWN
                    instruction = "Follow Ramp to Floor ${next.floor}"
                    detail = "Accessible ramp to Level ${next.floor}"
                }
            } else {
                // Calculate 2D turn angle
                val angle1 = atan2(curr.y - prev.y, curr.x - prev.x)
                val angle2 = atan2(next.y - curr.y, next.x - curr.x)
                var diffDeg = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()
                while (diffDeg < -180f) diffDeg += 360f
                while (diffDeg > 180f) diffDeg -= 360f

                turnType = when {
                    diffDeg in -25f..25f -> TurnType.STRAIGHT
                    diffDeg in -65f..-25f -> TurnType.SLIGHT_LEFT
                    diffDeg in -120f..-65f -> TurnType.LEFT
                    diffDeg < -120f -> TurnType.SHARP_LEFT
                    diffDeg in 25f..65f -> TurnType.SLIGHT_RIGHT
                    diffDeg in 65f..120f -> TurnType.RIGHT
                    else -> TurnType.SHARP_RIGHT
                }

                val actionWord = when (turnType) {
                    TurnType.STRAIGHT -> "Continue straight"
                    TurnType.SLIGHT_LEFT -> "Bear slightly left"
                    TurnType.LEFT -> "Turn left"
                    TurnType.SHARP_LEFT -> "Sharp left turn"
                    TurnType.SLIGHT_RIGHT -> "Bear slightly right"
                    TurnType.RIGHT -> "Turn right"
                    TurnType.SHARP_RIGHT -> "Sharp right turn"
                    else -> "Head toward"
                }

                instruction = "$actionWord at ${curr.name} toward ${next.name}"
                detail = if (curr.visualSignageHint.isNotEmpty()) {
                    "Look for sign: \"${curr.visualSignageHint}\" (${dist.roundToInt()}m)"
                } else {
                    "Follow hallway for ~${dist.roundToInt()} meters on Level ${curr.floor}"
                }
            }

            steps.add(
                RouteStep(
                    stepIndex = steps.size,
                    instruction = instruction,
                    detail = detail,
                    turnType = turnType,
                    distanceMeters = dist,
                    targetNode = next,
                    floor = curr.floor
                )
            )
        }

        // Final arrival step
        val dest = nodes.last()
        steps.add(
            RouteStep(
                stepIndex = steps.size,
                instruction = if (isEmergency) "EMERGENCY EVACUATION COMPLETE: You have reached ${dest.name}" else "Arrive at destination: ${dest.name} (${dest.code})",
                detail = dest.description.ifEmpty { "Destination is on your floor (Level ${dest.floor})" },
                turnType = if (isEmergency) TurnType.EMERGENCY_EVACUATE else TurnType.ARRIVAL,
                distanceMeters = 0f,
                targetNode = dest,
                floor = dest.floor
            )
        )

        return steps
    }
}
