package com.pasindu.nursingotapp.statgraph.engine.recommendation

import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport

/**
 * Exact verdict tiers required for Phase 2 chart evaluation.
 *
 * Notice that [NOT_SUITABLE] (structurally possible, statistically/perceptually poor)
 * is strictly separate from [IMPOSSIBLE] (cannot be plotted reliably because required
 * field roles, observations, or numeric properties are absent).
 */
enum class ChartVerdict(val displayName: String) {
    RECOMMENDED("Recommended"),
    VALID("Valid"),
    VALID_WITH_WARNING("Valid with Warning"),
    NOT_SUITABLE("Not Suitable"),
    IMPOSSIBLE("Impossible")
}

/**
 * Configurable transparent scoring weights used by [RecommendationEngine].
 */
data class ScoringWeights(
    val taskFitWeight: Double = 0.35,
    val perceptualAccuracyWeight: Double = 0.25,
    val dataFitWeight: Double = 0.25,
    val phoneReadabilityWeight: Double = 0.15,
    val maxMisleadingPenalty: Double = 35.0
) {
    init {
        require(taskFitWeight >= 0.0 && perceptualAccuracyWeight >= 0.0 && dataFitWeight >= 0.0 && phoneReadabilityWeight >= 0.0) {
            "Scoring weights must be non-negative."
        }
        val total = taskFitWeight + perceptualAccuracyWeight + dataFitWeight + phoneReadabilityWeight
        require(total > 0.0) { "At least one scoring weight must be positive." }
        require(maxMisleadingPenalty >= 0.0) { "Penalty ceiling must be non-negative." }
    }

    fun normalized(): ScoringWeights {
        val total = taskFitWeight + perceptualAccuracyWeight + dataFitWeight + phoneReadabilityWeight
        return copy(
            taskFitWeight = taskFitWeight / total,
            perceptualAccuracyWeight = perceptualAccuracyWeight / total,
            dataFitWeight = dataFitWeight / total,
            phoneReadabilityWeight = phoneReadabilityWeight / total
        )
    }
}

/**
 * Transparent sub-score breakdown (each dimension on a 0..100 scale before weighting).
 */
data class ScoreBreakdown(
    val taskFitScore: Double,
    val perceptualAccuracyScore: Double,
    val dataFitScore: Double,
    val phoneReadabilityScore: Double,
    val misleadingDesignPenalty: Double,
    val finalScore: Double,
    val componentNotes: List<String> = emptyList()
)

data class ChartAlternative(
    val specification: ChartSpecification,
    val verdict: ChartVerdict,
    val score: Double,
    val reason: String
)

data class ChartRecommendation(
    val specification: ChartSpecification,
    val verdict: ChartVerdict,
    val scoreBreakdown: ScoreBreakdown,
    val summaryReason: String,
    val warnings: List<String> = emptyList(),
    val missingRequirements: List<String> = emptyList(),
    val suggestedAlternatives: List<ChartAlternative> = emptyList()
)

data class ChartVerdictReport(
    val query: String,
    val matchResult: ChartMatchResult,
    val verdict: ChartVerdict,
    val evaluatedChart: ChartSpecification?,
    val scoreBreakdown: ScoreBreakdown?,
    val explanation: String,
    val warnings: List<String> = emptyList(),
    val missingRequirements: List<String> = emptyList(),
    val alternatives: List<ChartAlternative> = emptyList(),
    val relevantDiagnostics: List<DatasetDiagnostic> = emptyList(),
    val numericalEvidence: List<String> = emptyList()
)

data class RecommendationSuiteResult(
    val datasetId: String,
    val datasetName: String,
    val taskClassification: TaskClassificationResult,
    val diagnosticsReport: DatasetDiagnosticsReport,
    val recommendedCharts: List<ChartRecommendation>,
    val validCharts: List<ChartRecommendation>,
    val warningCharts: List<ChartRecommendation>,
    val notSuitableCharts: List<ChartRecommendation>,
    val impossibleCharts: List<ChartRecommendation>,
    val derivedEvidenceSummary: List<String>,
    val plainLanguageNarrative: String,
    val simpleNarrative: String = plainLanguageNarrative,
) {
    val topRecommendations: List<ChartRecommendation>
        get() = recommendedCharts.take(3)

    val allEvaluatedCharts: List<ChartRecommendation>
        get() = recommendedCharts + validCharts + warningCharts + notSuitableCharts + impossibleCharts
}
