package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import javax.inject.Inject

/**
 * Shared pay-rate resolver for PH/DO.
 *
 * Implements Phase 4, Step 4.1 from NURSINGOT_FIX_PLAN.md:
 * - Derives authoritative PH and DO day rates from the nurse's grade and 2026 basic salary,
 *   matching the corresponding 2027 basic salary row and dividing by 30.
 * - Keeps persisted pay rate settings in sync via [ApplyMatched2027DayRateUseCase].
 * - Returns null when there is no matching salary step, preventing silent fallback to incorrect rates.
 * - Does NOT alter or derive the user-configured OT rate.
 */
class PayRateResolver @Inject constructor(
    private val matchSalaryStep: MatchSalaryStepUseCase,
    private val applyDayRate: ApplyMatched2027DayRateUseCase
) {
    data class Rates(val phRate: Double, val doRate: Double, val basis2027: Double)

    suspend fun resolve(profile: ProfileEntity): Rates? {
        val row = matchSalaryStep(profile.grade, profile.basicSalary) ?: return null
        val dayRate = row.basicSalary2027 / 30.0
        applyDayRate(row.basicSalary2027) // keep persisted settings in sync
        return Rates(dayRate, dayRate, row.basicSalary2027)
    }
}
