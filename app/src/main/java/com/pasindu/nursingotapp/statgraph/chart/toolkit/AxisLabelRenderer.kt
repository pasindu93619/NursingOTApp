package com.pasindu.nursingotapp.statgraph.chart.toolkit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * [AxisTextMeasurer] backed by Compose [TextMeasurer] so [AxisLabelPlanner] uses real
 * rendered glyph metrics (Latin and Sinhala) at the exact target font size, weight, and density.
 */
class ComposeAxisTextMeasurer(
    private val textMeasurer: TextMeasurer,
    private val baseStyle: TextStyle,
    private val density: Density,
) : AxisTextMeasurer {

    private val layoutCache = HashMap<Triple<String, Float, Int>, TextLayoutResult>()

    override fun measure(
        text: String,
        fontSizePx: Float,
        fontWeight: Int,
    ): AxisTextMetrics {
        val layout = measureLayout(text, fontSizePx, fontWeight)
        return AxisTextMetrics(
            widthPx = layout.size.width.toFloat(),
            heightPx = layout.size.height.toFloat(),
        )
    }

    fun measureLayout(
        text: String,
        fontSizePx: Float = with(density) { baseStyle.fontSize.toPx() },
        fontWeight: Int = baseStyle.fontWeight?.weight ?: 400,
    ): TextLayoutResult {
        val key = Triple(text, fontSizePx, fontWeight)
        return layoutCache.getOrPut(key) {
            val fontSizeSp = with(density) { fontSizePx.coerceAtLeast(1f).toSp() }
            val effectiveStyle = baseStyle.copy(
                fontSize = fontSizeSp,
                fontWeight = FontWeight(fontWeight.coerceIn(100, 900)),
            )
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = effectiveStyle,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

data class TwoPassAxisLayout(
    val geometry: ResolvedChartGeometry,
    val xLabelPlan: AxisLabelPlan?,
)

/**
 * Shared axis-label planning and text-rendering helper used across all StatGraph painters
 * and offscreen PDF/PNG/JPG exports.
 */
object AxisLabelRenderer {

    fun resolveTwoPassGeometry(
        outerRect: ChartRect,
        data: ChartRenderData,
        baseInsets: ChartInsets,
        style: ChartStyle,
        textMeasurer: TextMeasurer,
        density: Density,
    ): TwoPassAxisLayout {
        if (!outerRect.isValid) {
            val fallback = ChartGeometryResolver.resolve(outerRect, data, baseInsets)
            return TwoPassAxisLayout(fallback, null)
        }

        val measurer = ComposeAxisTextMeasurer(textMeasurer, style.axisLabelStyle, density)
        val labelGapPx = with(density) { 6.dp.toPx() }
        val fontSizePx = with(density) { style.axisLabelStyle.fontSize.toPx() }

        // Pass 1: Initial geometry to obtain Y ticks and initial X scale slot centres
        val pass1Geometry = ChartGeometryResolver.resolve(outerRect, data, baseInsets)
        val isHorizontalCategoryChart = data.chartKind in setOf(
            ChartKind.DUMBBELL,
            ChartKind.BULLET,
            ChartKind.DIVERGING_BAR,
            ChartKind.LIKERT_DIVERGING,
            ChartKind.HORIZONTAL_BAR,
            ChartKind.LOLLIPOP,
            ChartKind.DOT_PLOT,
            ChartKind.RANGE_PLOT,
            ChartKind.FOREST_PLOT,
            ChartKind.POPULATION_PYRAMID,
        )
        val maxYTickWidth = if (isHorizontalCategoryChart) {
            data.domainLabels
                .filter { it.isNotEmpty() }
                .maxOfOrNull { measurer.measure(it, fontSizePx, style.axisLabelStyle.fontWeight?.weight ?: 400).widthPx }
                ?: 0f
        } else {
            pass1Geometry.yScale.formattedTicks
                .filter { it.isNotEmpty() }
                .maxOfOrNull { measurer.measure(it, fontSizePx, style.axisLabelStyle.fontWeight?.weight ?: 400).widthPx }
                ?: 0f
        }

        val pass1XPlan = planXAxis(
            geometry = pass1Geometry,
            data = data,
            style = style,
            measurer = measurer,
            density = density,
        )

        val maxLeftRatio = if (isHorizontalCategoryChart) 0.48f else 0.36f
        val rotatedFirstOverhangLeft = if (!isHorizontalCategoryChart &&
            data.domainLabels.isNotEmpty() &&
            pass1XPlan?.strategy == AxisLabelStrategy.ROTATED_45
        ) {
            val firstLabelW = measurer.measure(
                data.domainLabels.first(),
                fontSizePx,
                style.axisLabelStyle.fontWeight?.weight ?: 400,
            ).widthPx
            val halfSlot = if (data.domainLabels.size > 0) {
                (pass1Geometry.plotRect.width / (data.domainLabels.size * 2f)).coerceAtLeast(4f)
            } else {
                8f
            }
            (firstLabelW * 0.70710677f + fontSizePx * 0.5f - halfSlot + 4f)
                .coerceAtMost(outerRect.width * 0.22f)
        } else {
            0f
        }
        val neededLeft = max(
            baseInsets.left,
            max(maxYTickWidth + labelGapPx + 4f, rotatedFirstOverhangLeft),
        ).coerceAtMost(outerRect.width * maxLeftRatio)
        val neededRight = if (isHorizontalCategoryChart) {
            max(baseInsets.right, with(density) { 18.dp.toPx() })
        } else {
            baseInsets.right
        }
        val neededBottom = if (pass1XPlan != null) {
            max(baseInsets.bottom, pass1XPlan.requiredAxisHeightPx + labelGapPx)
                .coerceAtMost(outerRect.height * 0.48f)
        } else {
            baseInsets.bottom
        }

        // Pass 2: Re-resolve plotRect using planned axis heights/margins, then re-plan X labels
        val adjustedInsets = baseInsets.copy(
            left = neededLeft,
            right = neededRight,
            bottom = neededBottom,
        )
        val finalGeometry = ChartGeometryResolver.resolve(outerRect, data, adjustedInsets)
        val finalXPlan = planXAxis(
            geometry = finalGeometry,
            data = data,
            style = style,
            measurer = measurer,
            density = density,
        )
        return TwoPassAxisLayout(
            geometry = finalGeometry,
            xLabelPlan = finalXPlan,
        )
    }

    fun planXAxis(
        geometry: ResolvedChartGeometry,
        data: ChartRenderData,
        style: ChartStyle,
        measurer: AxisTextMeasurer,
        density: Density,
    ): AxisLabelPlan? {
        val plot = geometry.plotRect
        if (!plot.isValid) return null

        val fontSizePx = with(density) { style.axisLabelStyle.fontSize.toPx() }
        val fontWeight = style.axisLabelStyle.fontWeight?.weight ?: 400
        val numericCanvasLeft = (plot.left - 12f).coerceAtLeast(geometry.outerRect.left + 2f)
        val categoricalCanvasLeft = (geometry.outerRect.left + 2f).coerceAtMost(plot.left)
        val canvasRight = (geometry.outerRect.right - 2f).coerceAtLeast(plot.right)

        val usesNumericX = data.chartKind in setOf(
            ChartKind.SCATTER,
            ChartKind.CONNECTED_SCATTER,
            ChartKind.BUBBLE,
            ChartKind.DUMBBELL,
            ChartKind.BULLET,
            ChartKind.DIVERGING_BAR,
            ChartKind.LIKERT_DIVERGING,
            ChartKind.HORIZONTAL_BAR,
            ChartKind.LOLLIPOP,
            ChartKind.DOT_PLOT,
            ChartKind.RANGE_PLOT,
            ChartKind.FOREST_PLOT,
            ChartKind.POPULATION_PYRAMID,
            ChartKind.ECDF,
            ChartKind.QQ_PLOT,
            ChartKind.HEXBIN,
            ChartKind.KAPLAN_MEIER,
            ChartKind.DENSITY,
        )
        return if (usesNumericX && geometry.xNumericScale != null) {
            val xScale = geometry.xNumericScale
            val labels = ArrayList<String>(xScale.ticks.size)
            val centers = ArrayList<Float>(xScale.ticks.size)
            for (i in xScale.ticks.indices) {
                val xPx = xScale.toPixel(xScale.ticks[i]) ?: continue
                if (xPx < plot.left - 1f || xPx > plot.right + 1f) continue
                val label = xScale.formattedTicks.getOrElse(i) { "" }
                if (label.isNotEmpty()) {
                    labels.add(label)
                    centers.add(xPx)
                }
            }
            if (labels.isEmpty()) return null
            AxisLabelPlanner.plan(
                AxisLabelInput(
                    labels = labels,
                    slotCentersPx = centers,
                    axisStartPx = plot.left,
                    axisEndPx = plot.right,
                    fontSizePx = fontSizePx,
                    fontWeight = fontWeight,
                    textMeasurer = measurer,
                    allowRotation = false,
                    canvasLeftPx = numericCanvasLeft,
                    canvasRightPx = canvasRight,
                ),
            )
        } else {
            val xScale = geometry.xCategoricalScale ?: return null
            if (xScale.count <= 0) return null
            val family = ChartDensityDiagnostics.classifyFamily(data.chartKind)
            val isSequentialOrTimeChart = family == AdaptiveChartFamily.ORDERED_LINE_OR_AREA ||
                data.chartKind == ChartKind.HISTOGRAM
            val isBarChart = !isSequentialOrTimeChart
            val labels = ArrayList<String>(xScale.count)
            val centers = ArrayList<Float>(xScale.count)
            for (i in 0 until xScale.count) {
                val centerX = (if (isBarChart) xScale.bandCenter(i) else xScale.pointX(i)) ?: continue
                labels.add(xScale.labels.getOrNull(i).orEmpty())
                centers.add(centerX)
            }
            if (labels.isEmpty()) return null
            AxisLabelPlanner.plan(
                AxisLabelInput(
                    labels = labels,
                    slotCentersPx = centers,
                    axisStartPx = plot.left,
                    axisEndPx = plot.right,
                    fontSizePx = fontSizePx,
                    fontWeight = fontWeight,
                    textMeasurer = measurer,
                    allowRotation = true,
                    temporalMetadata = if (isSequentialOrTimeChart) {
                        AxisTemporalMetadata(isTemporal = true)
                    } else {
                        null
                    },
                    canvasLeftPx = categoricalCanvasLeft,
                    canvasRightPx = canvasRight,
                    preferredDisplayMode = data.preferredLabelMode,
                ),
            )
        }
    }

    fun drawYAxisTickLabel(
        canvas: DrawScope,
        textMeasurer: TextMeasurer,
        text: String,
        yPx: Float,
        plot: ChartRect,
        labelGapPx: Float,
        style: ChartStyle,
    ) {
        if (text.isEmpty()) return
        val maxAllowedW = (plot.left - labelGapPx - 3f).toInt().coerceAtLeast(20)
        val measured = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style.axisLabelStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxAllowedW),
        )
        val textX = (plot.left - labelGapPx - measured.size.width).coerceAtLeast(2f)
        val textY = (yPx - measured.size.height / 2f).coerceIn(
            plot.top - measured.size.height / 2f,
            plot.bottom - measured.size.height / 2f,
        )
        canvas.drawText(
            textLayoutResult = measured,
            color = style.axisLabelColor,
            topLeft = Offset(textX, textY),
        )
    }

    fun drawPlannedXAxisLabels(
        canvas: DrawScope,
        textMeasurer: TextMeasurer,
        plan: AxisLabelPlan,
        geometry: ResolvedChartGeometry,
        labelGapPx: Float,
        style: ChartStyle,
        selectedDomainIndex: Int?,
    ) {
        val plot = geometry.plotRect
        val measurer = ComposeAxisTextMeasurer(textMeasurer, style.axisLabelStyle, canvas)
        val fontSizePx = with(canvas) { style.axisLabelStyle.fontSize.toPx() }
        val fontWeight = style.axisLabelStyle.fontWeight?.weight ?: 400
        val tickTopY = plot.bottom + labelGapPx
        var maxTickBottomY = tickTopY

        for (item in plan.items) {
            if (!item.visible || item.displayText.isEmpty()) continue
            val layout = measurer.measureLayout(item.displayText, fontSizePx, fontWeight)
            val color = if (selectedDomainIndex == item.originalIndex) {
                style.titleColor
            } else {
                style.axisLabelColor
            }
            if (item.rotationDegrees == 0f) {
                val drawX = item.bounds.left.coerceIn(
                    geometry.outerRect.left,
                    (geometry.outerRect.right - layout.size.width).coerceAtLeast(geometry.outerRect.left),
                )
                canvas.drawText(
                    textLayoutResult = layout,
                    color = color,
                    topLeft = Offset(drawX, tickTopY),
                )
                maxTickBottomY = max(maxTickBottomY, tickTopY + layout.size.height)
            } else if (abs(item.rotationDegrees + 90f) < 1f) {
                // End-anchored 90° vertical rotation centered on item.xPx
                val pivot = Offset(item.xPx, tickTopY)
                canvas.withTransform({
                    rotate(degrees = item.rotationDegrees, pivot = pivot)
                }) {
                    drawText(
                        textLayoutResult = layout,
                        color = color,
                        topLeft = Offset(
                            item.xPx - layout.size.width.toFloat(),
                            tickTopY - layout.size.height * 0.5f,
                        ),
                    )
                }
                maxTickBottomY = max(maxTickBottomY, tickTopY + item.bounds.height)
            } else {
                // End-anchored 45° rotation pivoted at the tick X
                val pivot = Offset(item.xPx, tickTopY)
                canvas.withTransform({
                    rotate(degrees = item.rotationDegrees, pivot = pivot)
                }) {
                    drawText(
                        textLayoutResult = layout,
                        color = color,
                        topLeft = Offset(
                            item.xPx - layout.size.width.toFloat(),
                            tickTopY - layout.size.height * 0.15f,
                        ),
                    )
                }
                maxTickBottomY = max(maxTickBottomY, tickTopY + item.bounds.height)
            }
        }

        // Draw simplified axis title (e.g. "Week", "සතිය", or "2026") centred below ticks
        val title = plan.axisTitle
        if (!title.isNullOrBlank()) {
            val titleLayout = measurer.measureLayout(title, fontSizePx, fontWeight)
            val titleX = (plot.centerX - titleLayout.size.width / 2f).coerceIn(
                geometry.outerRect.left,
                (geometry.outerRect.right - titleLayout.size.width).coerceAtLeast(geometry.outerRect.left),
            )
            val titleY = (maxTickBottomY + with(canvas) { 3.dp.toPx() }).coerceAtMost(
                (geometry.outerRect.bottom - titleLayout.size.height).coerceAtLeast(tickTopY),
            )
            canvas.drawText(
                textLayoutResult = titleLayout,
                color = style.subtitleColor,
                topLeft = Offset(titleX, titleY),
            )
        }
    }

    fun clampTooltipPosition(
        anchorX: Float,
        anchorY: Float,
        tooltipWidth: Float,
        tooltipHeight: Float,
        container: ChartRect,
        gap: Float,
    ): ChartPoint = LabelCollisionSolver.clampTooltipPosition(
        anchorX = anchorX,
        anchorY = anchorY,
        tooltipWidth = tooltipWidth,
        tooltipHeight = tooltipHeight,
        container = container,
        gap = gap,
    )

    /**
     * Shared text drawing entrypoint for all StatGraph toolkit painters so text rendering
     * remains centralized inside [AxisLabelRenderer].
     */
    fun drawTextLabel(
        canvas: DrawScope,
        textLayoutResult: TextLayoutResult,
        topLeft: Offset,
        color: Color,
    ) {
        canvas.drawText(
            textLayoutResult = textLayoutResult,
            topLeft = topLeft,
            color = color,
        )
    }
}
