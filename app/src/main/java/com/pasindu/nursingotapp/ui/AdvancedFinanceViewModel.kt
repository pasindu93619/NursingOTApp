package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.logic.CalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

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

@HiltViewModel
class AdvancedFinanceViewModel @Inject constructor(
    private val profileDao: ProfileDao,
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val payRateSettingsDao: PayRateSettingsDao,
    private val profileCompensationDao: ProfileCompensationDao,
    private val salaryStep2027Dao: SalaryStep2027Dao
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdvancedFinanceUiState())
    val uiState: StateFlow<AdvancedFinanceUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeClaimPeriod()
        observePayRateSettings()
        observeCompensation()
    }

    private fun observeProfile() = viewModelScope.launch {
        profileDao.observeProfile().collect { profile ->
            _uiState.value = _uiState.value.copy(profile = profile)
            ensureManualRateRecordExists()
            if (profile != null) synchronizePolicyRates(profile)
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
            recalculate()
        }
    }

    private fun ensureManualRateRecordExists() = viewModelScope.launch {
        if (payRateSettingsDao.observe().first() == null) {
            payRateSettingsDao.upsert(PayRateSettingsEntity(id = 1, rateSource = "MANUAL"))
        }
    }

    /**
     * Uses the stored current 2026 basic + grade to find the authoritative
     * 2027 paid/basic row. PH and DO are both calculated from that 2027 basis.
     * OT remains the independently entered Health-sector rate.
     */
    private fun synchronizePolicyRates(profile: ProfileEntity) = viewModelScope.launch {
        val current = payRateSettingsDao.observe().first()
        val grade = normalizeGrade(profile.grade)
        val rows = salaryStep2027Dao.observeForGrade(grade).first()
        val matched = rows.firstOrNull { abs(it.currentBasicSalary2026 - profile.basicSalary) < 0.01 }
            ?: return@launch

        val dayRate = matched.basicSalary2027 / 30.0
        val existingOtRate = current?.otRate ?: 0.0
        val alreadyCorrect = current?.basisSalary2027 == matched.basicSalary2027 &&
            abs(current.phRate - dayRate) < 0.01 &&
            abs(current.doRate - dayRate) < 0.01 &&
            current.rateSource == "2027_BASIC_SALARY_DIV_30"

        if (!alreadyCorrect) {
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = existingOtRate.coerceAtLeast(0.0),
                    phRate = dayRate,
                    doRate = dayRate,
                    rateSource = "2027_BASIC_SALARY_DIV_30",
                    basisSalary2027 = matched.basicSalary2027,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val profile = state.profile
        val claimPeriod = state.claimPeriod
        val settings = state.payRateSettings
        if (profile == null || claimPeriod == null) {
            _uiState.value = state.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, errorMessage = null)
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

    fun updateOtRate(value: String) = saveRates(parseMoney(value), _uiState.value.phRate, _uiState.value.doRate, _uiState.value.basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    fun updatePhRate(value: String) = saveRates(_uiState.value.otRate, parseMoney(value), _uiState.value.doRate, _uiState.value.basisSalary2027, "MANUAL")
    fun updateDoRate(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, parseMoney(value), _uiState.value.basisSalary2027, "MANUAL")
    fun updateBasisSalary2027(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, _uiState.value.doRate, value.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }, _uiState.value.payRateSettings?.rateSource ?: "MANUAL")

    fun apply2027DayRateFromSalary(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return
        val dayRate = basisSalary2027 / 30.0
        saveRates(_uiState.value.otRate, dayRate, dayRate, basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    }

    fun saveCompensation(riskAllowance: Double, claAllowance: Double, additionalAllowancesTotal: Double, totalDeductions: Double) = viewModelScope.launch {
        profileCompensationDao.upsert(ProfileCompensationEntity(1, riskAllowance.coerceAtLeast(0.0), claAllowance.coerceAtLeast(0.0), additionalAllowancesTotal.coerceAtLeast(0.0), totalDeductions.coerceAtLeast(0.0), System.currentTimeMillis()))
    }

    private fun saveRates(otRate: Double, phRate: Double, doRate: Double, basisSalary2027: Double?, source: String) = viewModelScope.launch {
        payRateSettingsDao.upsert(PayRateSettingsEntity(1, otRate.coerceAtLeast(0.0), phRate.coerceAtLeast(0.0), doRate.coerceAtLeast(0.0), source, basisSalary2027, System.currentTimeMillis()))
    }

    private fun parseMoney(value: String): Double = value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    private fun normalizeGrade(value: String): String {
        val cleaned = value.trim().uppercase().replace("GRADE", "").trim()
        return when {
            cleaned == "1" || cleaned == "I" -> "I"
            cleaned == "2" || cleaned == "II" -> "II"
            cleaned == "3" || cleaned == "III" -> "III"
            else -> value.trim().uppercase()
        }
    }
}
