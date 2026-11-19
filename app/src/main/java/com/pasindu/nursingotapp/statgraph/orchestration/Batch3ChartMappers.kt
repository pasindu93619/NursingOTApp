package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.BoxPlotGroupData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.StatisticalMath
import kotlin.math.sin

/**
 * Pure-Kotlin mappers for Batch 3 Distribution, Uncertainty, Quality & Survival Charts:
 * Histogram, Box Plot, Strip Plot, Error Bar Chart, Range Plot, Forest Plot,
 * Control Chart (I-MR), and Kaplan-Meier Survival Curve.
 */
internal object Batch3ChartMappers {

    fun mapHistogram(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Histogram requires at least one numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds || it.id == selection.xFieldId }
            ?: numericCols.first()

        var missingCount = 0
        val validValues = mutableListOf<Double>()
        dataset.rows.forEachIndexed { idx, row ->
            val v = yCol.extractor(idx, row).value?.takeIf { it.isFinite() }
            if (v == null) missingCount++ else validValues.add(v)
        }
        if (validValues.size < 2) {
            return MappingOutcome.InvalidMapping("Histogram requires at least 2 valid numeric observations.")
        }

        val hist = StatisticalMath.computeHistogram(validValues)
        if (hist.bins.isEmpty()) {
            return MappingOutcome.InvalidMapping("Could not compute histogram bins for '${yCol.name}'.")
        }

        val domainLabels = hist.bins.map { bin ->
            val loStr = DatasetToChartRenderDataMapper.formatPlainNumber(bin.lowerBound)
            val hiStr = DatasetToChartRenderDataMapper.formatPlainNumber(bin.upperBound)
            val closing = if (bin.isLastInclusive) "]" else ")"
            "[$loStr, $hiStr$closing"
        }

        val pts = hist.bins.map { bin ->
            ChartDatum(
                domainIndex = bin.index,
                domainLabel = domainLabels[bin.index],
                numericValue = bin.count.toDouble(),
                lowerLimit = bin.lowerBound,
                upperLimit = bin.upperBound,
                formattedValue = "${bin.count} obs",
                unit = "obs",
            )
        }

        val series = ChartDataSeries(
            id = "hist_0",
            name = "${yCol.name} frequency",
            unit = "obs",
            colorIndex = 0,
            markerShape = SeriesMarkerShape.SQUARE,
            points = pts,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.HISTOGRAM,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            add("Equal-width binning (${hist.ruleUsed.name.replace('_', ' ')}): ${hist.bins.size} bins, width = ${DatasetToChartRenderDataMapper.formatPlainNumber(hist.binWidth)}.")
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Distribution Histogram",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.HISTOGRAM,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueAxisTitle = "Observations (count)",
            valueUnit = "obs",
            domainLabels = domainLabels,
            series = listOf(series),
            enforceZeroBaseline = true,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = yCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, series.name, "obs", 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapBoxPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Box plot requires at least one numeric field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, null, selection, allowCategoryFallback = true)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)

        val boxGroups = mutableListOf<BoxPlotGroupData>()
        var missingCount = 0

        if (groupField != null && groupNames.size >= 2) {
            groupNames.forEachIndexed { gIdx, gName ->
                val vals = mutableListOf<Double>()
                dataset.rows.forEachIndexed { rIdx, row ->
                    val rGroup = DatasetToChartRenderDataMapper.extractGroupValue(row, groupField)
                    if (rGroup == gName) {
                        val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                        if (v == null) missingCount++ else vals.add(v)
                    }
                }
                val summary = StatisticalMath.computeBoxPlot(vals)
                if (summary != null) {
                    boxGroups.add(
                        BoxPlotGroupData(
                            groupLabel = gName,
                            count = summary.count,
                            min = summary.min,
                            q1 = summary.q1,
                            median = summary.median,
                            q3 = summary.q3,
                            max = summary.max,
                            lowerWhisker = summary.lowerWhisker,
                            upperWhisker = summary.upperWhisker,
                            outliers = summary.outliers,
                            colorIndex = gIdx,
                        ),
                    )
                }
            }
        } else {
            val colsToSummarize = if (selection.yFieldIds.size >= 2) {
                numericCols.filter { it.id in selection.yFieldIds }
            } else {
                listOf(yCol)
            }
            colsToSummarize.forEachIndexed { cIdx, col ->
                val vals = mutableListOf<Double>()
                dataset.rows.forEachIndexed { rIdx, row ->
                    val v = col.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                    if (v == null) missingCount++ else vals.add(v)
                }
                val summary = StatisticalMath.computeBoxPlot(vals)
                if (summary != null) {
                    boxGroups.add(
                        BoxPlotGroupData(
                            groupLabel = col.name,
                            count = summary.count,
                            min = summary.min,
                            q1 = summary.q1,
                            median = summary.median,
                            q3 = summary.q3,
                            max = summary.max,
                            lowerWhisker = summary.lowerWhisker,
                            upperWhisker = summary.upperWhisker,
                            outliers = summary.outliers,
                            colorIndex = cIdx,
                        ),
                    )
                }
            }
        }

        if (boxGroups.isEmpty()) {
            return MappingOutcome.InvalidMapping("Box plot requires valid numeric observations to compute quartiles.")
        }

        val domainLabels = boxGroups.map { it.groupLabel }
        val pts = boxGroups.mapIndexed { idx, grp ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = grp.groupLabel,
                numericValue = grp.median,
                lowerLimit = grp.q1,
                upperLimit = grp.q3,
                qualitativeRanges = listOf(grp.lowerWhisker, grp.upperWhisker) + grp.outliers,
                formattedValue = "Median ${DatasetToChartRenderDataMapper.formatPlainNumber(grp.median)} (IQR ${DatasetToChartRenderDataMapper.formatPlainNumber(grp.q1)}–${DatasetToChartRenderDataMapper.formatPlainNumber(grp.q3)}, n=${grp.count})",
                unit = yCol.unit,
            )
        }
        val series = ChartDataSeries(
            id = "box_0",
            name = yCol.name,
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.SQUARE,
            points = pts,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.BOX_PLOT,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(yCol.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            add("Tukey box plot: box spans Q1–Q3 (IQR), center bar is median, whiskers extend up to 1.5×IQR, and points beyond fences are plotted individually.")
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Box Plot",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.BOX_PLOT,
            domainAxisTitle = groupField?.name ?: "Variable",
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            boxPlotGroups = boxGroups,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = groupField ?: yCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, series.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapStripPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Strip plot requires at least one numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, null, selection, allowCategoryFallback = true)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
        val domainLabels = if (groupField != null && groupNames.isNotEmpty()) groupNames else listOf(yCol.name)

        var missingCount = 0
        val pts = mutableListOf<ChartDatum>()

        dataset.rows.forEachIndexed { rIdx, row ->
            val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
            if (v == null) {
                missingCount++
                return@forEachIndexed
            }
            val gLabel = if (groupField != null && groupNames.isNotEmpty()) {
                DatasetToChartRenderDataMapper.extractGroupValue(row, groupField) ?: return@forEachIndexed
            } else {
                yCol.name
            }
            val dIdx = domainLabels.indexOf(gLabel).coerceAtLeast(0)
            // Deterministic jitter in [-0.75, +0.75] based on row index so tied observations never hide each other
            val jitter = if (dataset.rows.size <= 1) 0.0 else sin((rIdx + 1) * 2.399963229728653) * 0.72
            pts.add(
                ChartDatum(
                    domainIndex = dIdx,
                    domainLabel = gLabel,
                    numericValue = v,
                    secondaryNumericValue = jitter,
                    formattedValue = DatasetToChartRenderDataMapper.formatCellValue(v, yCol.unit, ""),
                    unit = yCol.unit,
                ),
            )
        }

        if (pts.isEmpty()) {
            return MappingOutcome.InvalidMapping("Strip plot has no valid numeric observations.")
        }

        val series = ChartDataSeries(
            id = "strip_0",
            name = yCol.name,
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.CIRCLE,
            points = pts,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.STRIP_PLOT,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(yCol.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        )
        val renderData = ChartRenderData(
            title = "${yCol.name} — Strip Plot",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.STRIP_PLOT,
            domainAxisTitle = groupField?.name ?: "Observation Group",
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = groupField ?: yCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, series.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapErrorBar(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome = mapIntervalChart(
        dataset = dataset,
        specification = specification,
        chartKind = ChartKind.ERROR_BAR,
        diagnosticsReport = diagnosticsReport,
        derivedResults = derivedResults,
        selection = selection,
    )

    fun mapRangePlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome = mapIntervalChart(
        dataset = dataset,
        specification = specification,
        chartKind = ChartKind.RANGE_PLOT,
        diagnosticsReport = diagnosticsReport,
        derivedResults = derivedResults,
        selection = selection,
    )

    fun mapForestPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome = mapIntervalChart(
        dataset = dataset,
        specification = specification,
        chartKind = ChartKind.FOREST_PLOT,
        diagnosticsReport = diagnosticsReport,
        derivedResults = derivedResults,
        selection = selection,
    )

    private fun mapIntervalChart(
        dataset: StatDataset,
        specification: ChartSpecification,
        chartKind: ChartKind,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, chartKind, selection)
            ?: return MappingOutcome.InvalidMapping("${specification.canonicalName} requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val estimateField = dataset.fields.firstOrNull { it.role is FieldRole.EstimateWithLimits }
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val domainLabels = orderedRows.map { it.displayLabel }

        var missingCount = 0
        var invalidBoundsCount = 0
        val points = mutableListOf<ChartDatum>()
        val unit: String
        val measureName: String
        val rawField: StatField?

        if (estimateField != null) {
            unit = estimateField.unit
            measureName = estimateField.name
            rawField = estimateField
            orderedRows.forEachIndexed { idx, indexed ->
                val raw = indexed.row.values[estimateField.id]
                if (raw is StatValue.EstimateValue) {
                    val est = raw.estimate?.takeIf { it.isFinite() }
                    val lo = raw.lowerLimit?.takeIf { it.isFinite() }
                    val hi = raw.upperLimit?.takeIf { it.isFinite() }
                    val validation = if (est != null && lo != null && hi != null) {
                        StatisticalMath.validateIntervalBounds(est, lo, hi)
                    } else {
                        StatisticalMath.IntervalBoundValidation(false)
                    }
                    if (est != null && lo != null && hi != null && !validation.isValid) {
                        invalidBoundsCount++
                    }
                    if (!validation.isValid) missingCount++
                    points.add(
                        ChartDatum(
                            domainIndex = idx,
                            domainLabel = indexed.displayLabel,
                            numericValue = if (validation.isValid) est else null,
                            lowerLimit = if (validation.isValid) lo else null,
                            upperLimit = if (validation.isValid) hi else null,
                            formattedValue = if (validation.isValid && est != null && lo != null && hi != null) {
                                "${DatasetToChartRenderDataMapper.formatPlainNumber(est)} (${DatasetToChartRenderDataMapper.formatPlainNumber(lo)}–${DatasetToChartRenderDataMapper.formatPlainNumber(hi)})"
                            } else {
                                "Missing"
                            },
                            unit = unit,
                        ),
                    )
                } else {
                    missingCount++
                    points.add(ChartDatum(idx, indexed.displayLabel, null, formattedValue = "Missing", unit = unit))
                }
            }
        } else if (numericCols.size >= 3) {
            val central = numericCols[0]
            val lower = numericCols[1]
            val upper = numericCols[2]
            unit = central.unit
            measureName = central.name
            rawField = central.rawField
            orderedRows.forEachIndexed { idx, indexed ->
                val est = central.extractor(indexed.originalIndex, indexed.row).value
                val lo = lower.extractor(indexed.originalIndex, indexed.row).value
                val hi = upper.extractor(indexed.originalIndex, indexed.row).value
                val validation = if (est != null && lo != null && hi != null) {
                    StatisticalMath.validateIntervalBounds(est, lo, hi)
                } else {
                    StatisticalMath.IntervalBoundValidation(false)
                }
                if (est != null && lo != null && hi != null && !validation.isValid) invalidBoundsCount++
                if (!validation.isValid) missingCount++
                points.add(
                    ChartDatum(
                        domainIndex = idx,
                        domainLabel = indexed.displayLabel,
                        numericValue = if (validation.isValid) est else null,
                        lowerLimit = if (validation.isValid) lo else null,
                        upperLimit = if (validation.isValid) hi else null,
                        formattedValue = if (validation.isValid && est != null && lo != null && hi != null) {
                            "${DatasetToChartRenderDataMapper.formatPlainNumber(est)} (${DatasetToChartRenderDataMapper.formatPlainNumber(lo)}–${DatasetToChartRenderDataMapper.formatPlainNumber(hi)})"
                        } else {
                            "Missing"
                        },
                        unit = unit,
                    ),
                )
            }
        } else {
            return MappingOutcome.InvalidMapping(
                "${specification.canonicalName} requires an Estimate-with-Limits field or 3 numeric fields (Estimate, Lower, Upper).",
            )
        }

        if (points.none { it.numericValue != null && it.lowerLimit != null && it.upperLimit != null }) {
            return MappingOutcome.InvalidMapping(
                "${specification.canonicalName} has no valid intervals satisfying lower <= estimate <= upper.",
            )
        }

        // For forest plot, use 1.0 null reference line when all lower bounds > 0 and ratio-like, else 0.0
        val validLo = points.mapNotNull { it.lowerLimit }
        val validHi = points.mapNotNull { it.upperLimit }
        val isRatioEffect = validLo.isNotEmpty() && validLo.all { it > 0.0 } &&
            (measureName.contains("OR", ignoreCase = true) ||
                measureName.contains("RR", ignoreCase = true) ||
                measureName.contains("HR", ignoreCase = true) ||
                measureName.contains("ratio", ignoreCase = true) ||
                (validLo.min() <= 1.5 && validHi.max() >= 0.5 && validHi.max() <= 15.0))
        val nullRef = if (chartKind == ChartKind.FOREST_PLOT && isRatioEffect) 1.0 else 0.0

        val series = ChartDataSeries(
            id = "interval_0",
            name = measureName,
            unit = unit,
            colorIndex = 0,
            markerShape = if (chartKind == ChartKind.FOREST_PLOT) SeriesMarkerShape.SQUARE else SeriesMarkerShape.CIRCLE,
            points = points,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = chartKind,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(domainField.id, rawField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = emptyList(),
        ).toMutableList().apply {
            if (invalidBoundsCount > 0) {
                add("$invalidBoundsCount row(s) with inverted bounds (lower > upper or estimate outside bounds) were excluded without silent bound swapping.")
            }
        }

        val renderData = ChartRenderData(
            title = "${dataset.name.ifBlank { measureName }} — ${specification.canonicalName}",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = chartKind,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(measureName, unit),
            valueUnit = unit,
            domainLabels = domainLabels,
            series = listOf(series),
            midpointValue = nullRef,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding(series.id, rawField?.id ?: "est", false, measureName, unit, 0, series.markerShape, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapControlChart(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.CONTROL_CHART, selection)
            ?: return MappingOutcome.InvalidMapping("Control chart requires an ordered Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Control chart requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val domainLabels = orderedRows.map { it.displayLabel }

        var missingCount = 0
        val rawValues = orderedRows.map { entry ->
            val v = yCol.extractor(entry.originalIndex, entry.row).value?.takeIf { it.isFinite() }
            if (v == null) missingCount++
            v
        }

        val imr = StatisticalMath.computeImrChart(rawValues)
            ?: return MappingOutcome.InvalidMapping("Control chart requires at least 2 valid observations to estimate moving-range limits.")
        val ind = imr.individuals

        val points = ind.points.mapIndexed { idx, spcPt ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = domainLabels[idx],
                numericValue = spcPt.value,
                secondaryNumericValue = spcPt.centerLine,
                lowerLimit = spcPt.lcl,
                upperLimit = spcPt.ucl,
                isConflictFlagged = spcPt.isOutOfControl,
                formattedValue = spcPt.value?.let {
                    val base = DatasetToChartRenderDataMapper.formatCellValue(it, yCol.unit, "")
                    if (spcPt.isOutOfControl) "$base (Out of 3σ limits)" else base
                } ?: "Missing",
                unit = yCol.unit,
            )
        }

        val series = ChartDataSeries(
            id = "spc_0",
            name = yCol.name,
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.CIRCLE,
            points = points,
        )
        val oocCount = ind.points.count { it.isOutOfControl }
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.CONTROL_CHART,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            val clStr = DatasetToChartRenderDataMapper.formatPlainNumber(ind.centerLine)
            val lclStr = DatasetToChartRenderDataMapper.formatPlainNumber(ind.points.first().lcl)
            val uclStr = DatasetToChartRenderDataMapper.formatPlainNumber(ind.points.first().ucl)
            add("Shewhart I-MR control limits: CL = $clStr, LCL = $lclStr, UCL = $uclStr ($oocCount point(s) beyond 3σ).")
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Control Chart (I-MR)",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.CONTROL_CHART,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, series.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapKaplanMeier(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val durationField = dataset.fields.firstOrNull { it.role is FieldRole.DurationWithEvent }
            ?: return MappingOutcome.InvalidMapping("Kaplan-Meier curve requires a Duration-with-Event field (duration >= 0 and event/censor indicator).")
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, durationField.id, selection, allowCategoryFallback = true)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        var missingCount = 0
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()

        fun extractObsForRows(rows: List<com.pasindu.nursingotapp.statgraph.model.StatRow>): List<StatisticalMath.SurvivalObservation> {
            val obs = mutableListOf<StatisticalMath.SurvivalObservation>()
            rows.forEach { row ->
                val dv = row.values[durationField.id] as? StatValue.DurationEventValue
                val dur = dv?.duration?.takeIf { it.isFinite() && it >= 0.0 }
                val ev = dv?.eventOccurred
                if (dur != null && ev != null) {
                    obs.add(StatisticalMath.SurvivalObservation(dur, ev))
                } else {
                    missingCount++
                }
            }
            return obs
        }

        if (groupField != null && groupNames.size >= 2) {
            groupNames.forEachIndexed { gIdx, gName ->
                val gRows = dataset.rows.filter { DatasetToChartRenderDataMapper.extractGroupValue(it, groupField) == gName }
                val steps = StatisticalMath.computeKaplanMeier(extractObsForRows(gRows))
                if (steps.isNotEmpty()) {
                    val pts = steps.mapIndexed { sIdx, step ->
                        val survPct = DatasetToChartRenderDataMapper.formatPlainNumber(step.survival * 100.0)
                        ChartDatum(
                            domainIndex = sIdx,
                            domainLabel = "t=${DatasetToChartRenderDataMapper.formatPlainNumber(step.time)}",
                            numericValue = step.survival,
                            secondaryNumericValue = step.time,
                            lowerLimit = step.ciLower95,
                            upperLimit = step.ciUpper95,
                            qualitativeRanges = listOf(step.events.toDouble(), step.censored.toDouble(), step.atRisk.toDouble()),
                            formattedValue = "S(t)=$survPct% (at risk=${step.atRisk}, events=${step.events}, censored=${step.censored})",
                            unit = durationField.unit,
                        )
                    }
                    val shape = markerRotation[gIdx % markerRotation.size]
                    seriesList.add(ChartDataSeries("km_$gIdx", gName, "", gIdx, shape, dashed = gIdx % 2 == 1, points = pts))
                    bindings.add(ResolvedSeriesBinding("km_$gIdx", durationField.id, false, gName, "", gIdx, shape, gIdx % 2 == 1))
                }
            }
        } else {
            val steps = StatisticalMath.computeKaplanMeier(extractObsForRows(dataset.rows))
            if (steps.isNotEmpty()) {
                val pts = steps.mapIndexed { sIdx, step ->
                    val survPct = DatasetToChartRenderDataMapper.formatPlainNumber(step.survival * 100.0)
                    ChartDatum(
                        domainIndex = sIdx,
                        domainLabel = "t=${DatasetToChartRenderDataMapper.formatPlainNumber(step.time)}",
                        numericValue = step.survival,
                        secondaryNumericValue = step.time,
                        lowerLimit = step.ciLower95,
                        upperLimit = step.ciUpper95,
                        qualitativeRanges = listOf(step.events.toDouble(), step.censored.toDouble(), step.atRisk.toDouble()),
                        formattedValue = "S(t)=$survPct% (at risk=${step.atRisk}, events=${step.events}, censored=${step.censored})",
                        unit = durationField.unit,
                    )
                }
                seriesList.add(ChartDataSeries("km_0", durationField.name, "", 0, SeriesMarkerShape.CIRCLE, points = pts))
                bindings.add(ResolvedSeriesBinding("km_0", durationField.id, false, durationField.name, "", 0, SeriesMarkerShape.CIRCLE, false))
            }
        }

        if (seriesList.isEmpty()) {
            return MappingOutcome.InvalidMapping("Kaplan-Meier curve has no valid (duration >= 0, eventOccurred) observations.")
        }

        val domainLabels = seriesList.first().points.map { it.domainLabel }
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.KAPLAN_MEIER,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(durationField.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = emptyList(),
        ).toMutableList().apply {
            add("Kaplan-Meier product-limit survival curve S(t) starting at 1.0 (100% at t=0); censored observations are marked with '+' ticks.")
        }

        val renderData = ChartRenderData(
            title = "${durationField.name} — Kaplan-Meier Survival Curve",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.KAPLAN_MEIER,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(durationField.name, durationField.unit),
            valueAxisTitle = "Survival probability S(t)",
            valueUnit = "",
            domainLabels = domainLabels,
            series = seriesList,
            minNumericOverride = 0.0,
            maxNumericOverride = 1.0,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = durationField,
            yAxisFields = listOf(durationField),
            groupingField = groupField,
            bindings = bindings,
            missingCount = missingCount,
            notes = notes,
        )
    }

    private fun buildSuccess(
        specification: ChartSpecification,
        renderData: ChartRenderData,
        xAxisField: StatField?,
        yAxisFields: List<StatField>,
        groupingField: StatField?,
        bindings: List<ResolvedSeriesBinding>,
        missingCount: Int,
        notes: List<String>,
    ): MappingOutcome.Success = MappingOutcome.Success(
        configuration = ResolvedChartConfiguration(
            specification = specification,
            chartKind = renderData.chartKind,
            xAxisField = xAxisField,
            yAxisFields = yAxisFields,
            groupingField = groupingField,
            seriesBindings = bindings,
            xAxisLabel = renderData.domainAxisTitle,
            yAxisLabel = renderData.valueAxisTitle,
            valueUnit = renderData.valueUnit,
            missingObservationCount = missingCount,
            totalDomainCount = renderData.domainLabels.size,
            clinicalNotes = notes,
            accessibleSummary = ChartGeometryResolver.buildAccessibleSummary(renderData),
        ),
        renderData = renderData,
    )
}
