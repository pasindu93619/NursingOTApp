package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisTickCalculator
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwFillPattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.NumericScale
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisScaleConfig
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesStrokePattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders compact small-multiple sparklines ([ChartKind.SPARKLINE_MULTIPLES]).
 *
 * Supports shared-scale and independent-scale modes, clearly labelling scale mode
 * and preserving missing gaps and zero values.
 */
class SparklineMultiplesPainter(
    private val textMeasurer: TextMeasurer,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val sparkPath = Path()
    private val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
    private val dotEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f), 0f)
    private val dashDotEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f, 4f, 5f), 0f)

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        if (!rect.isValid || data.series.isEmpty()) return

        val pad = with(canvas) { 10.dp.toPx() }
        val inner = ChartRect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        if (!inner.isValid) return

        val labelStyle = style.axisLabelStyle
        val scaleBadgeText = if (data.useSharedScale) "Scale: Shared across panels" else "Scale: Independent per panel"
        val badgeLayout = textMeasurer.measure(
            text = AnnotatedString(scaleBadgeText),
            style = labelStyle.copy(color = style.subtitleColor, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        AxisLabelRenderer.drawTextLabel(canvas, badgeLayout, Offset(inner.left, inner.top), style.subtitleColor)

        val panelsTop = inner.top + badgeLayout.size.height + with(canvas) { 8.dp.toPx() }
        val availHeight = (inner.bottom - panelsTop).coerceAtLeast(40f)
        val count = data.series.size
        val rowGap = with(canvas) { 8.dp.toPx() }
        val rowHeight = ((availHeight - rowGap * (count - 1).coerceAtLeast(0)) / count.toFloat()).coerceAtLeast(24f)

        val sharedValues = if (data.useSharedScale) {
            data.series.flatMap { s -> s.points.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() } } }
        } else {
            emptyList()
        }
        val sharedMin = sharedValues.minOrNull() ?: 0.0
        val sharedMax = sharedValues.maxOrNull() ?: 1.0
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val clampedProgress = progress.coerceIn(0f, 1f)
        val strokeW = with(canvas) { 2.dp.toPx() }
        val markerR = with(canvas) { 3.dp.toPx() }

        data.series.forEachIndexed { idx, series ->
            val top = panelsTop + idx * (rowHeight + rowGap)
            val bottom = (top + rowHeight).coerceAtMost(inner.bottom)
            if (bottom - top < 14f) return@forEachIndexed

            val panelLeft = inner.left
            val panelRight = inner.right
            val nameWidth = (inner.width * 0.32f).coerceIn(72f, 150f)
            val sparkLeft = panelLeft + nameWidth + with(canvas) { 8.dp.toPx() }
            val valueColWidth = with(canvas) { 58.dp.toPx() }
            val sparkRight = (panelRight - valueColWidth).coerceAtLeast(sparkLeft + 30f)

            // Subtle panel border
            canvas.drawRoundRect(
                color = style.gridLineColor.copy(alpha = 0.65f),
                topLeft = Offset(panelLeft, top),
                size = Size(panelRight - panelLeft, bottom - top),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1f),
            )

            // Series title
            val titleLayout = textMeasurer.measure(
                text = AnnotatedString(series.name),
                style = labelStyle.copy(color = style.titleColor, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = nameWidth.toInt().coerceAtLeast(20)),
            )
            AxisLabelRenderer.drawTextLabel(
                canvas,
                titleLayout,
                Offset(panelLeft + 6f, top + ((bottom - top) - titleLayout.size.height) * 0.5f),
                style.titleColor,
            )

            val validVals = series.points.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() } }
            val minVal = if (data.useSharedScale) sharedMin else (validVals.minOrNull() ?: 0.0)
            val maxVal = if (data.useSharedScale) sharedMax else (validVals.maxOrNull() ?: 1.0)
            val span = (maxVal - minVal).takeIf { it > 1e-9 } ?: 1.0

            val sparkPadV = with(canvas) { 5.dp.toPx() }
            val sTop = top + sparkPadV
            val sBottom = (bottom - sparkPadV).coerceAtLeast(sTop + 4f)
            val sHeight = sBottom - sTop
            val sWidth = sparkRight - sparkLeft

            // Zero baseline reference if 0 falls inside [minVal, maxVal]
            if (0.0 in minVal..maxVal) {
                val zeroFrac = ((0.0 - minVal) / span).toFloat().coerceIn(0f, 1f)
                val zeroY = sBottom - zeroFrac * sHeight
                canvas.drawLine(
                    color = style.zeroAxisColor.copy(alpha = 0.5f),
                    start = Offset(sparkLeft, zeroY),
                    end = Offset(sparkRight, zeroY),
                    strokeWidth = 1f,
                )
            }

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

            val n = series.points.size.coerceAtLeast(1)
            val revealRight = sparkLeft + sWidth * clampedProgress
            sparkPath.reset()
            var open = false
            var lastValidX: Float? = null
            var lastValidY: Float? = null

            series.points.forEachIndexed { pIdx, pt ->
                val fracX = if (n == 1) 0.5f else pIdx.toFloat() / (n - 1).toFloat()
                val px = sparkLeft + fracX * sWidth
                val v = pt.numericValue?.takeIf { it.isFinite() }
                if (v != null && px <= revealRight + 0.5f) {
                    val fracY = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                    val py = sBottom - fracY * sHeight
                    if (!open) {
                        sparkPath.moveTo(px, py)
                        open = true
                    } else {
                        sparkPath.lineTo(px, py)
                    }
                    lastValidX = px
                    lastValidY = py
                } else {
                    open = false
                }
            }

            canvas.drawPath(
                path = sparkPath,
                color = seriesColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = pathEffect),
            )

            if (lastValidX != null && lastValidY != null) {
                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = lastValidX!!,
                    centerY = lastValidY!!,
                    radiusPx = markerR,
                    shape = series.markerShape,
                    fillColor = seriesColor,
                    surfacePunchColor = style.plotBackground,
                    isSelected = false,
                )
            }

            val latestStr = validVals.lastOrNull()?.let { AxisTickCalculator.formatTick(it, 0.1) } ?: "—"
            val rangeSuffix = if (!data.useSharedScale && validVals.isNotEmpty()) {
                "\n[${AxisTickCalculator.formatTick(minVal, 0.1)}–${AxisTickCalculator.formatTick(maxVal, 0.1)}]"
            } else {
                ""
            }
            val rightLayout = textMeasurer.measure(
                text = AnnotatedString("$latestStr$rangeSuffix"),
                style = labelStyle.copy(color = style.axisLabelColor),
                maxLines = 2,
                constraints = Constraints(maxWidth = valueColWidth.toInt().coerceAtLeast(24)),
            )
            AxisLabelRenderer.drawTextLabel(
                canvas,
                rightLayout,
                Offset(
                    (panelRight - rightLayout.size.width - 6f).coerceAtLeast(sparkRight + 4f),
                    top + ((bottom - top) - rightLayout.size.height) * 0.5f,
                ),
                style.axisLabelColor,
            )
        }
    }
}

/**
 * Renders daily observations mapped to a calendar grid ([ChartKind.CALENDAR_HEATMAP]).
 *
 * Distinctly renders missing dates (neutral empty/outlined) vs measured zero (explicit zero tint),
 * and flags conflicting multi-observation dates.
 */
class CalendarHeatmapPainter(
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
        if (!rect.isValid) return
        val points = data.series.firstOrNull()?.points.orEmpty()
        val parsed = points.mapNotNull { pt ->
            val raw = pt.cycleInstance.ifBlank { pt.domainLabel }
            val date = runCatching { LocalDate.parse(raw.trim()) }.getOrNull() ?: return@mapNotNull null
            date to pt
        }.toMap()

        if (parsed.isEmpty()) return

        val minDate = parsed.keys.minOrNull()!!
        val maxDate = parsed.keys.maxOrNull()!!
        // Align start to Monday of minDate's week
        val startMonday = minDate.with(DayOfWeek.MONDAY).let { if (it.isAfter(minDate)) it.minusWeeks(1) else it }
        val totalDays = ChronoUnit.DAYS.between(startMonday, maxDate).toInt().coerceAtLeast(0)
        val weekCount = (totalDays / 7) + 1

        val pad = with(canvas) { 10.dp.toPx() }
        val dayLabelWidth = with(canvas) { 30.dp.toPx() }
        val monthHeaderH = with(canvas) { 18.dp.toPx() }
        val legendH = with(canvas) { 26.dp.toPx() }

        val gridLeft = rect.left + pad + dayLabelWidth
        val gridTop = rect.top + pad + monthHeaderH
        val gridRight = rect.right - pad
        val gridBottom = rect.bottom - pad - legendH
        val gridW = (gridRight - gridLeft).coerceAtLeast(28f)
        val gridH = (gridBottom - gridTop).coerceAtLeast(28f)

        val cellGap = with(canvas) { 2.dp.toPx() }
        val cellSize = min(
            (gridW - cellGap * (weekCount - 1)) / weekCount.toFloat(),
            (gridH - cellGap * 6) / 7f,
        ).coerceAtLeast(4f)

        val labelStyle = style.axisLabelStyle.copy(color = style.axisLabelColor)
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (row in 0 until 7) {
            if (row % 2 == 0) {
                val dl = textMeasurer.measure(AnnotatedString(dayNames[row]), style = labelStyle, maxLines = 1)
                val cy = gridTop + row * (cellSize + cellGap) + (cellSize - dl.size.height) * 0.5f
                AxisLabelRenderer.drawTextLabel(canvas, dl, Offset(rect.left + pad, cy), style.axisLabelColor)
            }
        }

        // Value range for intensity levels (strictly positive or non-zero)
        val validNonZero = parsed.values.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() && v > 0.0 } }
        val maxPos = validNonZero.maxOrNull() ?: 1.0
        val isJournalBw = style.palette == StatGraphChartPalette.JOURNAL_BW
        val baseAccent = style.seriesColor(0)

        var lastMonthLabelX = -1000f
        var lastMonth = -1

        for (w in 0 until weekCount) {
            val weekStart = startMonday.plusWeeks(w.toLong())
            if (weekStart.monthValue != lastMonth) {
                val mText = weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val ml = textMeasurer.measure(AnnotatedString(mText), style = labelStyle, maxLines = 1)
                val mx = gridLeft + w * (cellSize + cellGap)
                if (mx - lastMonthLabelX >= ml.size.width + 6f && mx + ml.size.width <= gridRight + 4f) {
                    AxisLabelRenderer.drawTextLabel(canvas, ml, Offset(mx, rect.top + pad), style.axisLabelColor)
                    lastMonthLabelX = mx
                }
                lastMonth = weekStart.monthValue
            }

            for (d in 0 until 7) {
                val cellDate = weekStart.plusDays(d.toLong())
                if (cellDate.isBefore(minDate) || cellDate.isAfter(maxDate)) continue

                val left = gridLeft + w * (cellSize + cellGap)
                val top = gridTop + d * (cellSize + cellGap)
                val pt = parsed[cellDate]
                val valNum = pt?.numericValue?.takeIf { it.isFinite() }

                when {
                    // Missing date / missing observation: crisp hollow outline, never zero
                    valNum == null -> {
                        canvas.drawRoundRect(
                            color = style.gridLineColor,
                            topLeft = Offset(left, top),
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(2f, 2f),
                            style = Stroke(width = 1f),
                        )
                    }
                    // Measured zero: explicit zero fill distinct from missing
                    valNum == 0.0 -> {
                        if (isJournalBw) {
                            canvas.drawRoundRect(
                                color = Color(0xFFF1F5F9),
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                            canvas.drawRoundRect(
                                color = Color.Black,
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(2f, 2f),
                                style = Stroke(width = 1f),
                            )
                        } else {
                            canvas.drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                    // Positive value quantized into 4 intensity levels
                    else -> {
                        val ratio = (valNum / maxPos).coerceIn(0.0, 1.0).toFloat()
                        if (isJournalBw) {
                            val pattern = when {
                                ratio < 0.25f -> JournalBwFillPattern.DOTS
                                ratio < 0.50f -> JournalBwFillPattern.DIAGONAL_HATCH
                                ratio < 0.75f -> JournalBwFillPattern.CROSS_HATCH
                                else -> JournalBwFillPattern.SOLID_BLACK
                            }
                            JournalBwPatternPainter.drawPatternedRectCompose(
                                drawScope = canvas,
                                left = left,
                                top = top,
                                width = cellSize,
                                height = cellSize,
                                pattern = pattern,
                                strokeWidthPx = 1f,
                                cornerRadiusPx = 2f,
                            )
                        } else {
                            val alpha = (0.28f + 0.72f * ratio).coerceIn(0.28f, 1f)
                            canvas.drawRoundRect(
                                color = baseAccent.copy(alpha = alpha),
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                }

                // Conflict indicator dot in top-right corner when conflicting observations occurred on this date
                if (pt?.isConflictFlagged == true) {
                    canvas.drawCircle(
                        color = if (isJournalBw) Color.Black else Color(0xFFDC2626),
                        radius = (cellSize * 0.16f).coerceAtLeast(2f),
                        center = Offset(left + cellSize * 0.8f, top + cellSize * 0.2f),
                    )
                }
            }
        }

        // Compact legend strip distinguishing Missing vs Zero vs High
        val legendY = gridBottom + with(canvas) { 8.dp.toPx() }
        val boxS = with(canvas) { 10.dp.toPx() }
        var curX = gridLeft
        // Missing swatch
        canvas.drawRoundRect(
            color = style.gridLineColor,
            topLeft = Offset(curX, legendY),
            size = Size(boxS, boxS),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1f),
        )
        val missLbl = textMeasurer.measure(AnnotatedString("Missing"), style = labelStyle, maxLines = 1)
        AxisLabelRenderer.drawTextLabel(canvas, missLbl, Offset(curX + boxS + 4f, legendY - 1f), style.axisLabelColor)
        curX += boxS + missLbl.size.width + with(canvas) { 14.dp.toPx() }

        // Measured 0 swatch
        canvas.drawRoundRect(
            color = if (isJournalBw) Color(0xFFF1F5F9) else Color(0xFFE2E8F0),
            topLeft = Offset(curX, legendY),
            size = Size(boxS, boxS),
            cornerRadius = CornerRadius(2f, 2f),
        )
        if (isJournalBw) {
            canvas.drawRoundRect(
                color = Color.Black,
                topLeft = Offset(curX, legendY),
                size = Size(boxS, boxS),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(width = 1f),
            )
        }
        val zeroLbl = textMeasurer.measure(AnnotatedString("0"), style = labelStyle, maxLines = 1)
        AxisLabelRenderer.drawTextLabel(canvas, zeroLbl, Offset(curX + boxS + 4f, legendY - 1f), style.axisLabelColor)
        curX += boxS + zeroLbl.size.width + with(canvas) { 14.dp.toPx() }

        // High swatch
        canvas.drawRoundRect(
            color = if (isJournalBw) Color.Black else baseAccent,
            topLeft = Offset(curX, legendY),
            size = Size(boxS, boxS),
            cornerRadius = CornerRadius(2f, 2f),
        )
        val highLbl = textMeasurer.measure(AnnotatedString("High"), style = labelStyle, maxLines = 1)
        AxisLabelRenderer.drawTextLabel(canvas, highLbl, Offset(curX + boxS + 4f, legendY - 1f), style.axisLabelColor)
    }
}

/**
 * Renders 3–8 comparable radial axes ([ChartKind.RADAR]).
 *
 * Measures axis labels to prevent phone/export clipping, preserves missing axes
 * without coercing to 0, and supports Journal B&W stroke + marker patterns.
 */
class RadarChartPainter(
    private val textMeasurer: TextMeasurer,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val ringPath = Path()
    private val polyPath = Path()
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
        val axes = data.domainLabels
        val axisCount = axes.size
        if (!rect.isValid || axisCount < 3) return

        val labelStyle = style.axisLabelStyle.copy(color = style.axisLabelColor)
        val labelLayouts = axes.map { label ->
            textMeasurer.measure(
                text = AnnotatedString(label),
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = (rect.width * 0.28f).toInt().coerceAtLeast(48)),
            )
        }
        val maxLabelW = labelLayouts.maxOfOrNull { it.size.width.toFloat() } ?: 40f
        val maxLabelH = labelLayouts.maxOfOrNull { it.size.height.toFloat() } ?: 16f
        val pad = with(canvas) { 10.dp.toPx() }
        val radius = min(
            (rect.width - 2f * (maxLabelW + pad)) * 0.5f,
            (rect.height - 2f * (maxLabelH + pad)) * 0.5f,
        ).coerceAtLeast(24f)
        val cx = rect.centerX
        val cy = rect.centerY

        val allValues = data.series.flatMap { s -> s.points.mapNotNull { it.numericValue?.takeIf { v -> v.isFinite() } } }
        val scale = NumericScale.create(
            values = allValues,
            rangeStart = 0f,
            rangeEnd = radius,
            config = AxisScaleConfig(includeZeroBaseline = true, paddingFraction = 0.08, desiredTickCount = 4),
            minOverride = data.minNumericOverride,
            maxOverride = data.maxNumericOverride,
        )
        val minVal = scale.domainMin
        val span = (scale.domainMax - minVal).takeIf { it > 1e-9 } ?: 1.0

        val gridStroke = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val ringCount = 4

        // Concentric polygon grid rings
        for (rIdx in 1..ringCount) {
            val r = radius * (rIdx.toFloat() / ringCount.toFloat())
            ringPath.reset()
            for (a in 0 until axisCount) {
                val angle = -PI / 2.0 + (2.0 * PI * a / axisCount)
                val px = cx + r * cos(angle).toFloat()
                val py = cy + r * sin(angle).toFloat()
                if (a == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
            }
            ringPath.close()
            canvas.drawPath(path = ringPath, color = style.gridLineColor, style = Stroke(width = gridStroke))
        }

        // Radial spokes & measured labels
        val labelGap = with(canvas) { 6.dp.toPx() }
        for (a in 0 until axisCount) {
            val angle = -PI / 2.0 + (2.0 * PI * a / axisCount)
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            val ex = cx + radius * cosA
            val ey = cy + radius * sinA
            canvas.drawLine(
                color = style.gridLineColor,
                start = Offset(cx, cy),
                end = Offset(ex, ey),
                strokeWidth = gridStroke,
            )

            val layout = labelLayouts[a]
            val lxCenter = cx + (radius + labelGap) * cosA
            val lyCenter = cy + (radius + labelGap) * sinA
            val drawX = when {
                cosA > 0.25f -> lxCenter
                cosA < -0.25f -> lxCenter - layout.size.width
                else -> lxCenter - layout.size.width * 0.5f
            }.coerceIn(rect.left + 2f, (rect.right - layout.size.width - 2f).coerceAtLeast(rect.left + 2f))
            val drawY = when {
                sinA > 0.25f -> lyCenter
                sinA < -0.25f -> lyCenter - layout.size.height
                else -> lyCenter - layout.size.height * 0.5f
            }.coerceIn(rect.top + 2f, (rect.bottom - layout.size.height - 2f).coerceAtLeast(rect.top + 2f))

            AxisLabelRenderer.drawTextLabel(canvas, layout, Offset(drawX, drawY), style.axisLabelColor)
        }

        if (!data.hasDrawablePoints) return
        val clampedProgress = progress.coerceIn(0f, 1f)
        val lineStroke = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadius = with(canvas) { style.markerRadiusDp.toPx() }
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

            val coords = ArrayList<Pair<Float, Float>?>(axisCount)
            for (a in 0 until axisCount) {
                val v = series.points.firstOrNull { it.domainIndex == a }?.numericValue?.takeIf { it.isFinite() }
                if (v == null) {
                    coords.add(null)
                } else {
                    val frac = ((v - minVal) / span).toFloat().coerceIn(0f, 1f) * clampedProgress
                    val angle = -PI / 2.0 + (2.0 * PI * a / axisCount)
                    coords.add(Pair(cx + radius * frac * cos(angle).toFloat(), cy + radius * frac * sin(angle).toFloat()))
                }
            }

            // Only draw closed fill when all axes are present; otherwise draw contiguous segments without fabricating 0s
            val allPresent = coords.all { it != null }
            if (allPresent) {
                polyPath.reset()
                coords.forEachIndexed { idx, pt ->
                    val (px, py) = pt!!
                    if (idx == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                }
                polyPath.close()
                canvas.drawPath(
                    path = polyPath,
                    color = seriesColor.copy(alpha = if (isJournalBw) 0.10f else 0.18f),
                    style = Fill,
                )
                canvas.drawPath(
                    path = polyPath,
                    color = seriesColor,
                    style = Stroke(width = lineStroke, join = StrokeJoin.Round, pathEffect = pathEffect),
                )
            } else {
                polyPath.reset()
                var open = false
                for (pt in coords) {
                    if (pt != null) {
                        if (!open) {
                            polyPath.moveTo(pt.first, pt.second)
                            open = true
                        } else {
                            polyPath.lineTo(pt.first, pt.second)
                        }
                    } else {
                        open = false
                    }
                }
                canvas.drawPath(
                    path = polyPath,
                    color = seriesColor,
                    style = Stroke(width = lineStroke, cap = StrokeCap.Round, pathEffect = pathEffect),
                )
            }

            coords.forEach { pt ->
                if (pt != null) {
                    markersPainter.drawMarker(
                        canvas = canvas,
                        centerX = pt.first,
                        centerY = pt.second,
                        radiusPx = markerRadius,
                        shape = series.markerShape,
                        fillColor = seriesColor,
                        surfacePunchColor = style.plotBackground,
                        isSelected = false,
                    )
                }
            }
        }
    }
}
