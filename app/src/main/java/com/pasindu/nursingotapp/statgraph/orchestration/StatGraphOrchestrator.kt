package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticSeverity
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartAlternative
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartRecommendation
import com.pasindu.nursingotapp.statgraph.engine.recommendation.ChartVerdict
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationEngine
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationSuiteResult
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull

/**
 * Pure-Kotlin orchestration facade connecting [StatDataset], [RecommendationEngine],
 * [DataDiagnosticsEngine], [DerivedFieldCalculator], [DatasetToChartRenderDataMapper],
 * and [ChartPainterResolver] into a single deterministic [ChartRenderState].
 */
object StatGraphOrchestrator {

    fun orchestrate(
        dataset: StatDataset,
        selection: ChartMappingSelection = ChartMappingSelection(),
        isLoading: Boolean = false,
    ): ChartRenderState {
        if (isLoading) return ChartRenderState.Loading

        val datasetName = dataset.name.ifBlank { "Untitled Dataset" }
        val suite = try {
            RecommendationEngine.recommend(
                dataset = dataset,
                userIntentQuery = selection.userIntentQuery,
            )
        } catch (t: Throwable) {
            return ChartRenderState.RenderingError(
                datasetName = datasetName,
                message = "Unable to analyze dataset: ${t.message ?: "unexpected error"}.",
            )
        }

        if (dataset.fields.isEmpty() || dataset.rows.isEmpty()) {
            return ChartRenderState.EmptyDataset(
                datasetName = datasetName,
                message = if (dataset.fields.isEmpty()) {
                    "Dataset has no defined columns/fields yet."
                } else {
                    "Dataset has no observations recorded yet."
                },
                suite = suite,
            )
        }

        val hasAnyNumericSource = dataset.fields.any { it.role.isNumeric } || dataset.derivedFields.isNotEmpty()
        if (!hasAnyNumericSource) {
            return ChartRenderState.InvalidDataset(
                datasetName = datasetName,
                blockingIssues = suite.diagnosticsReport.blockingDiagnostics,
                message = "Dataset requires at least one numeric Measure, Rate, Estimate, or derived numeric field to render a statistical chart.",
                suite = suite,
            )
        }

        val hasValidNumericValues = dataset.fields.filter { it.role.isNumeric }.any { field ->
            dataset.rows.any { row -> row.values[field.id].asDoubleOrNull() != null }
        }

        // Handle explicit user-selected chart specification first.
        val requestedSpecId = selection.chartSpecId?.trim()?.takeIf { it.isNotEmpty() }
        val targetEvaluation: ChartRecommendation = if (requestedSpecId != null) {
            val found = suite.allEvaluatedCharts.firstOrNull { it.specification.id == requestedSpecId }
                ?: return ChartRenderState.RenderingError(
                    datasetName = datasetName,
                    message = "Requested chart specification '$requestedSpecId' was not found in ChartCatalogue.",
                    suite = suite,
                )

            when (val support = ChartPainterResolver.resolveSupport(found.specification)) {
                is PainterSupportResult.Unsupported -> {
                    return ChartRenderState.UnsupportedChartRenderer(
                        datasetName = datasetName,
                        requestedSpec = found.specification,
                        explanation = support.reason,
                        supportedAlternatives = buildSupportedAlternatives(suite, excludeId = found.specification.id),
                        suite = suite,
                    )
                }
                is PainterSupportResult.Supported -> Unit
            }

            if (!hasValidNumericValues && dataset.derivedFields.isEmpty()) {
                return ChartRenderState.EmptyDataset(
                    datasetName = datasetName,
                    message = "All numeric observations are missing; nothing valid to plot without inventing values.",
                    suite = suite,
                )
            }

            if (found.verdict == ChartVerdict.IMPOSSIBLE) {
                return ChartRenderState.InvalidDataset(
                    datasetName = datasetName,
                    blockingIssues = suite.diagnosticsReport.blockingDiagnostics,
                    message = found.missingRequirements.joinToString("; ").ifBlank { found.summaryReason },
                    suite = suite,
                )
            }
            found
        } else {
            if (!hasValidNumericValues && dataset.derivedFields.isEmpty()) {
                return ChartRenderState.EmptyDataset(
                    datasetName = datasetName,
                    message = "All numeric observations are missing; nothing valid to plot without inventing values.",
                    suite = suite,
                )
            }

            val eligible = suite.recommendedCharts + suite.validCharts + suite.warningCharts
            if (eligible.isEmpty()) {
                return if (suite.diagnosticsReport.hasBlockingIssues) {
                    ChartRenderState.InvalidDataset(
                        datasetName = datasetName,
                        blockingIssues = suite.diagnosticsReport.blockingDiagnostics,
                        message = suite.diagnosticsReport.blockingDiagnostics.joinToString("; ") { it.message },
                        suite = suite,
                    )
                } else {
                    ChartRenderState.NoSuitableRecommendation(
                        datasetName = datasetName,
                        message = "No statistically appropriate chart recommendation matched the current fields and observations.",
                        diagnostics = suite.diagnosticsReport.diagnostics,
                        suite = suite,
                    )
                }
            }

            selectBestSupportedRecommendation(eligible, selection) ?: run {
                val topUnsupported = eligible.first().specification
                val reason = when (val support = ChartPainterResolver.resolveSupport(topUnsupported)) {
                    is PainterSupportResult.Unsupported -> support.reason
                    is PainterSupportResult.Supported -> "No supported toolkit renderer matched."
                }
                return ChartRenderState.UnsupportedChartRenderer(
                    datasetName = datasetName,
                    requestedSpec = topUnsupported,
                    explanation = reason,
                    supportedAlternatives = emptyList(),
                    suite = suite,
                )
            }
        }

        val support = ChartPainterResolver.resolveSupport(targetEvaluation.specification)
        val chartKind = when (support) {
            is PainterSupportResult.Supported -> support.chartKind
            is PainterSupportResult.Unsupported -> {
                return ChartRenderState.UnsupportedChartRenderer(
                    datasetName = datasetName,
                    requestedSpec = targetEvaluation.specification,
                    explanation = support.reason,
                    supportedAlternatives = buildSupportedAlternatives(suite, excludeId = targetEvaluation.specification.id),
                    suite = suite,
                )
            }
        }

        return when (
            val mapped = DatasetToChartRenderDataMapper.resolveAndMap(
                dataset = dataset,
                specification = targetEvaluation.specification,
                chartKind = chartKind,
                diagnosticsReport = suite.diagnosticsReport,
                selection = selection,
            )
        ) {
            is MappingOutcome.InvalidMapping -> ChartRenderState.InvalidDataset(
                datasetName = datasetName,
                blockingIssues = suite.diagnosticsReport.blockingDiagnostics,
                message = mapped.message,
                suite = suite,
            )

            is MappingOutcome.Success -> {
                if (!mapped.renderData.hasDrawablePoints) {
                    ChartRenderState.EmptyDataset(
                        datasetName = datasetName,
                        message = "All values for '${mapped.configuration.yAxisLabel}' are missing; cannot plot without inventing values.",
                        suite = suite,
                    )
                } else {
                    val mappedIds = buildSet {
                        mapped.configuration.xAxisField?.id?.let(::add)
                        mapped.configuration.yAxisFields.forEach { add(it.id) }
                        mapped.configuration.groupingField?.id?.let(::add)
                    }
                    val blockingOnMapped = mapped.configuration.diagnostics.filter { diag ->
                        diag.severity == DiagnosticSeverity.BLOCKING &&
                            (diag.affectedFieldIds.isEmpty() || diag.affectedFieldIds.any { it in mappedIds })
                    }
                    if (blockingOnMapped.isNotEmpty() && requestedSpecId == "stacked_bar_100_chart") {
                        ChartRenderState.InvalidDataset(
                            datasetName = datasetName,
                            blockingIssues = blockingOnMapped,
                            message = blockingOnMapped.joinToString("; ") { it.message },
                            suite = suite,
                        )
                    } else {
                        ChartRenderState.Ready(
                            configuration = mapped.configuration,
                            renderData = mapped.renderData,
                            suite = suite,
                        )
                    }
                }
            }
        }
    }

    private fun selectBestSupportedRecommendation(
        eligible: List<ChartRecommendation>,
        selection: ChartMappingSelection,
    ): ChartRecommendation? {
        val supported = eligible.filter { ChartToolkitSupport.isSupported(it.specification.id) }
        if (supported.isEmpty()) return null

        val expectsMultiSeries = selection.groupFieldId != null ||
            (selection.yFieldIds.size + selection.derivedFieldIds.size) >= 2
        if (expectsMultiSeries) {
            val multiPreferred = supported.firstOrNull { rec ->
                rec.specification.id in setOf(
                    "multi_series_line_chart",
                    "grouped_bar_chart",
                    "stacked_bar_chart",
                    "stacked_bar_100_chart",
                )
            }
            if (multiPreferred != null) return multiPreferred
        }
        return supported.first()
    }

    private fun buildSupportedAlternatives(
        suite: RecommendationSuiteResult,
        excludeId: String,
    ): List<ChartAlternative> {
        return (suite.recommendedCharts + suite.validCharts + suite.warningCharts)
            .asSequence()
            .filter { it.specification.id != excludeId && ChartToolkitSupport.isSupported(it.specification.id) }
            .take(3)
            .map { rec ->
                ChartAlternative(
                    specification = rec.specification,
                    verdict = rec.verdict,
                    score = rec.scoreBreakdown.finalScore,
                    reason = rec.summaryReason,
                )
            }
            .toList()
    }
}
