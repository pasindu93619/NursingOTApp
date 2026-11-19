package com.pasindu.nursingotapp.statgraph.chart.toolkit

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb

/**
 * Five distinct, grayscale-safe vector fill patterns for the Journal B&W palette:
 * 1. [SOLID_BLACK]
 * 2. [DIAGONAL_HATCH] (45-degree diagonal hatch)
 * 3. [DOTS] (regular dot matrix)
 * 4. [CROSS_HATCH] (45-degree + -45-degree cross-hatch)
 * 5. [WHITE_OUTLINE] (white fill with crisp black border)
 *
 * Pattern-reuse strategy for > 5 series: cycles cleanly modulo 5 via [patternForSeries]
 * while pairing with distinct [SeriesMarkerShape] and [SeriesStrokePattern] tokens.
 */
enum class JournalBwFillPattern(val displayName: String) {
    SOLID_BLACK("Solid black"),
    DIAGONAL_HATCH("45° diagonal hatch"),
    DOTS("Dots"),
    CROSS_HATCH("Cross-hatch"),
    WHITE_OUTLINE("White with black outline"),
}

enum class JournalBwOutlineStyle(val displayName: String) {
    SOLID_THIN("Standard outline"),
    DOUBLE_FRAMED("Double-framed outline"),
}

/**
 * Combined Journal B&W visual identity for bar marks and legend swatches.
 * Supports up to [JournalBwPatternPainter.MAX_DISTINGUISHABLE_BW_BAR_SERIES] (10)
 * distinguishable grayscale series (5 fill patterns × 2 outline styles).
 */
data class JournalBwBarIdentity(
    val fillPattern: JournalBwFillPattern,
    val outlineStyle: JournalBwOutlineStyle,
)

object JournalBwPatternPainter {

    const val MAX_DISTINGUISHABLE_BW_BAR_SERIES: Int = 10

    val InkBlack: Color = JournalBwPalette.Black
    val PaperWhite: Color = Color.White

    fun patternForSeries(seriesIndex: Int): JournalBwFillPattern {
        val patterns = JournalBwFillPattern.entries
        val safe = ((seriesIndex % patterns.size) + patterns.size) % patterns.size
        return patterns[safe]
    }

    /**
     * Returns a multi-attribute B&W identity combining [JournalBwFillPattern] and
     * [JournalBwOutlineStyle]. Series 0..4 use the 5 base patterns with [JournalBwOutlineStyle.SOLID_THIN];
     * series 5..9 pair those 5 patterns with [JournalBwOutlineStyle.DOUBLE_FRAMED].
     * Above 10 series, identities cycle modulo 10.
     */
    fun barIdentityForSeries(seriesIndex: Int): JournalBwBarIdentity {
        val safe = ((seriesIndex % MAX_DISTINGUISHABLE_BW_BAR_SERIES) + MAX_DISTINGUISHABLE_BW_BAR_SERIES) %
            MAX_DISTINGUISHABLE_BW_BAR_SERIES
        val fill = patternForSeries(safe)
        val outline = if (safe < JournalBwFillPattern.entries.size) {
            JournalBwOutlineStyle.SOLID_THIN
        } else {
            JournalBwOutlineStyle.DOUBLE_FRAMED
        }
        return JournalBwBarIdentity(fillPattern = fill, outlineStyle = outline)
    }

    fun drawPatternedRectCompose(
        drawScope: DrawScope,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        pattern: JournalBwFillPattern,
        strokeWidthPx: Float,
        cornerRadiusPx: Float = 0f,
        outlineStyle: JournalBwOutlineStyle = JournalBwOutlineStyle.SOLID_THIN,
    ) {
        if (width <= 0f || height <= 0f) return
        val topLeft = Offset(left, top)
        val size = Size(width, height)
        val cr = CornerRadius(cornerRadiusPx, cornerRadiusPx)

        if (pattern == JournalBwFillPattern.SOLID_BLACK) {
            if (cornerRadiusPx > 0f) {
                drawScope.drawRoundRect(color = InkBlack, topLeft = topLeft, size = size, cornerRadius = cr)
            } else {
                drawScope.drawRect(color = InkBlack, topLeft = topLeft, size = size)
            }
            if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED &&
                width >= strokeWidthPx * 4f &&
                height >= strokeWidthPx * 4f
            ) {
                val inset = strokeWidthPx * 1.35f
                val innerCr = (cornerRadiusPx - inset).coerceAtLeast(0f)
                drawScope.drawRoundRect(
                    color = PaperWhite,
                    topLeft = Offset(left + inset, top + inset),
                    size = Size(width - inset * 2f, height - inset * 2f),
                    cornerRadius = CornerRadius(innerCr, innerCr),
                    style = Stroke(width = (strokeWidthPx * 0.85f).coerceAtLeast(1f)),
                )
            }
            return
        }

        // White paper base
        if (cornerRadiusPx > 0f) {
            drawScope.drawRoundRect(color = PaperWhite, topLeft = topLeft, size = size, cornerRadius = cr)
        } else {
            drawScope.drawRect(color = PaperWhite, topLeft = topLeft, size = size)
        }

        val clipShape = Path().apply {
            if (cornerRadiusPx > 0f) {
                addRoundRect(RoundRect(Rect(left, top, left + width, top + height), cr))
            } else {
                addRect(Rect(left, top, left + width, top + height))
            }
        }

        val step = (strokeWidthPx * 4.5f).coerceIn(5f, 14f)
        val patternStroke = (strokeWidthPx * 0.9f).coerceAtLeast(1f)

        drawScope.clipPath(clipShape) {
            when (pattern) {
                JournalBwFillPattern.SOLID_BLACK,
                JournalBwFillPattern.WHITE_OUTLINE -> Unit

                JournalBwFillPattern.DIAGONAL_HATCH -> {
                    val maxSpan = width + height
                    var offset = -height
                    while (offset <= maxSpan) {
                        drawLine(
                            color = InkBlack,
                            start = Offset(left + offset, top + height),
                            end = Offset(left + offset + height, top),
                            strokeWidth = patternStroke,
                        )
                        offset += step
                    }
                }

                JournalBwFillPattern.CROSS_HATCH -> {
                    val maxSpan = width + height
                    var offset = -height
                    while (offset <= maxSpan) {
                        drawLine(
                            color = InkBlack,
                            start = Offset(left + offset, top + height),
                            end = Offset(left + offset + height, top),
                            strokeWidth = patternStroke,
                        )
                        drawLine(
                            color = InkBlack,
                            start = Offset(left + offset, top),
                            end = Offset(left + offset + height, top + height),
                            strokeWidth = patternStroke,
                        )
                        offset += step * 1.15f
                    }
                }

                JournalBwFillPattern.DOTS -> {
                    val dotStep = (strokeWidthPx * 4.0f).coerceIn(4.5f, 12f)
                    val dotRadius = (patternStroke * 0.95f).coerceIn(1.1f, 3.0f)
                    var row = 0
                    var y = top + dotStep * 0.5f
                    while (y < top + height) {
                        val xOffset = if (row % 2 == 1) dotStep * 0.5f else 0f
                        var x = left + dotStep * 0.4f + xOffset
                        while (x < left + width) {
                            drawCircle(
                                color = InkBlack,
                                radius = dotRadius,
                                center = Offset(x, y),
                            )
                            x += dotStep
                        }
                        y += dotStep
                        row += 1
                    }
                }
            }
        }

        // Crisp black border on all non-solid patterns (heavy + inner frame for series 5..9)
        val effectiveStroke = if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED) {
            strokeWidthPx * 2.0f
        } else {
            strokeWidthPx
        }
        if (cornerRadiusPx > 0f) {
            drawScope.drawRoundRect(
                color = InkBlack,
                topLeft = topLeft,
                size = size,
                cornerRadius = cr,
                style = Stroke(width = effectiveStroke),
            )
        } else {
            drawScope.drawRect(
                color = InkBlack,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = effectiveStroke),
            )
        }
        if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED &&
            width >= strokeWidthPx * 6f &&
            height >= strokeWidthPx * 6f
        ) {
            val inset = strokeWidthPx * 2.8f
            val innerCr = (cornerRadiusPx - inset).coerceAtLeast(0f)
            drawScope.drawRoundRect(
                color = InkBlack,
                topLeft = Offset(left + inset, top + inset),
                size = Size(width - inset * 2f, height - inset * 2f),
                cornerRadius = CornerRadius(innerCr, innerCr),
                style = Stroke(width = (strokeWidthPx * 0.75f).coerceAtLeast(0.8f)),
            )
        }
    }

    fun drawPatternedRectAndroid(
        canvas: AndroidCanvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        pattern: JournalBwFillPattern,
        strokeWidthPx: Float,
        outlineStyle: JournalBwOutlineStyle = JournalBwOutlineStyle.SOLID_THIN,
    ) {
        val width = right - left
        val height = bottom - top
        if (width <= 0f || height <= 0f) return

        val inkArgb = InkBlack.toArgb()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (pattern == JournalBwFillPattern.SOLID_BLACK) inkArgb else android.graphics.Color.WHITE
        }
        canvas.drawRect(left, top, right, bottom, fillPaint)
        if (pattern == JournalBwFillPattern.SOLID_BLACK) {
            if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED &&
                width >= strokeWidthPx * 4f &&
                height >= strokeWidthPx * 4f
            ) {
                val inset = strokeWidthPx * 1.35f
                val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = android.graphics.Color.WHITE
                    strokeWidth = (strokeWidthPx * 0.85f).coerceAtLeast(1f)
                }
                canvas.drawRect(left + inset, top + inset, right - inset, bottom - inset, innerPaint)
            }
            return
        }

        val step = (strokeWidthPx * 4.5f).coerceIn(5f, 14f)
        val patternStroke = (strokeWidthPx * 0.9f).coerceAtLeast(1f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = inkArgb
            strokeWidth = patternStroke
        }

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        when (pattern) {
            JournalBwFillPattern.SOLID_BLACK,
            JournalBwFillPattern.WHITE_OUTLINE -> Unit

            JournalBwFillPattern.DIAGONAL_HATCH -> {
                val maxSpan = width + height
                var offset = -height
                while (offset <= maxSpan) {
                    canvas.drawLine(
                        left + offset,
                        top + height,
                        left + offset + height,
                        top,
                        linePaint,
                    )
                    offset += step
                }
            }

            JournalBwFillPattern.CROSS_HATCH -> {
                val maxSpan = width + height
                var offset = -height
                while (offset <= maxSpan) {
                    canvas.drawLine(
                        left + offset,
                        top + height,
                        left + offset + height,
                        top,
                        linePaint,
                    )
                    canvas.drawLine(
                        left + offset,
                        top,
                        left + offset + height,
                        top + height,
                        linePaint,
                    )
                    offset += step * 1.15f
                }
            }

            JournalBwFillPattern.DOTS -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = inkArgb
                }
                val dotStep = (strokeWidthPx * 4.0f).coerceIn(4.5f, 12f)
                val dotRadius = (patternStroke * 0.95f).coerceIn(1.1f, 3.0f)
                var row = 0
                var y = top + dotStep * 0.5f
                while (y < bottom) {
                    val xOffset = if (row % 2 == 1) dotStep * 0.5f else 0f
                    var x = left + dotStep * 0.4f + xOffset
                    while (x < right) {
                        canvas.drawCircle(x, y, dotRadius, dotPaint)
                        x += dotStep
                    }
                    y += dotStep
                    row += 1
                }
            }
        }
        canvas.restore()

        val effectiveStroke = if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED) {
            strokeWidthPx * 2.0f
        } else {
            strokeWidthPx
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = inkArgb
            strokeWidth = effectiveStroke
        }
        canvas.drawRect(left, top, right, bottom, borderPaint)
        if (outlineStyle == JournalBwOutlineStyle.DOUBLE_FRAMED &&
            width >= strokeWidthPx * 6f &&
            height >= strokeWidthPx * 6f
        ) {
            val inset = strokeWidthPx * 2.8f
            val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = inkArgb
                strokeWidth = (strokeWidthPx * 0.75f).coerceAtLeast(0.8f)
            }
            canvas.drawRect(left + inset, top + inset, right - inset, bottom - inset, innerBorderPaint)
        }
    }
}
