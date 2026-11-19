package com.pasindu.nursingotapp.statgraph.chart.toolkit

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class AxisScaleConfig(
    val includeZeroBaseline: Boolean = true,
    val paddingFraction: Double = 0.08,
    val desiredTickCount: Int = 5
) {
    init {
        require(desiredTickCount >= 2) { "desiredTickCount must be at least 2." }
        require(paddingFraction >= 0.0 && paddingFraction.isFinite()) { "paddingFraction must be finite and >= 0." }
    }
}

/**
 * Pure-Kotlin linear numerical scale transforming between data domain values and pixel coordinates.
 */
data class NumericScale(
    val domainMin: Double,
    val domainMax: Double,
    val rangeStart: Float,
    val rangeEnd: Float,
    val ticks: List<Double>,
    val formattedTicks: List<String>,
    val isZeroAnchored: Boolean,
    val isTruncated: Boolean
) {
    val span: Double get() = domainMax - domainMin

    fun normalize(value: Double): Double? {
        if (!value.isFinite() || span <= 0.0 || !span.isFinite()) return null
        val t = (value - domainMin) / span
        return t.takeIf { it.isFinite() }
    }

    fun toPixel(value: Double): Float? {
        val norm = normalize(value) ?: return null
        val pixel = rangeStart + (norm * (rangeEnd - rangeStart).toDouble())
        return if (pixel.isFinite()) pixel.toFloat() else null
    }

    fun fromPixel(pixel: Float): Double? {
        if (!pixel.isFinite() || !rangeStart.isFinite() || !rangeEnd.isFinite()) return null
        val pixelSpan = (rangeEnd - rangeStart).toDouble()
        if (pixelSpan == 0.0) return null
        val norm = (pixel.toDouble() - rangeStart.toDouble()) / pixelSpan
        val value = domainMin + (norm * span)
        return value.takeIf { it.isFinite() }
    }

    fun zeroLinePixel(): Float? {
        if (0.0 < domainMin || 0.0 > domainMax) return null
        return toPixel(0.0)
    }

    companion object {
        /**
         * Builds a linear [NumericScale] with nice Heckbert-style ticks, safe bounds for
         * empty/constant/extreme inputs, and honest zero-baseline tracking.
         */
        fun create(
            values: List<Double?>,
            rangeStart: Float,
            rangeEnd: Float,
            config: AxisScaleConfig = AxisScaleConfig(),
            minOverride: Double? = null,
            maxOverride: Double? = null
        ): NumericScale {
            val clean = values.mapNotNull { it?.takeIf { v -> v.isFinite() } }

            val rawMin = minOverride?.takeIf { it.isFinite() } ?: clean.minOrNull() ?: 0.0
            val rawMax = maxOverride?.takeIf { it.isFinite() } ?: clean.maxOrNull() ?: 1.0

            var effMin = minOf(rawMin, rawMax)
            var effMax = maxOf(rawMin, rawMax)

            if (config.includeZeroBaseline) {
                effMin = minOf(effMin, 0.0)
                effMax = maxOf(effMax, 0.0)
            }

            if (effMin == effMax) {
                if (effMax == 0.0) {
                    effMin = 0.0
                    effMax = 1.0
                } else {
                    val delta = abs(effMax) * 0.15
                    if (config.includeZeroBaseline) {
                        if (effMax > 0.0) effMax += delta else effMin -= delta
                    } else {
                        effMin -= delta
                        effMax += delta
                    }
                }
            } else if (!config.includeZeroBaseline && config.paddingFraction > 0.0) {
                val pad = (effMax - effMin) * config.paddingFraction
                effMin -= pad
                effMax += pad
            } else if (config.includeZeroBaseline && effMax > 0.0 && maxOverride == null) {
                // Slight headroom above top bar/peak while locking bottom strictly at 0 if >= 0
                effMax += (effMax - effMin) * (config.paddingFraction * 0.5)
            }

            val tickResult = AxisTickCalculator.computeNiceTicks(
                min = effMin,
                max = effMax,
                desiredCount = config.desiredTickCount,
                preserveZeroMin = config.includeZeroBaseline && effMin >= 0.0
            )

            val isTruncated = tickResult.niceMin > 0.0 || tickResult.niceMax < 0.0
            val formatted = tickResult.ticks.map { AxisTickCalculator.formatTick(it, tickResult.step) }

            return NumericScale(
                domainMin = tickResult.niceMin,
                domainMax = tickResult.niceMax,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                ticks = tickResult.ticks,
                formattedTicks = formatted,
                isZeroAnchored = tickResult.niceMin <= 0.0 && tickResult.niceMax >= 0.0,
                isTruncated = isTruncated
            )
        }
    }
}

data class NiceTicksResult(
    val niceMin: Double,
    val niceMax: Double,
    val step: Double,
    val ticks: List<Double>
)

/**
 * Pure-Kotlin Heckbert nice-tick generator supporting tiny decimals, counts, and large integers.
 */
object AxisTickCalculator {

    fun computeNiceTicks(
        min: Double,
        max: Double,
        desiredCount: Int = 5,
        preserveZeroMin: Boolean = false
    ): NiceTicksResult {
        val safeMin = if (min.isFinite()) min else 0.0
        val safeMax = if (max.isFinite() && max > safeMin) max else safeMin + 1.0
        val rawRange = niceNumber(safeMax - safeMin, round = false)
        val tickStep = niceNumber(rawRange / (desiredCount - 1).coerceAtLeast(1), round = true)
            .takeIf { it > 0.0 && it.isFinite() } ?: 1.0

        val niceMinRaw = floor(safeMin / tickStep) * tickStep
        val niceMin = if (preserveZeroMin && safeMin >= 0.0) 0.0 else cleanZero(niceMinRaw)
        val niceMax = cleanZero(ceil(safeMax / tickStep) * tickStep).let { candidate ->
            if (candidate <= niceMin) cleanZero(niceMin + tickStep) else candidate
        }

        val ticks = mutableListOf<Double>()
        var current = niceMin
        var guard = 0
        val epsilon = tickStep * 1e-6
        while (current <= niceMax + epsilon && guard < 32) {
            ticks.add(cleanZero(current))
            current += tickStep
            guard++
        }

        if (ticks.size < 2) {
            ticks.clear()
            ticks.add(niceMin)
            ticks.add(niceMax)
        }

        return NiceTicksResult(
            niceMin = ticks.first(),
            niceMax = ticks.last(),
            step = tickStep,
            ticks = ticks
        )
    }

    fun formatTick(value: Double, step: Double): String {
        val cleaned = cleanZero(value)
        if (!cleaned.isFinite()) return "—"
        if (cleaned == 0.0) return "0"

        val absVal = abs(cleaned)
        if (absVal >= 1_000_000.0) {
            val m = cleaned / 1_000_000.0
            return formatCompactDecimal(m, 1) + "M"
        }
        if (absVal >= 10_000.0 && cleaned % 1000.0 == 0.0) {
            val k = cleaned / 1_000.0
            return formatCompactDecimal(k, 1) + "k"
        }
        if (cleaned % 1.0 == 0.0) {
            return String.format(Locale.US, "%,d", cleaned.roundToLong())
        }
        val decimals = when {
            step < 0.01 -> 3
            step < 0.1 -> 2
            step < 1.0 -> 2
            else -> 1
        }
        return formatCompactDecimal(cleaned, decimals)
    }

    private fun niceNumber(range: Double, round: Boolean): Double {
        if (range <= 0.0 || !range.isFinite()) return 1.0
        val exponent = floor(log10(range))
        val fraction = range / 10.0.pow(exponent)
        val niceFraction = if (round) {
            when {
                fraction < 1.5 -> 1.0
                fraction < 3.0 -> 2.0
                fraction < 7.0 -> 5.0
                else -> 10.0
            }
        } else {
            when {
                fraction <= 1.0 -> 1.0
                fraction <= 2.0 -> 2.0
                fraction <= 5.0 -> 5.0
                else -> 10.0
            }
        }
        return niceFraction * 10.0.pow(exponent)
    }

    private fun cleanZero(v: Double): Double = if (abs(v) < 1e-12) 0.0 else v

    private fun formatCompactDecimal(value: Double, maxDecimals: Int): String {
        val pattern = "%,.${maxDecimals.coerceIn(0, 4)}f"
        val raw = String.format(Locale.US, pattern, value)
        return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }
}

/**
 * Pure-Kotlin categorical/band scale mapping discrete domain indices cleanly across pixel range.
 */
data class CategoricalScale(
    val labels: List<String>,
    val rangeStart: Float,
    val rangeEnd: Float,
    val innerPaddingFraction: Float = 0.25f
) {
    val count: Int get() = labels.size
    val totalSpan: Float get() = (rangeEnd - rangeStart).coerceAtLeast(0f)

    val stepWidth: Float
        get() = if (count <= 0) 0f else totalSpan / count.toFloat()

    val bandWidth: Float
        get() = (stepWidth * (1f - innerPaddingFraction.coerceIn(0f, 0.8f))).coerceAtLeast(1f)

    fun bandCenter(index: Int): Float? {
        if (index !in 0 until count || totalSpan <= 0f) return null
        return rangeStart + (index + 0.5f) * stepWidth
    }

    fun bandLeft(index: Int): Float? {
        val center = bandCenter(index) ?: return null
        return center - (bandWidth / 2f)
    }

    /**
     * Maps continuous point-to-point domain index onto `[rangeStart, rangeEnd]` cleanly when
     * `count == 1` (centered) or `count >= 2` (band centers or edge-inset steps).
     */
    fun pointX(index: Int): Float? = bandCenter(index)

    fun nearestIndex(pixelX: Float): Int? {
        if (count <= 0 || !pixelX.isFinite() || totalSpan <= 0f) return null
        val clamped = pixelX.coerceIn(rangeStart, rangeEnd)
        val rawIdx = ((clamped - rangeStart) / stepWidth).toInt()
        return rawIdx.coerceIn(0, count - 1)
    }
}

/**
 * Pure-Kotlin label collision & tooltip bounds helpers.
 */
object LabelCollisionSolver {

    /**
     * Selects evenly spaced domain indices preserving both first and last indices whenever both
     * fit within `maxLabels = floor(availableWidth / minSlotWidth)` and minimum slot spacing.
     * Never exceeds `maxLabels`, keeps results strictly increasing and unique, and handles
     * empty, single-item, and tiny viewports safely.
     */
    fun selectVisibleIndices(
        totalCount: Int,
        availableWidth: Float,
        minSlotWidth: Float
    ): List<Int> {
        if (totalCount <= 0) return emptyList()
        if (totalCount == 1) return listOf(0)
        if (!availableWidth.isFinite() || !minSlotWidth.isFinite() ||
            availableWidth <= 0f || minSlotWidth <= 0f
        ) {
            return listOf(0)
        }

        val maxLabels = floor(availableWidth.toDouble() / minSlotWidth.toDouble()).toInt().coerceAtLeast(1)
        if (maxLabels == 1) return listOf(0)
        if (maxLabels >= totalCount) return (0 until totalCount).toList()

        val lastIdx = totalCount - 1
        val minIndexStep = ceil((minSlotWidth.toDouble() * totalCount.toDouble() / availableWidth.toDouble()) - 1e-6)
            .toInt()
            .coerceAtLeast(1)
        val maxFittingWithStep = 1 + (lastIdx / minIndexStep)
        val targetCount = minOf(maxLabels, maxFittingWithStep)
        if (targetCount <= 1) return listOf(0)

        val stride = lastIdx.toDouble() / (targetCount - 1).toDouble()
        return (0 until targetCount).map { slot ->
            (slot.toDouble() * stride).roundToInt().coerceIn(0, lastIdx)
        }
    }

    /**
     * Clamps tooltip box coordinates so it stays completely inside [container] without covering
     * the tapped point [anchorX], [anchorY].
     */
    fun clampTooltipPosition(
        anchorX: Float,
        anchorY: Float,
        tooltipWidth: Float,
        tooltipHeight: Float,
        container: ChartRect,
        gap: Float = 12f
    ): ChartPoint {
        val preferredX = if (anchorX + gap + tooltipWidth <= container.right) {
            anchorX + gap
        } else {
            anchorX - gap - tooltipWidth
        }
        val clampedX = preferredX.coerceIn(
            container.left,
            (container.right - tooltipWidth).coerceAtLeast(container.left)
        )

        val preferredY = if (anchorY - gap - tooltipHeight >= container.top) {
            anchorY - gap - tooltipHeight
        } else {
            anchorY + gap
        }
        val clampedY = preferredY.coerceIn(
            container.top,
            (container.bottom - tooltipHeight).coerceAtLeast(container.top)
        )

        return ChartPoint(x = clampedX, y = clampedY)
    }
}
