package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 2027 salary-step lookup data.
 *
 * This table is intentionally separate from ProfileEntity so the nurse's
 * current paysheet basic salary is never overwritten by a future-policy value.
 */
@Entity(
    tableName = "salary_steps_2027"
)
data class SalaryStep2027Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val grade: String,
    val salaryStep: Int,
    val basicSalary2027: Double,
    val effectiveFrom: String = "2027-01-01",
    val sourceLabel: String = "2027 Government Salary Table"
)
