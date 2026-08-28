package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-entered monthly paysheet compensation components.
 *
 * Kept separate from ProfileEntity so permanent identity data and
 * month-to-month payroll data do not become coupled.
 */
@Entity(tableName = "profile_compensation")
data class ProfileCompensationEntity(
    @PrimaryKey val id: Int = 1,
    val riskAllowance: Double = 0.0,
    val claAllowance: Double = 0.0,
    val additionalAllowancesTotal: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
