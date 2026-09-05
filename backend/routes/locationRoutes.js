const express = require('express');
const router = express.Router();
const { searchLocations } = require('../controllers/locationController');

// Search Endpoint
router.get('/search', searchLocations);

module.exports = router;
