const express = require('express');
const router = express.Router();
const {
  getAllBuildings,
  getBuildingById,
  getBuildingFloors,
  getBuildingNodes,
  getBuildingEdges
} = require('../controllers/buildingController');
const { getBuildingLocations } = require('../controllers/locationController');

// Building Endpoints
router.get('/', getAllBuildings);
router.get('/:id', getBuildingById);
router.get('/:id/floors', getBuildingFloors);
router.get('/:id/locations', getBuildingLocations);
router.get('/:id/nodes', getBuildingNodes);
router.get('/:id/edges', getBuildingEdges);

module.exports = router;
