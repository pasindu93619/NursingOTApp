// com/pasindu/nursingotapp/ui/NursingViewModel.kt
package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ProfileCompensationDao
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class NursingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    private val profileDao = database.profileDao()
    private val dailyEntryDao = database.dailyEntryDao()
    private val profileCompensationDao: ProfileCompensationDao = database.profileCompensationDao()

    private val _userProfile = MutableStateFlow<ProfileEntity?>(null)
    val userProfile: StateFlow<ProfileEntity?> = _userProfile.asStateFlow()

    private val _profileCompensation = MutableStateFlow<ProfileCompensationEntity?>(null)
    val profileCompensation: StateFlow<ProfileCompensationEntity?> = _profileCompensation.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<DailyEntryEntity>>(emptyList())
    val dailyLogs: StateFlow<List<DailyEntryEntity>> = _dailyLogs.asStateFlow()

    init {
        loadProfile()
        loadProfileCompensation()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileDao.observeProfile().collect { profile ->
                _userProfile.value = profile
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

    // FIXED: Changed getEntriesForClaim to observeEntriesForPeriod to match the Database!
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