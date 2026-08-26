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
    val wellnessScore: Int = 100,
    val todayDutyRecorded: Boolean = false,
    val todayDutyHours: Double = 0.0,
    val todayOtHours: Double = 0.0,
    val todayPh: Boolean = false,
    val todayClaimRecorded: Boolean = false
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

    /** True when today's record needs user attention. */
    val todayNeedsAttention: Boolean
        get() = pendingClinicalTasks > 0 || !todayClaimRecorded

    /** Route for the primary action on today's card. */
    val todayActionRoute: String
        get() = when {
            pendingClinicalTasks > 0 -> "clinical_planning"
            !todayClaimRecorded -> "claim_period"
            wellnessScore < 55 -> "care_pulse"
            todayOtHours > 0.0 || todayPh -> "advanced_finance_hub"
            else -> "analytics"
        }

    /** Destination route for the most useful overall action. */
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

    val todayAction: String
        get() = when {
            pendingClinicalTasks > 0 ->
                "Review $pendingClinicalTasks pending clinical task${if (pendingClinicalTasks == 1) "" else "s"}."
            !todayClaimRecorded ->
                "Record today's duty/claim entry before you finish the shift."
            todayOtHours > 0.0 ->
                "Today's OT: ${formatHours(todayOtHours)}. Keep your monthly OT record current."
            todayPh ->
                "Today's public-holiday duty is recorded. Review the claim details when convenient."
            !todayDutyRecorded ->
                "No duty entry is recorded for today yet. Add it when you have the details."
            cpdProgress < 0.30f ->
                "Your work record is up to date. Consider adding your next CPD activity."
            else ->
                "Today is up to date. Your main records are in good shape."
        }

    val todayStatus: String
        get() = when {
            pendingClinicalTasks > 0 -> "ATTENTION"
            !todayClaimRecorded -> "ACTION NEEDED"
            wellnessScore < 55 -> "RECOVERY"
            todayPh -> "PH DUTY"
            todayOtHours > 0.0 -> "OT RECORDED"
            todayDutyRecorded -> "ON TRACK"
            else -> "READY"
        }

    private fun formatHours(value: Double): String =
        if (value % 1.0 == 0.0) "${value.toInt()} h" else "%.1f h".format(value)
}
