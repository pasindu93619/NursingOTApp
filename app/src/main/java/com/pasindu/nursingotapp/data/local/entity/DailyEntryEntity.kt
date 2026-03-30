// com/pasindu/nursingotapp/data/local/entity/DailyEntryEntity.kt
package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// THIS LINE WAS LIKELY MISSING THE tableName PARAMETER
@Entity(tableName = "daily_entry")
data class DailyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimPeriodId: Long,
    val date: LocalDate,
    val isPH: Boolean,
    val isDO: Boolean,
    val isLeave: Boolean,
    val leaveType: String?,
    val normalTimeIn: String,
    val normalTimeOut: String,
    val normalHours: Float,
    val otTimeIn: String,
    val otTimeOut: String,
    val otHours: Float,
    val wardOverride: String,
    val reason: String
)