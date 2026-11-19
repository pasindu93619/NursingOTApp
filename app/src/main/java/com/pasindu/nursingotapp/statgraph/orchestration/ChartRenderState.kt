package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartBarColorMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartLabelDisplayMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartAlternative
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationSuiteResult
import com.pasindu.nursingotapp.statgraph.model.StatField

/**
 * Optional caller/user selections controlling chart recommendation or field bindings.
 * All fields default to automatic recommendation and role-based binding.
 */
data class ChartMappingSelection(
    val chartSpecId: String? = null,
    val xFieldId: String? = null,
    val yFieldIds: List<String> = emptyList(),
    val groupFieldId: String? = null,
    val derivedFieldIds: List<String> = emptyList(),
    val userIntentQuery: String? = null,
    val customTitle: String? = null,
    val labelDisplayMode: ChartLabelDisplayMode = ChartLabelDisplayMode.AUTO,
    val barColorMode: ChartBarColorMode = ChartBarColorMode.AUTO,
)

/**
 * Metadata describing the source field or derived calculation bound to a plotted series.
 */
data class ResolvedSeriesBinding(
    val seriesId: String,
    val sourceFieldId: String,
    val isDerivedField: Boolean,
    val displayName: String,
    val unit: String,
    val colorIndex: Int,
    val markerShape: SeriesMarkerShape,
    val dashed: Boolean,
    val metadataNote: String = "",
)

/**
 * Complete validated render configuration resolved from dataset + recommendation.
 */
data class ResolvedChartConfiguration(
    val specification: ChartSpecification,
    val chartKind: ChartKind,
    val xAxisField: StatField?,
    val yAxisFields: List<StatField>,
    val groupingField: StatField?,
    val seriesBindings: List<ResolvedSeriesBinding>,
    val xAxisLabel: String,
    val yAxisLabel: String,
    val valueUnit: String,
    val missingObservationCount: Int,
    val totalDomainCount: Int,
    val duplicateDomainNote: String? = null,
    val diagnostics: List<DatasetDiagnostic> = emptyList(),
    val clinicalNotes: List<String> = emptyList(),
    val accessibleSummary: String,
)

/**
 * Explicit orchestration result state connecting analytics, recommendation, diagnostics,
 * and drawing toolkit readiness without hiding errors or fabricating renderers.
 */
sealed class ChartRenderState {
    data object Loading : ChartRenderState()

    data class EmptyDataset(
        val datasetName: String,
        val message: String,
        val suite: RecommendationSuiteResult? = null,
    ) : ChartRenderState()

    data class InvalidDataset(
        val datasetName: String,
        val blockingIssues: List<DatasetDiagnostic>,
        val message: String,
        val suite: RecommendationSuiteResult,
    ) : ChartRenderState()

    data class NoSuitableRecommendation(
        val datasetName: String,
        val message: String,
        val diagnostics: List<DatasetDiagnostic>,
        val suite: RecommendationSuiteResult,
    ) : ChartRenderState()

    data class UnsupportedChartRenderer(
        val datasetName: String,
        val requestedSpec: ChartSpecification,
        val explanation: String,
        val supportedAlternatives: List<ChartAlternative>,
        val suite: RecommendationSuiteResult,
    ) : ChartRenderState()

    data class RenderingError(
        val datasetName: String,
        val message: String,
        val suite: RecommendationSuiteResult? = null,
    ) : ChartRenderState()

    data class Ready(
        val configuration: ResolvedChartConfiguration,
        val renderData: ChartRenderData,
        val suite: RecommendationSuiteResult,
    ) : ChartRenderState()
}
