package com.pasindu.nursingotapp.statgraph.stats

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

enum class StatComputationStatus {
    VALID,
    INSUFFICIENT_OBSERVATIONS,
    ZERO_VARIANCE,
    DIVISION_BY_ZERO,
    INVALID_PARAMETER
}

data class ModeResult(
    val modes: List<Double>,
    val frequency: Int,
    val status: StatComputationStatus
)

data class QuartileResult(
    val q1: Double,
    val q2Median: Double,
    val q3: Double,
    val iqr: Double
)

data class CorrelationResult(
    val coefficient: Double?,
    val pairedCount: Int,
    val status: StatComputationStatus,
    val interpretation: String
)

data class FieldDescriptiveSummary(
    val fieldId: String,
    val fieldName: String,
    val unit: String,
    val totalRows: Int,
    val validCount: Int,
    val missingCount: Int,
    val sum: Double?,
    val mean: Double?,
    val median: Double?,
    val mode: ModeResult,
    val min: Double?,
    val max: Double?,
    val range: Double?,
    val sampleVariance: Double?,
    val populationVariance: Double?,
    val sampleStdDev: Double?,
    val populationStdDev: Double?,
    val quartiles: QuartileResult?,
    val skewness: Double?,
    val distinctCount: Int,
    val smallCountRows: Int,
    val negativeCount: Int
)

/**
 * Pure-Kotlin statistical computation engine.
 *
 * Guarantees:
 * - Missing values (`null` or non-finite) are excluded rather than coerced to zero.
 * - Sample variance/SD (`n - 1`) is explicitly separated from population variance/SD (`n`).
 * - Division by zero and insufficient observations return explicit `null`/status without `NaN` or `Infinity`.
 */
object StatisticsEngine {

    fun extractNumericSeries(dataset: StatDataset, fieldId: String): List<Double?> =
        dataset.rows.map { row ->
            row.values[fieldId].asDoubleOrNull()?.takeIf { it.isFinite() }
        }

    fun validValues(values: List<Double?>): List<Double> =
        values.mapNotNull { it?.takeIf { v -> v.isFinite() } }

    fun count(values: List<Double?>): Int = validValues(values).size

    fun missingCount(values: List<Double?>): Int = values.size - count(values)

    fun sum(values: List<Double?>): Double? {
        val clean = validValues(values)
        if (clean.isEmpty()) return null
        val total = clean.sum()
        return total.takeIf { it.isFinite() }
    }

    fun mean(values: List<Double?>): Double? {
        val clean = validValues(values)
        if (clean.isEmpty()) return null
        val result = clean.sum() / clean.size.toDouble()
        return result.takeIf { it.isFinite() }
    }

    fun median(values: List<Double?>): Double? = percentile(values, 50.0)

    fun mode(values: List<Double?>): ModeResult {
        val clean = validValues(values)
        if (clean.isEmpty()) {
            return ModeResult(emptyList(), 0, StatComputationStatus.INSUFFICIENT_OBSERVATIONS)
        }
        val frequencies = linkedMapOf<Double, Int>()
        clean.forEach { v ->
            frequencies[v] = (frequencies[v] ?: 0) + 1
        }
        val maxFreq = frequencies.values.maxOrNull() ?: 0
        if (maxFreq <= 1 && clean.size > 1) {
            return ModeResult(emptyList(), maxFreq, StatComputationStatus.VALID)
        }
        val modes = frequencies.filterValues { it == maxFreq }.keys.sorted()
        return ModeResult(modes = modes, frequency = maxFreq, status = StatComputationStatus.VALID)
    }

    fun minimum(values: List<Double?>): Double? = validValues(values).minOrNull()

    fun maximum(values: List<Double?>): Double? = validValues(values).maxOrNull()

    fun range(values: List<Double?>): Double? {
        val minVal = minimum(values) ?: return null
        val maxVal = maximum(values) ?: return null
        val diff = maxVal - minVal
        return diff.takeIf { it.isFinite() }
    }

    /**
     * Computes variance.
     *
     * @param population When `false` (default), computes unbiased sample variance using `n - 1`
     * and requires at least 2 valid observations. When `true`, computes population variance
     * using `n` and requires at least 1 valid observation.
     */
    fun variance(values: List<Double?>, population: Boolean = false): Double? {
        val clean = validValues(values)
        if (population) {
            if (clean.isEmpty()) return null
        } else {
            if (clean.size < 2) return null
        }
        val avg = clean.sum() / clean.size.toDouble()
        val sumSq = clean.sumOf { (it - avg) * (it - avg) }
        val divisor = if (population) clean.size.toDouble() else (clean.size - 1).toDouble()
        val result = sumSq / divisor
        return result.takeIf { it.isFinite() }
    }

    fun standardDeviation(values: List<Double?>, population: Boolean = false): Double? {
        val v = variance(values, population) ?: return null
        val sd = sqrt(v)
        return sd.takeIf { it.isFinite() }
    }

    fun quartiles(values: List<Double?>): QuartileResult? {
        val q1 = percentile(values, 25.0) ?: return null
        val q2 = percentile(values, 50.0) ?: return null
        val q3 = percentile(values, 75.0) ?: return null
        val iqr = (q3 - q1).takeIf { it.isFinite() } ?: return null
        return QuartileResult(q1 = q1, q2Median = q2, q3 = q3, iqr = iqr)
    }

    fun interquartileRange(values: List<Double?>): Double? = quartiles(values)?.iqr

    /**
     * Linear-interpolated percentile (R-7 / Excel PERCENTILE.INC) for `p` in `0.0..100.0`.
     */
    fun percentile(values: List<Double?>, p: Double): Double? {
        if (!p.isFinite() || p < 0.0 || p > 100.0) return null
        val sorted = validValues(values).sorted()
        if (sorted.isEmpty()) return null
        if (sorted.size == 1) return sorted.first()

        val rank = (p / 100.0) * (sorted.size - 1)
        val lowerIdx = rank.toInt()
        val upperIdx = (lowerIdx + 1).coerceAtMost(sorted.lastIndex)
        val fraction = rank - lowerIdx
        val interpolated = sorted[lowerIdx] + fraction * (sorted[upperIdx] - sorted[lowerIdx])
        return interpolated.takeIf { it.isFinite() }
    }

    fun proportion(part: Double?, total: Double?): Double? {
        if (part == null || total == null || !part.isFinite() || !total.isFinite() || total == 0.0) {
            return null
        }
        val ratio = part / total
        return ratio.takeIf { it.isFinite() }
    }

    fun percentage(part: Double?, total: Double?): Double? =
        proportion(part, total)?.let { (it * 100.0).takeIf { p -> p.isFinite() } }

    fun ratePerThousand(part: Double?, total: Double?): Double? =
        proportion(part, total)?.let { (it * 1000.0).takeIf { r -> r.isFinite() } }

    fun movingAverage(
        values: List<Double?>,
        windowSize: Int,
        requireFullWindow: Boolean = false
    ): List<Double?> {
        val safeWindow = windowSize.coerceAtLeast(1)
        return values.indices.map { idx ->
            val current = values[idx]?.takeIf { it.isFinite() } ?: return@map null
            val startIdx = (idx - safeWindow + 1).coerceAtLeast(0)
            val windowSlice = (startIdx..idx).mapNotNull { i ->
                values[i]?.takeIf { it.isFinite() }
            }
            if (requireFullWindow && windowSlice.size < safeWindow) {
                null
            } else {
                // current is guaranteed finite so windowSlice is non-empty
                val avg = (windowSlice.sum() / windowSlice.size.toDouble())
                avg.takeIf { it.isFinite() && current.isFinite() }
            }
        }
    }

    fun cumulativeTotals(values: List<Double?>): List<Double?> {
        var running = 0.0
        return values.map { value ->
            val clean = value?.takeIf { it.isFinite() } ?: return@map null
            running += clean
            running.takeIf { it.isFinite() }
        }
    }

    /**
     * Computes Pearson correlation coefficient across pairwise complete observations.
     */
    fun pearsonCorrelation(xValues: List<Double?>, yValues: List<Double?>): CorrelationResult {
        val pairCountLimit = minOf(xValues.size, yValues.size)
        val paired = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until pairCountLimit) {
            val x = xValues[i]?.takeIf { it.isFinite() }
            val y = yValues[i]?.takeIf { it.isFinite() }
            if (x != null && y != null) {
                paired.add(x to y)
            }
        }

        if (paired.size < 3) {
            return CorrelationResult(
                coefficient = null,
                pairedCount = paired.size,
                status = StatComputationStatus.INSUFFICIENT_OBSERVATIONS,
                interpretation = "At least 3 paired observations are required for correlation (found ${paired.size})."
            )
        }

        val n = paired.size.toDouble()
        val meanX = paired.sumOf { it.first } / n
        val meanY = paired.sumOf { it.second } / n

        var num = 0.0
        var sumSqX = 0.0
        var sumSqY = 0.0
        for ((x, y) in paired) {
            val dx = x - meanX
            val dy = y - meanY
            num += dx * dy
            sumSqX += dx * dx
            sumSqY += dy * dy
        }

        val denom = sqrt(sumSqX * sumSqY)
        if (!denom.isFinite() || denom == 0.0) {
            return CorrelationResult(
                coefficient = null,
                pairedCount = paired.size,
                status = StatComputationStatus.ZERO_VARIANCE,
                interpretation = "Correlation is undefined because one or both variables have zero variance."
            )
        }

        val r = (num / denom).coerceIn(-1.0, 1.0)
        if (!r.isFinite()) {
            return CorrelationResult(
                coefficient = null,
                pairedCount = paired.size,
                status = StatComputationStatus.DIVISION_BY_ZERO,
                interpretation = "Correlation could not be computed safely."
            )
        }

        val absR = abs(r)
        val strength = when {
            absR >= 0.8 -> "Strong"
            absR >= 0.5 -> "Moderate"
            absR >= 0.3 -> "Weak"
            else -> "Negligible"
        }
        val direction = if (r >= 0) "positive" else "negative"

        return CorrelationResult(
            coefficient = r,
            pairedCount = paired.size,
            status = StatComputationStatus.VALID,
            interpretation = "$strength $direction linear association (r = ${formatNumber(r)}, n = ${paired.size})."
        )
    }

    /**
     * Computes adjusted Fisher-Pearson sample skewness (requires n >= 3 and non-zero SD).
     */
    fun skewness(values: List<Double?>): Double? {
        val clean = validValues(values)
        val n = clean.size
        if (n < 3) return null
        val avg = clean.sum() / n.toDouble()
        val sd = standardDeviation(clean, population = false) ?: return null
        if (sd == 0.0) return null
        val sumCubedZ = clean.sumOf { ((it - avg) / sd).pow(3) }
        val factor = n.toDouble() / ((n - 1).toDouble() * (n - 2).toDouble())
        val skew = factor * sumCubedZ
        return skew.takeIf { it.isFinite() }
    }

    fun summarizeField(dataset: StatDataset, field: StatField): FieldDescriptiveSummary {
        val series = if (field.role.isNumeric) extractNumericSeries(dataset, field.id) else emptyList()
        val clean = validValues(series)
        val validRowCount = if (field.role.isNumeric) {
            clean.size
        } else {
            dataset.rows.count { row -> !row.values[field.id].isMissingValue() }
        }
        val missingRowCount = (dataset.rows.size - validRowCount).coerceAtLeast(0)
        val distinct = if (field.role.isNumeric) {
            clean.distinct().size
        } else {
            dataset.rows.mapNotNull { row ->
                val v = row.values[field.id]
                if (v.isMissingValue()) null
                else when (v) {
                    is StatValue.TextValue -> v.value.trim()
                    is StatValue.TimePeriodValue -> v.period?.formatIso() ?: v.rawText.trim()
                    else -> v.toString()
                }
            }.distinct().size
        }

        val smallCountRows = if (field.role is FieldRole.Measure || field.role is FieldRole.PartOfMeasure) {
            clean.count { it > 0.0 && it < 5.0 }
        } else {
            0
        }

        return FieldDescriptiveSummary(
            fieldId = field.id,
            fieldName = field.name,
            unit = field.unit,
            totalRows = dataset.rows.size,
            validCount = validRowCount,
            missingCount = missingRowCount,
            sum = sum(series),
            mean = mean(series),
            median = median(series),
            mode = mode(series),
            min = minimum(series),
            max = maximum(series),
            range = range(series),
            sampleVariance = variance(series, population = false),
            populationVariance = variance(series, population = true),
            sampleStdDev = standardDeviation(series, population = false),
            populationStdDev = standardDeviation(series, population = true),
            quartiles = quartiles(series),
            skewness = skewness(series),
            distinctCount = distinct,
            smallCountRows = smallCountRows,
            negativeCount = clean.count { it < 0.0 }
        )
    }

    fun formatNumber(value: Double?, decimals: Int = 2): String {
        if (value == null || !value.isFinite()) return "—"
        if (value % 1.0 == 0.0) {
            return String.format(Locale.US, "%,d", value.toLong())
        }
        val pattern = "%,.${decimals.coerceIn(0, 4)}f"
        val formatted = String.format(Locale.US, pattern, value)
        return if (formatted.contains('.')) formatted.trimEnd('0').trimEnd('.') else formatted
    }
}
