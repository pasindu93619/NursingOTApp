package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-configurable service payment rates.
 *
 * The current basic salary remains stored separately in ProfileEntity.
 *
 * OT rate is entered by the user because the Health-sector OT rate is
 * grade-dependent and must not be derived from the current basic salary.
 *
 * PH/DO rates are also entered by the user for now. From 2027, the app can
 * optionally calculate them from the user's 2027 salary-step basic salary
 * (2027 basic salary / 30) without changing the legacy attendance entities.
 */
@Entity(tableName = "pay_rate_settings")
data class PayRateSettingsEntity(
    @PrimaryKey val id: Int = 1,

    /** Health-sector OT rate entered by the user. */
    val otRate: Double = 0.0,

    /** Public-holiday day rate entered by the user for the current policy period. */
    val phRate: Double = 0.0,

    /** Day-off day rate entered by the user for the current policy period. */
    val doRate: Double = 0.0,

    /**
     * How the rates were obtained.
     * Examples: MANUAL, 2027_BASIC_SALARY_DIV_30
     */
    val rateSource: String = "MANUAL",

    /**
     * Optional 2027 salary-step basic salary used only when the future
     * automatic PH/DO calculation is enabled.
     */
    val basisSalary2027: Double? = null,

    val updatedAt: Long = System.currentTimeMillis()
)
