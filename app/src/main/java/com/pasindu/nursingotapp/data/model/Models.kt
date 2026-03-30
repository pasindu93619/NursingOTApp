// com/pasindu/nursingotapp/data/model/Models.kt
package com.pasindu.nursingotapp.data.model

import java.time.LocalDate

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

data class PeriodSummary(
    val totalNormalHours: Float = 0f, // <-- THIS IS THE MISSING VARIABLE WE ADDED!
    val totalOTHours: Float = 0f,
    val totalPHDays: Int = 0,
    val totalDODays: Int = 0,
    val otAmountRs: Double = 0.0,
    val phAmountRs: Double = 0.0,
    val doAmountRs: Double = 0.0,
    val totalAmountRs: Double = 0.0
)

data class Period(
    val claimStart: LocalDate,
    val claimEnd: LocalDate
)

data class UserProfile(
    val name: String = "",
    val serviceNo: String = "",
    val unit: String = "",
    val paySheetNo: String = "",
    val grade: String = "",
    val basicSalary: Double = 0.0,
    val otRate: Double = 0.0
)