const { errorResponse } = require('../utils/responseFormatter');

/**
 * Validation middleware for Navigation Route calculation
 */
const validateRouteRequest = (req, res, next) => {
  const { buildingId, startNodeId, targetNodeId } = req.body;

  const errors = [];
  if (!buildingId || typeof buildingId !== 'string') {
    errors.push('buildingId is required and must be a string.');
  }
  if (!startNodeId || typeof startNodeId !== 'string') {
    errors.push('startNodeId is required and must be a string.');
  }
  if (!targetNodeId || typeof targetNodeId !== 'string') {
    errors.push('targetNodeId is required and must be a string.');
  }

  if (errors.length > 0) {
    return errorResponse(res, 'Validation failed for route request', 400, errors);
  }

  next();
};

module.exports = {
  validateRouteRequest
};
