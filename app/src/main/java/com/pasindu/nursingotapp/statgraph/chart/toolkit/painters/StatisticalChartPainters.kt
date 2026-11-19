package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartColorPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDataSeries
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartDatum
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartGeometryResolver
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendEntryInput
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendLayoutPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendTextMeasurer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartInsets
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartPaletteTokens
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesStrokePattern
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Density.toPixelInsets(style: ChartStyle): ChartInsets = ChartInsets(
    left = style.plotInsetsDp.left.dp.toPx(),
    top = style.plotInsetsDp.top.dp.toPx(),
    right = style.plotInsetsDp.right.dp.toPx(),
    bottom = style.plotInsetsDp.bottom.dp.toPx(),
)

private fun resolveTwoPass(
    canvas: DrawScope,
    rect: ChartRect,
    data: ChartRenderData,
    style: ChartStyle,
    axisAndGridPainter: AxisAndGridPainter,
    preResolvedGeometry: ResolvedChartGeometry?,
): TwoPassAxisLayout {
    if (preResolvedGeometry != null) {
        return TwoPassAxisLayout(preResolvedGeometry, null)
    }
    return axisAndGridPainter.resolveTwoPass(
        outerRect = rect,
        data = data,
        baseInsets = canvas.toPixelInsets(style),
        style = style,
        density = canvas,
    )
}

/**
 * Renders single-series and multi-series line charts with:
 * - Contiguous missing-data gaps (no false interpolation across MissingValue)
 * - Optional lower/upper limit confidence ribbon when limits exist
 * - Distinct stroke dash + marker shape redundancy per series
 * - Draw-phase left-to-right clip animation (`progress`)
 */
class LineChartPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
) : InteractiveChartPainter {
    private val linePath: Path = Path()
    private val bandFillPath: Path = Path()
    private val seriesDashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    private val seriesDotEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
    private val seriesDashDotEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f, 4f, 6f), 0f)
    private val upperXScratch: MutableList<Float> = mutableListOf()
    private val upperYScratch: MutableList<Float> = mutableListOf()
    private val lowerYScratch: MutableList<Float> = mutableListOf()

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry?,
    ) {
        val twoPass = resolveTwoPass(canvas, rect, data, style, axisAndGridPainter, preResolvedGeometry)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)

        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val strokePx = with(canvas) { style.lineStrokeWidthDp.toPx() }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() }
        val selectedRadiusPx = with(canvas) { style.selectedMarkerRadiusDp.toPx() }
        val clampedProgress = progress.coerceIn(0f, 1f)
        val clipRight = plot.left + plot.width * clampedProgress

        canvas.clipRect(
            left = plot.left - selectedRadiusPx * 2.2f,
            top = plot.top - selectedRadiusPx * 2.2f,
            right = (clipRight + selectedRadiusPx * 2.2f).coerceAtMost(geometry.outerRect.right),
            bottom = plot.bottom + selectedRadiusPx * 2.2f,
        ) {
            for (series in data.series) {
                val color = style.seriesColor(series.colorIndex)
                val byDomain = indexPointsByDomain(series)

                // Draw lower/upper limit band if present across contiguous segments
                drawLimitBandIfPresent(canvas, byDomain, xScale.count, xScale::pointX, yScale::toPixel, color)

                // Build segmented line paths breaking across missing values
                linePath.reset()
                var segmentOpen = false
                for (index in 0 until xScale.count) {
                    val datum = byDomain[index]
                    val value = datum?.numericValue
                    if (value == null || !value.isFinite()) {
                        segmentOpen = false
                        continue
                    }
                    val x = xScale.pointX(index) ?: continue
                    val y = yScale.toPixel(value) ?: continue
                    if (!segmentOpen) {
                        linePath.moveTo(x, y)
                        segmentOpen = true
                    } else {
                        linePath.lineTo(x, y)
                    }
                }

                val strokeEffect = if (
                    style.palette == StatGraphChartPalette.JOURNAL_BW ||
                    style.palette == StatGraphChartPalette.APP_VIVID
                ) {
                    when (ChartPaletteTokens.strokePatternAt(series.colorIndex)) {
                        SeriesStrokePattern.SOLID -> if (series.dashed) seriesDashEffect else null
                        SeriesStrokePattern.DASHED -> seriesDashEffect
                        SeriesStrokePattern.DOTTED -> seriesDotEffect
                        SeriesStrokePattern.DASH_DOT -> seriesDashDotEffect
                    }
                } else {
                    if (series.dashed) seriesDashEffect else null
                }

                canvas.drawPath(
                    path = linePath,
                    color = color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = strokeEffect,
                    ),
                )

                // Draw point markers (respecting selection halo)
                for (index in 0 until xScale.count) {
                    val value = byDomain[index]?.numericValue ?: continue
                    if (!value.isFinite()) continue
                    val x = xScale.pointX(index) ?: continue
                    val y = yScale.toPixel(value) ?: continue
                    val isSelected = interaction.selectedDomainIndex == index
                    markersPainter.drawMarker(
                        canvas = canvas,
                        centerX = x,
                        centerY = y,
                        radiusPx = markerRadiusPx,
                        shape = series.markerShape,
                        fillColor = color,
                        surfacePunchColor = style.plotBackground,
                        isSelected = isSelected,
                    )
                }
            }
        }
    }

    private fun indexPointsByDomain(series: ChartDataSeries): HashMap<Int, ChartDatum> {
        val map = HashMap<Int, ChartDatum>(series.points.size)
        for (point in series.points) {
            map[point.domainIndex] = point
        }
        return map
    }

    private fun drawLimitBandIfPresent(
        canvas: DrawScope,
        pointsByDomain: Map<Int, ChartDatum>,
        domainCount: Int,
        xLookup: (Int) -> Float?,
        yLookup: (Double) -> Float?,
        seriesColor: androidx.compose.ui.graphics.Color,
    ) {
        upperXScratch.clear()
        upperYScratch.clear()
        lowerYScratch.clear()

        fun flushBandRun() {
            if (upperXScratch.size >= 2) {
                bandFillPath.reset()
                bandFillPath.moveTo(upperXScratch[0], upperYScratch[0])
                for (i in 1 until upperXScratch.size) {
                    bandFillPath.lineTo(upperXScratch[i], upperYScratch[i])
                }
                for (i in upperXScratch.size - 1 downTo 0) {
                    bandFillPath.lineTo(upperXScratch[i], lowerYScratch[i])
                }
                bandFillPath.close()
                canvas.drawPath(
                    path = bandFillPath,
                    color = seriesColor.copy(alpha = 0.14f),
                    style = Fill,
                )
            }
            upperXScratch.clear()
            upperYScratch.clear()
            lowerYScratch.clear()
        }

        for (i in 0 until domainCount) {
            val datum = pointsByDomain[i]
            val lower = datum?.lowerLimit
            val upper = datum?.upperLimit
            if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                val x = xLookup(i)
                val yU = yLookup(upper)
                val yL = yLookup(lower)
                if (x != null && yU != null && yL != null) {
                    upperXScratch.add(x)
                    upperYScratch.add(yU)
                    lowerYScratch.add(yL)
                    continue
                }
            }
            flushBandRun()
        }
        flushBandRun()
    }
}

/**
 * Renders single-series, grouped, stacked, and 100%-stacked bars anchored strictly at the
 * resolved zero baseline, with draw-phase growth animation and gentle focus emphasis.
 */
class BarChartPainter(
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
        val twoPass = resolveTwoPass(canvas, rect, data, style, axisAndGridPainter, preResolvedGeometry)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)

        val xScale = geometry.xCategoricalScale ?: return
        val yScale = geometry.yScale
        val zeroY = yScale.zeroLinePixel() ?: plot.bottom
        val cornerRadiusPx = with(canvas) { style.barCornerRadiusDp.toPx() }
        val clampedProgress = progress.coerceIn(0f, 1f)
        val seriesList = data.series
        if (seriesList.isEmpty() || xScale.count == 0) return

        val pointsBySeries = seriesList.map { series ->
            HashMap<Int, ChartDatum>(series.points.size).apply {
                series.points.forEach { put(it.domainIndex, it) }
            }
        }

        when (data.chartKind) {
            ChartKind.STACKED_BAR, ChartKind.PERCENT_STACKED_BAR -> {
                val minBarPx = with(canvas) { BarDensityPolicy.MIN_ON_SCREEN_BAR_DP.dp.toPx() }
                val barWidth = xScale.bandWidth
                    .coerceAtMost(with(canvas) { 42.dp.toPx() })
                    .coerceAtLeast(minBarPx)
                val isPercent = data.chartKind == ChartKind.PERCENT_STACKED_BAR

                for (domainIndex in 0 until xScale.count) {
                    val centerX = xScale.bandCenter(domainIndex) ?: continue
                    val leftX = centerX - barWidth / 2f
                    val alpha = if (interaction.selectedDomainIndex == null || interaction.selectedDomainIndex == domainIndex) {
                        1f
                    } else {
                        0.45f
                    }

                    val totalAbs = if (isPercent) {
                        pointsBySeries.sumOf { map ->
                            abs(map[domainIndex]?.numericValue?.takeIf { it.isFinite() } ?: 0.0)
                        }
                    } else {
                        1.0
                    }

                    var runningPos = 0.0
                    var runningNeg = 0.0
                    for (sIdx in seriesList.indices) {
                        val rawValue = pointsBySeries[sIdx][domainIndex]?.numericValue ?: continue
                        if (!rawValue.isFinite() || rawValue == 0.0) continue
                        val scaled = if (isPercent && totalAbs > 0.0) {
                            (rawValue / totalAbs) * 100.0
                        } else {
                            rawValue
                        }
                        val baseDomain = if (scaled >= 0.0) runningPos else runningNeg
                        val endDomain = baseDomain + scaled
                        if (scaled >= 0.0) runningPos = endDomain else runningNeg = endDomain

                        val animatedBase = baseDomain * clampedProgress
                        val animatedEnd = endDomain * clampedProgress
                        val yStart = yScale.toPixel(animatedBase) ?: continue
                        val yEnd = yScale.toPixel(animatedEnd) ?: continue
                        val topY = min(yStart, yEnd)
                        val height = abs(yEnd - yStart)
                        if (height < 0.5f) continue

                        val isTopSegment = (sIdx == seriesList.lastIndex)
                        val appliedCorner = if (isTopSegment && height > cornerRadiusPx * 2f) cornerRadiusPx else 0f
                        if (style.palette == StatGraphChartPalette.JOURNAL_BW) {
                            val identity = JournalBwPatternPainter.barIdentityForSeries(seriesList[sIdx].colorIndex)
                            JournalBwPatternPainter.drawPatternedRectCompose(
                                drawScope = canvas,
                                left = leftX,
                                top = topY,
                                width = barWidth,
                                height = height,
                                pattern = identity.fillPattern,
                                strokeWidthPx = with(canvas) { 1.dp.toPx() },
                                cornerRadiusPx = appliedCorner,
                                outlineStyle = identity.outlineStyle,
                            )
                        } else {
                            val color = style.seriesColor(seriesList[sIdx].colorIndex).copy(alpha = alpha)
                            val needsOutline = style.palette == StatGraphChartPalette.OKABE_ITO
                            val outlineStroke = if (needsOutline) Stroke(width = with(canvas) { 1.dp.toPx() }) else null
                            val outlineColor = style.titleColor.copy(alpha = 0.65f * alpha)
                            if (appliedCorner > 0f) {
                                canvas.drawRoundRect(
                                    color = color,
                                    topLeft = Offset(leftX, topY),
                                    size = Size(barWidth, height),
                                    cornerRadius = CornerRadius(appliedCorner, appliedCorner),
                                )
                                if (outlineStroke != null) {
                                    canvas.drawRoundRect(
                                        color = outlineColor,
                                        topLeft = Offset(leftX, topY),
                                        size = Size(barWidth, height),
                                        cornerRadius = CornerRadius(appliedCorner, appliedCorner),
                                        style = outlineStroke,
                                    )
                                }
                            } else {
                                canvas.drawRect(
                                    color = color,
                                    topLeft = Offset(leftX, topY),
                                    size = Size(barWidth, height),
                                )
                                if (outlineStroke != null) {
                                    canvas.drawRect(
                                        color = outlineColor,
                                        topLeft = Offset(leftX, topY),
                                        size = Size(barWidth, height),
                                        style = outlineStroke,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                val seriesCount = seriesList.size.coerceAtLeast(1)
                val minBarPx = with(canvas) { BarDensityPolicy.MIN_ON_SCREEN_BAR_DP.dp.toPx() }
                val innerGap = if (seriesCount > 1) with(canvas) { BarDensityPolicy.DEFAULT_INNER_GAP_DP.dp.toPx() } else 0f
                val stepPitch = if (xScale.count >= 2) {
                    val c0 = xScale.bandCenter(0) ?: 0f
                    val c1 = xScale.bandCenter(1) ?: xScale.bandWidth
                    abs(c1 - c0)
                } else {
                    xScale.bandWidth
                }
                val minInterGroupGap = with(canvas) {
                    when {
                        xScale.count >= 25 -> 2.dp.toPx()
                        xScale.count >= 15 -> 3.dp.toPx()
                        else -> 4.dp.toPx()
                    }
                }
                val maxBand = min(
                    xScale.bandWidth,
                    (stepPitch - minInterGroupGap).coerceAtLeast(minBarPx * seriesCount),
                ).coerceAtMost(
                    with(canvas) { (if (seriesCount == 1) 44 else 68).dp.toPx() },
                )
                val slotWidth = ((maxBand - innerGap * (seriesCount - 1)) / seriesCount).coerceAtLeast(minBarPx)
                val totalGroupWidth = slotWidth * seriesCount + innerGap * (seriesCount - 1)
                val useDistinctColors = ChartColorPlanner.shouldUseDistinctCategoryColors(
                    data = data,
                    mode = data.preferredBarColorMode,
                    hasAbbreviationKey = twoPass.xLabelPlan?.abbreviationKey?.isNotEmpty() == true,
                )

                for (domainIndex in 0 until xScale.count) {
                    val bandCenter = xScale.bandCenter(domainIndex) ?: continue
                    val groupLeft = bandCenter - totalGroupWidth / 2f
                    val alpha = if (interaction.selectedDomainIndex == null || interaction.selectedDomainIndex == domainIndex) {
                        1f
                    } else {
                        0.45f
                    }
                    val domainLabel = data.domainLabels.getOrElse(domainIndex) { "" }

                    for (sIdx in seriesList.indices) {
                        val value = pointsBySeries[sIdx][domainIndex]?.numericValue ?: continue
                        if (!value.isFinite()) continue
                        val targetY = yScale.toPixel(value) ?: continue
                        val animatedY = zeroY + (targetY - zeroY) * clampedProgress
                        val topY = min(zeroY, animatedY)
                        val height = abs(animatedY - zeroY)
                        if (height < 0.5f) continue

                        val leftX = groupLeft + sIdx * (slotWidth + innerGap)
                        val r = min(cornerRadiusPx, min(slotWidth / 2f, height / 2f))
                        if (style.palette == StatGraphChartPalette.JOURNAL_BW) {
                            val patIdx = if (useDistinctColors && seriesCount == 1) domainIndex else seriesList[sIdx].colorIndex
                            val identity = JournalBwPatternPainter.barIdentityForSeries(patIdx)
                            JournalBwPatternPainter.drawPatternedRectCompose(
                                drawScope = canvas,
                                left = leftX,
                                top = topY,
                                width = slotWidth,
                                height = height,
                                pattern = identity.fillPattern,
                                strokeWidthPx = with(canvas) { 1.dp.toPx() },
                                cornerRadiusPx = r,
                                outlineStyle = identity.outlineStyle,
                            )
                        } else {
                            val barColor = if (seriesCount == 1) {
                                ChartColorPlanner.colorForCategory(
                                    domainIndex = domainIndex,
                                    domainLabel = domainLabel,
                                    style = style,
                                    useDistinctColors = useDistinctColors,
                                    defaultSeriesColorIndex = seriesList[sIdx].colorIndex,
                                )
                            } else {
                                style.seriesColor(seriesList[sIdx].colorIndex)
                            }
                            canvas.drawRoundRect(
                                color = barColor.copy(alpha = alpha),
                                topLeft = Offset(leftX, topY),
                                size = Size(slotWidth, height),
                                cornerRadius = CornerRadius(r, r),
                            )
                            if (style.palette == StatGraphChartPalette.OKABE_ITO || useDistinctColors) {
                                canvas.drawRoundRect(
                                    color = style.titleColor.copy(alpha = 0.55f * alpha),
                                    topLeft = Offset(leftX, topY),
                                    size = Size(slotWidth, height),
                                    cornerRadius = CornerRadius(r, r),
                                    style = Stroke(width = with(canvas) { 0.85.dp.toPx() }),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders paired numeric (X, Y) scatter points using color + shape encoding and draw-phase
 * radial reveal (`progress`). Automatically adapts marker alpha and radius for dense point clouds.
 */
class ScatterPlotPainter(
    private val axisAndGridPainter: AxisAndGridPainter,
    private val markersPainter: SeriesMarkersPainter = SeriesMarkersPainter(),
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
        val twoPass = resolveTwoPass(canvas, rect, data, style, axisAndGridPainter, preResolvedGeometry)
        val geometry = twoPass.geometry
        val plot = geometry.plotRect
        if (!plot.isValid) return

        axisAndGridPainter.draw(canvas, data, geometry, style, interaction, twoPass.xLabelPlan)

        val xScale = geometry.xNumericScale ?: return
        val yScale = geometry.yScale
        val totalPts = data.series.sumOf { it.points.size }
        val densityAlpha = when {
            totalPts > 200 -> 0.42f
            totalPts > 60 -> 0.65f
            else -> 0.92f
        }
        val densityRadiusScale = when {
            totalPts > 200 -> 0.72f
            totalPts > 60 -> 0.86f
            else -> 1.1f
        }
        val markerRadiusPx = with(canvas) { style.markerRadiusDp.toPx() } *
            progress.coerceIn(0.15f, 1f) * densityRadiusScale

        for (series in data.series) {
            val baseColor = style.seriesColor(series.colorIndex)
            for (datum in series.points) {
                val xVal = datum.secondaryNumericValue ?: continue
                val yVal = datum.numericValue ?: continue
                if (!xVal.isFinite() || !yVal.isFinite()) continue

                val xPx = xScale.toPixel(xVal) ?: continue
                val yPx = yScale.toPixel(yVal) ?: continue
                val isSelected = interaction.selectedDomainIndex == datum.domainIndex

                markersPainter.drawMarker(
                    canvas = canvas,
                    centerX = xPx,
                    centerY = yPx,
                    radiusPx = markerRadiusPx,
                    shape = series.markerShape,
                    fillColor = if (isSelected) baseColor else baseColor.copy(alpha = densityAlpha),
                    surfacePunchColor = style.plotBackground,
                    isSelected = isSelected,
                )
            }
        }
    }
}

/**
 * Compact wrap-friendly series legend pairing each Okabe-Ito color with its redundant marker
 * glyph so color-vision-deficient readers can distinguish multiple series reliably.
 */
@Composable
fun ChartLegendRow(
    series: List<ChartDataSeries>,
    style: ChartStyle,
    modifier: Modifier = Modifier,
    chartKind: ChartKind? = null,
) {
    if (series.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isBarChart = chartKind != null && BarDensityPolicy.isBarChartKind(chartKind)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val swatchWidthPx = with(density) { 12.dp.toPx() }
        val swatchGapPx = with(density) { 6.dp.toPx() }
        val horizontalItemGapPx = with(density) { 16.dp.toPx() }
        val verticalRowGapPx = with(density) { 6.dp.toPx() }

        val plan = remember(
            series,
            availableWidthPx,
            style.legendLabelStyle,
            swatchWidthPx,
            swatchGapPx,
            horizontalItemGapPx,
            verticalRowGapPx,
        ) {
            val entries = series.mapIndexed { idx, s ->
                LegendEntryInput(
                    seriesIndex = idx,
                    name = s.name,
                    unit = s.unit,
                    colorIndex = s.colorIndex,
                    markerShape = s.markerShape,
                )
            }
            val measurer = object : LegendTextMeasurer {
                override fun measureWidth(text: String): Float =
                    textMeasurer.measure(AnnotatedString(text), style.legendLabelStyle, maxLines = 1).size.width.toFloat()

                override fun lineHeight(): Float =
                    textMeasurer.measure(AnnotatedString("Ag"), style.legendLabelStyle, maxLines = 1).size.height.toFloat()
            }
            LegendLayoutPlanner.plan(
                entries = entries,
                availableWidthPx = availableWidthPx,
                measurer = measurer,
                swatchWidthPx = swatchWidthPx,
                swatchGapPx = swatchGapPx,
                horizontalItemGapPx = horizontalItemGapPx,
                verticalRowGapPx = verticalRowGapPx,
            )
        }

        val rows = remember(plan) {
            plan.items.groupBy { it.rowIndex }.toSortedMap()
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.values.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowItems.forEach { placed ->
                        val item = placed.entry
                        val color = style.seriesColor(item.colorIndex)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                if (style.palette == StatGraphChartPalette.JOURNAL_BW && isBarChart) {
                                    val identity = JournalBwPatternPainter.barIdentityForSeries(item.colorIndex)
                                    JournalBwPatternPainter.drawPatternedRectCompose(
                                        drawScope = this,
                                        left = 0f,
                                        top = 0f,
                                        width = size.width,
                                        height = size.height,
                                        pattern = identity.fillPattern,
                                        strokeWidthPx = 1.dp.toPx(),
                                        outlineStyle = identity.outlineStyle,
                                    )
                                } else {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val r = size.minDimension * 0.36f
                                    when (item.markerShape) {
                                        SeriesMarkerShape.CIRCLE -> {
                                            drawCircle(color = color, radius = r, center = Offset(cx, cy))
                                        }
                                        SeriesMarkerShape.SQUARE -> {
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(cx - r, cy - r),
                                                size = Size(r * 2f, r * 2f),
                                            )
                                        }
                                        SeriesMarkerShape.TRIANGLE -> {
                                            val p = Path().apply {
                                                moveTo(cx, cy - r)
                                                lineTo(cx + r, cy + r)
                                                lineTo(cx - r, cy + r)
                                                close()
                                            }
                                            drawPath(path = p, color = color)
                                        }
                                        SeriesMarkerShape.DIAMOND -> {
                                            val p = Path().apply {
                                                moveTo(cx, cy - r * 1.15f)
                                                lineTo(cx + r * 1.15f, cy)
                                                lineTo(cx, cy + r * 1.15f)
                                                lineTo(cx - r * 1.15f, cy)
                                                close()
                                            }
                                            drawPath(path = p, color = color)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = placed.displayLabel,
                                style = style.legendLabelStyle,
                                color = style.subtitleColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
