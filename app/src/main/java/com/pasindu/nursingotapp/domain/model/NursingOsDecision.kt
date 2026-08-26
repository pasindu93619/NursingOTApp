package com.pasindu.nursingotapp.domain.model

/**
 * The single most important operational action for the nurse right now.
 * This is a prioritisation signal, not a clinical or financial diagnosis.
 */
data class NursingOsDecision(
    val priority: DecisionPriority,
    val title: String,
    val reason: String,
    val actionLabel: String,
    val route: String,
    val clinicalTaskId: Int? = null
)

enum class DecisionPriority {
    URGENT,
    TODAY,
    LATER,
    CLEAR
}
