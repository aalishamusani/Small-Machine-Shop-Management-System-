const path = require('path');
const fs = require('fs');

// backend/src/utils -> up 3 levels -> project root -> data/
const HISTORY_CSV = path.join(__dirname, '..', '..', '..', 'data', 'SMSMS_monthly_sales_history.csv');

// The last month covered by the original CSV, as 'YYYY-MM-01'. Months the
// Plant Head enters later (monthly_actuals table) continue from here.
// The CSV's first column is the month ('2025-07-01'), so no real CSV parser
// is needed — just take that column and keep the latest.
function getCsvLastMonth() {
  const lines = fs.readFileSync(HISTORY_CSV, 'utf8').split(/\r?\n/).slice(1);
  const months = lines
    .map((line) => line.split(',')[0].trim())
    .filter((m) => /^\d{4}-\d{2}-\d{2}$/.test(m));
  return months.sort().pop();
}

// Monthly totals from the original CSV (2nd column), used to judge whether a
// newly entered figure is plausible.
function getCsvAmounts() {
  const lines = fs.readFileSync(HISTORY_CSV, 'utf8').split(/\r?\n/).slice(1);
  return lines
    .map((line) => Number(line.split(',')[1]))
    .filter((n) => Number.isFinite(n) && n > 0);
}

// '2026-07-01' -> '2026-08-01' (rolls over the year correctly: Dec -> Jan).
function nextMonthAfter(monthStr) {
  const year = Number(monthStr.slice(0, 4));
  const month = Number(monthStr.slice(5, 7)); // 1-12
  const next = month === 12 ? { y: year + 1, m: 1 } : { y: year, m: month + 1 };
  return `${next.y}-${String(next.m).padStart(2, '0')}-01`;
}

// First day of the current month, 'YYYY-MM-01'. A month's sales figure is
// only final once the month is over, so only months strictly BEFORE this
// one can be entered.
function currentMonthStart() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
}

module.exports = { getCsvLastMonth, getCsvAmounts, nextMonthAfter, currentMonthStart };
