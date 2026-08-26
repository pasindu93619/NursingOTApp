package com.pasindu.nursingotapp.domain.model

/**
 * Shared dashboard snapshot for the NursingOS command center.
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

    /**
     * Destination route for the most useful action.
     * Kept deterministic so the UI remains simple and testable.
     */
    val insightRoute: String
        get() = when {
            pendingClinicalTasks >= 5 -> "clinical_planning"
            wellnessScore < 55 -> "care_pulse"
            otHoursThisMonth >= 40.0 -> "advanced_finance_hub"
            estimatedNetSalary > 0.0 && estimatedGrossSalary > 0.0 &&
                estimatedNetSalary / estimatedGrossSalary < 0.70 -> "advanced_finance_hub"
            claimTotalDays > 0 && claimProgress < 0.50f -> "claim_period"
            cpdTarget > 0 && cpdProgress < 0.30f -> "knowledge_hub"
            else -> "analytics"
        }

    /**
     * Human-readable next action generated from the current snapshot.
     * This is intentionally a simple transparent rule engine, not a clinical assessment.
     */
    val dailyInsight: String
        get() = when {
            pendingClinicalTasks >= 5 ->
                "You have $pendingClinicalTasks pending clinical tasks. Review the highest-priority items first."

            wellnessScore < 55 ->
                "Your current workload is high. Consider protecting recovery time after duty."

            otHoursThisMonth >= 40.0 ->
                "You have reached ${otHoursThisMonth.toInt()} OT hours this month. Keep an eye on workload balance."

            estimatedNetSalary > 0.0 && estimatedGrossSalary > 0.0 &&
                estimatedNetSalary / estimatedGrossSalary < 0.70 ->
                "Your deductions are taking a noticeable share of gross pay. Review your monthly deductions."

            claimTotalDays > 0 && claimProgress < 0.50f ->
                "Your claim record is below 50% complete. Keeping entries updated will make month-end easier."

            cpdTarget > 0 && cpdProgress < 0.30f ->
                "Your CPD progress is still early this cycle. Keep adding completed learning activities."

            else ->
                "Your dashboard is on track. Keep your duty, finance and professional records up to date."
        }
}
