package com.pasindu.nursingotapp.statgraph.chart.toolkit.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape

/**
 * Reusable marker renderer supporting accessible shape redundancy (circle, square, triangle,
 * diamond) alongside color encoding, with zero object allocations inside the draw pass.
 */
class SeriesMarkersPainter {
    private val shapePath: Path = Path()

    fun drawMarker(
        canvas: DrawScope,
        centerX: Float,
        centerY: Float,
        radiusPx: Float,
        shape: SeriesMarkerShape,
        fillColor: Color,
        surfacePunchColor: Color,
        isSelected: Boolean,
    ) {
        if (isSelected) {
            canvas.drawCircle(
                color = fillColor.copy(alpha = 0.22f),
                radius = radiusPx * 2.0f,
                center = Offset(centerX, centerY),
            )
        }

        val r = if (isSelected) radiusPx * 1.25f else radiusPx
        val outlineStroke = with(canvas) { 1.5.dp.toPx() }

        when (shape) {
            SeriesMarkerShape.CIRCLE -> {
                canvas.drawCircle(
                    color = surfacePunchColor,
                    radius = r + outlineStroke * 0.6f,
                    center = Offset(centerX, centerY),
                )
                canvas.drawCircle(
                    color = fillColor,
                    radius = r,
                    center = Offset(centerX, centerY),
                )
            }

            SeriesMarkerShape.SQUARE -> {
                val topLeft = Offset(centerX - r, centerY - r)
                val size = Size(r * 2f, r * 2f)
                canvas.drawRect(color = fillColor, topLeft = topLeft, size = size, style = Fill)
                canvas.drawRect(
                    color = surfacePunchColor,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = outlineStroke * 0.7f),
                )
            }

            SeriesMarkerShape.TRIANGLE -> {
                val heightOffset = r * 1.15f
                shapePath.reset()
                shapePath.moveTo(centerX, centerY - heightOffset)
                shapePath.lineTo(centerX + r * 1.1f, centerY + r * 0.9f)
                shapePath.lineTo(centerX - r * 1.1f, centerY + r * 0.9f)
                shapePath.close()
                canvas.drawPath(path = shapePath, color = fillColor, style = Fill)
                canvas.drawPath(
                    path = shapePath,
                    color = surfacePunchColor,
                    style = Stroke(width = outlineStroke * 0.7f),
                )
            }

            SeriesMarkerShape.DIAMOND -> {
                val d = r * 1.25f
                shapePath.reset()
                shapePath.moveTo(centerX, centerY - d)
                shapePath.lineTo(centerX + d, centerY)
                shapePath.lineTo(centerX, centerY + d)
                shapePath.lineTo(centerX - d, centerY)
                shapePath.close()
                canvas.drawPath(path = shapePath, color = fillColor, style = Fill)
                canvas.drawPath(
                    path = shapePath,
                    color = surfacePunchColor,
                    style = Stroke(width = outlineStroke * 0.7f),
                )
            }
        }
    }
}
