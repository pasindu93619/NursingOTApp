package com.pasindu.nursingotapp.ui.clinicalai

/** Builds neutral AI context from deterministic results without exposing hidden clinical rules. */
object ClinicalAiContextBuilder {
    fun build(
        toolId: String,
        inputSummary: String,
        resultSummary: String,
        units: String,
        warnings: List<String> = emptyList()
    ): ClinicalAiContext = ClinicalAiContext(
        toolId = toolId,
        inputSummary = inputSummary,
        resultSummary = resultSummary,
        units = units,
        warnings = warnings
    )
}
