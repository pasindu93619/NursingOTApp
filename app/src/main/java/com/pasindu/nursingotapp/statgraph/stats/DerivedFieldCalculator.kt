package com.pasindu.nursingotapp.statgraph.stats

import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue
import java.util.Locale

/**
 * Computes derived fields across a [StatDataset] without altering original values.
 *
 * Guarantees:
 * - Missing values propagate safely and are never treated as zero.
 * - Division by zero never produces [Double.POSITIVE_INFINITY], [Double.NEGATIVE_INFINITY], or [Double.NaN].
 */
object DerivedFieldCalculator {

    fun calculateAll(dataset: StatDataset): Map<String, List<DerivedCellResult>> {
        if (dataset.derivedFields.isEmpty()) return emptyMap()
        return dataset.derivedFields.associate { definition ->
            definition.id to calculateForDefinition(dataset, definition)
        }
    }

    fun calculateForDefinition(
        dataset: StatDataset,
        definition: DerivedFieldDefinition
    ): List<DerivedCellResult> {
        val fieldLookup = dataset.fields.associateBy { it.id }
        return when (val calc = definition.calculation) {
            is DerivedCalculationType.RatePercentage -> computeRatioSeries(
                rows = dataset.rows,
                definition = definition,
                numeratorFieldId = calc.numeratorFieldId,
                denominatorFieldId = calc.denominatorFieldId,
                constantDenominator = calc.constantDenominator,
                multiplier = 100.0,
                fieldLookup = fieldLookup
            )
            is DerivedCalculationType.PerThousand -> computeRatioSeries(
                rows = dataset.rows,
                definition = definition,
                numeratorFieldId = calc.numeratorFieldId,
                denominatorFieldId = calc.denominatorFieldId,
                constantDenominator = calc.constantDenominator,
                multiplier = 1000.0,
                fieldLookup = fieldLookup
            )
            is DerivedCalculationType.MovingAverage -> computeMovingAverage(
                rows = dataset.rows,
                definition = definition,
                calc = calc,
                fieldLookup = fieldLookup
            )
            is DerivedCalculationType.CumulativeTotal -> computeCumulativeTotal(
                rows = dataset.rows,
                definition = definition,
                calc = calc,
                fieldLookup = fieldLookup
            )
        }
    }

    private fun computeRatioSeries(
        rows: List<StatRow>,
        definition: DerivedFieldDefinition,
        numeratorFieldId: String,
        denominatorFieldId: String?,
        constantDenominator: Double?,
        multiplier: Double,
        fieldLookup: Map<String, StatField>
    ): List<DerivedCellResult> {
        val numName = fieldLookup[numeratorFieldId]?.name ?: "Numerator"
        val denLabel = denominatorFieldId?.let { fieldLookup[it]?.name }
            ?: constantDenominator?.let { formatScalar(it, 2) }
            ?: "Denominator"
        val effectiveUnit = definition.unit.ifBlank { definition.calculation.defaultUnit }
        val meta = "($numName / $denLabel) × ${multiplier.toInt()}"

        return rows.map { row ->
            val numerator = row.values[numeratorFieldId].asDoubleOrNull()
            val denominator = when {
                denominatorFieldId != null -> row.values[denominatorFieldId].asDoubleOrNull()
                else -> constantDenominator?.takeIf { it.isFinite() }
            }

            when {
                numerator == null || denominator == null -> DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = null,
                    formattedValue = "—",
                    unit = effectiveUnit,
                    status = DerivedValueStatus.MISSING_INPUT,
                    calculationMetadata = meta
                )
                denominator == 0.0 -> DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = null,
                    formattedValue = "Undefined (÷0)",
                    unit = effectiveUnit,
                    status = DerivedValueStatus.DIVISION_BY_ZERO,
                    calculationMetadata = meta
                )
                else -> {
                    val computed = (numerator / denominator) * multiplier
                    if (!computed.isFinite()) {
                        DerivedCellResult(
                            rowId = row.id,
                            derivedFieldId = definition.id,
                            numericValue = null,
                            formattedValue = "Undefined (÷0)",
                            unit = effectiveUnit,
                            status = DerivedValueStatus.DIVISION_BY_ZERO,
                            calculationMetadata = meta
                        )
                    } else {
                        DerivedCellResult(
                            rowId = row.id,
                            derivedFieldId = definition.id,
                            numericValue = computed,
                            formattedValue = formatWithUnit(computed, definition.decimalPlaces, effectiveUnit),
                            unit = effectiveUnit,
                            status = DerivedValueStatus.VALID,
                            calculationMetadata = meta
                        )
                    }
                }
            }
        }
    }

    private fun computeMovingAverage(
        rows: List<StatRow>,
        definition: DerivedFieldDefinition,
        calc: DerivedCalculationType.MovingAverage,
        fieldLookup: Map<String, StatField>
    ): List<DerivedCellResult> {
        val safeWindow = calc.windowSize.coerceAtLeast(1)
        val sourceField = fieldLookup[calc.sourceFieldId]
        val sourceName = sourceField?.name ?: "Value"
        val effectiveUnit = definition.unit.ifBlank { sourceField?.unit.orEmpty() }
        val meta = "$safeWindow-period Moving Avg of $sourceName"

        return rows.indices.map { index ->
            val row = rows[index]
            val currentVal = row.values[calc.sourceFieldId].asDoubleOrNull()
            if (currentVal == null) {
                return@map DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = null,
                    formattedValue = "—",
                    unit = effectiveUnit,
                    status = DerivedValueStatus.MISSING_INPUT,
                    calculationMetadata = meta
                )
            }

            val startIdx = (index - safeWindow + 1).coerceAtLeast(0)
            val windowValues = (startIdx..index).mapNotNull { i ->
                rows[i].values[calc.sourceFieldId].asDoubleOrNull()
            }

            if (calc.requireFullWindow && windowValues.size < safeWindow) {
                DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = null,
                    formattedValue = "— (<$safeWindow pts)",
                    unit = effectiveUnit,
                    status = DerivedValueStatus.INSUFFICIENT_WINDOW,
                    calculationMetadata = meta
                )
            } else {
                val avg = windowValues.sum() / windowValues.size.toDouble()
                if (!avg.isFinite()) {
                    DerivedCellResult(
                        rowId = row.id,
                        derivedFieldId = definition.id,
                        numericValue = null,
                        formattedValue = "—",
                        unit = effectiveUnit,
                        status = DerivedValueStatus.MISSING_INPUT,
                        calculationMetadata = meta
                    )
                } else {
                    DerivedCellResult(
                        rowId = row.id,
                        derivedFieldId = definition.id,
                        numericValue = avg,
                        formattedValue = formatWithUnit(avg, definition.decimalPlaces, effectiveUnit),
                        unit = effectiveUnit,
                        status = DerivedValueStatus.VALID,
                        calculationMetadata = meta
                    )
                }
            }
        }
    }

    private fun computeCumulativeTotal(
        rows: List<StatRow>,
        definition: DerivedFieldDefinition,
        calc: DerivedCalculationType.CumulativeTotal,
        fieldLookup: Map<String, StatField>
    ): List<DerivedCellResult> {
        val sourceField = fieldLookup[calc.sourceFieldId]
        val sourceName = sourceField?.name ?: "Value"
        val effectiveUnit = definition.unit.ifBlank { sourceField?.unit.orEmpty() }
        val groupName = calc.groupByFieldId?.let { fieldLookup[it]?.name }
        val meta = if (groupName != null) {
            "Cumulative sum of $sourceName (per $groupName)"
        } else {
            "Cumulative sum of $sourceName"
        }

        val runningByGroup = mutableMapOf<String, Double>()

        return rows.map { row ->
            val currentVal = row.values[calc.sourceFieldId].asDoubleOrNull()
            if (currentVal == null) {
                DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = null,
                    formattedValue = "—",
                    unit = effectiveUnit,
                    status = DerivedValueStatus.MISSING_INPUT,
                    calculationMetadata = meta
                )
            } else {
                val groupKey = if (calc.groupByFieldId != null) {
                    val gv = row.values[calc.groupByFieldId]
                    if (gv.isMissingValue()) "__UNGROUPED__"
                    else when (gv) {
                        is StatValue.TextValue -> gv.value.trim()
                        else -> gv.toString()
                    }
                } else {
                    "__ALL__"
                }
                val updated = (runningByGroup[groupKey] ?: 0.0) + currentVal
                runningByGroup[groupKey] = updated
                DerivedCellResult(
                    rowId = row.id,
                    derivedFieldId = definition.id,
                    numericValue = updated,
                    formattedValue = formatWithUnit(updated, definition.decimalPlaces, effectiveUnit),
                    unit = effectiveUnit,
                    status = DerivedValueStatus.VALID,
                    calculationMetadata = meta
                )
            }
        }
    }

    private fun formatWithUnit(value: Double, decimalPlaces: Int, unit: String): String {
        val numberStr = formatScalar(value, decimalPlaces)
        return when {
            unit.isBlank() -> numberStr
            unit == "%" -> "$numberStr%"
            else -> "$numberStr $unit"
        }
    }

    private fun formatScalar(value: Double, decimalPlaces: Int): String {
        if (!value.isFinite()) return "—"
        if (value % 1.0 == 0.0 && decimalPlaces == 0) return value.toLong().toString()
        val pattern = "%.${decimalPlaces.coerceIn(0, 4)}f"
        return String.format(Locale.US, pattern, value)
            .let { formatted ->
                if (formatted.contains('.')) formatted.trimEnd('0').trimEnd('.') else formatted
            }
    }
}
