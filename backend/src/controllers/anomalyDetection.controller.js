const path = require('path');
const fs = require('fs');
const { execFile } = require('child_process');
const { promisify } = require('util');
const db = require('../config/db');
const AppError = require('../utils/AppError');

const execFileAsync = promisify(execFile);

// backend/src/controllers -> up 3 levels -> project root -> ml/
const ML_DIR = path.join(__dirname, '..', '..', '..', 'ml');
const SCRIPT_PATH = path.join(ML_DIR, 'anomaly_detection.py');

// Picks whichever venv layout actually exists (Windows vs. Mac/Linux use
// different folder names inside a venv) so this works for every teammate
// regardless of OS, falling back to a bare 'python3' on PATH as a last
// resort if no venv was ever created.
function resolvePythonPath() {
  const windowsVenvPython = path.join(ML_DIR, '.venv', 'Scripts', 'python.exe');
  const posixVenvPython = path.join(ML_DIR, '.venv', 'bin', 'python');
  if (fs.existsSync(windowsVenvPython)) return windowsVenvPython;
  if (fs.existsSync(posixVenvPython)) return posixVenvPython;
  return 'python3';
}

// POST /api/anomaly-detection/run
// Runs the Python z-score script as a one-off child process rather than
// standing up a separate always-running ML service — simpler to operate
// for a college mini-project (one server to start, not two), and the
// script only takes a couple seconds to run against this dataset size.
// Sales Forecasting (Phase 4) may outgrow this pattern since the app
// needs to *query* forecasts interactively, not just trigger a batch job.
async function runAnomalyDetection(req, res) {
  const pythonPath = resolvePythonPath();

  let stdout;
  try {
    ({ stdout } = await execFileAsync(pythonPath, [SCRIPT_PATH], { cwd: ML_DIR }));
  } catch (err) {
    throw new AppError(500, `Anomaly detection script failed: ${err.stderr || err.message}`);
  }

  const summary = await db.query(
    `SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE is_outlier) AS outliers
     FROM anomaly_flags WHERE metric_type = 'defect_rate'`
  );

  res.json({
    message: 'Anomaly detection run complete.',
    total_flags: Number(summary.rows[0].total),
    outliers: Number(summary.rows[0].outliers),
    script_output: stdout.trim(),
  });
}

// GET /api/anomaly-flags?is_outlier=true
// Joined with quality_logs so a flag is meaningful on its own (which
// batch, what was checked) without a second round trip.
async function listAnomalyFlags(req, res) {
  const { is_outlier } = req.query;
  const conditions = [];
  const params = [];

  if (is_outlier !== undefined) {
    params.push(is_outlier === 'true');
    conditions.push(`af.is_outlier = $${params.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT af.*, ql.batch_id, ql.check_date, ql.qty_checked, ql.defect_count
     FROM anomaly_flags af
     JOIN quality_logs ql ON ql.log_id = af.quality_log_id
     ${where}
     ORDER BY af.value DESC`,
    params
  );
  res.json(result.rows);
}

module.exports = { runAnomalyDetection, listAnomalyFlags };
