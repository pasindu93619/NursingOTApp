package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisTickCalculator
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartBarColorMode
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartColorPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a zero-anchored horizontal bar chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.HORIZONTAL_BAR]).
 * Missing values are omitted cleanly rather than coerced to zero.
 */
class HorizontalBarChartPainter(
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
        val zeroPx = (xScale.toPixel(0.0) ?: plot.left).coerceIn(plot.left, plot.right)

        val n = data.domainLabels.size.coerceAtLeast(1)
        val seriesCount = data.series.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val groupBandHeight = (rowHeight * 0.68f).coerceAtLeast(with(canvas) { 4.dp.toPx() })
        val singleBarHeight = (groupBandHeight / seriesCount).coerceAtLeast(with(canvas) { 3.dp.toPx() })
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val cornerPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val useDistinctColors = seriesCount == 1 && data.preferredBarColorMode == ChartBarColorMode.DISTINCT_CATEGORY
        val showDirectValueLabels = seriesCount == 1 && rowHeight >= with(canvas) { 14.dp.toPx() } && clamped >= 0.95f

        // Vertical zero baseline
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(zeroPx, plot.top),
            end = Offset(zeroPx, plot.bottom),
            strokeWidth = strokePx * 1.3f,
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

            val groupTop = centerY - groupBandHeight * 0.5f
            val alpha = if (interaction.selectedDomainIndex == null || interaction.selectedDomainIndex == dIdx) {
                1f
            } else {
                0.45f
            }
            data.series.forEachIndexed { sIdx, series ->
                val datum = series.points.firstOrNull { it.domainIndex == dIdx }
                val value = datum?.numericValue?.takeIf { it.isFinite() } ?: return@forEachIndexed
                val targetPx = xScale.toPixel(value) ?: return@forEachIndexed
                val endPx = zeroPx + (targetPx - zeroPx) * clamped
                val left = min(zeroPx, endPx)
                val width = abs(endPx - zeroPx).coerceAtLeast(1f)
                val top = groupTop + sIdx * singleBarHeight
                val color = if (useDistinctColors) {
                    ChartColorPlanner.colorForCategory(dIdx, label, style, true, series.colorIndex)
                } else {
                    style.seriesColor(series.colorIndex)
                }

                if (isBw) {
                    val bwId = JournalBwPatternPainter.barIdentityForSeries(if (useDistinctColors) dIdx else series.colorIndex)
                    JournalBwPatternPainter.drawPatternedRectCompose(
                        drawScope = canvas,
                        left = left,
                        top = top,
                        width = width,
                        height = singleBarHeight,
                        pattern = bwId.fillPattern,
                        strokeWidthPx = strokePx,
                        cornerRadiusPx = cornerPx,
                        outlineStyle = bwId.outlineStyle,
                    )
                } else {
                    canvas.drawRoundRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(left, top),
                        size = Size(width, singleBarHeight),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                }

                if (showDirectValueLabels) {
                    val valStr = AxisTickCalculator.formatTick(value, 0.1)
                    val valLayout = axisAndGridPainter.textMeasurer.measure(
                        text = AnnotatedString(valStr),
                        style = style.axisLabelStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                    val outsideX = left + width + with(canvas) { 4.dp.toPx() }
                    val valY = centerY - valLayout.size.height * 0.5f
                    if (outsideX + valLayout.size.width <= geometry.outerRect.right - 2f) {
                        AxisLabelRenderer.drawTextLabel(
                            canvas = canvas,
                            textLayoutResult = valLayout,
                            topLeft = Offset(outsideX, valY),
                            color = style.axisLabelColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a zero-anchored horizontal lollipop chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.LOLLIPOP]).
 */
class LollipopChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
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
        val zeroPx = (xScale.toPixel(0.0) ?: plot.left).coerceIn(plot.left, plot.right)

        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val stemStrokePx = with(canvas) { 2.2.dp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 1.15f }
        val clamped = progress.coerceIn(0f, 1f)

        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(zeroPx, plot.top),
            end = Offset(zeroPx, plot.bottom),
            strokeWidth = with(canvas) { style.axisStrokeWidthDp.toPx() } * 1.25f,
        )

        val primarySeries = data.series.firstOrNull()
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

            val datum = primarySeries?.points?.firstOrNull { it.domainIndex == dIdx }
            val value = datum?.numericValue?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val targetPx = xScale.toPixel(value) ?: return@forEachIndexed
            val headX = zeroPx + (targetPx - zeroPx) * clamped
            val color = style.seriesColor(primarySeries.colorIndex)

            canvas.drawLine(
                color = color,
                start = Offset(zeroPx, centerY),
                end = Offset(headX, centerY),
                strokeWidth = stemStrokePx,
                cap = StrokeCap.Round,
            )
            markersPainter.drawMarker(
                canvas = canvas,
                centerX = headX,
                centerY = centerY,
                radiusPx = markerRadiusPx,
                shape = primarySeries.markerShape,
                fillColor = color,
                surfacePunchColor = style.plotBackground,
                isSelected = interaction.selectedDomainIndex == dIdx,
            )
        }
    }
}

/**
 * Renders a Cleveland dot plot ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.DOT_PLOT])
 * with horizontal guide tracks and distinct series markers.
 */
class DotPlotPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val trackDash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

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

        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val gridStrokePx = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 1.1f }
        val clamped = progress.coerceIn(0f, 1f)

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

            canvas.drawLine(
                color = style.gridLineColor,
                start = Offset(plot.left, centerY),
                end = Offset(plot.right, centerY),
                strokeWidth = gridStrokePx,
                pathEffect = trackDash,
            )

            data.series.forEach { series ->
                val datum = series.points.firstOrNull { it.domainIndex == dIdx }
                val value = datum?.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val targetX = xScale.toPixel(value) ?: return@forEach
                val xPx = plot.left + (targetX - plot.left) * clamped
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = centerY,
                    radiusPx = markerRadiusPx,
                    shape = series.markerShape,
                    fillColor = style.seriesColor(series.colorIndex),
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == dIdx,
                )
            }
        }
    }
}

/**
 * Renders a zero-anchored area chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.AREA]).
 * Splits filled polygons at missing (`null`) observations so missing periods remain visible gaps.
 */
class AreaChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val fillPath = Path()
    private val linePath = Path()
    private val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

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
        val baselineY = (yScale.toPixel(0.0) ?: plot.bottom).coerceIn(plot.top, plot.bottom)
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            val fillAlpha = if (style.palette == StatGraphChartPalette.JOURNAL_BW) 0.16f else 0.22f

            // Split points into contiguous non-null segments so missing values break the area cleanly
            val segments = mutableListOf<List<Pair<Float, Float>>>()
            var currentSegment = mutableListOf<Pair<Float, Float>>()

            series.points.sortedBy { it.domainIndex }.forEach { pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() }
                val xPx = xScale.pointX(pt.domainIndex)
                val targetY = v?.let { yScale.toPixel(it) }
                if (xPx != null && targetY != null) {
                    val yPx = baselineY + (targetY - baselineY) * clamped
                    currentSegment.add(xPx to yPx)
                } else if (currentSegment.isNotEmpty()) {
                    segments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            }
            if (currentSegment.isNotEmpty()) {
                segments.add(currentSegment)
            }

            segments.forEach { seg ->
                if (seg.size >= 2) {
                    fillPath.reset()
                    linePath.reset()
                    fillPath.moveTo(seg.first().first, baselineY)
                    seg.forEachIndexed { idx, (x, y) ->
                        fillPath.lineTo(x, y)
                        if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    }
                    fillPath.lineTo(seg.last().first, baselineY)
                    fillPath.close()

                    canvas.drawPath(path = fillPath, color = color.copy(alpha = fillAlpha), style = Fill)
                    canvas.drawPath(
                        path = linePath,
                        color = color,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (series.dashed) dashEffect else null,
                        ),
                    )
                }
            }

            series.points.forEach { pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val xPx = xScale.pointX(pt.domainIndex) ?: return@forEach
                val targetY = yScale.toPixel(v) ?: return@forEach
                val yPx = baselineY + (targetY - baselineY) * clamped
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = yPx,
                    radiusPx = markerRadiusPx,
                    shape = series.markerShape,
                    fillColor = color,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == pt.domainIndex,
                )
            }
        }
    }
}

/**
 * Renders a cumulative non-negative stacked area chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.STACKED_AREA]).
 * Breaks stacked ribbons cleanly across periods where values are missing.
 */
class StackedAreaChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val bandPath = Path()
    private val topEdgePath = Path()

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
        val baselineY = (yScale.toPixel(0.0) ?: plot.bottom).coerceIn(plot.top, plot.bottom)
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 0.9f }
        val clamped = progress.coerceIn(0f, 1f)

        val domainCount = data.domainLabels.size
        if (domainCount == 0 || data.series.isEmpty()) return

        // Identify valid domain indices (at least one finite non-negative value across series)
        val runningLowerByDomain = DoubleArray(domainCount) { 0.0 }
        val validDomainMask = BooleanArray(domainCount) { dIdx ->
            data.series.any { s ->
                val v = s.points.firstOrNull { it.domainIndex == dIdx }?.numericValue
                v != null && v.isFinite() && v >= 0.0
            }
        }

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            val fillAlpha = if (style.palette == StatGraphChartPalette.JOURNAL_BW) {
                (0.18f + (series.colorIndex % 4) * 0.14f).coerceAtMost(0.68f)
            } else {
                0.34f
            }

            // Build contiguous domain segments
            data class StackedSlice(val dIdx: Int, val xPx: Float, val lowY: Float, val highY: Float, val hasOwnValue: Boolean)
            val segments = mutableListOf<List<StackedSlice>>()
            var currentSeg = mutableListOf<StackedSlice>()

            for (dIdx in 0 until domainCount) {
                val xPx = xScale.pointX(dIdx)
                if (!validDomainMask[dIdx] || xPx == null) {
                    if (currentSeg.isNotEmpty()) {
                        segments.add(currentSeg)
                        currentSeg = mutableListOf()
                    }
                    continue
                }
                val rawVal = series.points.firstOrNull { it.domainIndex == dIdx }?.numericValue?.takeIf { it.isFinite() && it >= 0.0 }
                val delta = rawVal ?: 0.0
                val lowVal = runningLowerByDomain[dIdx]
                val highVal = lowVal + delta
                runningLowerByDomain[dIdx] = highVal

                val lowTargetY = yScale.toPixel(lowVal) ?: baselineY
                val highTargetY = yScale.toPixel(highVal) ?: baselineY
                val lowY = baselineY + (lowTargetY - baselineY) * clamped
                val highY = baselineY + (highTargetY - baselineY) * clamped
                currentSeg.add(StackedSlice(dIdx, xPx, lowY, highY, rawVal != null))
            }
            if (currentSeg.isNotEmpty()) {
                segments.add(currentSeg)
            }

            segments.forEach { seg ->
                if (seg.size >= 2) {
                    bandPath.reset()
                    topEdgePath.reset()
                    seg.forEachIndexed { idx, s ->
                        if (idx == 0) {
                            bandPath.moveTo(s.xPx, s.highY)
                            topEdgePath.moveTo(s.xPx, s.highY)
                        } else {
                            bandPath.lineTo(s.xPx, s.highY)
                            topEdgePath.lineTo(s.xPx, s.highY)
                        }
                    }
                    for (idx in seg.indices.reversed()) {
                        val s = seg[idx]
                        bandPath.lineTo(s.xPx, s.lowY)
                    }
                    bandPath.close()

                    canvas.drawPath(path = bandPath, color = color.copy(alpha = fillAlpha), style = Fill)
                    canvas.drawPath(
                        path = topEdgePath,
                        color = color,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                seg.forEach { s ->
                    if (s.hasOwnValue) {
                        markersPainter.drawMarker(
                            canvas = canvas,
                            centerX = s.xPx,
                            centerY = s.highY,
                            radiusPx = markerRadiusPx,
                            shape = series.markerShape,
                            fillColor = color,
                            surfacePunchColor = style.plotBackground,
                            isSelected = interaction.selectedDomainIndex == s.dIdx,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a right-continuous step-after staircase chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.STEP]).
 * Missing values break the step path rather than bridging across missing intervals.
 */
class StepChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val stepPath = Path()
    private val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

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
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            stepPath.reset()
            var prevPoint: Pair<Float, Float>? = null

            series.points.sortedBy { it.domainIndex }.forEach { pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() }
                val xPx = xScale.pointX(pt.domainIndex)
                val targetY = v?.let { yScale.toPixel(it) }
                if (xPx != null && targetY != null) {
                    val yPx = plot.bottom + (targetY - plot.bottom) * clamped
                    val prev = prevPoint
                    if (prev == null) {
                        stepPath.moveTo(xPx, yPx)
                    } else {
                        // Step-after: hold previous level horizontally to xPx, then step vertically to yPx
                        stepPath.lineTo(xPx, prev.second)
                        stepPath.lineTo(xPx, yPx)
                    }
                    prevPoint = xPx to yPx
                } else {
                    prevPoint = null
                }
            }

            canvas.drawPath(
                path = stepPath,
                color = color,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Miter,
                    pathEffect = if (series.dashed) dashEffect else null,
                ),
            )

            series.points.forEach { pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val xPx = xScale.pointX(pt.domainIndex) ?: return@forEach
                val targetY = yScale.toPixel(v) ?: return@forEach
                val yPx = plot.bottom + (targetY - plot.bottom) * clamped
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = yPx,
                    radiusPx = markerRadiusPx,
                    shape = series.markerShape,
                    fillColor = color,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == pt.domainIndex,
                )
            }
        }
    }
}
