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

    /** Transparent clinical-readiness component, not a medical score. */
    val clinicalHealthScore: Int
        get() = (100 - pendingClinicalTasks * 8).coerceIn(0, 100)

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
        get() = (100 - (otHoursThisMonth / 50.0 * 100.0).toInt()).coerceIn(0, 100)

    /**
     * Transparent NursingOS readiness index (0-100).
     * This is an operational dashboard signal, not a medical or mental-health score.
     */
    val nursingOsScore: Int
        get() {
            val workloadComponent = wellnessScore.coerceIn(0, 100)
            val claimComponent = (claimProgress * 100f).toInt().coerceIn(0, 100)
            val cpdComponent = (cpdProgress * 100f).toInt().coerceIn(0, 100)
            return (
                workloadComponent * 0.30 +
                    clinicalHealthScore * 0.20 +
                    claimComponent * 0.15 +
                    cpdComponent * 0.10 +
                    financialHealthScore * 0.15 +
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

    /**
     * One ranked operational recommendation derived from the current state.
     * The rules intentionally prefer concrete, reversible actions.
     */
    val nursingOsDecision: NursingOsDecision
        get() = when {
            pendingClinicalTaskDetails.isNotEmpty() -> {
                val task = pendingClinicalTaskDetails.first()
                val priority = task.priority.uppercase()
                val urgent = priority == "CRITICAL" || priority == "URGENT" || priority == "HIGH"
                NursingOsDecision(
                    priority = if (urgent) DecisionPriority.URGENT else DecisionPriority.TODAY,
                    title = task.taskName,
                    reason = if (pendingClinicalTasks > 1) {
                        "$pendingClinicalTasks clinical tasks are pending; this is the highest-priority recorded item."
                    } else {
                        "This is your highest-priority pending clinical task."
                    },
                    actionLabel = "Open Clinical Planning",
                    route = "clinical_planning",
                    clinicalTaskId = task.id
                )
            }
            wellnessScore < 55 -> NursingOsDecision(
                priority = DecisionPriority.URGENT,
                title = "Protect recovery time",
                reason = "Your workload indicator is $wellnessScore/100.",
                actionLabel = "Open CarePulse",
                route = "care_pulse"
            )
            !todayClaimRecorded -> NursingOsDecision(
                priority = DecisionPriority.TODAY,
                title = "Record today's duty claim",
                reason = "Today's duty/claim entry is not recorded yet.",
                actionLabel = "Open OT Claim",
                route = "claim_period"
            )
            financialHealthScore < 65 -> NursingOsDecision(
                priority = DecisionPriority.TODAY,
                title = "Review financial pressure",
                reason = "Only ${(netRetentionRatio * 100).toInt()}% of gross pay is currently retained.",
                actionLabel = "Open Finance",
                route = "advanced_finance_hub"
            )
            otHoursThisMonth >= 40.0 -> NursingOsDecision(
                priority = DecisionPriority.TODAY,
                title = "Review high OT load",
                reason = "${otHoursThisMonth.toInt()} OT hours are recorded this month.",
                actionLabel = "Open Finance",
                route = "advanced_finance_hub"
            )
            claimTotalDays > 0 && claimProgress < 0.50f -> NursingOsDecision(
                priority = DecisionPriority.TODAY,
                title = "Bring claims up to date",
                reason = "$claimCompletedDays of $claimTotalDays claim days are currently recorded.",
                actionLabel = "Open OT Claim",
                route = "claim_period"
            )
            cpdTarget > 0 && cpdProgress < 0.30f -> NursingOsDecision(
                priority = DecisionPriority.LATER,
                title = "Build CPD progress",
                reason = "$cpdPoints of $cpdTarget CPD points are currently recorded.",
                actionLabel = "Open Knowledge Hub",
                route = "knowledge_hub"
            )
            else -> NursingOsDecision(
                priority = DecisionPriority.CLEAR,
                title = "You're on track",
                reason = "No higher-priority operational action is currently detected.",
                actionLabel = "View Analytics",
                route = "analytics"
            )
        }

    val nursingOsRecommendation: String
        get() = when (nursingOsDecision.priority) {
            DecisionPriority.URGENT -> "Act first: ${nursingOsDecision.title}. ${nursingOsDecision.reason}"
            DecisionPriority.TODAY -> "Today's priority: ${nursingOsDecision.title}. ${nursingOsDecision.reason}"
            DecisionPriority.LATER -> "Next best step: ${nursingOsDecision.title}. ${nursingOsDecision.reason}"
            DecisionPriority.CLEAR -> nursingOsDecision.reason
        }

    val todayNeedsAttention: Boolean
        get() = pendingClinicalTasks > 0 || !todayClaimRecorded

    val todayActionRoute: String
        get() = nursingOsDecision.route

    val insightRoute: String
        get() = nursingOsDecision.route

    val dailyInsight: String
        get() = nursingOsDecision.reason

    val todayAction: String
        get() = when (nursingOsDecision.priority) {
            DecisionPriority.URGENT -> "Act now: ${nursingOsDecision.title}."
            DecisionPriority.TODAY -> "Today's action: ${nursingOsDecision.title}."
            DecisionPriority.LATER -> "Next: ${nursingOsDecision.title}."
            DecisionPriority.CLEAR -> "Your main records are up to date."
        }

    val todayStatus: String
        get() = when (nursingOsDecision.priority) {
            DecisionPriority.URGENT -> "ATTENTION"
            DecisionPriority.TODAY -> "ACTION NEEDED"
            DecisionPriority.LATER -> "ON TRACK"
            DecisionPriority.CLEAR -> "READY"
        }

    val urgentAction: AgendaItem?
        get() = when {
            nursingOsDecision.priority == DecisionPriority.URGENT -> AgendaItem(
                id = "decision_${nursingOsDecision.route}_${nursingOsDecision.clinicalTaskId ?: "general"}",
                priority = AgendaPriority.URGENT,
                title = nursingOsDecision.title,
                detail = nursingOsDecision.reason,
                actionLabel = nursingOsDecision.actionLabel,
                route = nursingOsDecision.route,
                clinicalTaskId = nursingOsDecision.clinicalTaskId
            )
            else -> null
        }

    val todayAgenda: List<AgendaItem>
        get() = buildList {
            if (nursingOsDecision.priority == DecisionPriority.TODAY) {
                add(
                    AgendaItem(
                        id = "decision_today_${nursingOsDecision.route}_${nursingOsDecision.clinicalTaskId ?: "general"}",
                        priority = AgendaPriority.TODAY,
                        title = nursingOsDecision.title,
                        detail = nursingOsDecision.reason,
                        actionLabel = nursingOsDecision.actionLabel,
                        route = nursingOsDecision.route,
                        clinicalTaskId = nursingOsDecision.clinicalTaskId
                    )
                )
            }

            pendingClinicalTaskDetails.drop(if (nursingOsDecision.clinicalTaskId != null) 1 else 0)
                .take(2)
                .forEach { task ->
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

            if (todayOtHours > 0.0 && nursingOsDecision.route != "advanced_finance_hub") {
                add(AgendaItem("today_ot", AgendaPriority.TODAY, "Review today's OT", "${formatHours(todayOtHours)} recorded today.", "Open Finance", "advanced_finance_hub"))
            }
        }

    val laterAgenda: List<AgendaItem>
        get() = buildList {
            if (cpdTarget > 0 && cpdProgress < 0.50f) {
                add(AgendaItem("cpd_progress", AgendaPriority.LATER, "Build CPD progress", "${cpdPoints}/${cpdTarget} CPD points recorded.", "Open Knowledge Hub", "knowledge_hub"))
            }
            if (claimTotalDays > 0 && claimProgress < 0.85f && todayClaimRecorded && nursingOsDecision.route != "claim_period") {
                add(AgendaItem("monthly_claim", AgendaPriority.LATER, "Keep the monthly claim current", "$claimCompletedDays/$claimTotalDays days recorded.", "Open OT Claim", "claim_period"))
            }
            if (estimatedGrossSalary > 0.0 && estimatedNetSalary > 0.0 && netRetentionRatio < 0.80f && nursingOsDecision.route != "advanced_finance_hub") {
                add(AgendaItem("deductions", AgendaPriority.LATER, "Review deductions", "Net pay is below 80% of gross pay.", "Open Finance", "advanced_finance_hub"))
            }
        }

    private fun formatHours(value: Double): String =
        if (value % 1.0 == 0.0) "${value.toInt()} h" else "%.1f h".format(value)
}
