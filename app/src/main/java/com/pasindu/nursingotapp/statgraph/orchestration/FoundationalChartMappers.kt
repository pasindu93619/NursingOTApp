package com.pasindu.nursingotapp.statgraph.orchestration

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
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import com.pasindu.nursingotapp.statgraph.model.toDisplayString
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Specialized pure-Kotlin mappers for Batch 1A Foundational Core Charts.
 * Shares helpers from [DatasetToChartRenderDataMapper] without duplicating logic.
 */
internal object FoundationalChartMappers {

    fun mapSlope(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.SLOPE, selection)
            ?: return MappingOutcome.InvalidMapping("Slope chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Slope chart requires at least one numeric Measure or Rate field.")
        }

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = true)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        // Case A: 2 domain points/periods + Group/Category entities (long format)
        val distinctDomainKeys = orderedRows.map { it.baseKey to it.displayLabel }.distinctBy { it.first }
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        val domainLabels: List<String>
        var missingCount = 0
        var primaryUnit = ""

        if (distinctDomainKeys.size == 2 && groupField != null) {
            domainLabels = distinctDomainKeys.map { it.second }
            val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
            primaryUnit = yCol.unit
            val groups = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
            if (groups.isEmpty()) {
                return MappingOutcome.InvalidMapping("Slope chart requires non-empty entity/group names.")
            }

            groups.forEachIndexed { gIdx, groupName ->
                val pts = distinctDomainKeys.mapIndexed { dIdx, (key, label) ->
                    val matching = orderedRows.filter {
                        it.baseKey == key && DatasetToChartRenderDataMapper.extractGroupValue(it.row, groupField) == groupName
                    }
                    val cell = matching.lastOrNull()?.let { yCol.extractor(it.originalIndex, it.row) }
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
                seriesList.add(
                    ChartDataSeries(
                        id = "slope_$gIdx",
                        name = groupName,
                        unit = yCol.unit,
                        colorIndex = gIdx,
                        markerShape = marker,
                        points = pts,
                    ),
                )
                bindings.add(
                    ResolvedSeriesBinding("slope_$gIdx", yCol.id, yCol.isDerived, groupName, yCol.unit, gIdx, marker, false),
                )
            }
        } else {
            // Case B: Wide format — 2 numeric columns (e.g., Baseline vs Follow-up) across entities/rows
            if (numericCols.size < 2) {
                return MappingOutcome.InvalidMapping(
                    "Slope chart requires either 2 numeric columns (paired conditions) or 2 periods with a Group field.",
                )
            }
            val colA = numericCols.firstOrNull { it.id == selection.yFieldIds.getOrNull(0) } ?: numericCols[0]
            val colB = numericCols.firstOrNull { it.id == selection.yFieldIds.getOrNull(1) && it.id != colA.id }
                ?: numericCols.first { it.id != colA.id }
            domainLabels = listOf(colA.name, colB.name)
            primaryUnit = colA.unit.ifBlank { colB.unit }

            orderedRows.forEachIndexed { idx, indexed ->
                val cA = colA.extractor(indexed.originalIndex, indexed.row)
                val cB = colB.extractor(indexed.originalIndex, indexed.row)
                if (cA.value == null) missingCount++
                if (cB.value == null) missingCount++
                val marker = markerRotation[idx % markerRotation.size]
                val pts = listOf(
                    ChartDatum(0, colA.name, cA.value, formattedValue = cA.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, colA.unit, cA.formattedValue) } ?: "Missing", unit = colA.unit),
                    ChartDatum(1, colB.name, cB.value, formattedValue = cB.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, colB.unit, cB.formattedValue) } ?: "Missing", unit = colB.unit),
                )
                seriesList.add(
                    ChartDataSeries(
                        id = "slope_$idx",
                        name = indexed.displayLabel,
                        unit = primaryUnit,
                        colorIndex = idx,
                        markerShape = marker,
                        points = pts,
                    ),
                )
                bindings.add(
                    ResolvedSeriesBinding("slope_$idx", colA.id, colA.isDerived, indexed.displayLabel, primaryUnit, idx, marker, false),
                )
            }
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.SLOPE,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = numericCols.take(2),
        ).toMutableList().apply {
            add("Two-point comparison only (${domainLabels[0]} vs ${domainLabels[1]}): does not imply a continuous intermediate trend.")
        }

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.SLOPE,
            domainAxisTitle = domainField.name,
            valueAxisTitle = primaryUnit.ifBlank { "Value" },
            valueUnit = primaryUnit,
            domainLabels = domainLabels,
            series = seriesList,
            enforceZeroBaseline = false,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = numericCols.take(2).mapNotNull { it.rawField },
            groupingField = groupField,
            bindings = bindings,
            missingCount = missingCount,
            notes = notes,
        )
    }

    /**
     * Bump chart using standard competition ranking ("1224"):
     * - Highest value in a period receives Rank 1.
     * - Tied values receive the exact same rank; the next distinct value skips accordingly.
     * - Underlying raw measurement is preserved in [ChartDatum.secondaryNumericValue].
     */
    fun mapBump(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.BUMP, selection)
            ?: return MappingOutcome.InvalidMapping("Bump chart requires an ordered Time or Category domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Bump chart requires numeric values to compute ranks.")
        }

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val distinctDomains = orderedRows.map { it.baseKey to it.displayLabel }.distinctBy { it.first }
        if (distinctDomains.size < 2) {
            return MappingOutcome.InvalidMapping("Bump chart requires at least 2 ordered periods.")
        }

        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = true)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        // Extract raw values per (seriesName, domainIndex)
        data class RawSeries(val name: String, val unit: String, val sourceId: String, val isDerived: Boolean, val valuesByDomain: Map<Int, Double?>)
        val rawSeries = mutableListOf<RawSeries>()

        if (groupField != null) {
            val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
            val groups = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)
            groups.forEach { gName ->
                val map = mutableMapOf<Int, Double?>()
                distinctDomains.forEachIndexed { dIdx, (key, _) ->
                    val rowMatch = orderedRows.lastOrNull {
                        it.baseKey == key && DatasetToChartRenderDataMapper.extractGroupValue(it.row, groupField) == gName
                    }
                    map[dIdx] = rowMatch?.let { yCol.extractor(it.originalIndex, it.row).value }
                }
                rawSeries.add(RawSeries(gName, yCol.unit, yCol.id, yCol.isDerived, map))
            }
        } else {
            val selectedCols = if (selection.yFieldIds.isNotEmpty()) {
                numericCols.filter { it.id in selection.yFieldIds }
            } else {
                numericCols
            }
            if (selectedCols.size < 2) {
                return MappingOutcome.InvalidMapping("Bump chart requires at least 2 series (multiple numeric fields or a Group field).")
            }
            selectedCols.forEach { col ->
                val map = mutableMapOf<Int, Double?>()
                distinctDomains.forEachIndexed { dIdx, (key, _) ->
                    val rowMatch = orderedRows.lastOrNull { it.baseKey == key }
                    map[dIdx] = rowMatch?.let { col.extractor(it.originalIndex, it.row).value }
                }
                rawSeries.add(RawSeries(col.name, col.unit, col.id, col.isDerived, map))
            }
        }

        if (rawSeries.size < 2) {
            return MappingOutcome.InvalidMapping("Bump chart requires at least 2 entities/series to rank.")
        }

        // Compute 1224 competition ranks per domainIndex
        val ranksBySeriesAndDomain = Array(rawSeries.size) { mutableMapOf<Int, Int?>() }
        var missingCount = 0
        var tieCount = 0

        for (dIdx in distinctDomains.indices) {
            val present = rawSeries.indices
                .mapNotNull { sIdx -> rawSeries[sIdx].valuesByDomain[dIdx]?.let { sIdx to it } }
                .sortedByDescending { it.second }
            missingCount += (rawSeries.size - present.size)

            var currentRank = 1
            for (pos in present.indices) {
                if (pos > 0) {
                    val prevVal = present[pos - 1].second
                    val curVal = present[pos].second
                    if (abs(curVal - prevVal) > 1e-12) {
                        currentRank = pos + 1
                    } else {
                        tieCount++
                    }
                }
                ranksBySeriesAndDomain[present[pos].first][dIdx] = currentRank
            }
        }

        val domainLabels = distinctDomains.map { it.second }
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()

        rawSeries.forEachIndexed { sIdx, rs ->
            val marker = markerRotation[sIdx % markerRotation.size]
            val pts = distinctDomains.indices.map { dIdx ->
                val rank = ranksBySeriesAndDomain[sIdx][dIdx]
                val rawVal = rs.valuesByDomain[dIdx]
                val formatted = if (rank != null && rawVal != null) {
                    "Rank #$rank (${DatasetToChartRenderDataMapper.formatCellValue(rawVal, rs.unit, "")})"
                } else {
                    "Missing"
                }
                ChartDatum(
                    domainIndex = dIdx,
                    domainLabel = domainLabels[dIdx],
                    numericValue = rank?.toDouble(),
                    secondaryNumericValue = rawVal,
                    formattedValue = formatted,
                    unit = rs.unit,
                )
            }
            seriesList.add(
                ChartDataSeries(
                    id = "bump_$sIdx",
                    name = rs.name,
                    unit = "rank",
                    colorIndex = sIdx,
                    markerShape = marker,
                    points = pts,
                ),
            )
            bindings.add(
                ResolvedSeriesBinding("bump_$sIdx", rs.sourceId, rs.isDerived, rs.name, "rank", sIdx, marker, false),
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.BUMP,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = emptyList(),
        ).toMutableList().apply {
            add("Y-axis shows ordinal Rank (1 = highest) using standard 1224 competition ranking; ties share the same rank (${tieCount} tied pair(s)).")
        }

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.BUMP,
            domainAxisTitle = domainField.name,
            valueAxisTitle = "Rank (1 = highest)",
            valueUnit = "",
            domainLabels = domainLabels,
            series = seriesList,
            enforceZeroBaseline = false,
            minNumericOverride = 1.0,
            maxNumericOverride = rawSeries.size.toDouble(),
            invertYAxis = true,
            isIntegerYAxis = true,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = emptyList(),
            groupingField = groupField,
            bindings = bindings,
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapRangeBand(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.RANGE_BAND, selection)
            ?: return MappingOutcome.InvalidMapping("Range band chart requires a Time or Category domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        val estimateField = dataset.fields.firstOrNull { it.role is FieldRole.EstimateWithLimits }
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val domainLabels = orderedRows.map { it.displayLabel }

        var missingCount = 0
        var invalidBoundsCount = 0
        val points = mutableListOf<ChartDatum>()
        val sourceCol: NumericSourceColumn

        if (estimateField != null) {
            sourceCol = numericCols.first { it.id == estimateField.id }
            orderedRows.forEachIndexed { idx, indexed ->
                val raw = indexed.row.values[estimateField.id]
                if (raw is StatValue.EstimateValue) {
                    val est = raw.estimate?.takeIf { it.isFinite() }
                    val lo = raw.lowerLimit?.takeIf { it.isFinite() }
                    val hi = raw.upperLimit?.takeIf { it.isFinite() }
                    val validBand = lo != null && hi != null && lo <= hi
                    if (lo != null && hi != null && lo > hi) invalidBoundsCount++
                    if (est == null || !validBand) missingCount++
                    points.add(
                        ChartDatum(
                            domainIndex = idx,
                            domainLabel = indexed.displayLabel,
                            numericValue = est,
                            lowerLimit = if (validBand) lo else null,
                            upperLimit = if (validBand) hi else null,
                            formattedValue = est?.let { DatasetToChartRenderDataMapper.formatCellValue(it, estimateField.unit, raw.toDisplayString()) } ?: "Missing",
                            unit = estimateField.unit,
                        ),
                    )
                } else {
                    missingCount++
                    points.add(ChartDatum(idx, indexed.displayLabel, null, formattedValue = "Missing", unit = estimateField.unit))
                }
            }
        } else if (numericCols.size >= 3) {
            // [central, lower, upper]
            val central = numericCols[0]
            val lower = numericCols[1]
            val upper = numericCols[2]
            sourceCol = central
            orderedRows.forEachIndexed { idx, indexed ->
                val c = central.extractor(indexed.originalIndex, indexed.row).value
                val lo = lower.extractor(indexed.originalIndex, indexed.row).value
                val hi = upper.extractor(indexed.originalIndex, indexed.row).value
                val validBand = lo != null && hi != null && lo <= hi
                if (lo != null && hi != null && lo > hi) invalidBoundsCount++
                if (c == null || !validBand) missingCount++
                points.add(
                    ChartDatum(
                        domainIndex = idx,
                        domainLabel = indexed.displayLabel,
                        numericValue = c,
                        lowerLimit = if (validBand) lo else null,
                        upperLimit = if (validBand) hi else null,
                        formattedValue = c?.let { DatasetToChartRenderDataMapper.formatCellValue(it, central.unit, "") } ?: "Missing",
                        unit = central.unit,
                    ),
                )
            }
        } else {
            return MappingOutcome.InvalidMapping(
                "Range band chart requires an Estimate-with-Limits field or 3 numeric fields (Central, Lower, Upper).",
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.RANGE_BAND,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, sourceCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(sourceCol),
        ).toMutableList().apply {
            if (invalidBoundsCount > 0) {
                add("$invalidBoundsCount observation(s) had inverted bounds (lower > upper) and were excluded from the band.")
            }
            if (estimateField == null) {
                add("Range band reflects supplied lower/upper bounds only and does not imply statistical confidence intervals.")
            }
        }

        val series = ChartDataSeries(
            id = "range_band_0",
            name = sourceCol.name,
            unit = sourceCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.CIRCLE,
            points = points,
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.RANGE_BAND,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(sourceCol.name, sourceCol.unit),
            valueUnit = sourceCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(sourceCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("range_band_0", sourceCol.id, sourceCol.isDerived, sourceCol.name, sourceCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapConnectedScatter(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.size < 2) {
            return MappingOutcome.InvalidMapping("Connected scatter plot requires at least 2 numeric fields (X and Y).")
        }
        val xCol = selection.xFieldId?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols[0]
        val yCol = selection.yFieldIds.firstOrNull()?.let { id -> numericCols.firstOrNull { it.id == id && it.id != xCol.id } }
            ?: numericCols.first { it.id != xCol.id }

        var missingCount = 0
        val pts = dataset.rows.mapIndexed { idx, row ->
            val xc = xCol.extractor(idx, row)
            val yc = yCol.extractor(idx, row)
            val pairOk = xc.value != null && yc.value != null
            if (!pairOk) missingCount++
            val label = row.periodOrCategoryLabel.ifBlank { "Step ${idx + 1}" }
            ChartDatum(
                domainIndex = idx,
                domainLabel = label,
                numericValue = if (pairOk) yc.value else null,
                secondaryNumericValue = if (pairOk) xc.value else null,
                formattedValue = if (pairOk) "${xc.formattedValue}, ${yc.formattedValue}" else "Missing",
                unit = yCol.unit,
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.CONNECTED_SCATTER,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(xCol.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(xCol, yCol),
        ).toMutableList().apply {
            add("Points are connected in observation sequence (never sorted by X); missing pairs break the path.")
        }

        val series = ChartDataSeries(
            id = "cscatter_0",
            name = "${yCol.name} vs ${xCol.name}",
            unit = yCol.unit,
            colorIndex = 0,
            markerShape = SeriesMarkerShape.CIRCLE,
            points = pts,
        )
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.CONNECTED_SCATTER,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(xCol.name, xCol.unit),
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = pts.map { it.domainLabel },
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = xCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("cscatter_0", yCol.id, yCol.isDerived, series.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapCyclePlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val timeField = dataset.fields.firstOrNull { it.role is FieldRole.Time }
            ?: return MappingOutcome.InvalidMapping("Cycle plot requires a Time field with recurring cycle positions (e.g., Month or Day).")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Cycle plot requires at least one numeric Measure or Rate field.")
        }
        val yCol = selection.yFieldIds.firstOrNull()?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols.first()
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, timeField)

        val cycleEntries = orderedRows.mapNotNull { indexed ->
            val period = indexed.timePeriod ?: return@mapNotNull null
            val posInfo: Pair<Int, String>
            val instanceStr: String
            when (period) {
                is TimePeriod.Month -> {
                    val m = period.yearMonth.monthValue
                    posInfo = m to period.yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    instanceStr = period.yearMonth.year.toString()
                }
                is TimePeriod.Day -> {
                    val dow = period.date.dayOfWeek
                    posInfo = dow.value to dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    instanceStr = period.date.toString()
                }
                is TimePeriod.Week -> {
                    posInfo = period.weekOfYear to "W${period.weekOfYear.toString().padStart(2, '0')}"
                    instanceStr = period.year.toString()
                }
                is TimePeriod.Year -> {
                    posInfo = period.year to period.year.toString()
                    instanceStr = period.year.toString()
                }
            }
            Triple(posInfo, instanceStr, indexed)
        }

        if (cycleEntries.isEmpty()) {
            return MappingOutcome.InvalidMapping("Cycle plot could not parse valid recurring Time periods.")
        }

        val orderedPositions = cycleEntries.map { it.first }.distinctBy { it.first }.sortedBy { it.first }
        val positionIndexByKey = orderedPositions.mapIndexed { idx, p -> p.first to idx }.toMap()
        val domainLabels = orderedPositions.map { it.second }
        val distinctInstances = cycleEntries.map { it.second }.distinct()

        var missingCount = 0
        val pts = cycleEntries.map { (posInfo, instStr, indexed) ->
            val cell = yCol.extractor(indexed.originalIndex, indexed.row)
            if (cell.value == null) missingCount++
            ChartDatum(
                domainIndex = positionIndexByKey.getValue(posInfo.first),
                domainLabel = posInfo.second,
                numericValue = cell.value,
                formattedValue = cell.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, yCol.unit, cell.formattedValue) } ?: "Missing",
                unit = yCol.unit,
                cycleInstance = instStr,
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.CYCLE_PLOT,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(timeField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            if (distinctInstances.size < 2) {
                add("Insufficient cycle repetition (< 2 full cycles): do not infer recurring seasonality yet.")
            }
        }

        val series = ChartDataSeries("cycle_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, points = pts)
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.CYCLE_PLOT,
            domainAxisTitle = "${timeField.name} (Cycle position)",
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = timeField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("cycle_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapCalendarHeatmap(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val timeField = dataset.fields.firstOrNull { it.role is FieldRole.Time }
            ?: return MappingOutcome.InvalidMapping("Calendar heatmap requires a daily Time field (yyyy-MM-dd).")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Calendar heatmap requires a numeric Measure or Rate field.")
        }
        val yCol = selection.yFieldIds.firstOrNull()?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols.first()

        val dateToValues = linkedMapOf<LocalDate, MutableList<Double?>>()
        dataset.rows.forEachIndexed { idx, row ->
            val cell = row.values[timeField.id]
            val parsedDate = when (cell) {
                is StatValue.TimePeriodValue -> (cell.period as? TimePeriod.Day)?.date
                    ?: runCatching { LocalDate.parse(cell.rawText.trim()) }.getOrNull()
                is StatValue.TextValue -> runCatching { LocalDate.parse(cell.value.trim()) }.getOrNull()
                else -> runCatching { LocalDate.parse(row.periodOrCategoryLabel.trim()) }.getOrNull()
            } ?: (TimePeriodParser.parse(row.periodOrCategoryLabel) as? TimePeriod.Day)?.date

            if (parsedDate != null) {
                val num = yCol.extractor(idx, row).value
                dateToValues.getOrPut(parsedDate) { mutableListOf() }.add(num)
            }
        }

        if (dateToValues.isEmpty()) {
            return MappingOutcome.InvalidMapping("Calendar heatmap requires valid daily dates (yyyy-MM-dd).")
        }

        val sortedDates = dateToValues.keys.sorted()
        var missingCount = 0
        var conflictDateCount = 0

        val pts = sortedDates.mapIndexed { idx, date ->
            val vals = dateToValues.getValue(date).filterNotNull()
            val hasConflict = vals.distinct().size > 1
            if (hasConflict) conflictDateCount++
            val aggValue = if (vals.isEmpty()) {
                missingCount++
                null
            } else {
                vals.average()
            }
            val iso = date.toString()
            ChartDatum(
                domainIndex = idx,
                domainLabel = iso,
                numericValue = aggValue,
                formattedValue = aggValue?.let { DatasetToChartRenderDataMapper.formatCellValue(it, yCol.unit, "") } ?: "Missing",
                unit = yCol.unit,
                cycleInstance = iso,
                isConflictFlagged = hasConflict,
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.CALENDAR_HEATMAP,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(timeField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            add("Missing dates are rendered as hollow outlines and never treated as 0.")
            if (conflictDateCount > 0) {
                add("Date aggregation rule: arithmetic mean used on $conflictDateCount date(s) with conflicting values (flagged with corner markers).")
            }
        }

        val series = ChartDataSeries("calendar_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, points = pts)
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.CALENDAR_HEATMAP,
            domainAxisTitle = timeField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = pts.map { it.domainLabel },
            series = listOf(series),
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = timeField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("calendar_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapBullet(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.BULLET, selection)
            ?: return MappingOutcome.InvalidMapping("Bullet chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.size < 2) {
            return MappingOutcome.InvalidMapping("Bullet chart requires at least 2 numeric fields (Actual value and Target).")
        }

        val actualCol = selection.yFieldIds.getOrNull(0)?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols[0]
        val targetCol = selection.yFieldIds.getOrNull(1)?.let { id -> numericCols.firstOrNull { it.id == id && it.id != actualCol.id } }
            ?: numericCols.first { it.id != actualCol.id }
        val extraRangeCols = numericCols.filter { it.id != actualCol.id && it.id != targetCol.id }.take(3)

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        var missingCount = 0
        var invalidRangeCount = 0

        val pts = orderedRows.mapIndexed { idx, indexed ->
            val actual = actualCol.extractor(indexed.originalIndex, indexed.row)
            val target = targetCol.extractor(indexed.originalIndex, indexed.row)
            if (actual.value == null) missingCount++
            val rawRanges = extraRangeCols.mapNotNull { it.extractor(indexed.originalIndex, indexed.row).value }
            val validRanges = if (rawRanges.isNotEmpty() && rawRanges.zipWithNext().all { (a, b) -> a <= b }) {
                rawRanges
            } else {
                if (rawRanges.isNotEmpty()) invalidRangeCount++
                emptyList()
            }
            ChartDatum(
                domainIndex = idx,
                domainLabel = indexed.displayLabel,
                numericValue = actual.value,
                secondaryNumericValue = target.value,
                formattedValue = actual.value?.let {
                    val aStr = DatasetToChartRenderDataMapper.formatCellValue(it, actualCol.unit, actual.formattedValue)
                    val tStr = target.value?.let { tv -> " (Target ${DatasetToChartRenderDataMapper.formatPlainNumber(tv)})" }.orEmpty()
                    "$aStr$tStr"
                } ?: "Missing",
                unit = actualCol.unit,
                qualitativeRanges = validRanges,
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.BULLET,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, actualCol.id, targetCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(actualCol, targetCol),
        ).toMutableList().apply {
            add("Actual bar anchors at 0; marker indicates '${targetCol.name}'. Qualitative bands (if supplied) represent benchmark thresholds, not clinical reference ranges.")
            if (invalidRangeCount > 0) {
                add("$invalidRangeCount row(s) had non-monotonic qualitative ranges and were omitted.")
            }
        }

        val series = ChartDataSeries("bullet_0", "${actualCol.name} vs ${targetCol.name}", actualCol.unit, 0, SeriesMarkerShape.SQUARE, points = pts)
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.BULLET,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(actualCol.name, actualCol.unit),
            valueUnit = actualCol.unit,
            domainLabels = orderedRows.map { it.displayLabel },
            series = listOf(series),
            enforceZeroBaseline = true,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(actualCol.rawField, targetCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("bullet_0", actualCol.id, actualCol.isDerived, series.name, actualCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapDumbbell(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.DUMBBELL, selection)
            ?: return MappingOutcome.InvalidMapping("Dumbbell chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.size < 2) {
            return MappingOutcome.InvalidMapping("Dumbbell chart requires at least 2 numeric fields to compare per category.")
        }

        val colA = selection.yFieldIds.getOrNull(0)?.let { id -> numericCols.firstOrNull { it.id == id } } ?: numericCols[0]
        val colB = selection.yFieldIds.getOrNull(1)?.let { id -> numericCols.firstOrNull { it.id == id && it.id != colA.id } }
            ?: numericCols.first { it.id != colA.id }

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        var missingCount = 0
        val pts = orderedRows.mapIndexed { idx, indexed ->
            val cA = colA.extractor(indexed.originalIndex, indexed.row)
            val cB = colB.extractor(indexed.originalIndex, indexed.row)
            if (cA.value == null || cB.value == null) missingCount++
            val diffStr = if (cA.value != null && cB.value != null) {
                val delta = cB.value - cA.value
                val sign = if (delta >= 0) "+" else ""
                " (${sign}${DatasetToChartRenderDataMapper.formatPlainNumber(delta)})"
            } else {
                ""
            }
            ChartDatum(
                domainIndex = idx,
                domainLabel = indexed.displayLabel,
                numericValue = cA.value,
                secondaryNumericValue = cB.value,
                formattedValue = "${cA.formattedValue} -> ${cB.formattedValue}$diffStr",
                unit = colA.unit.ifBlank { colB.unit },
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.DUMBBELL,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, colA.id, colB.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(colA, colB),
        ).toMutableList().apply {
            add("Circle = ${colA.name}; Diamond = ${colB.name}. Missing pairs omit the connecting stem.")
        }

        val unit = colA.unit.ifBlank { colB.unit }
        val series = ChartDataSeries("dumbbell_0", "${colA.name} vs ${colB.name}", unit, 0, SeriesMarkerShape.CIRCLE, points = pts)
        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.DUMBBELL,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle("${colA.name} / ${colB.name}", unit),
            valueUnit = unit,
            domainLabels = orderedRows.map { it.displayLabel },
            series = listOf(series),
            enforceZeroBaseline = false,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(colA.rawField, colB.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("dumbbell_0", colA.id, colA.isDerived, series.name, unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapRadar(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.RADAR, selection)
            ?: return MappingOutcome.InvalidMapping("Radar chart requires a Category or Time domain field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Radar chart requires at least one numeric field.")
        }

        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        val markerRotation = DatasetToChartRenderDataMapper.markerRotation

        // Support either: (A) 3-8 rows as axes with 1+ numeric columns as series, OR (B) 3-8 numeric columns as axes with rows as series
        val useRowsAsAxes = orderedRows.size in 3..8
        val domainLabels: List<String>
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        var missingCount = 0
        val distinctUnits = linkedSetOf<String>()
        val axisRanges = mutableListOf<Pair<Double, Double>>()

        if (useRowsAsAxes) {
            val selectedCols = if (selection.yFieldIds.isNotEmpty()) {
                numericCols.filter { it.id in selection.yFieldIds }
            } else {
                numericCols.take(4)
            }
            domainLabels = orderedRows.map { it.displayLabel }
            selectedCols.forEachIndexed { sIdx, col ->
                if (col.unit.isNotBlank()) distinctUnits.add(col.unit.trim().lowercase(Locale.ROOT))
                val colVals = mutableListOf<Double>()
                val pts = orderedRows.mapIndexed { dIdx, indexed ->
                    val cell = col.extractor(indexed.originalIndex, indexed.row)
                    if (cell.value == null) missingCount++ else colVals.add(cell.value)
                    ChartDatum(
                        domainIndex = dIdx,
                        domainLabel = indexed.displayLabel,
                        numericValue = cell.value,
                        formattedValue = cell.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, col.unit, cell.formattedValue) } ?: "Missing",
                        unit = col.unit,
                    )
                }
                if (colVals.isNotEmpty()) axisRanges.add(colVals.min() to colVals.max())
                val marker = markerRotation[sIdx % markerRotation.size]
                seriesList.add(ChartDataSeries("radar_$sIdx", col.name, col.unit, sIdx, marker, points = pts))
                bindings.add(ResolvedSeriesBinding("radar_$sIdx", col.id, col.isDerived, col.name, col.unit, sIdx, marker, false))
            }
        } else if (numericCols.size in 3..8) {
            val axisCols = numericCols
            domainLabels = axisCols.map { it.name }
            axisCols.forEach { col ->
                if (col.unit.isNotBlank()) distinctUnits.add(col.unit.trim().lowercase(Locale.ROOT))
                val vals = orderedRows.mapNotNull { col.extractor(it.originalIndex, it.row).value }
                if (vals.isNotEmpty()) axisRanges.add(vals.min() to vals.max())
            }
            orderedRows.take(5).forEachIndexed { rIdx, indexed ->
                val pts = axisCols.mapIndexed { aIdx, col ->
                    val cell = col.extractor(indexed.originalIndex, indexed.row)
                    if (cell.value == null) missingCount++
                    ChartDatum(
                        domainIndex = aIdx,
                        domainLabel = col.name,
                        numericValue = cell.value,
                        formattedValue = cell.value?.let { DatasetToChartRenderDataMapper.formatCellValue(it, col.unit, cell.formattedValue) } ?: "Missing",
                        unit = col.unit,
                    )
                }
                val marker = markerRotation[rIdx % markerRotation.size]
                seriesList.add(ChartDataSeries("radar_$rIdx", indexed.displayLabel, "", rIdx, marker, points = pts))
                bindings.add(ResolvedSeriesBinding("radar_$rIdx", axisCols.first().id, false, indexed.displayLabel, "", rIdx, marker, false))
            }
        } else {
            return MappingOutcome.InvalidMapping(
                "Radar chart supports 3–8 comparable axes (found ${orderedRows.size} categories and ${numericCols.size} numeric columns).",
            )
        }

        val scaleDisparity = if (axisRanges.size >= 2) {
            val maxOfMax = axisRanges.maxOf { max(abs(it.first), abs(it.second)) }.coerceAtLeast(1e-9)
            val minOfMax = axisRanges.minOf { max(abs(it.first), abs(it.second)) }.coerceAtLeast(1e-9)
            maxOfMax / minOfMax
        } else {
            1.0
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.RADAR,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = numericCols,
        ).toMutableList()

        if (distinctUnits.size > 1) {
            notes.add("Scale comparability warning: axes use different units (${distinctUnits.joinToString(", ")}); values are NOT silently normalized.")
        } else if (scaleDisparity >= 4.0) {
            notes.add("Scale comparability warning: axis magnitudes differ by ${DatasetToChartRenderDataMapper.formatPlainNumber(scaleDisparity)}x; smaller axes may appear compressed.")
        }

        val renderData = ChartRenderData(
            title = dataset.name.ifBlank { specification.canonicalName },
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.RADAR,
            domainAxisTitle = domainField.name,
            valueAxisTitle = distinctUnits.singleOrNull().orEmpty().ifBlank { "Common scale" },
            valueUnit = distinctUnits.singleOrNull().orEmpty(),
            domainLabels = domainLabels,
            series = seriesList,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = numericCols.mapNotNull { it.rawField },
            groupingField = null,
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
