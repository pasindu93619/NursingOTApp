package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import com.pasindu.nursingotapp.statgraph.stats.StatisticalMath
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders both [com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.PIE] (`innerHoleFraction = 0f`)
 * and [com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.DONUT] (`innerHoleFraction = 0.56f`).
 */
class PieOrDonutChartPainter(
    private val textMeasurer: TextMeasurer,
    private val innerHoleFraction: Float,
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
        if (!rect.isValid || !data.hasDrawablePoints) return
        val series = data.series.firstOrNull() ?: return
        val slices = series.points.filter { (it.numericValue ?: 0.0) > 0.0 }
        val total = slices.sumOf { it.numericValue ?: 0.0 }
        if (total <= 0.0) return

        val diameter = min(rect.width, rect.height) * 0.78f
        val radius = (diameter * 0.5f).coerceAtLeast(16f)
        val center = Offset(rect.centerX, rect.centerY)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val borderPx = with(canvas) { 1.8.dp.toPx() }

        var startAngle = -90f
        slices.forEachIndexed { idx, pt ->
            val v = pt.numericValue ?: return@forEachIndexed
            val fullSweep = ((v / total) * 360.0).toFloat()
            val sweep = fullSweep * clamped
            val sliceColor = if (isBw) {
                val shade = (0.14f + (idx % 6) * 0.14f).coerceAtMost(0.84f)
                Color(shade, shade, shade, 1f)
            } else {
                style.seriesColor(idx)
            }

            canvas.drawArc(
                color = sliceColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
                style = Fill,
            )
            canvas.drawArc(
                color = style.plotBackground,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = borderPx),
            )

            // Direct percentage label when slice is wide enough to avoid crowding
            if (fullSweep >= 18f && clamped > 0.85f) {
                val midRad = Math.toRadians((startAngle + fullSweep * 0.5f).toDouble())
                val labelRadius = if (innerHoleFraction > 0f) {
                    radius * (innerHoleFraction + (1f - innerHoleFraction) * 0.52f)
                } else {
                    radius * 0.65f
                }
                val lx = center.x + (labelRadius * cos(midRad)).toFloat()
                val ly = center.y + (labelRadius * sin(midRad)).toFloat()
                val pct = (v / total) * 100.0
                val labelText = String.format(Locale.US, "%.0f%%", pct)
                val measured = textMeasurer.measure(labelText, style.axisLabelStyle)
                val labelColor = if (isBw && idx % 6 <= 2) Color.White else style.titleColor
                AxisLabelRenderer.drawTextLabel(
                    canvas = canvas,
                    textLayoutResult = measured,
                    topLeft = Offset(lx - measured.size.width * 0.5f, ly - measured.size.height * 0.5f),
                    color = labelColor,
                )
            }

            startAngle += sweep
        }

        if (innerHoleFraction > 0f) {
            val holeRadius = radius * innerHoleFraction
            canvas.drawCircle(
                color = style.plotBackground,
                radius = holeRadius,
                center = center,
            )
            val totalStr = if (abs(total - total.toLong()) < 1e-6) {
                total.toLong().toString()
            } else {
                String.format(Locale.US, "%.1f", total)
            }
            val centerText = if (data.valueUnit.isNotBlank() && data.valueUnit != "%") {
                "$totalStr ${data.valueUnit}"
            } else {
                totalStr
            }
            val measuredTotal = textMeasurer.measure(centerText, style.titleStyle)
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = measuredTotal,
                topLeft = Offset(center.x - measuredTotal.size.width * 0.5f, center.y - measuredTotal.size.height * 0.5f),
                color = style.titleColor,
            )
        }
    }
}

/**
 * Renders a 3-variable Bubble chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.BUBBLE])
 * where bubble area is proportional to the 3rd numeric variable (`r ~ sqrt(size)`).
 */
class BubbleChartPainter(
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
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val minR = with(canvas) { 4.dp.toPx() }
        val maxR = min(plot.width, plot.height) * 0.11f
        val clamped = progress.coerceIn(0f, 1f)

        val maxSize = data.series
            .flatMap { it.points }
            .mapNotNull { it.lowerLimit?.takeIf { s -> s.isFinite() && s > 0.0 } }
            .maxOrNull() ?: 1.0

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            series.points.forEach { pt ->
                val yVal = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val xVal = pt.secondaryNumericValue?.takeIf { it.isFinite() } ?: return@forEach
                val sizeVal = pt.lowerLimit?.takeIf { it.isFinite() && it >= 0.0 } ?: return@forEach
                val xPx = xScale.toPixel(xVal) ?: return@forEach
                val yPx = yScale.toPixel(yVal) ?: return@forEach

                val r = StatisticalMath.bubbleRadiusFromArea(
                    value = sizeVal,
                    maxValue = maxSize,
                    maxRadiusPx = maxR.coerceAtLeast(minR + 2f),
                    minRadiusPx = minR,
                ) * clamped

                canvas.drawCircle(
                    color = color.copy(alpha = 0.42f),
                    radius = r,
                    center = Offset(xPx, yPx),
                    style = Fill,
                )
                canvas.drawCircle(
                    color = color,
                    radius = r,
                    center = Offset(xPx, yPx),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

/**
 * Renders a Correlation Heatmap ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.CORRELATION_HEATMAP])
 * showing pairwise Pearson correlation `r` in [-1, +1] with diverging color encoding and direct cell annotations.
 */
class CorrelationHeatmapPainter(
    private val textMeasurer: TextMeasurer,
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
        if (!rect.isValid || !data.hasDrawablePoints) return
        val vars = data.correlationVariableNames
        val n = vars.size
        if (n < 2) return

        val measurerMax = vars.maxOfOrNull { textMeasurer.measure(it, style.axisLabelStyle).size.width.toFloat() } ?: 40f
        val leftPad = min(rect.width * 0.28f, measurerMax + with(canvas) { 10.dp.toPx() })
        val topPad = with(canvas) { 26.dp.toPx() }
        val gridSize = min(rect.width - leftPad - 8f, rect.height - topPad - 8f).coerceAtLeast(24f)
        val gridLeft = rect.left + leftPad
        val gridTop = rect.top + topPad
        val cellSize = gridSize / n
        val gap = with(canvas) { 1.5.dp.toPx() }
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clamped = progress.coerceIn(0f, 1f)

        vars.forEachIndexed { idx, name ->
            val shortName = if (name.length > 10) name.take(9) + "…" else name
            val measured = textMeasurer.measure(shortName, style.axisLabelStyle)
            // Left row label
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (gridLeft - measured.size.width - with(canvas) { 6.dp.toPx() }).coerceAtLeast(rect.left),
                    y = gridTop + (idx + 0.5f) * cellSize - measured.size.height * 0.5f,
                ),
                color = style.axisLabelColor,
            )
            // Top column label
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = measured,
                topLeft = Offset(
                    x = gridLeft + (idx + 0.5f) * cellSize - measured.size.width * 0.5f,
                    y = (gridTop - measured.size.height - with(canvas) { 4.dp.toPx() }).coerceAtLeast(rect.top),
                ),
                color = style.axisLabelColor,
            )
        }

        data.correlationCells.forEach { cell ->
            val x = gridLeft + cell.colIndex * cellSize + gap * 0.5f
            val y = gridTop + cell.rowIndex * cellSize + gap * 0.5f
            val w = (cellSize - gap).coerceAtLeast(4f)
            val r = cell.coefficient

            val fillColor = when {
                r == null -> style.gridLineColor.copy(alpha = 0.25f)
                isBw -> {
                    val intensity = (1f - (abs(r).toFloat() * 0.72f * clamped)).coerceIn(0.22f, 0.96f)
                    Color(intensity, intensity, intensity, 1f)
                }
                else -> {
                    val norm = ((r * clamped + 1.0) * 0.5).toFloat().coerceIn(0f, 1f)
                    ChartPaletteTokens.divergingRamp(style.palette, norm)
                }
            }

            canvas.drawRoundRect(
                color = fillColor,
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(3f, 3f),
            )

            val text = r?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
            val measured = textMeasurer.measure(text, style.axisLabelStyle)
            if (measured.size.width <= w * 0.94f && measured.size.height <= w * 0.94f) {
                val textColor = if (r != null && abs(r) > 0.58) Color.White else style.titleColor
                AxisLabelRenderer.drawTextLabel(
                    canvas = canvas,
                    textLayoutResult = measured,
                    topLeft = Offset(x + (w - measured.size.width) * 0.5f, y + (w - measured.size.height) * 0.5f),
                    color = textColor,
                )
            }
        }
    }
}

/**
 * Renders a Waterfall bridge chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.WATERFALL])
 * with floating delta bars, horizontal connector lines, and a zero-anchored cumulative total bar.
 */
class WaterfallChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
) : InteractiveChartPainter {

    private val connectorDash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

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
        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val cornerPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val slotWidth = plot.width / data.domainLabels.size.coerceAtLeast(1)
        val barWidth = (slotWidth * 0.62f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        val series = data.series.firstOrNull() ?: return
        val sortedPts = series.points.sortedBy { it.domainIndex }

        var prevEndX: Float? = null
        var prevEndY: Float? = null

        sortedPts.forEach { pt ->
            val startVal = pt.lowerLimit ?: return@forEach
            val endVal = pt.upperLimit ?: return@forEach
            val centerX = xScale.bandCenter(pt.domainIndex) ?: return@forEach
            val startY = yScale.toPixel(startVal) ?: return@forEach
            val targetEndY = yScale.toPixel(endVal) ?: return@forEach
            val animEndY = startY + (targetEndY - startY) * clamped

            val left = centerX - barWidth * 0.5f
            val top = min(startY, animEndY)
            val height = max(2f, abs(animEndY - startY))

            // Horizontal connector from previous bar's end level
            if (prevEndX != null && prevEndY != null) {
                canvas.drawLine(
                    color = style.gridLineColor,
                    start = Offset(prevEndX, prevEndY),
                    end = Offset(left, prevEndY),
                    strokeWidth = strokePx,
                    pathEffect = connectorDash,
                )
            }

            val isTotal = pt.isConflictFlagged
            val isPositive = (pt.numericValue ?: 0.0) >= 0.0
            val colorIdx = when {
                isTotal -> 2
                isPositive -> 0
                else -> 1
            }

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(colorIdx)
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
                val color = when {
                    isTotal -> style.seriesColor(2)
                    isPositive -> ChartPaletteTokens.divergingRamp(style.palette, 1f)
                    else -> ChartPaletteTokens.divergingRamp(style.palette, 0f)
                }
                canvas.drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
            }

            prevEndX = left + barWidth
            prevEndY = animEndY
        }
    }
}

/**
 * Renders a mirrored Population Pyramid ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.POPULATION_PYRAMID])
 * around a central 0 axis for two opposing non-negative series.
 */
class PopulationPyramidPainter(
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
        val zeroPx = (xScale.toPixel(0.0) ?: plot.centerX).coerceIn(plot.left, plot.right)
        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val barHeight = (rowHeight * 0.66f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val cornerPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(zeroPx, plot.top),
            end = Offset(zeroPx, plot.bottom),
            strokeWidth = strokePx * 1.4f,
        )

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
            data.series.take(2).forEachIndexed { sIdx, series ->
                val value = series.points.firstOrNull { it.domainIndex == dIdx }?.numericValue?.takeIf { it.isFinite() }
                    ?: return@forEachIndexed
                val targetX = xScale.toPixel(value) ?: return@forEachIndexed
                val animX = zeroPx + (targetX - zeroPx) * clamped
                val left = min(zeroPx, animX)
                val width = max(1f, abs(animX - zeroPx))

                if (isBw) {
                    val bwId = JournalBwPatternPainter.barIdentityForSeries(series.colorIndex)
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
                        color = style.seriesColor(series.colorIndex),
                        topLeft = Offset(left, top),
                        size = Size(width, barHeight),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                }
            }
        }
    }
}
