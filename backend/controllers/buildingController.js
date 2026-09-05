const buildingService = require('../services/buildingService');
const { successResponse, errorResponse } = require('../utils/responseFormatter');

/**
 * @desc    Get all buildings
 * @route   GET /api/buildings
 * @access  Public
 */
const getAllBuildings = async (req, res, next) => {
  try {
    const buildings = await buildingService.getAllBuildings();
    return successResponse(res, buildings, 'Buildings retrieved successfully');
  } catch (error) {
    next(error);
  }
};

/**
 * @desc    Get single building by ID
 * @route   GET /api/buildings/:id
 * @access  Public
 */
const getBuildingById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const building = await buildingService.getBuildingById(id);
    if (!building) {
      return errorResponse(res, `Building with id '${id}' not found`, 404);
    }
    return successResponse(res, building, `Building '${id}' retrieved successfully`);
  } catch (error) {
    next(error);
  }
};

/**
 * @desc    Get floors for a building
 * @route   GET /api/buildings/:id/floors
 * @access  Public
 */
const getBuildingFloors = async (req, res, next) => {
  try {
    const { id } = req.params;
    const floors = await buildingService.getFloorsByBuilding(id);
    return successResponse(res, floors, `Floors for building '${id}' retrieved successfully`);
  } catch (error) {
    next(error);
  }
};

/**
 * @desc    Get navigation nodes for a building
 * @route   GET /api/buildings/:id/nodes
 * @access  Public
 */
const getBuildingNodes = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { floor } = req.query;
    const nodes = await buildingService.getNodesByBuilding(id, floor);
    return successResponse(res, nodes, `Navigation nodes for building '${id}' retrieved successfully`);
  } catch (error) {
    next(error);
  }
};

/**
 * @desc    Get navigation edges for a building
 * @route   GET /api/buildings/:id/edges
 * @access  Public
 */
const getBuildingEdges = async (req, res, next) => {
  try {
    const { id } = req.params;
    const edges = await buildingService.getEdgesByBuilding(id);
    return successResponse(res, edges, `Navigation edges for building '${id}' retrieved successfully`);
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getAllBuildings,
  getBuildingById,
  getBuildingFloors,
  getBuildingNodes,
  getBuildingEdges
};
