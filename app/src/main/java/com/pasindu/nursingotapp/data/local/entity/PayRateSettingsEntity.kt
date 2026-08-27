package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Policy/pay-rate settings kept separate from the legacy ProfileEntity.
 *
 * Current defaults:
 * - OT rate is copied from the profile's configured OT rate.
 * - PH and working-DO rates default to basic salary / 30.
 *
 * Later, these fields can be switched to official grade-specific or 2027
 * salary-basis rates without changing DailyEntryEntity or ClaimPeriodEntity.
 */
@Entity(tableName = "pay_rate_settings")
data class PayRateSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val otRate: Double = 0.0,
    val phRate: Double = 0.0,
    val doRate: Double = 0.0,
    val rateSource: String = "BASIC_SALARY_DIV_30",
    val basisSalary2027: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
