package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ResolvedChartGeometry

@Immutable
data class ChartInteractionState(
    val selectedDomainIndex: Int? = null,
    val showTooltipCrosshair: Boolean = true,
)

/**
 * Core drawing contract for all Stat Graph Studio chart renderers.
 *
 * Implementations must perform pure geometry mapping via [ResolvedChartGeometry] and must not
 * allocate Paths, Brushes, Paints, or TextMeasurers inside [draw].
 */
interface ChartPainter {
    fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
    )
}

/**
 * Extended painter contract supporting interactive selection focus and crosshair rendering while
 * preserving the standalone 5-argument [ChartPainter.draw] entrypoint.
 */
interface InteractiveChartPainter : ChartPainter {
    fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
        interaction: ChartInteractionState,
        preResolvedGeometry: ResolvedChartGeometry? = null,
    )

    override fun draw(
        canvas: DrawScope,
        rect: ChartRect,
        data: ChartRenderData,
        style: ChartStyle,
        progress: Float,
    ) {
        draw(
            canvas = canvas,
            rect = rect,
            data = data,
            style = style,
            progress = progress,
            interaction = ChartInteractionState(),
            preResolvedGeometry = null,
        )
    }
}
