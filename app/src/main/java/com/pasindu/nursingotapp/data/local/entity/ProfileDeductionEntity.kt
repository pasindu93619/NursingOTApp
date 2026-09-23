package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Named payroll deductions entered from Profile for one claim period.
 * The claimPeriodId is the existing ClaimPeriodEntity id; no new period model is introduced.
 */
@Entity(tableName = "profile_deductions")
data class ProfileDeductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimPeriodId: Long,
    val name: String,
    val amount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
