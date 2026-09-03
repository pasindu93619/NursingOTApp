package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.calculation.CalculationEngine

class CalculateFinanceSummaryUseCase {
    operator fun invoke(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: java.time.LocalDate,
        claimEnd: java.time.LocalDate,
        payRates: PayRateSettingsEntity?
    ): PeriodSummary {
        return CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = entries,
            claimStart = claimStart,
            claimEnd = claimEnd,
            payRates = payRates?.let {
                CalculationEngine.PayRates(
                    otRate = it.otRate.coerceAtLeast(0.0),
                    phRate = it.phRate.coerceAtLeast(0.0),
                    doRate = it.doRate.coerceAtLeast(0.0)
                )
            }
        ).second
    }
}
