const navigationService = require('../services/navigationService');
const { successResponse } = require('../utils/responseFormatter');

/**
 * @desc    Calculate shortest optimal route
 * @route   POST /api/navigation/route
 * @access  Public
 */
const calculateNavigationRoute = async (req, res, next) => {
  try {
    const { buildingId, startNodeId, targetNodeId, isWheelchairMode, isEmergencyMode } = req.body;
    const userId = req.user ? req.user.id : null;

    const route = await navigationService.calculateRoute({
      buildingId,
      startNodeId,
      targetNodeId,
      isWheelchairMode: Boolean(isWheelchairMode),
      isEmergencyMode: Boolean(isEmergencyMode),
      userId
    });

    return successResponse(res, route, 'Optimal navigation route calculated successfully');
  } catch (error) {
    next(error);
  }
};

module.exports = {
  calculateNavigationRoute
};
