package com.pasindu.nursingotapp.statgraph.chart.toolkit

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import java.util.Locale

enum class ExportDensityAlternative(
    val title: String,
    val description: String
) {
    SWITCH_TO_LINE_OR_AREA(
        title = "Use line or area chart",
        description = "Recommended for ordered time-series data so all points stay readable without compressing bars."
    ),
    GROUP_INTO_FOUR_WEEK_BLOCKS(
        title = "Group into 4-week blocks",
        description = "Creates a separate 4-week summary view while preserving your original dataset."
    ),
    USE_LANDSCAPE_OR_WIDER_OUTPUT(
        title = "Export in landscape or wider format",
        description = "Increases available plot width so each bar meets the minimum 1.5pt readability floor."
    )
}

data class ExportDensityAssessment(
    val canFit: Boolean,
    val requiredWidthPt: Float,
    val availableWidthPt: Float,
    val estimatedBarWidthPt: Float,
    val totalBars: Int,
    val categoryCount: Int,
    val effectiveSeriesCount: Int,
    val message: String,
    val alternatives: List<ExportDensityAlternative>
)

/**
 * Pure-Kotlin bar density policy for on-screen rendering, exports, and recommendations.
 */
object BarDensityPolicy {
    const val MIN_ON_SCREEN_BAR_DP: Float = 3f
    const val MIN_PDF_BAR_PT: Float = 1.5f
    const val MAX_PHONE_GROUPED_BARS: Int = 40

    const val DEFAULT_INNER_GAP_DP: Float = 3f
    const val DEFAULT_GROUP_GAP_DP: Float = 6f
    const val DEFAULT_INNER_GAP_PT: Float = 1.5f
    const val DEFAULT_GROUP_GAP_PT: Float = 3f

    fun isBarChartKind(chartKind: ChartKind): Boolean = when (chartKind) {
        ChartKind.BAR,
        ChartKind.HORIZONTAL_BAR,
        ChartKind.GROUPED_BAR,
        ChartKind.STACKED_BAR,
        ChartKind.PERCENT_STACKED_BAR,
        ChartKind.HISTOGRAM -> true
        else -> false
    }

    fun isVerticalBarChartKind(chartKind: ChartKind): Boolean = when (chartKind) {
        ChartKind.BAR,
        ChartKind.GROUPED_BAR,
        ChartKind.STACKED_BAR,
        ChartKind.PERCENT_STACKED_BAR,
        ChartKind.HISTOGRAM -> true
        else -> false
    }

    fun calculateEffectiveSeriesPerCategory(
        chartKind: ChartKind,
        seriesCount: Int
    ): Int = when (chartKind) {
        ChartKind.GROUPED_BAR -> seriesCount.coerceAtLeast(1)
        else -> 1
    }

    fun calculateRequiredBarSlotWidth(
        seriesCount: Int,
        isGrouped: Boolean,
        minBarThickness: Float,
        innerGap: Float = 0f,
        groupGap: Float = 0f
    ): Float {
        val effectiveSeries = if (isGrouped) seriesCount.coerceAtLeast(1) else 1
        val safeMinBar = minBarThickness.coerceAtLeast(0f)
        val safeInnerGap = if (effectiveSeries > 1) innerGap.coerceAtLeast(0f) else 0f
        val safeGroupGap = groupGap.coerceAtLeast(0f)
        return effectiveSeries * safeMinBar +
            (effectiveSeries - 1) * safeInnerGap +
            safeGroupGap
    }

    fun calculateRequiredPlotWidth(
        categoryCount: Int,
        seriesCount: Int,
        isGrouped: Boolean,
        minBarThickness: Float,
        innerGap: Float = 0f,
        groupGap: Float = 0f
    ): Float {
        val count = categoryCount.coerceAtLeast(1)
        val slotWidth = calculateRequiredBarSlotWidth(
            seriesCount = seriesCount,
            isGrouped = isGrouped,
            minBarThickness = minBarThickness,
            innerGap = innerGap,
            groupGap = groupGap
        )
        return count * slotWidth
    }

    fun evaluateExportDensity(
        data: ChartRenderData,
        availablePlotWidthPt: Float,
        isOrderedTimeSeries: Boolean,
        minBarPt: Float = MIN_PDF_BAR_PT,
        allowMultiPagePanels: Boolean = false,
    ): ExportDensityAssessment {
        val rawCategoryCount = data.domainLabels.size.coerceAtLeast(1)
        val categoryCount = if (allowMultiPagePanels) {
            rawCategoryCount.coerceAtMost(ChartExportLayoutPlanner.MAX_VERTICAL_CATEGORIES_PER_A4_LANDSCAPE_PANEL)
        } else {
            rawCategoryCount
        }
        val isGrouped = data.chartKind == ChartKind.GROUPED_BAR
        val effectiveSeries = calculateEffectiveSeriesPerCategory(data.chartKind, data.series.size)
        val totalBars = categoryCount * effectiveSeries

        if (!isVerticalBarChartKind(data.chartKind)) {
            return ExportDensityAssessment(
                canFit = true,
                requiredWidthPt = availablePlotWidthPt,
                availableWidthPt = availablePlotWidthPt,
                estimatedBarWidthPt = availablePlotWidthPt,
                totalBars = 0,
                categoryCount = rawCategoryCount,
                effectiveSeriesCount = effectiveSeries,
                message = "",
                alternatives = emptyList()
            )
        }

        val innerGapPt = if (isGrouped && effectiveSeries > 1) DEFAULT_INNER_GAP_PT else 0f
        val groupGapPt = DEFAULT_GROUP_GAP_PT
        val requiredWidthPt = calculateRequiredPlotWidth(
            categoryCount = categoryCount,
            seriesCount = effectiveSeries,
            isGrouped = isGrouped,
            minBarThickness = minBarPt,
            innerGap = innerGapPt,
            groupGap = groupGapPt
        )
        val availableSlotPt = availablePlotWidthPt / categoryCount.toFloat()
        val estimatedBarWidthPt = (
            (availableSlotPt - groupGapPt - (effectiveSeries - 1) * innerGapPt) /
                effectiveSeries.toFloat()
            ).coerceAtLeast(0f)
        val canFit = requiredWidthPt <= availablePlotWidthPt + 0.01f &&
            estimatedBarWidthPt + 0.01f >= minBarPt

        if (canFit) {
            return ExportDensityAssessment(
                canFit = true,
                requiredWidthPt = requiredWidthPt,
                availableWidthPt = availablePlotWidthPt,
                estimatedBarWidthPt = estimatedBarWidthPt,
                totalBars = totalBars,
                categoryCount = categoryCount,
                effectiveSeriesCount = effectiveSeries,
                message = "",
                alternatives = emptyList()
            )
        }

        val alternatives = buildList {
            if (isOrderedTimeSeries) {
                add(ExportDensityAlternative.SWITCH_TO_LINE_OR_AREA)
            }
            add(ExportDensityAlternative.GROUP_INTO_FOUR_WEEK_BLOCKS)
            add(ExportDensityAlternative.USE_LANDSCAPE_OR_WIDER_OUTPUT)
        }

        val message = String.format(
            Locale.US,
            "Chart is too dense for this export width: %d bars (%d categories × %d series) require at least %.0fpt plot width to keep bars at the %.1fpt minimum, but only %.0fpt is available (bars would shrink to %.2fpt).",
            totalBars,
            categoryCount,
            effectiveSeries,
            requiredWidthPt,
            minBarPt,
            availablePlotWidthPt,
            estimatedBarWidthPt
        )

        return ExportDensityAssessment(
            canFit = false,
            requiredWidthPt = requiredWidthPt,
            availableWidthPt = availablePlotWidthPt,
            estimatedBarWidthPt = estimatedBarWidthPt,
            totalBars = totalBars,
            categoryCount = categoryCount,
            effectiveSeriesCount = effectiveSeries,
            message = message,
            alternatives = alternatives
        )
    }

    /**
     * Non-destructively aggregates [dataset] rows into consecutive [blockSize]-row blocks
     * (default 4 weeks). Counts are summed; continuous/proportion/rate measures are averaged;
     * all-missing blocks remain [StatValue.Missing]. Incomplete final blocks are explicitly labeled.
     */
    fun groupIntoFourWeekBlocks(
        dataset: StatDataset,
        blockSize: Int = 4
    ): StatDataset {
        require(blockSize >= 2) { "blockSize must be at least 2" }
        if (dataset.rows.isEmpty()) return dataset

        val chunks = dataset.rows.chunked(blockSize)
        val timeOrCategoryFieldIds = dataset.fields
            .filter { it.role is FieldRole.Time || it.role is FieldRole.Category || it.role is FieldRole.Group }
            .map { it.id }
            .toSet()

        val groupedRows = chunks.mapIndexed { index, chunk ->
            val firstLabel = chunk.first().periodOrCategoryLabel.ifBlank { "Row ${index * blockSize + 1}" }
            val lastLabel = chunk.last().periodOrCategoryLabel.ifBlank { "Row ${index * blockSize + chunk.size}" }
            val isPartial = chunk.size < blockSize
            val blockLabel = when {
                chunk.size == 1 -> "$firstLabel (1 of $blockSize)"
                isPartial -> "$firstLabel–$lastLabel (${chunk.size} of $blockSize)"
                else -> "$firstLabel–$lastLabel"
            }

            val aggregatedValues = mutableMapOf<String, StatValue?>()
            dataset.fields.forEach { field ->
                if (field.id in timeOrCategoryFieldIds) {
                    aggregatedValues[field.id] = StatValue.TextValue(blockLabel)
                } else {
                    val numericValues = chunk.mapNotNull { row ->
                        row.values[field.id].asDoubleOrNull()
                    }
                    if (numericValues.isEmpty()) {
                        aggregatedValues[field.id] = StatValue.Missing
                    } else {
                        val isCountField = field.role == FieldRole.Measure || field.role is FieldRole.PartOfMeasure
                        val aggregated = if (isCountField) {
                            numericValues.sum()
                        } else {
                            numericValues.average()
                        }
                        aggregatedValues[field.id] = if (aggregated % 1.0 == 0.0 && isCountField) {
                            StatValue.WholeNumber(aggregated.toLong())
                        } else {
                            StatValue.DecimalNumber(aggregated)
                        }
                    }
                }
            }

            StatRow(
                periodOrCategoryLabel = blockLabel,
                values = aggregatedValues
            )
        }

        val hasPartialTail = chunks.last().size < blockSize
        val note = if (hasPartialTail) {
            "Grouped into $blockSize-week blocks (final block has ${chunks.last().size} of $blockSize observations)."
        } else {
            "Grouped into $blockSize-week blocks."
        }
        val combinedDescription = listOf(dataset.description.trim(), note)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return dataset.copy(
            id = "${dataset.id}_4wk",
            name = "${dataset.name} (4-week blocks)",
            description = combinedDescription,
            rows = groupedRows
        )
    }
}
