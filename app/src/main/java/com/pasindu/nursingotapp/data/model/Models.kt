// com/pasindu/nursingotapp/data/model/Models.kt
package com.pasindu.nursingotapp.data.model

import com.pasindu.nursingotapp.domain.model.DailyLog as DomainDailyLog
import java.time.LocalDate

/**
 * Compatibility alias for existing callers while DailyLog ownership moves to the domain layer.
 * New calculation/domain code should import com.pasindu.nursingotapp.domain.model.DailyLog.
 */
@Deprecated(
    message = "Use com.pasindu.nursingotapp.domain.model.DailyLog",
    replaceWith = ReplaceWith("DailyLog", "com.pasindu.nursingotapp.domain.model.DailyLog")
)
typealias DailyLog = DomainDailyLog

data class PeriodSummary(
    val totalNormalHours: Float = 0f,
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
