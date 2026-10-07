package com.smsms.app.model

// One row of GET api/anomaly-flags: the flag itself plus the quality log it
// was raised for (the backend joins them so no second request is needed).
data class AnomalyFlag(
    val flag_id: Int,
    val quality_log_id: Int,
    val metric_type: String,         // "defect_rate"
    val value: String,               // this log's defect rate, 0.0-1.0 (NUMERIC -> JSON string)
    val threshold: String,           // rate above which a log gets flagged (same for every row of a run)
    val is_outlier: Boolean,
    val batch_id: Int,
    val check_date: String,
    val qty_checked: Int,
    val defect_count: Int
)

data class AnomalyRunResponse(
    val message: String,
    val total_flags: Int,
    val outliers: Int,
    val script_output: String
)
