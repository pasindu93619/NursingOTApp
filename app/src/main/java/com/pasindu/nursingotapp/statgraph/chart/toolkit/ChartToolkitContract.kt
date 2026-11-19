package com.pasindu.nursingotapp.statgraph.chart.toolkit

import com.pasindu.nursingotapp.statgraph.model.ChartAxisHint
import com.pasindu.nursingotapp.statgraph.model.StatDataset

/**
 * Chart types supported by the future Stat Graph Studio visualization toolkit.
 * Phase 1 defines role-to-chart compatibility metadata without rendering charts.
 */
enum class StatChartType(val displayName: String) {
    TIME_SERIES_TREND("Time-Series Trend"),
    PART_TO_WHOLE_SHARE("Part-to-Whole Share"),
    CATEGORY_COMPARISON("Category Comparison"),
    RATE_INDICATOR("Rate / Continuous Trend"),
    ESTIMATE_INTERVAL_PLOT("Estimate & Confidence Interval"),
    DURATION_EVENT_CURVE("Duration / Time-to-Event")
}

data class ChartReadinessSuggestion(
    val chartType: StatChartType,
    val isReady: Boolean,
    val summary: String
)

/**
 * Evaluates a [StatDataset]'s confirmed/assigned field roles to determine which
 * statistical chart types are unlocked.
 */
object ChartToolkitAnalyzer {

    fun analyzeReadiness(dataset: StatDataset): List<ChartReadinessSuggestion> {
        val hints = dataset.fields.map { it.role.chartAxisHint }.toSet()
        val hasRows = dataset.rows.isNotEmpty()

        val suggestions = mutableListOf<ChartReadinessSuggestion>()

        if (ChartAxisHint.DOMAIN_TIME in hints &&
            (ChartAxisHint.MEASURE_PRIMARY in hints || ChartAxisHint.MEASURE_RATE in hints)
        ) {
            suggestions.add(
                ChartReadinessSuggestion(
                    chartType = StatChartType.TIME_SERIES_TREND,
                    isReady = hasRows,
                    summary = if (hasRows) "Ready: Time axis + numeric measure" else "Add rows to plot time trend"
                )
            )
        }

        if (ChartAxisHint.MEASURE_COMPONENT in hints && ChartAxisHint.MEASURE_PRIMARY in hints) {
            suggestions.add(
                ChartReadinessSuggestion(
                    chartType = StatChartType.PART_TO_WHOLE_SHARE,
                    isReady = hasRows,
                    summary = if (hasRows) "Ready: Part-of-measure paired with total" else "Add rows to plot share"
                )
            )
        }

        if (ChartAxisHint.DOMAIN_CATEGORY in hints &&
            (ChartAxisHint.MEASURE_PRIMARY in hints || ChartAxisHint.MEASURE_RATE in hints)
        ) {
            suggestions.add(
                ChartReadinessSuggestion(
                    chartType = StatChartType.CATEGORY_COMPARISON,
                    isReady = hasRows,
                    summary = if (hasRows) "Ready: Category dimension + measure" else "Add rows to compare categories"
                )
            )
        }

        if (ChartAxisHint.CONFIDENCE_INTERVAL in hints) {
            suggestions.add(
                ChartReadinessSuggestion(
                    chartType = StatChartType.ESTIMATE_INTERVAL_PLOT,
                    isReady = hasRows,
                    summary = if (hasRows) "Ready: Estimate with lower/upper limits" else "Add rows to plot intervals"
                )
            )
        }

        if (ChartAxisHint.SURVIVAL_DURATION in hints) {
            suggestions.add(
                ChartReadinessSuggestion(
                    chartType = StatChartType.DURATION_EVENT_CURVE,
                    isReady = hasRows,
                    summary = if (hasRows) "Ready: Duration + event flag" else "Add rows for duration curve"
                )
            )
        }

        return suggestions
    }
}
