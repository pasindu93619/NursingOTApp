package com.pasindu.nursingotapp.statgraph.export

import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartKind
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import java.util.Locale

enum class StatGraphExportFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
) {
    PDF(displayName = "PDF (A4)", extension = "pdf", mimeType = "application/pdf"),
    PNG(displayName = "PNG", extension = "png", mimeType = "image/png"),
    JPG(displayName = "JPG", extension = "jpg", mimeType = "image/jpeg"),
    CSV(displayName = "CSV", extension = "csv", mimeType = "text/csv"),
}

enum class PdfOrientation(
    val displayName: String,
    val pageWidthPt: Int,
    val pageHeightPt: Int,
) {
    PORTRAIT(displayName = "Portrait", pageWidthPt = 595, pageHeightPt = 842),
    LANDSCAPE(displayName = "Landscape", pageWidthPt = 842, pageHeightPt = 595),
}

enum class ImageExportScale(
    val label: String,
    val multiplier: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    STANDARD_1X(label = "Standard (1×)", multiplier = 1, widthPx = 1200, heightPx = 760),
    HIGH_2X(label = "High (2×)", multiplier = 2, widthPx = 2400, heightPx = 1520),
    PRINT_3X(label = "Print (3×)", multiplier = 3, widthPx = 3600, heightPx = 2280),
}

enum class ImageBackgroundMode(val displayName: String) {
    WHITE("White"),
    TRANSPARENT("Transparent"),
}

data class StatGraphExportOptions(
    val format: StatGraphExportFormat = StatGraphExportFormat.PDF,
    val pdfOrientation: PdfOrientation = PdfOrientation.PORTRAIT,
    val autoSelectOrientation: Boolean = true,
    val allowMultiPagePanels: Boolean = true,
    val includeDataTable: Boolean = true,
    val customNotes: String = "",
    val imageScale: ImageExportScale = ImageExportScale.HIGH_2X,
    val imageBackground: ImageBackgroundMode = ImageBackgroundMode.WHITE,
)

data class ExportDataTable(
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class StatGraphExportPayload(
    val renderData: ChartRenderData,
    val chartStyle: ChartStyle,
    val howToRead: String,
    val plainWhy: String,
    val sourceLabel: String,
    val clinicalNotes: List<String>,
    val exportDateText: String,
    val isOrderedTimeSeries: Boolean = false,
)

sealed class StatGraphExportUiState {
    data object Idle : StatGraphExportUiState()
    data object Exporting : StatGraphExportUiState()
    data class Success(
        val format: StatGraphExportFormat,
        val fileName: String,
        val savedToSaf: Boolean,
        val shareableUriString: String?,
        val mimeType: String,
    ) : StatGraphExportUiState()

    data class Cancelled(val message: String = "Export cancelled.") : StatGraphExportUiState()
    data class Error(val message: String) : StatGraphExportUiState()
}

object StatGraphExportFormatter {

    const val PRIVACY_FOOTER: String = "Aggregate data only – no patient identifiers"

    /**
     * Builds the exact aggregate data table corresponding to [renderData].
     * Missing values remain blank (`""`) rather than being coerced to `"0"`.
     */
    fun buildDataTable(renderData: ChartRenderData): ExportDataTable {
        if (renderData.series.isEmpty()) {
            return ExportDataTable(headers = emptyList(), rows = emptyList())
        }

        if (renderData.chartKind == ChartKind.SCATTER) {
            val xLabel = renderData.domainAxisTitle.ifBlank { "X" }
            val headers = mutableListOf("Observation", xLabel)
            renderData.series.forEach { s ->
                headers += if (s.unit.isNotBlank()) "${s.name} (${s.unit})" else s.name
            }
            val maxPts = renderData.series.maxOfOrNull { it.points.size } ?: 0
            val rows = (0 until maxPts).map { idx ->
                val firstPt = renderData.series.firstOrNull()?.points?.getOrNull(idx)
                val row = mutableListOf(
                    firstPt?.domainLabel?.ifBlank { "Row ${idx + 1}" } ?: "Row ${idx + 1}",
                    formatNumberOrBlank(firstPt?.secondaryNumericValue),
                )
                renderData.series.forEach { s ->
                    row += formatNumberOrBlank(s.points.getOrNull(idx)?.numericValue)
                }
                row
            }
            return ExportDataTable(headers = headers, rows = rows)
        }

        val domainHeader = renderData.domainAxisTitle.ifBlank { "Category / Period" }
        val headers = mutableListOf(domainHeader)
        val hasBoundsPerSeries = renderData.series.map { series ->
            series.points.any { it.lowerLimit != null && it.upperLimit != null }
        }
        renderData.series.forEachIndexed { idx, s ->
            headers += if (s.unit.isNotBlank()) "${s.name} (${s.unit})" else s.name
            if (hasBoundsPerSeries[idx]) {
                headers += "${s.name} Lower CI"
                headers += "${s.name} Upper CI"
            }
        }

        val pointsBySeries = renderData.series.map { s ->
            s.points.associateBy { it.domainIndex }
        }
        val rowCount = renderData.domainLabels.size
        val rows = (0 until rowCount).map { domainIdx ->
            val row = mutableListOf(renderData.domainLabels[domainIdx])
            renderData.series.forEachIndexed { sIdx, _ ->
                val pt = pointsBySeries[sIdx][domainIdx]
                row += formatNumberOrBlank(pt?.numericValue)
                if (hasBoundsPerSeries[sIdx]) {
                    row += formatNumberOrBlank(pt?.lowerLimit)
                    row += formatNumberOrBlank(pt?.upperLimit)
                }
            }
            row
        }
        return ExportDataTable(headers = headers, rows = rows)
    }

    /**
     * Serializes [table] using RFC 4180 CSV escaping.
     */
    fun toCsvString(table: ExportDataTable): String {
        if (table.headers.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append(table.headers.joinToString(",") { escapeCsvCell(it) }).append("\r\n")
        table.rows.forEach { row ->
            sb.append(row.joinToString(",") { escapeCsvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun escapeCsvCell(raw: String): String {
        val needsQuotes = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        return if (!needsQuotes) {
            raw
        } else {
            "\"" + raw.replace("\"", "\"\"") + "\""
        }
    }

    fun sanitizeFilename(
        chartTitle: String,
        format: StatGraphExportFormat,
    ): String {
        val cleaned = chartTitle
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .take(48)
        val base = cleaned.ifBlank { "statgraph_chart" }
        return "$base.${format.extension}"
    }

    internal fun formatNumberOrBlank(value: Double?): String {
        if (value == null || !value.isFinite()) return ""
        return if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1_000_000_000.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }
}
