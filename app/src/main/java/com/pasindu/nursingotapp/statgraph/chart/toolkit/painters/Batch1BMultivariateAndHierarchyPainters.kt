package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders a 2D hexagonal binning density plot ([ChartKind.HEXBIN]).
 * Only populated hex bins are drawn; empty cells remain blank to distinguish
 * absence of observations from zero-valued measurements.
 */
class HexbinPlotPainter(
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
        val cells = data.hexbinCells
        if (cells.isEmpty()) return

        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        val cols = data.hexbinGridCols.coerceAtLeast(4)
        val radiusPx = ((plot.width / cols) * 0.58f).coerceIn(4f, 36f) * progress.coerceIn(0f, 1f)
        val maxCount = cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val baseColor = style.seriesColor(0)
        val borderPx = with(canvas) { 1.dp.toPx() }

        canvas.clipRect(plot.left, plot.top, plot.right, plot.bottom) {
            cells.forEachIndexed { idx, cell ->
                val cx = xScale.toPixel(cell.centerX) ?: return@forEachIndexed
                val cy = yScale.toPixel(cell.centerY) ?: return@forEachIndexed
                val intensity = (cell.count.toFloat() / maxCount.toFloat()).coerceIn(0.15f, 1f)
                val fill = if (isBw) {
                    val shade = 0.85f - 0.75f * intensity
                    Color(shade, shade, shade, 1f)
                } else {
                    baseColor.copy(alpha = 0.22f + 0.78f * intensity)
                }
                val hexPath = buildHexagonPath(cx, cy, radiusPx)
                drawPath(path = hexPath, color = fill)
                drawPath(
                    path = hexPath,
                    color = if (interaction.selectedDomainIndex == idx) style.crosshairColor else style.plotBackground,
                    style = Stroke(width = if (interaction.selectedDomainIndex == idx) borderPx * 2f else borderPx),
                )
            }
        }
    }

    private fun buildHexagonPath(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        for (i in 0 until 6) {
            val angle = (PI / 3.0) * i - PI / 6.0
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
}

/**
 * Renders an m x m Scatterplot Matrix (SPLOM) ([ChartKind.SCATTERPLOT_MATRIX])
 * with variable titles on the diagonal and pairwise scatter subplots off-diagonal.
 */
class ScatterplotMatrixPainter(
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
        val vars = data.splomVariableNames
        val m = vars.size
        if (!rect.isValid || m < 2 || data.splomPanels.isEmpty()) return

        val pad = with(canvas) { 8.dp.toPx() }
        val gap = with(canvas) { 4.dp.toPx() }
        val gridSide = min(rect.width - pad * 2f, rect.height - pad * 2f)
        if (gridSide <= 12f) return

        val gridLeft = rect.left + (rect.width - gridSide) * 0.5f
        val gridTop = rect.top + (rect.height - gridSide) * 0.5f
        val cellSize = ((gridSide - gap * (m - 1)) / m).coerceAtLeast(8f)
        val borderPx = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val dotRadius = with(canvas) { (style.markerRadiusDp * 0.55f).toPx() }.coerceIn(1.5f, 4f)
        val pointColor = style.seriesColor(0)
        val clamped = progress.coerceIn(0f, 1f)

        data.splomPanels.forEachIndexed { panelIdx, panel ->
            val cellLeft = gridLeft + panel.colVarIndex * (cellSize + gap)
            val cellTop = gridTop + panel.rowVarIndex * (cellSize + gap)
            val isDiagonal = panel.rowVarIndex == panel.colVarIndex

            canvas.drawRect(
                color = if (isDiagonal) style.gridLineColor.copy(alpha = 0.18f) else style.plotBackground,
                topLeft = Offset(cellLeft, cellTop),
                size = Size(cellSize, cellSize),
            )
            canvas.drawRect(
                color = if (interaction.selectedDomainIndex == panelIdx) style.crosshairColor else style.gridLineColor,
                topLeft = Offset(cellLeft, cellTop),
                size = Size(cellSize, cellSize),
                style = Stroke(width = if (interaction.selectedDomainIndex == panelIdx) borderPx * 2f else borderPx),
            )

            if (isDiagonal) {
                val label = vars.getOrElse(panel.rowVarIndex) { "" }
                val measured = textMeasurer.measure(label, style.axisLabelStyle)
                canvas.clipRect(cellLeft, cellTop, cellLeft + cellSize, cellTop + cellSize) {
                    AxisLabelRenderer.drawTextLabel(
                        canvas = this,
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = cellLeft + (cellSize - measured.size.width) * 0.5f,
                            y = cellTop + (cellSize - measured.size.height) * 0.5f,
                        ),
                        color = style.axisLabelColor,
                    )
                }
            } else if (panel.points.isNotEmpty()) {
                val innerPad = cellSize * 0.12f
                val innerSpan = (cellSize - innerPad * 2f).coerceAtLeast(2f)
                val xMin = panel.points.minOf { it.first }
                val xMax = panel.points.maxOf { it.first }
                val yMin = panel.points.minOf { it.second }
                val yMax = panel.points.maxOf { it.second }
                val xSpan = (xMax - xMin).let { if (it > 1e-9) it else 1.0 }
                val ySpan = (yMax - yMin).let { if (it > 1e-9) it else 1.0 }

                val visible = max(1, (panel.points.size * clamped).toInt())
                panel.points.take(visible).forEach { (xv, yv) ->
                    val px = cellLeft + innerPad + (((xv - xMin) / xSpan).toFloat() * innerSpan)
                    val py = cellTop + cellSize - innerPad - (((yv - yMin) / ySpan).toFloat() * innerSpan)
                    canvas.drawCircle(
                        color = pointColor.copy(alpha = 0.8f),
                        radius = dotRadius,
                        center = Offset(px, py),
                    )
                }
            }
        }
    }
}

/**
 * Renders a Marimekko / Contingency Mosaic plot ([ChartKind.MOSAIC]) where column widths
 * encode marginal column proportions and vertical tile heights encode conditional row proportions.
 */
class MosaicPlotPainter(
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
        val cols = data.mosaicColumns
        if (!rect.isValid || cols.isEmpty()) return

        val pad = with(canvas) { 10.dp.toPx() }
        val bottomLabelHeight = with(canvas) { 26.dp.toPx() }
        val plotLeft = rect.left + pad
        val plotTop = rect.top + pad
        val plotWidth = (rect.width - pad * 2f).coerceAtLeast(4f)
        val plotHeight = (rect.height - pad * 2f - bottomLabelHeight).coerceAtLeast(4f)
        val colGap = with(canvas) { 2.dp.toPx() }
        val tileGap = with(canvas) { 1.dp.toPx() }
        val usableWidth = (plotWidth - colGap * (cols.size - 1).coerceAtLeast(0)).coerceAtLeast(4f)
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clamped = progress.coerceIn(0f, 1f)
        val strokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }

        var curX = plotLeft
        cols.forEachIndexed { cIdx, col ->
            val colWidth = (usableWidth * col.widthProportion.toFloat()).coerceAtLeast(2f)
            val usableColHeight = (plotHeight - tileGap * (col.tiles.size - 1).coerceAtLeast(0)).coerceAtLeast(2f)
            var curY = plotTop
            col.tiles.forEach { tile ->
                val tileHeight = (usableColHeight * tile.proportionInColumn.toFloat() * clamped).coerceAtLeast(1f)
                val color = style.seriesColor(tile.colorIndex)
                if (isBw) {
                    val bwId = JournalBwPatternPainter.barIdentityForSeries(tile.colorIndex)
                    JournalBwPatternPainter.drawPatternedRectCompose(
                        drawScope = canvas,
                        left = curX,
                        top = curY,
                        width = colWidth,
                        height = tileHeight,
                        pattern = bwId.fillPattern,
                        strokeWidthPx = strokePx,
                        outlineStyle = bwId.outlineStyle,
                    )
                } else {
                    canvas.drawRect(
                        color = color,
                        topLeft = Offset(curX, curY),
                        size = Size(colWidth, tileHeight),
                    )
                }
                curY += tileHeight + tileGap
            }

            if (interaction.selectedDomainIndex == cIdx) {
                canvas.drawRect(
                    color = style.crosshairColor,
                    topLeft = Offset(curX, plotTop),
                    size = Size(colWidth, plotHeight),
                    style = Stroke(width = strokePx * 1.5f),
                )
            }

            val measured = textMeasurer.measure(col.columnLabel, style.axisLabelStyle)
            if (measured.size.width <= colWidth + 16f) {
                AxisLabelRenderer.drawTextLabel(
                    canvas = canvas,
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = curX + (colWidth - measured.size.width) * 0.5f,
                        y = plotTop + plotHeight + with(canvas) { 4.dp.toPx() },
                    ),
                    color = style.axisLabelColor,
                )
            }
            curX += colWidth + colGap
        }
    }
}

/**
 * Renders a hierarchical Sunburst chart ([ChartKind.SUNBURST]) with concentric annular rings
 * whose angular sweeps represent non-negative hierarchical proportions.
 */
class SunburstChartPainter(
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
        val nodes = data.sunburstNodes
        if (!rect.isValid || nodes.isEmpty()) return

        val maxLevel = nodes.maxOfOrNull { it.level }?.coerceAtLeast(1) ?: 1
        val pad = with(canvas) { 10.dp.toPx() }
        val outerRadius = (min(rect.width, rect.height) * 0.5f - pad).coerceAtLeast(12f)
        val innerHoleRadius = outerRadius * 0.24f
        val ringWidth = ((outerRadius - innerHoleRadius) / maxLevel).coerceAtLeast(4f)
        val cx = rect.centerX
        val cy = rect.centerY
        val clamped = progress.coerceIn(0f, 1f)
        val separatorPx = with(canvas) { 1.5.dp.toPx() }
        val isBw = style.palette == StatGraphChartPalette.JOURNAL_BW

        // Compute angular layout by level from parent arcs
        val level1 = nodes.filter { it.level == 1 }
        val total1 = level1.sumOf { it.value }.coerceAtLeast(1e-9)
        val arcById = HashMap<String, Pair<Float, Float>>(nodes.size)
        var angle1 = 0f
        level1.forEach { n ->
            val sweep = ((n.value / total1) * 360.0).toFloat()
            arcById[n.id] = angle1 to sweep
            angle1 += sweep
        }

        for (lvl in 2..maxLevel) {
            val byParent = nodes.filter { it.level == lvl }.groupBy { it.parentId }
            byParent.forEach { (parentId, children) ->
                val parentArc = arcById[parentId] ?: return@forEach
                val childTotal = children.sumOf { it.value }.coerceAtLeast(1e-9)
                var curAngle = parentArc.first
                children.forEach { child ->
                    val sweep = ((child.value / childTotal) * parentArc.second).toFloat()
                    arcById[child.id] = curAngle to sweep
                    curAngle += sweep
                }
            }
        }

        nodes.forEachIndexed { idx, node ->
            val arc = arcById[node.id] ?: return@forEachIndexed
            val rInner = innerHoleRadius + (node.level - 1) * ringWidth
            val rOuter = rInner + ringWidth - separatorPx
            if (rOuter <= rInner) return@forEachIndexed

            val startAngle = -90f + arc.first * clamped
            val sweepAngle = (arc.second * clamped).coerceAtLeast(0.5f)
            val path = buildAnnularSectorPath(cx, cy, rInner, rOuter, startAngle, sweepAngle)

            val fill = if (isBw) {
                val shade = (0.20f + 0.14f * ((node.colorIndex + node.level) % 5)).coerceIn(0.15f, 0.85f)
                Color(shade, shade, shade, 1f)
            } else {
                val base = style.seriesColor(node.colorIndex)
                if (node.level > 1) base.copy(alpha = 0.72f) else base
            }

            canvas.drawPath(path = path, color = fill)
            canvas.drawPath(
                path = path,
                color = if (interaction.selectedDomainIndex == idx) style.crosshairColor else style.plotBackground,
                style = Stroke(width = if (interaction.selectedDomainIndex == idx) separatorPx * 2f else separatorPx),
            )
        }

        val centerText = data.series.firstOrNull()?.name?.take(14).orEmpty()
        if (centerText.isNotEmpty()) {
            val measured = textMeasurer.measure(centerText, style.axisLabelStyle)
            if (measured.size.width <= innerHoleRadius * 1.8f) {
                AxisLabelRenderer.drawTextLabel(
                    canvas = canvas,
                    textLayoutResult = measured,
                    topLeft = Offset(cx - measured.size.width * 0.5f, cy - measured.size.height * 0.5f),
                    color = style.axisLabelColor,
                )
            }
        }
    }

    private fun buildAnnularSectorPath(
        cx: Float,
        cy: Float,
        rInner: Float,
        rOuter: Float,
        startDeg: Float,
        sweepDeg: Float,
    ): Path {
        val path = Path()
        val outerRect = Rect(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter)
        val innerRect = Rect(cx - rInner, cy - rInner, cx + rInner, cy + rInner)
        if (sweepDeg >= 359.5f) {
            path.addOval(outerRect)
            path.addOval(innerRect)
            path.fillType = PathFillType.EvenOdd
            return path
        }
        val startRad = Math.toRadians(startDeg.toDouble())
        val endRad = Math.toRadians((startDeg + sweepDeg).toDouble())
        path.moveTo(cx + (rOuter * cos(startRad)).toFloat(), cy + (rOuter * sin(startRad)).toFloat())
        path.arcTo(outerRect, startDeg, sweepDeg, false)
        path.lineTo(cx + (rInner * cos(endRad)).toFloat(), cy + (rInner * sin(endRad)).toFloat())
        path.arcTo(innerRect, startDeg + sweepDeg, -sweepDeg, false)
        path.close()
        return path
    }
}
