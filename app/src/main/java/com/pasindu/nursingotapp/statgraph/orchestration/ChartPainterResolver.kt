package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.AreaChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.AxisAndGridPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BoxPlotChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BubbleChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BulletChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BumpChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.CalendarHeatmapPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ConnectedScatterPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ControlChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.CorrelationHeatmapPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.CyclePlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.DensityPlotChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.DotPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.DumbbellChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.DivergingBarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.EcdfChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ErrorBarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ForestPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.FunnelChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.HexbinPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.HistogramChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.HorizontalBarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.InteractiveChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.KaplanMeierChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.LikertDivergingChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.LineChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.LollipopChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.MosaicPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ParetoChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.PieOrDonutChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.PopulationPyramidPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.QqPlotChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.RadarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.RangeBandChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.RangePlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ScatterPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ScatterplotMatrixPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SeriesMarkersPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SlopeChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SparklineMultiplesPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.StackedAreaChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.StemAndLeafPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.StepChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.StripPlotChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SunburstChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.TreemapChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ViolinPlotChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.WaffleChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.WaterfallChartPainter
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification

/**
 * Authoritative bridge between Phase 2 [ChartSpecification] IDs and Phase 3 [ChartKind]
 * toolkit renderers. Any catalogue entry outside [SUPPORTED_CHART_KINDS] is explicitly reported
 * as unsupported rather than silently replaced with a misleading chart.
 */
object ChartToolkitSupport {
    val SUPPORTED_CHART_KINDS: Map<String, ChartKind> = linkedMapOf(
        "line_chart" to ChartKind.LINE,
        "multi_series_line_chart" to ChartKind.MULTI_LINE,
        "bar_chart" to ChartKind.BAR,
        "grouped_bar_chart" to ChartKind.GROUPED_BAR,
        "stacked_bar_chart" to ChartKind.STACKED_BAR,
        "stacked_bar_100_chart" to ChartKind.PERCENT_STACKED_BAR,
        "scatter_plot" to ChartKind.SCATTER,
        // Batch 1A Foundational Core Charts
        "slope_chart" to ChartKind.SLOPE,
        "bump_chart" to ChartKind.BUMP,
        "sparkline_small_multiples" to ChartKind.SPARKLINE_MULTIPLES,
        "range_band_chart" to ChartKind.RANGE_BAND,
        "connected_scatter_plot" to ChartKind.CONNECTED_SCATTER,
        "cycle_plot" to ChartKind.CYCLE_PLOT,
        "calendar_heatmap" to ChartKind.CALENDAR_HEATMAP,
        "bullet_chart" to ChartKind.BULLET,
        "dumbbell_chart" to ChartKind.DUMBBELL,
        "radar_chart" to ChartKind.RADAR,
        // Batch 1B Statistical and Distribution Charts (Group B1)
        "diverging_bar_chart" to ChartKind.DIVERGING_BAR,
        "likert_stacked_diverging_chart" to ChartKind.LIKERT_DIVERGING,
        "waffle_chart" to ChartKind.WAFFLE,
        "pareto_chart" to ChartKind.PARETO,
        // Batch 1B Statistical and Distribution Charts (Group B2)
        "ecdf_chart" to ChartKind.ECDF,
        "qq_plot" to ChartKind.QQ_PLOT,
        "stem_and_leaf_plot" to ChartKind.STEM_AND_LEAF,
        // Batch 1B Statistical and Distribution Charts (Group B3)
        "hexbin_plot" to ChartKind.HEXBIN,
        "scatterplot_matrix" to ChartKind.SCATTERPLOT_MATRIX,
        "mosaic_plot" to ChartKind.MOSAIC,
        "sunburst_chart" to ChartKind.SUNBURST,
        // Batch 2 Ranking, Trends & Comparison Extensions
        "horizontal_bar_chart" to ChartKind.HORIZONTAL_BAR,
        "lollipop_chart" to ChartKind.LOLLIPOP,
        "dot_plot" to ChartKind.DOT_PLOT,
        "area_chart" to ChartKind.AREA,
        "stacked_area_chart" to ChartKind.STACKED_AREA,
        "step_chart" to ChartKind.STEP,
        // Batch 3 Distribution, Uncertainty, Quality & Survival
        "histogram" to ChartKind.HISTOGRAM,
        "box_plot" to ChartKind.BOX_PLOT,
        "strip_plot" to ChartKind.STRIP_PLOT,
        "error_bar_chart" to ChartKind.ERROR_BAR,
        "range_plot" to ChartKind.RANGE_PLOT,
        "forest_plot" to ChartKind.FOREST_PLOT,
        "control_chart" to ChartKind.CONTROL_CHART,
        "kaplan_meier_curve" to ChartKind.KAPLAN_MEIER,
        // Batch 4 Composition, Correlation & Specialized Views
        "pie_chart" to ChartKind.PIE,
        "donut_chart" to ChartKind.DONUT,
        "bubble_chart" to ChartKind.BUBBLE,
        "correlation_heatmap" to ChartKind.CORRELATION_HEATMAP,
        "waterfall_chart" to ChartKind.WATERFALL,
        "population_pyramid" to ChartKind.POPULATION_PYRAMID,
        // Final 4 Remaining Charts
        "violin_plot" to ChartKind.VIOLIN,
        "density_plot" to ChartKind.DENSITY,
        "treemap" to ChartKind.TREEMAP,
        "funnel_chart" to ChartKind.FUNNEL,
    )

    val supportedSpecIds: Set<String>
        get() = SUPPORTED_CHART_KINDS.keys

    fun chartKindFor(specId: String): ChartKind? = SUPPORTED_CHART_KINDS[specId]

    fun isSupported(specId: String): Boolean = SUPPORTED_CHART_KINDS.containsKey(specId)
}

sealed class PainterSupportResult {
    data class Supported(
        val specification: ChartSpecification,
        val chartKind: ChartKind,
    ) : PainterSupportResult()

    data class Unsupported(
        val specification: ChartSpecification,
        val reason: String,
    ) : PainterSupportResult()
}

/**
 * Resolves validated [ChartSpecification] recommendations into concrete [ChartPainter] /
 * [InteractiveChartPainter] implementations, or explicit [PainterSupportResult.Unsupported].
 */
object ChartPainterResolver {

    fun resolveSupport(specification: ChartSpecification): PainterSupportResult {
        val kind = ChartToolkitSupport.chartKindFor(specification.id)
        return if (kind != null) {
            PainterSupportResult.Supported(specification = specification, chartKind = kind)
        } else {
            PainterSupportResult.Unsupported(
                specification = specification,
                reason = "'${specification.canonicalName}' (${specification.id}) does not have an implemented Phase 3 drawing-toolkit renderer yet and will not be substituted with a misleading chart.",
            )
        }
    }

    fun createPainter(
        chartKind: ChartKind,
        axisAndGridPainter: AxisAndGridPainter,
        markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
    ): InteractiveChartPainter = when (chartKind) {
        ChartKind.LINE,
        ChartKind.MULTI_LINE -> LineChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.BAR,
        ChartKind.GROUPED_BAR,
        ChartKind.STACKED_BAR,
        ChartKind.PERCENT_STACKED_BAR -> BarChartPainter(axisAndGridPainter)
        ChartKind.SCATTER -> ScatterPlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.SLOPE -> SlopeChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.BUMP -> BumpChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.SPARKLINE_MULTIPLES -> SparklineMultiplesPainter(axisAndGridPainter.textMeasurer, markersPainter)
        ChartKind.RANGE_BAND -> RangeBandChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.CONNECTED_SCATTER -> ConnectedScatterPlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.CYCLE_PLOT -> CyclePlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.CALENDAR_HEATMAP -> CalendarHeatmapPainter(axisAndGridPainter.textMeasurer)
        ChartKind.BULLET -> BulletChartPainter(axisAndGridPainter.textMeasurer)
        ChartKind.DUMBBELL -> DumbbellChartPainter(axisAndGridPainter.textMeasurer, markersPainter)
        ChartKind.RADAR -> RadarChartPainter(axisAndGridPainter.textMeasurer, markersPainter)
        ChartKind.DIVERGING_BAR -> DivergingBarChartPainter(axisAndGridPainter)
        ChartKind.LIKERT_DIVERGING -> LikertDivergingChartPainter(axisAndGridPainter)
        ChartKind.WAFFLE -> WaffleChartPainter()
        ChartKind.PARETO -> ParetoChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.ECDF -> EcdfChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.QQ_PLOT -> QqPlotChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.STEM_AND_LEAF -> StemAndLeafPainter(axisAndGridPainter.textMeasurer)
        ChartKind.HEXBIN -> HexbinPlotPainter(axisAndGridPainter)
        ChartKind.SCATTERPLOT_MATRIX -> ScatterplotMatrixPainter(axisAndGridPainter.textMeasurer)
        ChartKind.MOSAIC -> MosaicPlotPainter(axisAndGridPainter.textMeasurer)
        ChartKind.SUNBURST -> SunburstChartPainter(axisAndGridPainter.textMeasurer)
        ChartKind.HORIZONTAL_BAR -> HorizontalBarChartPainter(axisAndGridPainter)
        ChartKind.LOLLIPOP -> LollipopChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.DOT_PLOT -> DotPlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.AREA -> AreaChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.STACKED_AREA -> StackedAreaChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.STEP -> StepChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.HISTOGRAM -> HistogramChartPainter(axisAndGridPainter)
        ChartKind.BOX_PLOT -> BoxPlotChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.STRIP_PLOT -> StripPlotChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.ERROR_BAR -> ErrorBarChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.RANGE_PLOT -> RangePlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.FOREST_PLOT -> ForestPlotPainter(axisAndGridPainter, markersPainter)
        ChartKind.CONTROL_CHART -> ControlChartPainter(axisAndGridPainter, markersPainter)
        ChartKind.KAPLAN_MEIER -> KaplanMeierChartPainter(axisAndGridPainter)
        ChartKind.PIE -> PieOrDonutChartPainter(axisAndGridPainter.textMeasurer, innerHoleFraction = 0f)
        ChartKind.DONUT -> PieOrDonutChartPainter(axisAndGridPainter.textMeasurer, innerHoleFraction = 0.56f)
        ChartKind.BUBBLE -> BubbleChartPainter(axisAndGridPainter)
        ChartKind.CORRELATION_HEATMAP -> CorrelationHeatmapPainter(axisAndGridPainter.textMeasurer)
        ChartKind.WATERFALL -> WaterfallChartPainter(axisAndGridPainter)
        ChartKind.POPULATION_PYRAMID -> PopulationPyramidPainter(axisAndGridPainter)
        ChartKind.VIOLIN -> ViolinPlotChartPainter(axisAndGridPainter)
        ChartKind.DENSITY -> DensityPlotChartPainter(axisAndGridPainter)
        ChartKind.TREEMAP -> TreemapChartPainter(axisAndGridPainter.textMeasurer)
        ChartKind.FUNNEL -> FunnelChartPainter(axisAndGridPainter.textMeasurer)
    }

    fun createPainter(
        specification: ChartSpecification,
        axisAndGridPainter: AxisAndGridPainter,
        markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
    ): InteractiveChartPainter? {
        val kind = ChartToolkitSupport.chartKindFor(specification.id) ?: return null
        return createPainter(kind, axisAndGridPainter, markersPainter)
    }
}
