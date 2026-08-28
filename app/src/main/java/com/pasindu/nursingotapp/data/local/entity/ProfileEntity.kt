package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Master nurse profile.
 *
 * Current basic salary is kept here. Salary-step / 2027 policy data are kept
 * separately so policy changes never overwrite the nurse's actual current pay.
 *
 * otRate is retained for legacy compatibility; active Health-sector OT rate is
 * stored in PayRateSettingsEntity and is always user-configured.
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
    /** Detected internally from the supplied current basic salary. */
    val salaryStep: Int? = null
)
