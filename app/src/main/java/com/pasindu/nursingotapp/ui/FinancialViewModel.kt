package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Financial dashboard state sourced from the existing Room profile/rate data.
 * Legacy attendance entities are intentionally not modified here.
 */
data class FinancialState(
    val basicSalary: Double = 0.0,
    val otRate: Double = 0.0,
    val phRate: Double = 0.0,
    val doRate: Double = 0.0,
    val riskAllowance: Double = 0.0,
    val claAllowance: Double = 0.0,
    val additionalAllowancesTotal: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val profileName: String = "",
    val profileGrade: String = "",
    // Retained for compatibility with the current dashboard/history UI.
    val netEarnings: Double = 0.0,
    val apitTax: Double = 0.0,
    val wopDeduction: Double = 0.0,
    val historicalBasicSalaries: List<Float> = emptyList(),
    val historicalAllowances: List<Float> = emptyList(),
    val historicalOvertimeEarnings: List<Float> = emptyList()
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    private val profileDao = database.profileDao()
    private val rateDao = database.payRateSettingsDao()
    private val compensationDao = database.profileCompensationDao()

    private val _financialState = MutableStateFlow(FinancialState())
    val financialState: StateFlow<FinancialState> = _financialState.asStateFlow()

    init {
        combine(
            profileDao.observeProfile(),
            rateDao.observe(),
            compensationDao.observe()
        ) { profile, rates, compensation ->
            FinancialState(
                basicSalary = profile?.basicSalary ?: 0.0,
                otRate = rates?.otRate ?: profile?.otRate ?: 0.0,
                phRate = rates?.phRate ?: 0.0,
                doRate = rates?.doRate ?: 0.0,
                riskAllowance = compensation?.riskAllowance ?: 0.0,
                claAllowance = compensation?.claAllowance ?: 0.0,
                additionalAllowancesTotal = compensation?.additionalAllowancesTotal ?: 0.0,
                totalDeductions = compensation?.totalDeductions ?: 0.0,
                profileName = profile?.fullName.orEmpty(),
                profileGrade = profile?.grade.orEmpty()
            )
        }.onEach { state ->
            _financialState.value = state
        }.launchIn(viewModelScope)
    }
}
