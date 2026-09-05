const Beacon = require('../models/Beacon');
const { successResponse, errorResponse } = require('../utils/responseFormatter');

/**
 * Get all beacons for a specific building
 * GET /api/buildings/:id/beacons
 */
exports.getBeaconsByBuilding = async (req, res, next) => {
  try {
    const { id } = req.params;
    const beacons = await Beacon.find({ buildingId: id, activeStatus: true }).lean();
    return successResponse(res, beacons, `Found ${beacons.length} beacons for building ${id}`);
  } catch (error) {
    next(error);
  }
};

/**
 * Get a single beacon by ID
 * GET /api/beacons/:id
 */
exports.getBeaconById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const beacon = await Beacon.findOne({ beaconId: id }).lean();
    if (!beacon) {
      return errorResponse(res, `Beacon '${id}' not found`, 404);
    }
    return successResponse(res, beacon, 'Beacon retrieved successfully');
  } catch (error) {
    next(error);
  }
};

/**
 * Create a new beacon (Admin)
 * POST /api/admin/beacons
 */
exports.createBeacon = async (req, res, next) => {
  try {
    const { beaconId, buildingId, floorId, nodeId, x, y, major, minor, txPower, activeStatus, metadata } = req.body;
    
    if (!beaconId || !buildingId || !nodeId) {
      return errorResponse(res, 'beaconId, buildingId, and nodeId are required', 400);
    }

    const existing = await Beacon.findOne({ beaconId });
    if (existing) {
      return errorResponse(res, `Beacon with ID '${beaconId}' already exists`, 409);
    }

    const newBeacon = await Beacon.create({
      beaconId,
      buildingId,
      floorId: floorId || 0,
      nodeId,
      x: x !== undefined ? x : 500,
      y: y !== undefined ? y : 500,
      major: major || 1,
      minor: minor || 1,
      txPower: txPower || -59,
      activeStatus: activeStatus !== undefined ? activeStatus : true,
      metadata: metadata || {}
    });

    return successResponse(res, newBeacon, 'Beacon registered successfully', 201);
  } catch (error) {
    next(error);
  }
};

/**
 * Update an existing beacon (Admin)
 * PUT /api/admin/beacons/:id
 */
exports.updateBeacon = async (req, res, next) => {
  try {
    const { id } = req.params;
    const updated = await Beacon.findOneAndUpdate(
      { beaconId: id },
      { $set: req.body },
      { new: true, runValidators: true }
    ).lean();

    if (!updated) {
      return errorResponse(res, `Beacon '${id}' not found`, 404);
    }

    return successResponse(res, updated, 'Beacon updated successfully');
  } catch (error) {
    next(error);
  }
};

/**
 * Delete a beacon (Admin)
 * DELETE /api/admin/beacons/:id
 */
exports.deleteBeacon = async (req, res, next) => {
  try {
    const { id } = req.params;
    const deleted = await Beacon.findOneAndDelete({ beaconId: id });
    if (!deleted) {
      return errorResponse(res, `Beacon '${id}' not found`, 404);
    }
    return successResponse(res, { beaconId: id, deleted: true }, 'Beacon deleted successfully');
  } catch (error) {
    next(error);
  }
};
