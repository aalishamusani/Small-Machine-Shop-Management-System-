package com.smsms.app.model

data class SalesForecast(
    val forecast_id: Int,
    val forecast_date: String,       // "YYYY-MM-DD", one row per month
    val predicted_value: String,     // NUMERIC comes through as a JSON string — see Order.total_amount
    val actual_value: String?,       // null = a future month, not reached yet
    val model_used: String,
    val generated_at: String
)

data class SalesForecastRunResponse(
    val message: String,
    val script_output: String
)

// GET api/sales-forecast/actuals — tells the app which month to ask for next.
data class ActualsStatus(
    val history_end: String,         // "YYYY-MM-DD", last month with known sales
    val next_month: String,          // "YYYY-MM-DD", the only month that may be entered next
    val can_enter: Boolean,          // false while next_month hasn't finished yet
    val entered: List<EnteredMonth>
)

data class EnteredMonth(
    val month: String,
    val amount: String               // NUMERIC -> JSON string, same as Order.total_amount
)

data class AddActualRequest(
    val month: String,
    val amount: Double
)
