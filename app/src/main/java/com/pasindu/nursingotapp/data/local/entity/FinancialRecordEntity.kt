package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_records")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthYear: String, // e.g., "06-2026"
    val basicSalary: Double,
    val totalAllowance: Double,
    val calculatedOtAmount: Double,
    val apitTaxDeduction: Double,
    val wopPensionDeduction: Double,
    val loanDeduction: Double,
    val netSalary: Double
)