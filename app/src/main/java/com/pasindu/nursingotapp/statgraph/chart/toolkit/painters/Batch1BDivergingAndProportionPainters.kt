package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Renders horizontal diverging bars anchored at [ChartRenderData.midpointValue] (default 0.0)
 * for [ChartKind.DIVERGING_BAR]. Missing values are omitted rather than coerced to 0.
 */
class DivergingBarChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
) : InteractiveChartPainter {

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        val twoPass = preResolvedGeometry?.let { TwoPassAxisLayout(it, null) }
            ?: axisAndGridPainter.resolveTwoPass(rect, data, canvas.toPixelInsets(style), style, canvas)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        val xScale = geometry.xNumericScale ?: return
        val midpoint = data.midpointValue ?: 0.0
        val midPx = xScale.toPixel(midpoint) ?: plot.left

        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val barHeight = (rowHeight * 0.62f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val cornerPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        // Vertical reference line at midpoint
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(midPx, plot.top),
            end = Offset(midPx, plot.bottom),
            strokeWidth = strokePx * 1.4f,
        )

        val primarySeries = data.series.firstOrNull()
        data.domainLabels.forEachIndexed { idx, label ->
            val centerY = plot.top + (idx + 0.5f) * rowHeight
            AxisLabelRenderer.drawYAxisTickLabel(
                canvas = canvas,
                textMeasurer = axisAndGridPainter.textMeasurer,
                text = label,
                yPx = centerY,
                plot = plot,
                labelGapPx = labelGapPx,
                style = style,
            )

            val datum = primarySeries?.points?.firstOrNull { it.domainIndex == idx }
            val value = datum?.numericValue ?: return@forEachIndexed
            val targetPx = xScale.toPixel(value) ?: return@forEachIndexed
            val animatedEndPx = midPx + (targetPx - midPx) * clamped
            val left = min(midPx, animatedEndPx)
            val width = abs(animatedEndPx - midPx).coerceAtLeast(1f)
            val top = centerY - barHeight * 0.5f
            val isPositive = value >= midpoint
            val color = if (isBw) {
                style.seriesColor(if (isPositive) 0 else 1)
            } else {
                ChartPaletteTokens.divergingRamp(style.palette, if (isPositive) 1f else 0f)
            }

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(if (isPositive) 0 else 1)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = left,
                    top = top,
                    width = width,
                    height = barHeight,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = strokePx,
                    cornerRadiusPx = cornerPx,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, barHeight),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }

            if (interaction.selectedDomainIndex == idx) {
                canvas.drawRoundRect(
                    color = style.crosshairColor,
                    topLeft = Offset(left, top),
                    size = Size(width, barHeight),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = strokePx * 1.5f),
                )
            }
        }
    }
}

/**
 * Renders a Likert stacked diverging horizontal bar chart ([ChartKind.LIKERT_DIVERGING])
 * centered at 0%, stacking disagree levels leftward and agree levels rightward.
 * Neutral levels (if present) are split symmetrically across 0%.
 */
class LikertDivergingChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
) : InteractiveChartPainter {

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        val twoPass = preResolvedGeometry?.let { TwoPassAxisLayout(it, null) }
            ?: axisAndGridPainter.resolveTwoPass(rect, data, canvas.toPixelInsets(style), style, canvas)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        val xScale = geometry.xNumericScale ?: return
        val zeroPx = xScale.toPixel(0.0) ?: plot.centerX

        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val barHeight = (rowHeight * 0.62f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        val seriesCount = data.series.size.coerceAtLeast(1)
        val midIndex = seriesCount / 2
        val hasOddNeutral = seriesCount % 2 == 1 && seriesCount >= 3

        data.domainLabels.forEachIndexed { dIdx, label ->
            val centerY = plot.top + (dIdx + 0.5f) * rowHeight
            AxisLabelRenderer.drawYAxisTickLabel(
                canvas = canvas,
                textMeasurer = axisAndGridPainter.textMeasurer,
                text = label,
                yPx = centerY,
                plot = plot,
                labelGapPx = labelGapPx,
                style = style,
            )

            val top = centerY - barHeight * 0.5f
            val neutralVal = if (hasOddNeutral) {
                abs(data.series[midIndex].points.firstOrNull { it.domainIndex == dIdx }?.numericValue ?: 0.0)
            } else 0.0
            val neutralHalf = neutralVal * 0.5

            // Draw neutral centered around 0
            if (hasOddNeutral && neutralVal > 0.0) {
                val leftPx = xScale.toPixel(-neutralHalf * clamped) ?: zeroPx
                val rightPx = xScale.toPixel(neutralHalf * clamped) ?: zeroPx
                drawLikertSegment(
                    canvas = canvas,
                    left = leftPx,
                    top = top,
                    width = max(1f, rightPx - leftPx),
                    height = barHeight,
                    seriesIndex = midIndex,
                    colorIndex = data.series[midIndex].colorIndex,
                    style = style,
                    isBw = isBw,
                )
            }

            // Negative / Disagree side stacked leftward from -neutralHalf
            var leftAcc = -neutralHalf
            val negEnd = if (hasOddNeutral) midIndex - 1 else midIndex - 1
            for (sIdx in negEnd downTo 0) {
                val raw = abs(data.series[sIdx].points.firstOrNull { it.domainIndex == dIdx }?.numericValue ?: 0.0)
                if (raw <= 0.0) continue
                val segRight = xScale.toPixel(leftAcc * clamped) ?: zeroPx
                leftAcc -= raw
                val segLeft = xScale.toPixel(leftAcc * clamped) ?: zeroPx
                drawLikertSegment(
                    canvas = canvas,
                    left = min(segLeft, segRight),
                    top = top,
                    width = max(1f, abs(segRight - segLeft)),
                    height = barHeight,
                    seriesIndex = sIdx,
                    colorIndex = data.series[sIdx].colorIndex,
                    style = style,
                    isBw = isBw,
                )
            }

            // Positive / Agree side stacked rightward from +neutralHalf
            var rightAcc = neutralHalf
            val posStart = if (hasOddNeutral) midIndex + 1 else midIndex
            for (sIdx in posStart until seriesCount) {
                val raw = abs(data.series[sIdx].points.firstOrNull { it.domainIndex == dIdx }?.numericValue ?: 0.0)
                if (raw <= 0.0) continue
                val segLeft = xScale.toPixel(rightAcc * clamped) ?: zeroPx
                rightAcc += raw
                val segRight = xScale.toPixel(rightAcc * clamped) ?: zeroPx
                drawLikertSegment(
                    canvas = canvas,
                    left = min(segLeft, segRight),
                    top = top,
                    width = max(1f, abs(segRight - segLeft)),
                    height = barHeight,
                    seriesIndex = sIdx,
                    colorIndex = data.series[sIdx].colorIndex,
                    style = style,
                    isBw = isBw,
                )
            }

            if (interaction.selectedDomainIndex == dIdx) {
                canvas.drawRect(
                    color = style.crosshairColor,
                    topLeft = Offset(plot.left, top),
                    size = Size(plot.width, barHeight),
                    style = Stroke(width = strokePx * 1.4f),
                )
            }
        }

        // Draw crisp 0% center axis above bars
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(zeroPx, plot.top),
            end = Offset(zeroPx, plot.bottom),
            strokeWidth = strokePx * 1.5f,
        )
    }

    private fun drawLikertSegment(
        canvas: DrawScope,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        seriesIndex: Int,
        colorIndex: Int,
        style: ChartStyle,
        isBw: Boolean,
    ) {
        val color = style.seriesColor(colorIndex)
        if (isBw) {
            val bwId = JournalBwPatternPainter.barIdentityForSeries(seriesIndex)
            JournalBwPatternPainter.drawPatternedRectCompose(
                drawScope = canvas,
                left = left,
                top = top,
                width = width,
                height = height,
                pattern = bwId.fillPattern,
                strokeWidthPx = with(canvas) { style.axisStrokeWidthDp.toPx() },
                outlineStyle = bwId.outlineStyle,
            )
        } else {
            canvas.drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
            )
        }
    }
}

/**
 * Renders a 10x10 unit waffle chart ([ChartKind.WAFFLE]) where each cell represents 1%
 * of the total using Hamilton largest-remainder integer cell counts.
 */
class WaffleChartPainter : InteractiveChartPainter {

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        val insets = canvas.toPixelInsets(style)
        val availLeft = rect.left + insets.right
        val availTop = rect.top + insets.top
        val availWidth = (rect.width - insets.right * 2f).coerceAtLeast(1f)
        val availHeight = (rect.height - insets.top - insets.bottom * 0.4f).coerceAtLeast(1f)
        val gridSide = min(availWidth, availHeight)
        if (gridSide <= 4f || !data.hasDrawablePoints) return

        val gridLeft = availLeft + (availWidth - gridSide) * 0.5f
        val gridTop = availTop + (availHeight - gridSide) * 0.5f
        val gap = with(canvas) { 2.5.dp.toPx() }
        val cellSize = ((gridSide - gap * 9f) / 10f).coerceAtLeast(2f)
        val cornerPx = with(canvas) { 2.dp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        val expandedCategories = buildExpandedCells(data)
        val visibleCount = (100 * progress.coerceIn(0f, 1f)).toInt().coerceIn(0, 100)

        for (cellIdx in 0 until 100) {
            val rowFromBottom = cellIdx / 10
            val col = cellIdx % 10
            val row = 9 - rowFromBottom
            val x = gridLeft + col * (cellSize + gap)
            val y = gridTop + row * (cellSize + gap)

            val catIndex = if (cellIdx < visibleCount) expandedCategories.getOrNull(cellIdx) else null
            if (catIndex == null) {
                canvas.drawRoundRect(
                    color = style.gridLineColor.copy(alpha = 0.35f),
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            } else {
                val series = data.series.getOrNull(catIndex)
                val color = style.seriesColor(series?.colorIndex ?: catIndex)
                if (isBw) {
                    val bwId = JournalBwPatternPainter.barIdentityForSeries(catIndex)
                    JournalBwPatternPainter.drawPatternedRectCompose(
                        drawScope = canvas,
                        left = x,
                        top = y,
                        width = cellSize,
                        height = cellSize,
                        pattern = bwId.fillPattern,
                        strokeWidthPx = strokePx,
                        cornerRadiusPx = cornerPx,
                        outlineStyle = bwId.outlineStyle,
                    )
                } else {
                    canvas.drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                }
                if (interaction.selectedDomainIndex == catIndex) {
                    canvas.drawRoundRect(
                        color = style.crosshairColor,
                        topLeft = Offset(x, y),
                        size = Size(cellSize, cellSize),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = strokePx * 1.4f),
                    )
                }
            }
        }
    }

    private fun buildExpandedCells(data: ChartRenderData): List<Int> {
        val cells = ArrayList<Int>(100)
        data.series.forEachIndexed { sIdx, series ->
            val count = (series.points.firstOrNull()?.numericValue ?: 0.0).toInt().coerceAtLeast(0)
            repeat(count) {
                if (cells.size < 100) cells.add(sIdx)
            }
        }
        return cells
    }
}

/**
 * Renders a Pareto chart ([ChartKind.PARETO]) combining descending frequency/cost bars
 * on the left Y axis with a 0%–100% cumulative percentage curve on a secondary right Y axis.
 */
class ParetoChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val eightyDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        val baseInsets = canvas.toPixelInsets(style)
        val rightMargin = max(baseInsets.right, with(canvas) { 36.dp.toPx() })
        val adjustedInsets = baseInsets.copy(right = rightMargin)
        val twoPass = preResolvedGeometry?.let { TwoPassAxisLayout(it, null) }
            ?: axisAndGridPainter.resolveTwoPass(rect, data, adjustedInsets, style, canvas)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        if (!data.hasDrawablePoints) return

        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val baselineY = (yScale.toPixel(0.0) ?: plot.bottom).coerceIn(plot.top, plot.bottom)
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val cornerPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val lineStrokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() }

        val barSeries = data.series.firstOrNull() ?: return
        val slotWidth = plot.width / data.domainLabels.size.coerceAtLeast(1)
        val barWidth = (slotWidth * 0.62f).coerceAtLeast(with(canvas) { 3.dp.toPx() })
        val barColor = style.seriesColor(barSeries.colorIndex)

        // 1. Bars on primary left Y axis
        barSeries.points.forEach { datum ->
            val value = datum.numericValue ?: return@forEach
            val centerX = xScale.bandCenter(datum.domainIndex) ?: return@forEach
            val targetTop = (yScale.toPixel(value) ?: baselineY).coerceIn(plot.top, plot.bottom)
            val animTop = baselineY + (targetTop - baselineY) * clamped
            val top = min(animTop, baselineY)
            val height = abs(baselineY - animTop).coerceAtLeast(1f)
            val left = centerX - barWidth * 0.5f

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(0)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = left,
                    top = top,
                    width = barWidth,
                    height = height,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = strokePx,
                    cornerRadiusPx = cornerPx,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRoundRect(
                    color = barColor.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }
        }

        // 2. Right Y axis (0%–100%) and 80% Pareto threshold line
        val rightTicks = listOf(0, 50, 80, 100)
        val textMeasurer: TextMeasurer = axisAndGridPainter.textMeasurer
        val rightGap = with(canvas) { 4.dp.toPx() }
        rightTicks.forEach { pct ->
            val yPx = plot.bottom - (pct / 100f) * plot.height
            if (pct == 80) {
                canvas.drawLine(
                    color = style.gridLineColor.copy(alpha = 0.85f),
                    start = Offset(plot.left, yPx),
                    end = Offset(plot.right, yPx),
                    strokeWidth = strokePx,
                    pathEffect = eightyDash,
                )
            }
            val measured = textMeasurer.measure("$pct%", style.axisLabelStyle)
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = measured,
                topLeft = Offset(
                    x = plot.right + rightGap,
                    y = (yPx - measured.size.height * 0.5f).coerceIn(plot.top, plot.bottom - measured.size.height),
                ),
                color = style.axisLabelColor,
            )
        }

        // 3. Cumulative percentage line & markers
        val cumSeries = data.series.getOrNull(1)
        val lineColor = style.seriesColor(cumSeries?.colorIndex ?: 1)
        val cumPoints = (cumSeries?.points ?: barSeries.points).mapNotNull { datum ->
            val pct = if (cumSeries != null) datum.numericValue else datum.secondaryNumericValue
            val xPx = xScale.bandCenter(datum.domainIndex)
            if (pct != null && xPx != null) {
                val yPx = plot.bottom - ((pct.toFloat().coerceIn(0f, 100f)) / 100f) * plot.height
                Triple(datum.domainIndex, Offset(xPx, yPx), datum)
            } else null
        }

        if (cumPoints.size >= 2) {
            val path = Path()
            val maxIdx = max(1, floor((cumPoints.size - 1) * clamped).toInt())
            cumPoints.take(maxIdx + 1).forEachIndexed { idx, (_, pt, _) ->
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = lineStrokePx, cap = StrokeCap.Round),
            )
        }

        cumPoints.forEach { (dIdx, pt, _) ->
            val isSelected = interaction.selectedDomainIndex == dIdx
            markersPainter.drawMarker(
                canvas = canvas,
                centerX = pt.x,
                centerY = pt.y,
                radiusPx = if (isSelected) markerRadiusPx * 1.35f else markerRadiusPx,
                shape = cumSeries?.markerShape ?: SeriesMarkerShape.CIRCLE,
                fillColor = lineColor,
                surfacePunchColor = style.plotBackground,
                isSelected = isSelected,
            )
        }
    }
}
