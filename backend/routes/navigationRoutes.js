const express = require('express');
const router = express.Router();
const { calculateNavigationRoute } = require('../controllers/navigationController');
const { validateRouteRequest } = require('../middleware/validateRequest');

// Route Calculation Endpoint
router.post('/route', validateRouteRequest, calculateNavigationRoute);

module.exports = router;
