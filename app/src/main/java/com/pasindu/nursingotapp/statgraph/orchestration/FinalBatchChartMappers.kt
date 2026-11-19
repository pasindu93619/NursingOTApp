package com.pasindu.nursingotapp.statgraph.orchestration

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TreemapTileData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ViolinGroupData
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartSpecification
import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.stats.DerivedCellResult
import com.pasindu.nursingotapp.statgraph.stats.StatisticalMath
import java.util.Locale

/**
 * Pure-Kotlin mappers for the final four StatGraph charts:
 * Violin plot, Density plot, Treemap, and Funnel chart.
 */
internal object FinalBatchChartMappers {

    fun mapViolinPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Violin plot requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, null, selection, allowCategoryFallback = true)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)

        var missingCount = 0
        val violinGroups = mutableListOf<ViolinGroupData>()
        var omittedKdeGroups = 0

        fun addGroup(label: String, vals: List<Double>, colorIdx: Int) {
            val summary = StatisticalMath.computeBoxPlot(vals) ?: return
            val kde = StatisticalMath.computeGaussianKde(vals, gridSize = 48, minSamples = 10)
            if (kde == null || kde.isConstant) omittedKdeGroups++
            violinGroups.add(
                ViolinGroupData(
                    groupLabel = label,
                    count = summary.count,
                    min = summary.min,
                    q1 = summary.q1,
                    median = summary.median,
                    q3 = summary.q3,
                    max = summary.max,
                    kdePoints = if (kde != null && !kde.isConstant) kde.points else emptyList(),
                    bandwidth = kde?.bandwidth ?: 0.0,
                    isConstant = kde?.isConstant ?: false,
                    colorIndex = colorIdx,
                ),
            )
        }

        if (groupField != null && groupNames.isNotEmpty()) {
            groupNames.forEachIndexed { gIdx, gName ->
                val vals = mutableListOf<Double>()
                dataset.rows.forEachIndexed { rIdx, row ->
                    if (DatasetToChartRenderDataMapper.extractGroupValue(row, groupField) == gName) {
                        val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                        if (v == null) missingCount++ else vals.add(v)
                    }
                }
                if (vals.isNotEmpty()) addGroup(gName, vals, gIdx)
            }
        } else {
            val vals = mutableListOf<Double>()
            dataset.rows.forEachIndexed { rIdx, row ->
                val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                if (v == null) missingCount++ else vals.add(v)
            }
            if (vals.isNotEmpty()) addGroup(yCol.name, vals, 0)
        }

        if (violinGroups.isEmpty() || violinGroups.sumOf { it.count } < 3) {
            return MappingOutcome.InvalidMapping("Violin plot requires at least 3 valid numeric observations.")
        }

        val domainLabels = violinGroups.map { it.groupLabel }
        val pts = violinGroups.mapIndexed { idx, grp ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = grp.groupLabel,
                numericValue = grp.median,
                lowerLimit = grp.q1,
                upperLimit = grp.q3,
                formattedValue = "Median ${DatasetToChartRenderDataMapper.formatCellValue(grp.median, yCol.unit, "")} (n=${grp.count})",
                unit = yCol.unit,
            )
        }
        val series = ChartDataSeries("violin_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, points = pts)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.VIOLIN,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(yCol.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            add("Gaussian KDE uses Silverman's rule-of-thumb bandwidth with central IQR box and median.")
            if (omittedKdeGroups > 0) {
                add("$omittedKdeGroups group(s) had n < 10 or zero variance: density envelope omitted to avoid synthetic smoothing; central summary shown.")
            }
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Violin Distribution",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.VIOLIN,
            domainAxisTitle = groupField?.name ?: "Group",
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = domainLabels,
            series = listOf(series),
            violinGroups = violinGroups,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = groupField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            bindings = listOf(ResolvedSeriesBinding("violin_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.CIRCLE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapDensityPlot(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Density plot requires a numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, null, selection, allowCategoryFallback = false)
        val groupNames = DatasetToChartRenderDataMapper.extractOrderedGroupNames(dataset, groupField)

        var missingCount = 0
        val seriesList = mutableListOf<ChartDataSeries>()
        val bindings = mutableListOf<ResolvedSeriesBinding>()
        val bandwidthNotes = mutableListOf<String>()

        fun buildKdeSeries(id: String, name: String, vals: List<Double>, colorIdx: Int) {
            val kde = StatisticalMath.computeGaussianKde(vals, gridSize = 48, minSamples = 10) ?: return
            if (kde.isConstant) return
            val pts = kde.points.mapIndexed { idx, (xVal, density) ->
                ChartDatum(
                    domainIndex = idx,
                    domainLabel = DatasetToChartRenderDataMapper.formatPlainNumber(xVal),
                    numericValue = density,
                    secondaryNumericValue = xVal,
                    formattedValue = "KDE=${String.format(Locale.US, "%.4f", density)} at ${DatasetToChartRenderDataMapper.formatPlainNumber(xVal)}",
                    unit = "density",
                )
            }
            seriesList.add(ChartDataSeries(id, name, "density", colorIdx, SeriesMarkerShape.CIRCLE, points = pts))
            bindings.add(ResolvedSeriesBinding(id, yCol.id, yCol.isDerived, name, "density", colorIdx, SeriesMarkerShape.CIRCLE, false))
            bandwidthNotes.add("$name: n=${kde.sampleCount}, h=${DatasetToChartRenderDataMapper.formatPlainNumber(kde.bandwidth)}")
        }

        if (groupField != null && groupNames.size >= 2) {
            groupNames.forEachIndexed { gIdx, gName ->
                val vals = mutableListOf<Double>()
                dataset.rows.forEachIndexed { rIdx, row ->
                    if (DatasetToChartRenderDataMapper.extractGroupValue(row, groupField) == gName) {
                        val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                        if (v == null) missingCount++ else vals.add(v)
                    }
                }
                buildKdeSeries("kde_$gIdx", gName, vals, gIdx)
            }
        } else {
            val vals = mutableListOf<Double>()
            dataset.rows.forEachIndexed { rIdx, row ->
                val v = yCol.extractor(rIdx, row).value?.takeIf { it.isFinite() }
                if (v == null) missingCount++ else vals.add(v)
            }
            buildKdeSeries("kde_0", yCol.name, vals, 0)
        }

        if (seriesList.isEmpty()) {
            return MappingOutcome.InvalidMapping(
                "Density plot requires at least 10 non-constant finite observations to estimate a continuous Gaussian KDE.",
            )
        }

        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.DENSITY,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(yCol.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            add("Estimated continuous probability density (Gaussian kernel, Silverman bandwidth; not observed frequency counts): ${bandwidthNotes.joinToString("; ")}.")
        }

        val domainLabels = seriesList.first().points.map { it.domainLabel }
        val renderData = ChartRenderData(
            title = "${yCol.name} — Kernel Density Estimate",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.DENSITY,
            domainAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueAxisTitle = "Estimated Density (Gaussian KDE)",
            valueUnit = "",
            domainLabels = domainLabels,
            series = seriesList,
            enforceZeroBaseline = true,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = yCol.rawField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            bindings = bindings,
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapTreemap(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.TREEMAP, selection)
            ?: return MappingOutcome.InvalidMapping("Treemap requires a Category domain field.")
        if (domainField.role is FieldRole.Time) {
            return MappingOutcome.InvalidMapping("Treemap is not suitable for sequential time domains; use an Area or Bar chart.")
        }
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Treemap requires a non-negative numeric Measure or Rate field.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()
        val groupField = DatasetToChartRenderDataMapper.resolveGroupingField(dataset, domainField.id, selection, allowCategoryFallback = false)
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)

        var missingCount = 0
        var zeroCount = 0
        val rawEntries = mutableListOf<Triple<String, String?, Double>>()
        for (entry in orderedRows) {
            val v = yCol.extractor(entry.originalIndex, entry.row).value?.takeIf { it.isFinite() }
            if (v == null) {
                missingCount++
                continue
            }
            if (v < 0.0) {
                return MappingOutcome.InvalidMapping("Treemap cannot encode negative weights in '${yCol.name}'.")
            }
            if (v == 0.0) {
                zeroCount++
                continue
            }
            val grp = groupField?.let { DatasetToChartRenderDataMapper.extractGroupValue(entry.row, it) }
            rawEntries.add(Triple(entry.displayLabel, grp, v))
        }

        if (rawEntries.size < 2) {
            return MappingOutcome.InvalidMapping("Treemap requires at least 2 positive non-zero categories.")
        }

        val shares = StatisticalMath.normalizeTreemapAreas(rawEntries.map { it.third })
        val groupColorMap = mutableMapOf<String, Int>()
        val tiles = rawEntries.mapIndexed { idx, (label, grp, value) ->
            val colorIdx = if (grp != null) groupColorMap.getOrPut(grp) { groupColorMap.size } else idx
            TreemapTileData(
                label = label,
                groupLabel = grp,
                value = value,
                share = shares[idx],
                colorIndex = colorIdx,
            )
        }
        val pts = tiles.mapIndexed { idx, tile ->
            val pct = String.format(Locale.US, "%.1f%%", tile.share * 100.0)
            ChartDatum(
                domainIndex = idx,
                domainLabel = tile.label,
                numericValue = tile.value,
                secondaryNumericValue = tile.share * 100.0,
                formattedValue = "${DatasetToChartRenderDataMapper.formatCellValue(tile.value, yCol.unit, "")} ($pct)",
                unit = yCol.unit,
            )
        }
        val series = ChartDataSeries("treemap_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, points = pts)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.TREEMAP,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOfNotNull(domainField.id, yCol.id, groupField?.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            if (zeroCount > 0) {
                add("$zeroCount zero-value category/categories omitted because treemap area cannot encode zero.")
            }
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Hierarchical Treemap",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.TREEMAP,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = tiles.map { it.label },
            series = listOf(series),
            treemapTiles = tiles,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = groupField,
            bindings = listOf(ResolvedSeriesBinding("treemap_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
            missingCount = missingCount,
            notes = notes,
        )
    }

    fun mapFunnel(
        dataset: StatDataset,
        specification: ChartSpecification,
        diagnosticsReport: DatasetDiagnosticsReport,
        derivedResults: Map<String, List<DerivedCellResult>>,
        selection: ChartMappingSelection,
    ): MappingOutcome {
        val domainField = DatasetToChartRenderDataMapper.resolveDomainField(dataset, ChartKind.FUNNEL, selection)
            ?: return MappingOutcome.InvalidMapping("Funnel chart requires a sequential stage Category field.")
        val numericCols = DatasetToChartRenderDataMapper.buildAllNumericColumns(dataset, derivedResults)
        if (numericCols.isEmpty()) {
            return MappingOutcome.InvalidMapping("Funnel chart requires a non-negative numeric stage count or measure.")
        }
        val yCol = numericCols.firstOrNull { it.id in selection.yFieldIds } ?: numericCols.first()

        // Preserve exact sequential stage order from dataset rows (do not sort descending)
        val orderedRows = DatasetToChartRenderDataMapper.buildOrderedRows(dataset, domainField)
        var missingCount = 0
        val stages = mutableListOf<Pair<String, Double>>()
        orderedRows.forEach { entry ->
            val v = yCol.extractor(entry.originalIndex, entry.row).value?.takeIf { it.isFinite() }
            if (v == null) {
                missingCount++
            } else if (v < 0.0) {
                return MappingOutcome.InvalidMapping("Funnel chart requires non-negative stage values.")
            } else {
                stages.add(entry.displayLabel to v)
            }
        }

        if (stages.size < 2) {
            return MappingOutcome.InvalidMapping("Funnel chart requires at least 2 valid sequential stages.")
        }

        val firstVal = stages.first().second
        val isMonotonicDecrease = stages.zipWithNext().all { (a, b) -> b.second <= a.second + 1e-9 } && firstVal > 0.0

        val pts = stages.mapIndexed { idx, (label, v) ->
            val valText = DatasetToChartRenderDataMapper.formatCellValue(v, yCol.unit, "")
            if (isMonotonicDecrease) {
                val retentionPct = (v / firstVal) * 100.0
                val prevVal = if (idx == 0) v else stages[idx - 1].second
                val dropOffPct = if (idx == 0 || prevVal <= 0.0) 0.0 else ((prevVal - v) / prevVal) * 100.0
                val pctLabel = if (idx == 0) "100%" else "${String.format(Locale.US, "%.1f%%", retentionPct)} (-${String.format(Locale.US, "%.1f%%", dropOffPct)})"
                ChartDatum(
                    domainIndex = idx,
                    domainLabel = label,
                    numericValue = v,
                    secondaryNumericValue = retentionPct,
                    lowerLimit = dropOffPct,
                    isConflictFlagged = false,
                    formattedValue = "$valText ($pctLabel)",
                    unit = yCol.unit,
                )
            } else {
                ChartDatum(
                    domainIndex = idx,
                    domainLabel = label,
                    numericValue = v,
                    secondaryNumericValue = null,
                    isConflictFlagged = true,
                    formattedValue = valText,
                    unit = yCol.unit,
                )
            }
        }

        val series = ChartDataSeries("funnel_0", yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, points = pts)
        val notes = DatasetToChartRenderDataMapper.buildClinicalNotes(
            chartKind = ChartKind.FUNNEL,
            diagnostics = DatasetToChartRenderDataMapper.filterRelevantDiagnostics(diagnosticsReport, setOf(domainField.id, yCol.id)),
            missingCount = missingCount,
            duplicateNote = null,
            ySources = listOf(yCol),
        ).toMutableList().apply {
            if (!isMonotonicDecrease) {
                add("Stage values are not monotonically decreasing; conversion and drop-off percentages are omitted.")
            } else {
                add("Sequential stage order preserved; conversion % is relative to '${stages.first().first}'.")
            }
        }

        val renderData = ChartRenderData(
            title = "${yCol.name} — Stage Funnel",
            subtitle = DatasetToChartRenderDataMapper.buildSubtitle(dataset.name, specification.canonicalName, missingCount),
            chartKind = ChartKind.FUNNEL,
            domainAxisTitle = domainField.name,
            valueAxisTitle = DatasetToChartRenderDataMapper.formatAxisTitle(yCol.name, yCol.unit),
            valueUnit = yCol.unit,
            domainLabels = stages.map { it.first },
            series = listOf(series),
            enforceZeroBaseline = true,
            chartWarnings = notes,
        )
        return buildSuccess(
            specification = specification,
            renderData = renderData,
            xAxisField = domainField,
            yAxisFields = listOfNotNull(yCol.rawField),
            groupingField = null,
            bindings = listOf(ResolvedSeriesBinding("funnel_0", yCol.id, yCol.isDerived, yCol.name, yCol.unit, 0, SeriesMarkerShape.SQUARE, false)),
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
