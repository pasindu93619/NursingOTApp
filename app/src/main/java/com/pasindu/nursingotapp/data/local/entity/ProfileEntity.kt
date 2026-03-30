package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
