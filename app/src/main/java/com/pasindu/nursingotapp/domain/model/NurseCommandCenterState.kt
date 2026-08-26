package com.pasindu.nursingotapp.domain.model

import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity

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
    val pendingClinicalTaskDetails: List<ClinicalTaskEntity> = emptyList(),
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
        } else 0f

    val cpdProgress: Float
        get() = if (cpdTarget > 0) {
            (cpdPoints.toFloat() / cpdTarget.toFloat()).coerceIn(0f, 1f)
        } else 0f

    /** Ratio of net pay retained from gross pay, expressed as 0..1. */
    val netRetentionRatio: Float
        get() = if (estimatedGrossSalary > 0.0 && estimatedNetSalary >= 0.0) {
            (estimatedNetSalary / estimatedGrossSalary).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }

    /** Transparent financial-pressure indicator. Higher means less deduction pressure. */
    val financialHealthScore: Int
        get() = when {
            estimatedGrossSalary <= 0.0 -> 100
            netRetentionRatio >= 0.85f -> 100
            netRetentionRatio >= 0.75f -> 85
            netRetentionRatio >= 0.65f -> 70
            netRetentionRatio >= 0.55f -> 55
            else -> 40
        }

    /** Transparent OT-load component. Higher means more capacity remaining. */
    val otLoadScore: Int
        get() = (100 - (otHoursThisMonth / 50.0 * 100.0).toInt())
            .coerceIn(0, 100)

    /**
     * Transparent NursingOS readiness index (0-100).
     * This is an operational dashboard signal, not a medical or mental-health score.
     */
    val nursingOsScore: Int
        get() {
            val workloadComponent = wellnessScore.coerceIn(0, 100)
            val clinicalComponent = (100 - pendingClinicalTasks * 8).coerceIn(0, 100)
            val claimComponent = (claimProgress * 100f).toInt().coerceIn(0, 100)
            val cpdComponent = (cpdProgress * 100f).toInt().coerceIn(0, 100)
            val financeComponent = financialHealthScore

            return (
                workloadComponent * 0.30 +
                    clinicalComponent * 0.20 +
                    claimComponent * 0.15 +
                    cpdComponent * 0.10 +
                    financeComponent * 0.15 +
                    otLoadScore * 0.10
                ).toInt().coerceIn(0, 100)
        }

    val nursingOsScoreLabel: String
        get() = when {
            nursingOsScore >= 85 -> "Excellent control"
            nursingOsScore >= 70 -> "Good control"
            nursingOsScore >= 50 -> "Needs attention"
            else -> "High attention"
        }

    val nursingOsRecommendation: String
        get() = when {
            pendingClinicalTasks >= 5 ->
                "Clear high-priority clinical tasks first; they have the greatest operational impact today."
            wellnessScore < 55 ->
                "Your workload signal is high. Protect recovery time and avoid unnecessary extra workload where possible."
            financialHealthScore < 65 ->
                "A large share of gross pay is being reduced. Review deductions and commitments in Finance."
            otHoursThisMonth >= 40.0 ->
                "Your OT load is high this month. Review the financial benefit against your overall workload."
            !todayClaimRecorded ->
                "Record today's duty/claim entry so your operational and financial picture stays complete."
            claimProgress < 0.50f ->
                "Bring the monthly claim record up to date before month-end pressure builds."
            cpdProgress < 0.30f ->
                "Consider adding a small CPD activity when your shift load is stable."
            else ->
                "Your nursing work, finance and professional records are in good shape. Keep them current."
        }

    val todayNeedsAttention: Boolean
        get() = pendingClinicalTasks > 0 || !todayClaimRecorded

    val todayActionRoute: String
        get() = when {
            pendingClinicalTasks > 0 -> "clinical_planning"
            !todayClaimRecorded -> "claim_period"
            wellnessScore < 55 -> "care_pulse"
            financialHealthScore < 65 || todayOtHours > 0.0 || todayPh -> "advanced_finance_hub"
            else -> "analytics"
        }

    val insightRoute: String
        get() = when {
            pendingClinicalTasks >= 5 -> "clinical_planning"
            wellnessScore < 55 -> "care_pulse"
            financialHealthScore < 65 -> "advanced_finance_hub"
            otHoursThisMonth >= 40.0 -> "advanced_finance_hub"
            estimatedNetSalary > 0.0 && estimatedGrossSalary > 0.0 &&
                estimatedNetSalary / estimatedGrossSalary < 0.70 -> "advanced_finance_hub"
            claimTotalDays > 0 && claimProgress < 0.50f -> "claim_period"
            cpdTarget > 0 && cpdProgress < 0.30f -> "knowledge_hub"
            else -> "analytics"
        }

    val dailyInsight: String
        get() = when {
            pendingClinicalTasks >= 5 ->
                "You have $pendingClinicalTasks pending clinical tasks. Review the highest-priority items first."
            wellnessScore < 55 ->
                "Your current workload is high. Consider protecting recovery time after duty."
            financialHealthScore < 65 ->
                "Your deductions are taking a substantial share of gross pay. Review your Finance records."
            otHoursThisMonth >= 40.0 ->
                "You have reached ${otHoursThisMonth.toInt()} OT hours this month. Keep an eye on workload balance."
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
            financialHealthScore < 65 ->
                "Review your deductions and current financial record."
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
            financialHealthScore < 65 -> "FINANCE REVIEW"
            todayPh -> "PH DUTY"
            todayOtHours > 0.0 -> "OT RECORDED"
            todayDutyRecorded -> "ON TRACK"
            else -> "READY"
        }

    val urgentAction: AgendaItem?
        get() = when {
            pendingClinicalTasks > 0 && pendingClinicalTaskDetails.isNotEmpty() -> {
                val task = pendingClinicalTaskDetails.first()
                AgendaItem(
                    id = "clinical_task_${task.id}",
                    priority = AgendaPriority.URGENT,
                    title = task.taskName,
                    detail = task.description.ifBlank { "Pending clinical task" },
                    actionLabel = "Complete or open task",
                    route = "clinical_planning",
                    clinicalTaskId = task.id
                )
            }
            !todayClaimRecorded -> AgendaItem(
                id = "today_claim",
                priority = AgendaPriority.URGENT,
                title = "Record today's claim",
                detail = "Today's duty/claim entry is not recorded yet.",
                actionLabel = "Open OT Claim",
                route = "claim_period"
            )
            wellnessScore < 55 -> AgendaItem(
                id = "recovery",
                priority = AgendaPriority.URGENT,
                title = "Workload is high",
                detail = "Your current workload indicator is $wellnessScore/100.",
                actionLabel = "Open CarePulse",
                route = "care_pulse"
            )
            financialHealthScore < 65 -> AgendaItem(
                id = "finance_pressure",
                priority = AgendaPriority.URGENT,
                title = "Review financial pressure",
                detail = "Net pay retention is ${(netRetentionRatio * 100).toInt()}% of gross.",
                actionLabel = "Open Finance",
                route = "advanced_finance_hub"
            )
            else -> null
        }

    val todayAgenda: List<AgendaItem>
        get() = buildList {
            pendingClinicalTaskDetails.drop(1).take(3).forEach { task ->
                add(
                    AgendaItem(
                        id = "clinical_task_${task.id}",
                        priority = AgendaPriority.TODAY,
                        title = task.taskName,
                        detail = task.description.ifBlank { task.priority },
                        actionLabel = "Complete or open task",
                        route = "clinical_planning",
                        clinicalTaskId = task.id
                    )
                )
            }
            if (todayOtHours > 0.0) {
                add(AgendaItem("today_ot", AgendaPriority.TODAY, "Review today's OT", "${formatHours(todayOtHours)} recorded today.", "Open Finance", "advanced_finance_hub"))
            }
            if (todayPh) {
                add(AgendaItem("today_ph", AgendaPriority.TODAY, "Review PH duty", "Public-holiday duty is recorded today.", "Review Claim", "claim_period"))
            }
            if (todayDutyRecorded && todayClaimRecorded) {
                add(AgendaItem("today_recorded", AgendaPriority.TODAY, "Today's duty is recorded", "Keep the rest of the monthly record current.", "View Analytics", "analytics"))
            }
        }

    val laterAgenda: List<AgendaItem>
        get() = buildList {
            if (cpdTarget > 0 && cpdProgress < 0.50f) {
                add(AgendaItem("cpd_progress", AgendaPriority.LATER, "Build CPD progress", "${cpdPoints}/${cpdTarget} CPD points recorded.", "Open Knowledge Hub", "knowledge_hub"))
            }
            if (claimTotalDays > 0 && claimProgress < 0.85f && todayClaimRecorded) {
                add(AgendaItem("monthly_claim", AgendaPriority.LATER, "Keep the monthly claim current", "$claimCompletedDays/$claimTotalDays days recorded.", "Open OT Claim", "claim_period"))
            }
            if (estimatedGrossSalary > 0.0 && estimatedNetSalary > 0.0 && estimatedNetSalary / estimatedGrossSalary < 0.80) {
                add(AgendaItem("deductions", AgendaPriority.LATER, "Review deductions", "Net pay is below 80% of gross pay.", "Open Finance", "advanced_finance_hub"))
            }
        }

    private fun formatHours(value: Double): String =
        if (value % 1.0 == 0.0) "${value.toInt()} h" else "%.1f h".format(value)
}
