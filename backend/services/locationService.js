const Location = require('../models/Location');
const SearchHistory = require('../models/SearchHistory');
const { demoBuildings } = require('../utils/seedData');

class LocationService {
  /**
   * Get all locations for a building
   */
  async getLocationsByBuilding(buildingId, category = null, floor = null) {
    try {
      const query = { buildingId };
      if (category) query.category = category.toUpperCase();
      if (floor !== null && floor !== undefined) query.floorNumber = Number(floor);

      const locations = await Location.find(query).lean();
      if (locations && locations.length > 0) {
        return locations;
      }
    } catch (err) {
      console.warn('[LocationService] DB query failed, falling back to memory:', err.message);
    }

    const bld = demoBuildings.find(b => b.buildingId === buildingId);
    if (!bld) return [];

    return bld.nodes
      .filter(n => {
        if (category && n.category !== category.toUpperCase()) return false;
        if (floor !== null && floor !== undefined && n.floor !== Number(floor)) return false;
        return true;
      })
      .map(n => ({
        locationId: `loc_${n.nodeId}`,
        nodeId: n.nodeId,
        buildingId: n.buildingId,
        floorNumber: n.floor,
        name: n.name,
        code: n.code,
        category: n.category,
        description: n.description,
        keywords: n.keywords,
        isAccessible: n.isAccessible,
        isEmergencyExit: n.isEmergencyExit,
        coordinates: { x: n.x, y: n.y }
      }));
  }

  /**
   * Search locations across buildings or within a specific building
   */
  async searchLocations(query, buildingId = null, category = null, userId = null) {
    if (!query || typeof query !== 'string' || query.trim().length === 0) {
      return [];
    }

    const cleanQuery = query.trim().toLowerCase();

    try {
      const filter = {};
      if (buildingId) filter.buildingId = buildingId;
      if (category) filter.category = category.toUpperCase();

      filter.$or = [
        { name: { $regex: cleanQuery, $options: 'i' } },
        { code: { $regex: cleanQuery, $options: 'i' } },
        { description: { $regex: cleanQuery, $options: 'i' } },
        { keywords: { $in: [new RegExp(cleanQuery, 'i')] } }
      ];

      const results = await Location.find(filter).limit(20).lean();
      if (results && results.length > 0) {
        // Record search history if user or building is provided
        if (buildingId) {
          try {
            await SearchHistory.create({
              userId,
              buildingId,
              query: cleanQuery,
              resultCount: results.length
            });
          } catch (e) {
            // non-fatal
          }
        }
        return results;
      }
    } catch (err) {
      console.warn('[LocationService] DB search failed, falling back to memory:', err.message);
    }

    // In-memory search fallback
    let allNodes = [];
    demoBuildings.forEach(b => {
      if (!buildingId || b.buildingId === buildingId) {
        allNodes.push(...b.nodes);
      }
    });

    const matches = allNodes.filter(node => {
      if (category && node.category !== category.toUpperCase()) return false;
      const matchName = node.name.toLowerCase().includes(cleanQuery);
      const matchCode = node.code.toLowerCase().includes(cleanQuery);
      const matchDesc = node.description.toLowerCase().includes(cleanQuery);
      const matchKey = (node.keywords || []).some(k => k.toLowerCase().includes(cleanQuery));
      const matchCat = node.category.toLowerCase().includes(cleanQuery);
      return matchName || matchCode || matchDesc || matchKey || matchCat;
    });

    return matches.map(n => ({
      locationId: `loc_${n.nodeId}`,
      nodeId: n.nodeId,
      buildingId: n.buildingId,
      floorNumber: n.floor,
      name: n.name,
      code: n.code,
      category: n.category,
      description: n.description,
      keywords: n.keywords,
      isAccessible: n.isAccessible,
      isEmergencyExit: n.isEmergencyExit,
      coordinates: { x: n.x, y: n.y }
    }));
  }
}

module.exports = new LocationService();
