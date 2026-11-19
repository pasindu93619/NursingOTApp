package com.pasindu.nursingotapp.statgraph.chart.toolkit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.pasindu.nursingotapp.statgraph.export.ImageExportScale
import com.pasindu.nursingotapp.statgraph.export.PdfOrientation
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Unambiguous mapping entry connecting a shortened/abbreviated chart axis code
 * (e.g., `"ED"`, `"GM"`, `"GS"`) to its full original category name (`"Emergency Department"`).
 */
@Immutable
data class CategoryAbbreviationEntry(
    val domainIndex: Int,
    val shortCode: String,
    val fullLabel: String,
) {
    val fullCategory: String get() = fullLabel
}

/**
 * User-selectable override for category axis label rendering.
 */
enum class ChartLabelDisplayMode(
    val id: String,
    val displayName: String,
) {
    AUTO("auto", "Auto"),
    FULL("full", "Full labels"),
    ROTATED_45("rotated_45", "Rotated 45°"),
    ROTATED_90("rotated_90", "Vertical 90°"),
    ABBREVIATED_KEY("abbreviated_key", "Abbreviated + Key");

    companion object {
        fun fromId(id: String?): ChartLabelDisplayMode =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * User-selectable color strategy for single-series categorical bar charts.
 */
enum class ChartBarColorMode(
    val id: String,
    val displayName: String,
) {
    AUTO("auto", "Auto"),
    UNIFORM("uniform", "Single hue"),
    DISTINCT_CATEGORY("distinct", "Distinct per bar");

    companion object {
        fun fromId(id: String?): ChartBarColorMode =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * High-level chart family classification across all 52 supported charts in Stat Graph Studio.
 */
enum class AdaptiveChartFamily {
    VERTICAL_CATEGORICAL_BAR,
    HORIZONTAL_CATEGORICAL_RANKING,
    ORDERED_LINE_OR_AREA,
    SCATTER_AND_BIVARIATE,
    DISTRIBUTION_HISTOGRAM_DENSITY,
    GROUP_DISTRIBUTION_BOX_VIOLIN_STRIP,
    UNCERTAINTY_AND_FOREST,
    PART_TO_WHOLE_RADIAL_OR_GRID,
    HIERARCHICAL_TREEMAP_SUNBURST_MOSAIC,
    MATRIX_AND_HEATMAP,
    PAIRED_SLOPE_BUMP_RADAR,
    SMALL_MULTIPLES_AND_TABULAR,
}

/**
 * Quantitative density and collision diagnostics computed by [ChartReadabilityAnalyzer]
 * and [ChartDensityDiagnostics].
 */
@Immutable
data class ChartReadabilityMetrics(
    val chartKind: ChartKind,
    val family: AdaptiveChartFamily,
    val categoryCount: Int,
    val seriesCount: Int,
    val totalObservations: Int,
    val maxLabelCharLength: Int,
    val meanLabelCharLength: Float,
    val maxLabelWidthPx: Float,
    val availableViewportWidthPx: Float,
    val availableViewportHeightPx: Float,
    val estimatedPlotWidthPx: Float,
    val estimatedPlotHeightPx: Float,
    val horizontalSlotPitchPx: Float,
    val verticalRowPitchPx: Float,
    val horizontalCollisionRisk: Boolean,
    val rotated45CollisionRisk: Boolean,
    val overplottingRatio: Float,
    val requiresMultiColumnKey: Boolean,
    val diagnosticNotes: List<String>,
)

/**
 * Orientation recommendation produced by [ChartOrientationAdvisor].
 */
@Immutable
data class ChartOrientationAdvice(
    val currentKind: ChartKind,
    val recommendedKind: ChartKind,
    val shouldRecommendHorizontalSwitch: Boolean,
    val preserveExplicitUserVerticalSelection: Boolean,
    val reason: String?,
) {
    val recommendHorizontalBar: Boolean
        get() = recommendedKind == ChartKind.HORIZONTAL_BAR && currentKind != ChartKind.HORIZONTAL_BAR
}

/**
 * Complete layout decision produced by [ChartLayoutDecisionEngine] for either on-screen
 * mobile/tablet rendering or offscreen export rendering.
 */
@Immutable
data class ChartLayoutDecision(
    val chartKind: ChartKind,
    val family: AdaptiveChartFamily,
    val targetCanvasWidthPx: Float,
    val targetCanvasHeightPx: Float,
    val fitsSingleViewportWidth: Boolean,
    val isHorizontallyScrollable: Boolean,
    val recommendedLeftMarginRatio: Float,
    val recommendedBottomMarginRatio: Float,
    val minBarThicknessPx: Float,
    val preferredBarGapPx: Float,
    val useDistinctCategoryColors: Boolean,
    val scatterPointAlpha: Float,
    val scatterRadiusScale: Float,
    val preferredLabelMode: ChartLabelDisplayMode,
    val orientationAdvice: ChartOrientationAdvice,
    val abbreviationKey: List<CategoryAbbreviationEntry>,
    val adaptationReasons: List<String>,
)

/**
 * Diagnostics helper for all 52 chart types.
 */
object ChartDensityDiagnostics {

    fun classifyFamily(chartKind: ChartKind): AdaptiveChartFamily = when (chartKind) {
        ChartKind.BAR,
        ChartKind.GROUPED_BAR,
        ChartKind.STACKED_BAR,
        ChartKind.PERCENT_STACKED_BAR,
        ChartKind.PARETO,
        ChartKind.WATERFALL -> AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR

        ChartKind.HORIZONTAL_BAR,
        ChartKind.LOLLIPOP,
        ChartKind.DOT_PLOT,
        ChartKind.DUMBBELL,
        ChartKind.BULLET,
        ChartKind.DIVERGING_BAR,
        ChartKind.LIKERT_DIVERGING,
        ChartKind.POPULATION_PYRAMID -> AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING

        ChartKind.LINE,
        ChartKind.MULTI_LINE,
        ChartKind.AREA,
        ChartKind.STACKED_AREA,
        ChartKind.STEP,
        ChartKind.RANGE_BAND,
        ChartKind.CONTROL_CHART,
        ChartKind.CYCLE_PLOT,
        ChartKind.KAPLAN_MEIER -> AdaptiveChartFamily.ORDERED_LINE_OR_AREA

        ChartKind.SCATTER,
        ChartKind.CONNECTED_SCATTER,
        ChartKind.BUBBLE,
        ChartKind.HEXBIN,
        ChartKind.QQ_PLOT,
        ChartKind.ECDF -> AdaptiveChartFamily.SCATTER_AND_BIVARIATE

        ChartKind.HISTOGRAM,
        ChartKind.DENSITY -> AdaptiveChartFamily.DISTRIBUTION_HISTOGRAM_DENSITY

        ChartKind.BOX_PLOT,
        ChartKind.VIOLIN,
        ChartKind.STRIP_PLOT -> AdaptiveChartFamily.GROUP_DISTRIBUTION_BOX_VIOLIN_STRIP

        ChartKind.ERROR_BAR,
        ChartKind.RANGE_PLOT,
        ChartKind.FOREST_PLOT -> AdaptiveChartFamily.UNCERTAINTY_AND_FOREST

        ChartKind.PIE,
        ChartKind.DONUT,
        ChartKind.WAFFLE,
        ChartKind.FUNNEL -> AdaptiveChartFamily.PART_TO_WHOLE_RADIAL_OR_GRID

        ChartKind.TREEMAP,
        ChartKind.SUNBURST,
        ChartKind.MOSAIC -> AdaptiveChartFamily.HIERARCHICAL_TREEMAP_SUNBURST_MOSAIC

        ChartKind.CORRELATION_HEATMAP,
        ChartKind.CALENDAR_HEATMAP,
        ChartKind.SCATTERPLOT_MATRIX -> AdaptiveChartFamily.MATRIX_AND_HEATMAP

        ChartKind.SLOPE,
        ChartKind.BUMP,
        ChartKind.RADAR -> AdaptiveChartFamily.PAIRED_SLOPE_BUMP_RADAR

        ChartKind.SPARKLINE_MULTIPLES,
        ChartKind.STEM_AND_LEAF -> AdaptiveChartFamily.SMALL_MULTIPLES_AND_TABULAR
    }

    fun isVerticalBarFamily(chartKind: ChartKind): Boolean =
        classifyFamily(chartKind) == AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR

    fun isHorizontalCategoryFamily(chartKind: ChartKind): Boolean = when (chartKind) {
        ChartKind.HORIZONTAL_BAR,
        ChartKind.LOLLIPOP,
        ChartKind.DOT_PLOT,
        ChartKind.DUMBBELL,
        ChartKind.BULLET,
        ChartKind.DIVERGING_BAR,
        ChartKind.LIKERT_DIVERGING,
        ChartKind.POPULATION_PYRAMID,
        ChartKind.RANGE_PLOT,
        ChartKind.FOREST_PLOT -> true
        else -> false
    }
}

/**
 * Analyzes viewport geometry, category cardinality, label lengths, and data density
 * across all 52 chart types.
 */
object ChartReadabilityAnalyzer {

    fun classifyFamily(chartKind: ChartKind): AdaptiveChartFamily =
        ChartDensityDiagnostics.classifyFamily(chartKind)

    fun analyze(
        data: ChartRenderData,
        viewportWidthPx: Float = 955f,
        viewportHeightPx: Float = 700f,
        fontSizePx: Float = 30f,
        textMeasurer: AxisTextMeasurer? = null,
        fontWeight: Int = 400,
    ): ChartReadabilityMetrics {
        val family = ChartDensityDiagnostics.classifyFamily(data.chartKind)
        val labels = when {
            data.domainLabels.isNotEmpty() -> data.domainLabels
            data.boxPlotGroups.isNotEmpty() -> data.boxPlotGroups.map { it.groupLabel }
            data.violinGroups.isNotEmpty() -> data.violinGroups.map { it.groupLabel }
            data.correlationVariableNames.isNotEmpty() -> data.correlationVariableNames
            data.splomVariableNames.isNotEmpty() -> data.splomVariableNames
            else -> data.series.firstOrNull()?.points?.map { it.domainLabel }.orEmpty()
        }
        val categoryCount = labels.size.coerceAtLeast(1)
        val seriesCount = data.series.size.coerceAtLeast(1)
        val totalObs = data.series.sumOf { s -> s.points.count { it.numericValue != null } }
            .coerceAtLeast(categoryCount)

        val maxChars = labels.maxOfOrNull { it.trim().length } ?: 0
        val meanChars = if (labels.isNotEmpty()) {
            labels.sumOf { it.trim().length }.toFloat() / labels.size.toFloat()
        } else {
            0f
        }
        val maxLabelWidthPx = if (labels.isEmpty()) {
            0f
        } else if (textMeasurer != null) {
            labels.maxOf { textMeasurer.measure(it, fontSizePx, fontWeight).widthPx }
        } else {
            maxChars * fontSizePx * 0.58f
        }

        val safeViewportW = viewportWidthPx.coerceAtLeast(240f)
        val safeViewportH = viewportHeightPx.coerceAtLeast(180f)
        val isHorizontalCat = ChartDensityDiagnostics.isHorizontalCategoryFamily(data.chartKind)

        val estLeftMargin = if (isHorizontalCat) {
            (maxLabelWidthPx + fontSizePx * 1.2f).coerceIn(safeViewportW * 0.18f, safeViewportW * 0.42f)
        } else {
            (fontSizePx * 3.8f).coerceAtLeast(42f)
        }
        val estPlotW = (safeViewportW - estLeftMargin - 20f).coerceAtLeast(100f)
        val estPlotH = (safeViewportH - fontSizePx * 4.2f).coerceAtLeast(90f)

        val slotPitchX = estPlotW / categoryCount.toFloat()
        val rowPitchY = estPlotH / categoryCount.toFloat()

        val minGapPx = max(8f, 0.6f * fontSizePx)
        val horizontalCollision = !isHorizontalCat && categoryCount > 1 &&
            (maxLabelWidthPx + minGapPx > slotPitchX)
        val rotated45RequiredSlot = fontSizePx * 1.15f + 4f
        val rotated45Collision = !isHorizontalCat && categoryCount > 1 &&
            (slotPitchX < rotated45RequiredSlot || maxLabelWidthPx * 0.707f > estPlotH * 0.48f)

        val markerArea = totalObs * (fontSizePx * 0.75f) * (fontSizePx * 0.75f)
        val plotArea = (estPlotW * estPlotH).coerceAtLeast(1f)
        val overplottingRatio = (markerArea / plotArea).coerceIn(0f, 5f)

        val notes = buildList {
            if (horizontalCollision) {
                add("$categoryCount categories with max label length $maxChars chars exceed horizontal slot width (${slotPitchX.toInt()} px).")
            }
            if (isHorizontalCat && rowPitchY < fontSizePx * 1.45f) {
                add("Horizontal category rows require vertical expansion (${categoryCount} rows).")
            }
            if (family == AdaptiveChartFamily.SCATTER_AND_BIVARIATE && totalObs > 60) {
                add("High point density ($totalObs observations); applying adaptive marker alpha and radius.")
            }
            if (family == AdaptiveChartFamily.PART_TO_WHOLE_RADIAL_OR_GRID && categoryCount > 7) {
                add("$categoryCount slices exceed optimal radial angle discrimination; ranking / external legend applied.")
            }
        }

        return ChartReadabilityMetrics(
            chartKind = data.chartKind,
            family = family,
            categoryCount = categoryCount,
            seriesCount = seriesCount,
            totalObservations = totalObs,
            maxLabelCharLength = maxChars,
            meanLabelCharLength = meanChars,
            maxLabelWidthPx = maxLabelWidthPx,
            availableViewportWidthPx = safeViewportW,
            availableViewportHeightPx = safeViewportH,
            estimatedPlotWidthPx = estPlotW,
            estimatedPlotHeightPx = estPlotH,
            horizontalSlotPitchPx = slotPitchX,
            verticalRowPitchPx = rowPitchY,
            horizontalCollisionRisk = horizontalCollision,
            rotated45CollisionRisk = rotated45Collision,
            overplottingRatio = overplottingRatio,
            requiresMultiColumnKey = categoryCount >= 12,
            diagnosticNotes = notes,
        )
    }
}

/**
 * Advises when a horizontal bar chart is preferable while strictly respecting
 * explicit user selection of a vertical bar chart.
 */
object ChartOrientationAdvisor {

    fun advise(
        metrics: ChartReadabilityMetrics,
        userExplicitlySelectedVertical: Boolean = false,
        isTimeSeries: Boolean = false,
    ): ChartOrientationAdvice {
        if (isTimeSeries) {
            return ChartOrientationAdvice(
                currentKind = metrics.chartKind,
                recommendedKind = metrics.chartKind,
                shouldRecommendHorizontalSwitch = false,
                preserveExplicitUserVerticalSelection = true,
                reason = null,
            )
        }

        val manyOrLongCategories = metrics.categoryCount > 8 ||
            metrics.maxLabelCharLength > 12 ||
            metrics.horizontalCollisionRisk

        val recommended = when {
            metrics.chartKind == ChartKind.BAR && manyOrLongCategories -> ChartKind.HORIZONTAL_BAR
            metrics.chartKind == ChartKind.ERROR_BAR && metrics.categoryCount > 10 -> ChartKind.FOREST_PLOT
            else -> metrics.chartKind
        }

        val reason = if (recommended != metrics.chartKind) {
            if (userExplicitlySelectedVertical) {
                "Keeping requested vertical bar orientation with adaptive category labels (${metrics.categoryCount} categories); Horizontal Bar is also available for full unrotated names."
            } else {
                "Horizontal bar orientation keeps all ${metrics.categoryCount} category names horizontal and directly readable."
            }
        } else {
            null
        }

        return ChartOrientationAdvice(
            currentKind = metrics.chartKind,
            recommendedKind = recommended,
            shouldRecommendHorizontalSwitch = recommended != metrics.chartKind && !userExplicitlySelectedVertical,
            preserveExplicitUserVerticalSelection = userExplicitlySelectedVertical,
            reason = reason,
        )
    }
}

/**
 * Deterministically generates concise, domain-aware category abbreviations (such as
 * `ED` for `Emergency Department`, `GM` for `General Medicine`, `ICU` for `Intensive Care Unit`,
 * `NICU` for `Neonatal Intensive Care Unit`) with guaranteed uniqueness and a complete
 * `Chart label -> Full category` reference key.
 *
 * Short labels that already fit within their slot are kept as-is unless `forceAbbreviateAll`
 * is requested.
 */
object ChartAbbreviationResolver {

    private val STOP_WORDS = setOf(
        "and", "of", "the", "in", "for", "to", "at", "by", "with", "on", "&", "+",
    )

    private val KNOWN_CLINICAL_ABBREVIATIONS: Map<String, String> = mapOf(
        "emergency department" to "ED",
        "accident and emergency" to "AE",
        "general medicine" to "GM",
        "general surgery" to "GS",
        "paediatrics" to "PAE",
        "pediatrics" to "PED",
        "obstetrics and gynaecology" to "OG",
        "obstetrics & gynaecology" to "OG",
        "obstetrics and gynecology" to "OG",
        "orthopaedics" to "ORT",
        "orthopedics" to "ORT",
        "cardiology" to "CAR",
        "oncology" to "ONC",
        "neurology" to "NEU",
        "intensive care unit" to "ICU",
        "neonatal intensive care unit" to "NICU",
        "paediatric intensive care unit" to "PICU",
        "renal dialysis unit" to "RDU",
        "respiratory medicine" to "RM",
        "ent department" to "ENT",
        "ear nose and throat" to "ENT",
        "ophthalmology" to "OPH",
        "psychiatry" to "PSY",
        "dermatology" to "DER",
        "physiotherapy and rehabilitation" to "PR",
        "infectious diseases" to "ID",
        "palliative care" to "PC",
        "outpatient department" to "OPD",
        "operating theatre" to "OT",
        "high dependency unit" to "HDU",
        "coronary care unit" to "CCU",
        "maternity ward" to "MAT",
        "radiology" to "RAD",
        "pathology" to "PAT",
        "haematology" to "HAE",
        "hematology" to "HEM",
        "gastroenterology" to "GAS",
        "endocrinology" to "END",
        "nephrology" to "NEP",
        "urology" to "URO",
        "rheumatology" to "RHE",
        "geriatrics" to "GER",
        "anaesthesiology" to "ANA",
    )

    data class AbbreviationResolution(
        val displayLabels: List<String>,
        val referenceKey: List<CategoryAbbreviationEntry>,
    )

    fun buildAbbreviationKey(originalLabels: List<String>): List<CategoryAbbreviationEntry> {
        val simpleMeasurer = object : AxisTextMeasurer {
            override fun measure(text: String, fontSizePx: Float, fontWeight: Int): AxisTextMetrics =
                AxisTextMetrics(widthPx = text.length * fontSizePx * 0.56f, heightPx = fontSizePx * 1.25f)
        }
        return resolve(
            originalLabels = originalLabels,
            maxAllowedWidthPx = 48f,
            fontSizePx = 11f,
            fontWeight = 400,
            textMeasurer = simpleMeasurer,
            forceAbbreviateAll = true,
        ).referenceKey
    }

    fun resolve(
        originalLabels: List<String>,
        maxAllowedWidthPx: Float,
        fontSizePx: Float,
        fontWeight: Int,
        textMeasurer: AxisTextMeasurer,
        forceAbbreviateAll: Boolean = false,
    ): AbbreviationResolution {
        if (originalLabels.isEmpty()) {
            return AbbreviationResolution(emptyList(), emptyList())
        }

        val usedCodes = HashSet<String>(originalLabels.size * 2)
        val displayLabels = MutableList(originalLabels.size) { "" }
        val keyEntries = ArrayList<CategoryAbbreviationEntry>(originalLabels.size)

        // First pass: check which labels already fit without abbreviation
        val fitsNaturally = BooleanArray(originalLabels.size)
        if (!forceAbbreviateAll) {
            for (i in originalLabels.indices) {
                val raw = originalLabels[i].trim()
                val w = textMeasurer.measure(raw, fontSizePx, fontWeight).widthPx
                if (raw.length <= 4 && w <= maxAllowedWidthPx) {
                    fitsNaturally[i] = true
                    usedCodes.add(raw.uppercase(Locale.ROOT))
                    displayLabels[i] = raw
                }
            }
        }

        for (i in originalLabels.indices) {
            if (fitsNaturally[i]) continue
            val full = originalLabels[i].trim()
            val baseCandidate = buildBaseCandidate(full, index = i)
            var uniqueCode = disambiguate(baseCandidate, full, i, usedCodes)

            // Ensure the code itself fits maxAllowedWidthPx; if slot is very narrow, use compact code
            if (maxAllowedWidthPx > 0f &&
                textMeasurer.measure(uniqueCode, fontSizePx, fontWeight).widthPx > maxAllowedWidthPx
            ) {
                val shorter = baseCandidate.take(2).ifEmpty { "C${i + 1}" }
                uniqueCode = disambiguate(shorter, full, i, usedCodes)
            }

            usedCodes.add(uniqueCode.uppercase(Locale.ROOT))
            displayLabels[i] = uniqueCode
            if (!uniqueCode.equals(full, ignoreCase = false)) {
                keyEntries.add(
                    CategoryAbbreviationEntry(
                        domainIndex = i,
                        shortCode = uniqueCode,
                        fullLabel = originalLabels[i],
                    ),
                )
            }
        }

        return AbbreviationResolution(
            displayLabels = displayLabels,
            referenceKey = keyEntries,
        )
    }

    internal fun buildBaseCandidate(fullLabel: String, index: Int): String {
        val cleaned = fullLabel.trim()
        if (cleaned.isEmpty()) return "C${index + 1}"

        val lower = cleaned.lowercase(Locale.ROOT)
        KNOWN_CLINICAL_ABBREVIATIONS[lower]?.let { return it }

        // Check if label already starts with an uppercase code in parentheses or prefix
        val words = cleaned
            .split(Regex("""[\s\-_/,:;()]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return "C${index + 1}"

        val significantWords = words.filter { it.lowercase(Locale.ROOT) !in STOP_WORDS }
            .ifEmpty { words }

        // If first word is already an all-caps acronym (like "ENT", "ICU", "NICU", "OPD")
        if (significantWords.size >= 2 &&
            significantWords.first().length in 2..4 &&
            significantWords.first().all { it.isUpperCase() || it.isDigit() }
        ) {
            val firstWord = significantWords.first()
            if (significantWords.size == 2 && significantWords[1].equals("Department", ignoreCase = true)) {
                return firstWord
            }
        }

        return when {
            significantWords.size >= 2 -> {
                // Acronym from initials of significant words (up to 4 chars)
                val initials = StringBuilder()
                for (w in significantWords.take(4)) {
                    val clusters = AxisLabelPlanner.splitGraphemeClusters(w)
                    val firstCluster = clusters.firstOrNull() ?: continue
                    initials.append(firstCluster.uppercase(Locale.ROOT))
                }
                initials.toString().ifEmpty { "C${index + 1}" }
            }
            else -> {
                // Single word (e.g. "Paediatrics" -> "PAE", "Cardiology" -> "CAR", "Oncology" -> "ONC")
                val single = significantWords.first()
                val clusters = AxisLabelPlanner.splitGraphemeClusters(single)
                if (clusters.size <= 3) {
                    single.uppercase(Locale.ROOT)
                } else {
                    clusters.take(3).joinToString("").uppercase(Locale.ROOT)
                }
            }
        }
    }

    private fun disambiguate(
        baseCode: String,
        fullLabel: String,
        index: Int,
        usedCodes: Set<String>,
    ): String {
        if (baseCode.uppercase(Locale.ROOT) !in usedCodes) {
            return baseCode
        }
        // Try 4-letter prefix for single-word categories or adding next letter
        val clusters = AxisLabelPlanner.splitGraphemeClusters(
            fullLabel.replace(Regex("""\s+"""), ""),
        )
        if (clusters.size >= 4) {
            val fourChar = clusters.take(4).joinToString("").uppercase(Locale.ROOT)
            if (fourChar !in usedCodes) return fourChar
        }
        var suffix = 2
        while ("$baseCode$suffix".uppercase(Locale.ROOT) in usedCodes) {
            suffix++
        }
        return "$baseCode$suffix"
    }
}

/**
 * Plans color assignment for single-series and multi-series charts using recognized
 * color-blind-safe qualitative palettes (Okabe–Ito / App Vivid / Journal B&W)
 * while maintaining consistent category-to-color mapping across related charts.
 */
object ChartColorPlanner {

    fun shouldUseDistinctCategoryColors(
        data: ChartRenderData,
        mode: ChartBarColorMode = ChartBarColorMode.AUTO,
        hasAbbreviationKey: Boolean = false,
    ): Boolean {
        if (data.series.size > 1) return false
        return when (mode) {
            ChartBarColorMode.UNIFORM -> false
            ChartBarColorMode.DISTINCT_CATEGORY -> true
            ChartBarColorMode.AUTO -> {
                val isSingleSeriesCategoricalBar = data.chartKind in setOf(
                    ChartKind.BAR,
                    ChartKind.HORIZONTAL_BAR,
                    ChartKind.LOLLIPOP,
                )
                // Use distinct CVD-safe colors when abbreviations/codes are active or when
                // comparing multiple distinct categories so each bar is easily cross-referenced
                isSingleSeriesCategoricalBar && (hasAbbreviationKey || data.domainLabels.size in 2..30)
            }
        }
    }

    fun colorForCategory(
        domainIndex: Int,
        domainLabel: String,
        style: ChartStyle,
        useDistinctColors: Boolean,
        defaultSeriesColorIndex: Int = 0,
    ): Color {
        if (!useDistinctColors) {
            return style.seriesColor(defaultSeriesColorIndex)
        }
        val paletteSize = style.seriesColors.size.coerceAtLeast(1)
        val colorIdx = domainIndex % paletteSize
        return style.seriesColor(colorIdx)
    }
}

/**
 * Coordinates the label strategy hierarchy for categorical and temporal axes.
 */
object ChartLabelLayoutPlanner {

    fun planCategoricalAxis(
        input: AxisLabelInput,
        preferredMode: ChartLabelDisplayMode = ChartLabelDisplayMode.AUTO,
    ): AxisLabelPlan {
        return AxisLabelPlanner.plan(
            input = input.copy(preferredDisplayMode = preferredMode),
        )
    }
}

/**
 * Central adaptive layout decision engine for all 52 chart types.
 */
object ChartLayoutDecisionEngine {

    /**
     * Maximum number of vertical bars that should be fitted into a single mobile/tablet
     * card viewport before enabling horizontal scrolling.
     * Fitting up to 25 categories on a phone screen (with 8–10dp bars and 45°/90°/abbreviated labels)
     * ensures a 20-category dataset like "Bujete" is 100% visible at once without clipping!
     */
    const val MAX_SINGLE_VIEWPORT_VERTICAL_BARS_MOBILE: Int = 25
    const val MAX_SINGLE_VIEWPORT_VERTICAL_BARS_TABLET: Int = 40

    fun decideForScreen(
        renderData: ChartRenderData,
        viewportWidthPx: Int,
        baseHeightPx: Int,
        density: Float,
    ): ChartLayoutDecision = decideForScreen(
        data = renderData,
        viewportWidthPx = viewportWidthPx.toFloat(),
        defaultChartHeightPx = baseHeightPx.toFloat(),
        densityScale = density,
        fontSizePx = 11f * density,
        baseInsets = ChartInsets(
            left = 48f * density,
            top = 16f * density,
            right = 18f * density,
            bottom = 42f * density,
        ),
        userExplicitlySelectedChart = renderData.userExplicitlySelectedChart,
        preferredLabelMode = renderData.preferredLabelMode,
        preferredBarColorMode = renderData.preferredBarColorMode,
    )

    fun decideForScreen(
        data: ChartRenderData,
        viewportWidthPx: Float,
        defaultChartHeightPx: Float,
        densityScale: Float,
        fontSizePx: Float,
        baseInsets: ChartInsets,
        userExplicitlySelectedChart: Boolean = false,
        preferredLabelMode: ChartLabelDisplayMode = ChartLabelDisplayMode.AUTO,
        preferredBarColorMode: ChartBarColorMode = ChartBarColorMode.AUTO,
        textMeasurer: AxisTextMeasurer? = null,
    ): ChartLayoutDecision {
        val metrics = ChartReadabilityAnalyzer.analyze(
            data = data,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = defaultChartHeightPx,
            fontSizePx = fontSizePx,
            textMeasurer = textMeasurer,
        )
        val family = metrics.family
        val categoryCount = metrics.categoryCount
        val seriesCount = metrics.seriesCount
        val reasons = mutableListOf<String>()

        val isTimeSeries = data.domainLabels.isNotEmpty() &&
            AxisLabelPlanner.simplifyLabels(data.domainLabels, null).detectedTemporal

        val orientationAdvice = ChartOrientationAdvisor.advise(
            metrics = metrics,
            userExplicitlySelectedVertical = userExplicitlySelectedChart,
            isTimeSeries = isTimeSeries,
        )

        val dpPx = densityScale.coerceAtLeast(1f)
        val isTabletViewport = viewportWidthPx >= 600f * dpPx

        var targetWidthPx = viewportWidthPx
        var targetHeightPx = defaultChartHeightPx
        var leftMarginRatio = 0.22f
        var bottomMarginRatio = 0.25f
        var minBarPx = 6f * dpPx
        var barGapPx = 3f * dpPx
        var scatterAlpha = 0.88f
        var scatterRadiusScale = 1f

        when (family) {
            AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING -> {
                // CRITICAL FIX FOR DEFECT B:
                // Horizontal bar charts and horizontal category charts MUST ALWAYS fit
                // their numeric X-axis inside viewportWidthPx (never expand horizontally by categoryCount!),
                // and instead scale their height vertically so every category row is crisp and readable.
                targetWidthPx = viewportWidthPx
                val minRowHeightPx = when {
                    categoryCount <= 8 -> 30f * dpPx
                    categoryCount <= 25 -> 24f * dpPx
                    categoryCount <= 50 -> 20f * dpPx
                    else -> 18f * dpPx
                }
                val topBottomPad = baseInsets.top + baseInsets.bottom + 24f * dpPx
                val neededHeight = topBottomPad + categoryCount * minRowHeightPx
                targetHeightPx = max(defaultChartHeightPx, neededHeight).coerceAtMost(2400f * dpPx)
                leftMarginRatio = when {
                    metrics.maxLabelCharLength >= 24 -> 0.44f
                    metrics.maxLabelCharLength >= 16 -> 0.38f
                    else -> 0.30f
                }
                bottomMarginRatio = 0.18f
                if (targetHeightPx > defaultChartHeightPx + 4f) {
                    reasons.add("Expanded chart height for $categoryCount horizontal category rows while keeping X-axis scale fitted to screen width.")
                }
            }

            AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR -> {
                // CRITICAL FIX FOR DEFECT A:
                // Fit up to 25 vertical bars (or 40 on tablet) directly inside the visible card width
                // by dynamically sizing bar thickness and inter-bar gaps, and increase canvas height
                // slightly when rotated or abbreviated labels need bottom room.
                val effectiveBarsPerCat = BarDensityPolicy.calculateEffectiveSeriesPerCategory(data.chartKind, seriesCount)
                val totalBars = categoryCount * effectiveBarsPerCat
                val maxFitBars = if (isTabletViewport) {
                    MAX_SINGLE_VIEWPORT_VERTICAL_BARS_TABLET
                } else {
                    MAX_SINGLE_VIEWPORT_VERTICAL_BARS_MOBILE
                }

                minBarPx = when {
                    totalBars <= 10 -> 14f * dpPx
                    totalBars <= 20 -> 9f * dpPx
                    totalBars <= 30 -> 6.5f * dpPx
                    else -> 5.5f * dpPx
                }
                barGapPx = when {
                    categoryCount <= 10 -> 6f * dpPx
                    categoryCount <= 20 -> 3.5f * dpPx
                    else -> 2.5f * dpPx
                }

                if (totalBars <= maxFitBars) {
                    targetWidthPx = viewportWidthPx
                    if (categoryCount > 6 && metrics.horizontalCollisionRisk) {
                        reasons.add("Fitted all $categoryCount bars into viewport width with adaptive label layout.")
                    }
                } else {
                    val slotW = effectiveBarsPerCat * minBarPx + barGapPx + 4f * dpPx
                    val requiredPlotW = categoryCount * slotW
                    targetWidthPx = max(viewportWidthPx, baseInsets.left + baseInsets.right + requiredPlotW)
                    reasons.add("Adapted bar spacing for $categoryCount categories (scroll horizontally to inspect all bars).")
                }

                if (metrics.horizontalCollisionRisk) {
                    targetHeightPx = max(defaultChartHeightPx, defaultChartHeightPx + 36f * dpPx)
                    bottomMarginRatio = 0.36f
                }
            }

            AdaptiveChartFamily.ORDERED_LINE_OR_AREA -> {
                // Line/area charts can comfortably show up to 120 points in a single viewport with thinned ticks
                val maxFitPoints = if (isTabletViewport) 180 else 120
                if (categoryCount <= maxFitPoints) {
                    targetWidthPx = viewportWidthPx
                    if (categoryCount > 10) {
                        reasons.add("Dense sequence ($categoryCount periods): thinned X-axis ticks to prevent label overlap.")
                    }
                } else {
                    val slotPx = 6f * dpPx
                    targetWidthPx = max(viewportWidthPx, baseInsets.left + baseInsets.right + categoryCount * slotPx)
                        .coerceAtMost(viewportWidthPx * 2.5f)
                    reasons.add("Dense sequence ($categoryCount periods): thinned X-axis ticks to prevent label overlap.")
                }
            }

            AdaptiveChartFamily.SCATTER_AND_BIVARIATE -> {
                targetWidthPx = viewportWidthPx
                when {
                    metrics.totalObservations > 200 -> {
                        scatterAlpha = 0.42f
                        scatterRadiusScale = 0.72f
                        reasons.add("Applied high-density alpha (${(scatterAlpha * 100).toInt()}%) and reduced marker size for ${metrics.totalObservations} points.")
                    }
                    metrics.totalObservations > 60 -> {
                        scatterAlpha = 0.62f
                        scatterRadiusScale = 0.85f
                        reasons.add("Adjusted marker transparency to reduce overplotting across ${metrics.totalObservations} observations.")
                    }
                }
            }

            AdaptiveChartFamily.DISTRIBUTION_HISTOGRAM_DENSITY -> {
                targetWidthPx = viewportWidthPx
                minBarPx = 6f * dpPx
                barGapPx = 1.5f * dpPx
            }

            AdaptiveChartFamily.GROUP_DISTRIBUTION_BOX_VIOLIN_STRIP -> {
                val maxGroupsFit = if (isTabletViewport) 20 else 12
                if (categoryCount <= maxGroupsFit) {
                    targetWidthPx = viewportWidthPx
                } else {
                    targetWidthPx = max(viewportWidthPx, baseInsets.left + baseInsets.right + categoryCount * 36f * dpPx)
                    reasons.add("Expanded group axis for $categoryCount distribution groups.")
                }
                if (metrics.horizontalCollisionRisk) {
                    targetHeightPx = max(defaultChartHeightPx, defaultChartHeightPx + 28f * dpPx)
                    bottomMarginRatio = 0.34f
                }
            }

            AdaptiveChartFamily.UNCERTAINTY_AND_FOREST -> {
                if (ChartDensityDiagnostics.isHorizontalCategoryFamily(data.chartKind)) {
                    targetWidthPx = viewportWidthPx
                    val neededH = baseInsets.top + baseInsets.bottom + categoryCount * 24f * dpPx
                    targetHeightPx = max(defaultChartHeightPx, neededH).coerceAtMost(2200f * dpPx)
                    leftMarginRatio = 0.38f
                } else {
                    val maxFit = if (isTabletViewport) 28 else 18
                    targetWidthPx = if (categoryCount <= maxFit) {
                        viewportWidthPx
                    } else {
                        max(viewportWidthPx, baseInsets.left + baseInsets.right + categoryCount * 28f * dpPx)
                    }
                }
            }

            AdaptiveChartFamily.PART_TO_WHOLE_RADIAL_OR_GRID -> {
                targetWidthPx = viewportWidthPx
                if (categoryCount > 8) {
                    targetHeightPx = max(defaultChartHeightPx, defaultChartHeightPx + 32f * dpPx)
                    reasons.add("Preserved all $categoryCount categories with external callout/legend layout to avoid slice label overlap.")
                }
            }

            AdaptiveChartFamily.HIERARCHICAL_TREEMAP_SUNBURST_MOSAIC -> {
                targetWidthPx = viewportWidthPx
                if (categoryCount > 10) {
                    targetHeightPx = max(defaultChartHeightPx, defaultChartHeightPx + 40f * dpPx)
                }
            }

            AdaptiveChartFamily.MATRIX_AND_HEATMAP -> {
                targetWidthPx = viewportWidthPx
                val dim = max(
                    data.correlationVariableNames.size,
                    max(data.splomVariableNames.size, categoryCount),
                )
                if (dim >= 6) {
                    targetHeightPx = max(defaultChartHeightPx, min(viewportWidthPx * 0.95f, 460f * dpPx))
                }
            }

            AdaptiveChartFamily.PAIRED_SLOPE_BUMP_RADAR -> {
                targetWidthPx = viewportWidthPx
                if (seriesCount > 8 || categoryCount > 10) {
                    targetHeightPx = max(defaultChartHeightPx, defaultChartHeightPx + 40f * dpPx)
                }
            }

            AdaptiveChartFamily.SMALL_MULTIPLES_AND_TABULAR -> {
                targetWidthPx = viewportWidthPx
                val panelCount = when (data.chartKind) {
                    ChartKind.SPARKLINE_MULTIPLES -> data.series.size.coerceAtLeast(1)
                    ChartKind.STEM_AND_LEAF -> data.stemAndLeafRows.size.coerceAtLeast(1)
                    else -> categoryCount
                }
                val perRowH = if (data.chartKind == ChartKind.SPARKLINE_MULTIPLES) 44f * dpPx else 22f * dpPx
                targetHeightPx = max(defaultChartHeightPx, baseInsets.top + baseInsets.bottom + panelCount * perRowH)
                    .coerceAtMost(2000f * dpPx)
            }
        }

        val fitsSingleViewport = targetWidthPx <= viewportWidthPx + 2f
        val isOrderedOrHistogram = family == AdaptiveChartFamily.ORDERED_LINE_OR_AREA ||
            family == AdaptiveChartFamily.DISTRIBUTION_HISTOGRAM_DENSITY
        val precomputedKey = if (!isOrderedOrHistogram &&
            !ChartDensityDiagnostics.isHorizontalCategoryFamily(data.chartKind) &&
            (metrics.rotated45CollisionRisk || preferredLabelMode == ChartLabelDisplayMode.ABBREVIATED_KEY) &&
            data.domainLabels.size > 1
        ) {
            ChartAbbreviationResolver.buildAbbreviationKey(data.domainLabels)
        } else {
            emptyList()
        }
        val useDistinctColors = ChartColorPlanner.shouldUseDistinctCategoryColors(
            data = data,
            mode = preferredBarColorMode,
            hasAbbreviationKey = precomputedKey.isNotEmpty() || metrics.rotated45CollisionRisk,
        )

        return ChartLayoutDecision(
            chartKind = data.chartKind,
            family = family,
            targetCanvasWidthPx = targetWidthPx,
            targetCanvasHeightPx = targetHeightPx,
            fitsSingleViewportWidth = fitsSingleViewport,
            isHorizontallyScrollable = !fitsSingleViewport,
            recommendedLeftMarginRatio = leftMarginRatio,
            recommendedBottomMarginRatio = bottomMarginRatio,
            minBarThicknessPx = minBarPx,
            preferredBarGapPx = barGapPx,
            useDistinctCategoryColors = useDistinctColors,
            scatterPointAlpha = scatterAlpha,
            scatterRadiusScale = scatterRadiusScale,
            preferredLabelMode = preferredLabelMode,
            orientationAdvice = orientationAdvice,
            abbreviationKey = precomputedKey,
            adaptationReasons = reasons,
        )
    }
}

/**
 * Slice of [ChartRenderData] for multi-page / multi-panel export when a dataset has
 * many categories (e.g., 48 or 100 categories) so no page shrinks text to an unreadable size.
 */
@Immutable
data class ChartExportPanelSlice(
    val panelIndex: Int,
    val totalPanels: Int,
    val startDomainIndex: Int,
    val endDomainIndexInclusive: Int,
    val sliceRenderData: ChartRenderData,
    val panelSubtitle: String,
) {
    val sliceData: ChartRenderData get() = sliceRenderData
    val panelTitleSuffix: String get() = panelSubtitle
}

/**
 * Complete publication-ready export layout plan produced by [ChartExportLayoutPlanner].
 */
@Immutable
data class ChartExportLayoutPlan(
    val resolvedOrientation: PdfOrientation,
    val orientationAutoSelected: Boolean,
    val orientationReason: String,
    val pageWidthPt: Int,
    val pageHeightPt: Int,
    val marginPt: Float,
    val contentWidthPt: Int,
    val targetChartHeightPt: Float,
    val bitmapRenderWidthPx: Int,
    val bitmapRenderHeightPx: Int,
    val panels: List<ChartExportPanelSlice>,
    val abbreviationKey: List<CategoryAbbreviationEntry>,
    val adaptationSummary: String,
) {
    val orientation: PdfOrientation get() = resolvedOrientation
    val chartDisplayHeightPt: Float get() = targetChartHeightPt
}

/**
 * Independently optimizes chart layout for A4 Portrait / Landscape PDF and high-resolution
 * PNG/JPG exports without blindly stretching the mobile viewport bitmap.
 */
object ChartExportLayoutPlanner {

    private const val DEFAULT_MARGIN_PT = 36f
    const val MAX_VERTICAL_CATEGORIES_PER_A4_LANDSCAPE_PANEL = 32
    const val MAX_VERTICAL_CATEGORIES_PER_A4_PORTRAIT_PANEL = 20
    const val MAX_HORIZONTAL_CATEGORIES_PER_A4_PANEL = 28

    fun selectOptimalPdfOrientation(
        data: ChartRenderData,
        requestedMode: PdfOrientation? = null,
    ): Pair<PdfOrientation, String> {
        val family = ChartDensityDiagnostics.classifyFamily(data.chartKind)
        val categoryCount = data.domainLabels.size.coerceAtLeast(1)
        val maxLabelLen = data.domainLabels.maxOfOrNull { it.length } ?: 0

        if (requestedMode != null) {
            return requestedMode to "User-selected ${requestedMode.displayName} A4 layout."
        }

        return when (family) {
            AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR -> {
                if (categoryCount >= 9 || (categoryCount >= 6 && maxLabelLen >= 12)) {
                    PdfOrientation.LANDSCAPE to
                        "Landscape A4 selected automatically to give $categoryCount vertical bars and category labels maximum horizontal width."
                } else {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected for compact $categoryCount-category vertical bar comparison."
                }
            }

            AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING -> {
                if (categoryCount >= 18) {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected automatically to maximize vertical height for $categoryCount horizontal category rows."
                } else if (maxLabelLen >= 26) {
                    PdfOrientation.LANDSCAPE to
                        "Landscape A4 selected automatically to accommodate long category names ($maxLabelLen chars) alongside horizontal value bars."
                } else {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected for balanced horizontal bar and table composition."
                }
            }

            AdaptiveChartFamily.ORDERED_LINE_OR_AREA -> {
                if (categoryCount >= 12) {
                    PdfOrientation.LANDSCAPE to
                        "Landscape A4 selected automatically for wide $categoryCount-period sequence trend."
                } else {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected for $categoryCount-period trend."
                }
            }

            AdaptiveChartFamily.GROUP_DISTRIBUTION_BOX_VIOLIN_STRIP -> {
                val groups = max(categoryCount, max(data.boxPlotGroups.size, data.violinGroups.size))
                if (groups >= 8) {
                    PdfOrientation.LANDSCAPE to
                        "Landscape A4 selected automatically for $groups distribution groups."
                } else {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected for $groups distribution groups."
                }
            }

            AdaptiveChartFamily.MATRIX_AND_HEATMAP -> {
                if (data.chartKind == ChartKind.CALENDAR_HEATMAP) {
                    PdfOrientation.LANDSCAPE to "Landscape A4 selected for weekly calendar heatmap grid."
                } else {
                    PdfOrientation.PORTRAIT to "Portrait A4 selected for square matrix aspect ratio."
                }
            }

            else -> {
                if (categoryCount >= 14) {
                    PdfOrientation.LANDSCAPE to
                        "Landscape A4 selected automatically for $categoryCount categories."
                } else {
                    PdfOrientation.PORTRAIT to
                        "Portrait A4 selected for standard publication layout."
                }
            }
        }
    }

    fun planPdfExport(
        data: ChartRenderData,
        requestedOrientation: PdfOrientation? = null,
        allowMultiPagePanels: Boolean = true,
    ): ChartExportLayoutPlan {
        val (orientation, reason) = selectOptimalPdfOrientation(data, requestedOrientation)
        val pageWidthPt = orientation.pageWidthPt
        val pageHeightPt = orientation.pageHeightPt
        val contentWidthPt = (pageWidthPt - 2f * DEFAULT_MARGIN_PT).toInt()
        val family = ChartDensityDiagnostics.classifyFamily(data.chartKind)
        val categoryCount = data.domainLabels.size.coerceAtLeast(1)

        // Compute shared global numeric bounds so multi-panel bar charts preserve identical scale!
        val globalNumericValues = data.series.flatMap { s ->
            s.points.flatMap { listOfNotNull(it.numericValue, it.lowerLimit, it.upperLimit) }
        }.filter { it.isFinite() }
        val globalMin = if (data.enforceZeroBaseline) {
            min(0.0, globalNumericValues.minOrNull() ?: 0.0)
        } else {
            globalNumericValues.minOrNull()
        }
        val globalMax = if (data.enforceZeroBaseline) {
            max(0.0, globalNumericValues.maxOrNull() ?: 1.0)
        } else {
            globalNumericValues.maxOrNull()
        }

        val maxCategoriesPerPanel = when (family) {
            AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR -> {
                if (orientation == PdfOrientation.LANDSCAPE) {
                    MAX_VERTICAL_CATEGORIES_PER_A4_LANDSCAPE_PANEL
                } else {
                    MAX_VERTICAL_CATEGORIES_PER_A4_PORTRAIT_PANEL
                }
            }
            AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING -> MAX_HORIZONTAL_CATEGORIES_PER_A4_PANEL
            else -> Int.MAX_VALUE
        }

        val panels = if (allowMultiPagePanels &&
            categoryCount > maxCategoriesPerPanel &&
            (family == AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR ||
                family == AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING)
        ) {
            val totalPanels = ceil(categoryCount.toDouble() / maxCategoriesPerPanel.toDouble()).toInt()
            val chunkSize = ceil(categoryCount.toDouble() / totalPanels.toDouble()).toInt()
            (0 until totalPanels).mapNotNull { panelIdx ->
                val startIdx = panelIdx * chunkSize
                if (startIdx >= categoryCount) return@mapNotNull null
                val endIdx = min(categoryCount - 1, startIdx + chunkSize - 1)
                val slicedLabels = data.domainLabels.subList(startIdx, endIdx + 1)
                val slicedSeries = data.series.map { s ->
                    s.copy(
                        points = s.points
                            .filter { it.domainIndex in startIdx..endIdx }
                            .map { pt -> pt.copy(domainIndex = pt.domainIndex - startIdx) },
                    )
                }
                val panelSub = "Panel ${panelIdx + 1} of $totalPanels (Categories ${startIdx + 1}–${endIdx + 1} of $categoryCount • Shared scale)"
                ChartExportPanelSlice(
                    panelIndex = panelIdx,
                    totalPanels = totalPanels,
                    startDomainIndex = startIdx,
                    endDomainIndexInclusive = endIdx,
                    sliceRenderData = data.copy(
                        subtitle = listOfNotNull(
                            data.subtitle.takeIf { it.isNotBlank() },
                            panelSub,
                        ).joinToString(" • "),
                        domainLabels = slicedLabels,
                        series = slicedSeries,
                        minNumericOverride = data.minNumericOverride ?: globalMin,
                        maxNumericOverride = data.maxNumericOverride ?: globalMax,
                    ),
                    panelSubtitle = panelSub,
                )
            }
        } else {
            listOf(
                ChartExportPanelSlice(
                    panelIndex = 0,
                    totalPanels = 1,
                    startDomainIndex = 0,
                    endDomainIndexInclusive = (categoryCount - 1).coerceAtLeast(0),
                    sliceRenderData = data,
                    panelSubtitle = "",
                ),
            )
        }

        val activePanelCategories = panels.firstOrNull()?.sliceRenderData?.domainLabels?.size ?: categoryCount
        val targetChartHeightPt = when (family) {
            AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING -> {
                val baseH = if (orientation == PdfOrientation.LANDSCAPE) 260f else 310f
                val maxH = if (orientation == PdfOrientation.LANDSCAPE) 410f else 560f
                (70f + activePanelCategories * 16.5f).coerceIn(baseH, maxH)
            }
            AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR -> {
                if (orientation == PdfOrientation.LANDSCAPE) 300f else 310f
            }
            else -> {
                if (orientation == PdfOrientation.LANDSCAPE) 275f else 300f
            }
        }

        val renderScale = 2.75f
        val bitmapW = (contentWidthPt * renderScale).toInt().coerceAtLeast(1400)
        val bitmapH = (targetChartHeightPt * renderScale).toInt().coerceAtLeast(760)

        // Pre-resolve whether abbreviations will be used on this panel width so PDF can print the reference key table
        val approxPlotW = bitmapW * 0.84f
        val approxFontSizePx = 11f * (bitmapW / 600f).coerceIn(1f, 6f)
        val approxMeasurer = object : AxisTextMeasurer {
            override fun measure(text: String, fontSizePx: Float, fontWeight: Int): AxisTextMetrics =
                AxisTextMetrics(widthPx = text.length * fontSizePx * 0.56f, heightPx = fontSizePx * 1.25f)
        }
        val firstSlice = panels.first().sliceRenderData
        val abbreviationKey = if (!ChartDensityDiagnostics.isHorizontalCategoryFamily(firstSlice.chartKind) &&
            firstSlice.domainLabels.size > 1
        ) {
            val count = firstSlice.domainLabels.size
            val slotW = approxPlotW / count.toFloat()
            val centers = List(count) { idx -> (idx + 0.5f) * slotW }
            val plan = AxisLabelPlanner.plan(
                AxisLabelInput(
                    labels = firstSlice.domainLabels,
                    slotCentersPx = centers,
                    axisStartPx = 0f,
                    axisEndPx = approxPlotW,
                    fontSizePx = approxFontSizePx,
                    textMeasurer = approxMeasurer,
                    allowRotation = true,
                ),
            )
            plan.abbreviationKey
        } else {
            emptyList()
        }

        val adaptationSummary = buildString {
            append(reason)
            if (panels.size > 1) {
                append(" Split across ${panels.size} shared-scale panels so all $categoryCount categories remain legible.")
            }
            if (abbreviationKey.isNotEmpty()) {
                append(" Includes ${abbreviationKey.size}-item category reference key.")
            }
        }

        return ChartExportLayoutPlan(
            resolvedOrientation = orientation,
            orientationAutoSelected = requestedOrientation == null,
            orientationReason = reason,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt,
            marginPt = DEFAULT_MARGIN_PT,
            contentWidthPt = contentWidthPt,
            targetChartHeightPt = targetChartHeightPt,
            bitmapRenderWidthPx = bitmapW,
            bitmapRenderHeightPx = bitmapH,
            panels = panels,
            abbreviationKey = abbreviationKey,
            adaptationSummary = adaptationSummary,
        )
    }

    fun planImageExportDimensions(
        data: ChartRenderData,
        scale: ImageExportScale,
    ): Pair<Int, Int> {
        val family = ChartDensityDiagnostics.classifyFamily(data.chartKind)
        val categoryCount = data.domainLabels.size.coerceAtLeast(1)
        val baseW = scale.widthPx
        val baseH = scale.heightPx

        return when (family) {
            AdaptiveChartFamily.HORIZONTAL_CATEGORICAL_RANKING -> {
                val neededH = (220 * scale.multiplier) + categoryCount * (28 * scale.multiplier)
                baseW to max(baseH, neededH).coerceAtMost(4800 * scale.multiplier)
            }
            AdaptiveChartFamily.VERTICAL_CATEGORICAL_BAR -> {
                if (categoryCount > 24) {
                    val extraW = categoryCount * (34 * scale.multiplier)
                    max(baseW, extraW).coerceAtMost(5400 * scale.multiplier) to max(baseH, (860 * scale.multiplier))
                } else {
                    baseW to max(baseH, (820 * scale.multiplier))
                }
            }
            else -> baseW to baseH
        }
    }
}
