package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Master nurse profile.
 *
 * The current basic salary is stored here because it is part of the user's
 * current paysheet/profile information. Service-payment rates are kept in
 * PayRateSettingsEntity so Health-sector OT and future 2027 PH/DO policy
 * changes do not alter the legacy profile model.
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
    val updatedAt: Long
)
