const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const controller = require('../controllers/anomalyDetection.controller');

const router = express.Router();

router.post('/anomaly-detection/run', asyncHandler(controller.runAnomalyDetection));
router.get('/anomaly-flags', asyncHandler(controller.listAnomalyFlags));

module.exports = router;
