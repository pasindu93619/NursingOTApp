package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TreemapTileData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 1. Violin plot painter: mirrored Gaussian KDE envelope (only when n >= 10 and non-constant)
 *    with central IQR box, median bar, min-max whiskers, and sample-size label.
 */
class ViolinPlotChartPainter(
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
        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        if (!plot.isValid || data.violinGroups.isEmpty()) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)

        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clamped = progress.coerceIn(0f, 1f)
        val halfMaxWidth = (xScale.bandWidth * 0.42f).coerceAtLeast(8f) * clamped
        val boxHalfWidth = (halfMaxWidth * 0.24f).coerceIn(3.5f, 10f)
        val strokePx = with(canvas) { 2.dp.toPx() }
        val medianStrokePx = with(canvas) { 3.dp.toPx() }

        data.violinGroups.forEachIndexed { idx, group ->
            val cx = xScale.bandCenter(idx) ?: return@forEachIndexed
            val color = style.seriesColor(group.colorIndex)
            val isSelected = interaction.selectedDomainIndex == idx

            if (group.kdePoints.size >= 2) {
                val maxDensity = group.kdePoints.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
                val leftOffsets = mutableListOf<Offset>()
                val rightOffsets = mutableListOf<Offset>()
                for ((xVal, density) in group.kdePoints) {
                    val yPx = yScale.toPixel(xVal)?.coerceIn(plot.top, plot.bottom) ?: continue
                    val wPx = ((density / maxDensity) * halfMaxWidth).toFloat()
                    rightOffsets.add(Offset(cx + wPx, yPx))
                    leftOffsets.add(Offset(cx - wPx, yPx))
                }
                if (rightOffsets.size >= 2) {
                    val path = Path().apply {
                        moveTo(rightOffsets.first().x, rightOffsets.first().y)
                        for (i in 1 until rightOffsets.size) {
                            lineTo(rightOffsets[i].x, rightOffsets[i].y)
                        }
                        for (i in leftOffsets.indices.reversed()) {
                            lineTo(leftOffsets[i].x, leftOffsets[i].y)
                        }
                        close()
                    }
                    canvas.drawPath(
                        path = path,
                        color = color.copy(alpha = if (isBw) 0.16f else if (isSelected) 0.42f else 0.28f),
                        style = Fill,
                    )
                    canvas.drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = if (isSelected) strokePx * 1.4f else strokePx, join = StrokeJoin.Round),
                    )
                }
            }

            val yMin = yScale.toPixel(group.min)?.coerceIn(plot.top, plot.bottom)
            val yMax = yScale.toPixel(group.max)?.coerceIn(plot.top, plot.bottom)
            val yQ1 = yScale.toPixel(group.q1)?.coerceIn(plot.top, plot.bottom)
            val yQ3 = yScale.toPixel(group.q3)?.coerceIn(plot.top, plot.bottom)
            val yMed = yScale.toPixel(group.median)?.coerceIn(plot.top, plot.bottom)

            if (yMin != null && yMax != null) {
                canvas.drawLine(
                    color = color,
                    start = Offset(cx, yMin),
                    end = Offset(cx, yMax),
                    strokeWidth = strokePx * 0.85f,
                    cap = StrokeCap.Round,
                )
            }
            if (yQ1 != null && yQ3 != null) {
                val topY = min(yQ1, yQ3)
                val height = max(2f, abs(yQ1 - yQ3))
                canvas.drawRect(
                    color = if (isBw) style.plotBackground else color.copy(alpha = 0.85f),
                    topLeft = Offset(cx - boxHalfWidth, topY),
                    size = Size(boxHalfWidth * 2f, height),
                    style = Fill,
                )
                canvas.drawRect(
                    color = color,
                    topLeft = Offset(cx - boxHalfWidth, topY),
                    size = Size(boxHalfWidth * 2f, height),
                    style = Stroke(width = strokePx * 0.9f),
                )
            }
            if (yMed != null) {
                canvas.drawLine(
                    color = if (isBw) color else Color.White,
                    start = Offset(cx - boxHalfWidth - 2f, yMed),
                    end = Offset(cx + boxHalfWidth + 2f, yMed),
                    strokeWidth = medianStrokePx,
                    cap = StrokeCap.Round,
                )
            }

            val nTag = "n=${group.count}"
            val layout = axisAndGridPainter.textMeasurer.measure(AnnotatedString(nTag), style = style.axisLabelStyle, maxLines = 1)
            val labelY = ((yMax ?: plot.top) - layout.size.height - 4f).coerceAtLeast(plot.top + 2f)
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = layout,
                topLeft = Offset((cx - layout.size.width / 2f).coerceIn(plot.left, (plot.right - layout.size.width).coerceAtLeast(plot.left)), labelY),
                color = style.axisLabelColor,
            )
        }
    }
}

/**
 * 2. Density plot painter: smooth Gaussian KDE curves over a numeric X domain and zero-anchored
 *    estimated-density Y axis. Never implies discrete observed frequency bars.
 */
class DensityPlotChartPainter(
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
        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)

        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clamped = progress.coerceIn(0f, 1f)
        val baselineY = yScale.toPixel(0.0)?.coerceIn(plot.top, plot.bottom) ?: plot.bottom
        val strokePx = with(canvas) { 2.4.dp.toPx() }
        val dashPatterns = listOf(
            null,
            PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f),
            PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
            PathEffect.dashPathEffect(floatArrayOf(14f, 5f, 3f, 5f), 0f),
        )

        data.series.forEachIndexed { sIdx, series ->
            val pts = series.points.mapNotNull { p ->
                val x = p.secondaryNumericValue ?: return@mapNotNull null
                val y = p.numericValue ?: return@mapNotNull null
                val px = xScale.toPixel(x)?.coerceIn(plot.left, plot.right) ?: return@mapNotNull null
                val targetPy = yScale.toPixel(y)?.coerceIn(plot.top, plot.bottom) ?: return@mapNotNull null
                val py = baselineY + (targetPy - baselineY) * clamped
                Offset(px, py)
            }
            if (pts.size < 2) return@forEachIndexed

            val color = style.seriesColor(series.colorIndex)
            val linePath = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) {
                    lineTo(pts[i].x, pts[i].y)
                }
            }
            val fillPath = Path().apply {
                moveTo(pts.first().x, baselineY)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(pts.last().x, baselineY)
                close()
            }
            canvas.drawPath(
                path = fillPath,
                color = color.copy(alpha = if (isBw) 0.12f else 0.22f),
                style = Fill,
            )
            canvas.drawPath(
                path = linePath,
                color = color,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (isBw) dashPatterns[sIdx % dashPatterns.size] else null,
                ),
            )
        }
    }
}

/**
 * 3. Treemap painter: area-proportional recursive slice-and-dice partitioning of positive tiles,
 *    grouped by parent hierarchy when present.
 */
class TreemapChartPainter(
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
        val padPx = with(canvas) { 8.dp.toPx() }
        val plot = rect.inset(com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartInsets(padPx, padPx, padPx, padPx))
        val tiles = data.treemapTiles.filter { it.value > 0.0 }
        if (!plot.isValid || tiles.isEmpty()) return

        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val gapPx = with(canvas) { 2.dp.toPx() }
        val borderPx = with(canvas) { 1.25.dp.toPx() }

        val rects = mutableListOf<Pair<TreemapTileData, ChartRect>>()
        val hasGroups = tiles.any { !it.groupLabel.isNullOrBlank() }
        if (hasGroups) {
            val grouped = tiles.groupBy { it.groupLabel ?: "Other" }.entries.toList()
            val groupWeights = grouped.map { (_, groupTiles) -> groupTiles.sumOf { it.value } }
            val groupRects = partitionRectangles(plot, groupWeights, gapPx)
            grouped.forEachIndexed { gIdx, (_, groupTiles) ->
                val gRect = groupRects.getOrNull(gIdx) ?: return@forEachIndexed
                val tileRects = partitionRectangles(gRect, groupTiles.map { it.value }, gapPx * 0.75f)
                groupTiles.forEachIndexed { tIdx, tile ->
                    tileRects.getOrNull(tIdx)?.let { rects.add(tile to it) }
                }
            }
        } else {
            val tileRects = partitionRectangles(plot, tiles.map { it.value }, gapPx)
            tiles.forEachIndexed { idx, tile ->
                tileRects.getOrNull(idx)?.let { rects.add(tile to it) }
            }
        }

        rects.forEachIndexed { idx, (tile, tileRect) ->
            if (!tileRect.isValid) return@forEachIndexed
            val color = style.seriesColor(tile.colorIndex)
            val isSelected = interaction.selectedDomainIndex == idx

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(tile.colorIndex)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = tileRect.left,
                    top = tileRect.top,
                    width = tileRect.width,
                    height = tileRect.height,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = borderPx,
                    cornerRadiusPx = 0f,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRect(
                    color = color.copy(alpha = if (isSelected) 0.95f else 0.82f),
                    topLeft = Offset(tileRect.left, tileRect.top),
                    size = Size(tileRect.width, tileRect.height),
                )
                canvas.drawRect(
                    color = if (isSelected) style.titleColor else style.plotBackground,
                    topLeft = Offset(tileRect.left, tileRect.top),
                    size = Size(tileRect.width, tileRect.height),
                    style = Stroke(width = if (isSelected) borderPx * 1.8f else borderPx),
                )
            }

            if (tileRect.width >= 42f && tileRect.height >= 24f) {
                val pctStr = String.format(Locale.US, "%.0f%%", tile.share * 100.0)
                val labelText = if (tileRect.height >= 40f) "${tile.label}\n$pctStr" else tile.label
                val layout = textMeasurer.measure(
                    text = AnnotatedString(labelText),
                    style = style.axisLabelStyle,
                    maxLines = 2,
                )
                if (layout.size.width <= tileRect.width - 8f && layout.size.height <= tileRect.height - 6f) {
                    val textColor = if (isBw) style.titleColor else Color.White
                    AxisLabelRenderer.drawTextLabel(
                        canvas = canvas,
                        textLayoutResult = layout,
                        topLeft = Offset(tileRect.left + 5f, tileRect.top + 4f),
                        color = textColor,
                    )
                }
            }
        }
    }

    private fun partitionRectangles(
        bounds: ChartRect,
        weights: List<Double>,
        gap: Float,
    ): List<ChartRect> {
        if (weights.isEmpty() || !bounds.isValid) return emptyList()
        val result = MutableList(weights.size) { ChartRect(0f, 0f, 0f, 0f) }
        val indexed = weights.mapIndexed { idx, w -> idx to max(0.0, w) }
            .sortedByDescending { it.second }
        layoutRecursive(bounds, indexed, gap, result)
        return result
    }

    private fun layoutRecursive(
        rect: ChartRect,
        items: List<Pair<Int, Double>>,
        gap: Float,
        out: MutableList<ChartRect>,
    ) {
        if (items.isEmpty() || !rect.isValid) return
        if (items.size == 1) {
            out[items[0].first] = ChartRect(
                left = rect.left + gap * 0.5f,
                top = rect.top + gap * 0.5f,
                right = (rect.right - gap * 0.5f).coerceAtLeast(rect.left + gap * 0.5f + 1f),
                bottom = (rect.bottom - gap * 0.5f).coerceAtLeast(rect.top + gap * 0.5f + 1f),
            )
            return
        }

        val total = items.sumOf { it.second }.coerceAtLeast(1e-9)
        var running = 0.0
        var splitIdx = 1
        while (splitIdx < items.size - 1 && (running + items[splitIdx - 1].second) / total < 0.45) {
            running += items[splitIdx - 1].second
            splitIdx++
        }
        val head = items.subList(0, splitIdx)
        val tail = items.subList(splitIdx, items.size)
        val headFraction = (head.sumOf { it.second } / total).toFloat().coerceIn(0.05f, 0.95f)

        if (rect.width >= rect.height) {
            val splitX = rect.left + rect.width * headFraction
            layoutRecursive(ChartRect(rect.left, rect.top, splitX, rect.bottom), head, gap, out)
            layoutRecursive(ChartRect(splitX, rect.top, rect.right, rect.bottom), tail, gap, out)
        } else {
            val splitY = rect.top + rect.height * headFraction
            layoutRecursive(ChartRect(rect.left, rect.top, rect.right, splitY), head, gap, out)
            layoutRecursive(ChartRect(rect.left, splitY, rect.right, rect.bottom), tail, gap, out)
        }
    }
}

/**
 * 4. Funnel chart painter: renders sequential ordered stages top-to-bottom with centered bars
 *    proportional to each stage value and optional conversion/drop-off labels when monotonic.
 */
class FunnelChartPainter(
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
        val padPx = with(canvas) { 10.dp.toPx() }
        val plot = rect.inset(com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartInsets(padPx, padPx, padPx, padPx))
        val points = data.series.firstOrNull()?.points ?: return
        if (!plot.isValid || points.isEmpty()) return

        val maxVal = points.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() && v >= 0.0 } }
            .maxOrNull()?.takeIf { it > 0.0 } ?: return

        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clamped = progress.coerceIn(0f, 1f)
        val stageCount = points.size
        val slotHeight = plot.height / stageCount
        val barHeight = (slotHeight * 0.68f).coerceAtLeast(10f)
        val centerX = plot.left + plot.width * 0.52f
        val maxBarWidth = (plot.width * 0.56f).coerceAtLeast(32f)
        val strokePx = with(canvas) { 1.5.dp.toPx() }

        points.forEachIndexed { idx, p ->
            val v = p.numericValue?.takeIf { it.isFinite() && it >= 0.0 } ?: return@forEachIndexed
            val topY = plot.top + idx * slotHeight + (slotHeight - barHeight) * 0.5f
            val barW = ((v / maxVal) * maxBarWidth).toFloat().coerceAtLeast(4f) * clamped
            val leftX = centerX - barW * 0.5f
            val barRect = ChartRect(leftX, topY, leftX + barW, topY + barHeight)
            val color = style.seriesColor(idx)
            val isSelected = interaction.selectedDomainIndex == idx

            if (isBw) {
                val bwId = JournalBwPatternPainter.barIdentityForSeries(idx)
                JournalBwPatternPainter.drawPatternedRectCompose(
                    drawScope = canvas,
                    left = barRect.left,
                    top = barRect.top,
                    width = barRect.width,
                    height = barRect.height,
                    pattern = bwId.fillPattern,
                    strokeWidthPx = strokePx,
                    cornerRadiusPx = 0f,
                    outlineStyle = bwId.outlineStyle,
                )
            } else {
                canvas.drawRect(
                    color = color.copy(alpha = if (isSelected) 0.96f else 0.84f),
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                )
                canvas.drawRect(
                    color = if (isSelected) style.titleColor else color,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    style = Stroke(width = if (isSelected) strokePx * 1.6f else strokePx),
                )
            }

            val stageLayout = textMeasurer.measure(
                text = AnnotatedString(p.domainLabel),
                style = style.axisLabelStyle,
                maxLines = 1,
            )
            val stageX = (leftX - stageLayout.size.width - 6f).coerceAtLeast(plot.left)
            val stageY = topY + (barHeight - stageLayout.size.height) * 0.5f
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = stageLayout,
                topLeft = Offset(stageX, stageY),
                color = style.axisLabelColor,
            )

            val valText = p.formattedValue.ifBlank { v.toString() }
            val valLayout = textMeasurer.measure(
                text = AnnotatedString(valText),
                style = style.axisLabelStyle,
                maxLines = 1,
            )
            val valX = (barRect.right + 6f).coerceAtMost((plot.right - valLayout.size.width).coerceAtLeast(plot.left))
            val valY = topY + (barHeight - valLayout.size.height) * 0.5f
            AxisLabelRenderer.drawTextLabel(
                canvas = canvas,
                textLayoutResult = valLayout,
                topLeft = Offset(valX, valY),
                color = style.titleColor,
            )
        }
    }
}
