const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const ordersController = require('../controllers/orders.controller');
const batchesController = require('../controllers/batches.controller');

const router = express.Router();

router.post('/', asyncHandler(ordersController.createOrder));
router.get('/', asyncHandler(ordersController.listOrders));
router.get('/:orderId', asyncHandler(ordersController.getOrder));
router.patch('/:orderId', asyncHandler(ordersController.updateOrder));
router.patch('/:orderId/close', asyncHandler(ordersController.closeOrder));

// Batches are a sub-resource of an order: you always create/list them
// through the order they belong to (POST/GET /api/orders/:orderId/batches).
router.post('/:orderId/batches', asyncHandler(batchesController.createBatch));
router.get('/:orderId/batches', asyncHandler(batchesController.listBatchesForOrder));

module.exports = router;
