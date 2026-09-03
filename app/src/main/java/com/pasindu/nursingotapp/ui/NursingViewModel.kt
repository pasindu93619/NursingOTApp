package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.domain.usecase.ApplyMatched2027DayRateUseCase
import com.pasindu.nursingotapp.domain.usecase.CalculateDailyEntryHoursUseCase
import com.pasindu.nursingotapp.domain.usecase.GetDailyEntryForDateUseCase
import com.pasindu.nursingotapp.domain.usecase.MatchSalaryStepUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveDailyEntryUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileSettingsUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NursingViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    observeProfileCompensation: ObserveProfileCompensationUseCase,
    observeOtRate: ObserveOtRateUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val saveProfileCompensationUseCase: SaveProfileCompensationUseCase,
    private val saveOtRateUseCase: SaveOtRateUseCase,
    private val saveProfileSettingsUseCase: SaveProfileSettingsUseCase,
    private val matchSalaryStepUseCase: MatchSalaryStepUseCase,
    private val applyMatched2027DayRateUseCase: ApplyMatched2027DayRateUseCase,
    private val observeClaimDailyEntriesUseCase: ObserveClaimDailyEntriesUseCase,
    private val saveDailyEntryUseCase: SaveDailyEntryUseCase,
    private val getDailyEntryForDateUseCase: GetDailyEntryForDateUseCase,
    private val calculateDailyEntryHoursUseCase: CalculateDailyEntryHoursUseCase
) : ViewModel() {

    private val _userProfile = MutableStateFlow<ProfileEntity?>(null)
    val userProfile: StateFlow<ProfileEntity?> = _userProfile.asStateFlow()

    private val _profileCompensation = MutableStateFlow<com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity?>(null)
    val profileCompensation: StateFlow<com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity?> = _profileCompensation.asStateFlow()

    private val _matchedSalary2027 = MutableStateFlow<SalaryStep2027Entity?>(null)
    val matchedSalary2027: StateFlow<SalaryStep2027Entity?> = _matchedSalary2027.asStateFlow()

    private val _configuredOtRate = MutableStateFlow(0.0)
    val configuredOtRate: StateFlow<Double> = _configuredOtRate.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<DailyEntryEntity>>(emptyList())
    val dailyLogs: StateFlow<List<DailyEntryEntity>> = _dailyLogs.asStateFlow()

    init {
        viewModelScope.launch {
            observeProfile().collect { profile -> _userProfile.value = profile }
        }
        viewModelScope.launch {
            observeProfileCompensation().collect { compensation -> _profileCompensation.value = compensation }
        }
        viewModelScope.launch {
            observeOtRate().collect { settings ->
                _configuredOtRate.value = settings?.otRate?.coerceAtLeast(0.0) ?: 0.0
            }
        }
    }

    fun saveProfile(profile: ProfileEntity) = viewModelScope.launch { saveProfileUseCase(profile) }

    fun saveProfileCompensation(
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double
    ) = viewModelScope.launch {
        saveProfileCompensationUseCase(
            riskAllowance,
            claAllowance,
            additionalAllowancesTotal,
            totalDeductions
        )
    }

    fun saveOtRate(value: Double) = viewModelScope.launch { saveOtRateUseCase(value) }

    fun saveProfileAndContinue(
        profile: ProfileEntity,
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double,
        otRate: Double,
        matched2027Basic: Double?,
        onSaved: () -> Unit
    ) = viewModelScope.launch {
        saveProfileSettingsUseCase(
            profile = profile,
            riskAllowance = riskAllowance,
            claAllowance = claAllowance,
            additionalAllowancesTotal = additionalAllowancesTotal,
            totalDeductions = totalDeductions,
            otRate = otRate,
            matched2027Basic = matched2027Basic
        )
        onSaved()
    }

    fun applyMatched2027DayRate() = viewModelScope.launch {
        _matchedSalary2027.value?.basicSalary2027?.let { applyMatched2027DayRateUseCase(it) }
    }

    fun matchSalaryStep(grade: String, currentBasicSalary: Double) = viewModelScope.launch {
        _matchedSalary2027.value = matchSalaryStepUseCase(grade, currentBasicSalary)
    }

    fun loadEntriesForClaim(claimPeriodId: Long) = viewModelScope.launch {
        observeClaimDailyEntriesUseCase(claimPeriodId).collect { logs -> _dailyLogs.value = logs }
    }

    fun saveDailyEntry(
        id: Long = 0L,
        claimPeriodId: Long,
        date: LocalDate,
        isPH: Boolean,
        isDO: Boolean,
        isLeave: Boolean,
        leaveType: String?,
        normalTimeIn: String,
        normalTimeOut: String,
        normalHours: Float,
        otTimeIn: String,
        otTimeOut: String,
        otHours: Float,
        wardOverride: String,
        reason: String
    ) = viewModelScope.launch {
        saveDailyEntryUseCase(
            DailyEntryEntity(
                id = id,
                claimPeriodId = claimPeriodId,
                date = date,
                isPH = isPH,
                isDO = isDO,
                isLeave = isLeave,
                leaveType = leaveType,
                normalTimeIn = normalTimeIn,
                normalTimeOut = normalTimeOut,
                normalHours = normalHours,
                otTimeIn = otTimeIn,
                otTimeOut = otTimeOut,
                otHours = otHours,
                wardOverride = wardOverride,
                reason = reason
            )
        )
    }

    suspend fun getDailyEntryForDate(claimPeriodId: Long, date: LocalDate): DailyEntryEntity? =
        getDailyEntryForDateUseCase(claimPeriodId, date)

    fun calculateDailyEntryHours(
        logs: List<com.pasindu.nursingotapp.data.model.DailyLog>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ) = calculateDailyEntryHoursUseCase(logs, claimStart, claimEnd)
}
