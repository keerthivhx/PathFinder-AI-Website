const express = require('express');
const router = express.Router();

const healthRoutes = require('./healthRoutes');
const buildingRoutes = require('./buildingRoutes');
const locationRoutes = require('./locationRoutes');
const navigationRoutes = require('./navigationRoutes');
const beaconRoutes = require('./beaconRoutes');

// Mount sub-routers
router.use('/', healthRoutes);
router.use('/buildings', buildingRoutes);
router.use('/locations', locationRoutes);
router.use('/navigation', navigationRoutes);
router.use('/beacons', beaconRoutes);

module.exports = router;
