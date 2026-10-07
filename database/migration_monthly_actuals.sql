-- Migration: monthly_actuals
-- Sales for months AFTER the end of data/SMSMS_monthly_sales_history.csv
-- (which stops at Jul 2026), entered by the Plant Head through the app.
-- ml/train_forecast.py reads the CSV AND this table together, so every
-- month entered here becomes extra training/evaluation data on the next
-- forecast run.
--
-- Safe to run more than once (IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS monthly_actuals (
    month       DATE PRIMARY KEY CHECK (month = date_trunc('month', month)::date),  -- always the 1st of a month; PK also stops a month being entered twice
    amount      NUMERIC(14,2) NOT NULL CHECK (amount > 0),  -- total dispatched sales value for that month, in rupees
    entered_by  INT NOT NULL REFERENCES users(user_id),
    created_at  TIMESTAMP DEFAULT NOW()
);
