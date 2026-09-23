package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One named user-entered additional paysheet allowance.
 *
 * Fixed Risk/Responsibility and CLA allowances remain policy values and are not
 * stored here. This table is only for nurse-specific additional allowances.
 */
@Entity(tableName = "profile_additional_allowances")
data class ProfileAdditionalAllowanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
