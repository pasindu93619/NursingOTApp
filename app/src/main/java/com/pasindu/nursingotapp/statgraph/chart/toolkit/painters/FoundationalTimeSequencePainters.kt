package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesStrokePattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders lower/upper range bands around a central series ([ChartKind.RANGE_BAND]).
 *
 * Validates `lowerLimit <= upperLimit` per observation; missing or inverted bounds
 * break the band transparently rather than inventing intervals.
 */
class RangeBandChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val bandFillPath = Path()
    private val linePath = Path()
    private val lowerEdgePath = Path()
    private val upperEdgePath = Path()
    private val bandEdgeDash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
    private val upperXScratch = mutableListOf<Float>()
    private val upperYScratch = mutableListOf<Float>()
    private val lowerYScratch = mutableListOf<Float>()

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
        if (!data.hasDrawablePoints) return

        val xScale = geometry.xCategoricalScale ?: return
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val edgeStroke = with(canvas) { 1.25.dp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() }
        val revealRight = plot.left + plot.width * progress.coerceIn(0f, 1f)
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        canvas.clipRect(left = plot.left, top = plot.top - markerRadius * 2f, right = revealRight, bottom = plot.bottom + markerRadius * 2f) {
            data.series.forEach { series ->
                val seriesColor = style.seriesColor(series.colorIndex)
                val sorted = series.points.sortedBy { it.domainIndex }

                // 1. Draw valid contiguous range bands (lower <= upper)
                upperXScratch.clear()
                upperYScratch.clear()
                lowerYScratch.clear()

                fun flushBand() {
                    if (upperXScratch.size >= 2) {
                        bandFillPath.reset()
                        upperEdgePath.reset()
                        lowerEdgePath.reset()
                        bandFillPath.moveTo(upperXScratch[0], upperYScratch[0])
                        upperEdgePath.moveTo(upperXScratch[0], upperYScratch[0])
                        for (i in 1 until upperXScratch.size) {
                            bandFillPath.lineTo(upperXScratch[i], upperYScratch[i])
                            upperEdgePath.lineTo(upperXScratch[i], upperYScratch[i])
                        }
                        lowerEdgePath.moveTo(upperXScratch[0], lowerYScratch[0])
                        for (i in 1 until upperXScratch.size) {
                            lowerEdgePath.lineTo(upperXScratch[i], lowerYScratch[i])
                        }
                        for (i in upperXScratch.size - 1 downTo 0) {
                            bandFillPath.lineTo(upperXScratch[i], lowerYScratch[i])
                        }
                        bandFillPath.close()
                        canvas.drawPath(
                            path = bandFillPath,
                            color = seriesColor.copy(alpha = if (isJournalBw) 0.14f else 0.20f),
                            style = Fill,
                        )
                        canvas.drawPath(
                            path = upperEdgePath,
                            color = seriesColor.copy(alpha = 0.7f),
                            style = Stroke(width = edgeStroke, pathEffect = bandEdgeDash),
                        )
                        canvas.drawPath(
                            path = lowerEdgePath,
                            color = seriesColor.copy(alpha = 0.7f),
                            style = Stroke(width = edgeStroke, pathEffect = bandEdgeDash),
                        )
                    } else if (upperXScratch.size == 1) {
                        // Single-point vertical interval whisker
                        canvas.drawLine(
                            color = seriesColor.copy(alpha = 0.75f),
                            start = Offset(upperXScratch[0], upperYScratch[0]),
                            end = Offset(upperXScratch[0], lowerYScratch[0]),
                            strokeWidth = edgeStroke * 1.4f,
                            cap = StrokeCap.Round,
                        )
                    }
                    upperXScratch.clear()
                    upperYScratch.clear()
                    lowerYScratch.clear()
                }

                for (pt in sorted) {
                    val lo = pt.lowerLimit?.takeIf { it.isFinite() }
                    val hi = pt.upperLimit?.takeIf { it.isFinite() }
                    val px = xScale.pointX(pt.domainIndex)
                    if (px != null && lo != null && hi != null && lo <= hi) {
                        val yHi = geometry.yScale.toPixel(hi)
                        val yLo = geometry.yScale.toPixel(lo)
                        if (yHi != null && yLo != null) {
                            upperXScratch.add(px)
                            upperYScratch.add(yHi)
                            lowerYScratch.add(yLo)
                            continue
                        }
                    }
                    flushBand()
                }
                flushBand()

                // 2. Central series line (breaks on missing numericValue)
                linePath.reset()
                var open = false
                for (pt in sorted) {
                    val v = pt.numericValue?.takeIf { it.isFinite() }
                    val px = xScale.pointX(pt.domainIndex)
                    val py = v?.let { geometry.yScale.toPixel(it) }
                    if (px != null && py != null) {
                        if (!open) {
                            linePath.moveTo(px, py)
                            open = true
                        } else {
                            linePath.lineTo(px, py)
                        }
                    } else {
                        open = false
                    }
                }
                canvas.drawPath(
                    path = linePath,
                    color = seriesColor,
                    style = Stroke(width = lineStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                for (pt in sorted) {
                    val v = pt.numericValue?.takeIf { it.isFinite() } ?: continue
                    val px = xScale.pointX(pt.domainIndex) ?: continue
                    val py = geometry.yScale.toPixel(v) ?: continue
                    markersPainter.drawMarker(
                        canvas = canvas,
                        centerX = px,
                        centerY = py,
                        radiusPx = markerRadius,
                        shape = series.markerShape,
                        fillColor = seriesColor,
                        surfacePunchColor = style.plotBackground,
                        isSelected = interaction.selectedDomainIndex == pt.domainIndex,
                    )
                }
            }
        }
    }
}

/**
 * Renders paired (X, Y) observations connected in their actual sequence ([ChartKind.CONNECTED_SCATTER]).
 *
 * Never sorts points by X; breaks the path whenever either X or Y is missing.
 */
class ConnectedScatterPlotPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val arrowPath = Path()
    private val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    private val dotEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
    private val dashDotEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f, 4f, 6f), 0f)

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
        val xScale = geometry.xNumericScale ?: return
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        if (!data.hasDrawablePoints) return

        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() }
        val arrowLen = with(canvas) { 6.dp.toPx() }
        val clampedProgress = progress.coerceIn(0f, 1f)
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        data.series.forEach { series ->
            val seriesColor = style.seriesColor(series.colorIndex)
            val strokePattern = if (isJournalBw || series.dashed) {
                ChartPaletteTokens.strokePatternAt(series.colorIndex)
            } else {
                SeriesStrokePattern.SOLID
            }
            val pathEffect = when (strokePattern) {
                SeriesStrokePattern.SOLID -> null
                SeriesStrokePattern.DASHED -> dashEffect
                SeriesStrokePattern.DOTTED -> dotEffect
                SeriesStrokePattern.DASH_DOT -> dashDotEffect
            }

            // Preserve original sequence order (do NOT sort by X)
            val pts = series.points
            val maxSegments = ((pts.size - 1).coerceAtLeast(0) * clampedProgress).toInt().coerceAtLeast(
                if (clampedProgress > 0f && pts.size >= 2) 1 else 0
            )

            for (i in 0 until pts.size - 1) {
                if (i > maxSegments && clampedProgress < 1f) break
                val a = pts[i]
                val b = pts[i + 1]
                val axVal = a.secondaryNumericValue?.takeIf { it.isFinite() } ?: continue
                val ayVal = a.numericValue?.takeIf { it.isFinite() } ?: continue
                val bxVal = b.secondaryNumericValue?.takeIf { it.isFinite() } ?: continue
                val byVal = b.numericValue?.takeIf { it.isFinite() } ?: continue

                val ax = xScale.toPixel(axVal) ?: continue
                val ay = geometry.yScale.toPixel(ayVal) ?: continue
                val bx = xScale.toPixel(bxVal) ?: continue
                val by = geometry.yScale.toPixel(byVal) ?: continue

                canvas.drawLine(
                    color = seriesColor,
                    start = Offset(ax, ay),
                    end = Offset(bx, by),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect,
                )

                // Directional arrowhead near target point
                val dx = bx - ax
                val dy = by - ay
                val distSq = dx * dx + dy * dy
                if (distSq > arrowLen * arrowLen * 4f) {
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    val tipX = bx - cos(angle).toFloat() * (markerRadius * 1.4f)
                    val tipY = by - sin(angle).toFloat() * (markerRadius * 1.4f)
                    val spread = 0.45
                    arrowPath.reset()
                    arrowPath.moveTo(tipX, tipY)
                    arrowPath.lineTo(
                        tipX - arrowLen * cos(angle - spread).toFloat(),
                        tipY - arrowLen * sin(angle - spread).toFloat(),
                    )
                    arrowPath.lineTo(
                        tipX - arrowLen * cos(angle + spread).toFloat(),
                        tipY - arrowLen * sin(angle + spread).toFloat(),
                    )
                    arrowPath.close()
                    canvas.drawPath(path = arrowPath, color = seriesColor, style = Fill)
                }
            }

            pts.forEach { pt ->
                val xVal = pt.secondaryNumericValue?.takeIf { it.isFinite() } ?: return@forEach
                val yVal = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val px = xScale.toPixel(xVal) ?: return@forEach
                val py = geometry.yScale.toPixel(yVal) ?: return@forEach
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = px,
                    centerY = py,
                    radiusPx = markerRadius,
                    shape = series.markerShape,
                    fillColor = seriesColor,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == pt.domainIndex,
                )
            }
        }
    }
}

/**
 * Renders recurring cycle positions with chronological sub-trends and per-position mean bars
 * ([ChartKind.CYCLE_PLOT]).
 */
class CyclePlotPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val subLinePath = Path()

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
        if (!data.hasDrawablePoints) return

        val xScale = geometry.xCategoricalScale ?: return
        val categoryCount = data.domainLabels.size.coerceAtLeast(1)
        val slotWidth = (plot.width / categoryCount.toFloat()).coerceAtLeast(12f)
        val halfSpan = slotWidth * 0.34f
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val meanStroke = with(canvas) { 2.5.dp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() * 0.85f }
        val dividerStroke = with(canvas) { 0.8.dp.toPx() }
        val clampedProgress = progress.coerceIn(0f, 1f)

        // Subtle vertical dividers between cycle positions
        for (posIdx in 0 until categoryCount - 1) {
            val c1 = xScale.pointX(posIdx) ?: continue
            val c2 = xScale.pointX(posIdx + 1) ?: continue
            val midX = (c1 + c2) * 0.5f
            canvas.drawLine(
                color = style.gridLineColor.copy(alpha = 0.55f),
                start = Offset(midX, plot.top),
                end = Offset(midX, plot.bottom),
                strokeWidth = dividerStroke,
            )
        }

        val primaryColor = style.seriesColor(0)
        val meanColor = style.seriesColor(1)
        val allPoints = data.series.flatMap { it.points }

        for (posIdx in 0 until categoryCount) {
            val centerX = xScale.pointX(posIdx) ?: continue
            val slotPoints = allPoints.filter { it.domainIndex == posIdx }
            if (slotPoints.isEmpty()) continue

            val n = slotPoints.size
            subLinePath.reset()
            var open = false
            var sum = 0.0
            var validCount = 0

            slotPoints.forEachIndexed { idx, pt ->
                val frac = if (n == 1) 0f else (idx.toFloat() / (n - 1).toFloat()) * 2f - 1f
                val px = centerX + frac * halfSpan
                val v = pt.numericValue?.takeIf { it.isFinite() }
                val py = v?.let { geometry.yScale.toPixel(it) }
                if (v != null && py != null) {
                    sum += v
                    validCount++
                    val animY = plot.bottom + (py - plot.bottom) * clampedProgress
                    if (!open) {
                        subLinePath.moveTo(px, animY)
                        open = true
                    } else {
                        subLinePath.lineTo(px, animY)
                    }
                } else {
                    open = false
                }
            }

            canvas.drawPath(
                path = subLinePath,
                color = primaryColor,
                style = Stroke(width = lineStroke, cap = StrokeCap.Round),
            )

            // Cycle-position mean reference bar
            if (validCount > 0) {
                val meanY = geometry.yScale.toPixel(sum / validCount)
                if (meanY != null) {
                    val animMeanY = plot.bottom + (meanY - plot.bottom) * clampedProgress
                    canvas.drawLine(
                        color = meanColor,
                        start = Offset(centerX - halfSpan, animMeanY),
                        end = Offset(centerX + halfSpan, animMeanY),
                        strokeWidth = meanStroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            slotPoints.forEachIndexed { idx, pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEachIndexed
                val frac = if (n == 1) 0f else (idx.toFloat() / (n - 1).toFloat()) * 2f - 1f
                val px = centerX + frac * halfSpan
                val py = geometry.yScale.toPixel(v) ?: return@forEachIndexed
                val animY = plot.bottom + (py - plot.bottom) * clampedProgress
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = px,
                    centerY = animY,
                    radiusPx = markerRadius,
                    shape = data.series.firstOrNull()?.markerShape
                        ?: com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape.CIRCLE,
                    fillColor = primaryColor,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == posIdx,
                )
            }
        }
    }
}
