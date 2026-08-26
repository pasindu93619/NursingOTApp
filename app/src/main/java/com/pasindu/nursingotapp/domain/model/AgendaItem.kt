package com.pasindu.nursingotapp.domain.model

/**
 * A single actionable item displayed by the NursingOS Daily Agenda.
 * Pure domain data: no Compose or Room dependencies.
 */
data class AgendaItem(
    val id: String,
    val priority: AgendaPriority,
    val title: String,
    val detail: String,
    val actionLabel: String,
    val route: String,
    val clinicalTaskId: Int? = null
)

enum class AgendaPriority {
    URGENT,
    TODAY,
    LATER
}
