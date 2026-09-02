const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isValidDateString } = require('../utils/validators');

const isNonNegativeInt = (v) => Number.isInteger(v) && v >= 0;
const isPositiveInt = (v) => Number.isInteger(v) && v > 0;

// POST /api/batches/:batchId/quality-logs
// Records an inspection result for a batch. Two design decisions worth
// explaining:
//
// 1. pass_fail is NEVER accepted from the client — it's always computed
//    here from defect_count (0 defects = pass, any defects = fail). Same
//    principle as total_amount in orders.controller.js: a value that can
//    be derived from other stored data shouldn't also be hand-entered,
//    because the two copies can then disagree (e.g. someone logging
//    3 defects but clicking "pass"). Real factories often use a defect
//    *rate* threshold instead of "zero tolerance" — that's a natural
//    extension for Phase 3 (Anomaly Detection), not this phase.
//
// 2. logged_by is a plain user_id in the request body, which is NOT
//    secure — anything sent by the client can be forged. This is a
//    known, temporary gap: real auth (Phase 5) will replace this with
//    the user_id read off a verified JWT, so a client can't claim to be
//    someone else. Until then we at least verify the id refers to a real
//    quality_head account, not just any integer.
async function createQualityLog(req, res) {
  const { batchId } = req.params;
  const { logged_by, qty_checked, defect_count, check_date } = req.body;

  if (!isPositiveInt(logged_by)) {
    throw new AppError(400, 'logged_by is required and must be a positive integer (user_id).');
  }
  if (!isPositiveInt(qty_checked)) {
    throw new AppError(400, 'qty_checked is required and must be a positive integer.');
  }
  if (!isNonNegativeInt(defect_count)) {
    throw new AppError(400, 'defect_count is required and must be a non-negative integer.');
  }
  if (check_date !== undefined && check_date !== null && !isValidDateString(check_date)) {
    throw new AppError(400, 'check_date must be YYYY-MM-DD.');
  }
  if (defect_count > qty_checked) {
    throw new AppError(400, 'defect_count cannot exceed qty_checked.');
  }

  const batchResult = await db.query('SELECT * FROM batches WHERE batch_id = $1', [batchId]);
  if (batchResult.rows.length === 0) {
    throw new AppError(404, 'Batch not found.');
  }
  const batch = batchResult.rows[0];

  // Mirrors the Phase 1 rule that batches only move forward through
  // pending -> in_progress -> completed: you can't meaningfully inspect
  // a batch that hasn't started production yet.
  if (batch.status === 'pending') {
    throw new AppError(400, 'Cannot log a quality check for a batch that has not started production yet.');
  }
  if (qty_checked > batch.quantity) {
    throw new AppError(400, `qty_checked (${qty_checked}) cannot exceed the batch quantity (${batch.quantity}).`);
  }

  const userResult = await db.query('SELECT * FROM users WHERE user_id = $1', [logged_by]);
  if (userResult.rows.length === 0) {
    throw new AppError(400, 'logged_by does not refer to an existing user.');
  }
  if (userResult.rows[0].role !== 'quality_head') {
    throw new AppError(400, 'logged_by must be a quality_head user.');
  }

  const pass_fail = defect_count === 0 ? 'pass' : 'fail';

  const result = await db.query(
    `INSERT INTO quality_logs (batch_id, logged_by, check_date, qty_checked, defect_count, pass_fail)
     VALUES ($1, $2, COALESCE($3, CURRENT_DATE), $4, $5, $6)
     RETURNING *`,
    [batchId, logged_by, check_date ?? null, qty_checked, defect_count, pass_fail]
  );

  res.status(201).json(result.rows[0]);
}

// GET /api/batches/:batchId/quality-logs
async function listLogsForBatch(req, res) {
  const { batchId } = req.params;
  const result = await db.query(
    'SELECT * FROM quality_logs WHERE batch_id = $1 ORDER BY check_date DESC, log_id DESC',
    [batchId]
  );
  res.json(result.rows);
}

// GET /api/quality-logs?pass_fail=fail&batch_id=3
// A general listing endpoint — useful for the Phase 3 anomaly-detection
// script and any future "recent failures" dashboard view.
async function listQualityLogs(req, res) {
  const { pass_fail, batch_id } = req.query;
  const conditions = [];
  const params = [];

  if (pass_fail) {
    params.push(pass_fail);
    conditions.push(`pass_fail = $${params.length}`);
  }
  if (batch_id) {
    params.push(batch_id);
    conditions.push(`batch_id = $${params.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT * FROM quality_logs ${where} ORDER BY check_date DESC, log_id DESC`,
    params
  );
  res.json(result.rows);
}

// GET /api/quality-logs/:logId
async function getQualityLog(req, res) {
  const { logId } = req.params;
  const result = await db.query('SELECT * FROM quality_logs WHERE log_id = $1', [logId]);
  if (result.rows.length === 0) {
    throw new AppError(404, 'Quality log not found.');
  }
  res.json(result.rows[0]);
}

module.exports = { createQualityLog, listLogsForBatch, listQualityLogs, getQualityLog };
