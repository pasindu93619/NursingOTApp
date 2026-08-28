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
    val currentBasicSalary: Double
        get() = profile?.basicSalary ?: 0.0

    /**
     * Health-sector OT rate. This MUST come from the user's configured rate;
     * it is not derived from current basic salary.
     */
    val otRate: Double
        get() = payRateSettings?.otRate?.coerceAtLeast(0.0) ?: 0.0

    /**
     * PH rate. For now this is entered by the user.
     * Future policy support can populate it from the 2027 salary-step basic.
     */
    val phRate: Double
        get() = payRateSettings?.phRate?.coerceAtLeast(0.0) ?: 0.0

    /**
     * DO rate. For now this is entered by the user.
     * Future policy support can populate it from the 2027 salary-step basic.
     */
    val doRate: Double
        get() = payRateSettings?.doRate?.coerceAtLeast(0.0) ?: 0.0

    val basisSalary2027: Double?
        get() = payRateSettings?.basisSalary2027

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
        get() = currentBasicSalary + otAmountRs + phAmountRs + doAmountRs

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
                ensureManualRateRecordExists()
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

    /**
     * Creates an empty manual rate record once, without inventing any OT/PH/DO
     * rate from the current basic salary.
     */
    private fun ensureManualRateRecordExists() {
        viewModelScope.launch {
            val current = payRateSettingsDao.observe().first()
            if (current == null) {
                payRateSettingsDao.upsert(
                    PayRateSettingsEntity(
                        id = 1,
                        otRate = 0.0,
                        phRate = 0.0,
                        doRate = 0.0,
                        rateSource = "MANUAL",
                        basisSalary2027 = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun recalculate() {
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

        viewModelScope.launch {
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
        recalculate()
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

    /** Store Health-sector OT rate exactly as entered by the user. */
    fun updateOtRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = parsed,
            phRate = _uiState.value.phRate,
            doRate = _uiState.value.doRate,
            basisSalary2027 = _uiState.value.basisSalary2027
        )
    }

    /** Store current PH rate exactly as entered by the user. */
    fun updatePhRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = _uiState.value.otRate,
            phRate = parsed,
            doRate = _uiState.value.doRate,
            basisSalary2027 = _uiState.value.basisSalary2027
        )
    }

    /** Store current DO rate exactly as entered by the user. */
    fun updateDoRate(value: String) {
        val parsed = parseMoney(value)
        saveRates(
            otRate = _uiState.value.otRate,
            phRate = _uiState.value.phRate,
            doRate = parsed,
            basisSalary2027 = _uiState.value.basisSalary2027
        )
    }

    /**
     * Stores the 2027 salary-step basic salary for future policy calculation.
     * This value is NOT used to change the current basic salary.
     */
    fun updateBasisSalary2027(value: String) {
        val parsed = value.trim()
            .replace(",", "")
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }

        saveRates(
            otRate = _uiState.value.otRate,
            phRate = _uiState.value.phRate,
            doRate = _uiState.value.doRate,
            basisSalary2027 = parsed
        )
    }

    /**
     * Optional future helper. When the 2027 policy becomes active, the UI can
     * call this with the user's 2027 salary-step basic salary to derive PH/DO.
     * OT remains independently user-configured.
     */
    fun apply2027DayRateFromSalary(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return

        val dayRate = basisSalary2027 / 30.0

        saveRates(
            otRate = _uiState.value.otRate,
            phRate = dayRate,
            doRate = dayRate,
            basisSalary2027 = basisSalary2027
        )
    }

    fun clearDeductions() {
        _uiState.value = _uiState.value.copy(
            apit = 0.0,
            wop = 0.0,
            loanDeduction = 0.0,
            otherDeduction = 0.0
        )
    }

    private fun saveRates(
        otRate: Double,
        phRate: Double,
        doRate: Double,
        basisSalary2027: Double?
    ) {
        viewModelScope.launch {
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = otRate.coerceAtLeast(0.0),
                    phRate = phRate.coerceAtLeast(0.0),
                    doRate = doRate.coerceAtLeast(0.0),
                    rateSource = "MANUAL",
                    basisSalary2027 = basisSalary2027,
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