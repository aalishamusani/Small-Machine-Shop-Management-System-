const db = require('../config/db');
const AppError = require('../utils/AppError');
const { runMlScript } = require('../utils/mlRunner');
const { isValidDateString, isPositiveNumber } = require('../utils/validators');
const { getCsvLastMonth, nextMonthAfter, currentMonthStart } = require('../utils/salesHistory');

// POST /api/sales-forecast/run
async function runSalesForecast(req, res) {
  const scriptOutput = await runMlScript('train_forecast.py');

  const rows = await db.query(
    `SELECT model_used,
            COUNT(*) FILTER (WHERE actual_value IS NOT NULL) AS evaluation_rows,
            COUNT(*) FILTER (WHERE actual_value IS NULL) AS future_rows
     FROM sales_forecast
     GROUP BY model_used`
  );

  res.json({
    message: 'Sales forecast run complete.',
    models_used: rows.rows,
    script_output: scriptOutput,
  });
}

// GET /api/sales-forecast
// Returns every row (past evaluation + future forecast) ordered by date —
// the Android results screen plots this directly rather than the backend
// pre-computing MAE/RMSE itself, since that's a trivial derived value the
// client can compute from actual_value/predicted_value pairs it already
// has, same "don't duplicate a value that's derivable" principle as
// total_amount back in Phase 1.
async function listSalesForecast(req, res) {
  const result = await db.query('SELECT * FROM sales_forecast ORDER BY forecast_date');
  res.json(result.rows);
}

// Works out where the sales history currently ends (the later of: the CSV's
// last month, the latest month entered through the app) and therefore which
// single month may be entered next.
async function loadActualsStatus() {
  const entered = await db.query('SELECT month, amount FROM monthly_actuals ORDER BY month');
  const csvLast = getCsvLastMonth();
  const lastEntered = entered.rows.length ? entered.rows[entered.rows.length - 1].month : null;
  const historyEnd = lastEntered && lastEntered > csvLast ? lastEntered : csvLast;
  const nextMonth = nextMonthAfter(historyEnd);

  return {
    history_end: historyEnd,
    next_month: nextMonth,
    can_enter: nextMonth < currentMonthStart(),
    entered: entered.rows,
  };
}

// GET /api/sales-forecast/actuals
// Tells the app which month it should ask the Plant Head for next.
async function getActualsStatus(req, res) {
  res.json(await loadActualsStatus());
}

// POST /api/sales-forecast/actuals   { month: 'YYYY-MM-01', amount: 3500000 }
// Records the real total sales of one finished month. Two rules keep the
// time series valid: months must be added in order with no gaps (a
// forecasting model can't handle a missing month in the middle), and a
// month can only be entered once it has actually ended.
async function addActual(req, res) {
  const { month, amount } = req.body;

  if (!isValidDateString(month)) {
    throw new AppError(400, 'month is required and must be YYYY-MM-DD.');
  }
  if (!isPositiveNumber(amount) || amount >= 1e11) {
    throw new AppError(400, 'amount is required and must be a positive number.');
  }

  const status = await loadActualsStatus();
  if (month !== status.next_month) {
    throw new AppError(
      400,
      `The next month that can be entered is ${status.next_month.slice(0, 7)} — months must be added in order, with no gaps.`
    );
  }
  if (!status.can_enter) {
    throw new AppError(400, `${month.slice(0, 7)} has not finished yet — sales can only be entered for completed months.`);
  }

  try {
    const result = await db.query(
      `INSERT INTO monthly_actuals (month, amount, entered_by)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [month, amount, req.user.user_id]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    // 23505 = unique_violation: two people submitted the same month at once.
    if (err.code === '23505') throw new AppError(409, 'That month has already been entered.');
    throw err;
  }
}

module.exports = { runSalesForecast, listSalesForecast, getActualsStatus, addActual };
