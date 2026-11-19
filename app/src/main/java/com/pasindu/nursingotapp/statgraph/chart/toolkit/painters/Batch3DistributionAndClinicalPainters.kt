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
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Renders an equal-width histogram ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.HISTOGRAM])
 * with zero-anchored count bars and preserved empty internal bins.
 */
class HistogramChartPainter(
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
        val yScale = geometry.yScale
        val baselineY = (yScale.toPixel(0.0) ?: plot.bottom).coerceIn(plot.top, plot.bottom)
        val binCount = data.domainLabels.size.coerceAtLeast(1)
        val slotWidth = plot.width / binCount
        val gapPx = with(canvas) { 1.dp.toPx() }
        val barWidth = (slotWidth - gapPx).coerceAtLeast(with(canvas) { 2.dp.toPx() })
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        val series = data.series.firstOrNull() ?: return
        val color = style.seriesColor(series.colorIndex)

        series.points.forEach { pt ->
            val count = pt.numericValue?.takeIf { it.isFinite() && it > 0.0 } ?: return@forEach
            val left = plot.left + pt.domainIndex * slotWidth + gapPx * 0.5f
            val targetTop = (yScale.toPixel(count) ?: baselineY).coerceIn(plot.top, plot.bottom)
            val animTop = baselineY + (targetTop - baselineY) * clamped
            val height = abs(baselineY - animTop).coerceAtLeast(1f)
            val top = min(animTop, baselineY)

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(series.colorIndex)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = left,
                    top = top,
                    width = barWidth,
                    height = height,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = strokePx,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRect(
                    color = color.copy(alpha = 0.86f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                )
                canvas.drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

/**
 * Renders a Tukey box-and-whisker plot ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.BOX_PLOT])
 * showing Q1, median, Q3, 1.5*IQR whiskers, and individual outlier points.
 */
class BoxPlotChartPainter(
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
        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 0.95f }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        val groups = data.boxPlotGroups
        val slotWidth = plot.width / groups.size.coerceAtLeast(1)
        val boxWidth = min(with(canvas) { 42.dp.toPx() }, slotWidth * 0.56f).coerceAtLeast(with(canvas) { 8.dp.toPx() })
        val capHalfWidth = boxWidth * 0.32f

        groups.forEachIndexed { idx, grp ->
            val centerX = xScale.bandCenter(idx) ?: return@forEachIndexed
            val medY = yScale.toPixel(grp.median) ?: return@forEachIndexed
            val q1TargetY = yScale.toPixel(grp.q1) ?: medY
            val q3TargetY = yScale.toPixel(grp.q3) ?: medY
            val loTargetY = yScale.toPixel(grp.lowerWhisker) ?: q1TargetY
            val hiTargetY = yScale.toPixel(grp.upperWhisker) ?: q3TargetY

            val q1Y = medY + (q1TargetY - medY) * clamped
            val q3Y = medY + (q3TargetY - medY) * clamped
            val loY = medY + (loTargetY - medY) * clamped
            val hiY = medY + (hiTargetY - medY) * clamped

            val color = style.seriesColor(grp.colorIndex)
            val boxTop = min(q1Y, q3Y)
            val boxBottom = max(q1Y, q3Y)
            val boxHeight = max(2f, boxBottom - boxTop)
            val boxLeft = centerX - boxWidth * 0.5f

            // Whisker stems and caps
            canvas.drawLine(color = color, start = Offset(centerX, hiY), end = Offset(centerX, boxTop), strokeWidth = strokePx)
            canvas.drawLine(color = color, start = Offset(centerX, boxBottom), end = Offset(centerX, loY), strokeWidth = strokePx)
            canvas.drawLine(color = color, start = Offset(centerX - capHalfWidth, hiY), end = Offset(centerX + capHalfWidth, hiY), strokeWidth = strokePx)
            canvas.drawLine(color = color, start = Offset(centerX - capHalfWidth, loY), end = Offset(centerX + capHalfWidth, loY), strokeWidth = strokePx)

            // IQR box
            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(grp.colorIndex)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = boxLeft,
                    top = boxTop,
                    width = boxWidth,
                    height = boxHeight,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = strokePx,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRect(
                    color = color.copy(alpha = 0.26f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    style = Fill,
                )
                canvas.drawRect(
                    color = color,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    style = Stroke(width = strokePx),
                )
            }

            // Median line
            canvas.drawLine(
                color = if (isBw) style.titleColor else color,
                start = Offset(boxLeft, medY),
                end = Offset(boxLeft + boxWidth, medY),
                strokeWidth = strokePx * 1.6f,
            )

            // Outliers beyond 1.5 * IQR fences
            grp.outliers.forEach { outVal ->
                val outTargetY = yScale.toPixel(outVal) ?: return@forEach
                val outY = medY + (outTargetY - medY) * clamped
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = outY,
                    radiusPx = markerRadiusPx,
                    shape = SeriesMarkerShape.DIAMOND,
                    fillColor = color,
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == idx,
                )
            }
        }
    }
}

/**
 * Renders a 1D strip plot ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.STRIP_PLOT])
 * with deterministic horizontal jitter to prevent tied points from colliding.
 */
class StripPlotChartPainter(
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
        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val slotWidth = plot.width / data.domainLabels.size.coerceAtLeast(1)
        val maxJitterPx = min(with(canvas) { 14.dp.toPx() }, slotWidth * 0.28f)
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 0.95f }
        val clamped = progress.coerceIn(0f, 1f)

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            series.points.forEach { pt ->
                val v = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val baseX = xScale.bandCenter(pt.domainIndex) ?: return@forEach
                val jitterNorm = (pt.secondaryNumericValue ?: 0.0).toFloat().coerceIn(-1f, 1f)
                val xPx = baseX + jitterNorm * maxJitterPx
                val targetY = yScale.toPixel(v) ?: return@forEach
                val yPx = plot.bottom + (targetY - plot.bottom) * clamped

                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = yPx,
                    radiusPx = markerRadiusPx,
                    shape = series.markerShape,
                    fillColor = color.copy(alpha = 0.85f),
                    surfacePunchColor = style.plotBackground,
                    isSelected = interaction.selectedDomainIndex == pt.domainIndex,
                )
            }
        }
    }
}

/**
 * Renders a vertical error-bar chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.ERROR_BAR])
 * with central estimate markers and validated lower/upper interval whiskers.
 */
class ErrorBarChartPainter(
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
        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val capHalfPx = with(canvas) { 5.dp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 1.1f }
        val clamped = progress.coerceIn(0f, 1f)

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            series.points.forEach { pt ->
                val est = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
                val lo = pt.lowerLimit?.takeIf { it.isFinite() } ?: return@forEach
                val hi = pt.upperLimit?.takeIf { it.isFinite() } ?: return@forEach
                if (lo > hi || est < lo || est > hi) return@forEach

                val xPx = xScale.bandCenter(pt.domainIndex) ?: return@forEach
                val estY = yScale.toPixel(est) ?: return@forEach
                val loTargetY = yScale.toPixel(lo) ?: estY
                val hiTargetY = yScale.toPixel(hi) ?: estY
                val loY = estY + (loTargetY - estY) * clamped
                val hiY = estY + (hiTargetY - estY) * clamped

                canvas.drawLine(color = color, start = Offset(xPx, hiY), end = Offset(xPx, loY), strokeWidth = strokePx)
                canvas.drawLine(color = color, start = Offset(xPx - capHalfPx, hiY), end = Offset(xPx + capHalfPx, hiY), strokeWidth = strokePx)
                canvas.drawLine(color = color, start = Offset(xPx - capHalfPx, loY), end = Offset(xPx + capHalfPx, loY), strokeWidth = strokePx)

                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = estY,
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
 * Renders a horizontal range plot ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.RANGE_PLOT])
 * showing lower-to-upper interval bars and central estimate markers across categories.
 */
class RangePlotPainter(
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
        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val barHeight = min(with(canvas) { 8.dp.toPx() }, rowHeight * 0.36f).coerceAtLeast(with(canvas) { 3.dp.toPx() })
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 1.05f }
        val cornerPx = barHeight * 0.5f
        val clamped = progress.coerceIn(0f, 1f)

        val series = data.series.firstOrNull() ?: return
        val color = style.seriesColor(series.colorIndex)

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

            val pt = series.points.firstOrNull { it.domainIndex == dIdx } ?: return@forEachIndexed
            val est = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val lo = pt.lowerLimit?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val hi = pt.upperLimit?.takeIf { it.isFinite() } ?: return@forEachIndexed
            if (lo > hi) return@forEachIndexed

            val estX = xScale.toPixel(est) ?: return@forEachIndexed
            val loTargetX = xScale.toPixel(lo) ?: estX
            val hiTargetX = xScale.toPixel(hi) ?: estX
            val loX = estX + (loTargetX - estX) * clamped
            val hiX = estX + (hiTargetX - estX) * clamped

            val left = min(loX, hiX)
            val width = max(2f, abs(hiX - loX))
            canvas.drawRoundRect(
                color = color.copy(alpha = 0.35f),
                topLeft = Offset(left, centerY - barHeight * 0.5f),
                size = Size(width, barHeight),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
            )
            markersPainter.drawMarker(
                canvas = canvas,
                centerX = estX,
                centerY = centerY,
                radiusPx = markerRadiusPx,
                shape = series.markerShape,
                fillColor = color,
                surfacePunchColor = style.plotBackground,
                isSelected = interaction.selectedDomainIndex == dIdx,
            )
        }
    }
}

/**
 * Renders a clinical forest plot ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.FOREST_PLOT])
 * with study/subgroup rows, horizontal 95% confidence intervals, square point-estimate markers,
 * and a vertical null-effect reference line at [ChartRenderData.midpointValue].
 */
class ForestPlotPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val refDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

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
        val refX = xScale.toPixel(data.midpointValue)
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val capHalfPx = with(canvas) { 4.dp.toPx() }
        val labelGapPx = with(canvas) { 6.dp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() * 1.15f }
        val clamped = progress.coerceIn(0f, 1f)

        if (refX != null && refX in plot.left..plot.right) {
            canvas.drawLine(
                color = style.zeroAxisColor,
                start = Offset(refX, plot.top),
                end = Offset(refX, plot.bottom),
                strokeWidth = strokePx * 1.25f,
                pathEffect = refDash,
            )
        }

        val n = data.domainLabels.size.coerceAtLeast(1)
        val rowHeight = plot.height / n
        val series = data.series.firstOrNull() ?: return
        val color = style.seriesColor(series.colorIndex)

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

            val pt = series.points.firstOrNull { it.domainIndex == dIdx } ?: return@forEachIndexed
            val est = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val lo = pt.lowerLimit?.takeIf { it.isFinite() } ?: return@forEachIndexed
            val hi = pt.upperLimit?.takeIf { it.isFinite() } ?: return@forEachIndexed
            if (lo > hi || est < lo || est > hi) return@forEachIndexed

            val estX = xScale.toPixel(est) ?: return@forEachIndexed
            val loTargetX = xScale.toPixel(lo) ?: estX
            val hiTargetX = xScale.toPixel(hi) ?: estX
            val loX = estX + (loTargetX - estX) * clamped
            val hiX = estX + (hiTargetX - estX) * clamped

            canvas.drawLine(color = color, start = Offset(loX, centerY), end = Offset(hiX, centerY), strokeWidth = strokePx)
            canvas.drawLine(color = color, start = Offset(loX, centerY - capHalfPx), end = Offset(loX, centerY + capHalfPx), strokeWidth = strokePx)
            canvas.drawLine(color = color, start = Offset(hiX, centerY - capHalfPx), end = Offset(hiX, centerY + capHalfPx), strokeWidth = strokePx)

            markersPainter.drawMarker(
                canvas = canvas,
                centerX = estX,
                centerY = centerY,
                radiusPx = markerRadiusPx,
                shape = SeriesMarkerShape.SQUARE,
                fillColor = color,
                surfacePunchColor = style.plotBackground,
                isSelected = interaction.selectedDomainIndex == dIdx,
            )
        }
    }
}

/**
 * Renders a Shewhart Individuals control chart ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.CONTROL_CHART])
 * with center line (CL), 3-sigma upper/lower control limits (UCL/LCL), and highlighted out-of-control points.
 */
class ControlChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter,
) : InteractiveChartPainter {

    private val limitDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    private val linePath = Path()

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

        val series = data.series.firstOrNull() ?: return
        val firstValid = series.points.firstOrNull { it.secondaryNumericValue != null }
        val cl = firstValid?.secondaryNumericValue
        val lcl = firstValid?.lowerLimit
        val ucl = firstValid?.upperLimit

        // Draw CL, UCL, LCL reference lines
        cl?.let { yScale.toPixel(it) }?.takeIf { it in plot.top..plot.bottom }?.let { clY ->
            canvas.drawLine(
                color = style.zeroAxisColor,
                start = Offset(plot.left, clY),
                end = Offset(plot.right, clY),
                strokeWidth = strokePx * 1.15f,
            )
        }
        ucl?.let { yScale.toPixel(it) }?.takeIf { it in plot.top..plot.bottom }?.let { uclY ->
            canvas.drawLine(
                color = style.errorColor.copy(alpha = 0.75f),
                start = Offset(plot.left, uclY),
                end = Offset(plot.right, uclY),
                strokeWidth = strokePx,
                pathEffect = limitDash,
            )
        }
        lcl?.let { yScale.toPixel(it) }?.takeIf { it in plot.top..plot.bottom }?.let { lclY ->
            canvas.drawLine(
                color = style.errorColor.copy(alpha = 0.75f),
                start = Offset(plot.left, lclY),
                end = Offset(plot.right, lclY),
                strokeWidth = strokePx,
                pathEffect = limitDash,
            )
        }

        val color = style.seriesColor(series.colorIndex)
        linePath.reset()
        var started = false

        series.points.sortedBy { it.domainIndex }.forEach { pt ->
            val v = pt.numericValue?.takeIf { it.isFinite() }
            val xPx = xScale.pointX(pt.domainIndex)
            val targetY = v?.let { yScale.toPixel(it) }
            if (xPx != null && targetY != null) {
                val yPx = plot.bottom + (targetY - plot.bottom) * clamped
                if (!started) {
                    linePath.moveTo(xPx, yPx)
                    started = true
                } else {
                    linePath.lineTo(xPx, yPx)
                }
            } else {
                started = false
            }
        }

        canvas.drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        series.points.forEach { pt ->
            val v = pt.numericValue?.takeIf { it.isFinite() } ?: return@forEach
            val xPx = xScale.pointX(pt.domainIndex) ?: return@forEach
            val targetY = yScale.toPixel(v) ?: return@forEach
            val yPx = plot.bottom + (targetY - plot.bottom) * clamped
            val isOutOfControl = pt.isConflictFlagged

            if (isOutOfControl) {
                canvas.drawCircle(
                    color = style.errorColor,
                    radius = markerRadiusPx * 1.9f,
                    center = Offset(xPx, yPx),
                    style = Stroke(width = strokePx * 1.1f),
                )
            }
            markersPainter.drawMarker(
                canvas = canvas,
                centerX = xPx,
                centerY = yPx,
                radiusPx = markerRadiusPx,
                shape = if (isOutOfControl) SeriesMarkerShape.DIAMOND else series.markerShape,
                fillColor = if (isOutOfControl) style.errorColor else color,
                surfacePunchColor = style.plotBackground,
                isSelected = interaction.selectedDomainIndex == pt.domainIndex,
            )
        }
    }
}

/**
 * Renders a Kaplan-Meier survival step curve ([com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind.KAPLAN_MEIER])
 * from (0, 1.0) with right-continuous event drops and vertical tick marks at censored times.
 */
class KaplanMeierChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
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
        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val censorTickHalfPx = with(canvas) { 4.5.dp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)

        data.series.forEach { series ->
            val color = style.seriesColor(series.colorIndex)
            stepPath.reset()
            var prevPt: Pair<Float, Float>? = null

            val sortedPts = series.points
                .filter { it.numericValue != null && it.secondaryNumericValue != null }
                .sortedBy { it.secondaryNumericValue }

            sortedPts.forEach { pt ->
                val time = pt.secondaryNumericValue ?: return@forEach
                val surv = pt.numericValue ?: return@forEach
                val xPx = xScale.toPixel(time) ?: return@forEach
                val targetY = yScale.toPixel(surv) ?: return@forEach
                val yPx = plot.top + (targetY - plot.top) * clamped

                val prev = prevPt
                if (prev == null) {
                    stepPath.moveTo(xPx, yPx)
                } else {
                    // Right-continuous survival step: horizontal at previous survival, then vertical drop at event time
                    stepPath.lineTo(xPx, prev.second)
                    stepPath.lineTo(xPx, yPx)
                }
                prevPt = xPx to yPx
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

            // Draw censoring tick marks (`+` / vertical ticks) where censored > 0
            sortedPts.forEach { pt ->
                val censoredCount = pt.qualitativeRanges.getOrNull(1) ?: 0.0
                if (censoredCount > 0.0) {
                    val time = pt.secondaryNumericValue ?: return@forEach
                    val surv = pt.numericValue ?: return@forEach
                    val xPx = xScale.toPixel(time) ?: return@forEach
                    val targetY = yScale.toPixel(surv) ?: return@forEach
                    val yPx = plot.top + (targetY - plot.top) * clamped

                    canvas.drawLine(
                        color = color,
                        start = Offset(xPx, yPx - censorTickHalfPx),
                        end = Offset(xPx, yPx + censorTickHalfPx),
                        strokeWidth = strokePx * 1.15f,
                    )
                    canvas.drawLine(
                        color = color,
                        start = Offset(xPx - censorTickHalfPx * 0.7f, yPx),
                        end = Offset(xPx + censorTickHalfPx * 0.7f, yPx),
                        strokeWidth = strokePx * 1.15f,
                    )
                }
            }
        }
    }
}
