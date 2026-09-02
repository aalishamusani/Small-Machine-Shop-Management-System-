package com.smsms.app.model

data class QualityLog(
    val log_id: Int,
    val batch_id: Int,
    val logged_by: Int,
    val check_date: String,
    val qty_checked: Int,
    val defect_count: Int,
    val pass_fail: String, // always server-derived — see backend qualityLogs.controller.js
    val created_at: String
)

// Note: no pass_fail field here — the backend computes it from
// defect_count and would ignore one sent from the client anyway.
data class CreateQualityLogRequest(
    val logged_by: Int,
    val qty_checked: Int,
    val defect_count: Int
)
