package com.example.positioning

import com.example.data.model.Building
import com.example.data.model.NavEdge
import com.example.data.model.NavNode
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object GraphSnapper {

    data class SnappedResult(
        val snappedX: Float,
        val snappedY: Float,
        val nearestNode: NavNode,
        val distanceToGraph: Float,
        val isSnappedToSegment: Boolean
    )

    /**
     * Snaps an estimated (rawX, rawY) position to the nearest valid graph corridor segment or node
     * strictly within the target floor of the given building.
     *
     * @param maxSnapDistanceThreshold Maximum distance in normalized canvas units (e.g. 80f ~ 8m)
     */
    fun snapToFloorGraph(
        rawX: Float,
        rawY: Float,
        floor: Int,
        building: Building,
        maxSnapDistanceThreshold: Float = 90f
    ): SnappedResult? {
        val floorNodes = building.nodes.filter { it.floor == floor }
        if (floorNodes.isEmpty()) return null

        val nodeMap = floorNodes.associateBy { it.id }
        val floorEdges = building.edges.filter { it.fromId in nodeMap && it.toId in nodeMap }

        var closestNode: NavNode = floorNodes.first()
        var minNodeDist = Float.MAX_VALUE

        for (node in floorNodes) {
            val d = hypot(node.x - rawX, node.y - rawY)
            if (d < minNodeDist) {
                minNodeDist = d
                closestNode = node
            }
        }

        var bestSnapX = closestNode.x
        var bestSnapY = closestNode.y
        var bestDist = minNodeDist
        var isSegment = false

        // Check proximity to corridor segments (edges)
        for (edge in floorEdges) {
            val n1 = nodeMap[edge.fromId] ?: continue
            val n2 = nodeMap[edge.toId] ?: continue

            val proj = projectPointOnSegment(rawX, rawY, n1.x, n1.y, n2.x, n2.y)
            val d = hypot(proj.first - rawX, proj.second - rawY)
            if (d < bestDist) {
                bestDist = d
                bestSnapX = proj.first
                bestSnapY = proj.second
                isSegment = true
                // Identify closest endpoint
                closestNode = if (hypot(n1.x - rawX, n1.y - rawY) <= hypot(n2.x - rawX, n2.y - rawY)) n1 else n2
            }
        }

        // If too far away, apply soft dampening towards the graph rather than hard jumping through obstacles
        if (bestDist > maxSnapDistanceThreshold) {
            val factor = maxSnapDistanceThreshold / bestDist
            val constrainedX = rawX + (bestSnapX - rawX) * factor
            val constrainedY = rawY + (bestSnapY - rawY) * factor
            return SnappedResult(
                snappedX = constrainedX,
                snappedY = constrainedY,
                nearestNode = closestNode,
                distanceToGraph = bestDist,
                isSnappedToSegment = isSegment
            )
        }

        return SnappedResult(
            snappedX = bestSnapX,
            snappedY = bestSnapY,
            nearestNode = closestNode,
            distanceToGraph = bestDist,
            isSnappedToSegment = isSegment
        )
    }

    private fun projectPointOnSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Pair<Float, Float> {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return Pair(x1, y1)

        val t = max(0f, min(1f, ((px - x1) * dx + (py - y1) * dy) / lenSq))
        return Pair(x1 + t * dx, y1 + t * dy)
    }
}
