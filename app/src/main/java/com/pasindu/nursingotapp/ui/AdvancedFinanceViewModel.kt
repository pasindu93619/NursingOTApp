package com.pasindu.nursingotapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.logic.CalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AdvancedFinanceUiState(
    val isLoading: Boolean = true,
    val profile: ProfileEntity? = null,
    val claimPeriod: ClaimPeriodEntity? = null,
    val periodSummary: PeriodSummary? = null,
    val payRateSettings: PayRateSettingsEntity? = null,
    val compensation: ProfileCompensationEntity? = null,
    val claimStart: LocalDate? = null,
    val claimEnd: LocalDate? = null,
    val apit: Double = 0.0,
    val wop: Double = 0.0,
    val loanDeduction: Double = 0.0,
    val otherDeduction: Double = 0.0,
    val errorMessage: String? = null
) {
    val currentBasicSalary: Double get() = profile?.basicSalary ?: 0.0
    val riskAllowance: Double get() = compensation?.riskAllowance ?: 0.0
    val claAllowance: Double get() = compensation?.claAllowance ?: 0.0
    val additionalAllowancesTotal: Double get() = compensation?.additionalAllowancesTotal ?: 0.0
    val paysheetDeductions: Double get() = compensation?.totalDeductions ?: 0.0
    val otRate: Double get() = payRateSettings?.otRate?.coerceAtLeast(0.0) ?: 0.0
    val phRate: Double get() = payRateSettings?.phRate?.coerceAtLeast(0.0) ?: 0.0
    val doRate: Double get() = payRateSettings?.doRate?.coerceAtLeast(0.0) ?: 0.0
    val basisSalary2027: Double? get() = payRateSettings?.basisSalary2027
    val totalNormalHours: Double get() = periodSummary?.totalNormalHours?.toDouble() ?: 0.0
    val totalOTHours: Double get() = periodSummary?.totalOTHours?.toDouble() ?: 0.0
    val totalPHDays: Int get() = periodSummary?.totalPHDays ?: 0
    val totalDODays: Int get() = periodSummary?.totalDODays ?: 0
    val otAmountRs: Double get() = totalOTHours * otRate
    val phAmountRs: Double get() = totalPHDays * phRate
    val doAmountRs: Double get() = totalDODays * doRate
    val grossEarnings: Double get() = currentBasicSalary + riskAllowance + claAllowance + additionalAllowancesTotal + otAmountRs + phAmountRs + doAmountRs
    val estimatedNetSalary: Double get() = grossEarnings - paysheetDeductions
    val dutyProgress36Hours: Float get() = if (totalNormalHours <= 0.0) 0f else (totalNormalHours / 36.0).coerceIn(0.0, 1.0).toFloat()
}

class AdvancedFinanceViewModel(
    private val profileDao: ProfileDao,
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val payRateSettingsDao: PayRateSettingsDao,
    private val profileCompensationDao: ProfileCompensationDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdvancedFinanceUiState())
    val uiState: StateFlow<AdvancedFinanceUiState> = _uiState.asStateFlow()

    init {
        observeProfile(); observeClaimPeriod(); observePayRateSettings(); observeCompensation()
    }

    private fun observeProfile() = viewModelScope.launch {
        profileDao.observeProfile().collect { profile ->
            _uiState.value = _uiState.value.copy(profile = profile)
            ensureManualRateRecordExists()
            recalculate()
        }
    }

    private fun observeClaimPeriod() = viewModelScope.launch {
        claimPeriodDao.observeClaimPeriods().collect { periods ->
            _uiState.value = _uiState.value.copy(claimPeriod = periods.firstOrNull())
            recalculate()
        }
    }

    private fun observePayRateSettings() = viewModelScope.launch {
        payRateSettingsDao.observe().collect { settings ->
            _uiState.value = _uiState.value.copy(payRateSettings = settings)
            recalculate()
        }
    }

    private fun observeCompensation() = viewModelScope.launch {
        profileCompensationDao.observe().collect { compensation ->
            _uiState.value = _uiState.value.copy(compensation = compensation)
        }
    }

    private fun ensureManualRateRecordExists() = viewModelScope.launch {
        if (payRateSettingsDao.observe().first() == null) {
            payRateSettingsDao.upsert(PayRateSettingsEntity(id = 1, rateSource = "MANUAL"))
        }
    }

    private fun recalculate() {
        val profile = _uiState.value.profile
        val claimPeriod = _uiState.value.claimPeriod
        val settings = _uiState.value.payRateSettings
        if (profile == null || claimPeriod == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, errorMessage = null)
            return
        }
        viewModelScope.launch {
            try {
                dailyEntryDao.observeEntriesForPeriod(claimPeriod.id).collect { entries ->
                    try {
                        val result = CalculationEngine.processClaimData(
                            profileEntity = profile,
                            entries = entries,
                            claimStart = claimPeriod.startDate,
                            claimEnd = claimPeriod.endDate,
                            payRates = settings?.let { CalculationEngine.PayRates(it.otRate, it.phRate, it.doRate) }
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            periodSummary = result.second,
                            claimStart = claimPeriod.startDate,
                            claimEnd = claimPeriod.endDate,
                            errorMessage = null
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unable to calculate financial summary.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Unable to load financial information.")
            }
        }
    }

    fun refresh() = recalculate()
    fun updateApit(value: String) { _uiState.value = _uiState.value.copy(apit = parseMoney(value)) }
    fun updateWop(value: String) { _uiState.value = _uiState.value.copy(wop = parseMoney(value)) }
    fun updateLoanDeduction(value: String) { _uiState.value = _uiState.value.copy(loanDeduction = parseMoney(value)) }
    fun updateOtherDeduction(value: String) { _uiState.value = _uiState.value.copy(otherDeduction = parseMoney(value)) }

    fun updateOtRate(value: String) = saveRates(parseMoney(value), _uiState.value.phRate, _uiState.value.doRate, _uiState.value.basisSalary2027)
    fun updatePhRate(value: String) = saveRates(_uiState.value.otRate, parseMoney(value), _uiState.value.doRate, _uiState.value.basisSalary2027)
    fun updateDoRate(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, parseMoney(value), _uiState.value.basisSalary2027)

    fun updateBasisSalary2027(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, _uiState.value.doRate, value.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 })

    fun apply2027DayRateFromSalary(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return
        val dayRate = basisSalary2027 / 30.0
        saveRates(_uiState.value.otRate, dayRate, dayRate, basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    }

    fun saveCompensation(riskAllowance: Double, claAllowance: Double, additionalAllowancesTotal: Double, totalDeductions: Double) = viewModelScope.launch {
        profileCompensationDao.upsert(ProfileCompensationEntity(1, riskAllowance.coerceAtLeast(0.0), claAllowance.coerceAtLeast(0.0), additionalAllowancesTotal.coerceAtLeast(0.0), totalDeductions.coerceAtLeast(0.0), System.currentTimeMillis()))
    }

    private fun saveRates(otRate: Double, phRate: Double, doRate: Double, basisSalary2027: Double?, source: String = "MANUAL") = viewModelScope.launch {
        payRateSettingsDao.upsert(PayRateSettingsEntity(1, otRate.coerceAtLeast(0.0), phRate.coerceAtLeast(0.0), doRate.coerceAtLeast(0.0), source, basisSalary2027, System.currentTimeMillis()))
    }

    private fun parseMoney(value: String): Double = value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdvancedFinanceViewModel::class.java)) {
                val database = DatabaseProvider.getDatabase(context)
                return AdvancedFinanceViewModel(database.profileDao(), database.claimPeriodDao(), database.dailyEntryDao(), database.payRateSettingsDao(), database.profileCompensationDao()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
