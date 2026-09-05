const Building = require('../models/Building');
const Floor = require('../models/Floor');
const NavNode = require('../models/NavNode');
const NavEdge = require('../models/NavEdge');
const { demoBuildings } = require('../utils/seedData');

class BuildingService {
  /**
   * Get all buildings
   */
  async getAllBuildings() {
    try {
      const buildings = await Building.find({ activeStatus: true }).lean();
      if (buildings && buildings.length > 0) {
        return buildings;
      }
    } catch (err) {
      console.warn('[BuildingService] DB query failed, falling back to memory:', err.message);
    }
    // Fallback
    return demoBuildings.map(b => ({
      buildingId: b.buildingId,
      name: b.name,
      type: b.type,
      description: b.description,
      address: b.address,
      totalFloors: b.totalFloors,
      activeStatus: b.activeStatus
    }));
  }

  /**
   * Get building by ID with full details
   */
  async getBuildingById(buildingId) {
    try {
      const building = await Building.findOne({ buildingId }).lean();
      if (building) {
        return building;
      }
    } catch (err) {
      console.warn('[BuildingService] DB query failed, falling back to memory:', err.message);
    }
    return demoBuildings.find(b => b.buildingId === buildingId) || null;
  }

  /**
   * Get floors for a building
   */
  async getFloorsByBuilding(buildingId) {
    try {
      const floors = await Floor.find({ buildingId }).sort({ floorNumber: 1 }).lean();
      if (floors && floors.length > 0) {
        return floors;
      }
    } catch (err) {
      console.warn('[BuildingService] DB query failed, falling back to memory:', err.message);
    }
    const bld = demoBuildings.find(b => b.buildingId === buildingId);
    return bld ? bld.floors : [];
  }

  /**
   * Get all navigation nodes for a building
   */
  async getNodesByBuilding(buildingId, floor = null) {
    try {
      const query = { buildingId };
      if (floor !== null && floor !== undefined) {
        query.floor = Number(floor);
      }
      const nodes = await NavNode.find(query).lean();
      if (nodes && nodes.length > 0) {
        return nodes;
      }
    } catch (err) {
      console.warn('[BuildingService] DB query failed, falling back to memory:', err.message);
    }
    const bld = demoBuildings.find(b => b.buildingId === buildingId);
    if (!bld) return [];
    if (floor !== null && floor !== undefined) {
      return bld.nodes.filter(n => n.floor === Number(floor));
    }
    return bld.nodes;
  }

  /**
   * Get all navigation edges for a building
   */
  async getEdgesByBuilding(buildingId) {
    try {
      const edges = await NavEdge.find({ buildingId }).lean();
      if (edges && edges.length > 0) {
        return edges;
      }
    } catch (err) {
      console.warn('[BuildingService] DB query failed, falling back to memory:', err.message);
    }
    const bld = demoBuildings.find(b => b.buildingId === buildingId);
    return bld ? bld.edges : [];
  }
}

module.exports = new BuildingService();
