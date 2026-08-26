package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.repository.NurseCommandCenterRepository
import com.pasindu.nursingotapp.domain.model.NurseCommandCenterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Live orchestration layer for the Nurse Command Center.
 * Room stays behind the repository; Compose only observes state.
 */
class NurseCommandCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NurseCommandCenterRepository(
        DatabaseProvider.getDatabase(application)
    )

    private val _state = MutableStateFlow(NurseCommandCenterState())
    val state: StateFlow<NurseCommandCenterState> = _state.asStateFlow()

    init {
        observeRepository()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.observeSnapshot()
                .map { snapshot ->
                    val profile = snapshot.profile
                    val cpdTarget = 10
                    val wellnessScore = calculateWellnessScore(
                        dutyHours = snapshot.dutyHours,
                        otHours = snapshot.otHours,
                        pendingTasks = snapshot.pendingClinicalTasks
                    )

                    NurseCommandCenterState(
                        nurseName = profile?.fullName?.ifBlank { "Nursing Officer" }
                            ?: "Nursing Officer",
                        unitName = profile?.unit.orEmpty(),
                        dutyHoursThisMonth = snapshot.dutyHours,
                        otHoursThisMonth = snapshot.otHours,
                        phHoursThisMonth = snapshot.phHours,
                        estimatedGrossSalary = snapshot.grossSalary,
                        estimatedNetSalary = snapshot.netSalary,
                        pendingClinicalTasks = snapshot.pendingClinicalTasks,
                        cpdPoints = snapshot.cpdPoints,
                        cpdTarget = cpdTarget,
                        claimCompletedDays = snapshot.claimCompletedDays,
                        claimTotalDays = snapshot.claimTotalDays,
                        wellnessScore = wellnessScore
                    )
                }
                .catch {
                    // Keep the last successful state when a local stream fails.
                }
                .collect { snapshot ->
                    _state.value = snapshot
                }
        }
    }

    /**
     * Stage-one workload indicator.
     * This is deliberately a transparent heuristic, not a clinical diagnosis.
     * Later it can consume richer workload/recovery signals.
     */
    private fun calculateWellnessScore(
        dutyHours: Double,
        otHours: Double,
        pendingTasks: Int
    ): Int {
        val dutyPenalty = (dutyHours / 220.0 * 25.0).coerceAtMost(25.0)
        val otPenalty = (otHours / 50.0 * 35.0).coerceAtMost(35.0)
        val taskPenalty = (pendingTasks * 4.0).coerceAtMost(20.0)

        return (100.0 - dutyPenalty - otPenalty - taskPenalty)
            .toInt()
            .coerceIn(0, 100)
    }

    /** Kept for future modules that need to push a derived profile snapshot. */
    fun updateProfile(
        name: String,
        unitName: String,
        basicSalary: Double,
        otRate: Double
    ) {
        _state.value = _state.value.copy(
            nurseName = name.ifBlank { "Nursing Officer" },
            unitName = unitName,
            estimatedGrossSalary = basicSalary.coerceAtLeast(0.0),
            estimatedNetSalary = basicSalary.coerceAtLeast(0.0)
        )
    }

    fun updateWellnessScore(score: Int) {
        _state.value = _state.value.copy(
            wellnessScore = score.coerceIn(0, 100)
        )
    }
}
