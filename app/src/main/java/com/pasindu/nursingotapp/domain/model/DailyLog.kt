package com.pasindu.nursingotapp.domain.model

import java.time.LocalDate

/**
 * Domain-facing daily log used by deterministic calculation engines.
 * This model is intentionally independent of Room/database entities.
 */
data class DailyLog(
    val id: Long = 0,
    val date: LocalDate,
    val isPH: Boolean = false,
    val isDO: Boolean = false,
    val isLeave: Boolean = false,
    val leaveType: String? = null,
    val reason: String? = null,
    val wardOverride: String? = null,
    val normalTimeInStr: String = "",
    val normalTimeOutStr: String = "",
    val computedNormalHours: Float = 0f,
    val otTimeInStr: String = "",
    val otTimeOutStr: String = "",
    val computedOtHours: Float = 0f
)
