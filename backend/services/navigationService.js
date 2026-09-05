const buildingService = require('./buildingService');
const { findShortestPath } = require('../algorithms/astar');
const NavigationSession = require('../models/NavigationSession');

class NavigationService {
  /**
   * Calculate indoor navigation route
   */
  async calculateRoute({ buildingId, startNodeId, targetNodeId, isWheelchairMode = false, isEmergencyMode = false, userId = null }) {
    if (!buildingId || !startNodeId || !targetNodeId) {
      throw new Error('buildingId, startNodeId, and targetNodeId are required parameters.');
    }

    // Fetch nodes and edges for building
    const nodes = await buildingService.getNodesByBuilding(buildingId);
    const edges = await buildingService.getEdgesByBuilding(buildingId);

    if (!nodes || nodes.length === 0) {
      throw new Error(`No navigation data found for building '${buildingId}'.`);
    }

    const startNode = nodes.find(n => n.nodeId === startNodeId);
    const targetNode = nodes.find(n => n.nodeId === targetNodeId);

    if (!startNode) {
      throw new Error(`Start node '${startNodeId}' not found in building '${buildingId}'.`);
    }
    if (!targetNode) {
      throw new Error(`Target destination node '${targetNodeId}' not found in building '${buildingId}'.`);
    }

    // Run A* Shortest Path
    const routeResult = findShortestPath(nodes, edges, startNodeId, targetNodeId, isWheelchairMode);

    if (!routeResult) {
      const modeText = isWheelchairMode ? 'wheelchair-accessible' : 'standard';
      throw new Error(`No ${modeText} path exists between '${startNode.name}' and '${targetNode.name}'.`);
    }

    // Extract unique floors traversed
    const floorsTraversed = [...new Set(routeResult.pathNodes.map(n => n.floor))];

    // Log session to DB if available
    try {
      await NavigationSession.create({
        userId,
        buildingId,
        startNodeId,
        targetNodeId,
        isWheelchairMode,
        isEmergencyMode,
        totalDistanceMeters: routeResult.totalDistanceMeters,
        estimatedTimeSeconds: routeResult.estimatedTimeMinutes * 60,
        floorsTraversed,
        stepCount: routeResult.instructions.length,
        status: 'COMPLETED'
      });
    } catch (e) {
      // non-fatal
    }

    return {
      buildingId,
      startNode,
      targetNode,
      isWheelchairMode,
      isEmergencyMode,
      totalDistanceMeters: routeResult.totalDistanceMeters,
      estimatedTimeMinutes: routeResult.estimatedTimeMinutes,
      floorsTraversed,
      stepCount: routeResult.instructions.length,
      pathNodeIds: routeResult.pathNodeIds,
      pathNodes: routeResult.pathNodes,
      instructions: routeResult.instructions
    };
  }
}

module.exports = new NavigationService();
