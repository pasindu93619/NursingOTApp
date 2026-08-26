package com.pasindu.nursingotapp.domain.model

/**
 * Shared dashboard snapshot for the future NursingOS command center.
 * Keep this model UI-friendly, but independent from Compose and Room.
 */
data class NurseCommandCenterState(
    val nurseName: String = "Nursing Officer",
    val unitName: String = "",
    val otHoursThisMonth: Double = 0.0,
    val phHoursThisMonth: Double = 0.0,
    val dutyHoursThisMonth: Double = 0.0,
    val claimCompletedDays: Int = 0,
    val claimTotalDays: Int = 0,
    val estimatedNetSalary: Double = 0.0,
    val estimatedGrossSalary: Double = 0.0,
    val cpdPoints: Int = 0,
    val cpdTarget: Int = 10,
    val pendingClinicalTasks: Int = 0,
    val wellnessScore: Int = 100
) {
    val totalWorkedHours: Double
        get() = dutyHoursThisMonth + otHoursThisMonth + phHoursThisMonth

    val claimProgress: Float
        get() = if (claimTotalDays > 0) {
            (claimCompletedDays.toFloat() / claimTotalDays.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val cpdProgress: Float
        get() = if (cpdTarget > 0) {
            (cpdPoints.toFloat() / cpdTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
