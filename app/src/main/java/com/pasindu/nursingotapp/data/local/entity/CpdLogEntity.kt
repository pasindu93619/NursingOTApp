package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cpd_logs")
data class CpdLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seminarTitle: String,
    val date: Long,
    val earnedPoints: Int,
    val speakerOrInstitution: String,
    val notes: String
)