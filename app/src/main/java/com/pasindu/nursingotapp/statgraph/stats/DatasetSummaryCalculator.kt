package com.pasindu.nursingotapp.statgraph.stats

import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.model.isMissingValue

/**
 * Aggregate quality and completeness summary for a [StatDataset].
 * Missing cells are counted explicitly and never coerced to zero.
 */
data class DatasetSummaryMetrics(
    val fieldCount: Int,
    val rowCount: Int,
    val derivedCount: Int,
    val missingCellCount: Int,
    val completenessPercent: Int,
    val smallCountCellCount: Int
)

object DatasetSummaryCalculator {

    fun summarize(dataset: StatDataset): DatasetSummaryMetrics {
        val fieldCount = dataset.fields.size
        val rowCount = dataset.rows.size
        val totalCells = fieldCount * rowCount

        var missingCells = 0
        var smallCounts = 0

        for (row in dataset.rows) {
            for (field in dataset.fields) {
                val cell = row.values[field.id]
                if (cell.isMissingValue()) {
                    missingCells++
                } else if (field.role is FieldRole.Measure || field.role is FieldRole.PartOfMeasure) {
                    val numeric = cell.asDoubleOrNull()
                    if (numeric != null && numeric > 0.0 && numeric < 5.0) {
                        smallCounts++
                    }
                }
            }
        }

        val completeness = if (totalCells == 0) {
            100
        } else {
            (((totalCells - missingCells).toDouble() / totalCells.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        }

        return DatasetSummaryMetrics(
            fieldCount = fieldCount,
            rowCount = rowCount,
            derivedCount = dataset.derivedFields.size,
            missingCellCount = missingCells,
            completenessPercent = completeness,
            smallCountCellCount = smallCounts
        )
    }
}
