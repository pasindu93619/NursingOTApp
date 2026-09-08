package com.pasindu.nursingotapp.ui.model

import java.time.LocalDate

/**
 * UI-facing representation of a saved daily entry.
 *
 * This keeps Room's DailyEntryEntity out of screen state while preserving
 * the same values and display behavior.
 */
data class DailyEntryUiModel(
    val id: Long = 0L,
    val claimPeriodId: Long,
    val date: LocalDate,
    val isPH: Boolean,
    val isDO: Boolean,
    val isLeave: Boolean,
    val leaveType: String?,
    val normalTimeIn: String,
    val normalTimeOut: String,
    val normalHours: Float,
    val otTimeIn: String,
    val otTimeOut: String,
    val otHours: Float,
    val wardOverride: String,
    val reason: String
)