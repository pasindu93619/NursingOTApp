package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Master nurse profile.
 *
 * The current basic salary is stored here because it belongs to the current
 * paysheet/profile. Salary-step and 2027 policy data are kept separately so
 * future policy calculations never overwrite the current salary.
 *
 * legacyOtRate is retained for backwards compatibility with older data paths.
 * Health-sector OT calculations use PayRateSettingsEntity.otRate.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String,
    val serviceNo: String,
    val unit: String,
    val paySheetNo: String,
    val grade: String,
    val basicSalary: Double,
    val otRate: Double,
    val updatedAt: Long,
    val salaryStep: Int? = null
)
