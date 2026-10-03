package com.pasindu.nursingotapp.domain.usecase

import androidx.room.withTransaction
import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileAdditionalAllowanceDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDeductionDao
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileAdditionalAllowanceEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.first

/**
 * Persists the complete profile settings bundle.
 *
 * Save failures are allowed to propagate to the ViewModel; navigation must only
 * happen after every persistence operation below has returned successfully.
 */
class SaveProfileSettingsUseCase(
    private val profileDao: ProfileDao,
    private val compensationDao: ProfileCompensationDao,
    private val additionalAllowanceDao: ProfileAdditionalAllowanceDao,
    private val payRateSettingsDao: PayRateSettingsDao,
    private val claimPeriodDao: ClaimPeriodDao,
    private val profileDeductionDao: ProfileDeductionDao,
    private val database: AppDatabase
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
        require(profile.fullName.isNotBlank()) { "Profile name cannot be empty." }
        require(profile.serviceNo.isNotBlank()) { "Service number cannot be empty." }
        require(profile.grade.isNotBlank()) { "Nursing grade must be selected." }
        require(profile.basicSalary > 0.0) { "Current basic salary must be greater than zero." }
        require(otRate > 0.0) { "A valid nursing-service OT rate is required." }

        database.withTransaction {
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

            additionalAllowanceDao.deleteAll()
            val validAllowances = additionalAllowances
                .filter { it.name.trim().isNotEmpty() && it.amount > 0.0 }
                .map {
                    it.copy(
                        id = 0L,
                        name = it.name.trim(),
                        amount = it.amount.coerceAtLeast(0.0)
                    )
                }
            if (validAllowances.isNotEmpty()) {
                additionalAllowanceDao.upsertAll(validAllowances)
            }

            val claimId = claimPeriodDao.getLatestClaimPeriod()?.id
            if (claimId != null) {
                profileDeductionDao.deleteForClaimPeriod(claimId)
                val validDeductions = deductions
                    .filter { it.name.trim().isNotEmpty() && it.amount >= 0.0 }
                    .map {
                        it.copy(
                            id = 0L,
                            claimPeriodId = claimId,
                            name = it.name.trim(),
                            amount = it.amount.coerceAtLeast(0.0)
                        )
                    }
                if (validDeductions.isNotEmpty()) {
                    profileDeductionDao.upsertAll(validDeductions)
                }
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

            val savedProfile = profileDao.getProfileOnce()
            require(savedProfile == profile) {
                "Profile was not persisted correctly. Please try saving again."
            }

            val savedCompensation = compensationDao.getOnce()
            require(savedCompensation?.riskAllowance == riskAllowance.coerceAtLeast(0.0)) {
                "Risk allowance was not persisted correctly."
            }
            require(savedCompensation.claAllowance == claAllowance.coerceAtLeast(0.0)) {
                "CLA allowance was not persisted correctly."
            }
            require(savedCompensation.totalDeductions == totalDeductions.coerceAtLeast(0.0)) {
                "Paysheet deductions were not persisted correctly."
            }
        }
    }
}