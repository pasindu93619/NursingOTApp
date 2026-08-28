package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 2026/2027 salary-step lookup data.
 *
 * currentBasicSalary2026 is the paid/basic amount used to identify the
 * nurse's current salary row. basicSalary2027 is the 2027 paid/basic amount
 * used as the PH/DO calculation basis.
 */
@Entity(tableName = "salary_steps_2027")
data class SalaryStep2027Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val grade: String,
    val salaryStep: Int,
    val currentBasicSalary2026: Double,
    val basicSalary2027: Double,
    val effectiveFrom: String = "2027-01-01",
    val sourceLabel: String = "2027 Government Salary Table"
)
