package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesStrokePattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a right-continuous Empirical Cumulative Distribution Function ([ChartKind.ECDF])
 * with step transitions and optional multi-group overlays.
 */
class EcdfChartPainter(
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

        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadius = with(canvas) { (style.markerRadiusDp * 0.85f).toPx() }
        val clamped = progress.coerceIn(0f, 1f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        // Median 0.5 guide
        val yMedian = yScale.toPixel(0.5)
        if (yMedian != null && yMedian in plot.top..plot.bottom) {
            canvas.drawLine(
                color = style.gridLineColor.copy(alpha = 0.75f),
                start = Offset(plot.left, yMedian),
                end = Offset(plot.right, yMedian),
                strokeWidth = with(canvas) { style.gridStrokeWidthDp.toPx() },
                pathEffect = dotEffect,
            )
        }

        data.series.forEachIndexed { sIdx, series ->
            val color = style.seriesColor(series.colorIndex)
            val pattern = if (isBw || series.dashed) ChartPaletteTokens.strokePatternAt(sIdx) else SeriesStrokePattern.SOLID
            val pathEffect = when (pattern) {
                SeriesStrokePattern.SOLID -> null
                SeriesStrokePattern.DASHED -> dashEffect
                SeriesStrokePattern.DOTTED -> dotEffect
                SeriesStrokePattern.DASH_DOT -> dashDotEffect
            }

            val pts = series.points.mapNotNull { datum ->
                val xVal = datum.secondaryNumericValue ?: return@mapNotNull null
                val yVal = datum.numericValue ?: return@mapNotNull null
                val xPx = xScale.toPixel(xVal) ?: return@mapNotNull null
                val yPx = yScale.toPixel(yVal) ?: return@mapNotNull null
                Triple(datum, xPx, yPx)
            }
            if (pts.isEmpty()) return@forEachIndexed

            val maxCount = max(1, (pts.size * clamped).toInt().coerceAtLeast(1))
            val visiblePts = pts.take(maxCount)
            val path = Path()
            val yZero = (yScale.toPixel(0.0) ?: plot.bottom).coerceIn(plot.top, plot.bottom)
            var prevY = yZero
            visiblePts.forEachIndexed { idx, (_, xPx, yPx) ->
                if (idx == 0) {
                    path.moveTo(xPx, yZero)
                    path.lineTo(xPx, yPx)
                } else {
                    path.lineTo(xPx, prevY)
                    path.lineTo(xPx, yPx)
                }
                prevY = yPx
            }
            if (clamped >= 0.99f && visiblePts.isNotEmpty()) {
                path.lineTo(plot.right, prevY)
            }

            canvas.drawPath(
                path = path,
                color = color,
                style = Stroke(width = lineStroke, cap = StrokeCap.Round, pathEffect = pathEffect),
            )

            visiblePts.forEach { (datum, xPx, yPx) ->
                val isSelected = interaction.selectedDomainIndex == datum.domainIndex
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = yPx,
                    radiusPx = if (isSelected) markerRadius * 1.4f else markerRadius,
                    shape = series.markerShape,
                    fillColor = color,
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
            }
        }
    }
}

/**
 * Renders a Normal Quantile-Quantile (Q-Q) plot ([ChartKind.QQ_PLOT]) with theoretical
 * quantiles on X, sample quantiles on Y, and an optional quartile reference line.
 */
class QqPlotChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val refDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

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

        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() }
        val clamped = progress.coerceIn(0f, 1f)

        // Optional quartile reference line (series index 1)
        val refSeries = data.series.getOrNull(1)
        if (refSeries != null && refSeries.points.size >= 2) {
            val p0 = refSeries.points.first()
            val p1 = refSeries.points.last()
            val x0 = p0.secondaryNumericValue?.let { xScale.toPixel(it) }
            val y0 = p0.numericValue?.let { yScale.toPixel(it) }
            val x1 = p1.secondaryNumericValue?.let { xScale.toPixel(it) }
            val y1 = p1.numericValue?.let { yScale.toPixel(it) }
            if (x0 != null && y0 != null && x1 != null && y1 != null) {
                canvas.drawLine(
                    color = style.seriesColor(refSeries.colorIndex).copy(alpha = 0.85f),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = lineStroke * 0.9f,
                    pathEffect = refDash,
                )
            }
        }

        // Sample vs theoretical quantile points (series index 0)
        val pointSeries = data.series.firstOrNull() ?: return
        val pointColor = style.seriesColor(pointSeries.colorIndex)
        val visibleCount = max(1, (pointSeries.points.size * clamped).toInt())
        pointSeries.points.take(visibleCount).forEach { datum ->
            val tx = datum.secondaryNumericValue ?: return@forEach
            val sy = datum.numericValue ?: return@forEach
            val xPx = xScale.toPixel(tx) ?: return@forEach
            val yPx = yScale.toPixel(sy) ?: return@forEach
            val isSelected = interaction.selectedDomainIndex == datum.domainIndex
            markersPainter.drawMarker(
                canvas = canvas,
                centerX = xPx,
                centerY = yPx,
                radiusPx = if (isSelected) markerRadius * 1.4f else markerRadius,
                shape = pointSeries.markerShape,
                fillColor = pointColor,
                surfacePunchColor = style.plotBackground,
                isSelected = isSelected,
            )
        }
    }
}

/**
 * Renders a Tukey Stem-and-Leaf display ([ChartKind.STEM_AND_LEAF]) using aligned
 * monospace stems, a vertical stem/leaf divider, sorted leaves, and an explicit precision key.
 */
class StemAndLeafPainter(
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
        val rows = data.stemAndLeafRows
        if (!rect.isValid || rows.isEmpty()) return

        val pad = with(canvas) { 12.dp.toPx() }
        val innerLeft = rect.left + pad
        val innerRight = rect.right - pad
        val innerTop = rect.top + pad
        val innerBottom = rect.bottom - pad
        if (innerRight <= innerLeft || innerBottom <= innerTop) return

        val monoStyle = style.axisLabelStyle.copy(fontFamily = FontFamily.Monospace)
        val stemStyle = monoStyle.copy(fontWeight = FontWeight.SemiBold)
        val keyLayout = textMeasurer.measure(
            text = data.stemLeafKey.ifBlank { "Stem | Leaf" },
            style = monoStyle,
        )
        AxisLabelRenderer.drawTextLabel(
            canvas = canvas,
            textLayoutResult = keyLayout,
            topLeft = Offset(innerLeft, innerTop),
            color = style.subtitleColor,
        )

        val tableTop = innerTop + keyLayout.size.height + with(canvas) { 8.dp.toPx() }
        val availHeight = (innerBottom - tableTop).coerceAtLeast(1f)
        val rowHeight = min(
            with(canvas) { 26.dp.toPx() },
            availHeight / rows.size.coerceAtLeast(1),
        )

        val maxStemWidth = rows.maxOfOrNull {
            textMeasurer.measure(it.stemLabel, stemStyle).size.width.toFloat()
        } ?: with(canvas) { 24.dp.toPx() }
        val stemColWidth = max(maxStemWidth + with(canvas) { 10.dp.toPx() }, with(canvas) { 36.dp.toPx() })
        val dividerX = innerLeft + stemColWidth
        val leafStartX = dividerX + with(canvas) { 10.dp.toPx() }
        val dividerStroke = with(canvas) { style.axisStrokeWidthDp.toPx() }

        val visibleRows = max(1, (rows.size * progress.coerceIn(0f, 1f)).toInt())
        val tableBottom = tableTop + rowHeight * rows.size
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(dividerX, tableTop),
            end = Offset(dividerX, min(innerBottom, tableBottom)),
            strokeWidth = dividerStroke * 1.4f,
        )

        val stemGap = with(canvas) { 8.dp.toPx() }
        val accentColor = style.seriesColor(0)

        rows.take(visibleRows).forEachIndexed { idx, row ->
            val rowTop = tableTop + idx * rowHeight
            if (interaction.selectedDomainIndex == idx) {
                canvas.drawRect(
                    color = accentColor.copy(alpha = 0.14f),
                    topLeft = Offset(innerLeft, rowTop),
                    size = Size(innerRight - innerLeft, rowHeight),
                )
            }

            val stemMeasured = textMeasurer.measure(row.stemLabel, stemStyle)
            val textY = rowTop + (rowHeight - stemMeasured.size.height) * 0.5f
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = stemMeasured,
                topLeft = Offset(dividerX - stemGap - stemMeasured.size.width, textY),
                color = accentColor,
            )

            val leavesText = row.leaves.joinToString(" ")
            val countText = "(${row.leaves.size})"
            val leavesMeasured = textMeasurer.measure(leavesText, monoStyle)
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = leavesMeasured,
                topLeft = Offset(leafStartX, textY),
                color = style.axisLabelColor,
            )

            val countMeasured = textMeasurer.measure(countText, monoStyle)
            val countX = max(
                leafStartX + leavesMeasured.size.width + with(canvas) { 10.dp.toPx() },
                innerRight - countMeasured.size.width,
            )
            if (countX + countMeasured.size.width <= innerRight + 2f) {
                AxisLabelRenderer.drawTextLabel(
                    canvas = canvas,
                    textLayoutResult = countMeasured,
                    topLeft = Offset(countX, textY),
                    color = style.subtitleColor,
                )
            }
        }
    }
}
