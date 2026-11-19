package com.pasindu.nursingotapp.statgraph.stats

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Pure-Kotlin statistical algorithms for Batch 1B charts.
 */
object Batch1BStatisticalMath {

    // -------------------------------------------------------------------------
    // 1. Waffle Chart (Hamilton / Largest Remainder Apportionment)
    // -------------------------------------------------------------------------

    data class WaffleAllocationItem(
        val label: String,
        val rawValue: Double,
        val percent: Double,
        val cells: Int
    )

    data class WaffleAllocationResult(
        val items: List<WaffleAllocationItem>,
        val totalValue: Double,
        val totalCells: Int
    )

    /**
     * Apportions [totalCells] (default 100) across categories using Hamilton's
     * Largest Remainder Method (Balinski & Young, 1982) so cell counts are exact
     * integers summing to [totalCells].
     */
    fun computeWaffleAllocation(
        items: List<Pair<String, Double>>,
        totalCells: Int = 100
    ): WaffleAllocationResult {
        if (items.isEmpty() || totalCells <= 0) {
            return WaffleAllocationResult(emptyList(), 0.0, totalCells)
        }
        val clean = items.filter { it.second.isFinite() && it.second >= 0.0 }
        val total = clean.sumOf { it.second }
        if (total <= 0.0) {
            return WaffleAllocationResult(emptyList(), 0.0, totalCells)
        }

        data class Working(
            val index: Int,
            val label: String,
            val rawValue: Double,
            val exactQuota: Double,
            var floorCells: Int,
            val remainder: Double
        )

        val working = clean.mapIndexed { idx, (label, value) ->
            val quota = (value / total) * totalCells
            val floored = floor(quota).toInt()
            Working(
                index = idx,
                label = label,
                rawValue = value,
                exactQuota = quota,
                floorCells = floored,
                remainder = quota - floored
            )
        }.toMutableList()

        val remaining = totalCells - working.sumOf { it.floorCells }
        if (remaining > 0) {
            working.sortedWith(
                compareByDescending<Working> { it.remainder }
                    .thenByDescending { it.rawValue }
                    .thenBy { it.index }
            ).take(remaining).forEach { it.floorCells += 1 }
        }

        val allocated = working.sortedBy { it.index }.map {
            WaffleAllocationItem(
                label = it.label,
                rawValue = it.rawValue,
                percent = (it.rawValue / total) * 100.0,
                cells = it.floorCells
            )
        }
        return WaffleAllocationResult(allocated, total, totalCells)
    }

    // -------------------------------------------------------------------------
    // 2. Pareto Chart
    // -------------------------------------------------------------------------

    data class ParetoItem(
        val label: String,
        val value: Double,
        val percent: Double,
        val cumulativeValue: Double,
        val cumulativePercent: Double
    )

    data class ParetoResult(
        val items: List<ParetoItem>,
        val totalValue: Double,
        val hasNegativeValues: Boolean = false
    )

    /**
     * Computes Pareto-ordered bars (descending by value, preserving tie order)
     * and cumulative percentage curve. Rejects negative values because Pareto
     * cumulative percentages require non-negative additive counts/costs.
     */
    fun computePareto(items: List<Pair<String, Double>>): ParetoResult {
        if (items.isEmpty()) return ParetoResult(emptyList(), 0.0)
        val finite = items.filter { it.second.isFinite() }
        if (finite.any { it.second < 0.0 }) {
            return ParetoResult(emptyList(), 0.0, hasNegativeValues = true)
        }
        val total = finite.sumOf { it.second }
        if (total <= 0.0) return ParetoResult(emptyList(), 0.0)

        val sorted = finite.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<Pair<String, Double>>> { it.value.second }
                    .thenBy { it.index }
            )
            .map { it.value }

        var running = 0.0
        val result = sorted.map { (label, value) ->
            running += value
            ParetoItem(
                label = label,
                value = value,
                percent = (value / total) * 100.0,
                cumulativeValue = running,
                cumulativePercent = (running / total) * 100.0
            )
        }
        return ParetoResult(result, total)
    }

    // -------------------------------------------------------------------------
    // 3. Empirical Cumulative Distribution Function (ECDF)
    // -------------------------------------------------------------------------

    data class EcdfPoint(
        val x: Double,
        val cumulativeProportion: Double,
        val cumulativeCount: Int
    )

    data class EcdfResult(
        val points: List<EcdfPoint>,
        val n: Int,
        val min: Double,
        val max: Double,
        val median: Double
    )

    /**
     * Computes the right-continuous empirical CDF:
     *   F_n(x) = (1 / n) * sum_{i=1..n} I(X_i <= x)
     * Ties are collapsed to a single jump at each distinct value so the step
     * height reflects the exact multiplicity of tied observations.
     */
    fun computeEcdf(values: List<Double>): EcdfResult {
        val sorted = values.filter { it.isFinite() }.sorted()
        val n = sorted.size
        if (n == 0) {
            return EcdfResult(emptyList(), 0, 0.0, 0.0, 0.0)
        }

        val points = mutableListOf<EcdfPoint>()
        var i = 0
        while (i < n) {
            val currentX = sorted[i]
            var j = i
            while (j < n && sorted[j] == currentX) {
                j++
            }
            points += EcdfPoint(
                x = currentX,
                cumulativeProportion = j.toDouble() / n.toDouble(),
                cumulativeCount = j
            )
            i = j
        }

        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        return EcdfResult(
            points = points,
            n = n,
            min = sorted.first(),
            max = sorted.last(),
            median = median
        )
    }

    // -------------------------------------------------------------------------
    // 4. Normal Q-Q Plot (Blom's Plotting Positions + Beasley-Springer-Moro)
    // -------------------------------------------------------------------------

    data class QqPoint(
        val theoreticalQuantile: Double,
        val sampleQuantile: Double,
        val plottingPosition: Double
    )

    data class QqPlotResult(
        val points: List<QqPoint>,
        val referenceLineStart: Pair<Double, Double>?,
        val referenceLineEnd: Pair<Double, Double>?,
        val n: Int
    )

    /**
     * Computes Normal Q-Q plot coordinates using Blom's (1958) plotting positions:
     *   p_i = (i - 3/8) / (n + 1/4), i = 1..n
     * and the Beasley-Springer-Moro inverse standard normal CDF.
     * When n >= 4, computes the standard quartile reference line passing through
     * (Q1_theoretical, Q1_sample) and (Q3_theoretical, Q3_sample).
     */
    fun computeNormalQqPlot(values: List<Double>): QqPlotResult {
        val sorted = values.filter { it.isFinite() }.sorted()
        val n = sorted.size
        if (n == 0) return QqPlotResult(emptyList(), null, null, 0)

        val points = sorted.mapIndexed { idx, sampleVal ->
            val i = idx + 1.0
            val p = (i - 0.375) / (n + 0.25)
            QqPoint(
                theoreticalQuantile = inverseNormalCdf(p),
                sampleQuantile = sampleVal,
                plottingPosition = p
            )
        }

        var lineStart: Pair<Double, Double>? = null
        var lineEnd: Pair<Double, Double>? = null
        if (n >= 4) {
            val q1Sample = linearQuantile(sorted, 0.25)
            val q3Sample = linearQuantile(sorted, 0.75)
            val q1Theo = inverseNormalCdf(0.25)
            val q3Theo = inverseNormalCdf(0.75)
            val slope = (q3Sample - q1Sample) / (q3Theo - q1Theo)
            val intercept = q1Sample - slope * q1Theo
            val xMin = points.first().theoreticalQuantile
            val xMax = points.last().theoreticalQuantile
            lineStart = xMin to (intercept + slope * xMin)
            lineEnd = xMax to (intercept + slope * xMax)
        }

        return QqPlotResult(points, lineStart, lineEnd, n)
    }

    private fun linearQuantile(sorted: List<Double>, p: Double): Double {
        val pos = p * (sorted.size - 1)
        val lo = floor(pos).toInt()
        val hi = ceil(pos).toInt()
        if (lo == hi) return sorted[lo]
        val frac = pos - lo
        return sorted[lo] * (1.0 - frac) + sorted[hi] * frac
    }

    /**
     * Beasley-Springer-Moro approximation to the inverse standard normal CDF.
     */
    internal fun inverseNormalCdf(p: Double): Double {
        val clamped = p.coerceIn(1e-12, 1.0 - 1e-12)
        val a = doubleArrayOf(
            -3.969683028665376e+01, 2.209460984245205e+02,
            -2.759285104469687e+02, 1.383577518672690e+02,
            -3.066479806614716e+01, 2.506628277459239e+00
        )
        val b = doubleArrayOf(
            -5.447609879822406e+01, 1.615858368580409e+02,
            -1.556989798598866e+02, 6.680131188771972e+01,
            -1.328068155288572e+01
        )
        val c = doubleArrayOf(
            -7.784894002430293e-03, -3.223964580411365e-01,
            -2.400758277161838e+00, -2.549732539343734e+00,
            4.374664141464968e+00, 2.938163982698783e+00
        )
        val d = doubleArrayOf(
            7.784695709041462e-03, 3.224671290700398e-01,
            2.445134137142996e+00, 3.754408661907416e+00
        )
        val pLow = 0.02425
        val pHigh = 1.0 - pLow
        return when {
            clamped < pLow -> {
                val q = sqrt(-2.0 * ln(clamped))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
            clamped <= pHigh -> {
                val q = clamped - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - clamped))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5. Stem-and-Leaf Plot (Tukey 1977)
    // -------------------------------------------------------------------------

    data class StemLeafRow(
        val stemLabel: String,
        val leaves: List<Int>,
        val count: Int,
        val sortKey: Double
    )

    data class StemAndLeafResult(
        val rows: List<StemLeafRow>,
        val keyText: String,
        val stemUnit: Double,
        val leafUnit: Double,
        val n: Int
    )

    /**
     * Computes a Tukey (1977) stem-and-leaf display, preserving `-0` vs `+0` stems
     * for values near zero and providing an explicit precision key.
     */
    fun computeStemAndLeaf(values: List<Double>): StemAndLeafResult {
        val clean = values.filter { it.isFinite() }
        if (clean.isEmpty()) {
            return StemAndLeafResult(emptyList(), "", 10.0, 1.0, 0)
        }
        val minVal = clean.min()
        val maxVal = clean.max()
        val span = max(maxVal - minVal, 1e-6)
        val rawExp = floor(log10(span)).toInt()
        var stemUnit = 10.0.pow(rawExp)
        // Target roughly 5 to 16 stems.
        val estimatedStems = span / stemUnit
        if (estimatedStems < 4.0) {
            stemUnit /= 10.0
        } else if (estimatedStems > 18.0) {
            stemUnit *= 10.0
        }
        val leafUnit = stemUnit / 10.0

        data class StemKey(val isNegativeZero: Boolean, val stemInt: Long) : Comparable<StemKey> {
            override fun compareTo(other: StemKey): Int {
                val thisVal = if (isNegativeZero) -0.5 else if (stemInt == 0L) 0.5 else stemInt.toDouble()
                val otherVal = if (other.isNegativeZero) -0.5 else if (other.stemInt == 0L) 0.5 else other.stemInt.toDouble()
                return thisVal.compareTo(otherVal)
            }
        }

        val grouped = sortedMapOf<StemKey, MutableList<Int>>()
        clean.forEach { v ->
            val scaled = v / leafUnit
            // Round half away from zero to preserve sign near 0.
            val rounded = if (scaled >= 0.0) floor(scaled + 0.5).roundToLong() else -floor(-scaled + 0.5).roundToLong()
            val isNeg = v < 0.0 && rounded <= 0L && (rounded < 0L || v < 0.0)
            val absRounded = abs(rounded)
            val stemAbs = absRounded / 10L
            val leaf = (absRounded % 10L).toInt()
            val key = if (isNeg && stemAbs == 0L && rounded < 0L) {
                StemKey(isNegativeZero = true, stemInt = 0L)
            } else if (isNeg && stemAbs > 0L) {
                StemKey(isNegativeZero = false, stemInt = -stemAbs)
            } else {
                StemKey(isNegativeZero = false, stemInt = stemAbs)
            }
            grouped.getOrPut(key) { mutableListOf() }.add(leaf)
        }

        val rows = grouped.entries.map { (key, leaves) ->
            val label = when {
                key.isNegativeZero -> "-0"
                else -> key.stemInt.toString()
            }
            val sortedLeaves = leaves.sorted()
            val sortVal = if (key.isNegativeZero) -0.5 else if (key.stemInt == 0L) 0.5 else key.stemInt.toDouble()
            StemLeafRow(
                stemLabel = label,
                leaves = sortedLeaves,
                count = sortedLeaves.size,
                sortKey = sortVal
            )
        }

        val exampleVal = 4.0 * stemUnit + 2.0 * leafUnit
        val formattedExample = formatCompact(exampleVal)
        val keyText = "Key: 4 | 2 = $formattedExample"
        return StemAndLeafResult(
            rows = rows,
            keyText = keyText,
            stemUnit = stemUnit,
            leafUnit = leafUnit,
            n = clean.size
        )
    }

    private fun formatCompact(value: Double): String {
        val rounded = value.roundToLong()
        return if (abs(value - rounded.toDouble()) < 1e-9) {
            rounded.toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    // -------------------------------------------------------------------------
    // 6. Hexbin Plot (Carr et al., 1987)
    // -------------------------------------------------------------------------

    data class HexbinBin(
        val col: Int,
        val row: Int,
        val centerX: Double,
        val centerY: Double,
        val count: Int
    )

    data class HexbinResult(
        val bins: List<HexbinBin>,
        val gridCols: Int,
        val gridRows: Int,
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
        val maxCount: Int,
        val totalPoints: Int
    )

    /**
     * Bins 2D points into a regular hexagonal lattice (Carr et al., 1987)
     * in normalized [0, 1] x [0, 1] space mapped back to data coordinates.
     * Only populated bins (count > 0) are returned; empty cells remain distinct
     * from zero-valued observations.
     */
    fun computeHexbin(
        points: List<Pair<Double, Double>>,
        targetCols: Int = 10
    ): HexbinResult {
        val clean = points.filter { it.first.isFinite() && it.second.isFinite() }
        if (clean.isEmpty()) {
            return HexbinResult(emptyList(), targetCols, targetCols, 0.0, 1.0, 0.0, 1.0, 0, 0)
        }
        val rawXMin = clean.minOf { it.first }
        val rawXMax = clean.maxOf { it.first }
        val rawYMin = clean.minOf { it.second }
        val rawYMax = clean.maxOf { it.second }
        val xSpan = if (rawXMax - rawXMin > 1e-9) rawXMax - rawXMin else 1.0
        val ySpan = if (rawYMax - rawYMin > 1e-9) rawYMax - rawYMin else 1.0

        val cols = targetCols.coerceIn(4, 24)
        val dx = 1.0 / cols
        val dy = dx * (sqrt(3.0) / 2.0)
        val rows = max(2, ceil(1.0 / dy).toInt() + 1)

        val counts = linkedMapOf<Pair<Int, Int>, Int>()
        clean.forEach { (x, y) ->
            val nx = (x - rawXMin) / xSpan
            val ny = (y - rawYMin) / ySpan
            val approxRow = floor(ny / dy).toInt()
            var bestCol = 0
            var bestRow = 0
            var bestDist = Double.POSITIVE_INFINITY
            for (r in (approxRow - 1)..(approxRow + 1)) {
                val offset = if ((r and 1) == 1) 0.5 * dx else 0.0
                val approxCol = floor((nx - offset) / dx).toInt()
                for (c in (approxCol - 1)..(approxCol + 1)) {
                    val cx = c * dx + offset
                    val cy = r * dy
                    val d2 = (nx - cx) * (nx - cx) + (ny - cy) * (ny - cy)
                    if (d2 < bestDist) {
                        bestDist = d2
                        bestCol = c
                        bestRow = r
                    }
                }
            }
            val key = bestCol.coerceIn(0, cols) to bestRow.coerceIn(0, rows)
            counts[key] = (counts[key] ?: 0) + 1
        }

        val bins = counts.entries.map { (key, count) ->
            val (c, r) = key
            val offset = if ((r and 1) == 1) 0.5 * dx else 0.0
            val cxNorm = (c * dx + offset).coerceIn(0.0, 1.0)
            val cyNorm = (r * dy).coerceIn(0.0, 1.0)
            HexbinBin(
                col = c,
                row = r,
                centerX = rawXMin + cxNorm * xSpan,
                centerY = rawYMin + cyNorm * ySpan,
                count = count
            )
        }
        return HexbinResult(
            bins = bins,
            gridCols = cols,
            gridRows = rows,
            xMin = rawXMin,
            xMax = rawXMin + xSpan,
            yMin = rawYMin,
            yMax = rawYMin + ySpan,
            maxCount = bins.maxOfOrNull { it.count } ?: 0,
            totalPoints = clean.size
        )
    }
}
