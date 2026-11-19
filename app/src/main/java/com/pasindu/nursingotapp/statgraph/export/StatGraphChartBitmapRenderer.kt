package com.pasindu.nursingotapp.statgraph.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AdaptiveChartFamily
import com.pasindu.nursingotapp.statgraph.chart.toolkit.AxisLabelRenderer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.BarDensityPolicy
import com.pasindu.nursingotapp.statgraph.chart.toolkit.CategoryAbbreviationEntry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartExportLayoutPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartReadabilityAnalyzer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRect
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.JournalBwPatternPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendEntryInput
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendLayoutPlanner
import com.pasindu.nursingotapp.statgraph.chart.toolkit.LegendTextMeasurer
import com.pasindu.nursingotapp.statgraph.chart.toolkit.SeriesMarkerShape
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.AxisAndGridPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ChartInteractionState
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SeriesMarkersPainter
import com.pasindu.nursingotapp.statgraph.orchestration.ChartPainterResolver
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

data class RenderedChartBitmapResult(
    val bitmap: Bitmap,
    val abbreviationKey: List<CategoryAbbreviationEntry>,
)

/**
 * Renders [ChartRenderData] + [ChartStyle] offscreen using the exact same [ChartPainterResolver]
 * and [InteractiveChartPainter] implementations as [StatGraphChartHost], with Sinhala font support.
 */
object StatGraphChartBitmapRenderer {

    fun loadSinhalaTypeface(context: Context): Typeface? =
        runCatching { ResourcesCompat.getFont(context, R.font.notosassinhala) }.getOrNull()

    fun sinhalaFontFamily(): FontFamily = FontFamily(Font(R.font.notosassinhala))

    fun containsSinhala(text: String): Boolean = text.any { ch -> ch in '\u0D80'..'\u0DFF' }

    fun selectTypefaceForText(
        text: String,
        sinhalaTypeface: Typeface?,
        bold: Boolean = false,
    ): Typeface {
        val styleFlag = if (bold) Typeface.BOLD else Typeface.NORMAL
        return if (containsSinhala(text) && sinhalaTypeface != null) {
            Typeface.create(sinhalaTypeface, styleFlag)
        } else {
            Typeface.create(Typeface.SANS_SERIF, styleFlag)
        }
    }

    /**
     * Renders the chart (optionally with title/subtitle and series legend) to an ARGB_8888 [Bitmap]
     * at the requested pixel dimensions using flat fills (`progress = 1f`).
     */
    fun renderChartBitmap(
        context: Context,
        data: ChartRenderData,
        style: ChartStyle,
        widthPx: Int,
        heightPx: Int,
        backgroundMode: ImageBackgroundMode = ImageBackgroundMode.WHITE,
        includeTitle: Boolean = true,
        includeLegend: Boolean = true,
        logicalWidthDp: Float? = null,
        includeReferenceKeyInBitmap: Boolean = false,
    ): Bitmap = renderChartBitmapWithMeta(
        context = context,
        data = data,
        style = style,
        widthPx = widthPx,
        heightPx = heightPx,
        backgroundMode = backgroundMode,
        includeTitle = includeTitle,
        includeLegend = includeLegend,
        logicalWidthDp = logicalWidthDp,
        includeReferenceKeyInBitmap = includeReferenceKeyInBitmap,
    ).bitmap

    fun renderChartBitmapWithMeta(
        context: Context,
        data: ChartRenderData,
        style: ChartStyle,
        widthPx: Int,
        heightPx: Int,
        backgroundMode: ImageBackgroundMode = ImageBackgroundMode.WHITE,
        includeTitle: Boolean = true,
        includeLegend: Boolean = true,
        logicalWidthDp: Float? = null,
        includeReferenceKeyInBitmap: Boolean = false,
    ): RenderedChartBitmapResult {
        val safeW = widthPx.coerceAtLeast(320)
        val safeH = heightPx.coerceAtLeast(220)
        val targetLogicalW = (logicalWidthDp ?: 600f).coerceIn(320f, 1200f)
        val scaleFactor = (safeW / targetLogicalW).coerceIn(1f, 6f)

        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val androidCanvas = AndroidCanvas(bitmap)
        if (backgroundMode == ImageBackgroundMode.WHITE) {
            androidCanvas.drawColor(android.graphics.Color.WHITE)
        }

        val sinhalaTypeface = loadSinhalaTypeface(context) ?: Typeface.DEFAULT
        val textColorArgb = if (backgroundMode == ImageBackgroundMode.WHITE) {
            Color(0xFF0F172A).toArgb()
        } else {
            style.titleColor.toArgb()
        }
        val subtitleColorArgb = if (backgroundMode == ImageBackgroundMode.WHITE) {
            Color(0xFF475569).toArgb()
        } else {
            style.subtitleColor.toArgb()
        }

        var topOffsetPx = (16f * scaleFactor)
        val sidePadPx = (24f * scaleFactor)
        val contentWidthPx = (safeW - 2f * sidePadPx).toInt().coerceAtLeast(100)

        if (includeTitle) {
            if (data.title.isNotBlank()) {
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColorArgb
                    textSize = 18f * scaleFactor
                    typeface = selectTypefaceForText(data.title, sinhalaTypeface, bold = true)
                }
                val layout = StaticLayout.Builder
                    .obtain(data.title, 0, data.title.length, titlePaint, contentWidthPx)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                androidCanvas.save()
                androidCanvas.translate(sidePadPx, topOffsetPx)
                layout.draw(androidCanvas)
                androidCanvas.restore()
                topOffsetPx += layout.height + 4f * scaleFactor
            }

            if (data.subtitle.isNotBlank()) {
                val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = subtitleColorArgb
                    textSize = 12f * scaleFactor
                    typeface = selectTypefaceForText(data.subtitle, sinhalaTypeface, bold = false)
                }
                val layout = StaticLayout.Builder
                    .obtain(data.subtitle, 0, data.subtitle.length, subPaint, contentWidthPx)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                androidCanvas.save()
                androidCanvas.translate(sidePadPx, topOffsetPx)
                layout.draw(androidCanvas)
                androidCanvas.restore()
                topOffsetPx += layout.height + 8f * scaleFactor
            }
        }

        if (includeLegend && data.series.size > 1) {
            topOffsetPx = drawSeriesLegend(
                canvas = androidCanvas,
                data = data,
                style = style,
                sinhalaTypeface = sinhalaTypeface,
                textColorArgb = textColorArgb,
                startX = sidePadPx,
                startY = topOffsetPx,
                maxWidth = contentWidthPx.toFloat(),
                scaleFactor = scaleFactor,
            )
            topOffsetPx += 8f * scaleFactor
        }

        // Draw the exact InteractiveChartPainter onto the remaining plot area
        val chartTop = topOffsetPx
        val density = Density(density = scaleFactor, fontScale = 1f)
        val fontFamilyResolver = createFontFamilyResolver(context)
        val textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = fontFamilyResolver,
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
        val hasAnySinhala = containsSinhala(data.title) ||
            containsSinhala(data.subtitle) ||
            containsSinhala(data.domainAxisTitle) ||
            containsSinhala(data.valueAxisTitle) ||
            data.domainLabels.any { containsSinhala(it) } ||
            data.series.any { containsSinhala(it.name) }
        val activeFontFamily = if (hasAnySinhala) sinhalaFontFamily() else FontFamily.SansSerif
        val exportStyle = style.copy(
            cardBackground = if (backgroundMode == ImageBackgroundMode.WHITE) Color.White else Color.Transparent,
            plotBackground = if (backgroundMode == ImageBackgroundMode.WHITE) Color.White else Color.Transparent,
            axisLabelStyle = style.axisLabelStyle.copy(fontFamily = activeFontFamily),
            titleStyle = style.titleStyle.copy(fontFamily = activeFontFamily),
            subtitleStyle = style.subtitleStyle.copy(fontFamily = activeFontFamily),
            legendLabelStyle = style.legendLabelStyle.copy(fontFamily = activeFontFamily),
        )

        val family = ChartReadabilityAnalyzer.classifyFamily(data.chartKind)
        val preliminaryChartHeight = (safeH - chartTop - 12f * scaleFactor).coerceAtLeast(120f)
        val preliminaryRect = ChartRect(
            left = sidePadPx * 0.5f,
            top = chartTop,
            right = safeW - sidePadPx * 0.5f,
            bottom = chartTop + preliminaryChartHeight,
        )
        val exportBaseInsets = com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartInsets(
            left = 48f * scaleFactor,
            top = 16f * scaleFactor,
            right = 18f * scaleFactor,
            bottom = 42f * scaleFactor,
        )
        val abbreviationKey: List<CategoryAbbreviationEntry> = if (
            family != AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING &&
            family != AdaptiveChartFamily.PART_TO_WHOLE_RADIAL_OR_GRID &&
            family != AdaptiveChartFamily.HIERARCHICAL_TREEMAP_SUNBURST_MOSAIC &&
            data.domainLabels.isNotEmpty()
        ) {
            AxisLabelRenderer.resolveTwoPassGeometry(
                outerRect = preliminaryRect,
                data = data,
                baseInsets = exportBaseInsets,
                style = exportStyle,
                density = density,
                textMeasurer = textMeasurer,
            ).xLabelPlan?.abbreviationKey.orEmpty()
        } else {
            emptyList()
        }

        val keyBlockHeightPx = if (includeReferenceKeyInBitmap && abbreviationKey.isNotEmpty()) {
            val cols = 3
            val rows = ceil(abbreviationKey.size.toDouble() / cols.toDouble()).toInt()
            (28f * scaleFactor) + rows * (16f * scaleFactor) + (12f * scaleFactor)
        } else {
            0f
        }
        val chartHeight = (safeH - chartTop - keyBlockHeightPx - 12f * scaleFactor).coerceAtLeast(120f)

        val axisAndGridPainter = AxisAndGridPainter(textMeasurer)
        val markersPainter = SeriesMarkersPainter()
        val painter = ChartPainterResolver.createPainter(
            chartKind = data.chartKind,
            axisAndGridPainter = axisAndGridPainter,
            markersPainter = markersPainter,
        )

        val composeCanvas = Canvas(bitmap.asImageBitmap())
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(safeW.toFloat(), safeH.toFloat()),
        ) {
            painter.draw(
                canvas = this,
                rect = ChartRect(
                    left = sidePadPx * 0.5f,
                    top = chartTop,
                    right = safeW - sidePadPx * 0.5f,
                    bottom = chartTop + chartHeight,
                ),
                data = data,
                style = exportStyle,
                interaction = ChartInteractionState(),
                progress = 1f,
            )
        }

        if (includeReferenceKeyInBitmap && abbreviationKey.isNotEmpty()) {
            drawCategoryReferenceKeyOnBitmap(
                canvas = androidCanvas,
                abbreviationKey = abbreviationKey,
                sinhalaTypeface = sinhalaTypeface,
                textColorArgb = textColorArgb,
                subtitleColorArgb = subtitleColorArgb,
                startX = sidePadPx,
                startY = chartTop + chartHeight + 4f * scaleFactor,
                contentWidth = contentWidthPx.toFloat(),
                scaleFactor = scaleFactor,
            )
        }

        return RenderedChartBitmapResult(
            bitmap = bitmap,
            abbreviationKey = abbreviationKey,
        )
    }

    fun renderImageBytes(
        context: Context,
        data: ChartRenderData,
        style: ChartStyle,
        format: StatGraphExportFormat,
        scale: ImageExportScale,
        backgroundMode: ImageBackgroundMode,
    ): ByteArray {
        val effectiveBg = if (format == StatGraphExportFormat.JPG) {
            ImageBackgroundMode.WHITE
        } else {
            backgroundMode
        }
        val (targetWidthPx, targetHeightPx) = ChartExportLayoutPlanner.planImageExportDimensions(
            data = data,
            scale = scale,
        )
        val bitmap = renderChartBitmap(
            context = context,
            data = data,
            style = style,
            widthPx = targetWidthPx,
            heightPx = targetHeightPx,
            backgroundMode = effectiveBg,
            includeTitle = true,
            includeLegend = true,
            includeReferenceKeyInBitmap = true,
        )
        val compressFormat = if (format == StatGraphExportFormat.JPG) {
            Bitmap.CompressFormat.JPEG
        } else {
            Bitmap.CompressFormat.PNG
        }
        val quality = if (format == StatGraphExportFormat.JPG) 95 else 100
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(compressFormat, quality, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun drawCategoryReferenceKeyOnBitmap(
        canvas: AndroidCanvas,
        abbreviationKey: List<CategoryAbbreviationEntry>,
        sinhalaTypeface: Typeface?,
        textColorArgb: Int,
        subtitleColorArgb: Int,
        startX: Float,
        startY: Float,
        contentWidth: Float,
        scaleFactor: Float,
    ) {
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorArgb
            textSize = 11f * scaleFactor
            typeface = Typeface.create(sinhalaTypeface ?: Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorArgb
            textSize = 10f * scaleFactor
            typeface = Typeface.create(sinhalaTypeface ?: Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val fullPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subtitleColorArgb
            textSize = 10f * scaleFactor
            typeface = Typeface.create(sinhalaTypeface ?: Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            "Category Reference Key (${abbreviationKey.size} categories)",
            startX,
            startY + 14f * scaleFactor,
            headingPaint,
        )
        val cols = 3
        val colWidth = contentWidth / cols
        val rowHeight = 15f * scaleFactor
        val baseTop = startY + 28f * scaleFactor
        abbreviationKey.forEachIndexed { idx, entry ->
            val col = idx % cols
            val row = idx / cols
            val cellX = startX + col * colWidth
            val cellY = baseTop + row * rowHeight
            val codeStr = "${entry.shortCode}: "
            canvas.drawText(codeStr, cellX, cellY, codePaint)
            val codeW = codePaint.measureText(codeStr)
            val maxNameW = (colWidth - codeW - 8f * scaleFactor).coerceAtLeast(20f)
            var displayFull = entry.fullCategory
            while (displayFull.length > 3 && fullPaint.measureText(displayFull) > maxNameW) {
                displayFull = displayFull.dropLast(2) + "…"
            }
            canvas.drawText(displayFull, cellX + codeW, cellY, fullPaint)
        }
    }

    private fun drawSeriesLegend(
        canvas: AndroidCanvas,
        data: ChartRenderData,
        style: ChartStyle,
        sinhalaTypeface: Typeface?,
        textColorArgb: Int,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        scaleFactor: Float,
    ): Float {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorArgb
            textSize = 11f * scaleFactor
        }
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
        }
        val swatchSize = 10f * scaleFactor
        val swatchGap = 6f * scaleFactor
        val itemGap = 14f * scaleFactor
        val rowGap = 6f * scaleFactor
        val isBarChart = BarDensityPolicy.isBarChartKind(data.chartKind)

        val entries = data.series.mapIndexed { idx, s ->
            LegendEntryInput(
                seriesIndex = idx,
                name = s.name,
                unit = s.unit,
                colorIndex = s.colorIndex,
                markerShape = s.markerShape,
            )
        }
        val measurer = object : LegendTextMeasurer {
            override fun measureWidth(text: String): Float {
                textPaint.typeface = selectTypefaceForText(text, sinhalaTypeface)
                return textPaint.measureText(text)
            }

            override fun lineHeight(): Float {
                val fm = textPaint.fontMetrics
                return (fm.descent - fm.ascent).coerceAtLeast(swatchSize)
            }
        }
        val plan = LegendLayoutPlanner.plan(
            entries = entries,
            availableWidthPx = maxWidth,
            measurer = measurer,
            swatchWidthPx = swatchSize,
            swatchGapPx = swatchGap,
            horizontalItemGapPx = itemGap,
            verticalRowGapPx = rowGap,
        )

        val baselineOffset = -textPaint.fontMetrics.ascent
        plan.items.forEach { placed ->
            val itemX = startX + placed.xPx
            val itemY = startY + placed.yPx
            val series = placed.entry
            if (style.palette == StatGraphChartPalette.JOURNAL_BW && isBarChart) {
                val swatchTop = itemY + (placed.heightPx - swatchSize) * 0.5f
                val identity = JournalBwPatternPainter.barIdentityForSeries(series.colorIndex)
                JournalBwPatternPainter.drawPatternedRectAndroid(
                    canvas = canvas,
                    left = itemX,
                    top = swatchTop,
                    right = itemX + swatchSize,
                    bottom = swatchTop + swatchSize,
                    pattern = identity.fillPattern,
                    strokeWidthPx = 1f * scaleFactor,
                    outlineStyle = identity.outlineStyle,
                )
            } else {
                markerPaint.color = style.seriesColor(series.colorIndex).toArgb()
                val cx = itemX + swatchSize * 0.5f
                val cy = itemY + placed.heightPx * 0.5f
                val r = swatchSize * 0.45f
                when (series.markerShape) {
                    SeriesMarkerShape.CIRCLE -> canvas.drawCircle(cx, cy, r, markerPaint)
                    SeriesMarkerShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, markerPaint)
                    SeriesMarkerShape.DIAMOND -> {
                        val p = android.graphics.Path().apply {
                            moveTo(cx, cy - r * 1.15f)
                            lineTo(cx + r * 1.15f, cy)
                            lineTo(cx, cy + r * 1.15f)
                            lineTo(cx - r * 1.15f, cy)
                            close()
                        }
                        canvas.drawPath(p, markerPaint)
                    }
                    SeriesMarkerShape.TRIANGLE -> {
                        val p = android.graphics.Path().apply {
                            moveTo(cx, cy - r * 1.15f)
                            lineTo(cx + r * 1.1f, cy + r)
                            lineTo(cx - r * 1.1f, cy + r)
                            close()
                        }
                        canvas.drawPath(p, markerPaint)
                    }
                }
            }

            textPaint.typeface = selectTypefaceForText(placed.displayLabel, sinhalaTypeface)
            val textY = itemY + (placed.heightPx - (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent)) * 0.5f + baselineOffset
            canvas.drawText(placed.displayLabel, itemX + swatchSize + swatchGap, textY, textPaint)
        }
        return startY + plan.totalHeightPx
    }
}
