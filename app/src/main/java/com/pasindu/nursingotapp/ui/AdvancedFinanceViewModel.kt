package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.usecase.CalculateFinanceSummaryUseCase
import com.pasindu.nursingotapp.domain.usecase.EnsureManualPayRateRecordUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceRatesUseCase
import com.pasindu.nursingotapp.domain.usecase.SynchronizePolicyRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

data class AdvancedFinanceUiState(
    val isLoading: Boolean = true,
    val profile: ProfileEntity? = null,
    val claimPeriod: ClaimPeriodEntity? = null,
    val periodSummary: PeriodSummary? = null,
    val payRateSettings: PayRateSettingsEntity? = null,
    val compensation: ProfileCompensationEntity? = null,
    val claimStart: java.time.LocalDate? = null,
    val claimEnd: java.time.LocalDate? = null,
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

@HiltViewModel
class AdvancedFinanceViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    observeClaimPeriod: ObserveClaimPeriodsUseCase,
    observePayRates: ObserveOtRateUseCase,
    observeCompensation: ObserveProfileCompensationUseCase,
    private val ensureManualPayRateRecordUseCase: EnsureManualPayRateRecordUseCase,
    private val synchronizePolicyRatesUseCase: SynchronizePolicyRatesUseCase,
    private val observeClaimDailyEntriesUseCase: ObserveClaimDailyEntriesUseCase,
    private val calculateFinanceSummaryUseCase: CalculateFinanceSummaryUseCase,
    private val saveFinanceCompensationUseCase: SaveFinanceCompensationUseCase,
    private val saveFinanceRatesUseCase: SaveFinanceRatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedFinanceUiState())
    val uiState: StateFlow<AdvancedFinanceUiState> = _uiState.asStateFlow()

    init {
        observeProfile().collectInViewModel { profile ->
            _uiState.value = _uiState.value.copy(profile = profile)
            viewModelScope.launch {
                ensureManualPayRateRecordUseCase()
                if (profile != null) synchronizePolicyRatesUseCase(profile)
                recalculate()
            }
        }
        observeClaimPeriod().collectInViewModel { periods ->
            _uiState.value = _uiState.value.copy(claimPeriod = periods.firstOrNull())
            recalculate()
        }
        observePayRates().collectInViewModel { settings ->
            _uiState.value = _uiState.value.copy(payRateSettings = settings)
            recalculate()
        }
        observeCompensation().collectInViewModel { compensation ->
            _uiState.value = _uiState.value.copy(compensation = compensation)
            recalculate()
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val profile = state.profile ?: run {
            _uiState.value = state.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, errorMessage = null)
            return
        }
        val claimPeriod = state.claimPeriod ?: run {
            _uiState.value = state.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, errorMessage = null)
            return
        }

        viewModelScope.launch {
            runCatching {
                observeClaimDailyEntriesUseCase(claimPeriod.id).first()
            }.onSuccess { entries ->
                runCatching {
                    calculateFinanceSummaryUseCase(
                        profile = profile,
                        entries = entries,
                        claimStart = claimPeriod.startDate,
                        claimEnd = claimPeriod.endDate,
                        payRates = state.payRateSettings
                    )
                }.onSuccess { summary ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        periodSummary = summary,
                        claimStart = claimPeriod.startDate,
                        claimEnd = claimPeriod.endDate,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message ?: "Unable to calculate financial summary.")
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message ?: "Unable to load financial information.")
            }
        }
    }

    fun refresh() = recalculate()
    fun updateApit(value: String) { _uiState.value = _uiState.value.copy(apit = parseMoney(value)) }
    fun updateWop(value: String) { _uiState.value = _uiState.value.copy(wop = parseMoney(value)) }
    fun updateLoanDeduction(value: String) { _uiState.value = _uiState.value.copy(loanDeduction = parseMoney(value)) }
    fun updateOtherDeduction(value: String) { _uiState.value = _uiState.value.copy(otherDeduction = parseMoney(value)) }

    fun updateOtRate(value: String) = saveRates(parseMoney(value), _uiState.value.phRate, _uiState.value.doRate, _uiState.value.basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    fun updatePhRate(value: String) = saveRates(_uiState.value.otRate, parseMoney(value), _uiState.value.doRate, _uiState.value.basisSalary2027, "MANUAL")
    fun updateDoRate(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, parseMoney(value), _uiState.value.basisSalary2027, "MANUAL")
    fun updateBasisSalary2027(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, _uiState.value.doRate, value.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }, _uiState.value.payRateSettings?.rateSource ?: "MANUAL")

    fun apply2027DayRateFromSalary(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return
        val dayRate = basisSalary2027 / 30.0
        saveRates(_uiState.value.otRate, dayRate, dayRate, basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    }

    fun saveCompensation(riskAllowance: Double, claAllowance: Double, additionalAllowancesTotal: Double, totalDeductions: Double) {
        viewModelScope.launch {
            saveFinanceCompensationUseCase(riskAllowance, claAllowance, additionalAllowancesTotal, totalDeductions)
        }
    }

    private fun saveRates(otRate: Double, phRate: Double, doRate: Double, basisSalary2027: Double?, source: String) {
        viewModelScope.launch {
            saveFinanceRatesUseCase(otRate, phRate, doRate, basisSalary2027, source)
        }
    }

    private fun parseMoney(value: String): Double = value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    private fun <T> kotlinx.coroutines.flow.Flow<T>.collectInViewModel(block: (T) -> Unit) {
        viewModelScope.launch { collect { block(it) } }
    }
}
