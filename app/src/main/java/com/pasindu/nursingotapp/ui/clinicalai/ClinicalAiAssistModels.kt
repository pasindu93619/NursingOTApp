package com.pasindu.nursingotapp.ui.clinicalai

/** AI assists with routing, explanation, and checklist-style verification only. */
data class ClinicalAiSuggestion(
    val title: String,
    val message: String,
    val severity: AiSeverity = AiSeverity.INFO,
    val actions: List<String> = emptyList()
)

enum class AiSeverity { INFO, CHECK, HIGH_ALERT }

data class ClinicalAiContext(
    val toolId: String,
    val inputSummary: String,
    val resultSummary: String,
    val units: String,
    val warnings: List<String> = emptyList()
)
