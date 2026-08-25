package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FinancialState(
    val netEarnings: Double = 88500.00,
    val apitTax: Double = 3500.00,
    val wopDeduction: Double = 2800.00,
    val historicalBasicSalaries: List<Float> = listOf(45000f, 45000f, 48000f, 48000f, 52000f, 52000f),
    val historicalAllowances: List<Float> = listOf(15000f, 15000f, 16000f, 16000f, 18000f, 18000f),
    val historicalOvertimeEarnings: List<Float> = listOf(22000f, 24000f, 21000f, 26000f, 25000f, 29000f)
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {
    private val _financialState = MutableStateFlow(FinancialState())
    val financialState: StateFlow<FinancialState> = _financialState.asStateFlow()
}