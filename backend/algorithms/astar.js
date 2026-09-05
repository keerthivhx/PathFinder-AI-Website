/**
 * A* Multi-Floor Graph Pathfinding Algorithm & Wayfinding Instruction Generator
 */

class PriorityQueue {
  constructor() {
    this.elements = [];
  }

  enqueue(element, priority) {
    this.elements.push({ element, priority });
    this.elements.sort((a, b) => a.priority - b.priority);
  }

  dequeue() {
    return this.elements.shift()?.element;
  }

  isEmpty() {
    return this.elements.length === 0;
  }
}

/**
 * Calculate Euclidean heuristic with 3D floor penalty
 */
const heuristic = (nodeA, nodeB) => {
  const dx = nodeA.x - nodeB.x;
  const dy = nodeA.y - nodeB.y;
  const xyDist = Math.hypot(dx, dy);
  const floorDiff = Math.abs(nodeA.floor - nodeB.floor);
  // Penalty for floor changes to prioritize same-floor routing where possible
  const floorPenalty = floorDiff * 250;
  return xyDist + floorPenalty;
};

/**
 * Compute turn angle between vector (A->B) and (B->C) in degrees
 */
const getAngleTurn = (prevNode, currNode, nextNode) => {
  if (!prevNode || !currNode || !nextNode) return 'STRAIGHT';

  // Floor transition takes precedence
  if (currNode.floor !== nextNode.floor) {
    return nextNode.floor > currNode.floor ? 'ELEVATOR_UP' : 'ELEVATOR_DOWN';
  }

  const v1x = currNode.x - prevNode.x;
  const v1y = currNode.y - prevNode.y;
  const v2x = nextNode.x - currNode.x;
  const v2y = nextNode.y - currNode.y;

  const angle1 = Math.atan2(v1y, v1x);
  const angle2 = Math.atan2(v2y, v2x);

  let diff = (angle2 - angle1) * (180 / Math.PI);
  while (diff > 180) diff -= 360;
  while (diff < -180) diff += 360;

  if (diff > 45 && diff < 135) return 'RIGHT';
  if (diff >= 135) return 'SHARP_RIGHT';
  if (diff < -45 && diff > -135) return 'LEFT';
  if (diff <= -135) return 'SHARP_LEFT';
  if (diff > 15) return 'SLIGHT_RIGHT';
  if (diff < -15) return 'SLIGHT_LEFT';
  return 'STRAIGHT';
};

/**
 * Run A* Shortest Path Algorithm
 * @param {Array} nodes List of NavNodes
 * @param {Array} edges List of NavEdges
 * @param {String} startId Start node identifier
 * @param {String} targetId Target node identifier
 * @param {Boolean} wheelchairOnly If true, filters out stairs and escalators
 * @returns {Object|null} Result path object or null
 */
const findShortestPath = (nodes, edges, startId, targetId, wheelchairOnly = false) => {
  const nodeMap = new Map();
  nodes.forEach(n => nodeMap.set(n.nodeId, n));

  const startNode = nodeMap.get(startId);
  const targetNode = nodeMap.get(targetId);

  if (!startNode || !targetNode) {
    return null;
  }

  if (startId === targetId) {
    return {
      pathNodes: [startNode],
      totalDistanceMeters: 0,
      estimatedTimeMinutes: 0,
      instructions: [
        {
          stepNumber: 1,
          instruction: `You are already at your destination: ${startNode.name}`,
          direction: 'ARRIVE',
          nodeId: startId,
          floor: startNode.floor,
          distanceMeters: 0
        }
      ]
    };
  }

  // Build Adjacency Graph
  const adjacency = new Map();
  nodes.forEach(n => adjacency.set(n.nodeId, []));

  edges.forEach(edge => {
    // Check wheelchair constraint
    if (wheelchairOnly && !edge.isWheelchairAccessible) {
      return;
    }
    if (wheelchairOnly && (edge.edgeType === 'STAIRS' || edge.edgeType === 'ESCALATOR')) {
      return;
    }

    if (adjacency.has(edge.fromId) && adjacency.has(edge.toId)) {
      adjacency.get(edge.fromId).push({
        to: edge.toId,
        dist: edge.distanceMeters,
        type: edge.edgeType
      });

      if (edge.bidirectional !== false) {
        adjacency.get(edge.toId).push({
          to: edge.fromId,
          dist: edge.distanceMeters,
          type: edge.edgeType
        });
      }
    }
  });

  const openSet = new PriorityQueue();
  openSet.enqueue(startId, 0);

  const cameFrom = new Map();
  const gScore = new Map();
  gScore.set(startId, 0);

  const fScore = new Map();
  fScore.set(startId, heuristic(startNode, targetNode));

  const visited = new Set();

  while (!openSet.isEmpty()) {
    const currentId = openSet.dequeue();

    if (currentId === targetId) {
      // Reconstruct path
      const pathNodeIds = [currentId];
      let curr = currentId;
      while (cameFrom.has(curr)) {
        curr = cameFrom.get(curr);
        pathNodeIds.unshift(curr);
      }

      const pathNodes = pathNodeIds.map(id => nodeMap.get(id));
      let totalDistance = 0;

      // Calculate total edge distance
      for (let i = 0; i < pathNodes.length - 1; i++) {
        const u = pathNodes[i];
        const v = pathNodes[i + 1];
        const neighbors = adjacency.get(u.nodeId) || [];
        const edge = neighbors.find(e => e.to === v.nodeId);
        totalDistance += edge ? edge.dist : Math.hypot(u.x - v.x, u.y - v.y) * 0.1;
      }

      // Generate step-by-step instructions
      const instructions = generateInstructions(pathNodes);
      // Walking speed: ~1.2 meters/second (approx 72 meters/minute)
      const estimatedTimeMinutes = Math.max(1, Math.ceil(totalDistance / 70));

      return {
        pathNodeIds,
        pathNodes,
        totalDistanceMeters: Math.round(totalDistance),
        estimatedTimeMinutes,
        instructions
      };
    }

    visited.add(currentId);
    const currentNode = nodeMap.get(currentId);
    const currentG = gScore.get(currentId) || 0;

    const neighbors = adjacency.get(currentId) || [];
    for (const neighbor of neighbors) {
      const neighborId = neighbor.to;
      const neighborNode = nodeMap.get(neighborId);
      if (!neighborNode) continue;

      const tentativeG = currentG + neighbor.dist;

      if (!gScore.has(neighborId) || tentativeG < gScore.get(neighborId)) {
        cameFrom.set(neighborId, currentId);
        gScore.set(neighborId, tentativeG);
        const f = tentativeG + heuristic(neighborNode, targetNode);
        fScore.set(neighborId, f);
        openSet.enqueue(neighborId, f);
      }
    }
  }

  return null; // No route available
};

/**
 * Generate Turn-by-Turn Wayfinding Cards from Path Nodes
 */
const generateInstructions = (pathNodes) => {
  const instructions = [];
  if (!pathNodes || pathNodes.length === 0) return instructions;

  for (let i = 0; i < pathNodes.length; i++) {
    const curr = pathNodes[i];
    const prev = i > 0 ? pathNodes[i - 1] : null;
    const next = i < pathNodes.length - 1 ? pathNodes[i + 1] : null;

    let turn = 'STRAIGHT';
    let text = '';

    if (i === 0) {
      turn = 'START';
      text = `Start at ${curr.name} (${curr.code})`;
    } else if (i === pathNodes.length - 1) {
      turn = 'ARRIVE';
      text = `Arrive at destination: ${curr.name} on Floor ${curr.floor}`;
    } else if (curr.floor !== next.floor) {
      const isElevator = curr.category === 'ELEVATOR' || next.category === 'ELEVATOR';
      const verb = isElevator ? 'Take Elevator' : 'Take Stairs';
      const floorName = next.floor === 0 ? 'Ground Floor' : `Floor ${next.floor}`;
      turn = next.floor > curr.floor ? 'ELEVATOR_UP' : 'ELEVATOR_DOWN';
      text = `${verb} from Floor ${curr.floor} to ${floorName}`;
    } else {
      turn = getAngleTurn(prev, curr, next);
      switch (turn) {
        case 'LEFT':
          text = `Turn left past ${curr.name} and continue along corridor`;
          break;
        case 'RIGHT':
          text = `Turn right at ${curr.name} and follow corridor`;
          break;
        case 'SLIGHT_LEFT':
          text = `Bear slightly left towards ${next.name}`;
          break;
        case 'SLIGHT_RIGHT':
          text = `Bear slightly right towards ${next.name}`;
          break;
        default:
          text = `Continue straight through corridor past ${curr.name}`;
      }
    }

    instructions.push({
      stepNumber: i + 1,
      instruction: text,
      direction: turn,
      nodeId: curr.nodeId,
      floor: curr.floor,
      locationName: curr.name,
      locationCode: curr.code,
      visualSignageHint: curr.visualSignageHint || ''
    });
  }

  return instructions;
};

module.exports = {
  findShortestPath,
  generateInstructions
};
