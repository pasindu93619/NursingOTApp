package com.pasindu.nursingotapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.logic.CalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AdvancedFinanceUiState(
    val isLoading: Boolean = true,
    val profile: ProfileEntity? = null,
    val claimPeriod: ClaimPeriodEntity? = null,
    val periodSummary: PeriodSummary? = null,
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
        get() = profile?.otRate ?: 0.0

    val totalNormalHours: Double
        get() = periodSummary?.totalNormalHours?.toDouble() ?: 0.0

    val totalOTHours: Double
        get() = periodSummary?.totalOTHours?.toDouble() ?: 0.0

    val totalPHDays: Int
        get() = periodSummary?.totalPHDays ?: 0

    val totalDODays: Int
        get() = periodSummary?.totalDODays ?: 0

    val otAmountRs: Double
        get() = periodSummary?.otAmountRs?.toDouble() ?: 0.0

    val phAmountRs: Double
        get() = periodSummary?.phAmountRs?.toDouble() ?: 0.0

    val doAmountRs: Double
        get() = periodSummary?.doAmountRs?.toDouble() ?: 0.0

    val grossEarnings: Double
        get() = basicSalary +
                otAmountRs +
                phAmountRs +
                doAmountRs

    val totalDeductions: Double
        get() = apit +
                wop +
                loanDeduction +
                otherDeduction

    val estimatedNetSalary: Double
        get() = grossEarnings - totalDeductions

    val dutyProgress36Hours: Float
        get() = if (totalNormalHours <= 0.0) {
            0f
        } else {
            (totalNormalHours / 36.0)
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
}

class AdvancedFinanceViewModel(
    private val profileDao: ProfileDao,
    private val claimPeriodDao: ClaimPeriodDao,
    private val dailyEntryDao: DailyEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdvancedFinanceUiState()
    )

    val uiState: StateFlow<AdvancedFinanceUiState> =
        _uiState.asStateFlow()

    init {
        observeProfile()
        observeClaimPeriod()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileDao.observeProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile
                )
                recalculate()
            }
        }
    }

    private fun observeClaimPeriod() {
        viewModelScope.launch {
            claimPeriodDao.observeClaimPeriods().collect { periods ->
                val currentPeriod = periods.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    claimPeriod = currentPeriod
                )

                recalculate()
            }
        }
    }

    private suspend fun recalculate() {
        val profile = _uiState.value.profile
        val claimPeriod = _uiState.value.claimPeriod

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
            dailyEntryDao
                .observeEntriesForPeriod(claimPeriod.id)
                .collect { entries ->

                    calculateSummary(
                        profile = profile,
                        claimPeriod = claimPeriod,
                        entries = entries
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
        entries: List<DailyEntryEntity>
    ) {
        try {
            val result = CalculationEngine.processClaimData(
                profileEntity = profile,
                entries = entries,
                claimStart = claimPeriod.startDate,
                claimEnd = claimPeriod.endDate
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
        viewModelScope.launch {
            recalculate()
        }
    }

    fun updateApit(value: String) {
        _uiState.value = _uiState.value.copy(
            apit = parseMoney(value)
        )
    }

    fun updateWop(value: String) {
        _uiState.value = _uiState.value.copy(
            wop = parseMoney(value)
        )
    }

    fun updateLoanDeduction(value: String) {
        _uiState.value = _uiState.value.copy(
            loanDeduction = parseMoney(value)
        )
    }

    fun updateOtherDeduction(value: String) {
        _uiState.value = _uiState.value.copy(
            otherDeduction = parseMoney(value)
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

    private fun parseMoney(value: String): Double {
        return value
            .trim()
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
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                AdvancedFinanceViewModel::class.java
            )
        ) {
            val database =
                DatabaseProvider.getDatabase(context)

            return AdvancedFinanceViewModel(
                profileDao = database.profileDao(),
                claimPeriodDao = database.claimPeriodDao(),
                dailyEntryDao = database.dailyEntryDao()
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}