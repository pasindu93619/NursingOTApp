package com.pasindu.nursingotapp.statgraph.stats

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Authoritative clinical and statistical math functions for Stat Graph Studio.
 *
 * Every function preserves missing/non-finite values honestly without silent imputation
 * or invalid bound swapping.
 */
object StatisticalMath {

    /** Two-sided 95% normal quantile z_{0.975}. */
    const val Z_95: Double = 1.959963984540054

    /** Anti-bias constant d2 for moving ranges of span 2 (I-MR charts). */
    const val IMR_D2_SPAN2: Double = 1.128

    /** Lower control limit factor D3 for moving ranges of span 2 (I-MR charts). */
    const val IMR_D3_SPAN2: Double = 0.0

    /** Upper control limit factor D4 for moving ranges of span 2 (I-MR charts). */
    const val IMR_D4_SPAN2: Double = 3.267

    /** Default clinical outbreak threshold label. */
    const val OUTBREAK_THRESHOLD_LABEL: String = "Baseline mean + 2 standard deviations"

    // -------------------------------------------------------------------------
    // 1. Histogram Binning (Freedman-Diaconis with Sturges fallback)
    // -------------------------------------------------------------------------

    enum class HistogramRule {
        FREEDMAN_DIACONIS,
        STURGES,
    }

    data class HistogramBin(
        val index: Int,
        val lowerBound: Double,
        val upperBound: Double,
        val count: Int,
        val isLastInclusive: Boolean,
    ) {
        val midpoint: Double get() = (lowerBound + upperBound) / 2.0
    }

    data class HistogramResult(
        val bins: List<HistogramBin>,
        val ruleUsed: HistogramRule,
        val binWidth: Double,
        val totalCount: Int,
    )

    fun sturgesBinCount(n: Int): Int {
        if (n <= 1) return 1
        return max(1, ceil(ln(n.toDouble()) / ln(2.0) + 1.0).toInt())
    }

    /**
     * Computes histogram bins using Freedman-Diaconis (`h = 2 * IQR * n^(-1/3)`),
     * falling back to Sturges' rule when `n < 2`, `IQR <= 0`, or the range is degenerate.
     * Guarantees strictly positive bin widths.
     */
    fun computeHistogram(
        values: List<Double>,
        maxBins: Int = 50,
    ): HistogramResult {
        val valid = values.filter { it.isFinite() }.sorted()
        if (valid.isEmpty()) {
            return HistogramResult(emptyList(), HistogramRule.STURGES, 0.0, 0)
        }
        val minVal = valid.first()
        val maxVal = valid.last()
        val span = maxVal - minVal
        if (span <= 0.0 || valid.size == 1) {
            val halfPad = 0.5
            val bin = HistogramBin(
                index = 0,
                lowerBound = minVal - halfPad,
                upperBound = maxVal + halfPad,
                count = valid.size,
                isLastInclusive = true,
            )
            return HistogramResult(listOf(bin), HistogramRule.STURGES, 1.0, valid.size)
        }

        val quartiles = StatisticsEngine.quartiles(valid)
        val iqr = quartiles?.iqr ?: 0.0
        val (binCount, ruleUsed) = if (valid.size >= 2 && iqr > 0.0) {
            val fdWidth = 2.0 * iqr * valid.size.toDouble().pow(-1.0 / 3.0)
            if (fdWidth > 0.0 && fdWidth.isFinite()) {
                val rawCount = ceil(span / fdWidth).toInt().coerceIn(1, maxBins)
                rawCount to HistogramRule.FREEDMAN_DIACONIS
            } else {
                sturgesBinCount(valid.size).coerceIn(1, maxBins) to HistogramRule.STURGES
            }
        } else {
            sturgesBinCount(valid.size).coerceIn(1, maxBins) to HistogramRule.STURGES
        }

        val width = span / binCount
        val counts = IntArray(binCount)
        for (v in valid) {
            val idx = if (v >= maxVal) {
                binCount - 1
            } else {
                floor((v - minVal) / width).toInt().coerceIn(0, binCount - 1)
            }
            counts[idx]++
        }

        val bins = (0 until binCount).map { i ->
            val lower = minVal + i * width
            val upper = if (i == binCount - 1) maxVal else minVal + (i + 1) * width
            HistogramBin(
                index = i,
                lowerBound = lower,
                upperBound = upper,
                count = counts[i],
                isLastInclusive = (i == binCount - 1),
            )
        }
        return HistogramResult(
            bins = bins,
            ruleUsed = ruleUsed,
            binWidth = width,
            totalCount = valid.size,
        )
    }

    // -------------------------------------------------------------------------
    // 2. Box Plot (R-7 Quartiles + Tukey 1.5 * IQR Inner Fences)
    // -------------------------------------------------------------------------

    data class BoxPlotSummary(
        val count: Int,
        val min: Double,
        val q1: Double,
        val median: Double,
        val q3: Double,
        val max: Double,
        val iqr: Double,
        val lowerFence: Double,
        val upperFence: Double,
        val lowerWhisker: Double,
        val upperWhisker: Double,
        val outliers: List<Double>,
    )

    fun computeBoxPlot(values: List<Double>): BoxPlotSummary? {
        val valid = values.filter { it.isFinite() }.sorted()
        if (valid.isEmpty()) return null
        val q = StatisticsEngine.quartiles(valid) ?: return null
        val lowerFence = q.q1 - 1.5 * q.iqr
        val upperFence = q.q3 + 1.5 * q.iqr

        val inFence = valid.filter { it >= lowerFence && it <= upperFence }
        val lowerWhisker = inFence.firstOrNull() ?: q.q1
        val upperWhisker = inFence.lastOrNull() ?: q.q3
        val outliers = valid.filter { it < lowerFence || it > upperFence }

        return BoxPlotSummary(
            count = valid.size,
            min = valid.first(),
            q1 = q.q1,
            median = q.q2Median,
            q3 = q.q3,
            max = valid.last(),
            iqr = q.iqr,
            lowerFence = lowerFence,
            upperFence = upperFence,
            lowerWhisker = lowerWhisker,
            upperWhisker = upperWhisker,
            outliers = outliers,
        )
    }

    // -------------------------------------------------------------------------
    // 3. Gaussian Kernel Density Estimation (Silverman's Rule of Thumb)
    // -------------------------------------------------------------------------

    data class KdePoint(
        val x: Double,
        val density: Double,
    )

    data class KdeResult(
        val points: List<KdePoint>,
        val bandwidth: Double,
    )

    fun computeGaussianKde(
        values: List<Double>,
        gridPoints: Int = 64,
    ): KdeResult? {
        val valid = values.filter { it.isFinite() }.sorted()
        if (valid.size < 2 || gridPoints < 2) return null

        val sd = StatisticsEngine.standardDeviation(valid, population = false) ?: 0.0
        val iqr = StatisticsEngine.quartiles(valid)?.iqr ?: 0.0
        val spread = when {
            sd > 0.0 && iqr > 0.0 -> min(sd, iqr / 1.34)
            sd > 0.0 -> sd
            iqr > 0.0 -> iqr / 1.34
            else -> 0.0
        }
        if (spread <= 0.0) return null

        val bandwidth = 0.9 * spread * valid.size.toDouble().pow(-0.2)
        if (bandwidth <= 0.0 || !bandwidth.isFinite()) return null

        val minX = valid.first() - 3.0 * bandwidth
        val maxX = valid.last() + 3.0 * bandwidth
        val step = (maxX - minX) / (gridPoints - 1)
        val norm = 1.0 / (valid.size * bandwidth * sqrt(2.0 * PI))

        val points = (0 until gridPoints).map { idx ->
            val x = minX + idx * step
            var sum = 0.0
            for (v in valid) {
                val u = (x - v) / bandwidth
                sum += exp(-0.5 * u * u)
            }
            KdePoint(x = x, density = norm * sum)
        }
        return KdeResult(points = points, bandwidth = bandwidth)
    }

    // -------------------------------------------------------------------------
    // 4. Wilson 95% Score Confidence Interval
    // -------------------------------------------------------------------------

    data class WilsonInterval(
        val numerator: Int,
        val denominator: Int,
        val proportion: Double,
        val lower: Double,
        val upper: Double,
        val isSmallCount: Boolean,
    )

    /**
     * Computes the Wilson 95% score confidence interval for a binomial proportion `k / n`.
     * Returns `null` if `n <= 0`, `k < 0`, or `k > n` (invalid clinical denominators/counts).
     */
    fun wilsonScoreInterval(
        successes: Int,
        trials: Int,
        z: Double = Z_95,
    ): WilsonInterval? {
        if (trials <= 0 || successes < 0 || successes > trials || !z.isFinite() || z <= 0.0) {
            return null
        }
        val n = trials.toDouble()
        val p = successes.toDouble() / n
        val z2 = z * z
        val denom = 1.0 + z2 / n
        val center = (p + z2 / (2.0 * n)) / denom
        val margin = (z * sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n)) / denom
        return WilsonInterval(
            numerator = successes,
            denominator = trials,
            proportion = p,
            lower = (center - margin).coerceIn(0.0, 1.0),
            upper = (center + margin).coerceIn(0.0, 1.0),
            isSmallCount = successes < 5 || (trials - successes) < 5,
        )
    }

    // -------------------------------------------------------------------------
    // 5. Outbreak Threshold & Control Charts (SPC)
    // -------------------------------------------------------------------------

    data class OutbreakThreshold(
        val label: String,
        val baselineMean: Double,
        val baselineSampleSd: Double,
        val threshold: Double,
    )

    /**
     * Computes `"Baseline mean + 2 standard deviations"` using sample SD (`n - 1`).
     */
    fun computeOutbreakThreshold(baselineValues: List<Double>): OutbreakThreshold? {
        val valid = baselineValues.filter { it.isFinite() }
        if (valid.size < 2) return null
        val mean = valid.average()
        val sd = StatisticsEngine.standardDeviation(valid, population = false) ?: return null
        return OutbreakThreshold(
            label = OUTBREAK_THRESHOLD_LABEL,
            baselineMean = mean,
            baselineSampleSd = sd,
            threshold = mean + 2.0 * sd,
        )
    }

    enum class SpcRuleType(val description: String) {
        RULE_1_BEYOND_3_SIGMA("1 point beyond 3σ control limit"),
        RULE_2_TWO_OF_THREE_BEYOND_2_SIGMA("2 of 3 consecutive points beyond 2σ on the same side"),
        RULE_3_FOUR_OF_FIVE_BEYOND_1_SIGMA("4 of 5 consecutive points beyond 1σ on the same side"),
        RULE_4_EIGHT_CONSECUTIVE_SAME_SIDE("8 consecutive points on the same side of the centre line"),
    }

    data class SpcRuleViolation(
        val index: Int,
        val rule: SpcRuleType,
    )

    data class SpcPoint(
        val index: Int,
        val value: Double?,
        val centerLine: Double,
        val ucl: Double,
        val lcl: Double,
        val sigma: Double,
    ) {
        val isOutOfControl: Boolean
            get() = value != null && value.isFinite() && (value > ucl || value < lcl)
    }

    data class SpcChartResult(
        val chartType: String,
        val centerLine: Double,
        val points: List<SpcPoint>,
        val violations: List<SpcRuleViolation>,
    )

    /**
     * p-chart for defectives/proportions with variable subgroup sizes `n_i`.
     */
    fun computePChart(
        events: List<Int>,
        denominators: List<Int>,
    ): SpcChartResult? {
        if (events.isEmpty() || events.size != denominators.size) return null
        val validPairs = events.indices.filter { i ->
            denominators[i] > 0 && events[i] >= 0 && events[i] <= denominators[i]
        }
        if (validPairs.isEmpty()) return null
        val totalEvents = validPairs.sumOf { events[it].toDouble() }
        val totalDenom = validPairs.sumOf { denominators[it].toDouble() }
        if (totalDenom <= 0.0) return null
        val pBar = totalEvents / totalDenom

        val points = events.indices.map { i ->
            val n = denominators[i]
            val k = events[i]
            if (n > 0 && k >= 0 && k <= n) {
                val sigma = sqrt((pBar * (1.0 - pBar)) / n.toDouble())
                SpcPoint(
                    index = i,
                    value = k.toDouble() / n.toDouble(),
                    centerLine = pBar,
                    ucl = (pBar + 3.0 * sigma).coerceAtMost(1.0),
                    lcl = (pBar - 3.0 * sigma).coerceAtLeast(0.0),
                    sigma = sigma,
                )
            } else {
                SpcPoint(i, null, pBar, 1.0, 0.0, 0.0)
            }
        }
        return SpcChartResult(
            chartType = "p-chart",
            centerLine = pBar,
            points = points,
            violations = detectSpcRunRules(points),
        )
    }

    /**
     * np-chart for count of defectives with constant subgroup size `subgroupSize`.
     */
    fun computeNpChart(
        events: List<Int>,
        subgroupSize: Int,
    ): SpcChartResult? {
        if (events.isEmpty() || subgroupSize <= 0) return null
        val valid = events.filter { it in 0..subgroupSize }
        if (valid.isEmpty()) return null
        val npBar = valid.average()
        val pBar = npBar / subgroupSize.toDouble()
        val sigma = sqrt(npBar * (1.0 - pBar))
        val ucl = npBar + 3.0 * sigma
        val lcl = (npBar - 3.0 * sigma).coerceAtLeast(0.0)

        val points = events.mapIndexed { i, k ->
            SpcPoint(
                index = i,
                value = if (k in 0..subgroupSize) k.toDouble() else null,
                centerLine = npBar,
                ucl = ucl,
                lcl = lcl,
                sigma = sigma,
            )
        }
        return SpcChartResult(
            chartType = "np-chart",
            centerLine = npBar,
            points = points,
            violations = detectSpcRunRules(points),
        )
    }

    /**
     * c-chart for defect counts per constant inspection unit.
     */
    fun computeCChart(counts: List<Int>): SpcChartResult? {
        if (counts.isEmpty()) return null
        val valid = counts.filter { it >= 0 }
        if (valid.isEmpty()) return null
        val cBar = valid.average()
        val sigma = sqrt(cBar)
        val ucl = cBar + 3.0 * sigma
        val lcl = (cBar - 3.0 * sigma).coerceAtLeast(0.0)
        val points = counts.mapIndexed { i, c ->
            SpcPoint(
                index = i,
                value = if (c >= 0) c.toDouble() else null,
                centerLine = cBar,
                ucl = ucl,
                lcl = lcl,
                sigma = sigma,
            )
        }
        return SpcChartResult(
            chartType = "c-chart",
            centerLine = cBar,
            points = points,
            violations = detectSpcRunRules(points),
        )
    }

    /**
     * u-chart for defect rate per unit with variable inspection unit sizes `units[i]`.
     */
    fun computeUChart(
        counts: List<Int>,
        units: List<Double>,
    ): SpcChartResult? {
        if (counts.isEmpty() || counts.size != units.size) return null
        val validIdx = counts.indices.filter { counts[it] >= 0 && units[it].isFinite() && units[it] > 0.0 }
        if (validIdx.isEmpty()) return null
        val totalCounts = validIdx.sumOf { counts[it].toDouble() }
        val totalUnits = validIdx.sumOf { units[it] }
        if (totalUnits <= 0.0) return null
        val uBar = totalCounts / totalUnits

        val points = counts.indices.map { i ->
            val uSize = units[i]
            val c = counts[i]
            if (c >= 0 && uSize.isFinite() && uSize > 0.0) {
                val sigma = sqrt(uBar / uSize)
                SpcPoint(
                    index = i,
                    value = c.toDouble() / uSize,
                    centerLine = uBar,
                    ucl = uBar + 3.0 * sigma,
                    lcl = (uBar - 3.0 * sigma).coerceAtLeast(0.0),
                    sigma = sigma,
                )
            } else {
                SpcPoint(i, null, uBar, uBar, 0.0, 0.0)
            }
        }
        return SpcChartResult(
            chartType = "u-chart",
            centerLine = uBar,
            points = points,
            violations = detectSpcRunRules(points),
        )
    }

    data class ImrResult(
        val individuals: SpcChartResult,
        val movingRanges: SpcChartResult,
        val mrBar: Double,
        val estimatedSigma: Double,
    )

    /**
     * I-MR chart (Individuals and Moving Range).
     *
     * Missing or non-finite values break the moving-range pair rather than bridging across gaps.
     */
    fun computeImrChart(values: List<Double?>): ImrResult? {
        val validValues = values.mapNotNull { it?.takeIf { v -> v.isFinite() } }
        if (validValues.size < 2) return null

        val movingRangesRaw = values.indices.map { i ->
            if (i == 0) {
                null
            } else {
                val curr = values[i]
                val prev = values[i - 1]
                if (curr != null && prev != null && curr.isFinite() && prev.isFinite()) {
                    abs(curr - prev)
                } else {
                    null
                }
            }
        }
        val validMrs = movingRangesRaw.filterNotNull()
        if (validMrs.isEmpty()) return null

        val xBar = validValues.average()
        val mrBar = validMrs.average()
        val sigma = mrBar / IMR_D2_SPAN2
        val iUcl = xBar + 3.0 * sigma
        val iLcl = xBar - 3.0 * sigma

        val iPoints = values.mapIndexed { i, v ->
            val clean = v?.takeIf { it.isFinite() }
            SpcPoint(i, clean, xBar, iUcl, iLcl, sigma)
        }
        val mrUcl = IMR_D4_SPAN2 * mrBar
        val mrLcl = IMR_D3_SPAN2 * mrBar
        val mrSigma = (mrUcl - mrBar) / 3.0
        val mrPoints = movingRangesRaw.mapIndexed { i, mr ->
            SpcPoint(i, mr, mrBar, mrUcl, mrLcl, mrSigma)
        }
        return ImrResult(
            individuals = SpcChartResult("I-chart", xBar, iPoints, detectSpcRunRules(iPoints)),
            movingRanges = SpcChartResult("MR-chart", mrBar, mrPoints, detectSpcRunRules(mrPoints)),
            mrBar = mrBar,
            estimatedSigma = sigma,
        )
    }

    private data class XbarRFactors(val a2: Double, val d3: Double, val d4: Double)

    // NIST Engineering Statistics Handbook / ASTM E2587 constants for n = 2..10
    private val XBAR_R_TABLE: Map<Int, XbarRFactors> = mapOf(
        2 to XbarRFactors(a2 = 1.880, d3 = 0.000, d4 = 3.267),
        3 to XbarRFactors(a2 = 1.023, d3 = 0.000, d4 = 2.574),
        4 to XbarRFactors(a2 = 0.729, d3 = 0.000, d4 = 2.282),
        5 to XbarRFactors(a2 = 0.577, d3 = 0.000, d4 = 2.114),
        6 to XbarRFactors(a2 = 0.483, d3 = 0.000, d4 = 2.004),
        7 to XbarRFactors(a2 = 0.419, d3 = 0.076, d4 = 1.924),
        8 to XbarRFactors(a2 = 0.373, d3 = 0.136, d4 = 1.864),
        9 to XbarRFactors(a2 = 0.337, d3 = 0.184, d4 = 1.816),
        10 to XbarRFactors(a2 = 0.308, d3 = 0.223, d4 = 1.777),
    )

    data class XbarRResult(
        val xBarChart: SpcChartResult,
        val rChart: SpcChartResult,
        val xDoubleBar: Double,
        val rBar: Double,
    )

    /**
     * X-bar and R chart for rational subgroups of equal size `n` in `2..10`.
     */
    fun computeXbarRChart(subgroups: List<List<Double>>): XbarRResult? {
        if (subgroups.isEmpty()) return null
        val n = subgroups.first().size
        val factors = XBAR_R_TABLE[n] ?: return null
        if (subgroups.any { it.size != n || it.any { v -> !v.isFinite() } }) return null

        val means = subgroups.map { it.average() }
        val ranges = subgroups.map { it.max() - it.min() }
        val xDoubleBar = means.average()
        val rBar = ranges.average()

        val xUcl = xDoubleBar + factors.a2 * rBar
        val xLcl = xDoubleBar - factors.a2 * rBar
        val xSigma = (factors.a2 * rBar) / 3.0

        val rUcl = factors.d4 * rBar
        val rLcl = factors.d3 * rBar
        val rSigma = (rUcl - rBar) / 3.0

        val xPoints = means.mapIndexed { i, m -> SpcPoint(i, m, xDoubleBar, xUcl, xLcl, xSigma) }
        val rPoints = ranges.mapIndexed { i, r -> SpcPoint(i, r, rBar, rUcl, rLcl, rSigma) }
        return XbarRResult(
            xBarChart = SpcChartResult("X-bar", xDoubleBar, xPoints, detectSpcRunRules(xPoints)),
            rChart = SpcChartResult("R-chart", rBar, rPoints, detectSpcRunRules(rPoints)),
            xDoubleBar = xDoubleBar,
            rBar = rBar,
        )
    }

    /**
     * Exponentially Weighted Moving Average (EWMA) chart with exact time-varying limits.
     */
    fun computeEwmaChart(
        values: List<Double>,
        lambda: Double = 0.2,
        limitWidthL: Double = 3.0,
        targetMean: Double? = null,
        sigma: Double? = null,
    ): SpcChartResult? {
        val valid = values.filter { it.isFinite() }
        if (valid.size < 2 || lambda <= 0.0 || lambda > 1.0 || limitWidthL <= 0.0) return null

        val mu0 = targetMean ?: valid.average()
        val s = sigma ?: StatisticsEngine.standardDeviation(valid, population = false) ?: return null
        if (s <= 0.0 || !s.isFinite()) return null

        var prevZ = mu0
        val points = valid.mapIndexed { idx, x ->
            val i = idx + 1
            val z = lambda * x + (1.0 - lambda) * prevZ
            prevZ = z
            val factor = sqrt((lambda / (2.0 - lambda)) * (1.0 - (1.0 - lambda).pow(2 * i)))
            val pointSigma = s * factor
            SpcPoint(
                index = idx,
                value = z,
                centerLine = mu0,
                ucl = mu0 + limitWidthL * pointSigma,
                lcl = mu0 - limitWidthL * pointSigma,
                sigma = pointSigma,
            )
        }
        return SpcChartResult(
            chartType = "EWMA",
            centerLine = mu0,
            points = points,
            violations = points.filter { it.isOutOfControl }
                .map { SpcRuleViolation(it.index, SpcRuleType.RULE_1_BEYOND_3_SIGMA) },
        )
    }

    data class CusumPoint(
        val index: Int,
        val value: Double,
        val upperCusum: Double,
        val lowerCusum: Double,
        val decisionIntervalH: Double,
    ) {
        val isUpperSignal: Boolean get() = upperCusum > decisionIntervalH
        val isLowerSignal: Boolean get() = lowerCusum > decisionIntervalH
    }

    /**
     * Two-sided tabular CUSUM with reference value `K = kSigma * sigma` and threshold `H = hSigma * sigma`.
     */
    fun computeCusum(
        values: List<Double>,
        kSigma: Double = 0.5,
        hSigma: Double = 5.0,
        targetMean: Double? = null,
        sigma: Double? = null,
    ): List<CusumPoint> {
        val valid = values.filter { it.isFinite() }
        if (valid.size < 2) return emptyList()
        val mu0 = targetMean ?: valid.average()
        val s = sigma ?: StatisticsEngine.standardDeviation(valid, population = false) ?: return emptyList()
        if (s <= 0.0 || !s.isFinite()) return emptyList()

        val k = kSigma * s
        val h = hSigma * s
        var cPlus = 0.0
        var cMinus = 0.0
        return valid.mapIndexed { i, x ->
            cPlus = max(0.0, x - (mu0 + k) + cPlus)
            cMinus = max(0.0, (mu0 - k) - x + cMinus)
            CusumPoint(
                index = i,
                value = x,
                upperCusum = cPlus,
                lowerCusum = cMinus,
                decisionIntervalH = h,
            )
        }
    }

    /**
     * Evaluates the four Western Electric / NHS SPC special-cause run rules across finite points.
     */
    fun detectSpcRunRules(
        points: List<SpcPoint>,
        runLengthRule4: Int = 8,
    ): List<SpcRuleViolation> {
        val finite = points.filter { it.value != null && it.value.isFinite() }
        if (finite.isEmpty()) return emptyList()
        val violations = mutableListOf<SpcRuleViolation>()

        // Rule 1: 1 point beyond 3 sigma (or outside [lcl, ucl])
        for (p in finite) {
            if (p.isOutOfControl) {
                violations.add(SpcRuleViolation(p.index, SpcRuleType.RULE_1_BEYOND_3_SIGMA))
            }
        }

        // Helper: signed z-score
        fun zScore(p: SpcPoint): Double {
            val v = p.value ?: return 0.0
            return if (p.sigma > 0.0) (v - p.centerLine) / p.sigma else 0.0
        }

        // Rule 2: 2 of 3 consecutive points beyond 2 sigma on same side
        for (i in 2 until finite.size) {
            val window = finite.subList(i - 2, i + 1)
            val above = window.count { zScore(it) > 2.0 }
            val below = window.count { zScore(it) < -2.0 }
            if ((above >= 2 && zScore(finite[i]) > 2.0) || (below >= 2 && zScore(finite[i]) < -2.0)) {
                violations.add(SpcRuleViolation(finite[i].index, SpcRuleType.RULE_2_TWO_OF_THREE_BEYOND_2_SIGMA))
            }
        }

        // Rule 3: 4 of 5 consecutive points beyond 1 sigma on same side
        for (i in 4 until finite.size) {
            val window = finite.subList(i - 4, i + 1)
            val above = window.count { zScore(it) > 1.0 }
            val below = window.count { zScore(it) < -1.0 }
            if ((above >= 4 && zScore(finite[i]) > 1.0) || (below >= 4 && zScore(finite[i]) < -1.0)) {
                violations.add(SpcRuleViolation(finite[i].index, SpcRuleType.RULE_3_FOUR_OF_FIVE_BEYOND_1_SIGMA))
            }
        }

        // Rule 4: 8 consecutive points on same side of center line
        if (finite.size >= runLengthRule4) {
            for (i in (runLengthRule4 - 1) until finite.size) {
                val window = finite.subList(i - runLengthRule4 + 1, i + 1)
                val allAbove = window.all { (it.value ?: it.centerLine) > it.centerLine }
                val allBelow = window.all { (it.value ?: it.centerLine) < it.centerLine }
                if (allAbove || allBelow) {
                    violations.add(SpcRuleViolation(finite[i].index, SpcRuleType.RULE_4_EIGHT_CONSECUTIVE_SAME_SIDE))
                }
            }
        }
        return violations
    }

    // -------------------------------------------------------------------------
    // 6. Kaplan-Meier Survival + Greenwood Variance
    // -------------------------------------------------------------------------

    data class SurvivalObservation(
        val time: Double,
        val eventOccurred: Boolean,
    )

    data class KaplanMeierStep(
        val time: Double,
        val atRisk: Int,
        val events: Int,
        val censored: Int,
        val survival: Double,
        val greenwoodVariance: Double,
        val standardError: Double,
        val ciLower95: Double,
        val ciUpper95: Double,
    )

    /**
     * Computes the Kaplan-Meier product-limit survival curve and Greenwood's variance.
     * At tied event and censoring times, events occur immediately before censorings.
     */
    fun computeKaplanMeier(observations: List<SurvivalObservation>): List<KaplanMeierStep> {
        val valid = observations.filter { it.time.isFinite() && it.time >= 0.0 }
        if (valid.isEmpty()) return emptyList()

        val grouped = valid.groupBy { it.time }.toSortedMap()
        var atRisk = valid.size
        var survival = 1.0
        var greenwoodSum = 0.0

        val steps = mutableListOf(
            KaplanMeierStep(
                time = 0.0,
                atRisk = atRisk,
                events = 0,
                censored = 0,
                survival = 1.0,
                greenwoodVariance = 0.0,
                standardError = 0.0,
                ciLower95 = 1.0,
                ciUpper95 = 1.0,
            ),
        )

        for ((time, obsAtTime) in grouped) {
            val d = obsAtTime.count { it.eventOccurred }
            val c = obsAtTime.size - d
            if (d > 0 && atRisk > 0) {
                survival *= (1.0 - d.toDouble() / atRisk.toDouble())
                if (atRisk > d) {
                    greenwoodSum += d.toDouble() / (atRisk.toDouble() * (atRisk - d).toDouble())
                }
            }
            val variance = survival * survival * greenwoodSum
            val se = sqrt(variance)
            val ciLower = (survival - Z_95 * se).coerceIn(0.0, 1.0)
            val ciUpper = (survival + Z_95 * se).coerceIn(0.0, 1.0)

            steps.add(
                KaplanMeierStep(
                    time = time,
                    atRisk = atRisk,
                    events = d,
                    censored = c,
                    survival = survival,
                    greenwoodVariance = variance,
                    standardError = se,
                    ciLower95 = ciLower,
                    ciUpper95 = ciUpper,
                ),
            )
            atRisk -= (d + c)
        }
        return steps
    }

    // -------------------------------------------------------------------------
    // 7. Specialised Chart Mappings & Interval Bound Validation
    // -------------------------------------------------------------------------

    /**
     * Maps a non-negative quantitative value `v` to a bubble radius proportional to `sqrt(v)`
     * so bubble area scales linearly with `v`.
     */
    fun bubbleRadiusFromArea(
        value: Double,
        maxValue: Double,
        maxRadiusPx: Float,
        minRadiusPx: Float = 4f,
    ): Float {
        if (!value.isFinite() || value <= 0.0 || !maxValue.isFinite() || maxValue <= 0.0) {
            return minRadiusPx
        }
        val ratio = sqrt((value / maxValue).coerceIn(0.0, 1.0)).toFloat()
        return max(minRadiusPx, ratio * maxRadiusPx)
    }

    /**
     * Normalizes positive values to sum to 1.0 for treemap area partitioning.
     */
    fun normalizeTreemapAreas(values: List<Double>): List<Double> {
        val clean = values.map { if (it.isFinite() && it > 0.0) it else 0.0 }
        val total = clean.sum()
        if (total <= 0.0) return clean.map { 0.0 }
        return clean.map { it / total }
    }

    data class WaterfallStep(
        val label: String,
        val delta: Double,
        val start: Double,
        val end: Double,
        val isTotal: Boolean,
    )

    fun computeWaterfallPositions(
        deltas: List<Pair<String, Double>>,
        includeTotal: Boolean = true,
    ): List<WaterfallStep> {
        var running = 0.0
        val steps = deltas.map { (label, rawDelta) ->
            val delta = if (rawDelta.isFinite()) rawDelta else 0.0
            val start = running
            val end = running + delta
            running = end
            WaterfallStep(label = label, delta = delta, start = start, end = end, isTotal = false)
        }
        return if (includeTotal && steps.isNotEmpty()) {
            steps + WaterfallStep(label = "Total", delta = running, start = 0.0, end = running, isTotal = true)
        } else {
            steps
        }
    }

    /**
     * Normalizes a heatmap cell value into `[0.0, 1.0]` (sequential) or `[-1.0, 1.0]` (diverging around `center`).
     */
    fun normalizeHeatmapValue(
        value: Double,
        min: Double,
        max: Double,
        divergingCenter: Double? = null,
    ): Double {
        if (!value.isFinite()) return 0.0
        return if (divergingCenter == null) {
            val span = max - min
            if (span <= 0.0) 0.5 else ((value - min) / span).coerceIn(0.0, 1.0)
        } else {
            val halfSpan = max(abs(max - divergingCenter), abs(divergingCenter - min))
            if (halfSpan <= 0.0) 0.0 else ((value - divergingCenter) / halfSpan).coerceIn(-1.0, 1.0)
        }
    }

    data class IntervalBoundValidation(
        val isValid: Boolean,
        val reason: String? = null,
    )

    /**
     * Validates forest/error-bar interval bounds without silently swapping `lower > upper`.
     */
    fun validateIntervalBounds(
        estimate: Double,
        lower: Double,
        upper: Double,
    ): IntervalBoundValidation {
        if (!estimate.isFinite() || !lower.isFinite() || !upper.isFinite()) {
            return IntervalBoundValidation(false, "Estimate and bounds must be finite numbers.")
        }
        if (lower > upper) {
            return IntervalBoundValidation(false, "Lower bound ($lower) exceeds upper bound ($upper).")
        }
        if (estimate < lower || estimate > upper) {
            return IntervalBoundValidation(false, "Point estimate ($estimate) falls outside [$lower, $upper].")
        }
        return IntervalBoundValidation(true)
    }

    data class KernelDensityEstimate(
        val points: List<Pair<Double, Double>>,
        val bandwidth: Double,
        val sampleCount: Int,
        val isConstant: Boolean,
    )

    /**
     * Computes a 1D Gaussian Kernel Density Estimate using Silverman's rule-of-thumb bandwidth:
     * `h = 0.9 * min(sd, IQR / 1.34) * n^(-1/5)`.
     *
     * Returns `null` when fewer than [minSamples] finite observations are available,
     * and sets [KernelDensityEstimate.isConstant] when variance is zero.
     */
    fun computeGaussianKde(
        values: List<Double>,
        gridSize: Int = 48,
        minSamples: Int = 10,
    ): KernelDensityEstimate? {
        val clean = values.filter { it.isFinite() }.sorted()
        val n = clean.size
        if (n < minSamples || gridSize < 4) return null

        val minVal = clean.first()
        val maxVal = clean.last()
        if (abs(maxVal - minVal) < 1e-12) {
            return KernelDensityEstimate(
                points = emptyList(),
                bandwidth = 0.0,
                sampleCount = n,
                isConstant = true,
            )
        }

        val mean = clean.average()
        val variance = clean.sumOf { (it - mean) * (it - mean) } / (n - 1).coerceAtLeast(1)
        val sd = sqrt(variance)
        val q1 = linearPercentile(clean, 0.25)
        val q3 = linearPercentile(clean, 0.75)
        val iqr = (q3 - q1).coerceAtLeast(0.0)
        val scale = if (iqr > 0.0) minOf(sd, iqr / 1.34) else sd
        val bandwidth = (0.9 * scale * Math.pow(n.toDouble(), -0.2)).coerceAtLeast(1e-6)

        val pad = 2.0 * bandwidth
        val lo = minVal - pad
        val hi = maxVal + pad
        val step = (hi - lo) / (gridSize - 1)
        val invSqrt2Pi = 1.0 / sqrt(2.0 * Math.PI)

        val pts = (0 until gridSize).map { i ->
            val x = lo + i * step
            var sumK = 0.0
            for (xi in clean) {
                val u = (x - xi) / bandwidth
                sumK += invSqrt2Pi * kotlin.math.exp(-0.5 * u * u)
            }
            val density = sumK / (n * bandwidth)
            x to density
        }
        return KernelDensityEstimate(
            points = pts,
            bandwidth = bandwidth,
            sampleCount = n,
            isConstant = false,
        )
    }

    private fun linearPercentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted.first()
        val idx = p.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lo = kotlin.math.floor(idx).toInt()
        val hi = kotlin.math.ceil(idx).toInt()
        val frac = idx - lo
        return sorted[lo] * (1.0 - frac) + sorted[hi] * frac
    }
}
