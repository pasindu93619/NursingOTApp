// com/pasindu/nursingotapp/data/local/entity/ClaimPeriodEntity.kt
package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "claim_period")
data class ClaimPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long,
    val wardType: String = "Normal" // NEW: Database now remembers the Ward Type!
)