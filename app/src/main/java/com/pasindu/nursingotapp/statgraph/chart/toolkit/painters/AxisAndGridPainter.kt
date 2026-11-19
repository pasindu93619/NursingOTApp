package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelPlan
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartInsets
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ComposeAxisTextMeasurer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.TwoPassAxisLayout
import kotlin.math.abs

/**
 * Draws horizontal dashed gridlines, zero-line / baseline rules, Y and X tick labels
 * via [AxisLabelRenderer] and [AxisLabelPlanner], subtle axis-truncation break marks,
 * and active crosshairs.
 */
class AxisAndGridPainter(
    internal val textMeasurer: TextMeasurer,
) {
    private val gridDashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    private val crosshairDashEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    private val breakPath: Path = Path()

    fun resolveTwoPass(
        outerRect: ChartRect,
        data: ChartRenderData,
        baseInsets: ChartInsets,
        style: ChartStyle,
        density: Density,
    ): TwoPassAxisLayout = AxisLabelRenderer.resolveTwoPassGeometry(
        outerRect = outerRect,
        data = data,
        baseInsets = baseInsets,
        style = style,
        textMeasurer = textMeasurer,
        density = density,
    )

    fun draw(
        canvas: DrawScope,
        data: ChartRenderData,
        geometry: ResolvedChartGeometry,
        style: ChartStyle,
        interaction: ChartInteractionState,
        prePlannedXAxis: AxisLabelPlan? = null,
    ) {
        val plot = geometry.plotRect
        if (!plot.isValid) return

        val gridStrokePx = with(canvas) { style.gridStrokeWidthDp.toPx() }
        val axisStrokePx = with(canvas) { style.axisStrokeWidthDp.toPx() }
        val labelGapPx = with(canvas) { 6.dp.toPx() }

        // 1. Horizontal gridlines and right-aligned Y-axis tick labels
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
        val yScale = geometry.yScale
        val tickCount = if (isHorizontalCategoryChart) 0 else yScale.ticks.size
        for (i in 0 until tickCount) {
            val tickValue = yScale.ticks[i]
            val tickLabel = yScale.formattedTicks.getOrElse(i) { "" }
            val yPx = yScale.toPixel(tickValue) ?: continue
            if (yPx < plot.top - 1f || yPx > plot.bottom + 1f) continue

            val isZero = abs(tickValue) < 1e-9
            canvas.drawLine(
                color = if (isZero) style.zeroAxisColor else style.gridLineColor,
                start = Offset(plot.left, yPx),
                end = Offset(plot.right, yPx),
                strokeWidth = if (isZero) axisStrokePx * 1.25f else gridStrokePx,
                pathEffect = if (isZero) null else gridDashEffect,
            )

            if (tickLabel.isNotEmpty()) {
                AxisLabelRenderer.drawYAxisTickLabel(
                    canvas = canvas,
                    textMeasurer = textMeasurer,
                    text = tickLabel,
                    yPx = yPx,
                    plot = plot,
                    labelGapPx = labelGapPx,
                    style = style,
                )
            }
        }

        // 2. Plot bottom baseline (and faint left guide when truncated)
        canvas.drawLine(
            color = style.zeroAxisColor,
            start = Offset(plot.left, plot.bottom),
            end = Offset(plot.right, plot.bottom),
            strokeWidth = axisStrokePx,
        )

        if (yScale.isTruncated) {
            drawTruncationBreak(canvas, plot.left, plot.bottom, style, axisStrokePx)
        }

        // 3. Domain X labels (categorical vs numeric scatter) via shared AxisLabelPlanner + AxisLabelRenderer
        if (data.chartKind in setOf(
                ChartKind.SCATTER,
                ChartKind.CONNECTED_SCATTER,
                ChartKind.BUBBLE,
                ChartKind.ECDF,
                ChartKind.QQ_PLOT,
                ChartKind.HEXBIN,
                ChartKind.HORIZONTAL_BAR,
                ChartKind.LOLLIPOP,
                ChartKind.DOT_PLOT,
                ChartKind.RANGE_PLOT,
                ChartKind.FOREST_PLOT,
                ChartKind.POPULATION_PYRAMID,
                ChartKind.KAPLAN_MEIER,
                ChartKind.DENSITY,
            ) && geometry.xNumericScale != null
        ) {
            val xScale = geometry.xNumericScale
            for (i in xScale.ticks.indices) {
                val xPx = xScale.toPixel(xScale.ticks[i]) ?: continue
                if (xPx < plot.left - 1f || xPx > plot.right + 1f) continue
                canvas.drawLine(
                    color = style.gridLineColor.copy(alpha = 0.65f),
                    start = Offset(xPx, plot.top),
                    end = Offset(xPx, plot.bottom),
                    strokeWidth = gridStrokePx,
                    pathEffect = gridDashEffect,
                )
            }
        }

        val xPlan = prePlannedXAxis ?: AxisLabelRenderer.planXAxis(
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
                selectedDomainIndex = interaction.selectedDomainIndex,
            )
        }

        // 4. Vertical crosshair for selected point/category
        val selectedIndex = interaction.selectedDomainIndex
        if (selectedIndex != null && interaction.showTooltipCrosshair) {
            val crosshairX = resolveSelectedX(data, geometry, selectedIndex)
            if (crosshairX != null && crosshairX in plot.left..plot.right) {
                canvas.drawLine(
                    color = style.crosshairColor,
                    start = Offset(crosshairX, plot.top),
                    end = Offset(crosshairX, plot.bottom),
                    strokeWidth = axisStrokePx * 1.5f,
                    pathEffect = crosshairDashEffect,
                )
            }
        }
    }

    private fun resolveSelectedX(
        data: ChartRenderData,
        geometry: ResolvedChartGeometry,
        domainIndex: Int,
    ): Float? {
        return when (data.chartKind) {
            ChartKind.LINE,
            ChartKind.MULTI_LINE,
            ChartKind.AREA,
            ChartKind.STACKED_AREA,
            ChartKind.STEP,
            ChartKind.CONTROL_CHART,
            ChartKind.SLOPE,
            ChartKind.BUMP,
            ChartKind.RANGE_BAND,
            ChartKind.CYCLE_PLOT -> geometry.xCategoricalScale?.pointX(domainIndex)
            ChartKind.BAR,
            ChartKind.GROUPED_BAR,
            ChartKind.STACKED_BAR,
            ChartKind.PERCENT_STACKED_BAR,
            ChartKind.HISTOGRAM,
            ChartKind.BOX_PLOT,
            ChartKind.VIOLIN,
            ChartKind.STRIP_PLOT,
            ChartKind.ERROR_BAR,
            ChartKind.WATERFALL,
            ChartKind.PARETO -> geometry.xCategoricalScale?.bandCenter(domainIndex)
            ChartKind.SCATTER,
            ChartKind.CONNECTED_SCATTER,
            ChartKind.BUBBLE,
            ChartKind.ECDF,
            ChartKind.QQ_PLOT,
            ChartKind.HEXBIN,
            ChartKind.KAPLAN_MEIER,
            ChartKind.DENSITY -> {
                val datum = data.series.asSequence()
                    .flatMap { it.points.asSequence() }
                    .firstOrNull { it.domainIndex == domainIndex && it.secondaryNumericValue != null }
                val xVal = datum?.secondaryNumericValue ?: return null
                geometry.xNumericScale?.toPixel(xVal)
            }
            ChartKind.DUMBBELL,
            ChartKind.BULLET,
            ChartKind.SPARKLINE_MULTIPLES,
            ChartKind.CALENDAR_HEATMAP,
            ChartKind.RADAR,
            ChartKind.DIVERGING_BAR,
            ChartKind.LIKERT_DIVERGING,
            ChartKind.WAFFLE,
            ChartKind.STEM_AND_LEAF,
            ChartKind.SCATTERPLOT_MATRIX,
            ChartKind.MOSAIC,
            ChartKind.SUNBURST,
            ChartKind.HORIZONTAL_BAR,
            ChartKind.LOLLIPOP,
            ChartKind.DOT_PLOT,
            ChartKind.RANGE_PLOT,
            ChartKind.FOREST_PLOT,
            ChartKind.PIE,
            ChartKind.DONUT,
            ChartKind.CORRELATION_HEATMAP,
            ChartKind.POPULATION_PYRAMID,
            ChartKind.TREEMAP,
            ChartKind.FUNNEL -> null
        }
    }

    private fun drawTruncationBreak(
        canvas: DrawScope,
        axisLeftX: Float,
        baselineY: Float,
        style: ChartStyle,
        strokeWidth: Float,
    ) {
        val w = with(canvas) { 6.dp.toPx() }
        val h = with(canvas) { 4.dp.toPx() }
        val yCenter = baselineY - with(canvas) { 10.dp.toPx() }
        breakPath.reset()
        breakPath.moveTo(axisLeftX - w * 0.5f, yCenter + h * 0.5f)
        breakPath.lineTo(axisLeftX + w * 0.5f, yCenter - h * 0.5f)
        breakPath.moveTo(axisLeftX - w * 0.5f, yCenter + h * 1.3f)
        breakPath.lineTo(axisLeftX + w * 0.5f, yCenter + h * 0.3f)
        canvas.drawPath(
            path = breakPath,
            color = style.truncationIndicatorColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth * 1.2f,
                cap = StrokeCap.Round,
            ),
        )
    }
}
