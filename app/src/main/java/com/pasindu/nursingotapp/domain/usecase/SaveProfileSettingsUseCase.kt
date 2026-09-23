package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileAdditionalAllowanceDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDeductionDao
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.first

/** Atomically persists profile, compensation, and resolved service rates. */
class SaveProfileSettingsUseCase(
    private val profileDao: ProfileDao,
    private val compensationDao: ProfileCompensationDao,
    private val additionalAllowanceDao: ProfileAdditionalAllowanceDao,
    private val payRateSettingsDao: PayRateSettingsDao,
    private val claimPeriodDao: com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao,
    private val profileDeductionDao: ProfileDeductionDao
) {
    suspend operator fun invoke(
        profile: ProfileEntity,
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double,
        deductions: List<ProfileDeductionEntity>,
        additionalAllowances: List<ProfileAdditionalAllowanceEntity>,
        otRate: Double,
        matched2027Basic: Double?
    ) {
        profileDao.upsert(profile)

        compensationDao.upsert(
            ProfileCompensationEntity(
                id = 1,
                riskAllowance = 6850.0,
                claAllowance = 17800.0,
                additionalAllowancesTotal = additionalAllowancesTotal.coerceAtLeast(0.0),
                totalDeductions = totalDeductions.coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
        )

        additionalAllowanceDao.deleteAll()
        additionalAllowances
            .filter { it.name.trim().isNotEmpty() && it.amount > 0.0 }
            .map { it.copy(id = 0L, name = it.name.trim(), amount = it.amount.coerceAtLeast(0.0)) }
            .let { valid ->
                if (valid.isNotEmpty()) additionalAllowanceDao.upsertAll(valid)
            }

        claimPeriodDao.getLatestClaimPeriod()?.id?.let { claimId ->
            profileDeductionDao.deleteForClaimPeriod(claimId)
            deductions
                .filter { it.name.trim().isNotEmpty() && it.amount >= 0.0 }
                .map {
                    it.copy(
                        id = 0L,
                        claimPeriodId = claimId,
                        name = it.name.trim(),
                        amount = it.amount.coerceAtLeast(0.0)
                    )
                }
                .let { valid -> if (valid.isNotEmpty()) profileDeductionDao.upsertAll(valid) }
        }

        val current = payRateSettingsDao.observe().first()
        val basisSalary2027 = matched2027Basic?.takeIf { it > 0.0 }
        val calculatedDayRate = basisSalary2027?.div(30.0)

        payRateSettingsDao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = otRate.coerceAtLeast(0.0),
                phRate = calculatedDayRate ?: current?.phRate ?: 0.0,
                doRate = calculatedDayRate ?: current?.doRate ?: 0.0,
                rateSource = if (calculatedDayRate != null) {
                    "NURSING_GRADE_FIXED_OT_2027_BASIC_SALARY_DIV_30"
                } else {
                    current?.rateSource ?: "NURSING_GRADE_FIXED"
                },
                basisSalary2027 = basisSalary2027 ?: current?.basisSalary2027,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
