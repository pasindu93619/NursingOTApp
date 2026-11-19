package com.pasindu.nursingotapp.statgraph.engine.recommendation

import com.pasindu.nursingotapp.statgraph.engine.diagnostics.DatasetDiagnosticsReport
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.asDoubleOrNull
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldCalculator
import com.pasindu.nursingotapp.statgraph.stats.DerivedValueStatus
import com.pasindu.nursingotapp.statgraph.stats.StatisticsEngine

/**
 * Generates plain-language dataset summaries and recommendation explanations
 * strictly grounded in actual supplied rows, statistics, and diagnostics.
 */
object DatasetNarrativeExplainer {

    fun buildDerivedEvidence(dataset: StatDataset): List<String> {
        val evidence = mutableListOf<String>()
        val rowLookup = dataset.rows.associateBy { it.id }

        if (dataset.derivedFields.isNotEmpty()) {
            val allDerived = DerivedFieldCalculator.calculateAll(dataset)
            for (definition in dataset.derivedFields) {
                val cells = allDerived[definition.id].orEmpty()
                val validParts = mutableListOf<String>()
                var skippedMissing = 0
                var zeroDenom = 0

                for (cell in cells) {
                    val label = resolveRowLabel(dataset, rowLookup[cell.rowId])
                    when (cell.status) {
                        DerivedValueStatus.VALID -> {
                            validParts.add("$label=${cell.formattedValue}")
                        }
                        DerivedValueStatus.MISSING_INPUT -> skippedMissing++
                        DerivedValueStatus.DIVISION_BY_ZERO -> zeroDenom++
                        DerivedValueStatus.INSUFFICIENT_WINDOW -> Unit
                    }
                }

                if (validParts.isNotEmpty()) {
                    val sample = validParts.take(6).joinToString(", ")
                    val suffix = buildString {
                        if (validParts.size > 6) append(" (+${validParts.size - 6} more)")
                        if (skippedMissing > 0) append("; $skippedMissing row(s) excluded for missing input")
                        if (zeroDenom > 0) append("; $zeroDenom row(s) undefined due to zero denominator")
                    }
                    evidence.add("${definition.name}: $sample$suffix.")
                }
            }
        }

        // Also summarize PartOfMeasure -> parent Measure proportions directly when not already defined
        val coveredPartParentPairs = dataset.derivedFields.mapNotNull { def ->
            when (val calc = def.calculation) {
                is DerivedCalculationType.RatePercentage ->
                    calc.numeratorFieldId to (calc.denominatorFieldId ?: "")
                else -> null
            }
        }.toSet()

        val fieldById = dataset.fields.associateBy { it.id }
        val partFields = dataset.fields.filter { it.role is FieldRole.PartOfMeasure }
        for (partField in partFields) {
            val role = partField.role as FieldRole.PartOfMeasure
            val parentId = role.parentFieldId ?: continue
            val parentField = fieldById[parentId] ?: continue
            if ((partField.id to parentId) in coveredPartParentPairs) continue

            val rowSummaries = mutableListOf<String>()
            for (row in dataset.rows) {
                val partVal = row.values[partField.id].asDoubleOrNull()
                val totalVal = row.values[parentId].asDoubleOrNull()
                if (partVal != null && totalVal != null) {
                    val pct = StatisticsEngine.percentage(partVal, totalVal)
                    if (pct != null) {
                        val label = resolveRowLabel(dataset, row)
                        rowSummaries.add(
                            "$label=${StatisticsEngine.formatNumber(pct)}% " +
                                "(${StatisticsEngine.formatNumber(partVal)}/${StatisticsEngine.formatNumber(totalVal)})"
                        )
                    }
                }
            }
            if (rowSummaries.isNotEmpty()) {
                val shown = rowSummaries.take(6).joinToString(", ")
                val extra = if (rowSummaries.size > 6) " (+${rowSummaries.size - 6} more)" else ""
                evidence.add("${partField.name} share of ${parentField.name}: $shown$extra.")
            }
        }

        return evidence
    }

    fun buildNarrative(
        dataset: StatDataset,
        taskClassification: TaskClassificationResult,
        diagnosticsReport: DatasetDiagnosticsReport,
        topRecommendations: List<ChartRecommendation>,
        derivedEvidence: List<String>
    ): String {
        val parts = mutableListOf<String>()

        val timeFields = dataset.fields.filter { it.role is FieldRole.Time }
        val categoryFields = dataset.fields.filter { it.role is FieldRole.Category }
        val domainText = when {
            timeFields.isNotEmpty() -> {
                val tf = timeFields.first()
                "${tf.name} (${tf.role.displayName})"
            }
            categoryFields.isNotEmpty() -> {
                "${categoryFields.first().name} categories"
            }
            else -> "unindexed records"
        }

        parts.add(
            "Dataset '${dataset.name}' has ${dataset.rows.size} observation(s) organized by $domainText."
        )

        val numericFieldFacts = dataset.fields.mapNotNull { field ->
            val summary = diagnosticsReport.fieldSummaries[field.id] ?: return@mapNotNull null
            if (!field.role.isNumeric || summary.validCount == 0) return@mapNotNull null
            val minStr = summary.min?.let { StatisticsEngine.formatNumber(it) } ?: "—"
            val maxStr = summary.max?.let { StatisticsEngine.formatNumber(it) } ?: "—"
            val meanStr = summary.mean?.let { StatisticsEngine.formatNumber(it) } ?: "—"
            val unitSuffix = if (field.unit.isNotBlank()) " ${field.unit}" else ""
            val missingNote = if (summary.missingCount > 0) " (${summary.missingCount} missing kept empty)" else ""
            "${field.name}: n=${summary.validCount}, range $minStr to $maxStr$unitSuffix, mean $meanStr$unitSuffix$missingNote"
        }
        if (numericFieldFacts.isNotEmpty()) {
            parts.add("Observed numeric ranges — ${numericFieldFacts.joinToString("; ")}.")
        }

        if (derivedEvidence.isNotEmpty()) {
            parts.add("Calculated rates/proportions: ${derivedEvidence.joinToString(" ")}")
        }

        val blockingCount = diagnosticsReport.blockingDiagnostics.size
        val warningCount = diagnosticsReport.warningDiagnostics.size
        when {
            blockingCount > 0 -> {
                val firstMsg = diagnosticsReport.blockingDiagnostics.first().message
                parts.add("Data quality alert: $blockingCount blocking issue(s) and $warningCount warning(s) detected ($firstMsg).")
            }
            warningCount > 0 -> {
                val firstWarning = diagnosticsReport.warningDiagnostics.first().message
                parts.add("Data quality note: $warningCount warning(s) detected ($firstWarning).")
            }
            else -> {
                parts.add("No blocking errors or statistical warnings were detected.")
            }
        }

        parts.add("Primary analytical task: ${taskClassification.primaryTask.displayName} (${taskClassification.rationale})")

        if (topRecommendations.isNotEmpty()) {
            val topList = topRecommendations.joinToString(", ") { rec ->
                "${rec.specification.canonicalName} (score ${StatisticsEngine.formatNumber(rec.scoreBreakdown.finalScore, 1)}/100)"
            }
            parts.add("Top suitable chart options: $topList.")
        } else {
            parts.add("No chart could be safely recommended until blocking dataset issues are resolved.")
        }

        return parts.joinToString(" ")
    }

    /**
     * Generates a nurse-friendly, jargon-free summary grounded strictly in actual dataset rows and units.
     */
    fun buildSimpleNarrative(
        dataset: StatDataset,
        diagnosticsReport: DatasetDiagnosticsReport,
        topRecommendations: List<ChartRecommendation>,
    ): String {
        if (dataset.rows.isEmpty()) {
            return "Add a few rows of data to see a summary of your numbers."
        }

        val sentences = mutableListOf<String>()
        val timeField = dataset.fields.firstOrNull { it.role is FieldRole.Time }
        val categoryField = dataset.fields.firstOrNull { it.role is FieldRole.Category }
        val primaryMeasure = dataset.fields.firstOrNull {
            it.role is FieldRole.PartOfMeasure || it.role is FieldRole.Measure || it.role.isNumeric
        }

        if (primaryMeasure != null) {
            val unitSuffix = if (primaryMeasure.unit.isNotBlank()) " ${primaryMeasure.unit}" else ""
            val observations = dataset.rows.mapNotNull { row ->
                val v = row.values[primaryMeasure.id].asDoubleOrNull() ?: return@mapNotNull null
                val label = resolveRowLabel(dataset, row)
                label to v
            }

            if (observations.size >= 2) {
                if (timeField != null) {
                    val first = observations.first()
                    val last = observations.last()
                    val changeWord = when {
                        last.second > first.second -> "increased"
                        last.second < first.second -> "decreased"
                        else -> "stayed steady"
                    }
                    val firstText = "${StatisticsEngine.formatNumber(first.second)}$unitSuffix in ${first.first}"
                    val lastText = "${StatisticsEngine.formatNumber(last.second)}$unitSuffix in ${last.first}"
                    if (last.second == first.second) {
                        sentences.add(
                            "${primaryMeasure.name} stayed steady at ${StatisticsEngine.formatNumber(first.second)}$unitSuffix from ${first.first} to ${last.first}.",
                        )
                    } else {
                        sentences.add(
                            "${primaryMeasure.name} $changeWord from $firstText to $lastText.",
                        )
                    }
                    val peak = observations.maxByOrNull { it.second }
                    if (peak != null && peak.first != first.first && peak.first != last.first) {
                        sentences.add(
                            "The highest point was ${StatisticsEngine.formatNumber(peak.second)}$unitSuffix in ${peak.first}.",
                        )
                    }
                } else {
                    val lowest = observations.minByOrNull { it.second }
                    val highest = observations.maxByOrNull { it.second }
                    if (lowest != null && highest != null) {
                        val scope = if (categoryField != null) {
                            "Across ${observations.size} ${categoryField.name.lowercase()} entries"
                        } else {
                            "Across ${observations.size} entries"
                        }
                        sentences.add(
                            "$scope, ${primaryMeasure.name} ranged from ${StatisticsEngine.formatNumber(lowest.second)}$unitSuffix (${lowest.first}) to ${StatisticsEngine.formatNumber(highest.second)}$unitSuffix (${highest.first}).",
                        )
                    }
                }
            } else if (observations.size == 1) {
                val only = observations.first()
                sentences.add(
                    "${primaryMeasure.name} is ${StatisticsEngine.formatNumber(only.second)}$unitSuffix for ${only.first}.",
                )
            }

            val summary = diagnosticsReport.fieldSummaries[primaryMeasure.id]
            if (summary != null && summary.missingCount > 0) {
                sentences.add(
                    "${summary.missingCount} row(s) have blank ${primaryMeasure.name} cells and were kept empty.",
                )
            }
        } else {
            sentences.add(
                "${dataset.name.ifBlank { "This dataset" }} currently has ${dataset.rows.size} row(s) ready for review.",
            )
        }

        val blockingCount = diagnosticsReport.blockingDiagnostics.size
        val warningCount = diagnosticsReport.warningDiagnostics.size
        when {
            blockingCount > 0 -> sentences.add(
                "There are $blockingCount item(s) to fix before drawing a chart.",
            )
            warningCount > 0 -> sentences.add(
                "There are $warningCount helpful note(s) to review in Data checks & privacy.",
            )
        }

        val bestChart = topRecommendations.firstOrNull()
        if (bestChart != null) {
            sentences.add(
                "A ${bestChart.specification.canonicalName.lowercase()} is a great match for showing this data clearly.",
            )
        }

        return sentences.joinToString(" ")
    }

    private fun resolveRowLabel(
        dataset: StatDataset,
        row: com.pasindu.nursingotapp.statgraph.model.StatRow?
    ): String {
        if (row == null) return "Row"
        if (row.periodOrCategoryLabel.isNotBlank()) return row.periodOrCategoryLabel.trim()
        val domainField = dataset.fields.firstOrNull {
            it.role is FieldRole.Time || it.role is FieldRole.Category || it.role is FieldRole.Label
        }
        val raw = domainField?.let { row.values[it.id] }
        return when (raw) {
            is StatValue.TimePeriodValue -> raw.period?.formatDisplay() ?: raw.rawText
            is StatValue.TextValue -> raw.value
            else -> row.id
        }.ifBlank { row.id }
    }
}
