package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.HexbinCellData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.MosaicColumnData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.MosaicTileData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SplomCellData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StemLeafRowData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SunburstNodeData
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.Batch1BStatisticalMath
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import kotlin.math.abs

/**
 * Pure-Kotlin mappers for Batch 1B Statistical and Distribution Charts.
 * Reuses shared helpers from [DatasetToChartRenderDataMapper].
 */
internal object Batch1BChartMappers {

    // -------------------------------------------------------------------------
    // Group B1: Diverging Bar, Likert Stacked Diverging, Waffle, Pareto
    // -------------------------------------------------------------------------

    fun mapDivergingBar(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.DIVERGING_BAR, selection)
            ?: return MappingOutcome.InvalidMapping("Diverging bar chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Diverging bar chart requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        if (orderedRows.isEmpty()) {
            return MappingOutcome.InvalidMapping("Diverging bar chart has no rows to plot.")
        }

        val domainLabels = orderedRows.map { it.displayLabel }
        var missingCount = 0
        val points = orderedRows.mapIndexed { idx, row ->
            val cell = yCol.extractor(row.originalIndex, row.row)
            if (cell?.value == null) missingCount++
            ChartDatum(
                domainIndex = idx,
                domainLabel = row.displayLabel,
                numericValue = cell?.value,
                secondaryNumericValue = cell?.value,
                formattedValue = cell?.value?.let {
                    DatasetToChartRenderDataMapper.formatCellValue(it, yCol.unit, cell.formattedValue)
                } ?: "Missing",
                unit = yCol.unit,
            )
        }

        val series = ChartDataSeries(
            id = "diverging_0",
            name = yCol.name,
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.SQUARE,
            points = points,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.DIVERGING_BAR,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.DIVERGING_BAR,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            midpointValue = 0.0,
            chartWarnings = notes,
        )
        val bindings = listOf(
            binding(series, yCol.id, yCol.rawField == null)
        )
        return buildSuccess(specification, renderData, domainField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    fun mapLikertDiverging(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.LIKERT_DIVERGING, selection)
            ?: return MappingOutcome.InvalidMapping("Likert diverging chart requires a Category item/question field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Likert diverging chart requires numeric response columns or a grouped count.")
        }

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val selectedCols = if (selection.yFieldIds.isNotEmpty()) {
            numericCols.filter { it.id in selection.yFieldIds }.ifEmpty { numericCols }
        } else {
            numericCols
        }
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = true)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        val domainLabels: List<String>
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        var missingCount = 0
        var primaryUnit = ""

        if (selectedCols.size >= 2) {
            domainLabels = orderedRows.map { it.displayLabel }
            primaryUnit = selectedCols.first().unit
            selectedCols.forEachIndexed { sIdx, col ->
                val pts = orderedRows.mapIndexed { dIdx, row ->
                    val cell = col.extractor(row.originalIndex, row.row)
                    val v = cell?.value
                    if (v == null) missingCount++
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = row.displayLabel,
                        numericValue = v,
                        formattedValue = v?.let { DatasetToChartRenderDataMapper.formatCellValue(it, col.unit, cell.formattedValue) } ?: "Missing",
                        unit = col.unit,
                    )
                }
                val marker = markerRotation[sIdx % markerRotation.size]
                val s = ChartDataSeries("likert_$sIdx", col.name, col.unit, sIdx, marker, points = pts)
                seriesList.add(s)
                bindings.add(binding(s, col.id, col.rawField == null))
            }
        } else if (groupField != null) {
            val yCol = selectedCols.first()
            primaryUnit = yCol.unit
            val distinctItems = orderedRows.map { it.baseKey to it.displayLabel }.distinctBy { it.first }
            domainLabels = distinctItems.map { it.second }
            val groups = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
            if (groups.size < 2) {
                return MappingOutcome.InvalidMapping("Likert diverging chart requires at least 2 ordered response levels.")
            }
            groups.forEachIndexed { gIdx, groupName ->
                val pts = distinctItems.mapIndexed { dIdx, (key, label) ->
                    val match = orderedRows.lastOrNull {
                        it.baseKey == key && DatasetToChartRenderDataMapper.extractGroupValue(it.row, groupField) == groupName
                    }
                    val cell = match?.let { yCol.extractor(it.originalIndex, it.row) }
                    if (cell?.value == null) missingCount++
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = label,
                        numericValue = cell?.value,
                        formattedValue = cell?.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, yCol.unit, cell.formattedValue) } ?: "Missing",
                        unit = yCol.unit,
                    )
                }
                val marker = markerRotation[gIdx % markerRotation.size]
                val s = ChartDataSeries("likert_$gIdx", groupName, yCol.unit, gIdx, marker, points = pts)
                seriesList.add(s)
                bindings.add(binding(s, yCol.id, yCol.rawField == null))
            }
        } else {
            return MappingOutcome.InvalidMapping("Likert diverging chart requires at least 2 response-level columns or a grouping field.")
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.LIKERT_DIVERGING,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = selectedCols,
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.LIKERT_DIVERGING,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle("Response Share", primaryUnit.ifBlank { "%" }),
            valueUnit = primaryUnit.ifBlank { "%" },
            domainLabels = domainLabels,
            series = seriesList,
            midpointValue = 0.0,
            chartWarnings = notes,
        )
        return buildSuccess(specification, renderData, domainField, selectedCols.mapNotNull { it.rawField }, groupField, bindings, missingCount, notes)
    }

    fun mapWaffle(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.WAFFLE, selection)
            ?: return MappingOutcome.InvalidMapping("Waffle chart requires a Category field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Waffle chart requires a non-negative additive numeric Measure or Count field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        val rawItems = mutableListOf<Pair<String, Double>>()
        var missingCount = 0
        orderedRows.forEach { row ->
            val cell = yCol.extractor(row.originalIndex, row.row)
            val v = cell?.value
            if (v == null) {
                missingCount++
            } else if (v < 0.0) {
                return MappingOutcome.InvalidMapping("Waffle chart requires non-negative values; found negative value ($v).")
            } else {
                rawItems.add(row.displayLabel to v)
            }
        }

        val allocation = Batch1BStatisticalMath.computeWaffleAllocation(rawItems, totalCells = 100)
        if (allocation.items.isEmpty()) {
            return MappingOutcome.InvalidMapping("Waffle chart requires a positive total across categories.")
        }

        val markerRotation = DatasetToChartRenderDataMapper.markerRotation
        val domainLabels = allocation.items.map { it.label }
        val seriesList = allocation.items.mapIndexed { idx, item ->
            val marker = markerRotation[idx % markerRotation.size]
            val formatted = "${item.cells} cells (${DatasetToChartRenderDataMapper.formatPlainNumber(item.percent)}%)"
            ChartDataSeries(
                id = "waffle_$idx",
                name = item.label,
                unit = "%",
                colorIndex = idx,
                markerShape = marker,
                points = listOf(
                    ChartDatum(
                        domainIndex = idx,
                        domainLabel = item.label,
                        numericValue = item.cells.toDouble(),
                        secondaryNumericValue = item.percent,
                        formattedValue = formatted,
                        unit = "%",
                    )
                ),
            )
        }
        val bindings = seriesList.map {
            binding(it, yCol.id, yCol.rawField == null)
        }
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.WAFFLE,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ) + "Apportioned to 100 cells (1 cell = 1%) using Hamilton largest-remainder method."

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.WAFFLE,
            domainAxisTitle = domainField.name,
            valueAxisTitle = "Share (100 cells)",
            valueUnit = "%",
            domainLabels = domainLabels,
            series = seriesList,
            chartWarnings = notes,
        )
        return buildSuccess(specification, renderData, domainField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    fun mapPareto(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.PARETO, selection)
            ?: return MappingOutcome.InvalidMapping("Pareto chart requires a Category field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Pareto chart requires a non-negative Count or Measure field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        val rawItems = mutableListOf<Pair<String, Double>>()
        var missingCount = 0
        orderedRows.forEach { row ->
            val cell = yCol.extractor(row.originalIndex, row.row)
            val v = cell?.value
            if (v == null) {
                missingCount++
            } else {
                rawItems.add(row.displayLabel to v)
            }
        }

        val pareto = Batch1BStatisticalMath.computePareto(rawItems)
        if (pareto.hasNegativeValues) {
            return MappingOutcome.InvalidMapping("Pareto chart requires non-negative additive counts or costs.")
        }
        if (pareto.items.isEmpty()) {
            return MappingOutcome.InvalidMapping("Pareto chart requires at least one positive category value.")
        }

        val domainLabels = pareto.items.map { it.label }
        val barPoints = pareto.items.mapIndexed { idx, item ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = item.label,
                numericValue = item.value,
                secondaryNumericValue = item.cumulativePercent,
                formattedValue = DatasetToChartRenderDataMapper.formatCellValue(item.value, yCol.unit, "") +
                    " (Cum ${DatasetToChartRenderDataMapper.formatPlainNumber(item.cumulativePercent)}%)",
                unit = yCol.unit,
            )
        }
        val cumPoints = pareto.items.mapIndexed { idx, item ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = item.label,
                numericValue = item.cumulativePercent,
                formattedValue = "${DatasetToChartRenderDataMapper.formatPlainNumber(item.cumulativePercent)}%",
                unit = "%",
            )
        }
        val barSeries = ChartDataSeries("pareto_bar", yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, points = barPoints)
        val cumSeries = ChartDataSeries("pareto_cum", "Cumulative %", "%", 1, SeriesMarkerShape.CIRCLE, points = cumPoints)

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.PARETO,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.PARETO,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(barSeries, cumSeries),
            chartWarnings = notes,
        )
        val bindings = listOf(
            binding(barSeries, yCol.id, yCol.rawField == null),
            binding(cumSeries, yCol.id, isDerived = true),
        )
        return buildSuccess(specification, renderData, domainField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    // -------------------------------------------------------------------------
    // Group B2: ECDF, Q-Q Plot, Stem-and-Leaf Plot
    // -------------------------------------------------------------------------

    fun mapEcdf(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("ECDF plot requires at least one numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, yCol.id, selection, allowCategoryFallback = false)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        var missingCount = 0
        var totalN = 0

        if (groupField != null) {
            val groups = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
            groups.forEachIndexed { gIdx, groupName ->
                val rawVals = mutableListOf<Double>()
                dataset.rows.forEachIndexed { rIdx, row ->
                    if (DatasetToChartRenderDataMapper.extractGroupValue(row, groupField) == groupName) {
                        val v = yCol.extractor(rIdx, row)?.value
                        if (v == null) missingCount++ else rawVals.add(v)
                    }
                }
                val ecdf = Batch1BStatisticalMath.computeEcdf(rawVals)
                totalN += ecdf.n
                val marker = markerRotation[gIdx % markerRotation.size]
                val pts = ecdf.points.mapIndexed { pIdx, pt ->
                    ChartDatum(
                        domainIndex = pIdx,
                        domainLabel = DatasetToChartRenderDataMapper.formatPlainNumber(pt.x),
                        numericValue = pt.cumulativeProportion,
                        secondaryNumericValue = pt.x,
                        formattedValue = "x=${DatasetToChartRenderDataMapper.formatPlainNumber(pt.x)}, F(x)=${DatasetToChartRenderDataMapper.formatPlainNumber(pt.cumulativeProportion)}",
                        unit = "",
                    )
                }
                if (pts.isNotEmpty()) {
                    val s = ChartDataSeries("ecdf_$gIdx", "$groupName (n=${ecdf.n})", "", gIdx, marker, points = pts)
                    seriesList.add(s)
                    bindings.add(binding(s, yCol.id, yCol.rawField == null))
                }
            }
        } else {
            val rawVals = mutableListOf<Double>()
            dataset.rows.forEachIndexed { rIdx, row ->
                val v = yCol.extractor(rIdx, row)?.value
                if (v == null) missingCount++ else rawVals.add(v)
            }
            val ecdf = Batch1BStatisticalMath.computeEcdf(rawVals)
            totalN = ecdf.n
            val pts = ecdf.points.mapIndexed { pIdx, pt ->
                ChartDatum(
                    domainIndex = pIdx,
                    domainLabel = DatasetToChartRenderDataMapper.formatPlainNumber(pt.x),
                    numericValue = pt.cumulativeProportion,
                    secondaryNumericValue = pt.x,
                    formattedValue = "x=${DatasetToChartRenderDataMapper.formatPlainNumber(pt.x)}, F(x)=${DatasetToChartRenderDataMapper.formatPlainNumber(pt.cumulativeProportion)}",
                    unit = "",
                )
            }
            if (pts.isNotEmpty()) {
                val s = ChartDataSeries("ecdf_0", "${yCol.name} (n=${ecdf.n})", "", 0, SeriesMarkerShape.CIRCLE, points = pts)
                seriesList.add(s)
                bindings.add(binding(s, yCol.id, yCol.rawField == null))
            }
        }

        if (seriesList.isEmpty()) {
            return MappingOutcome.InvalidMapping("ECDF plot requires finite numeric observations.")
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.ECDF,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ) + "Effective sample size n=$totalN (missing observations excluded from denominator)."

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.ECDF,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueAxisTitle = "Cumulative Proportion F(x)",
            valueUnit = "",
            domainLabels = seriesList.first().points.map { it.domainLabel },
            series = seriesList,
            chartWarnings = notes,
        )
        return buildSuccess(specification, renderData, yCol.rawField, listOfNotNull(yCol.rawField), groupField, bindings, missingCount, notes)
    }

    fun mapQqPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Q-Q plot requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val rawVals = mutableListOf<Double>()
        var missingCount = 0
        dataset.rows.forEachIndexed { rIdx, row ->
            val v = yCol.extractor(rIdx, row)?.value
            if (v == null) missingCount++ else rawVals.add(v)
        }
        val qq = Batch1BStatisticalMath.computeNormalQqPlot(rawVals)
        if (qq.n < 3) {
            return MappingOutcome.InvalidMapping("Normal Q-Q plot requires at least 3 finite numeric observations.")
        }

        val pointData = qq.points.mapIndexed { idx, pt ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = DatasetToChartRenderDataMapper.formatPlainNumber(pt.theoreticalQuantile),
                numericValue = pt.sampleQuantile,
                secondaryNumericValue = pt.theoreticalQuantile,
                formattedValue = "z=${DatasetToChartRenderDataMapper.formatPlainNumber(pt.theoreticalQuantile)}, y=${DatasetToChartRenderDataMapper.formatCellValue(pt.sampleQuantile, yCol.unit, "")}",
                unit = yCol.unit,
            )
        }
        val ptsSeries = ChartDataSeries("qq_pts", "Observed Quantiles (n=${qq.n})", yCol.unit, 0, SeriesMarkerShape.CIRCLE, points = pointData)
        val seriesList = mutableListOf(ptsSeries)
        val bindings = mutableListOf(binding(ptsSeries, yCol.id, yCol.rawField == null))

        if (qq.referenceLineStart != null && qq.referenceLineEnd != null) {
            val refPts = listOf(
                ChartDatum(0, "Q1", qq.referenceLineStart.second, qq.referenceLineStart.first, formattedValue = "Reference", unit = yCol.unit),
                ChartDatum(1, "Q3", qq.referenceLineEnd.second, qq.referenceLineEnd.first, formattedValue = "Reference", unit = yCol.unit),
            )
            val refSeries = ChartDataSeries("qq_ref", "Quartile Reference Line", yCol.unit, 1, SeriesMarkerShape.CIRCLE, dashed = true, points = refPts)
            seriesList.add(refSeries)
            bindings.add(binding(refSeries, yCol.id, isDerived = true))
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.QQ_PLOT,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ) + "Normal Q-Q plot using Blom plotting positions (n=${qq.n})."

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.QQ_PLOT,
            domainAxisTitle = "Theoretical Normal Quantiles (z)",
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = pointData.map { it.domainLabel },
            series = seriesList,
            chartWarnings = notes,
        )
        return buildSuccess(specification, renderData, yCol.rawField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    fun mapStemAndLeaf(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Stem-and-leaf plot requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val rawVals = mutableListOf<Double>()
        var missingCount = 0
        dataset.rows.forEachIndexed { rIdx, row ->
            val v = yCol.extractor(rIdx, row)?.value
            if (v == null) missingCount++ else rawVals.add(v)
        }
        val stemResult = Batch1BStatisticalMath.computeStemAndLeaf(rawVals)
        if (stemResult.rows.isEmpty()) {
            return MappingOutcome.InvalidMapping("Stem-and-leaf plot requires at least one finite numeric observation.")
        }

        val rowData = stemResult.rows.map {
            StemLeafRowData(stemLabel = it.stemLabel, leaves = it.leaves)
        }
        val summaryPoints = stemResult.rows.mapIndexed { idx, row ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = row.stemLabel,
                numericValue = row.count.toDouble(),
                formattedValue = "${row.stemLabel} | ${row.leaves.joinToString(" ")} (n=${row.count})",
                unit = "count",
            )
        }
        val series = ChartDataSeries("stem_leaf", yCol.name, "count", 0, SeriesMarkerShape.SQUARE, points = summaryPoints)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.STEM_AND_LEAF,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ) + stemResult.keyText

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.STEM_AND_LEAF,
            domainAxisTitle = "Stem",
            valueAxisTitle = "Leaves (${yCol.name})",
            valueUnit = yCol.unit,
            domainLabels = rowData.map { it.stemLabel },
            series = listOf(series),
            stemAndLeafRows = rowData,
            stemLeafKey = stemResult.keyText,
            chartWarnings = notes,
        )
        val bindings = listOf(binding(series, yCol.id, yCol.rawField == null))
        return buildSuccess(specification, renderData, yCol.rawField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    // -------------------------------------------------------------------------
    // Group B3: Hexbin, Scatterplot Matrix, Mosaic, Sunburst
    // -------------------------------------------------------------------------

    fun mapHexbin(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.size < 2) {
            return MappingOutcome.InvalidMapping("Hexbin plot requires at least two numeric Measure or Rate fields.")
        }
        val xCol = numericCols.firstOrNull { it.id == selection.xFieldId } ?: numericCols[0]
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds && it.id != xCol.id }
            ?: numericCols.firstOrNull { it.id != xCol.id }
            ?: return MappingOutcome.InvalidMapping("Hexbin plot requires two distinct numeric fields.")

        val pairs = mutableListOf<Pair<Double, Double>>()
        var missingCount = 0
        dataset.rows.forEachIndexed { rIdx, row ->
            val xv = xCol.extractor(rIdx, row)?.value
            val yv = yCol.extractor(rIdx, row)?.value
            if (xv == null || yv == null) missingCount++ else pairs.add(xv to yv)
        }
        val hex = Batch1BStatisticalMath.computeHexbin(pairs, targetCols = 10)
        if (hex.bins.isEmpty()) {
            return MappingOutcome.InvalidMapping("Hexbin plot requires finite paired (X, Y) observations.")
        }

        val hexCells = hex.bins.map {
            HexbinCellData(it.col, it.row, it.centerX, it.centerY, it.count)
        }
        val pts = hex.bins.mapIndexed { idx, bin ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = "Bin ${idx + 1}",
                numericValue = bin.centerY,
                secondaryNumericValue = bin.centerX,
                formattedValue = "Count=${bin.count} (${DatasetToChartRenderDataMapper.formatPlainNumber(bin.centerX)}, ${DatasetToChartRenderDataMapper.formatPlainNumber(bin.centerY)})",
                unit = "count",
            )
        }
        val series = ChartDataSeries("hexbin_0", "Point Density (max=${hex.maxCount})", "count", 0, SeriesMarkerShape.SQUARE, points = pts)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.HEXBIN,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(xCol.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(xCol, yCol),
        ) + "Binned ${hex.totalPoints} points into ${hex.bins.size} populated hexagonal cells (empty cells omitted)."

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.HEXBIN,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(xCol.name, xCol.unit),
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = pts.map { it.domainLabel },
            series = listOf(series),
            hexbinCells = hexCells,
            hexbinGridCols = hex.gridCols,
            hexbinGridRows = hex.gridRows,
            chartWarnings = notes,
        )
        val bindings = listOf(binding(series, yCol.id, yCol.rawField == null))
        return buildSuccess(specification, renderData, xCol.rawField, listOfNotNull(yCol.rawField), null, bindings, missingCount, notes)
    }

    fun mapScatterplotMatrix(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val selectedCols = if (selection.yFieldIds.size >= 2) {
            numericCols.filter { it.id in selection.yFieldIds }.take(5)
        } else {
            numericCols.take(5)
        }
        if (selectedCols.size < 2) {
            return MappingOutcome.InvalidMapping("Scatterplot matrix requires at least 2 numeric Measure or Rate fields.")
        }

        val panels = mutableListOf<SplomCellData>()
        val summaryPoints = mutableListOf<ChartDatum>()
        var missingCount = 0

        selectedCols.forEachIndexed { rIdx, yCol ->
            selectedCols.forEachIndexed { cIdx, xCol ->
                if (rIdx == cIdx) {
                    panels.add(SplomCellData(rIdx, cIdx, points = emptyList(), missingPairCount = 0))
                    summaryPoints.add(
                        ChartDatum(
                            domainIndex = panels.lastIndex,
                            domainLabel = yCol.name,
                            numericValue = dataset.rows.size.toDouble(),
                            formattedValue = "${yCol.name} (diagonal)",
                            unit = yCol.unit,
                        )
                    )
                } else {
                    val pairs = mutableListOf<Pair<Double, Double>>()
                    var pairMissing = 0
                    dataset.rows.forEachIndexed { rowIdx, row ->
                        val xv = xCol.extractor(rowIdx, row)?.value
                        val yv = yCol.extractor(rowIdx, row)?.value
                        if (xv == null || yv == null) {
                            pairMissing++
                            if (rIdx < cIdx) missingCount++
                        } else {
                            pairs.add(xv to yv)
                        }
                    }
                    panels.add(SplomCellData(rIdx, cIdx, points = pairs, missingPairCount = pairMissing))
                    summaryPoints.add(
                        ChartDatum(
                            domainIndex = panels.lastIndex,
                            domainLabel = "${yCol.name} vs ${xCol.name}",
                            numericValue = pairs.size.toDouble(),
                            formattedValue = "${pairs.size} paired points",
                            unit = "pairs",
                        )
                    )
                }
            }
        }

        val varNames = selectedCols.map { it.name }
        val series = ChartDataSeries("splom_0", "${varNames.size}x${varNames.size} Matrix", "", 0, SeriesMarkerShape.CIRCLE, points = summaryPoints)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.SCATTERPLOT_MATRIX,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, selectedCols.map { it.id }.toSet()),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = selectedCols,
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.SCATTERPLOT_MATRIX,
            domainAxisTitle = "Variables",
            valueAxisTitle = "Pairwise Scatter",
            valueUnit = "",
            domainLabels = varNames,
            series = listOf(series),
            splomPanels = panels,
            splomVariableNames = varNames,
            chartWarnings = notes,
        )
        val bindings = listOf(binding(series, selectedCols.first().id, selectedCols.first().rawField == null))
        return buildSuccess(specification, renderData, selectedCols.first().rawField, selectedCols.mapNotNull { it.rawField }, null, bindings, missingCount, notes)
    }

    fun mapMosaic(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val catFields = dataset.fields.filter { it.role is FieldRole.Category || it.role is FieldRole.Group }
        val colField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.MOSAIC, selection)
            ?: catFields.firstOrNull()
            ?: return MappingOutcome.InvalidMapping("Mosaic plot requires at least two categorical fields.")
        val rowField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, colField.id, selection, allowCategoryFallback = true)
            ?: catFields.firstOrNull { it.id != colField.id }
            ?: return MappingOutcome.InvalidMapping("Mosaic plot requires a second categorical/group field for conditional tiles.")

        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.firstOrNull()

        val colNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, colField)
        val rowNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, rowField)
        if (colNames.isEmpty() || rowNames.isEmpty()) {
            return MappingOutcome.InvalidMapping("Mosaic plot requires non-empty category values in both categorical fields.")
        }

        val cellMatrix = linkedMapOf<Pair<String, String>, Double>()
        var missingCount = 0
        dataset.rows.forEachIndexed { rIdx, row ->
            val cVal = DatasetToChartRenderDataMapper.extractGroupValue(row, colField)
            val rVal = DatasetToChartRenderDataMapper.extractGroupValue(row, rowField)
            if (cVal == null || rVal == null) {
                missingCount++
                return@forEachIndexed
            }
            val weight = if (yCol != null) {
                val v = yCol.extractor(rIdx, row)?.value
                if (v == null) {
                    missingCount++
                    return@forEachIndexed
                }
                if (v < 0.0) {
                    return MappingOutcome.InvalidMapping("Mosaic plot requires non-negative counts/weights; found $v.")
                }
                v
            } else {
                1.0
            }
            val key = cVal to rVal
            cellMatrix[key] = (cellMatrix[key] ?: 0.0) + weight
        }

        val grandTotal = cellMatrix.values.sum()
        if (grandTotal <= 0.0) {
            return MappingOutcome.InvalidMapping("Mosaic plot requires a positive total count across cells.")
        }

        val mosaicColumns = colNames.mapNotNull { cName ->
            val colTotal = rowNames.sumOf { rName -> cellMatrix[cName to rName] ?: 0.0 }
            if (colTotal <= 0.0) return@mapNotNull null
            val tiles = rowNames.mapIndexed { rIdx, rName ->
                val count = cellMatrix[cName to rName] ?: 0.0
                MosaicTileData(
                    rowLabel = rName,
                    count = count,
                    proportionInColumn = count / colTotal,
                    colorIndex = rIdx,
                )
            }
            MosaicColumnData(
                columnLabel = cName,
                columnTotal = colTotal,
                widthProportion = colTotal / grandTotal,
                tiles = tiles,
            )
        }

        val markerRotation = DatasetToChartRenderDataMapper.markerRotation
        val seriesList = rowNames.mapIndexed { rIdx, rName ->
            val pts = mosaicColumns.mapIndexed { cIdx, col ->
                val tile = col.tiles.firstOrNull { it.rowLabel == rName }
                val count = tile?.count ?: 0.0
                val pct = (tile?.proportionInColumn ?: 0.0) * 100.0
                ChartDatum(
                    domainIndex = cIdx,
                    domainLabel = col.columnLabel,
                    numericValue = count,
                    secondaryNumericValue = pct,
                    formattedValue = "${DatasetToChartRenderDataMapper.formatPlainNumber(count)} (${DatasetToChartRenderDataMapper.formatPlainNumber(pct)}%)",
                    unit = yCol?.unit ?: "count",
                )
            }
            ChartDataSeries("mosaic_$rIdx", rName, yCol?.unit ?: "count", rIdx, markerRotation[rIdx % markerRotation.size], points = pts)
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.MOSAIC,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(colField.id, rowField.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOfNotNull(yCol),
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.MOSAIC,
            domainAxisTitle = colField.name,
            valueAxisTitle = "${rowField.name} (Conditional Share)",
            valueUnit = "%",
            domainLabels = mosaicColumns.map { it.columnLabel },
            series = seriesList,
            mosaicColumns = mosaicColumns,
            chartWarnings = notes,
        )
        val bindings = seriesList.map {
            binding(it, yCol?.id ?: colField.id, yCol?.rawField == null)
        }
        return buildSuccess(specification, renderData, colField, listOfNotNull(yCol?.rawField), rowField, bindings, missingCount, notes)
    }

    fun mapSunburst(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.SUNBURST, selection)
            ?: return MappingOutcome.InvalidMapping("Sunburst chart requires a primary Category hierarchy field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.firstOrNull()
            ?: return MappingOutcome.InvalidMapping("Sunburst chart requires a non-negative numeric Measure or Count field.")
        val childField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = true)

        val parentTotals = linkedMapOf<String, Double>()
        val childTotals = linkedMapOf<String, MutableMap<String, Double>>()
        var missingCount = 0

        dataset.rows.forEachIndexed { rIdx, row ->
            val parent = DatasetToChartRenderDataMapper.extractGroupValue(row, domainField)
            val v = yCol.extractor(rIdx, row)?.value
            if (parent == null || v == null) {
                missingCount++
                return@forEachIndexed
            }
            if (v < 0.0) {
                return MappingOutcome.InvalidMapping("Sunburst chart requires non-negative values; found $v.")
            }
            parentTotals[parent] = (parentTotals[parent] ?: 0.0) + v
            if (childField != null) {
                val child = DatasetToChartRenderDataMapper.extractGroupValue(row, childField)
                if (child != null) {
                    val map = childTotals.getOrPut(parent) { linkedMapOf() }
                    map[child] = (map[child] ?: 0.0) + v
                }
            }
        }

        val grandTotal = parentTotals.values.sum()
        if (grandTotal <= 0.0) {
            return MappingOutcome.InvalidMapping("Sunburst chart requires a positive total across hierarchy nodes.")
        }

        val nodes = mutableListOf<SunburstNodeData>()
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation
        val seriesList = mutableListOf<ChartDataSeries>()
        var currentAngle = 0f

        parentTotals.entries.forEachIndexed { pIdx, (pName, pVal) ->
            val parentId = "p_$pIdx"
            nodes.add(
                SunburstNodeData(
                    id = parentId,
                    label = pName,
                    level = 1,
                    parentId = null,
                    value = pVal,
                    colorIndex = pIdx,
                )
            )
            val pct = (pVal / grandTotal) * 100.0
            val marker = markerRotation[pIdx % markerRotation.size]
            seriesList.add(
                ChartDataSeries(
                    id = "sunburst_$pIdx",
                    name = pName,
                    unit = yCol.unit,
                    colorIndex = pIdx,
                    markerShape = marker,
                    points = listOf(
                        ChartDatum(
                            domainIndex = pIdx,
                            domainLabel = pName,
                            numericValue = pVal,
                            secondaryNumericValue = pct,
                            formattedValue = "${DatasetToChartRenderDataMapper.formatCellValue(pVal, yCol.unit, "")} (${DatasetToChartRenderDataMapper.formatPlainNumber(pct)}%)",
                            unit = yCol.unit,
                        )
                    ),
                )
            )

            val children = childTotals[pName]
            if (!children.isNullOrEmpty() && pVal > 0.0) {
                children.entries.forEachIndexed { cIdx, (cName, cVal) ->
                    nodes.add(
                        SunburstNodeData(
                            id = "c_${pIdx}_$cIdx",
                            label = cName,
                            level = 2,
                            parentId = parentId,
                            value = cVal,
                            colorIndex = pIdx,
                        )
                    )
                }
            }
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.SUNBURST,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.SUNBURST,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = parentTotals.keys.toList(),
            series = seriesList,
            sunburstNodes = nodes,
            chartWarnings = notes,
        )
        val bindings = seriesList.map {
            binding(it, yCol.id, yCol.rawField == null)
        }
        return buildSuccess(specification, renderData, domainField, listOfNotNull(yCol.rawField), childField, bindings, missingCount, notes)
    }

    private fun binding(
        series: ChartDataSeries,
        sourceFieldId: String,
        isDerived: Boolean,
    ): ResolvedSeriesBinding = ResolvedSeriesBinding(
        seriesId = series.id,
        sourceFieldId = sourceFieldId,
        isDerivedField = isDerived,
        displayName = series.name,
        unit = series.unit,
        colorIndex = series.colorIndex,
        markerShape = series.markerShape,
        dashed = series.dashed,
    )

    internal fun buildSuccess(
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
