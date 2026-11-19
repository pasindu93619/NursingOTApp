package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.CorrelationCellData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.StatisticalMath
import com.pasindu.nursingotapp.statgraph.stats.StatisticsEngine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure-Kotlin mappers for Batch 4 Composition, Correlation & Specialized Views:
 * Pie Chart, Donut Chart, Bubble Chart, Correlation Heatmap, Waterfall Chart,
 * and Population Pyramid.
 */
internal object Batch4ChartMappers {

    fun mapPieOrDonut(
        dataset: StatDataset,
        specification: ChartSpecification,
        chartKind: ChartKind,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, chartKind, selection)
            ?: return MappingOutcome.InvalidMapping("${specification.canonicalName} requires a Category domain field.")
        if (domainField.role is FieldRole.Time) {
            return MappingOutcome.InvalidMapping(
                "${specification.canonicalName} is not suitable for sequential time periods; use a Line or Bar chart.",
            )
        }
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("${specification.canonicalName} requires a non-negative numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        var missingCount = 0
        val rawSlices = mutableListOf<Pair<String, Double>>()
        for (entry in orderedRows) {
            val v = yCol.extractor(entry.originalIndex, entry.row).value?.takeIf { it.isFinite() }
            if (v == null) {
                missingCount++
                continue
            }
            if (v < 0.0) {
                return MappingOutcome.InvalidMapping("${specification.canonicalName} requires non-negative values.")
            }
            if (v > 0.0) {
                rawSlices.add(entry.displayLabel to v)
            }
        }

        val total = rawSlices.sumOf { it.second }
        if (rawSlices.size < 2 || total <= 0.0) {
            return MappingOutcome.InvalidMapping(
                "${specification.canonicalName} requires at least 2 positive categories summing to a non-zero total.",
            )
        }

        val domainLabels = rawSlices.map { it.first }
        val points = rawSlices.mapIndexed { idx, (label, v) ->
            val pct = String.format(Locale.US, "%.1f%%", (v / total) * 100.0)
            val valText = DatasetToChartRenderDataMapper.formatCellValue(v, yCol.unit, "")
            ChartDatum(
                domainIndex = idx,
                domainLabel = label,
                numericValue = v,
                secondaryNumericValue = (v / total) * 100.0,
                formattedValue = "$valText ($pct)",
                unit = yCol.unit,
            )
        }

        val series = ChartDataSeries(
            id = "slice_0",
            name = yCol.name,
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.CIRCLE,
            points = points,
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = chartKind,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList()
        if (rawSlices.size > 5) {
            notes.add("${rawSlices.size} slices may crowd narrow phone screens; consider a Horizontal Bar chart for clearer comparison.")
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} by ${domainField.name}",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = chartKind,
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
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapBubble(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.size < 3) {
            return MappingOutcome.InvalidMapping("Bubble chart requires 3 distinct numeric fields (X, Y, and Size).")
        }

        val xCol = selection.xFieldId?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols[0]
        val yCol = selection.yFieldIds.firstOrNull()?.let { id -> numericCols.firstOrNull { it.id == id && it.id != xCol.id } }
            ?: numericCols.firstOrNull { it.id != xCol.id }
            ?: return MappingOutcome.InvalidMapping("Bubble chart requires distinct X and Y numeric fields.")
        val sizeCol = selection.yFieldIds.drop(1).firstOrNull()?.let { id -> numericCols.firstOrNull { it.id == id && it.id != xCol.id && it.id != yCol.id } }
            ?: numericCols.firstOrNull { it.id != xCol.id && it.id != yCol.id }
            ?: return MappingOutcome.InvalidMapping("Bubble chart requires a 3rd numeric field for bubble area.")

        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, null, selection, allowCategoryFallback = false)
        val groupKeys = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)

        var missingCount = 0
        var negativeSizeCount = 0
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()

        fun extractPointsForRows(rowIndices: List<Int>): List<ChartDatum> {
            val pts = mutableListOf<ChartDatum>()
            for (rIdx in rowIndices) {
                val row = dataset.rows[rIdx]
                val xv = xCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                val yv = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                val sv = sizeCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                if (sv != null && sv < 0.0) {
                    negativeSizeCount++
                }
                val valid = xv != null && yv != null && sv != null && sv >= 0.0
                if (!valid) missingCount++
                val label = row.periodOrCategoryLabel.ifBlank { "Obs ${rIdx + 1}" }
                pts.add(
                    ChartDatum(
                        domainIndex = rIdx,
                        domainLabel = label,
                        numericValue = if (valid) yv else null,
                        secondaryNumericValue = if (valid) xv else null,
                        lowerLimit = if (valid) sv else null,
                        formattedValue = if (valid) {
                            "${DatasetToChartRenderDataMapper.formatPlainNumber(xv!!)}, ${DatasetToChartRenderDataMapper.formatPlainNumber(yv!!)} (size ${DatasetToChartRenderDataMapper.formatPlainNumber(sv!!)})"
                        } else {
                            "Missing"
                        },
                        unit = yCol.unit,
                    ),
                )
            }
            return pts
        }

        if (groupField != null && groupKeys.size >= 2) {
            groupKeys.forEachIndexed { gIdx, gName ->
                val indices = dataset.rows.indices.filter { DatasetToChartRenderDataMapper.extractGroupValue(dataset.rows[it], groupField) == gName }
                val pts = extractPointsForRows(indices)
                seriesList.add(ChartDataSeries("bubble_$gIdx", gName, yCol.unit, gIdx, SeriesMarkerShape.CIRCLE, points = pts))
                bindings.add(ResolvedSeriesBinding("bubble_$gIdx", yCol.id, yCol.isDerived, gName, yCol.unit, gIdx, SeriesMarkerShape.CIRCLE, false))
            }
        } else {
            val pts = extractPointsForRows(dataset.rows.indices.toList())
            seriesList.add(ChartDataSeries("bubble_0", "${yCol.name} vs ${xCol.name} (size: ${sizeCol.name})", yCol.unit, 0, SeriesMarkerShape.CIRCLE, points = pts))
            bindings.add(ResolvedSeriesBinding("bubble_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false))
        }

        if (negativeSizeCount > 0 && seriesList.all { s -> s.points.all { it.numericValue == null } }) {
            return MappingOutcome.InvalidMapping("Bubble chart requires non-negative size values in '${sizeCol.name}'.")
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.BUBBLE,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(xCol.id, yCol.id, sizeCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(xCol, yCol, sizeCol),
        ).toMutableList().apply {
            add("Bubble area (not radius) scales linearly with '${sizeCol.name}' using sqrt(value).")
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} vs ${xCol.name} (Bubble: ${sizeCol.name})",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.BUBBLE,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(xCol.name, xCol.unit),
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = dataset.rows.mapIndexed { idx, row -> row.periodOrCategoryLabel.ifBlank { "Obs ${idx + 1}" } },
            series = seriesList,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = xCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField, sizeCol.rawField),
            groupingField = groupField,
            bindings = bindings,
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapCorrelationHeatmap(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val pickedCols = if (selection.yFieldIds.size >= 2) {
            numericCols.filter { it.id in selection.yFieldIds }
        } else {
            numericCols.filter { !it.isDerived }.ifEmpty { numericCols }
        }.take(6)

        if (pickedCols.size < 2) {
            return MappingOutcome.InvalidMapping("Correlation heatmap requires at least 2 numeric fields.")
        }

        val colValues = pickedCols.map { col ->
            dataset.rows.mapIndexed { rIdx, row -> col.extractor(rIdx, row).value }
        }
        val missingCount = colValues.sumOf { list -> list.count { it == null } }

        val cells = mutableListOf<CorrelationCellData>()
        for (i in pickedCols.indices) {
            for (j in pickedCols.indices) {
                if (i == j) {
                    val validN = colValues[i].count { it != null && it.isFinite() }
                    cells.add(CorrelationCellData(i, j, if (validN >= 3) 1.0 else null, validN))
                } else {
                    val res = StatisticsEngine.pearsonCorrelation(colValues[i], colValues[j])
                    cells.add(CorrelationCellData(i, j, res.coefficient, res.pairedCount))
                }
            }
        }

        val varNames = pickedCols.map { it.name }
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.CORRELATION_HEATMAP,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, pickedCols.map { it.id }.toSet()),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = pickedCols,
        ).toMutableList().apply {
            add("Pairwise Pearson correlation r across ${pickedCols.size} numeric variables (requires n >= 3 paired observations; correlation does not imply causation).")
        }

        val renderData = ChartRenderData(
            title = "${dataset.name.ifBlank { "Clinical Dataset" }} — Correlation Matrix",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.CORRELATION_HEATMAP,
            domainAxisTitle = "Variables",
            valueAxisTitle = "Pearson r",
            valueUnit = "",
            domainLabels = varNames,
            series = listOf(ChartDataSeries("corr_0", "Pearson r", "", 0, points = emptyList())),
            correlationCells = cells,
            correlationVariableNames = varNames,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = pickedCols.first().rawField,
            yAxisFields = pickedCols.mapNotNull { it.rawField },
            groupingField = null,
            bindings = emptyList(),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapWaterfall(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.WATERFALL, selection)
            ?: return MappingOutcome.InvalidMapping("Waterfall chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Waterfall chart requires a numeric delta Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        var missingCount = 0
        val deltas = mutableListOf<Pair<String, Double>>()
        orderedRows.forEach { entry ->
            val v = yCol.extractor(entry.originalIndex, entry.row).value?.takeIf { it.isFinite() }
            if (v == null) {
                missingCount++
            } else {
                deltas.add(entry.displayLabel to v)
            }
        }
        if (deltas.size < 2) {
            return MappingOutcome.InvalidMapping("Waterfall chart requires at least 2 valid numeric steps.")
        }

        val steps = StatisticalMath.computeWaterfallPositions(deltas, includeTotal = true)
        val domainLabels = steps.map { it.label }
        val pts = steps.mapIndexed { idx, step ->
            val deltaSign = if (!step.isTotal && step.delta > 0.0) "+" else ""
            ChartDatum(
                domainIndex = idx,
                domainLabel = step.label,
                numericValue = step.delta,
                lowerLimit = step.start,
                upperLimit = step.end,
                isConflictFlagged = step.isTotal,
                formattedValue = "$deltaSign${DatasetToChartRenderDataMapper.formatCellValue(step.delta, yCol.unit, "")} → ${DatasetToChartRenderDataMapper.formatPlainNumber(step.end)}",
                unit = yCol.unit,
            )
        }

        val series = ChartDataSeries("waterfall_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, points = pts)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.WATERFALL,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        )
        val renderData = ChartRenderData(
            title = "${yCol.name} — Waterfall Bridge",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.WATERFALL,
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
            bindings = listOf(ResolvedSeriesBinding(series.id, yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapPopulationPyramid(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.POPULATION_PYRAMID, selection)
            ?: return MappingOutcome.InvalidMapping("Population pyramid requires a Category domain field (e.g., Age Group).")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Population pyramid requires numeric count fields.")
        }

        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = false)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        var missingCount = 0
        val domainLabels: List<String>
        val leftPts = mutableListOf<ChartDatum>()
        val rightPts = mutableListOf<ChartDatum>()
        val leftName: String
        val rightName: String
        val unit: String

        if (groupField != null && groupNames.size >= 2) {
            leftName = groupNames[0]
            rightName = groupNames[1]
            val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
            unit = yCol.unit
            domainLabels = orderedRows.map { it.displayLabel }.distinct()
            domainLabels.forEachIndexed { dIdx, dLabel ->
                val matching = orderedRows.filter { it.displayLabel == dLabel }
                val lVal = matching.lastOrNull { DatasetToChartRenderDataMapper.extractGroupValue(it.row, groupField) == leftName }
                    ?.let { yCol.extractor(it.originalIndex, it.row).value }
                val rVal = matching.lastOrNull { DatasetToChartRenderDataMapper.extractGroupValue(it.row, groupField) == rightName }
                    ?.let { yCol.extractor(it.originalIndex, it.row).value }

                if (lVal != null && lVal < 0.0 || rVal != null && rVal < 0.0) {
                    return MappingOutcome.InvalidMapping("Population pyramid requires non-negative counts.")
                }
                if (lVal == null) missingCount++
                if (rVal == null) missingCount++

                leftPts.add(
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = dLabel,
                        numericValue = lVal?.let { -it },
                        secondaryNumericValue = lVal,
                        formattedValue = lVal?.let { DatasetToChartRenderDataMapper.formatCellValue(it, unit, "") } ?: "Missing",
                        unit = unit,
                    ),
                )
                rightPts.add(
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = dLabel,
                        numericValue = rVal,
                        secondaryNumericValue = rVal,
                        formattedValue = rVal?.let { DatasetToChartRenderDataMapper.formatCellValue(it, unit, "") } ?: "Missing",
                        unit = unit,
                    ),
                )
            }
        } else if (numericCols.size >= 2) {
            val leftCol = numericCols[0]
            val rightCol = numericCols[1]
            leftName = leftCol.name
            rightName = rightCol.name
            unit = leftCol.unit.ifBlank { rightCol.unit }
            domainLabels = orderedRows.map { it.displayLabel }
            orderedRows.forEachIndexed { dIdx, entry ->
                val lVal = leftCol.extractor(entry.originalIndex, entry.row).value
                val rVal = rightCol.extractor(entry.originalIndex, entry.row).value
                if (lVal != null && lVal < 0.0 || rVal != null && rVal < 0.0) {
                    return MappingOutcome.InvalidMapping("Population pyramid requires non-negative counts.")
                }
                if (lVal == null) missingCount++
                if (rVal == null) missingCount++

                leftPts.add(
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = entry.displayLabel,
                        numericValue = lVal?.let { -it },
                        secondaryNumericValue = lVal,
                        formattedValue = lVal?.let { DatasetToChartRenderDataMapper.formatCellValue(it, unit, "") } ?: "Missing",
                        unit = unit,
                    ),
                )
                rightPts.add(
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = entry.displayLabel,
                        numericValue = rVal,
                        secondaryNumericValue = rVal,
                        formattedValue = rVal?.let { DatasetToChartRenderDataMapper.formatCellValue(it, unit, "") } ?: "Missing",
                        unit = unit,
                    ),
                )
            }
        } else {
            return MappingOutcome.InvalidMapping("Population pyramid requires 2 opposing groups or 2 non-negative numeric columns.")
        }

        val maxMag = max(
            leftPts.mapNotNull { it.secondaryNumericValue }.maxOrNull() ?: 1.0,
            rightPts.mapNotNull { it.secondaryNumericValue }.maxOrNull() ?: 1.0,
        ).coerceAtLeast(1.0)

        val seriesList = listOf(
            ChartDataSeries("pyr_left", leftName, unit, 0, SeriesMarkerShape.SQUARE, points = leftPts),
            ChartDataSeries("pyr_right", rightName, unit, 1, SeriesMarkerShape.SQUARE, points = rightPts),
        )
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.POPULATION_PYRAMID,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(domainField.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = emptyList(),
        )
        val renderData = ChartRenderData(
            title = "${dataset.name.ifBlank { "Population Pyramid" }} ($leftName vs $rightName)",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.POPULATION_PYRAMID,
            domainAxisTitle = domainField.name,
            valueAxisTitle = if (unit.isNotBlank()) "Count ($unit) — $leftName (-) | $rightName (+)" else "$leftName (-) | $rightName (+)",
            valueUnit = unit,
            domainLabels = domainLabels,
            series = seriesList,
            midpointValue = 0.0,
            minNumericOverride = -maxMag * 1.1,
            maxNumericOverride = maxMag * 1.1,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = numericCols.take(2).mapNotNull { it.rawField },
            groupingField = groupField,
            bindings = listOf(
                ResolvedSeriesBinding("pyr_left", domainField.id, false, leftName, unit, 0, SeriesMarkerShape.SQUARE, false),
                ResolvedSeriesBinding("pyr_right", domainField.id, false, rightName, unit, 1, SeriesMarkerShape.SQUARE, false),
            ),
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
