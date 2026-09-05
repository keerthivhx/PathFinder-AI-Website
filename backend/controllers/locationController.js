const locationService = require('../services/locationService');
const { successResponse } = require('../utils/responseFormatter');

/**
 * @desc    Get locations for a building
 * @route   GET /api/buildings/:id/locations
 * @access  Public
 */
const getBuildingLocations = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { category, floor } = req.query;
    const locations = await locationService.getLocationsByBuilding(id, category, floor);
    return successResponse(res, locations, `Locations for building '${id}' retrieved successfully`);
  } catch (error) {
    next(error);
  }
};

/**
 * @desc    Search locations across campus
 * @route   GET /api/locations/search
 * @access  Public
 */
const searchLocations = async (req, res, next) => {
  try {
    const { q, buildingId, category } = req.query;
    const userId = req.user ? req.user.id : null;
    const results = await locationService.searchLocations(q, buildingId, category, userId);
    return successResponse(res, results, `Found ${results.length} locations matching '${q || ''}'`);
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getBuildingLocations,
  searchLocations
};
