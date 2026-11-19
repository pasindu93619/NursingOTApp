package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnostic
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DiagnosticCode
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import com.pasindu.nursingotapp.statgraph.model.toDisplayString
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldCalculator
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import com.pasindu.nursingotapp.statgraph.stats.DerivedValueStatus
import com.pasindu.nursingotapp.statgraph.stats.StatisticalMath
import java.util.Locale

internal sealed class MappingOutcome {
    data class Success(
        val configuration: ResolvedChartConfiguration,
        val renderData: ChartRenderData,
    ) : MappingOutcome()

    data class InvalidMapping(
        val message: String,
        val diagnostics: List<DatasetDiagnostic> = emptyList(),
    ) : MappingOutcome()
}

internal data class NumericSourceColumn(
    val id: String,
    val name: String,
    val unit: String,
    val isDerived: Boolean,
    val rawField: StatField?,
    val derivedDefinition: DerivedFieldDefinition?,
    val extractor: (rowIndex: Int, row: StatRow) -> ExtractedNumericCell,
)

internal data class ExtractedNumericCell(
    val value: Double?,
    val lowerLimit: Double? = null,
    val upperLimit: Double? = null,
    val formattedValue: String = "Missing",
    val note: String = "",
)

internal data class IndexedRow(
    val originalIndex: Int,
    val row: StatRow,
    val baseKey: String,
    val displayLabel: String,
    val timePeriod: TimePeriod?,
)

/**
 * Pure-Kotlin mapper turning validated [StatDataset] + [ChartSpecification] recommendations into
 * render-ready [ChartRenderData] and [ResolvedChartConfiguration] while preserving missingness,
 * chronological ordering, units, derived ratios, and clinical integrity rules.
 */
object DatasetToChartRenderDataMapper {

    internal val markerRotation = listOf(
        SeriesMarkerShape.CIRCLE,
        SeriesMarkerShape.DIAMOND,
        SeriesMarkerShape.SQUARE,
        SeriesMarkerShape.TRIANGLE,
    )

    internal fun resolveAndMap(
        dataset: StatDataset,
        specification: ChartSpecification,
        chartKind: ChartKind,
        diagnosticsReport: DatasetDiagnosticsReport,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        if (dataset.fields.isEmpty() || dataset.rows.isEmpty()) {
            return MappingOutcome.InvalidMapping("Dataset contains no fields or observations to map.")
        }

        val derivedResults = DerivedFieldCalculator.calculateAll(dataset)

        val rawOutcome = when (chartKind) {
            ChartKind.SCATTER -> mapScatter(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.CONNECTED_SCATTER -> FoundationalChartMappers.mapConnectedScatter(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.SLOPE -> FoundationalChartMappers.mapSlope(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.BUMP -> FoundationalChartMappers.mapBump(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.RANGE_BAND -> FoundationalChartMappers.mapRangeBand(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.CYCLE_PLOT -> FoundationalChartMappers.mapCyclePlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.CALENDAR_HEATMAP -> FoundationalChartMappers.mapCalendarHeatmap(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.BULLET -> FoundationalChartMappers.mapBullet(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.DUMBBELL -> FoundationalChartMappers.mapDumbbell(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.RADAR -> FoundationalChartMappers.mapRadar(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.DIVERGING_BAR -> Batch1BChartMappers.mapDivergingBar(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.LIKERT_DIVERGING -> Batch1BChartMappers.mapLikertDiverging(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.WAFFLE -> Batch1BChartMappers.mapWaffle(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.PARETO -> Batch1BChartMappers.mapPareto(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.ECDF -> Batch1BChartMappers.mapEcdf(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.QQ_PLOT -> Batch1BChartMappers.mapQqPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.STEM_AND_LEAF -> Batch1BChartMappers.mapStemAndLeaf(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.HEXBIN -> Batch1BChartMappers.mapHexbin(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.SCATTERPLOT_MATRIX -> Batch1BChartMappers.mapScatterplotMatrix(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.MOSAIC -> Batch1BChartMappers.mapMosaic(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.SUNBURST -> Batch1BChartMappers.mapSunburst(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.HISTOGRAM -> Batch3ChartMappers.mapHistogram(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.BOX_PLOT -> Batch3ChartMappers.mapBoxPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.STRIP_PLOT -> Batch3ChartMappers.mapStripPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.ERROR_BAR -> Batch3ChartMappers.mapErrorBar(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.RANGE_PLOT -> Batch3ChartMappers.mapRangePlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.FOREST_PLOT -> Batch3ChartMappers.mapForestPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.CONTROL_CHART -> Batch3ChartMappers.mapControlChart(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.KAPLAN_MEIER -> Batch3ChartMappers.mapKaplanMeier(dataset, specification, diagnosticsReport, selection)
            ChartKind.PIE -> Batch4ChartMappers.mapPieOrDonut(dataset, specification, ChartKind.PIE, diagnosticsReport, derivedResults, selection)
            ChartKind.DONUT -> Batch4ChartMappers.mapPieOrDonut(dataset, specification, ChartKind.DONUT, diagnosticsReport, derivedResults, selection)
            ChartKind.BUBBLE -> Batch4ChartMappers.mapBubble(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.CORRELATION_HEATMAP -> Batch4ChartMappers.mapCorrelationHeatmap(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.WATERFALL -> Batch4ChartMappers.mapWaterfall(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.POPULATION_PYRAMID -> Batch4ChartMappers.mapPopulationPyramid(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.VIOLIN -> FinalBatchChartMappers.mapViolinPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.DENSITY -> FinalBatchChartMappers.mapDensityPlot(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.TREEMAP -> FinalBatchChartMappers.mapTreemap(dataset, specification, diagnosticsReport, derivedResults, selection)
            ChartKind.FUNNEL -> FinalBatchChartMappers.mapFunnel(dataset, specification, diagnosticsReport, derivedResults, selection)
            else -> mapCartesianSeries(dataset, specification, chartKind, diagnosticsReport, derivedResults, selection)
        }
        return when (rawOutcome) {
            is MappingOutcome.Success -> rawOutcome.copy(
                renderData = rawOutcome.renderData.copy(
                    preferredLabelMode = selection.labelDisplayMode,
                    preferredBarColorMode = selection.barColorMode,
                    userExplicitlySelectedChart = !selection.chartSpecId.isNullOrBlank(),
                ),
            )
            is MappingOutcome.InvalidMapping -> rawOutcome
        }
    }

    private fun mapScatter(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericColumns = buildAllNumericColumns(dataset, derivedResults)
        if (numericColumns.size < 2) {
            return MappingOutcome.InvalidMapping(
                message = "Scatter plot requires at least two numeric Measure, Rate, or derived numeric fields.",
            )
        }

        val xCol = if (selection.xFieldId != null) {
            numericColumns.firstOrNull { it.id == selection.xFieldId }
                ?: return MappingOutcome.InvalidMapping(
                    message = "Selected X-axis field '${selection.xFieldId}' is missing or non-numeric for scatter plotting.",
                )
        } else {
            numericColumns[0]
        }

        val yId = selection.yFieldIds.firstOrNull() ?: selection.derivedFieldIds.firstOrNull()
        val yCol = if (yId != null) {
            numericColumns.firstOrNull { it.id == yId }
                ?: return MappingOutcome.InvalidMapping(
                    message = "Selected Y-axis field '$yId' is missing or non-numeric for scatter plotting.",
                )
        } else {
            numericColumns.firstOrNull { it.id != xCol.id }
                ?: return MappingOutcome.InvalidMapping("Scatter plot requires two distinct numeric variables.")
        }

        if (xCol.id == yCol.id) {
            return MappingOutcome.InvalidMapping(
                message = "Scatter plot X ('${xCol.name}') and Y ('${yCol.name}') must be distinct fields.",
            )
        }

        val groupField = resolveGroupingField(
            dataset = dataset,
            excludeFieldId = null,
            selection = selection,
            allowCategoryFallback = false,
        )
        val groupKeys = extractOrderedGroupNames(dataset, groupField)

        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        var missingCount = 0

        if (groupField != null && groupKeys.size >= 2) {
            groupKeys.forEachIndexed { gIdx, groupName ->
                val pts = mutableListOf<ChartDatum>()
                dataset.rows.forEachIndexed { rowIdx, row ->
                    val rowGroup = extractGroupValue(row, groupField) ?: return@forEachIndexed
                    if (rowGroup != groupName) return@forEachIndexed
                    val xCell = xCol.extractor(rowIdx, row)
                    val yCell = yCol.extractor(rowIdx, row)
                    val pairValid = xCell.value != null && yCell.value != null
                    if (!pairValid) missingCount++
                    val label = row.periodOrCategoryLabel.ifBlank { "Obs ${rowIdx + 1}" }
                    pts.add(
                        ChartDatum(
                            domainIndex = rowIdx,
                            domainLabel = "$label ($groupName)",
                            numericValue = if (pairValid) yCell.value else null,
                            secondaryNumericValue = if (pairValid) xCell.value else null,
                            formattedValue = if (pairValid) {
                                "${xCell.formattedValue}, ${yCell.formattedValue}"
                            } else {
                                "Missing"
                            },
                            unit = yCol.unit,
                        ),
                    )
                }
                seriesList.add(
                    ChartDataSeries(
                        id = "scatter_$gIdx",
                        name = groupName,
                        unit = yCol.unit,
                        colorIndex = gIdx,
                        markerShape = markerRotation[gIdx % markerRotation.size],
                        dashed = false,
                        points = pts,
                    ),
                )
                bindings.add(
                    ResolvedSeriesBinding(
                        seriesId = "scatter_$gIdx",
                        sourceFieldId = yCol.id,
                        isDerivedField = yCol.isDerived,
                        displayName = groupName,
                        unit = yCol.unit,
                        colorIndex = gIdx,
                        markerShape = markerRotation[gIdx % markerRotation.size],
                        dashed = false,
                    ),
                )
            }
        } else {
            val pts = dataset.rows.mapIndexed { rowIdx, row ->
                val xCell = xCol.extractor(rowIdx, row)
                val yCell = yCol.extractor(rowIdx, row)
                val pairValid = xCell.value != null && yCell.value != null
                if (!pairValid) missingCount++
                val label = row.periodOrCategoryLabel.ifBlank { "Obs ${rowIdx + 1}" }
                ChartDatum(
                    domainIndex = rowIdx,
                    domainLabel = label,
                    numericValue = if (pairValid) yCell.value else null,
                    secondaryNumericValue = if (pairValid) xCell.value else null,
                    formattedValue = if (pairValid) {
                        "${xCell.formattedValue}, ${yCell.formattedValue}"
                    } else {
                        "Missing"
                    },
                    unit = yCol.unit,
                )
            }
            seriesList.add(
                ChartDataSeries(
                    id = "scatter_0",
                    name = "${yCol.name} vs ${xCol.name}",
                    unit = yCol.unit,
                    colorIndex = 0,
                    markerShape = SeriesMarkerShape.CIRCLE,
                    points = pts,
                ),
            )
            bindings.add(
                ResolvedSeriesBinding(
                    seriesId = "scatter_0",
                    sourceFieldId = yCol.id,
                    isDerivedField = yCol.isDerived,
                    displayName = yCol.name,
                    unit = yCol.unit,
                    colorIndex = 0,
                    markerShape = SeriesMarkerShape.CIRCLE,
                    dashed = false,
                ),
            )
        }

        val relevantFieldIds = listOfNotNull(xCol.id, yCol.id, groupField?.id).toSet()
        val relevantDiagnostics = filterRelevantDiagnostics(diagnosticsReport, relevantFieldIds)
        val notes = buildClinicalNotes(
            chartKind = ChartKind.SCATTER,
            diagnostics = relevantDiagnostics,
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(xCol, yCol),
        ).toMutableList().apply {
            add("Descriptive relationship only (${xCol.name} vs ${yCol.name}): correlation does not imply causation.")
        }

        val xTitle = formatAxisTitle(xCol.name, xCol.unit)
        val yTitle = formatAxisTitle(yCol.name, yCol.unit)
        val title = selection.customTitle?.takeIf { it.isNotBlank() }
            ?: "${yCol.name} vs ${xCol.name}"
        val subtitle = buildSubtitle(dataset.name, specification.canonicalName, missingCount)

        val renderData = ChartRenderData(
            title = title,
            subtitle = subtitle,
            chartKind = ChartKind.SCATTER,
            domainAxisTitle = xTitle,
            valueAxisTitle = yTitle,
            valueUnit = yCol.unit,
            domainLabels = dataset.rows.mapIndexed { idx, row -> row.periodOrCategoryLabel.ifBlank { "Obs ${idx + 1}" } },
            series = seriesList,
            enforceZeroBaseline = false,
        )

        val summary = ChartGeometryResolver.buildAccessibleSummary(renderData)
        val config = ResolvedChartConfiguration(
            specification = specification,
            chartKind = ChartKind.SCATTER,
            xAxisField = xCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            seriesBindings = bindings,
            xAxisLabel = xTitle,
            yAxisLabel = yTitle,
            valueUnit = yCol.unit,
            missingObservationCount = missingCount,
            totalDomainCount = dataset.rows.size,
            diagnostics = relevantDiagnostics,
            clinicalNotes = notes,
            accessibleSummary = summary,
        )
        return MappingOutcome.Success(configuration = config, renderData = renderData)
    }

    private fun mapCartesianSeries(
        dataset: StatDataset,
        specification: ChartSpecification,
        chartKind: ChartKind,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = resolveDomainField(dataset, chartKind, selection)
            ?: return MappingOutcome.InvalidMapping(
                message = "Chart '${specification.canonicalName}' requires a valid Time or Category domain field.",
            )

        val isTrendKind = chartKind in setOf(
            ChartKind.LINE,
            ChartKind.MULTI_LINE,
            ChartKind.AREA,
            ChartKind.STACKED_AREA,
            ChartKind.STEP,
        )
        if (isTrendKind && specification.disallowsSequentialTimeComparison && domainField.role is FieldRole.Time) {
            return MappingOutcome.InvalidMapping(
                message = "Selected chart '${specification.canonicalName}' disallows sequential time comparison.",
            )
        }

        val allNumeric = buildAllNumericColumns(dataset, derivedResults)
        val groupField = resolveGroupingField(dataset, excludeFieldId = domainField.id, selection = selection)
        val distinctGroups = extractOrderedGroupNames(dataset, groupField)
        val canSplitByGroup = groupField != null &&
            distinctGroups.size >= 2 &&
            chartKind in setOf(
                ChartKind.MULTI_LINE,
                ChartKind.GROUPED_BAR,
                ChartKind.STACKED_BAR,
                ChartKind.PERCENT_STACKED_BAR,
                ChartKind.HORIZONTAL_BAR,
                ChartKind.DOT_PLOT,
                ChartKind.AREA,
                ChartKind.STACKED_AREA,
                ChartKind.STEP,
            )

        val ySourcesResult = resolveCartesianYColumns(
            dataset = dataset,
            allNumeric = allNumeric,
            chartKind = chartKind,
            canSplitByGroup = canSplitByGroup,
            selection = selection,
        )
        val ySources = when (ySourcesResult) {
            is YColumnSelectionResult.Valid -> ySourcesResult.columns
            is YColumnSelectionResult.Invalid -> return MappingOutcome.InvalidMapping(ySourcesResult.message)
        }

        // Clinical guard for stacked / 100%-stacked / area charts: reject negative numbers cleanly.
        if (chartKind in setOf(
                ChartKind.STACKED_BAR,
                ChartKind.PERCENT_STACKED_BAR,
                ChartKind.AREA,
                ChartKind.STACKED_AREA,
            )
        ) {
            val hasNegatives = dataset.rows.indices.any { rowIdx ->
                val row = dataset.rows[rowIdx]
                ySources.any { col -> (col.extractor(rowIdx, row).value ?: 0.0) < 0.0 }
            }
            if (hasNegatives) {
                return MappingOutcome.InvalidMapping(
                    message = "${specification.canonicalName} cannot display negative values accurately.",
                )
            }
        }

        // Order rows cleanly (chronological sort if Time domain; insertion order otherwise).
        val indexedRows = buildOrderedRows(dataset, domainField)

        val builtSeries = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        val domainLabels: List<String>
        var missingCount = 0
        var duplicateCount = 0

        if (canSplitByGroup) {
            val primaryY = ySources.first()
            // Build aligned domain slots keyed by baseKey + occurrence within the same group
            val slotKeys = mutableListOf<String>()
            val slotLabels = mutableListOf<String>()
            val seenPerGroupSlot = mutableMapOf<Pair<String, String>, Int>()
            val cellMatrix = mutableMapOf<Pair<String, String>, ExtractedNumericCell>()

            indexedRows.forEach { entry ->
                val gName = extractGroupValue(entry.row, groupField) ?: "Unspecified"
                val basePair = entry.baseKey to gName
                val occ = (seenPerGroupSlot[basePair] ?: 0) + 1
                seenPerGroupSlot[basePair] = occ

                val effectiveKey = if (occ == 1) entry.baseKey else "${entry.baseKey}#$occ"
                val effectiveLabel = if (occ == 1) entry.displayLabel else "${entry.displayLabel} (#$occ)"
                if (occ > 1) duplicateCount++

                if (effectiveKey !in slotKeys) {
                    slotKeys.add(effectiveKey)
                    slotLabels.add(effectiveLabel)
                }
                cellMatrix[effectiveKey to gName] = primaryY.extractor(entry.originalIndex, entry.row)
            }

            domainLabels = slotLabels

            val effectiveGroups = distinctGroups.toMutableList().apply {
                val hasUnspecified = indexedRows.any { extractGroupValue(it.row, groupField) == null }
                if (hasUnspecified && "Unspecified" !in this) add("Unspecified")
            }

            effectiveGroups.forEachIndexed { gIdx, gName ->
                val pts = slotKeys.mapIndexed { dIdx, slotKey ->
                    val cell = cellMatrix[slotKey to gName]
                    if (cell == null || cell.value == null) missingCount++
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = slotLabels[dIdx],
                        numericValue = cell?.value,
                        lowerLimit = cell?.lowerLimit,
                        upperLimit = cell?.upperLimit,
                        formattedValue = if (cell?.value == null) "Missing" else formatCellValue(cell.value, primaryY.unit, cell.formattedValue),
                        unit = primaryY.unit,
                    )
                }
                val shape = markerRotation[gIdx % markerRotation.size]
                val dashed = isTrendKind && gIdx % 2 == 1
                builtSeries.add(
                    ChartDataSeries(
                        id = "grp_${gIdx}_$gName",
                        name = gName,
                        unit = primaryY.unit,
                        colorIndex = gIdx,
                        markerShape = shape,
                        dashed = dashed,
                        points = pts,
                    ),
                )
                bindings.add(
                    ResolvedSeriesBinding(
                        seriesId = "grp_${gIdx}_$gName",
                        sourceFieldId = primaryY.id,
                        isDerivedField = primaryY.isDerived,
                        displayName = "$gName (${primaryY.name})",
                        unit = primaryY.unit,
                        colorIndex = gIdx,
                        markerShape = shape,
                        dashed = dashed,
                    ),
                )
            }
        } else {
            // Single or multi-column series over ordered rows, preserving every duplicate domain occurrence explicitly
            val seenBaseCounts = mutableMapOf<String, Int>()
            val effectiveLabels = indexedRows.map { entry ->
                val occ = (seenBaseCounts[entry.baseKey] ?: 0) + 1
                seenBaseCounts[entry.baseKey] = occ
                if (occ == 1) {
                    entry.displayLabel
                } else {
                    duplicateCount++
                    "${entry.displayLabel} (#$occ)"
                }
            }
            domainLabels = effectiveLabels

            ySources.forEachIndexed { sIdx, col ->
                val pts = indexedRows.mapIndexed { dIdx, entry ->
                    val cell = col.extractor(entry.originalIndex, entry.row)
                    if (cell.value == null) missingCount++
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = effectiveLabels[dIdx],
                        numericValue = cell.value,
                        lowerLimit = cell.lowerLimit,
                        upperLimit = cell.upperLimit,
                        formattedValue = if (cell.value == null) "Missing" else formatCellValue(cell.value, col.unit, cell.formattedValue),
                        unit = col.unit,
                    )
                }
                val shape = markerRotation[sIdx % markerRotation.size]
                val dashed = isTrendKind && sIdx % 2 == 1
                builtSeries.add(
                    ChartDataSeries(
                        id = "series_${col.id}",
                        name = col.name,
                        unit = col.unit,
                        colorIndex = sIdx,
                        markerShape = shape,
                        dashed = dashed,
                        points = pts,
                    ),
                )
                bindings.add(
                    ResolvedSeriesBinding(
                        seriesId = "series_${col.id}",
                        sourceFieldId = col.id,
                        isDerivedField = col.isDerived,
                        displayName = col.name,
                        unit = col.unit,
                        colorIndex = sIdx,
                        markerShape = shape,
                        dashed = dashed,
                        metadataNote = col.derivedDefinition?.formulaSummary.orEmpty(),
                    ),
                )
            }
        }

        // Clinical integrity guard: 100%-stacked bar must have valid positive denominator totals.
        if (chartKind == ChartKind.PERCENT_STACKED_BAR) {
            val hasPositiveTotal = domainLabels.indices.any { dIdx ->
                builtSeries.sumOf { s -> s.points.getOrNull(dIdx)?.numericValue ?: 0.0 } > 0.0
            }
            if (!hasPositiveTotal) {
                return MappingOutcome.InvalidMapping(
                    message = "100% stacked chart has zero positive component totals and cannot compute valid percentages.",
                )
            }
        }

        val uniqueUnits = ySources.map { it.unit.trim() }.filter { it.isNotEmpty() }.distinct()
        val effectiveUnit = when {
            chartKind == ChartKind.PERCENT_STACKED_BAR -> "%"
            uniqueUnits.size == 1 -> uniqueUnits.first()
            uniqueUnits.size > 1 -> uniqueUnits.joinToString(" / ")
            else -> ""
        }

        val yAxisTitle = when {
            chartKind == ChartKind.PERCENT_STACKED_BAR -> "Share (%)"
            ySources.size == 1 -> formatAxisTitle(ySources.first().name, ySources.first().unit)
            effectiveUnit.isNotBlank() -> "Value ($effectiveUnit)"
            else -> ySources.joinToString(" & ") { it.name }
        }
        val xAxisTitle = domainField.name
        val duplicateNote = if (duplicateCount > 0) {
            "$duplicateCount duplicate ${domainField.name.lowercase(Locale.US)} entries preserved individually as (#2), (#3)..."
        } else {
            null
        }

        val relevantIds = (listOfNotNull(domainField.id, groupField?.id) + ySources.mapNotNull { it.rawField?.id }).toSet()
        val relevantDiagnostics = filterRelevantDiagnostics(diagnosticsReport, relevantIds)
        val notes = buildClinicalNotes(
            chartKind = chartKind,
            diagnostics = relevantDiagnostics,
            missingCount = missingCount,
            duplicateNote = duplicateNote,
            ySources = ySources,
        ).toMutableList()
        if (uniqueUnits.size > 1) {
            notes.add("Mixed units plotted (${uniqueUnits.joinToString(", ")}); interpret scale carefully.")
        }

        val chartTitle = selection.customTitle?.takeIf { it.isNotBlank() } ?: when {
            canSplitByGroup -> "${ySources.first().name} by ${domainField.name} & ${groupField?.name}"
            ySources.size == 1 -> "${ySources.first().name} by ${domainField.name}"
            else -> "${dataset.name.ifBlank { "Clinical Dataset" }} — ${specification.canonicalName}"
        }

        val renderData = ChartRenderData(
            title = chartTitle,
            subtitle = buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = chartKind,
            domainAxisTitle = xAxisTitle,
            valueAxisTitle = yAxisTitle,
            valueUnit = effectiveUnit,
            domainLabels = domainLabels,
            series = builtSeries,
        )
        val summary = ChartGeometryResolver.buildAccessibleSummary(renderData)
        val config = ResolvedChartConfiguration(
            specification = specification,
            chartKind = chartKind,
            xAxisField = domainField,
            yAxisFields = ySources.mapNotNull { it.rawField },
            groupingField = if (canSplitByGroup) groupField else null,
            seriesBindings = bindings,
            xAxisLabel = xAxisTitle,
            yAxisLabel = yAxisTitle,
            valueUnit = effectiveUnit,
            missingObservationCount = missingCount,
            totalDomainCount = domainLabels.size,
            duplicateDomainNote = duplicateNote,
            diagnostics = relevantDiagnostics,
            clinicalNotes = notes,
            accessibleSummary = summary,
        )
        return MappingOutcome.Success(configuration = config, renderData = renderData)
    }

    private sealed class YColumnSelectionResult {
        data class Valid(val columns: List<NumericSourceColumn>) : YColumnSelectionResult()
        data class Invalid(val message: String) : YColumnSelectionResult()
    }

    private fun resolveCartesianYColumns(
        dataset: StatDataset,
        allNumeric: List<NumericSourceColumn>,
        chartKind: ChartKind,
        canSplitByGroup: Boolean,
        selection: ChartMappingSelection,
    ): YColumnSelectionResult {
        val byId = allNumeric.associateBy { it.id }
        val explicitRawIds = selection.yFieldIds
        val explicitDerivedIds = selection.derivedFieldIds

        if (explicitRawIds.isNotEmpty() || explicitDerivedIds.isNotEmpty()) {
            val picked = mutableListOf<NumericSourceColumn>()
            for (id in explicitRawIds + explicitDerivedIds) {
                val found = byId[id]
                    ?: return YColumnSelectionResult.Invalid(
                        "Field '$id' does not exist or is not a numeric/derived field suitable for the value axis.",
                    )
                if (found !in picked) picked.add(found)
            }
            return maybeConvertPartTotalForStacking(dataset, picked, chartKind)
        }

        if (allNumeric.isEmpty()) {
            return YColumnSelectionResult.Invalid("Dataset has no numeric Measure, Rate, Estimate, or derived numeric fields.")
        }

        val rawFieldsOnly = allNumeric.filter { !it.isDerived }
        val pool = rawFieldsOnly.ifEmpty { allNumeric }

        if (chartKind in setOf(
                ChartKind.LINE,
                ChartKind.BAR,
                ChartKind.HORIZONTAL_BAR,
                ChartKind.LOLLIPOP,
                ChartKind.DOT_PLOT,
                ChartKind.AREA,
                ChartKind.STEP,
            ) || canSplitByGroup
        ) {
            val primary = pool.firstOrNull { it.rawField?.role is FieldRole.PartOfMeasure }
                ?: pool.firstOrNull { it.rawField?.role is FieldRole.Measure }
                ?: pool.first()
            return YColumnSelectionResult.Valid(listOf(primary))
        }

        if (chartKind == ChartKind.MULTI_LINE || chartKind == ChartKind.GROUPED_BAR) {
            // Group measures sharing the same unit as the primary measure (e.g. Dengue Patients + Total Patients)
            val anchor = pool.firstOrNull { it.rawField?.role is FieldRole.PartOfMeasure }
                ?: pool.firstOrNull { it.rawField?.role is FieldRole.Measure }
                ?: pool.first()
            val anchorUnit = anchor.unit.trim()
            val sameUnit = pool.filter { it.unit.trim().equals(anchorUnit, ignoreCase = true) }
            return YColumnSelectionResult.Valid(sameUnit.ifEmpty { listOf(anchor) })
        }

        // STACKED_BAR or PERCENT_STACKED_BAR without group split:
        val picked = pool.filter {
            it.rawField?.role is FieldRole.PartOfMeasure || it.rawField?.role is FieldRole.Measure
        }.ifEmpty { pool }
        return maybeConvertPartTotalForStacking(dataset, picked, chartKind)
    }

    /**
     * Prevents double-counting when stacking a [FieldRole.PartOfMeasure] alongside its parent
     * total [FieldRole.Measure] by computing the mutually exclusive remainder (`Total - Part`).
     */
    private fun maybeConvertPartTotalForStacking(
        dataset: StatDataset,
        picked: List<NumericSourceColumn>,
        chartKind: ChartKind,
    ): YColumnSelectionResult {
        if (chartKind != ChartKind.STACKED_BAR &&
            chartKind != ChartKind.PERCENT_STACKED_BAR &&
            chartKind != ChartKind.STACKED_AREA
        ) {
            return YColumnSelectionResult.Valid(picked)
        }
        if (picked.size != 2) return YColumnSelectionResult.Valid(picked)

        val partCol = picked.firstOrNull { it.rawField?.role is FieldRole.PartOfMeasure } ?: return YColumnSelectionResult.Valid(picked)
        val parentId = (partCol.rawField?.role as FieldRole.PartOfMeasure).parentFieldId
        val totalCol = picked.firstOrNull { it.id == parentId } ?: return YColumnSelectionResult.Valid(picked)

        // Verify part never exceeds total on valid paired rows.
        val hasPartExceedingTotal = dataset.rows.indices.any { idx ->
            val p = partCol.extractor(idx, dataset.rows[idx]).value
            val t = totalCol.extractor(idx, dataset.rows[idx]).value
            p != null && t != null && p > t
        }
        if (hasPartExceedingTotal) {
            return YColumnSelectionResult.Invalid(
                "Cannot construct stacked composition: '${partCol.name}' exceeds '${totalCol.name}' on one or more rows.",
            )
        }

        val remainderCol = NumericSourceColumn(
            id = "${totalCol.id}_remainder",
            name = "Other ${totalCol.name}",
            unit = totalCol.unit,
            isDerived = true,
            rawField = totalCol.rawField,
            derivedDefinition = null,
            extractor = { idx, row ->
                val p = partCol.extractor(idx, row).value
                val t = totalCol.extractor(idx, row).value
                if (p != null && t != null && t >= p) {
                    val rem = t - p
                    ExtractedNumericCell(
                        value = rem,
                        formattedValue = formatPlainNumber(rem),
                    )
                } else {
                    ExtractedNumericCell(value = null, formattedValue = "Missing")
                }
            },
        )
        return YColumnSelectionResult.Valid(listOf(partCol, remainderCol))
    }

    internal fun resolveDomainField(
        dataset: StatDataset,
        chartKind: ChartKind,
        selection: ChartMappingSelection,
    ): StatField? {
        if (selection.xFieldId != null) {
            val explicit = dataset.fields.firstOrNull { it.id == selection.xFieldId } ?: return null
            return if (explicit.role is FieldRole.Time || explicit.role is FieldRole.Category) explicit else null
        }

        val timeFields = dataset.fields.filter { it.role is FieldRole.Time }
        val categoryFields = dataset.fields.filter { it.role is FieldRole.Category }

        return if (chartKind == ChartKind.LINE ||
            chartKind == ChartKind.MULTI_LINE ||
            chartKind == ChartKind.AREA ||
            chartKind == ChartKind.STACKED_AREA ||
            chartKind == ChartKind.STEP ||
            chartKind == ChartKind.CONTROL_CHART ||
            chartKind == ChartKind.BUMP ||
            chartKind == ChartKind.SPARKLINE_MULTIPLES ||
            chartKind == ChartKind.CYCLE_PLOT ||
            chartKind == ChartKind.CALENDAR_HEATMAP
        ) {
            timeFields.firstOrNull() ?: categoryFields.firstOrNull()
        } else {
            // For categorical bars, prefer a Category field that actually varies; otherwise prefer Time.
            val nonSingletonCategory = categoryFields.firstOrNull { distinctNonBlankCount(dataset, it.id) > 1 }
            val nonSingletonTime = timeFields.firstOrNull { distinctNonBlankCount(dataset, it.id) > 1 }
            nonSingletonCategory ?: nonSingletonTime ?: categoryFields.firstOrNull() ?: timeFields.firstOrNull()
        }
    }

    internal fun resolveGroupingField(
        dataset: StatDataset,
        excludeFieldId: String?,
        selection: ChartMappingSelection,
        allowCategoryFallback: Boolean = true,
    ): StatField? {
        if (selection.groupFieldId != null) {
            val found = dataset.fields.firstOrNull { it.id == selection.groupFieldId } ?: return null
            return if (found.id != excludeFieldId && (found.role is FieldRole.Group || found.role is FieldRole.Category)) {
                found
            } else {
                null
            }
        }
        return dataset.fields.firstOrNull { it.id != excludeFieldId && it.role is FieldRole.Group }
            ?: if (allowCategoryFallback) {
                dataset.fields.firstOrNull {
                    it.id != excludeFieldId &&
                        it.role is FieldRole.Category &&
                        distinctNonBlankCount(dataset, it.id) >= 2
                }
            } else {
                null
            }
    }

    internal fun buildOrderedRows(dataset: StatDataset, domainField: StatField): List<IndexedRow> {
        val raw = dataset.rows.mapIndexed { idx, row ->
            val cell = row.values[domainField.id]
            val parsedPeriod = when (cell) {
                is StatValue.TimePeriodValue -> cell.period ?: TimePeriodParser.parse(cell.rawText)
                is StatValue.TextValue -> if (domainField.role is FieldRole.Time) TimePeriodParser.parse(cell.value) else null
                else -> if (domainField.role is FieldRole.Time) TimePeriodParser.parse(row.periodOrCategoryLabel) else null
            }
            val label = when {
                cell != null && !cell.isMissingValue() -> cell.toDisplayString()
                row.periodOrCategoryLabel.isNotBlank() -> row.periodOrCategoryLabel
                else -> "Missing"
            }
            val baseKey = parsedPeriod?.formatIso() ?: label
            IndexedRow(
                originalIndex = idx,
                row = row,
                baseKey = baseKey,
                displayLabel = label,
                timePeriod = parsedPeriod,
            )
        }

        if (domainField.role is FieldRole.Time && raw.any { it.timePeriod != null }) {
            return raw.sortedWith { a, b ->
                when {
                    a.timePeriod != null && b.timePeriod != null -> a.timePeriod.compareTo(b.timePeriod)
                    a.timePeriod != null -> -1
                    b.timePeriod != null -> 1
                    else -> a.originalIndex.compareTo(b.originalIndex)
                }
            }
        }
        return raw
    }

    internal fun buildAllNumericColumns(
        dataset: StatDataset,
        derivedResults: Map<String, List<DerivedCellResult>>,
    ): List<NumericSourceColumn> {
        val rawCols = dataset.fields.filter { it.role.isNumeric }.map { field ->
            NumericSourceColumn(
                id = field.id,
                name = field.name,
                unit = field.unit,
                isDerived = false,
                rawField = field,
                derivedDefinition = null,
                extractor = { _, row ->
                    val cell = row.values[field.id]
                    if (cell.isMissingValue()) {
                        ExtractedNumericCell(value = null, formattedValue = "Missing")
                    } else if (cell is StatValue.EstimateValue) {
                        val est = cell.estimate?.takeIf { it.isFinite() }
                        val lo = cell.lowerLimit?.takeIf { it.isFinite() }
                        val hi = cell.upperLimit?.takeIf { it.isFinite() }
                        val boundsValid = est != null && lo != null && hi != null &&
                            StatisticalMath.validateIntervalBounds(est, lo, hi).isValid
                        ExtractedNumericCell(
                            value = est,
                            lowerLimit = if (boundsValid) lo else null,
                            upperLimit = if (boundsValid) hi else null,
                            formattedValue = if (est == null) "Missing" else cell.toDisplayString(),
                        )
                    } else {
                        val v = cell.asDoubleOrNull()?.takeIf { it.isFinite() }
                        ExtractedNumericCell(
                            value = v,
                            formattedValue = if (v == null) "Missing" else cell.toDisplayString(),
                        )
                    }
                },
            )
        }

        val derivedCols = dataset.derivedFields.map { def ->
            val cells = derivedResults[def.id].orEmpty()
            val effectiveUnit = def.unit.ifBlank { def.calculation.defaultUnit }
            NumericSourceColumn(
                id = def.id,
                name = def.name,
                unit = effectiveUnit,
                isDerived = true,
                rawField = null,
                derivedDefinition = def,
                extractor = { idx, _ ->
                    val cell = cells.getOrNull(idx)
                    val v = if (cell != null && cell.status == DerivedValueStatus.VALID) {
                        cell.numericValue?.takeIf { it.isFinite() }
                    } else {
                        null
                    }
                    ExtractedNumericCell(
                        value = v,
                        formattedValue = if (v == null) "Missing" else cell!!.formattedValue,
                        note = cell?.calculationMetadata.orEmpty(),
                    )
                },
            )
        }

        return rawCols + derivedCols
    }

    internal fun extractOrderedGroupNames(dataset: StatDataset, groupField: StatField?): List<String> {
        if (groupField == null) return emptyList()
        val seen = linkedSetOf<String>()
        dataset.rows.forEach { row ->
            extractGroupValue(row, groupField)?.let { seen.add(it) }
        }
        return seen.toList()
    }

    internal fun extractGroupValue(row: StatRow, groupField: StatField?): String? {
        if (groupField == null) return null
        val cell = row.values[groupField.id]
        if (cell.isMissingValue()) return null
        return cell.toDisplayString().takeIf { it.isNotBlank() && it != "—" }
    }

    private fun distinctNonBlankCount(dataset: StatDataset, fieldId: String): Int =
        dataset.rows.mapNotNull { row ->
            val cell = row.values[fieldId]
            if (cell.isMissingValue()) null else cell.toDisplayString().takeIf { it.isNotBlank() && it != "—" }
        }.distinct().size

    internal fun filterRelevantDiagnostics(
        report: DatasetDiagnosticsReport,
        relevantFieldIds: Set<String>,
    ): List<DatasetDiagnostic> = report.diagnostics.filter { diag ->
        diag.affectedFieldIds.isEmpty() || diag.affectedFieldIds.any { it in relevantFieldIds }
    }

    internal fun buildClinicalNotes(
        chartKind: ChartKind,
        diagnostics: List<DatasetDiagnostic>,
        missingCount: Int,
        duplicateNote: String?,
        ySources: List<NumericSourceColumn>,
    ): List<String> {
        val notes = mutableListOf<String>()
        ySources.filter { it.isDerived && it.derivedDefinition != null }.forEach { col ->
            notes.add("Derived '${col.name}': ${col.derivedDefinition!!.formulaSummary}")
        }
        if (missingCount > 0) {
            notes.add("$missingCount missing observation(s) preserved explicitly without zero imputation.")
        }
        if (duplicateNote != null) {
            notes.add(duplicateNote)
        }
        diagnostics.filter { it.code == DiagnosticCode.SMALL_COUNTS_BELOW_5 }.forEach {
            notes.add("Privacy notice: ${it.message}")
        }
        if (chartKind == ChartKind.BAR ||
            chartKind == ChartKind.GROUPED_BAR ||
            chartKind == ChartKind.STACKED_BAR ||
            chartKind == ChartKind.PERCENT_STACKED_BAR
        ) {
            notes.add("Length-encoded bars anchor at a mandatory zero baseline.")
        }
        return notes.distinct()
    }

    internal fun buildSubtitle(datasetName: String, specName: String, missingCount: Int): String =
        listOfNotNull(
            specName.takeIf { it.isNotBlank() },
            datasetName.takeIf { it.isNotBlank() },
            if (missingCount > 0) "$missingCount missing kept as gap" else null,
        ).joinToString(" • ")

    internal fun formatAxisTitle(name: String, unit: String): String =
        if (unit.isNotBlank()) "$name ($unit)" else name

    internal fun formatCellValue(value: Double, unit: String, rawFormatted: String): String {
        val base = rawFormatted.takeIf { it.isNotBlank() && it != "—" } ?: formatPlainNumber(value)
        return when {
            unit.isBlank() -> base
            base.endsWith(unit) -> base
            unit == "%" -> "$base%"
            else -> "$base $unit"
        }
    }

    internal fun formatPlainNumber(value: Double): String =
        if (!value.isFinite()) "—"
        else if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
