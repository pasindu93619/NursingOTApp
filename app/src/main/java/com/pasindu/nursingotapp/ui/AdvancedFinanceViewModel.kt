package com.pasindu.nursingotapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.PayRateSettingsDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
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
    val claimStart: LocalDate? = null,
    val claimEnd: LocalDate? = null,
    val apit: Double = 0.0,
    val wop: Double = 0.0,
    val loanDeduction: Double = 0.0,
    val otherDeduction: Double = 0.0,
    val errorMessage: String? = null
) {
    val basicSalary: Double
        get() = profile?.basicSalary ?: 0.0

    val otRate: Double
        get() = payRateSettings?.otRate?.takeIf { it > 0.0 }
            ?: profile?.otRate
            ?: 0.0

    val phRate: Double
        get() = payRateSettings?.phRate?.takeIf { it > 0.0 }
            ?: if (basicSalary > 0.0) basicSalary / 30.0 else 0.0

    val doRate: Double
        get() = payRateSettings?.doRate?.takeIf { it > 0.0 }
            ?: if (basicSalary > 0.0) basicSalary / 30.0 else 0.0

    val totalNormalHours: Double
        get() = periodSummary?.totalNormalHours?.toDouble() ?: 0.0

    val totalOTHours: Double
        get() = periodSummary?.totalOTHours?.toDouble() ?: 0.0

    val totalPHDays: Int
        get() = periodSummary?.totalPHDays ?: 0

    val totalDODays: Int
        get() = periodSummary?.totalDODays ?: 0

    val otAmountRs: Double
        get() = totalOTHours * otRate

    val phAmountRs: Double
        get() = totalPHDays * phRate

    val doAmountRs: Double
        get() = totalDODays * doRate

    val grossEarnings: Double
        get() = basicSalary + otAmountRs + phAmountRs + doAmountRs

    val totalDeductions: Double
        get() = apit + wop + loanDeduction + otherDeduction

    val estimatedNetSalary: Double
        get() = grossEarnings - totalDeductions

    val dutyProgress36Hours: Float
        get() = if (totalNormalHours <= 0.0) {
            0f
        } else {
            (totalNormalHours / 36.0).coerceIn(0.0, 1.0).toFloat()
        }
}

class AdvancedFinanceViewModel(
    private val profileDao: ProfileDao,
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val payRateSettingsDao: PayRateSettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedFinanceUiState())
    val uiState: StateFlow<AdvancedFinanceUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeClaimPeriod()
        observePayRateSettings()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileDao.observeProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile)
                syncDefaultPayRates(profile)
                recalculate()
            }
        }
    }

    private fun observeClaimPeriod() {
        viewModelScope.launch {
            claimPeriodDao.observeClaimPeriods().collect { periods ->
                _uiState.value = _uiState.value.copy(
                    claimPeriod = periods.firstOrNull()
                )
                recalculate()
            }
        }
    }

    private fun observePayRateSettings() {
        viewModelScope.launch {
            payRateSettingsDao.observe().collect { settings ->
                _uiState.value = _uiState.value.copy(payRateSettings = settings)
                recalculate()
            }
        }
    }

    private fun syncDefaultPayRates(profile: ProfileEntity?) {
        if (profile == null) return

        viewModelScope.launch {
            val current = payRateSettingsDao.observe().first()
            if (current == null) {
                val dayRate = if (profile.basicSalary > 0.0) {
                    profile.basicSalary / 30.0
                } else {
                    0.0
                }

                payRateSettingsDao.upsert(
                    PayRateSettingsEntity(
                        id = 1,
                        otRate = profile.otRate,
                        phRate = dayRate,
                        doRate = dayRate,
                        rateSource = "BASIC_SALARY_DIV_30",
                        basisSalary2027 = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun recalculate() {
        val profile = _uiState.value.profile
        val claimPeriod = _uiState.value.claimPeriod
        val settings = _uiState.value.payRateSettings

        if (profile == null || claimPeriod == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                periodSummary = null,
                claimStart = null,
                claimEnd = null,
                errorMessage = null
            )
            return
        }

        try {
            dailyEntryDao.observeEntriesForPeriod(claimPeriod.id).collect { entries ->
                calculateSummary(
                    profile = profile,
                    claimPeriod = claimPeriod,
                    entries = entries,
                    settings = settings
                )
            }
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = exception.message
                    ?: "Unable to load financial information."
            )
        }
    }

    private fun calculateSummary(
        profile: ProfileEntity,
        claimPeriod: ClaimPeriodEntity,
        entries: List<DailyEntryEntity>,
        settings: PayRateSettingsEntity?
    ) {
        try {
            val result = CalculationEngine.processClaimData(
                profileEntity = profile,
                entries = entries,
                claimStart = claimPeriod.startDate,
                claimEnd = claimPeriod.endDate,
                payRates = settings?.let {
                    CalculationEngine.PayRates(
                        otRate = it.otRate,
                        phRate = it.phRate,
                        doRate = it.doRate
                    )
                }
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profile = profile,
                claimPeriod = claimPeriod,
                periodSummary = result.second,
                claimStart = claimPeriod.startDate,
                claimEnd = claimPeriod.endDate,
                errorMessage = null
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = exception.message
                    ?: "Unable to calculate financial summary."
            )
        }
    }

    fun refresh() {
        viewModelScope.launch { recalculate() }
    }

    fun updateApit(value: String) {
        _uiState.value = _uiState.value.copy(apit = parseMoney(value))
    }

    fun updateWop(value: String) {
        _uiState.value = _uiState.value.copy(wop = parseMoney(value))
    }

    fun updateLoanDeduction(value: String) {
        _uiState.value = _uiState.value.copy(loanDeduction = parseMoney(value))
    }

    fun updateOtherDeduction(value: String) {
        _uiState.value = _uiState.value.copy(otherDeduction = parseMoney(value))
    }

    fun updateOtRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = parsed,
            phRate = _uiState.value.phRate,
            doRate = _uiState.value.doRate
        )
    }

    fun updatePhRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = _uiState.value.otRate,
            phRate = parsed,
            doRate = _uiState.value.doRate
        )
    }

    fun updateDoRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = _uiState.value.otRate,
            phRate = _uiState.value.phRate,
            doRate = parsed
        )
    }

    fun clearDeductions() {
        _uiState.value = _uiState.value.copy(
            apit = 0.0,
            wop = 0.0,
            loanDeduction = 0.0,
            otherDeductions = 0.0
        )
    }

    private fun saveRates(otRate: Double, phRate: Double, doRate: Double) {
        viewModelScope.launch {
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = otRate,
                    phRate = phRate,
                    doRate = doRate,
                    rateSource = "MANUAL",
                    basisSalary2027 = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun parseMoney(value: String): Double {
        return value.trim()
            .replace(",", "")
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0
    }
}

class AdvancedFinanceViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvancedFinanceViewModel::class.java)) {
            val database = DatabaseProvider.getDatabase(context)
            return AdvancedFinanceViewModel(
                profileDao = database.profileDao(),
                claimPeriodDao = database.claimPeriodDao(),
                dailyEntryDao = database.dailyEntryDao(),
                payRateSettingsDao = database.payRateSettingsDao()
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}