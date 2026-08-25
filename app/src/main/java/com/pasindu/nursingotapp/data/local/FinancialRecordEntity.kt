package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_records")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Temporal Data for the Vico Charting Library
    val recordMonth: String, // Format: "YYYY-MM" (e.g., "2026-08")
    val timestamp: Long = System.currentTimeMillis(),

    // Core Salary Inputs
    val basicSalary: Double,
    val otRate: Double,
    val otHours: Double,
    val phDays: Double,
    val doDays: Double,

    // Advanced Financial Management Fields
    val wopDeduction: Double, // Widows' and Orphans' Pension scheme
    val apitTaxAmount: Double, // Advance Personal Income Tax
    val loanDeduction: Double, // State bank/distress loans
    val otherDeductions: Double,

    // Final Calculated Engine Outputs
    val totalHoursWorked: Double,
    val grossSalary: Double,
    val netSalary: Double
)