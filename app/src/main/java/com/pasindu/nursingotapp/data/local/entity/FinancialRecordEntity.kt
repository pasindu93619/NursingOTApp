package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_records")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val recordMonth: String,
    val timestamp: Long = System.currentTimeMillis(),

    val basicSalary: Double,
    val otRate: Double,
    val otHours: Double,
    val phDays: Double,
    val doDays: Double,

    val wopDeduction: Double,
    val apitTaxAmount: Double,
    val loanDeduction: Double,
    val otherDeductions: Double,

    val totalHoursWorked: Double,
    val grossSalary: Double,
    val netSalary: Double
)