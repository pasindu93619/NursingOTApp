package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import java.time.LocalDate

/**
 * Presentation-safe wrapper around the authoritative deterministic OT engine.
 * The use case deliberately keeps all rates at zero because Daily Entry displays
 * hours only; monetary calculations remain owned by the finance layer.
 */
class CalculateDailyEntryHoursUseCase {
    operator fun invoke(
        logs: List<DailyLog>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): DailyEntryHours {
        val result = WeeklyOtCalculator.calculate(
            logs = logs,
            claimStart = claimStart,
            claimEnd = claimEnd,
            otRate = 0.0,
            dayRate = 0.0,
            doRate = 0.0
        )
        return DailyEntryHours(
            totalNormalHours = result.totalNormalHours.toFloat(),
            totalOtHours = result.totalOtHours.toFloat()
        )
    }
}

data class DailyEntryHours(
    val totalNormalHours: Float,
    val totalOtHours: Float
)
