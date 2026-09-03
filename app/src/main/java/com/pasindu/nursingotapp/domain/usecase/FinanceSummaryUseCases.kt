package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.logic.CalculationEngine
import java.time.LocalDate

/**
 * Domain boundary for finance summary calculation.
 *
 * The existing CalculationEngine remains the legacy adapter and delegates
 * weekly allocation to WeeklyOtCalculator, so this use case does not create
 * a second OT rules engine.
 */
class CalculateFinanceSummaryUseCase {
    operator fun invoke(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRateSettingsEntity?
    ): PeriodSummary {
        return CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = entries,
            claimStart = claimStart,
            claimEnd = claimEnd,
            payRates = payRates?.let { settings ->
                CalculationEngine.PayRates(
                    otRate = settings.otRate.coerceAtLeast(0.0),
                    phRate = settings.phRate.coerceAtLeast(0.0),
                    doRate = settings.doRate.coerceAtLeast(0.0)
                )
            }
        ).second
    }
}
