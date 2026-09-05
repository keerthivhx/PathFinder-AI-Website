const { successResponse } = require('../utils/responseFormatter');

/**
 * @desc    Health check endpoint
 * @route   GET /api/health
 * @access  Public
 */
const getHealthStatus = async (req, res) => {
  return successResponse(res, {
    status: 'ONLINE',
    service: 'PathFinder AI Backend Core Engine',
    version: '1.0.0',
    uptimeSeconds: process.uptime(),
    nodeVersion: process.version,
    memoryUsageMB: Math.round(process.memoryUsage().heapUsed / 1024 / 1024)
  }, 'PathFinder AI system is healthy and operational.');
};

module.exports = {
  getHealthStatus
};
