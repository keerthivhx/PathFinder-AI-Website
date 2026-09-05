const express = require('express');
const router = express.Router();
const beaconController = require('../controllers/beaconController');
const { verifyToken, requireAdmin } = require('../middleware/auth');

// Public read routes
router.get('/building/:id', beaconController.getBeaconsByBuilding);
router.get('/:id', beaconController.getBeaconById);

// Admin-protected routes
router.post('/admin', verifyToken, requireAdmin, beaconController.createBeacon);
router.put('/admin/:id', verifyToken, requireAdmin, beaconController.updateBeacon);
router.delete('/admin/:id', verifyToken, requireAdmin, beaconController.deleteBeacon);

module.exports = router;
