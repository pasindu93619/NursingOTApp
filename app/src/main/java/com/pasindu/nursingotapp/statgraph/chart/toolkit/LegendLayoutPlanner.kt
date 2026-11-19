package com.pasindu.nursingotapp.statgraph.chart.toolkit

import kotlin.math.max

/**
 * Pure-Kotlin font metrics interface for measuring legend labels.
 */
interface LegendTextMeasurer {
    fun measureWidth(text: String): Float
    fun lineHeight(): Float
}

data class LegendEntryInput(
    val seriesIndex: Int,
    val name: String,
    val unit: String = "",
    val colorIndex: Int = seriesIndex,
    val markerShape: SeriesMarkerShape = SeriesMarkerShape.CIRCLE,
)

data class LegendPlacedItem(
    val entry: LegendEntryInput,
    val displayLabel: String,
    val rowIndex: Int,
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val swatchWidthPx: Float,
    val swatchGapPx: Float,
)

data class LegendLayoutPlan(
    val rowsCount: Int,
    val totalHeightPx: Float,
    val rowHeightPx: Float,
    val items: List<LegendPlacedItem>,
)

/**
 * Pure-Kotlin responsive legend layout planner.
 * Measures every series label with real font metrics, wraps onto additional rows
 * when required by [availableWidthPx], and strips internal debug/formatting suffixes.
 */
object LegendLayoutPlanner {

    fun formatSeriesLabel(series: ChartDataSeries): String {
        val cleanName = series.name
            .replace(" [dash-dot]", "")
            .replace(" [dash]", "")
            .replace(" [dot]", "")
            .trim()
        return if (series.unit.isNotBlank()) {
            "$cleanName (${series.unit.trim()})"
        } else {
            cleanName
        }
    }

    fun formatSeriesLabel(name: String, unit: String = ""): String {
        val cleanName = name
            .replace(" [dash-dot]", "")
            .replace(" [dash]", "")
            .replace(" [dot]", "")
            .trim()
        return if (unit.isNotBlank()) "$cleanName (${unit.trim()})" else cleanName
    }

    fun plan(
        entries: List<LegendEntryInput>,
        availableWidthPx: Float,
        measurer: LegendTextMeasurer,
        swatchWidthPx: Float,
        swatchGapPx: Float,
        horizontalItemGapPx: Float,
        verticalRowGapPx: Float,
    ): LegendLayoutPlan {
        if (entries.isEmpty()) {
            return LegendLayoutPlan(
                rowsCount = 0,
                totalHeightPx = 0f,
                rowHeightPx = 0f,
                items = emptyList(),
            )
        }

        val safeMaxWidth = availableWidthPx.coerceAtLeast(1f)
        val rowHeight = max(swatchWidthPx, measurer.lineHeight()).coerceAtLeast(1f)
        val placed = ArrayList<LegendPlacedItem>(entries.size)

        var rowIndex = 0
        var curX = 0f
        var curY = 0f

        entries.forEach { entry ->
            val label = formatSeriesLabel(entry.name, entry.unit)
            val textWidth = measurer.measureWidth(label).coerceAtLeast(0f)
            val itemWidth = swatchWidthPx + swatchGapPx + textWidth

            if (curX > 0f && curX + itemWidth > safeMaxWidth) {
                rowIndex += 1
                curX = 0f
                curY += rowHeight + verticalRowGapPx
            }

            placed.add(
                LegendPlacedItem(
                    entry = entry,
                    displayLabel = label,
                    rowIndex = rowIndex,
                    xPx = curX,
                    yPx = curY,
                    widthPx = itemWidth,
                    heightPx = rowHeight,
                    swatchWidthPx = swatchWidthPx,
                    swatchGapPx = swatchGapPx,
                )
            )
            curX += itemWidth + horizontalItemGapPx
        }

        val rowsCount = rowIndex + 1
        val totalHeight = rowsCount * rowHeight + (rowsCount - 1) * verticalRowGapPx
        return LegendLayoutPlan(
            rowsCount = rowsCount,
            totalHeightPx = totalHeight,
            rowHeightPx = rowHeight,
            items = placed,
        )
    }
}
