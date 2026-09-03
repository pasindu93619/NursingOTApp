package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import kotlinx.coroutines.flow.first

/** Atomically persists the profile-related settings needed by the OT flow. */
class SaveProfileSettingsUseCase(
    private val profileDao: ProfileDao,
    private val compensationDao: ProfileCompensationDao,
    private val payRateSettingsDao: PayRateSettingsDao
) {
    suspend operator fun invoke(
        profile: ProfileEntity,
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double,
        otRate: Double,
        matched2027Basic: Double?
    ) {
        profileDao.upsert(profile)
        compensationDao.upsert(
            ProfileCompensationEntity(
                id = 1,
                riskAllowance = riskAllowance.coerceAtLeast(0.0),
                claAllowance = claAllowance.coerceAtLeast(0.0),
                additionalAllowancesTotal = additionalAllowancesTotal.coerceAtLeast(0.0),
                totalDeductions = totalDeductions.coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
        )

        val current = payRateSettingsDao.observe().first()
        val basisSalary2027 = matched2027Basic?.takeIf { it > 0.0 }
        val calculatedDayRate = basisSalary2027?.div(30.0)

        payRateSettingsDao.upsert(
            PayRateSettingsEntity(
                id = 1,
                otRate = otRate.coerceAtLeast(0.0),
                phRate = calculatedDayRate ?: current?.phRate ?: 0.0,
                doRate = calculatedDayRate ?: current?.doRate ?: 0.0,
                rateSource = if (calculatedDayRate != null) "2027_BASIC_SALARY_DIV_30" else current?.rateSource ?: "MANUAL",
                basisSalary2027 = basisSalary2027 ?: current?.basisSalary2027,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
