package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.finance.FinanceCalculationEngine
import com.pasindu.nursingotapp.domain.finance.FinancialSnapshot
import java.time.LocalDate

/** Domain boundary for the authoritative finance calculation. */
class CalculateFinanceSummaryUseCase {
    operator fun invoke(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        payRates: PayRateSettingsEntity?,
        compensation: ProfileCompensationEntity? = null
    ): FinancialSnapshot = FinanceCalculationEngine.calculate(
        profile = profile,
        entries = entries,
        claimStart = claimStart,
        claimEnd = claimEnd,
        payRates = payRates,
        compensation = compensation
    )
}
