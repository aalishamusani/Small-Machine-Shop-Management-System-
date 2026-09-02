const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const controller = require('../controllers/salesForecast.controller');

const router = express.Router();

router.post('/run', asyncHandler(controller.runSalesForecast));
router.get('/', asyncHandler(controller.listSalesForecast));

module.exports = router;
