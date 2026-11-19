package com.pasindu.nursingotapp.statgraph.chart.toolkit

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure-Kotlin, Android-independent axis label measurement and layout planner.
 *
 * Computes deterministic, non-overlapping, bounds-safe category/time axis labels
 * and required two-pass axis height/title dimensions using real measured text widths.
 */
object AxisLabelPlanner {

    private const val ELLIPSIS = "\u2026"
    private const val ROTATION_ANGLE_DEGREES = -45f
    private const val ROTATION_90_ANGLE_DEGREES = -90f
    private val COS_45 = cos(PI / 4.0).toFloat()
    private val SIN_45 = sin(PI / 4.0).toFloat()

    private val STANDARD_STEPS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 13, 15, 20, 25, 26, 52, 60, 90, 120)
    private val MONTH_STEPS = listOf(1, 2, 3, 4, 6, 12)
    private val DAY_STEPS = listOf(1, 2, 3, 5, 7, 10, 14, 15, 21, 28, 30, 60, 90, 120)

    private val ISO_DATE_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:[T\s].*)?$""")
    private val NUMERIC_ONLY_REGEX = Regex("""^\d+$""")
    private val MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    private val SEPARATORS = setOf(' ', '-', '_', ':', '/')

    fun plan(input: AxisLabelInput): AxisLabelPlan {
        val count = input.labels.size
        require(input.slotCentersPx.size == count) {
            "slotCentersPx size (${input.slotCentersPx.size}) must match labels size ($count)."
        }

        val axisLeft = min(input.axisStartPx, input.axisEndPx)
        val axisRight = max(input.axisStartPx, input.axisEndPx)
        val canvasLeft = min(input.canvasLeftPx, input.canvasRightPx).coerceAtMost(axisLeft)
        val canvasRight = max(input.canvasLeftPx, input.canvasRightPx).coerceAtLeast(axisRight)
        val effectiveMinGap = max(input.minGapPx, max(6f, 0.45f * input.fontSizePx))

        if (count == 0 || !axisLeft.isFinite() || !axisRight.isFinite() || canvasRight <= canvasLeft) {
            return AxisLabelPlan(
                items = emptyList(),
                visibleIndices = emptyList(),
                strategy = AxisLabelStrategy.FULL_HORIZONTAL,
                axisTitle = null,
                secondaryYearLabel = null,
                requiredAxisHeightPx = 0f,
                titleRowHeightPx = 0f,
                horizontalBarRecommended = false,
                effectiveMinGapPx = effectiveMinGap,
                abbreviationKey = emptyList(),
            )
        }

        // Step 1: Simplify repeated prefixes/suffixes or concise temporal labels
        val simplified = simplifyLabels(input.labels, input.temporalMetadata)
        val workingLabels = simplified.labels
        val axisTitle = simplified.axisTitle
        val secondaryYearLabel = simplified.secondaryYearLabel
        val titleRowHeightPx = measureTitleHeight(
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            input = input,
        )

        val isTimeSeries = input.temporalMetadata?.isTemporal == true ||
            simplified.detectedTemporal ||
            workingLabels.all { NUMERIC_ONLY_REGEX.matches(it.trim()) }

        // Honor explicit user display mode override when applicable for categorical axes
        if (!isTimeSeries && input.preferredDisplayMode != ChartLabelDisplayMode.AUTO) {
            when (input.preferredDisplayMode) {
                ChartLabelDisplayMode.ABBREVIATED_KEY -> {
                    tryAbbreviatedWithKey(
                        originalLabels = input.labels,
                        input = input,
                        canvasLeft = canvasLeft,
                        canvasRight = canvasRight,
                        effectiveMinGap = effectiveMinGap,
                        axisTitle = axisTitle,
                        secondaryYearLabel = secondaryYearLabel,
                        titleRowHeightPx = titleRowHeightPx,
                        forceAbbreviateAll = true,
                    )?.let { return it }
                }
                ChartLabelDisplayMode.ROTATED_45 -> {
                    tryRotated45(
                        originalLabels = input.labels,
                        displayLabels = workingLabels,
                        input = input,
                        canvasLeft = canvasLeft,
                        canvasRight = canvasRight,
                        effectiveMinGap = effectiveMinGap,
                        axisTitle = axisTitle,
                        secondaryYearLabel = secondaryYearLabel,
                        titleRowHeightPx = titleRowHeightPx,
                        allowEllipsis = true,
                    )?.let { return it }
                }
                ChartLabelDisplayMode.ROTATED_90 -> {
                    tryRotated90(
                        originalLabels = input.labels,
                        displayLabels = workingLabels,
                        input = input,
                        canvasLeft = canvasLeft,
                        canvasRight = canvasRight,
                        effectiveMinGap = effectiveMinGap,
                        axisTitle = axisTitle,
                        secondaryYearLabel = secondaryYearLabel,
                        titleRowHeightPx = titleRowHeightPx,
                        allowEllipsis = true,
                    )?.let { return it }
                }
                ChartLabelDisplayMode.FULL, ChartLabelDisplayMode.AUTO -> Unit
            }
        }

        // Step 2: Try full horizontal labels
        tryHorizontalAll(
            originalLabels = input.labels,
            displayLabels = workingLabels,
            input = input,
            canvasLeft = canvasLeft,
            canvasRight = canvasRight,
            effectiveMinGap = effectiveMinGap,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            titleRowHeightPx = titleRowHeightPx,
        )?.let { return it }

        // Step 3: Thin labels (ONLY for time series / ordinal numeric axes; never drop distinct categories)
        if (isTimeSeries) {
            tryThinnedHorizontal(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
            )?.let { return it }
        }

        // Step 4: Rotate 45 degrees for categorical axes when full labels (or minimally trimmed labels) fit cleanly
        if (input.allowRotation && !isTimeSeries) {
            tryRotated45(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = false,
            )?.let { return it }
        }

        // Step 5: Shorten labels with grapheme-cluster-aware truncation (when majority of text stays intact)
        if (isTimeSeries || count <= 8) {
            tryTruncatedHorizontal(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                isTimeSeries = isTimeSeries,
            )?.let { return it }
        }

        // Step 6: Try 45-degree rotation with gentle truncation (at least 8 visible characters preserved)
        if (input.allowRotation && !isTimeSeries) {
            tryRotated45(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = true,
            )?.let { return it }
        }

        // Step 7: Try 90-degree vertical labels when full or legible names fit vertically
        if (input.allowRotation && !isTimeSeries && count <= 16) {
            tryRotated90(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = false,
            )?.let { return it }
        }

        // Step 8: Intelligent abbreviated category codes (e.g. ED, GM, GS, ONC) with full-name reference key!
        // Guarantees every single bar is unambiguously labeled on-chart and mapped in the reference key.
        if (!isTimeSeries) {
            tryAbbreviatedWithKey(
                originalLabels = input.labels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                forceAbbreviateAll = false,
            )?.let { return it }
        }

        // Step 9: Try 90-degree vertical labels with truncation if needed
        if (input.allowRotation && !isTimeSeries) {
            tryRotated90(
                originalLabels = input.labels,
                displayLabels = workingLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = true,
            )?.let { return it }
        }

        // Step 10: Final fallback for ultra-dense continuous/time series
        val fallbackFirst = buildSafeSingleFirstFallback(
            originalLabels = input.labels,
            displayLabels = workingLabels,
            input = input,
            canvasLeft = canvasLeft,
            canvasRight = canvasRight,
            effectiveMinGap = effectiveMinGap,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            titleRowHeightPx = titleRowHeightPx,
            horizontalBarRecommended = !isTimeSeries,
        )
        return fallbackFirst
    }

    private fun tryHorizontalAll(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
    ): AxisLabelPlan? {
        val indices = displayLabels.indices.toList()
        return validateAndBuildHorizontalPlan(
            indices = indices,
            originalLabels = originalLabels,
            displayLabels = displayLabels,
            input = input,
            canvasLeft = canvasLeft,
            canvasRight = canvasRight,
            effectiveMinGap = effectiveMinGap,
            strategy = AxisLabelStrategy.FULL_HORIZONTAL,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            titleRowHeightPx = titleRowHeightPx,
        )
    }

    private fun tryThinnedHorizontal(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
    ): AxisLabelPlan? {
        val count = displayLabels.size
        val steps = when (input.temporalMetadata?.granularity) {
            TemporalGranularity.MONTH -> MONTH_STEPS
            TemporalGranularity.DAY -> DAY_STEPS
            else -> STANDARD_STEPS
        }

        for (step in steps) {
            if (step >= count && count > 1) continue
            val candidates = selectThinnedIndices(
                count = count,
                step = step,
                displayLabels = displayLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
            )
            val plan = validateAndBuildHorizontalPlan(
                indices = candidates,
                originalLabels = originalLabels,
                displayLabels = displayLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                strategy = AxisLabelStrategy.THINNED_HORIZONTAL,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
            )
            if (plan != null) return plan
        }

        // Also try first + last only if count > 2 and no step fit
        if (count >= 2) {
            validateAndBuildHorizontalPlan(
                indices = listOf(0, count - 1),
                originalLabels = originalLabels,
                displayLabels = displayLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = effectiveMinGap,
                strategy = AxisLabelStrategy.THINNED_HORIZONTAL,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
            )?.let { return it }
        }
        return null
    }

    private fun selectThinnedIndices(
        count: Int,
        step: Int,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
    ): List<Int> {
        if (count <= 1) return listOf(0)
        val indices = (0 until count step step).toMutableList()
        val lastIndex = count - 1
        if (indices.last() != lastIndex) {
            val lastMetrics = input.textMeasurer.measure(displayLabels[lastIndex], input.fontSizePx, input.fontWeight)
            val lastRect = horizontalRect(
                centerPx = input.slotCentersPx[lastIndex],
                widthPx = lastMetrics.widthPx,
                heightPx = lastMetrics.heightPx,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
            )
            val prevIdx = indices.last()
            val prevMetrics = input.textMeasurer.measure(displayLabels[prevIdx], input.fontSizePx, input.fontWeight)
            val prevRect = horizontalRect(
                centerPx = input.slotCentersPx[prevIdx],
                widthPx = prevMetrics.widthPx,
                heightPx = prevMetrics.heightPx,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
            )
            if (lastRect != null && prevRect != null && lastRect.left - prevRect.right >= effectiveMinGap) {
                indices.add(lastIndex)
            } else if (indices.size >= 2) {
                // Remove preceding lower-priority label and check if last now fits
                indices.removeAt(indices.lastIndex)
                val newPrevIdx = indices.last()
                val newPrevMetrics = input.textMeasurer.measure(displayLabels[newPrevIdx], input.fontSizePx, input.fontWeight)
                val newPrevRect = horizontalRect(
                    centerPx = input.slotCentersPx[newPrevIdx],
                    widthPx = newPrevMetrics.widthPx,
                    heightPx = newPrevMetrics.heightPx,
                    canvasLeft = canvasLeft,
                    canvasRight = canvasRight,
                )
                if (lastRect != null && newPrevRect != null && lastRect.left - newPrevRect.right >= effectiveMinGap) {
                    indices.add(lastIndex)
                }
            }
        }
        return indices
    }

    private fun tryTruncatedHorizontal(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        isTimeSeries: Boolean,
    ): AxisLabelPlan? {
        val count = displayLabels.size
        val candidateLists = if (isTimeSeries) {
            buildList {
                if (count >= 2) add(listOf(0, count - 1))
                add(listOf(0))
            }
        } else {
            listOf(displayLabels.indices.toList())
        }

        for (indices in candidateLists) {
            val truncated = MutableList(count) { i -> displayLabels[i] }
            var allFit = true
            for (k in indices.indices) {
                val idx = indices[k]
                val leftLimit = if (k == 0) {
                    canvasLeft
                } else {
                    val prevIdx = indices[k - 1]
                    val prevW = input.textMeasurer.measure(truncated[prevIdx], input.fontSizePx, input.fontWeight).widthPx
                    val prevRect = horizontalRect(
                        centerPx = input.slotCentersPx[prevIdx],
                        widthPx = prevW,
                        heightPx = input.fontSizePx,
                        canvasLeft = canvasLeft,
                        canvasRight = canvasRight,
                    )
                    (prevRect?.right ?: canvasLeft) + effectiveMinGap
                }
                val rightLimit = if (k == indices.lastIndex) {
                    canvasRight
                } else {
                    val nextCenter = input.slotCentersPx[indices[k + 1]]
                    min(canvasRight, (input.slotCentersPx[idx] + nextCenter) * 0.5f - effectiveMinGap * 0.5f)
                }
                val maxAllowedWidth = min(
                    rightLimit - leftLimit,
                    min(
                        (input.slotCentersPx[idx] - leftLimit) * 2f,
                        (rightLimit - input.slotCentersPx[idx]) * 2f,
                    ).coerceAtLeast(rightLimit - leftLimit),
                )
                if (maxAllowedWidth <= 0f) {
                    allFit = false
                    break
                }
                val shortened = truncateToFit(
                    text = displayLabels[idx],
                    maxWidthPx = maxAllowedWidth,
                    fontSizePx = input.fontSizePx,
                    fontWeight = input.fontWeight,
                    textMeasurer = input.textMeasurer,
                )
                if (shortened.isEmpty()) {
                    allFit = false
                    break
                }
                // For categorical lists, only accept horizontal truncation when most of the label remains intact
                if (!isTimeSeries && count > 1) {
                    val origClusters = graphemeCount(displayLabels[idx])
                    val shortClusters = graphemeCount(shortened)
                    if (shortClusters <= 4 || (origClusters > 6 && shortClusters < (origClusters * 0.75f).toInt())) {
                        allFit = false
                        break
                    }
                }
                truncated[idx] = shortened
            }
            if (allFit) {
                validateAndBuildHorizontalPlan(
                    indices = indices,
                    originalLabels = originalLabels,
                    displayLabels = truncated,
                    input = input,
                    canvasLeft = canvasLeft,
                    canvasRight = canvasRight,
                    effectiveMinGap = effectiveMinGap,
                    strategy = AxisLabelStrategy.TRUNCATED_HORIZONTAL,
                    axisTitle = axisTitle,
                    secondaryYearLabel = secondaryYearLabel,
                    titleRowHeightPx = titleRowHeightPx,
                )?.let { return it }
            }
        }
        return null
    }

    private fun tryRotated45(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        allowEllipsis: Boolean = false,
        strategyOverride: AxisLabelStrategy = AxisLabelStrategy.ROTATED_45,
        abbreviationKey: List<CategoryAbbreviationEntry> = emptyList(),
    ): AxisLabelPlan? {
        val count = displayLabels.size
        if (count == 0) return null
        val items = ArrayList<AxisLabelItem>(count)
        var maxBottom = 0f

        val maxAllowedVerticalSpan = (input.fontSizePx * 9.8f).coerceAtLeast(88f)
        val maxDiagonalWidth = (maxAllowedVerticalSpan / SIN_45).coerceAtLeast(input.fontSizePx * 3f)
        val minPerpGap = max(2.5f, input.fontSizePx * 0.22f)
        val dynamicAbbreviationKey = abbreviationKey.toMutableList()
        val precomputedCodes by lazy {
            ChartAbbreviationResolver.resolve(
                originalLabels = originalLabels,
                maxAllowedWidthPx = input.fontSizePx * 4.5f,
                fontSizePx = input.fontSizePx,
                fontWeight = input.fontWeight,
                textMeasurer = input.textMeasurer,
                forceAbbreviateAll = true,
            )
        }

        for (i in 0 until count) {
            val tickX = input.slotCentersPx[i]
            val rawMetrics = input.textMeasurer.measure(displayLabels[i], input.fontSizePx, input.fontWeight)
            val halfH = rawMetrics.heightPx * SIN_45 * 0.5f

            // Check perpendicular ribbon spacing between parallel -45° labels
            if (i > 0) {
                val dx = tickX - input.slotCentersPx[i - 1]
                val perpDist = dx * SIN_45
                val requiredPerp = rawMetrics.heightPx * 0.88f + minPerpGap
                if (perpDist < requiredPerp) return null
            }

            // Leftmost boundary check: label extends left by width * COS_45 + halfH
            val maxLeftExtent = (tickX - canvasLeft).coerceAtLeast(0f)
            val maxRightExtent = (canvasRight - tickX).coerceAtLeast(0f)
            if (halfH > maxRightExtent + 1f) return null

            val maxFromLeftBound = ((maxLeftExtent - halfH) / COS_45).coerceAtLeast(0f)
            val maxLabelWidth = min(maxDiagonalWidth, maxFromLeftBound)

            val finalText = if (rawMetrics.widthPx <= maxLabelWidth + 0.5f) {
                displayLabels[i]
            } else if (allowEllipsis) {
                val code = precomputedCodes.displayLabels.getOrElse(i) { displayLabels[i] }
                val codeMetrics = input.textMeasurer.measure(code, input.fontSizePx, input.fontWeight)
                if (codeMetrics.widthPx <= maxLabelWidth + 0.5f && abbreviationKey.isEmpty()) {
                    if (dynamicAbbreviationKey.none { it.domainIndex == i }) {
                        dynamicAbbreviationKey.add(
                            CategoryAbbreviationEntry(
                                domainIndex = i,
                                shortCode = code,
                                fullLabel = originalLabels[i],
                            ),
                        )
                    }
                    code
                } else {
                    val truncated = truncateToFit(
                        text = displayLabels[i],
                        maxWidthPx = maxLabelWidth,
                        fontSizePx = input.fontSizePx,
                        fontWeight = input.fontWeight,
                        textMeasurer = input.textMeasurer,
                    )
                    if (graphemeCount(truncated) < 6 && graphemeCount(displayLabels[i]) > 6) {
                        return null
                    }
                    if (abbreviationKey.isEmpty() && dynamicAbbreviationKey.none { it.domainIndex == i }) {
                        dynamicAbbreviationKey.add(
                            CategoryAbbreviationEntry(
                                domainIndex = i,
                                shortCode = truncated,
                                fullLabel = originalLabels[i],
                            ),
                        )
                    }
                    truncated
                }
            } else {
                return null
            }
            if (finalText.isEmpty()) return null

            val m = input.textMeasurer.measure(finalText, input.fontSizePx, input.fontWeight)
            val boxLeft = (tickX - m.widthPx * COS_45 - m.heightPx * SIN_45 * 0.5f).coerceAtLeast(canvasLeft)
            val boxRight = (tickX + m.heightPx * SIN_45 * 0.5f).coerceAtMost(canvasRight)
            val boxHeight = m.widthPx * SIN_45 + m.heightPx * COS_45
            val rect = ChartRect(
                left = boxLeft,
                top = 0f,
                right = max(boxLeft + 1f, boxRight),
                bottom = boxHeight.coerceAtLeast(1f),
            )
            maxBottom = max(maxBottom, rect.bottom)
            items.add(
                AxisLabelItem(
                    originalIndex = i,
                    originalText = originalLabels[i],
                    displayText = finalText,
                    visible = true,
                    xPx = tickX,
                    rotationDegrees = ROTATION_ANGLE_DEGREES,
                    anchor = AxisLabelAnchor.END,
                    measuredWidthPx = m.widthPx,
                    measuredHeightPx = m.heightPx,
                    bounds = rect,
                ),
            )
        }

        val labelPad = 0.45f * input.fontSizePx
        return AxisLabelPlan(
            items = items,
            visibleIndices = items.indices.toList(),
            strategy = strategyOverride,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            requiredAxisHeightPx = maxBottom + titleRowHeightPx + labelPad,
            titleRowHeightPx = titleRowHeightPx,
            horizontalBarRecommended = false,
            effectiveMinGapPx = effectiveMinGap,
            abbreviationKey = dynamicAbbreviationKey,
        )
    }

    private fun tryRotated90(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        allowEllipsis: Boolean = false,
        strategyOverride: AxisLabelStrategy = AxisLabelStrategy.ROTATED_90,
        abbreviationKey: List<CategoryAbbreviationEntry> = emptyList(),
    ): AxisLabelPlan? {
        val count = displayLabels.size
        if (count == 0) return null
        val items = ArrayList<AxisLabelItem>(count)
        var maxBottom = 0f

        val maxVerticalSpan = (input.fontSizePx * 6.8f).coerceAtLeast(64f)
        val minHorizontalPitch = input.fontSizePx * 0.92f

        for (i in 0 until count) {
            val tickX = input.slotCentersPx[i]
            if (i > 0) {
                val dx = tickX - input.slotCentersPx[i - 1]
                if (dx < minHorizontalPitch) return null
            }

            val rawMetrics = input.textMeasurer.measure(displayLabels[i], input.fontSizePx, input.fontWeight)
            val halfLineH = rawMetrics.heightPx * 0.5f
            if (tickX - halfLineH < canvasLeft - 1f || tickX + halfLineH > canvasRight + 1f) {
                return null
            }

            val finalText = if (rawMetrics.widthPx <= maxVerticalSpan + 0.5f) {
                displayLabels[i]
            } else if (allowEllipsis) {
                val truncated = truncateToFit(
                    text = displayLabels[i],
                    maxWidthPx = maxVerticalSpan,
                    fontSizePx = input.fontSizePx,
                    fontWeight = input.fontWeight,
                    textMeasurer = input.textMeasurer,
                )
                if (graphemeCount(truncated) < 5 && graphemeCount(displayLabels[i]) > 5) {
                    return null
                }
                truncated
            } else {
                return null
            }
            if (finalText.isEmpty()) return null

            val m = input.textMeasurer.measure(finalText, input.fontSizePx, input.fontWeight)
            val rect = ChartRect(
                left = (tickX - m.heightPx * 0.5f).coerceAtLeast(canvasLeft),
                top = 0f,
                right = (tickX + m.heightPx * 0.5f).coerceAtMost(canvasRight),
                bottom = m.widthPx.coerceAtLeast(1f),
            )
            maxBottom = max(maxBottom, rect.bottom)
            items.add(
                AxisLabelItem(
                    originalIndex = i,
                    originalText = originalLabels[i],
                    displayText = finalText,
                    visible = true,
                    xPx = tickX,
                    rotationDegrees = ROTATION_90_ANGLE_DEGREES,
                    anchor = AxisLabelAnchor.END,
                    measuredWidthPx = m.widthPx,
                    measuredHeightPx = m.heightPx,
                    bounds = rect,
                ),
            )
        }

        val labelPad = 0.45f * input.fontSizePx
        return AxisLabelPlan(
            items = items,
            visibleIndices = items.indices.toList(),
            strategy = strategyOverride,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            requiredAxisHeightPx = maxBottom + titleRowHeightPx + labelPad,
            titleRowHeightPx = titleRowHeightPx,
            horizontalBarRecommended = false,
            effectiveMinGapPx = effectiveMinGap,
            abbreviationKey = abbreviationKey,
        )
    }

    private fun tryAbbreviatedWithKey(
        originalLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        forceAbbreviateAll: Boolean = false,
    ): AxisLabelPlan? {
        val count = originalLabels.size
        if (count == 0) return null

        val minSlotPitch = if (count >= 2) {
            (1 until count).minOf { idx ->
                (input.slotCentersPx[idx] - input.slotCentersPx[idx - 1]).coerceAtLeast(1f)
            }
        } else {
            canvasRight - canvasLeft
        }

        val maxHorizCodeWidth = (minSlotPitch - effectiveMinGap * 0.65f).coerceAtLeast(input.fontSizePx * 1.6f)
        val resolution = ChartAbbreviationResolver.resolve(
            originalLabels = originalLabels,
            maxAllowedWidthPx = maxHorizCodeWidth,
            fontSizePx = input.fontSizePx,
            fontWeight = input.fontWeight,
            textMeasurer = input.textMeasurer,
            forceAbbreviateAll = forceAbbreviateAll,
        )
        val shortLabels = resolution.displayLabels
        val key = resolution.referenceKey

        // 1. Try horizontal short codes first (e.g. ED, GM, GS, ONC horizontally under each bar)
        val compactGap = max(3f, min(effectiveMinGap, input.fontSizePx * 0.3f))
        validateAndBuildHorizontalPlan(
            indices = shortLabels.indices.toList(),
            originalLabels = originalLabels,
            displayLabels = shortLabels,
            input = input,
            canvasLeft = canvasLeft,
            canvasRight = canvasRight,
            effectiveMinGap = compactGap,
            strategy = AxisLabelStrategy.ABBREVIATED_HORIZONTAL,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            titleRowHeightPx = titleRowHeightPx,
            abbreviationKey = key,
        )?.let { return it }

        // 2. Try 45-degree or 90-degree rotated short codes using full 3-4 letter clinical codes
        if (input.allowRotation) {
            val rotatedResolution = ChartAbbreviationResolver.resolve(
                originalLabels = originalLabels,
                maxAllowedWidthPx = (input.fontSizePx * 4.5f).coerceAtLeast(maxHorizCodeWidth),
                fontSizePx = input.fontSizePx,
                fontWeight = input.fontWeight,
                textMeasurer = input.textMeasurer,
                forceAbbreviateAll = true,
            )
            val rotatedShortLabels = rotatedResolution.displayLabels
            val rotatedKey = rotatedResolution.referenceKey

            tryRotated45(
                originalLabels = originalLabels,
                displayLabels = rotatedShortLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = compactGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = false,
                strategyOverride = AxisLabelStrategy.ABBREVIATED_ROTATED_45,
                abbreviationKey = rotatedKey,
            )?.let { return it }

            // 3. Try 90-degree vertical short codes (ideal when 20–48 bars are fitted tightly)
            tryRotated90(
                originalLabels = originalLabels,
                displayLabels = rotatedShortLabels,
                input = input,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
                effectiveMinGap = compactGap,
                axisTitle = axisTitle,
                secondaryYearLabel = secondaryYearLabel,
                titleRowHeightPx = titleRowHeightPx,
                allowEllipsis = false,
                strategyOverride = AxisLabelStrategy.ABBREVIATED_ROTATED_90,
                abbreviationKey = rotatedKey,
            )?.let { return it }
        }

        return null
    }

    private fun buildSafeSingleFirstFallback(
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        horizontalBarRecommended: Boolean,
    ): AxisLabelPlan {
        val maxW = (canvasRight - canvasLeft).coerceAtLeast(0f)
        val firstText = truncateToFit(
            text = displayLabels.first(),
            maxWidthPx = maxW,
            fontSizePx = input.fontSizePx,
            fontWeight = input.fontWeight,
            textMeasurer = input.textMeasurer,
        ).ifEmpty { ELLIPSIS }
        val metrics = input.textMeasurer.measure(firstText, input.fontSizePx, input.fontWeight)
        val rect = horizontalRect(
            centerPx = input.slotCentersPx.first(),
            widthPx = min(metrics.widthPx, maxW),
            heightPx = metrics.heightPx,
            canvasLeft = canvasLeft,
            canvasRight = canvasRight,
        ) ?: ChartRect(canvasLeft, 0f, canvasLeft + maxW.coerceAtLeast(1f), metrics.heightPx.coerceAtLeast(1f))

        val items = originalLabels.mapIndexed { idx, orig ->
            if (idx == 0) {
                AxisLabelItem(
                    originalIndex = 0,
                    originalText = orig,
                    displayText = firstText,
                    visible = true,
                    xPx = rect.centerX,
                    rotationDegrees = 0f,
                    anchor = AxisLabelAnchor.CENTER,
                    measuredWidthPx = metrics.widthPx,
                    measuredHeightPx = metrics.heightPx,
                    bounds = rect,
                )
            } else {
                AxisLabelItem(
                    originalIndex = idx,
                    originalText = orig,
                    displayText = displayLabels[idx],
                    visible = false,
                    xPx = input.slotCentersPx[idx],
                    rotationDegrees = 0f,
                    anchor = AxisLabelAnchor.CENTER,
                    measuredWidthPx = 0f,
                    measuredHeightPx = 0f,
                    bounds = ChartRect(input.slotCentersPx[idx], 0f, input.slotCentersPx[idx], 0f),
                )
            }
        }
        val labelPad = 0.45f * input.fontSizePx
        return AxisLabelPlan(
            items = items,
            visibleIndices = listOf(0),
            strategy = AxisLabelStrategy.HORIZONTAL_BAR_RECOMMENDED,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            requiredAxisHeightPx = rect.height + titleRowHeightPx + labelPad,
            titleRowHeightPx = titleRowHeightPx,
            horizontalBarRecommended = horizontalBarRecommended,
            effectiveMinGapPx = effectiveMinGap,
        )
    }

    private fun validateAndBuildHorizontalPlan(
        indices: List<Int>,
        originalLabels: List<String>,
        displayLabels: List<String>,
        input: AxisLabelInput,
        canvasLeft: Float,
        canvasRight: Float,
        effectiveMinGap: Float,
        strategy: AxisLabelStrategy,
        axisTitle: String?,
        secondaryYearLabel: String?,
        titleRowHeightPx: Float,
        abbreviationKey: List<CategoryAbbreviationEntry> = emptyList(),
    ): AxisLabelPlan? {
        if (indices.isEmpty() || indices.first() != 0) return null
        val visibleSet = indices.toHashSet()
        val rectsByIndex = HashMap<Int, ChartRect>(indices.size)
        val metricsByIndex = HashMap<Int, AxisTextMetrics>(indices.size)
        var maxHeight = 0f

        var prevRight = Float.NEGATIVE_INFINITY
        for (idx in indices) {
            val text = displayLabels[idx]
            if (text.isEmpty()) return null
            val metrics = input.textMeasurer.measure(text, input.fontSizePx, input.fontWeight)
            if (!metrics.widthPx.isFinite() || !metrics.heightPx.isFinite() || metrics.widthPx <= 0f) {
                return null
            }
            val rect = horizontalRect(
                centerPx = input.slotCentersPx[idx],
                widthPx = metrics.widthPx,
                heightPx = metrics.heightPx,
                canvasLeft = canvasLeft,
                canvasRight = canvasRight,
            ) ?: return null

            if (prevRight.isFinite() && rect.left - prevRight < effectiveMinGap - 0.01f) {
                return null
            }
            prevRight = rect.right
            maxHeight = max(maxHeight, rect.height)
            rectsByIndex[idx] = rect
            metricsByIndex[idx] = metrics
        }

        val items = originalLabels.mapIndexed { idx, orig ->
            if (idx in visibleSet) {
                val rect = rectsByIndex.getValue(idx)
                val metrics = metricsByIndex.getValue(idx)
                AxisLabelItem(
                    originalIndex = idx,
                    originalText = orig,
                    displayText = displayLabels[idx],
                    visible = true,
                    xPx = rect.centerX,
                    rotationDegrees = 0f,
                    anchor = AxisLabelAnchor.CENTER,
                    measuredWidthPx = metrics.widthPx,
                    measuredHeightPx = metrics.heightPx,
                    bounds = rect,
                )
            } else {
                val cx = input.slotCentersPx[idx]
                AxisLabelItem(
                    originalIndex = idx,
                    originalText = orig,
                    displayText = displayLabels[idx],
                    visible = false,
                    xPx = cx,
                    rotationDegrees = 0f,
                    anchor = AxisLabelAnchor.CENTER,
                    measuredWidthPx = 0f,
                    measuredHeightPx = 0f,
                    bounds = ChartRect(cx, 0f, cx, 0f),
                )
            }
        }

        val labelPad = 0.45f * input.fontSizePx
        return AxisLabelPlan(
            items = items,
            visibleIndices = indices,
            strategy = strategy,
            axisTitle = axisTitle,
            secondaryYearLabel = secondaryYearLabel,
            requiredAxisHeightPx = maxHeight + titleRowHeightPx + labelPad,
            titleRowHeightPx = titleRowHeightPx,
            horizontalBarRecommended = false,
            effectiveMinGapPx = effectiveMinGap,
            abbreviationKey = abbreviationKey,
        )
    }

    private fun horizontalRect(
        centerPx: Float,
        widthPx: Float,
        heightPx: Float,
        canvasLeft: Float,
        canvasRight: Float,
    ): ChartRect? {
        if (!centerPx.isFinite() || !widthPx.isFinite() || !heightPx.isFinite()) return null
        val availableWidth = canvasRight - canvasLeft
        if (widthPx > availableWidth + 0.01f) return null
        val rawLeft = centerPx - widthPx * 0.5f
        val clampedLeft = rawLeft.coerceIn(canvasLeft, (canvasRight - widthPx).coerceAtLeast(canvasLeft))
        val rect = ChartRect(
            left = clampedLeft,
            top = 0f,
            right = clampedLeft + widthPx,
            bottom = heightPx.coerceAtLeast(1f),
        )
        return if (rect.isValid && rect.left >= canvasLeft - 0.01f && rect.right <= canvasRight + 0.01f) {
            rect
        } else {
            null
        }
    }

    private fun measureTitleHeight(
        axisTitle: String?,
        secondaryYearLabel: String?,
        input: AxisLabelInput,
    ): Float {
        var height = 0f
        if (!axisTitle.isNullOrBlank()) {
            val m = input.textMeasurer.measure(axisTitle, input.fontSizePx, input.fontWeight)
            height += m.heightPx + 0.35f * input.fontSizePx
        }
        if (!secondaryYearLabel.isNullOrBlank() && secondaryYearLabel != axisTitle) {
            val m = input.textMeasurer.measure(secondaryYearLabel, input.fontSizePx, input.fontWeight)
            height += m.heightPx + 0.25f * input.fontSizePx
        }
        return height
    }

    internal data class SimplifiedLabelsResult(
        val labels: List<String>,
        val axisTitle: String? = null,
        val secondaryYearLabel: String? = null,
        val detectedTemporal: Boolean = false,
    )

    internal fun simplifyLabels(
        labels: List<String>,
        temporalMetadata: AxisTemporalMetadata?,
    ): SimplifiedLabelsResult {
        if (labels.size < 2) return SimplifiedLabelsResult(labels)

        // 1. Check ISO dates (e.g. 2026-01-01 .. 2026-04-30)
        val dateMatches = labels.map { ISO_DATE_REGEX.matchEntire(it.trim()) }
        if (dateMatches.all { it != null }) {
            val years = dateMatches.map { it!!.groupValues[1] }.distinct()
            val concise = dateMatches.map { match ->
                val monthIdx = match!!.groupValues[2].toIntOrNull()?.minus(1) ?: 0
                val day = match.groupValues[3].toIntOrNull() ?: 1
                val monthStr = MONTH_NAMES.getOrElse(monthIdx) { match.groupValues[2] }
                "$monthStr $day"
            }
            val yearTitle = if (years.size == 1) years.first() else "${years.first()}–${years.last()}"
            return SimplifiedLabelsResult(
                labels = concise,
                axisTitle = yearTitle,
                secondaryYearLabel = yearTitle,
                detectedTemporal = true,
            )
        }

        // 2. Detect repeated prefix ending at separator (e.g. "Week 01".."Week 45", "සතිය 01".."සතිය 45")
        val trimmed = labels.map { it.trim() }
        if (trimmed.all { it.isNotEmpty() }) {
            val commonPrefix = longestCommonSeparatorPrefix(trimmed)
            if (commonPrefix != null) {
                val title = commonPrefix.trimEnd { it in SEPARATORS }
                val rawSuffixes = trimmed.map { it.substring(commonPrefix.length).trim() }
                if (title.isNotEmpty() && rawSuffixes.all { it.isNotEmpty() }) {
                    val allNumeric = rawSuffixes.all { NUMERIC_ONLY_REGEX.matches(it) }
                    val finalSuffixes = if (allNumeric) {
                        rawSuffixes.map { it.trimStart('0').ifEmpty { "0" } }
                    } else {
                        rawSuffixes
                    }
                    return SimplifiedLabelsResult(
                        labels = finalSuffixes,
                        axisTitle = title,
                        detectedTemporal = temporalMetadata?.isTemporal == true || allNumeric,
                    )
                }
            }
        }

        return SimplifiedLabelsResult(
            labels = labels,
            detectedTemporal = temporalMetadata?.isTemporal == true,
        )
    }

    private fun longestCommonSeparatorPrefix(labels: List<String>): String? {
        val first = labels.first()
        var lcpLen = first.length
        for (i in 1 until labels.size) {
            val s = labels[i]
            val limit = min(lcpLen, s.length)
            var j = 0
            while (j < limit && first[j] == s[j]) j++
            lcpLen = j
            if (lcpLen == 0) return null
        }
        // Walk backwards to a separator boundary so we never cut inside a word or Sinhala cluster
        var sepIdx = lcpLen - 1
        while (sepIdx >= 0 && first[sepIdx] !in SEPARATORS) {
            sepIdx--
        }
        if (sepIdx <= 0) return null
        return first.substring(0, sepIdx + 1)
    }

    /**
     * Truncates [text] at Unicode grapheme-cluster boundaries (preserving Sinhala combining signs
     * and virama+ZWJ conjuncts) with an ellipsis so its measured width fits within [maxWidthPx].
     */
    fun truncateToFit(
        text: String,
        maxWidthPx: Float,
        fontSizePx: Float,
        fontWeight: Int,
        textMeasurer: AxisTextMeasurer,
    ): String {
        if (text.isEmpty() || maxWidthPx <= 0f) return ""
        if (textMeasurer.measure(text, fontSizePx, fontWeight).widthPx <= maxWidthPx) {
            return text
        }
        val clusters = splitGraphemeClusters(text)
        if (clusters.isEmpty()) return ""
        val ellipsisWidth = textMeasurer.measure(ELLIPSIS, fontSizePx, fontWeight).widthPx
        if (ellipsisWidth > maxWidthPx) return ""

        var lo = 0
        var hi = clusters.size - 1
        var best = ""
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val candidate = if (mid == 0) "" else clusters.subList(0, mid).joinToString("") + ELLIPSIS
            val w = if (candidate.isEmpty()) 0f else textMeasurer.measure(candidate, fontSizePx, fontWeight).widthPx
            if (w <= maxWidthPx) {
                best = candidate
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }

    /**
     * Splits [text] into user-perceived grapheme clusters using [BreakIterator.getCharacterInstance]
     * reinforced with Sinhala/Indic virama+ZWJ conjunct and combining-mark preservation.
     */
    fun splitGraphemeClusters(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(text)
        val rawClusters = ArrayList<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            rawClusters.add(text.substring(start, end))
            start = end
            end = iterator.next()
        }
        if (rawClusters.size <= 1) return rawClusters

        // Merge any boundary that JVM BreakIterator might split inside a Sinhala conjunct or mark
        val merged = ArrayList<String>(rawClusters.size)
        for (chunk in rawClusters) {
            if (merged.isEmpty()) {
                merged.add(chunk)
                continue
            }
            val prev = merged.last()
            if (shouldMergeWithPrevious(prev, chunk)) {
                merged[merged.lastIndex] = prev + chunk
            } else {
                merged.add(chunk)
            }
        }
        return merged
    }

    private fun shouldMergeWithPrevious(prev: String, next: String): Boolean {
        if (prev.isEmpty() || next.isEmpty()) return false
        val nextCp = next.codePointAt(0)
        val prevLastCp = prev.codePointBefore(prev.length)

        // Never split before ZWJ/ZWNJ or Unicode combining marks
        if (nextCp == 0x200D || nextCp == 0x200C) return true
        val nextType = Character.getType(nextCp)
        if (nextType == Character.NON_SPACING_MARK.toInt() ||
            nextType == Character.COMBINING_SPACING_MARK.toInt() ||
            nextType == Character.ENCLOSING_MARK.toInt()
        ) {
            return true
        }

        // Sinhala dependent vowel signs / signs / virama (U+0D82..0D83, 0DCA, 0DCF..0DDF, 0DF2..0DF3)
        if (isSinhalaDependentSign(nextCp)) return true

        // Sinhala Virama (0DCA) + ZWJ (200D) + Sinhala consonant (conjuncts like ක්‍ර, ශ්‍රී, ව්‍යා)
        if (prevLastCp == 0x200D && prev.length >= 2) {
            val beforeZwj = prev.codePointBefore(prev.length - 1)
            if (beforeZwj == 0x0DCA && nextCp in 0x0D9A..0x0DC6) {
                return true
            }
        }
        return false
    }

    private fun isSinhalaDependentSign(cp: Int): Boolean =
        cp == 0x0D82 || cp == 0x0D83 || cp == 0x0DCA ||
            cp in 0x0DCF..0x0DDF || cp == 0x0DF2 || cp == 0x0DF3

    private fun graphemeCount(text: String): Int = splitGraphemeClusters(text).size
}

enum class AxisOrientation {
    HORIZONTAL,
    VERTICAL,
}

enum class TemporalGranularity {
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
}

data class AxisTemporalMetadata(
    val isTemporal: Boolean = false,
    val granularity: TemporalGranularity? = null,
    val isoDatesOrTimestamps: List<String?> = emptyList(),
)

enum class AxisLabelAnchor {
    START,
    CENTER,
    END,
}

enum class AxisLabelStrategy {
    FULL_HORIZONTAL,
    THINNED_HORIZONTAL,
    TRUNCATED_HORIZONTAL,
    ROTATED_45,
    ROTATED_90,
    ABBREVIATED_HORIZONTAL,
    ABBREVIATED_ROTATED_45,
    ABBREVIATED_ROTATED_90,
    HORIZONTAL_BAR_RECOMMENDED,
}

data class AxisTextMetrics(
    val widthPx: Float,
    val heightPx: Float,
)

interface AxisTextMeasurer {
    fun measure(
        text: String,
        fontSizePx: Float,
        fontWeight: Int = 400,
    ): AxisTextMetrics
}

data class AxisLabelInput(
    val labels: List<String>,
    val slotCentersPx: List<Float>,
    val axisStartPx: Float,
    val axisEndPx: Float,
    val fontSizePx: Float,
    val fontWeight: Int = 400,
    val minGapPx: Float = max(8f, 0.6f * fontSizePx),
    val textMeasurer: AxisTextMeasurer,
    val temporalMetadata: AxisTemporalMetadata? = null,
    val orientation: AxisOrientation = AxisOrientation.HORIZONTAL,
    val allowRotation: Boolean = true,
    val canvasLeftPx: Float = min(axisStartPx, axisEndPx),
    val canvasRightPx: Float = max(axisStartPx, axisEndPx),
    val preferredDisplayMode: ChartLabelDisplayMode = ChartLabelDisplayMode.AUTO,
)

data class AxisLabelItem(
    val originalIndex: Int,
    val originalText: String,
    val displayText: String,
    val visible: Boolean,
    val xPx: Float,
    val rotationDegrees: Float,
    val anchor: AxisLabelAnchor,
    val measuredWidthPx: Float,
    val measuredHeightPx: Float,
    val bounds: ChartRect,
)

data class AxisLabelPlan(
    val items: List<AxisLabelItem>,
    val visibleIndices: List<Int>,
    val strategy: AxisLabelStrategy,
    val axisTitle: String? = null,
    val secondaryYearLabel: String? = null,
    val requiredAxisHeightPx: Float,
    val titleRowHeightPx: Float,
    val horizontalBarRecommended: Boolean,
    val effectiveMinGapPx: Float,
    val abbreviationKey: List<CategoryAbbreviationEntry> = emptyList(),
)
