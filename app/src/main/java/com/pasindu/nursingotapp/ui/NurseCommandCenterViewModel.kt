package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * First-stage orchestration layer for the Nurse Command Center.
 *
 * At this stage it exposes safe placeholder values and the profile data already
 * available in NursingOTApp. Later phases can inject Room repositories and
 * calculation engines without changing the HomeScreen contract.
 */
class NurseCommandCenterViewModel : ViewModel() {

    private val _state = MutableStateFlow(NurseCommandCenterState())
    val state: StateFlow<NurseCommandCenterState> = _state.asStateFlow()

    fun updateProfile(
        name: String,
        unitName: String,
        basicSalary: Double,
        otRate: Double
    ) {
        _state.value = _state.value.copy(
            nurseName = name.ifBlank { "Nursing Officer" },
            unitName = unitName,
            estimatedGrossSalary = basicSalary,
            estimatedNetSalary = basicSalary,
        )
    }

    fun updateWorkSnapshot(
        dutyHours: Double,
        otHours: Double,
        phHours: Double,
        claimCompletedDays: Int,
        claimTotalDays: Int
    ) {
        _state.value = _state.value.copy(
            dutyHoursThisMonth = dutyHours.coerceAtLeast(0.0),
            otHoursThisMonth = otHours.coerceAtLeast(0.0),
            phHoursThisMonth = phHours.coerceAtLeast(0.0),
            claimCompletedDays = claimCompletedDays.coerceAtLeast(0),
            claimTotalDays = claimTotalDays.coerceAtLeast(0)
        )
    }

    fun updateProfessionalSnapshot(
        cpdPoints: Int,
        cpdTarget: Int,
        pendingClinicalTasks: Int
    ) {
        _state.value = _state.value.copy(
            cpdPoints = cpdPoints.coerceAtLeast(0),
            cpdTarget = cpdTarget.coerceAtLeast(0),
            pendingClinicalTasks = pendingClinicalTasks.coerceAtLeast(0)
        )
    }

    fun updateFinancialSnapshot(
        grossSalary: Double,
        netSalary: Double
    ) {
        _state.value = _state.value.copy(
            estimatedGrossSalary = grossSalary.coerceAtLeast(0.0),
            estimatedNetSalary = netSalary.coerceAtLeast(0.0)
        )
    }

    fun updateWellnessScore(score: Int) {
        _state.value = _state.value.copy(
            wellnessScore = score.coerceIn(0, 100)
        )
    }
}
