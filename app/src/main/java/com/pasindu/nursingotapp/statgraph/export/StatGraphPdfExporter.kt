package com.pasindu.nursingotapp.statgraph.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.pasindu.nursingotapp.statgraph.chart.toolkit.CategoryAbbreviationEntry
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartExportLayoutPlanner
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

/**
 * Generates multi-page A4 PDF reports using Android [PdfDocument], embedding the exact
 * [StatGraphChartBitmapRenderer] output, Sinhala-capable font ([R.font.notosassinhala]),
 * plain-language guidance, optional notes, and paginated aggregate data tables.
 */
object StatGraphPdfExporter {

    private const val MARGIN_PT = 36f
    private const val FOOTER_HEIGHT_PT = 26f

    fun generatePdfBytes(
        context: Context,
        payload: StatGraphExportPayload,
        options: StatGraphExportOptions,
    ): ByteArray {
        val exportPlan = ChartExportLayoutPlanner.planPdfExport(
            data = payload.renderData,
            requestedOrientation = if (options.autoSelectOrientation) null else options.pdfOrientation,
            allowMultiPagePanels = options.allowMultiPagePanels,
        )
        val pageWidth = exportPlan.orientation.pageWidthPt
        val pageHeight = exportPlan.orientation.pageHeightPt
        val contentWidth = (pageWidth - 2f * MARGIN_PT).toInt()
        val maxContentBottom = pageHeight - MARGIN_PT - FOOTER_HEIGHT_PT

        val sinhalaTypeface = StatGraphChartBitmapRenderer.loadSinhalaTypeface(context) ?: Typeface.DEFAULT
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            typeface = Typeface.create(sinhalaTypeface, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            typeface = sinhalaTypeface
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            typeface = sinhalaTypeface
        }
        val sectionHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
            typeface = Typeface.create(sinhalaTypeface, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            typeface = sinhalaTypeface
        }
        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(sinhalaTypeface, Typeface.BOLD)
        }
        val tableCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 8.5f
            typeface = sinhalaTypeface
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN_PT

        fun finishCurrentPage() {
            drawFooter(canvas, pageWidth, pageHeight, pageNumber, metaPaint, borderPaint)
            document.finishPage(page)
        }

        fun startNextPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = MARGIN_PT
        }

        // 1. Title & Subtitle
        val titleText = payload.renderData.title.ifBlank { "Stat Graph Studio Chart" }
        val effectiveTitlePaint = TextPaint(titlePaint).apply {
            typeface = StatGraphChartBitmapRenderer.selectTypefaceForText(titleText, sinhalaTypeface, bold = true)
        }
        y += drawWrappedText(canvas, titleText, effectiveTitlePaint, MARGIN_PT, y, contentWidth) + 3f

        if (payload.renderData.subtitle.isNotBlank()) {
            val effectiveSubPaint = TextPaint(subtitlePaint).apply {
                typeface = StatGraphChartBitmapRenderer.selectTypefaceForText(payload.renderData.subtitle, sinhalaTypeface)
            }
            y += drawWrappedText(canvas, payload.renderData.subtitle, effectiveSubPaint, MARGIN_PT, y, contentWidth) + 3f
        }

        val metaLine = buildString {
            if (payload.sourceLabel.isNotBlank()) {
                append("Source: ").append(payload.sourceLabel).append("   •   ")
            }
            append("Exported: ").append(payload.exportDateText)
        }
        val effectiveMetaPaint = TextPaint(metaPaint).apply {
            typeface = StatGraphChartBitmapRenderer.selectTypefaceForText(metaLine, sinhalaTypeface)
        }
        y += drawWrappedText(canvas, metaLine, effectiveMetaPaint, MARGIN_PT, y, contentWidth) + 8f

        // 2. High-res chart bitmap(s) (using adaptive A4 layout plan & multi-page panels when needed)
        val targetDisplayH = exportPlan.chartDisplayHeightPt
        val collectedAbbreviationKeys = linkedMapOf<String, CategoryAbbreviationEntry>()

        exportPlan.panels.forEach { panel ->
            if (panel.panelIndex > 0) {
                startNextPage()
                val panelHeader = if (panel.panelTitleSuffix.isNotBlank()) {
                    "$titleText — ${panel.panelTitleSuffix}"
                } else {
                    "$titleText (Continued)"
                }
                y += drawWrappedText(canvas, panelHeader, effectiveTitlePaint, MARGIN_PT, y, contentWidth) + 6f
            } else if (panel.panelTitleSuffix.isNotBlank()) {
                val subPanelPaint = TextPaint(sectionHeadingPaint).apply {
                    typeface = StatGraphChartBitmapRenderer.selectTypefaceForText(panel.panelTitleSuffix, sinhalaTypeface, bold = true)
                }
                y += drawWrappedText(canvas, panel.panelTitleSuffix, subPanelPaint, MARGIN_PT, y, contentWidth) + 4f
            }

            if (y + targetDisplayH > maxContentBottom && y > MARGIN_PT + 60f) {
                startNextPage()
            }

            val rendered = StatGraphChartBitmapRenderer.renderChartBitmapWithMeta(
                context = context,
                data = panel.sliceData,
                style = payload.chartStyle,
                widthPx = exportPlan.bitmapRenderWidthPx,
                heightPx = exportPlan.bitmapRenderHeightPx,
                backgroundMode = ImageBackgroundMode.WHITE,
                includeTitle = false,
                includeLegend = true,
                logicalWidthDp = exportPlan.contentWidthPt.toFloat(),
                includeReferenceKeyInBitmap = false,
            )
            rendered.abbreviationKey.forEach { entry ->
                collectedAbbreviationKeys.putIfAbsent("${entry.shortCode}_${entry.fullCategory}", entry)
            }
            val chartBitmap = rendered.bitmap
            val destRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + targetDisplayH)
            canvas.drawBitmap(chartBitmap, Rect(0, 0, chartBitmap.width, chartBitmap.height), destRect, null)
            canvas.drawRect(destRect, borderPaint)
            chartBitmap.recycle()
            y += targetDisplayH + 10f
        }

        // 2b. Category Reference Key table when abbreviated category codes are displayed
        val activeAbbreviationKey = collectedAbbreviationKeys.values.toList()
        if (activeAbbreviationKey.isNotEmpty()) {
            val cols = if (exportPlan.orientation == PdfOrientation.LANDSCAPE) 3 else 2
            val colWidth = contentWidth.toFloat() / cols
            val rowHeight = 16f
            val totalRows = ceil(activeAbbreviationKey.size.toDouble() / cols.toDouble()).toInt()

            fun drawReferenceKeyHeader() {
                if (y + rowHeight * 2f + 16f > maxContentBottom) {
                    startNextPage()
                }
                canvas.drawText(
                    "Category Reference Key (${activeAbbreviationKey.size} categories)",
                    MARGIN_PT,
                    y + 10f,
                    sectionHeadingPaint,
                )
                y += 14f
                val headerRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + rowHeight)
                canvas.drawRect(headerRect, headerBgPaint)
                canvas.drawRect(headerRect, borderPaint)
                for (c in 0 until cols) {
                    val cLeft = MARGIN_PT + c * colWidth
                    if (c > 0) {
                        canvas.drawLine(cLeft, y, cLeft, y + rowHeight, borderPaint)
                    }
                    canvas.drawText("Code", cLeft + 4f, y + 11f, tableHeaderPaint)
                    canvas.drawText("Full Category", cLeft + 46f, y + 11f, tableHeaderPaint)
                }
                y += rowHeight
            }

            drawReferenceKeyHeader()
            for (r in 0 until totalRows) {
                if (y + rowHeight > maxContentBottom) {
                    startNextPage()
                    drawReferenceKeyHeader()
                }
                val rowRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + rowHeight)
                if (r % 2 == 1) {
                    canvas.drawRect(rowRect, zebraBgPaint)
                }
                canvas.drawRect(rowRect, borderPaint)
                for (c in 0 until cols) {
                    val cLeft = MARGIN_PT + c * colWidth
                    if (c > 0) {
                        canvas.drawLine(cLeft, y, cLeft, y + rowHeight, borderPaint)
                    }
                    val idx = r * cols + c
                    val entry = activeAbbreviationKey.getOrNull(idx) ?: continue
                    canvas.drawText(
                        truncateForCell(entry.shortCode, tableHeaderPaint, 40f),
                        cLeft + 4f,
                        y + 11f,
                        tableHeaderPaint,
                    )
                    canvas.drawText(
                        truncateForCell(entry.fullCategory, tableCellPaint, colWidth - 50f),
                        cLeft + 46f,
                        y + 11f,
                        tableCellPaint,
                    )
                }
                y += rowHeight
            }
            y += 10f
        }

        // 3. Plain-language "How to read" guidance
        val guidanceParts = listOfNotNull(
            payload.howToRead.takeIf { it.isNotBlank() },
            payload.plainWhy.takeIf { it.isNotBlank() },
        )
        if (guidanceParts.isNotEmpty()) {
            val guidanceText = guidanceParts.joinToString("\n")
            val guidanceLayout = StaticLayout.Builder
                .obtain(guidanceText, 0, guidanceText.length, bodyPaint, contentWidth - 16)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            val neededHeight = 24f + guidanceLayout.height
            if (y + neededHeight > maxContentBottom) {
                startNextPage()
            }
            val boxRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + neededHeight)
            canvas.drawRoundRect(boxRect, 6f, 6f, zebraBgPaint)
            canvas.drawRoundRect(boxRect, 6f, 6f, borderPaint)
            canvas.drawText("How to read this chart", MARGIN_PT + 8f, y + 14f, sectionHeadingPaint)
            canvas.withTranslation(MARGIN_PT + 8f, y + 20f) {
                guidanceLayout.draw(this)
            }
            y += neededHeight + 10f
        }

        // 4. Optional notes (clinical + user notes)
        val combinedNotes = buildList {
            payload.clinicalNotes.filter { it.isNotBlank() }.forEach { add("• $it") }
            if (options.customNotes.isNotBlank()) add("• Note: ${options.customNotes.trim()}")
        }
        if (combinedNotes.isNotEmpty()) {
            val notesText = combinedNotes.joinToString("\n")
            val notesLayout = StaticLayout.Builder
                .obtain(notesText, 0, notesText.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            if (y + 18f + notesLayout.height > maxContentBottom) {
                startNextPage()
            }
            canvas.drawText("Notes", MARGIN_PT, y + 10f, sectionHeadingPaint)
            y += 15f
            canvas.withTranslation(MARGIN_PT, y) {
                notesLayout.draw(this)
            }
            y += notesLayout.height + 10f
        }

        // 5. Optional paginated aggregate data table
        if (options.includeDataTable) {
            val table = StatGraphExportFormatter.buildDataTable(payload.renderData)
            if (table.headers.isNotEmpty()) {
                val colCount = table.headers.size
                val colWidth = contentWidth.toFloat() / colCount
                val rowHeight = 18f

                fun drawTableHeader() {
                    if (y + rowHeight * 2f > maxContentBottom) {
                        startNextPage()
                    }
                    canvas.drawText("Data table (aggregate values)", MARGIN_PT, y + 10f, sectionHeadingPaint)
                    y += 16f
                    val headerRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + rowHeight)
                    canvas.drawRect(headerRect, headerBgPaint)
                    canvas.drawRect(headerRect, borderPaint)
                    table.headers.forEachIndexed { colIdx, header ->
                        val cellText = truncateForCell(header, tableHeaderPaint, colWidth - 6f)
                        canvas.drawText(
                            cellText,
                            MARGIN_PT + colIdx * colWidth + 3f,
                            y + 12f,
                            tableHeaderPaint,
                        )
                    }
                    y += rowHeight
                }

                drawTableHeader()
                table.rows.forEachIndexed { rowIdx, row ->
                    if (y + rowHeight > maxContentBottom) {
                        startNextPage()
                        drawTableHeader()
                    }
                    val rowRect = RectF(MARGIN_PT, y, MARGIN_PT + contentWidth, y + rowHeight)
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(rowRect, zebraBgPaint)
                    }
                    canvas.drawRect(rowRect, borderPaint)
                    row.forEachIndexed { colIdx, cell ->
                        val cellText = truncateForCell(cell, tableCellPaint, colWidth - 6f)
                        canvas.drawText(
                            cellText,
                            MARGIN_PT + colIdx * colWidth + 3f,
                            y + 12f,
                            tableCellPaint,
                        )
                    }
                    y += rowHeight
                }
            }
        }

        finishCurrentPage()

        return ByteArrayOutputStream().use { out ->
            document.writeTo(out)
            document.close()
            out.toByteArray()
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
    ): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.withTranslation(x, y) {
            layout.draw(this)
        }
        return layout.height.toFloat()
    }

    private fun drawFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int,
        paint: TextPaint,
        borderPaint: Paint,
    ) {
        val lineY = pageHeight - MARGIN_PT - FOOTER_HEIGHT_PT + 6f
        canvas.drawLine(MARGIN_PT, lineY, pageWidth - MARGIN_PT, lineY, borderPaint)
        val textY = lineY + 14f
        canvas.drawText(StatGraphExportFormatter.PRIVACY_FOOTER, MARGIN_PT, textY, paint)
        val pageLabel = "Page $pageNumber"
        val labelW = paint.measureText(pageLabel)
        canvas.drawText(pageLabel, pageWidth - MARGIN_PT - labelW, textY, paint)
    }

    private fun truncateForCell(text: String, paint: TextPaint, maxWidth: Float): String {
        if (maxWidth <= 4f || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
