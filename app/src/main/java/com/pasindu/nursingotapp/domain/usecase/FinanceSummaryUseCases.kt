package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.logic.CalculationEngine
import java.time.LocalDate

/**
 * Domain boundary for the financial calculation already used by the app.
 * The use case does not reimplement payroll rules; CalculationEngine remains
 * the deterministic calculation authority for the finance workflow.
 */
class CalculateFinanceSummaryUseCase {
    operator fun invoke(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRateSettingsEntity?
    ): PeriodSummary {
        require(!claimEnd.isBefore(claimStart)) {
            "Claim end date cannot be before claim start date."
        }

        val result = CalculationEngine.processClaimData(
            profileEntity = profile,
            entries = entries,
            claimStart = claimStart,
            claimEnd = claimEnd,
            payRates = payRates?.let {
                CalculationEngine.PayRates(it.otRate, it.phRate, it.doRate)
            }
        )

        return result.second
    }
}

class EnsureManualPayRateRecordUseCase(
    private val observe: suspend () -> PayRateSettingsEntity?,
    private val save: suspend (PayRateSettingsEntity) -> Unit
) {
    suspend operator fun invoke() {
        if (observe() == null) {
            save(PayRateSettingsEntity(id = 1, rateSource = "MANUAL"))
        }
    }
}
