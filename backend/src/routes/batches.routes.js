const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const batchesController = require('../controllers/batches.controller');

const router = express.Router();

router.get('/:batchId', asyncHandler(batchesController.getBatch));
router.patch('/:batchId/status', asyncHandler(batchesController.updateBatchStatus));

module.exports = router;
