package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import com.pasindu.nursingotapp.domain.model.DailyLog
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
import com.pasindu.nursingotapp.ui.model.DailyEntryUiModel
import com.pasindu.nursingotapp.ui.state.ViewModelOperationState
import com.pasindu.nursingotapp.ui.state.launchOperation
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

    private val _dailyLogs = MutableStateFlow<List<DailyEntryUiModel>>(emptyList())
    val dailyLogs: StateFlow<List<DailyEntryUiModel>> = _dailyLogs.asStateFlow()

    private val _operationState = MutableStateFlow<ViewModelOperationState>(ViewModelOperationState.Idle)
    val operationState: StateFlow<ViewModelOperationState> = _operationState.asStateFlow()

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

    fun saveProfile(profile: ProfileEntity) = launchOperation(_operationState::set) {
        saveProfileUseCase(profile)
    }

    fun saveProfileCompensation(
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double
    ) = launchOperation(_operationState::set) {
        saveProfileCompensationUseCase(
            riskAllowance,
            claAllowance,
            additionalAllowancesTotal,
            totalDeductions
        )
    }

    fun saveOtRate(value: Double) = launchOperation(_operationState::set) {
        saveOtRateUseCase(value)
    }

    fun saveProfileAndContinue(
        profile: ProfileEntity,
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double,
        otRate: Double,
        matched2027Basic: Double?,
        onSaved: () -> Unit
    ) = launchOperation(_operationState::set) {
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

    fun applyMatched2027DayRate() = launchOperation(_operationState::set) {
        _matchedSalary2027.value?.basicSalary2027?.let { applyMatched2027DayRateUseCase(it) }
    }

    fun matchSalaryStep(grade: String, currentBasicSalary: Double) = launchOperation(_operationState::set) {
        _matchedSalary2027.value = matchSalaryStepUseCase(grade, currentBasicSalary)
    }

    fun loadEntriesForClaim(claimPeriodId: Long) = viewModelScope.launch {
        observeClaimDailyEntriesUseCase(claimPeriodId).collect { entries ->
            _dailyLogs.value = entries.map { it.toUiModel() }
        }
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
    ) = launchOperation(_operationState::set) {
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
        logs: List<DailyLog>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ) = calculateDailyEntryHoursUseCase(logs, claimStart, claimEnd)

    fun calculateSavedDailyEntryHours(
        entries: List<DailyEntryUiModel>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ) = calculateDailyEntryHoursUseCase(
        logs = entries.map { entry ->
            DailyLog(
                id = entry.id,
                date = entry.date,
                isPH = entry.isPH,
                isDO = entry.isDO,
                isLeave = entry.isLeave,
                leaveType = entry.leaveType,
                reason = entry.reason,
                wardOverride = entry.wardOverride,
                normalTimeInStr = entry.normalTimeIn,
                normalTimeOutStr = entry.normalTimeOut,
                computedNormalHours = entry.normalHours,
                otTimeInStr = entry.otTimeIn,
                otTimeOutStr = entry.otTimeOut,
                computedOtHours = entry.otHours
            )
        },
        claimStart = claimStart,
        claimEnd = claimEnd
    )
}

private fun DailyEntryEntity.toUiModel() = DailyEntryUiModel(
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
