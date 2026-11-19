package com.pasindu.nursingotapp.statgraph.chart.toolkit

import kotlin.math.abs

enum class ChartKind {
    LINE,
    MULTI_LINE,
    BAR,
    GROUPED_BAR,
    STACKED_BAR,
    PERCENT_STACKED_BAR,
    SCATTER,
    SLOPE,
    BUMP,
    SPARKLINE_MULTIPLES,
    RANGE_BAND,
    CONNECTED_SCATTER,
    CYCLE_PLOT,
    CALENDAR_HEATMAP,
    BULLET,
    DUMBBELL,
    RADAR,
    // Batch 1B Statistical & Distribution Charts
    DIVERGING_BAR,
    LIKERT_DIVERGING,
    WAFFLE,
    PARETO,
    ECDF,
    QQ_PLOT,
    STEM_AND_LEAF,
    HEXBIN,
    SCATTERPLOT_MATRIX,
    MOSAIC,
    SUNBURST,
    // Batch 2 Ranking, Trends & Comparison Extensions
    HORIZONTAL_BAR,
    LOLLIPOP,
    DOT_PLOT,
    AREA,
    STACKED_AREA,
    STEP,
    // Batch 3 Distribution, Uncertainty, Quality & Survival
    HISTOGRAM,
    BOX_PLOT,
    STRIP_PLOT,
    ERROR_BAR,
    RANGE_PLOT,
    FOREST_PLOT,
    CONTROL_CHART,
    KAPLAN_MEIER,
    // Batch 4 Composition, Correlation & Specialized Views
    PIE,
    DONUT,
    BUBBLE,
    CORRELATION_HEATMAP,
    WATERFALL,
    POPULATION_PYRAMID,
    // Final 4 Remaining Charts
    VIOLIN,
    DENSITY,
    TREEMAP,
    FUNNEL
}

enum class SeriesMarkerShape {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    DIAMOND
}

/**
 * Single observation rendered on a chart.
 *
 * Null [numericValue] (or [secondaryNumericValue]) represents missing data and must be
 * preserved as a gap rather than coerced to zero.
 */
data class ChartDatum(
    val domainIndex: Int,
    val domainLabel: String,
    val numericValue: Double?,
    val secondaryNumericValue: Double? = null,
    val lowerLimit: Double? = null,
    val upperLimit: Double? = null,
    val formattedValue: String = "",
    val unit: String = "",
    val qualitativeRanges: List<Double> = emptyList(),
    val cycleInstance: String = "",
    val isConflictFlagged: Boolean = false
)

data class StemLeafRowData(
    val stemLabel: String,
    val leaves: List<Int>,
)

data class HexbinCellData(
    val col: Int,
    val row: Int,
    val centerX: Double,
    val centerY: Double,
    val count: Int,
    val containsZeroMeasurement: Boolean = false,
)

data class SplomCellData(
    val rowVarIndex: Int,
    val colVarIndex: Int,
    val points: List<Pair<Double, Double>>,
    val missingPairCount: Int,
)

data class MosaicTileData(
    val rowLabel: String,
    val count: Double,
    val proportionInColumn: Double,
    val colorIndex: Int,
)

data class MosaicColumnData(
    val columnLabel: String,
    val columnTotal: Double,
    val widthProportion: Double,
    val tiles: List<MosaicTileData>,
)

data class SunburstNodeData(
    val id: String,
    val label: String,
    val level: Int,
    val parentId: String?,
    val value: Double,
    val colorIndex: Int,
)

data class BoxPlotGroupData(
    val groupLabel: String,
    val count: Int,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val lowerWhisker: Double,
    val upperWhisker: Double,
    val outliers: List<Double>,
    val colorIndex: Int = 0,
)

data class CorrelationCellData(
    val rowIndex: Int,
    val colIndex: Int,
    val coefficient: Double?,
    val pairedCount: Int,
)

data class ViolinGroupData(
    val groupLabel: String,
    val count: Int,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val kdePoints: List<Pair<Double, Double>>,
    val bandwidth: Double = 0.0,
    val isConstant: Boolean = false,
    val colorIndex: Int = 0,
)

data class TreemapTileData(
    val label: String,
    val groupLabel: String? = null,
    val value: Double,
    val share: Double,
    val colorIndex: Int = 0,
)

data class ChartDataSeries(
    val id: String,
    val name: String,
    val unit: String = "",
    val colorIndex: Int = 0,
    val markerShape: SeriesMarkerShape = SeriesMarkerShape.CIRCLE,
    val dashed: Boolean = false,
    val points: List<ChartDatum>
)

/**
 * Presentation-ready pure-Kotlin input model for [ChartPainter] implementations.
 * Completely independent of the analytical recommendation engine.
 */
data class ChartRenderData(
    val title: String,
    val subtitle: String = "",
    val chartKind: ChartKind,
    val domainAxisTitle: String = "",
    val valueAxisTitle: String = "",
    val valueUnit: String = "",
    val domainLabels: List<String>,
    val series: List<ChartDataSeries>,
    val enforceZeroBaseline: Boolean = when (chartKind) {
        ChartKind.BAR,
        ChartKind.HORIZONTAL_BAR,
        ChartKind.LOLLIPOP,
        ChartKind.GROUPED_BAR,
        ChartKind.STACKED_BAR,
        ChartKind.PERCENT_STACKED_BAR,
        ChartKind.AREA,
        ChartKind.STACKED_AREA,
        ChartKind.HISTOGRAM,
        ChartKind.DENSITY,
        ChartKind.BULLET,
        ChartKind.PARETO -> true
        else -> false
    },
    val minNumericOverride: Double? = null,
    val maxNumericOverride: Double? = null,
    val emptyMessage: String = "No valid observations available to plot.",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val useSharedScale: Boolean = true,
    val invertYAxis: Boolean = chartKind == ChartKind.BUMP,
    val isIntegerYAxis: Boolean = chartKind == ChartKind.BUMP,
    val chartWarnings: List<String> = emptyList(),
    val midpointValue: Double = 0.0,
    val stemAndLeafRows: List<StemLeafRowData> = emptyList(),
    val stemLeafKey: String = "",
    val hexbinCells: List<HexbinCellData> = emptyList(),
    val hexbinGridCols: Int = 6,
    val hexbinGridRows: Int = 6,
    val splomPanels: List<SplomCellData> = emptyList(),
    val splomVariableNames: List<String> = emptyList(),
    val mosaicColumns: List<MosaicColumnData> = emptyList(),
    val sunburstNodes: List<SunburstNodeData> = emptyList(),
    val boxPlotGroups: List<BoxPlotGroupData> = emptyList(),
    val correlationCells: List<CorrelationCellData> = emptyList(),
    val correlationVariableNames: List<String> = emptyList(),
    val violinGroups: List<ViolinGroupData> = emptyList(),
    val treemapTiles: List<TreemapTileData> = emptyList(),
    val preferredLabelMode: ChartLabelDisplayMode = ChartLabelDisplayMode.AUTO,
    val preferredBarColorMode: ChartBarColorMode = ChartBarColorMode.AUTO,
    val userExplicitlySelectedChart: Boolean = false,
) {
    val hasDrawablePoints: Boolean
        get() = errorMessage == null && !isLoading && when (chartKind) {
            ChartKind.STEM_AND_LEAF -> stemAndLeafRows.isNotEmpty()
            ChartKind.HEXBIN -> hexbinCells.isNotEmpty()
            ChartKind.SCATTERPLOT_MATRIX -> splomPanels.any { it.points.isNotEmpty() }
            ChartKind.MOSAIC -> mosaicColumns.isNotEmpty()
            ChartKind.SUNBURST -> sunburstNodes.isNotEmpty()
            ChartKind.CORRELATION_HEATMAP -> correlationCells.isNotEmpty()
            ChartKind.TREEMAP -> treemapTiles.isNotEmpty()
            ChartKind.VIOLIN -> violinGroups.isNotEmpty()
            ChartKind.BOX_PLOT -> boxPlotGroups.isNotEmpty() || series.any { s -> s.points.any { it.numericValue != null } }
            else -> series.any { s ->
                s.points.any { p ->
                    val primaryOk = p.numericValue != null && p.numericValue.isFinite()
                    when (chartKind) {
                        ChartKind.SCATTER,
                        ChartKind.CONNECTED_SCATTER,
                        ChartKind.ECDF,
                        ChartKind.QQ_PLOT,
                        ChartKind.KAPLAN_MEIER ->
                            primaryOk && p.secondaryNumericValue != null && p.secondaryNumericValue.isFinite()
                        ChartKind.DUMBBELL ->
                            primaryOk || (p.secondaryNumericValue != null && p.secondaryNumericValue.isFinite())
                        ChartKind.RANGE_PLOT,
                        ChartKind.FOREST_PLOT,
                        ChartKind.ERROR_BAR ->
                            primaryOk && p.lowerLimit != null && p.upperLimit != null && p.lowerLimit <= p.upperLimit
                        else -> primaryOk
                    }
                }
            }
        }
}

data class ResolvedChartGeometry(
    val outerRect: ChartRect,
    val plotRect: ChartRect,
    val yScale: NumericScale,
    val xCategoricalScale: CategoricalScale?,
    val xNumericScale: NumericScale?
)

data class HitTestMatch(
    val domainIndex: Int,
    val anchorPoint: ChartPoint,
    val domainLabel: String,
    val seriesEntries: List<HitTestSeriesValue>
)

data class HitTestSeriesValue(
    val seriesId: String,
    val seriesName: String,
    val colorIndex: Int,
    val markerShape: SeriesMarkerShape,
    val formattedValue: String,
    val isMissing: Boolean
)

/**
 * Pure-Kotlin coordinate resolver & hit-tester shared by painters and unit tests.
 */
object ChartGeometryResolver {

    fun resolve(
        outerRect: ChartRect,
        data: ChartRenderData,
        insets: ChartInsets
    ): ResolvedChartGeometry {
        val plotRect = outerRect.inset(insets)
        val zeroRequired = data.enforceZeroBaseline || data.chartKind in setOf(
            ChartKind.BAR,
            ChartKind.HORIZONTAL_BAR,
            ChartKind.LOLLIPOP,
            ChartKind.GROUPED_BAR,
            ChartKind.STACKED_BAR,
            ChartKind.PERCENT_STACKED_BAR,
            ChartKind.AREA,
            ChartKind.STACKED_AREA,
            ChartKind.HISTOGRAM,
            ChartKind.DENSITY,
            ChartKind.BULLET,
            ChartKind.PARETO
        )

        val yValues: List<Double?> = when (data.chartKind) {
            ChartKind.PERCENT_STACKED_BAR -> listOf(0.0, 100.0)
            ChartKind.ECDF, ChartKind.KAPLAN_MEIER -> listOf(0.0, 1.0)
            ChartKind.STACKED_BAR, ChartKind.STACKED_AREA -> computeStackedExtents(data)
            ChartKind.BOX_PLOT -> data.boxPlotGroups.flatMap { listOf(it.min, it.max, it.lowerWhisker, it.upperWhisker) }
                .ifEmpty { data.series.flatMap { s -> s.points.flatMap { listOfNotNull(it.numericValue, it.lowerLimit, it.upperLimit) } } }
            ChartKind.VIOLIN -> data.violinGroups.flatMap { g ->
                listOf(g.min, g.max) + g.kdePoints.map { it.first }
            }.ifEmpty { data.series.flatMap { s -> s.points.mapNotNull { it.numericValue } } }
            ChartKind.SCATTER, ChartKind.CONNECTED_SCATTER, ChartKind.QQ_PLOT, ChartKind.BUBBLE -> data.series.flatMap { s ->
                s.points.map { it.numericValue }
            }
            ChartKind.WATERFALL -> data.series.flatMap { s ->
                s.points.flatMap { listOfNotNull(it.numericValue, it.lowerLimit, it.upperLimit, 0.0) }
            }
            ChartKind.HEXBIN -> data.hexbinCells.map { it.centerY }
            ChartKind.RANGE_BAND -> data.series.flatMap { s ->
                s.points.flatMap { p ->
                    val lo = p.lowerLimit
                    val hi = p.upperLimit
                    if (lo != null && hi != null && lo <= hi) {
                        listOfNotNull(p.numericValue, lo, hi)
                    } else {
                        listOfNotNull(p.numericValue)
                    }
                }
            }
            else -> data.series.flatMap { s ->
                s.points.flatMap { p ->
                    listOfNotNull(p.numericValue, p.lowerLimit, p.upperLimit)
                }
            }
        }

        val invertY = data.invertYAxis || data.chartKind == ChartKind.BUMP
        val yScale = NumericScale.create(
            values = yValues,
            rangeStart = if (invertY) plotRect.top else plotRect.bottom,
            rangeEnd = if (invertY) plotRect.bottom else plotRect.top,
            config = AxisScaleConfig(
                includeZeroBaseline = if (data.chartKind == ChartKind.BUMP || data.chartKind == ChartKind.QQ_PLOT) false else zeroRequired,
                paddingFraction = when (data.chartKind) {
                    ChartKind.PERCENT_STACKED_BAR, ChartKind.ECDF, ChartKind.KAPLAN_MEIER -> 0.0
                    ChartKind.BUMP -> 0.05
                    else -> 0.08
                },
                desiredTickCount = 5
            ),
            minOverride = when (data.chartKind) {
                ChartKind.PERCENT_STACKED_BAR, ChartKind.ECDF, ChartKind.KAPLAN_MEIER -> 0.0
                ChartKind.BUMP -> 1.0
                else -> data.minNumericOverride
            },
            maxOverride = when (data.chartKind) {
                ChartKind.PERCENT_STACKED_BAR -> 100.0
                ChartKind.ECDF, ChartKind.KAPLAN_MEIER -> 1.0
                else -> data.maxNumericOverride
            }
        )

        val xNumericScale = when (data.chartKind) {
            ChartKind.SCATTER, ChartKind.CONNECTED_SCATTER, ChartKind.ECDF, ChartKind.QQ_PLOT, ChartKind.KAPLAN_MEIER, ChartKind.BUBBLE, ChartKind.DENSITY -> {
                val xVals = data.series.flatMap { s -> s.points.map { it.secondaryNumericValue } }
                val includeZero = data.chartKind == ChartKind.KAPLAN_MEIER
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = includeZero, paddingFraction = if (includeZero) 0.04 else 0.08, desiredTickCount = 5)
                )
            }
            ChartKind.HEXBIN -> {
                val xVals = data.hexbinCells.map { it.centerX }
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = false, paddingFraction = 0.08, desiredTickCount = 5)
                )
            }
            ChartKind.DIVERGING_BAR, ChartKind.POPULATION_PYRAMID -> {
                val xVals = data.series.flatMap { s -> s.points.map { it.numericValue } } + listOf(data.midpointValue)
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = false, paddingFraction = 0.08, desiredTickCount = 5),
                    minOverride = data.minNumericOverride,
                    maxOverride = data.maxNumericOverride
                )
            }
            ChartKind.LIKERT_DIVERGING -> {
                NumericScale.create(
                    values = listOf(-100.0, 0.0, 100.0),
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = true, paddingFraction = 0.0, desiredTickCount = 5),
                    minOverride = -100.0,
                    maxOverride = 100.0
                )
            }
            ChartKind.DUMBBELL -> {
                val xVals = data.series.flatMap { s ->
                    s.points.flatMap { listOfNotNull(it.numericValue, it.secondaryNumericValue) }
                }
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = data.enforceZeroBaseline, paddingFraction = 0.08, desiredTickCount = 5),
                    minOverride = data.minNumericOverride,
                    maxOverride = data.maxNumericOverride
                )
            }
            ChartKind.BULLET -> {
                val xVals = data.series.flatMap { s ->
                    s.points.flatMap { p ->
                        listOfNotNull(p.numericValue, p.secondaryNumericValue) + p.qualitativeRanges
                    }
                }
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = true, paddingFraction = 0.05, desiredTickCount = 5),
                    minOverride = data.minNumericOverride ?: 0.0,
                    maxOverride = data.maxNumericOverride
                )
            }
            ChartKind.HORIZONTAL_BAR, ChartKind.LOLLIPOP, ChartKind.DOT_PLOT -> {
                val xVals = data.series.flatMap { s -> s.points.map { it.numericValue } }
                val includeZero = data.chartKind == ChartKind.HORIZONTAL_BAR ||
                    data.chartKind == ChartKind.LOLLIPOP ||
                    data.enforceZeroBaseline
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = includeZero, paddingFraction = 0.08, desiredTickCount = 5),
                    minOverride = data.minNumericOverride,
                    maxOverride = data.maxNumericOverride
                )
            }
            ChartKind.RANGE_PLOT, ChartKind.FOREST_PLOT -> {
                val refList = if (data.chartKind == ChartKind.FOREST_PLOT) listOf(data.midpointValue) else emptyList()
                val xVals = data.series.flatMap { s ->
                    s.points.flatMap { listOfNotNull(it.numericValue, it.lowerLimit, it.upperLimit) }
                } + refList
                NumericScale.create(
                    values = xVals,
                    rangeStart = plotRect.left,
                    rangeEnd = plotRect.right,
                    config = AxisScaleConfig(includeZeroBaseline = false, paddingFraction = 0.08, desiredTickCount = 5),
                    minOverride = data.minNumericOverride,
                    maxOverride = data.maxNumericOverride
                )
            }
            else -> null
        }

        val xCategoricalScale = if (data.chartKind !in setOf(
                ChartKind.SCATTER,
                ChartKind.CONNECTED_SCATTER,
                ChartKind.BUBBLE,
                ChartKind.ECDF,
                ChartKind.QQ_PLOT,
                ChartKind.HEXBIN,
                ChartKind.KAPLAN_MEIER,
            )
        ) {
            val labels = if (data.domainLabels.isNotEmpty()) {
                data.domainLabels
            } else {
                data.series.firstOrNull()?.points?.map { it.domainLabel }.orEmpty()
            }
            CategoricalScale(
                labels = labels,
                rangeStart = plotRect.left,
                rangeEnd = plotRect.right,
                innerPaddingFraction = when (data.chartKind) {
                    ChartKind.BAR, ChartKind.GROUPED_BAR, ChartKind.STACKED_BAR, ChartKind.PERCENT_STACKED_BAR, ChartKind.PARETO -> 0.28f
                    ChartKind.SLOPE -> 0.22f
                    ChartKind.CYCLE_PLOT -> 0.14f
                    else -> 0.10f
                }
            )
        } else {
            null
        }

        return ResolvedChartGeometry(
            outerRect = outerRect,
            plotRect = plotRect,
            yScale = yScale,
            xCategoricalScale = xCategoricalScale,
            xNumericScale = xNumericScale
        )
    }

    fun hitTest(
        touchX: Float,
        touchY: Float,
        data: ChartRenderData,
        geometry: ResolvedChartGeometry
    ): HitTestMatch? {
        if (!data.hasDrawablePoints || !geometry.plotRect.isValid) return null

        if (data.chartKind == ChartKind.SCATTER || data.chartKind == ChartKind.CONNECTED_SCATTER) {
            val xScale = geometry.xNumericScale ?: return null
            var bestSeriesIdx = -1
            var bestPoint: ChartDatum? = null
            var bestAnchor: ChartPoint? = null
            var bestDistSq = Float.MAX_VALUE

            data.series.forEachIndexed { sIdx, s ->
                s.points.forEach { p ->
                    val xv = p.secondaryNumericValue ?: return@forEach
                    val yv = p.numericValue ?: return@forEach
                    val px = xScale.toPixel(xv) ?: return@forEach
                    val py = geometry.yScale.toPixel(yv) ?: return@forEach
                    val dx = px - touchX
                    val dy = py - touchY
                    val dSq = dx * dx + dy * dy
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq
                        bestSeriesIdx = sIdx
                        bestPoint = p
                        bestAnchor = ChartPoint(px, py)
                    }
                }
            }

            val hit = bestPoint ?: return null
            val series = data.series[bestSeriesIdx]
            return HitTestMatch(
                domainIndex = hit.domainIndex,
                anchorPoint = bestAnchor!!,
                domainLabel = hit.domainLabel.ifBlank { "Point ${hit.domainIndex + 1}" },
                seriesEntries = listOf(
                    HitTestSeriesValue(
                        seriesId = series.id,
                        seriesName = series.name,
                        colorIndex = series.colorIndex,
                        markerShape = series.markerShape,
                        formattedValue = hit.formattedValue.ifBlank {
                            "${AxisTickCalculator.formatTick(hit.secondaryNumericValue ?: 0.0, 0.1)}, " +
                                AxisTickCalculator.formatTick(hit.numericValue ?: 0.0, 0.1)
                        },
                        isMissing = false
                    )
                )
            )
        }

        if (ChartDensityDiagnostics.isHorizontalCategoryFamily(data.chartKind) && data.domainLabels.isNotEmpty()) {
            val rowCount = data.domainLabels.size
            val rowHeight = (geometry.plotRect.height / rowCount.toFloat()).coerceAtLeast(1f)
            val rowIndex = (((touchY - geometry.plotRect.top) / rowHeight).toInt()).coerceIn(0, rowCount - 1)
            val cy = geometry.plotRect.top + (rowIndex + 0.5f) * rowHeight
            val domainLabel = data.domainLabels[rowIndex]
            val xScale = geometry.xNumericScale
            var bestAnchorX: Float? = null
            val entries = data.series.map { s ->
                val datum = s.points.firstOrNull { it.domainIndex == rowIndex }
                val numVal = datum?.numericValue?.takeIf { it.isFinite() }
                if (numVal != null && bestAnchorX == null) {
                    bestAnchorX = xScale?.toPixel(numVal)
                }
                val displayVal = when {
                    datum == null || numVal == null -> "Missing"
                    datum.formattedValue.isNotBlank() -> datum.formattedValue
                    else -> buildString {
                        append(AxisTickCalculator.formatTick(numVal, 0.1))
                        val effectiveUnit = datum.unit.ifBlank { s.unit.ifBlank { data.valueUnit } }
                        if (effectiveUnit.isNotBlank()) {
                            if (effectiveUnit == "%") append("%") else append(" $effectiveUnit")
                        }
                    }
                }
                HitTestSeriesValue(
                    seriesId = s.id,
                    seriesName = s.name,
                    colorIndex = s.colorIndex,
                    markerShape = s.markerShape,
                    formattedValue = displayVal,
                    isMissing = numVal == null,
                )
            }
            return HitTestMatch(
                domainIndex = rowIndex,
                anchorPoint = ChartPoint(bestAnchorX ?: geometry.plotRect.centerX, cy),
                domainLabel = domainLabel,
                seriesEntries = entries,
            )
        }

        val catScale = geometry.xCategoricalScale ?: return null
        val domainIndex = catScale.nearestIndex(touchX) ?: return null
        val anchorX = catScale.pointX(domainIndex) ?: return null
        val domainLabel = data.domainLabels.getOrElse(domainIndex) {
            data.series.firstOrNull()?.points?.firstOrNull { it.domainIndex == domainIndex }?.domainLabel ?: "#${domainIndex + 1}"
        }

        val entries = mutableListOf<HitTestSeriesValue>()
        var bestAnchorY: Float? = null
        var closestYDist = Float.MAX_VALUE

        data.series.forEach { s ->
            val datum = s.points.firstOrNull { it.domainIndex == domainIndex }
            val numVal = datum?.numericValue?.takeIf { it.isFinite() }
            if (numVal != null) {
                val py = geometry.yScale.toPixel(numVal)
                if (py != null) {
                    val dist = abs(py - touchY)
                    if (dist < closestYDist) {
                        closestYDist = dist
                        bestAnchorY = py
                    }
                }
            }
            val displayVal = when {
                datum == null || numVal == null -> "Missing"
                datum.formattedValue.isNotBlank() -> datum.formattedValue
                else -> buildString {
                    append(AxisTickCalculator.formatTick(numVal, 0.1))
                    val effectiveUnit = datum.unit.ifBlank { s.unit.ifBlank { data.valueUnit } }
                    if (effectiveUnit.isNotBlank()) {
                        if (effectiveUnit == "%") append("%") else append(" $effectiveUnit")
                    }
                }
            }
            entries.add(
                HitTestSeriesValue(
                    seriesId = s.id,
                    seriesName = s.name,
                    colorIndex = s.colorIndex,
                    markerShape = s.markerShape,
                    formattedValue = displayVal,
                    isMissing = numVal == null
                )
            )
        }

        return HitTestMatch(
            domainIndex = domainIndex,
            anchorPoint = ChartPoint(anchorX, bestAnchorY ?: geometry.plotRect.centerY),
            domainLabel = domainLabel,
            seriesEntries = entries
        )
    }

    fun buildAccessibleSummary(data: ChartRenderData): String {
        val chartTypeLabel = when (data.chartKind) {
            ChartKind.LINE -> "Line chart"
            ChartKind.MULTI_LINE -> "Multi-series line chart"
            ChartKind.BAR -> "Bar chart"
            ChartKind.GROUPED_BAR -> "Grouped bar chart"
            ChartKind.STACKED_BAR -> "Stacked bar chart"
            ChartKind.PERCENT_STACKED_BAR -> "100% stacked bar chart"
            ChartKind.SCATTER -> "Scatter plot"
            ChartKind.SLOPE -> "Slope chart"
            ChartKind.BUMP -> "Bump chart"
            ChartKind.SPARKLINE_MULTIPLES -> "Sparkline small multiples"
            ChartKind.RANGE_BAND -> "Range band chart"
            ChartKind.CONNECTED_SCATTER -> "Connected scatter plot"
            ChartKind.CYCLE_PLOT -> "Cycle plot"
            ChartKind.CALENDAR_HEATMAP -> "Calendar heatmap"
            ChartKind.BULLET -> "Bullet chart"
            ChartKind.DUMBBELL -> "Dumbbell chart"
            ChartKind.RADAR -> "Radar chart"
            ChartKind.DIVERGING_BAR -> "Diverging bar chart"
            ChartKind.LIKERT_DIVERGING -> "Likert stacked diverging chart"
            ChartKind.WAFFLE -> "Waffle chart"
            ChartKind.PARETO -> "Pareto chart"
            ChartKind.ECDF -> "Empirical cumulative distribution function (ECDF)"
            ChartKind.QQ_PLOT -> "Quantile-quantile (Q-Q) plot"
            ChartKind.STEM_AND_LEAF -> "Stem-and-leaf plot"
            ChartKind.HEXBIN -> "Hexbin plot"
            ChartKind.SCATTERPLOT_MATRIX -> "Scatterplot matrix"
            ChartKind.MOSAIC -> "Mosaic plot"
            ChartKind.SUNBURST -> "Sunburst chart"
            ChartKind.HORIZONTAL_BAR -> "Horizontal bar chart"
            ChartKind.LOLLIPOP -> "Lollipop chart"
            ChartKind.DOT_PLOT -> "Dot plot"
            ChartKind.AREA -> "Area chart"
            ChartKind.STACKED_AREA -> "Stacked area chart"
            ChartKind.STEP -> "Step chart"
            ChartKind.HISTOGRAM -> "Histogram"
            ChartKind.BOX_PLOT -> "Box plot"
            ChartKind.STRIP_PLOT -> "Strip plot"
            ChartKind.ERROR_BAR -> "Error bar chart"
            ChartKind.RANGE_PLOT -> "Range plot"
            ChartKind.FOREST_PLOT -> "Forest plot"
            ChartKind.CONTROL_CHART -> "Control chart"
            ChartKind.KAPLAN_MEIER -> "Kaplan-Meier survival curve"
            ChartKind.PIE -> "Pie chart"
            ChartKind.DONUT -> "Donut chart"
            ChartKind.BUBBLE -> "Bubble chart"
            ChartKind.CORRELATION_HEATMAP -> "Correlation heatmap"
            ChartKind.WATERFALL -> "Waterfall chart"
            ChartKind.POPULATION_PYRAMID -> "Population pyramid"
            ChartKind.VIOLIN -> "Violin plot"
            ChartKind.DENSITY -> "Density plot"
            ChartKind.TREEMAP -> "Treemap"
            ChartKind.FUNNEL -> "Funnel chart"
        }
        val titlePrefix = if (data.title.isNotBlank()) "$chartTypeLabel: ${data.title}" else chartTypeLabel
        if (data.errorMessage != null) return "$titlePrefix. Error: ${data.errorMessage}"
        if (!data.hasDrawablePoints) return "$titlePrefix. ${data.emptyMessage}"

        val rangeDesc = when {
            data.domainLabels.size >= 2 ->
                "${data.domainLabels.size} categories or periods, from ${data.domainLabels.first()} to ${data.domainLabels.last()}"
            data.domainLabels.size == 1 ->
                "1 category or period: ${data.domainLabels.first()}"
            else -> "${data.domainLabels.size} observations"
        }

        val seriesDesc = data.series.joinToString("; ") { s ->
            val validPts = s.points.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() } }
            val minStr = validPts.minOrNull()?.let { AxisTickCalculator.formatTick(it, 0.1) } ?: "unavailable"
            val maxStr = validPts.maxOrNull()?.let { AxisTickCalculator.formatTick(it, 0.1) } ?: "unavailable"
            val latestStr = validPts.lastOrNull()?.let { AxisTickCalculator.formatTick(it, 0.1) }
            val unitSuffix = if (s.unit.isNotBlank()) " in ${s.unit}" else ""
            val missingCount = s.points.count { it.numericValue == null || !it.numericValue.isFinite() }
            val missNote = if (missingCount > 0) ", $missingCount missing (not zero)" else ""
            val latestNote = if (latestStr != null) ", latest $latestStr" else ""
            "${s.name}$unitSuffix (min $minStr, max $maxStr$latestNote$missNote)"
        }

        val subtitleNote = if (data.subtitle.isNotBlank()) " Note: ${data.subtitle}." else ""
        return "$titlePrefix. $rangeDesc. Series: $seriesDesc.$subtitleNote"
    }

    private fun computeStackedExtents(data: ChartRenderData): List<Double> {
        val posTotals = mutableMapOf<Int, Double>()
        val negTotals = mutableMapOf<Int, Double>()
        data.series.forEach { s ->
            s.points.forEach { p ->
                val v = p.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                if (v >= 0.0) {
                    posTotals[p.domainIndex] = (posTotals[p.domainIndex] ?: 0.0) + v
                } else {
                    negTotals[p.domainIndex] = (negTotals[p.domainIndex] ?: 0.0) + v
                }
            }
        }
        return (posTotals.values + negTotals.values + listOf(0.0)).toList()
    }
}
