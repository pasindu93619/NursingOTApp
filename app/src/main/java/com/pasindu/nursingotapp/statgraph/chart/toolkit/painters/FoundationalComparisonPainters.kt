package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ComposeAxisTextMeasurer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwFillPattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesStrokePattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Renders paired two-point comparisons across entities ([ChartKind.SLOPE]).
 *
 * Preserves entity identity and breaks/omits the connecting slope whenever either
 * endpoint is missing rather than implying a continuous trend or coercing missing to 0.
 */
class SlopeChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
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
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        if (!data.hasDrawablePoints) return

        val xScale = geometry.xCategoricalScale ?: return
        val x0 = xScale.pointX(0) ?: return
        val x1 = xScale.pointX(1) ?: x0
        val guideStroke = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() }

        // Vertical anchor poles for the two compared conditions/periods
        canvas.drawLine(
            color = style.gridLineColor,
            start = Offset(x0, plot.top),
            end = Offset(x0, plot.bottom),
            strokeWidth = guideStroke,
        )
        if (x1 != x0) {
            canvas.drawLine(
                color = style.gridLineColor,
                start = Offset(x1, plot.top),
                end = Offset(x1, plot.bottom),
                strokeWidth = guideStroke,
            )
        }

        val clampedProgress = progress.coerceIn(0f, 1f)
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        data.series.forEachIndexed { sIdx, series ->
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

            val p0 = series.points.firstOrNull { it.domainIndex == 0 }?.numericValue?.takeIf { it.isFinite() }
            val p1 = series.points.firstOrNull { it.domainIndex == 1 }?.numericValue?.takeIf { it.isFinite() }
            val y0 = p0?.let { geometry.yScale.toPixel(it) }
            val y1 = p1?.let { geometry.yScale.toPixel(it) }

            if (y0 != null && y1 != null && x1 != x0) {
                val endX = x0 + (x1 - x0) * clampedProgress
                val endY = y0 + (y1 - y0) * clampedProgress
                canvas.drawLine(
                    color = seriesColor,
                    start = Offset(x0, y0),
                    end = Offset(endX, endY),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect,
                )
            }

            if (y0 != null) {
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = x0,
                    centerY = y0,
                    radiusPx = markerRadius,
                    shape = series.markerShape,
                    fillColor = seriesColor,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == 0,
                )
            }
            if (y1 != null && clampedProgress >= 0.99f) {
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = x1,
                    centerY = y1,
                    radiusPx = markerRadius,
                    shape = series.markerShape,
                    fillColor = seriesColor,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == 1,
                )
            }
        }
    }
}

/**
 * Renders rank trajectories across ordered periods ([ChartKind.BUMP]).
 *
 * Rank 1 is placed at the top of the plot. Missing observations break the line cleanly.
 */
class BumpChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val linePath = androidx.compose.ui.graphics.Path()
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
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)
        if (!data.hasDrawablePoints) return

        val xScale = geometry.xCategoricalScale ?: return
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() * 1.15f }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() * 1.15f }
        val revealRight = plot.left + plot.width * progress.coerceIn(0f, 1f)
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        canvas.clipRect(left = plot.left, top = plot.top - markerRadius * 2f, right = revealRight, bottom = plot.bottom + markerRadius * 2f) {
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

                val sortedPoints = series.points.sortedBy { it.domainIndex }
                linePath.reset()
                var segmentOpen = false
                for (pt in sortedPoints) {
                    val rankVal = pt.numericValue?.takeIf { it.isFinite() }
                    val px = xScale.pointX(pt.domainIndex)
                    val py = rankVal?.let { geometry.yScale.toPixel(it) }
                    if (px != null && py != null) {
                        if (!segmentOpen) {
                            linePath.moveTo(px, py)
                            segmentOpen = true
                        } else {
                            linePath.lineTo(px, py)
                        }
                    } else {
                        segmentOpen = false
                    }
                }
                canvas.drawPath(
                    path = linePath,
                    color = seriesColor,
                    style = Stroke(
                        width = lineStroke,
                        cap = StrokeCap.Round,
                        pathEffect = pathEffect,
                    ),
                )

                for (pt in sortedPoints) {
                    val rankVal = pt.numericValue?.takeIf { it.isFinite() } ?: continue
                    val px = xScale.pointX(pt.domainIndex) ?: continue
                    val py = geometry.yScale.toPixel(rankVal) ?: continue
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
 * Renders horizontal paired-value comparisons per category ([ChartKind.DUMBBELL]).
 *
 * Uses [AxisLabelRenderer] for horizontal numeric X ticks and left category labels.
 */
class DumbbellChartPainter(
    private val textMeasurer: TextMeasurer,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

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
            ?: AxisLabelRenderer.resolveTwoPassGeometry(
                outerRect = rect,
                data = data,
                baseInsets = canvas.toPixelInsets(style),
                style = style,
                textMeasurer = textMeasurer,
                density = canvas,
            )
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        val xScale = geometry.xNumericScale ?: return
        if (!plot.isValid) return

        val gridStrokePx = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val axisStrokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val labelGapPx = with(canvas) { 6.dp.toPx() }

        // Vertical numeric gridlines
        for (i in xScale.ticks.indices) {
            val xPx = xScale.toPixel(xScale.ticks[i]) ?: continue
            if (xPx < plot.left - 1f || xPx > plot.right + 1f) continue
            canvas.drawLine(
                color = style.gridLineColor,
                start = Offset(xPx, plot.top),
                end = Offset(xPx, plot.bottom),
                strokeWidth = gridStrokePx,
                pathEffect = gridDashEffect,
            )
        }
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(plot.left, plot.bottom),
            end = Offset(plot.right, plot.bottom),
            strokeWidth = axisStrokePx,
        )

        val xPlan = twoPass.xLabelPlan ?: AxisLabelRenderer.planXAxis(
            geometry = geometry,
            data = data,
            style = style,
            measurer = ComposeAxisTextMeasurer(textMeasurer, style.axisLabelStyle, canvas),
            density = canvas,
        )
        if (xPlan != null) {
            AxisLabelRenderer.drawPlannedXAxisLabels(
                canvas = canvas,
                textMeasurer = textMeasurer,
                plan = xPlan,
                geometry = geometry,
                labelGapPx = labelGapPx,
                style = style,
                selectedDomainIndex = null,
            )
        }

        val labels = data.domainLabels
        if (labels.isEmpty()) return
        val rowCount = labels.size
        val rowStep = plot.height / rowCount.toFloat()
        val stemStroke = with(canvas) { 2.5.dp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() * 1.25f }
        val colorA = style.seriesColor(0)
        val colorB = style.seriesColor(1)
        val points = data.series.firstOrNull()?.points.orEmpty()
        val clampedProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until rowCount) {
            val cy = plot.top + rowStep * (i + 0.5f)
            canvas.drawLine(
                color = style.gridLineColor.copy(alpha = 0.45f),
                start = Offset(plot.left, cy),
                end = Offset(plot.right, cy),
                strokeWidth = gridStrokePx * 0.8f,
            )
            AxisLabelRenderer.drawYAxisTickLabel(
                canvas = canvas,
                textMeasurer = textMeasurer,
                text = labels[i],
                yPx = cy,
                plot = plot,
                labelGapPx = labelGapPx,
                style = style,
            )

            val datum = points.firstOrNull { it.domainIndex == i } ?: continue
            val valA = datum.numericValue?.takeIf { it.isFinite() }
            val valB = datum.secondaryNumericValue?.takeIf { it.isFinite() }
            val xA = valA?.let { xScale.toPixel(it) }
            val xB = valB?.let { xScale.toPixel(it) }
            val isSelected = interaction.selectedDomainIndex == i

            if (xA != null && xB != null) {
                val midX = (xA + xB) * 0.5f
                val animXA = midX + (xA - midX) * clampedProgress
                val animXB = midX + (xB - midX) * clampedProgress
                canvas.drawLine(
                    color = style.axisLabelColor.copy(alpha = 0.7f),
                    start = Offset(animXA, cy),
                    end = Offset(animXB, cy),
                    strokeWidth = stemStroke,
                    cap = StrokeCap.Round,
                )
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = animXA,
                    centerY = cy,
                    radiusPx = markerRadius,
                    shape = SeriesMarkerShape.CIRCLE,
                    fillColor = colorA,
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = animXB,
                    centerY = cy,
                    radiusPx = markerRadius,
                    shape = SeriesMarkerShape.DIAMOND,
                    fillColor = colorB,
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
            } else if (xA != null) {
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xA,
                    centerY = cy,
                    radiusPx = markerRadius,
                    shape = SeriesMarkerShape.CIRCLE,
                    fillColor = colorA,
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
            } else if (xB != null) {
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xB,
                    centerY = cy,
                    radiusPx = markerRadius,
                    shape = SeriesMarkerShape.DIAMOND,
                    fillColor = colorB,
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
            }
        }
    }
}

/**
 * Renders Stephen Few-style horizontal bullet graphs ([ChartKind.BULLET]):
 * - Optional qualitative background bands (poor / satisfactory / good) validated ascending
 * - Actual performance bar anchored at 0
 * - Crisp perpendicular target marker
 */
class BulletChartPainter(
    private val textMeasurer: TextMeasurer,
) : InteractiveChartPainter {
    private val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

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
            ?: AxisLabelRenderer.resolveTwoPassGeometry(
                outerRect = rect,
                data = data,
                baseInsets = canvas.toPixelInsets(style),
                style = style,
                textMeasurer = textMeasurer,
                density = canvas,
            )
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        val xScale = geometry.xNumericScale ?: return
        if (!plot.isValid) return

        val gridStrokePx = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val axisStrokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val labelGapPx = with(canvas) { 6.dp.toPx() }

        for (i in xScale.ticks.indices) {
            val xPx = xScale.toPixel(xScale.ticks[i]) ?: continue
            if (xPx < plot.left - 1f || xPx > plot.right + 1f) continue
            canvas.drawLine(
                color = style.gridLineColor,
                start = Offset(xPx, plot.top),
                end = Offset(xPx, plot.bottom),
                strokeWidth = gridStrokePx,
                pathEffect = gridDashEffect,
            )
        }
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(plot.left, plot.bottom),
            end = Offset(plot.right, plot.bottom),
            strokeWidth = axisStrokePx,
        )

        val xPlan = twoPass.xLabelPlan ?: AxisLabelRenderer.planXAxis(
            geometry = geometry,
            data = data,
            style = style,
            measurer = ComposeAxisTextMeasurer(textMeasurer, style.axisLabelStyle, canvas),
            density = canvas,
        )
        if (xPlan != null) {
            AxisLabelRenderer.drawPlannedXAxisLabels(
                canvas = canvas,
                textMeasurer = textMeasurer,
                plan = xPlan,
                geometry = geometry,
                labelGapPx = labelGapPx,
                style = style,
                selectedDomainIndex = null,
            )
        }

        val labels = data.domainLabels
        if (labels.isEmpty()) return
        val rowCount = labels.size
        val rowStep = plot.height / rowCount.toFloat()
        val bandHeight = (rowStep * 0.62f).coerceAtLeast(with(canvas) { 10.dp.toPx() })
        val barHeight = (bandHeight * 0.46f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val zeroX = xScale.toPixel(0.0) ?: plot.left
        val points = data.series.firstOrNull()?.points.orEmpty()
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val barColor = style.seriesColor(0)
        val clampedProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until rowCount) {
            val cy = plot.top + rowStep * (i + 0.5f)
            AxisLabelRenderer.drawYAxisTickLabel(
                canvas = canvas,
                textMeasurer = textMeasurer,
                text = labels[i],
                yPx = cy,
                plot = plot,
                labelGapPx = labelGapPx,
                style = style,
            )

            val datum = points.firstOrNull { it.domainIndex == i } ?: continue

            // Draw qualitative bands largest-to-smallest if monotonically ascending
            val ranges = datum.qualitativeRanges.filter { it.isFinite() && it >= 0.0 }
            val isMonotonic = ranges.zipWithNext().all { (a, b) -> a <= b }
            if (isMonotonic && ranges.isNotEmpty()) {
                val bandTop = cy - bandHeight * 0.5f
                val shades = listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0), Color(0xFFF1F5F9))
                for (rIdx in ranges.indices.reversed()) {
                    val endX = xScale.toPixel(ranges[rIdx]) ?: continue
                    val left = min(zeroX, endX)
                    val width = max(0f, max(zeroX, endX) - left)
                    val shade = shades.getOrElse(rIdx) { Color(0xFFF1F5F9) }
                    canvas.drawRect(
                        color = shade,
                        topLeft = Offset(left, bandTop),
                        size = Size(width, bandHeight),
                    )
                }
            }

            // Actual value bar
            val actualVal = datum.numericValue?.takeIf { it.isFinite() }
            if (actualVal != null) {
                val targetX = xScale.toPixel(actualVal) ?: zeroX
                val animX = zeroX + (targetX - zeroX) * clampedProgress
                val left = min(zeroX, animX)
                val width = max(0f, max(zeroX, animX) - left)
                val barTop = cy - barHeight * 0.5f
                if (isJournalBw) {
                    JournalBwPatternPainter.drawPatternedRectCompose(
                        drawScope = canvas,
                        left = left,
                        top = barTop,
                        width = width,
                        height = barHeight,
                        pattern = JournalBwFillPattern.SOLID_BLACK,
                        strokeWidthPx = with(canvas) { 1.dp.toPx() },
                    )
                } else {
                    canvas.drawRect(
                        color = barColor,
                        topLeft = Offset(left, barTop),
                        size = Size(width, barHeight),
                    )
                }
            }

            // Perpendicular target marker line
            val targetVal = datum.secondaryNumericValue?.takeIf { it.isFinite() }
            if (targetVal != null) {
                val tx = xScale.toPixel(targetVal)
                if (tx != null) {
                    val halfTargetH = bandHeight * 0.42f
                    canvas.drawLine(
                        color = if (isJournalBw) Color.Black else Color(0xFF0F172A),
                        start = Offset(tx, cy - halfTargetH),
                        end = Offset(tx, cy + halfTargetH),
                        strokeWidth = with(canvas) { 2.5.dp.toPx() },
                        cap = StrokeCap.Round,
                    )
                }
            }

            if (interaction.selectedDomainIndex == i) {
                canvas.drawRect(
                    color = style.crosshairColor.copy(alpha = 0.25f),
                    topLeft = Offset(plot.left, cy - bandHeight * 0.55f),
                    size = Size(plot.width, bandHeight * 1.1f),
                    style = Stroke(width = with(canvas) { 1.5.dp.toPx() }),
                )
            }
        }
    }
}
