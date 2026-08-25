package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.FinancialDao
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import com.pasindu.nursingotapp.domain.calculation.FinancialCalculationEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val financialDao: FinancialDao = DatabaseProvider.getDatabase(application).financialDao()

    val financialRecords: StateFlow<List<FinancialRecordEntity>> = financialDao.getAllFinancialRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMonthlyRecord(
        monthYear: String,
        basicSalary: Double,
        totalAllowance: Double,
        calculatedOtAmount: Double,
        loanPrincipal: Double,
        loanRate: Double,
        loanYears: Int,
        wopRate: Double
    ) {
        viewModelScope.launch {
            val totalIncome = basicSalary + totalAllowance + calculatedOtAmount
            val apit = FinancialCalculationEngine.calculateApitTax(totalIncome)
            val wop = FinancialCalculationEngine.calculateWopDeduction(basicSalary, wopRate)
            val loan = FinancialCalculationEngine.calculateLoanAmortization(loanPrincipal, loanRate, loanYears)
            val net = totalIncome - (apit + wop + loan)

            val record = FinancialRecordEntity(
                monthYear = monthYear,
                basicSalary = basicSalary,
                totalAllowance = totalAllowance,
                calculatedOtAmount = calculatedOtAmount,
                apitTaxDeduction = apit,
                wopPensionDeduction = wop,
                loanDeduction = loan,
                netSalary = net
            )
            financialDao.insertFinancialRecord(record)
        }
    }
}