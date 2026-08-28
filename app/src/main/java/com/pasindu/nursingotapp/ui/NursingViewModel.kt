// com/pasindu/nursingotapp/ui/NursingViewModel.kt
package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.dao.SalaryStep2027Dao
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.local.entity.SalaryStep2027Entity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

class NursingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    private val profileDao = database.profileDao()
    private val dailyEntryDao = database.dailyEntryDao()
    private val profileCompensationDao: ProfileCompensationDao = database.profileCompensationDao()
    private val salaryStep2027Dao: SalaryStep2027Dao = database.salaryStep2027Dao()
    private val payRateSettingsDao = database.payRateSettingsDao()

    private val _userProfile = MutableStateFlow<ProfileEntity?>(null)
    val userProfile: StateFlow<ProfileEntity?> = _userProfile.asStateFlow()

    private val _profileCompensation = MutableStateFlow<ProfileCompensationEntity?>(null)
    val profileCompensation: StateFlow<ProfileCompensationEntity?> = _profileCompensation.asStateFlow()

    private val _matchedSalary2027 = MutableStateFlow<SalaryStep2027Entity?>(null)
    val matchedSalary2027: StateFlow<SalaryStep2027Entity?> = _matchedSalary2027.asStateFlow()

    private val _configuredOtRate = MutableStateFlow<Double>(0.0)
    val configuredOtRate: StateFlow<Double> = _configuredOtRate.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<DailyEntryEntity>>(emptyList())
    val dailyLogs: StateFlow<List<DailyEntryEntity>> = _dailyLogs.asStateFlow()

    init {
        loadProfile()
        loadProfileCompensation()
        loadOtRate()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileDao.observeProfile().collect { profile ->
                _userProfile.value = profile
                if (profile != null && profile.basicSalary > 0.0 && profile.grade.isNotBlank()) {
                    matchSalaryStep(profile.grade, profile.basicSalary)
                } else {
                    _matchedSalary2027.value = null
                }
            }
        }
    }

    private fun loadProfileCompensation() {
        viewModelScope.launch {
            profileCompensationDao.observe().collect { compensation ->
                _profileCompensation.value = compensation
            }
        }
    }

    private fun loadOtRate() {
        viewModelScope.launch {
            payRateSettingsDao.observe().collect { settings ->
                _configuredOtRate.value = settings?.otRate?.coerceAtLeast(0.0) ?: 0.0
            }
        }
    }

    fun saveProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            profileDao.upsert(profile)
        }
    }

    fun saveProfileCompensation(
        riskAllowance: Double,
        claAllowance: Double,
        additionalAllowancesTotal: Double,
        totalDeductions: Double
    ) {
        viewModelScope.launch {
            profileCompensationDao.upsert(
                ProfileCompensationEntity(
                    id = 1,
                    riskAllowance = riskAllowance.coerceAtLeast(0.0),
                    claAllowance = claAllowance.coerceAtLeast(0.0),
                    additionalAllowancesTotal = additionalAllowancesTotal.coerceAtLeast(0.0),
                    totalDeductions = totalDeductions.coerceAtLeast(0.0),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Saves the user's actual Health-sector OT rate.
     * OT is intentionally independent of salary step/basic salary.
     */
    fun saveOtRate(value: Double) {
        viewModelScope.launch {
            val current = payRateSettingsDao.observe().valueOrNull()
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = value.coerceAtLeast(0.0),
                    phRate = current?.phRate ?: 0.0,
                    doRate = current?.doRate ?: 0.0,
                    rateSource = "MANUAL",
                    basisSalary2027 = current?.basisSalary2027,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveManualDayRates(phRate: Double, doRate: Double) {
        viewModelScope.launch {
            val current = payRateSettingsDao.observe().valueOrNull()
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = current?.otRate ?: 0.0,
                    phRate = phRate.coerceAtLeast(0.0),
                    doRate = doRate.coerceAtLeast(0.0),
                    rateSource = "MANUAL",
                    basisSalary2027 = _matchedSalary2027.value?.basicSalary2027,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Match the user's current basic to the supplied salary table for the
     * selected grade. The user never enters a salary-step number.
     */
    fun matchSalaryStep(grade: String, currentBasicSalary: Double) {
        viewModelScope.launch {
            if (grade.isBlank() || currentBasicSalary <= 0.0) {
                _matchedSalary2027.value = null
                return@launch
            }

            val exact = salaryStep2027Dao.findByCurrentBasic(grade.trim(), currentBasicSalary)
            if (exact != null) {
                _matchedSalary2027.value = exact
                return@launch
            }

            // Graceful matching for tables where the entered paysheet amount
            // differs by a tiny rounding amount. Never invent a different row.
            val rows = salaryStep2027Dao.observeForGrade(grade.trim()).valueOrNull() ?: emptyList()
            _matchedSalary2027.value = rows
                .minByOrNull { abs(it.basicSalary2027 - currentBasicSalary) }
                ?.takeIf { abs(it.basicSalary2027 - currentBasicSalary) < 0.01 }
        }
    }

    fun applyMatched2027DayRate() {
        viewModelScope.launch {
            val matched = _matchedSalary2027.value ?: return@launch
            val dayRate = matched.basicSalary2027 / 30.0
            val current = payRateSettingsDao.observe().valueOrNull()
            payRateSettingsDao.upsert(
                PayRateSettingsEntity(
                    id = 1,
                    otRate = current?.otRate ?: 0.0,
                    phRate = dayRate,
                    doRate = dayRate,
                    rateSource = "2027_BASIC_SALARY_DIV_30",
                    basisSalary2027 = matched.basicSalary2027,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun loadEntriesForClaim(claimPeriodId: Long) {
        viewModelScope.launch {
            dailyEntryDao.observeEntriesForPeriod(claimPeriodId).collect { logs ->
                _dailyLogs.value = logs
            }
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
    ) {
        viewModelScope.launch {
            val entry = DailyEntryEntity(
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
            dailyEntryDao.insertEntry(entry)
        }
    }
}

/** Small helper to read the current value of a StateFlow without collecting. */
private fun <T> kotlinx.coroutines.flow.StateFlow<T?>.valueOrNull(): T? = value
