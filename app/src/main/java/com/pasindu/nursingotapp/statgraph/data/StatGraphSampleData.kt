package com.pasindu.nursingotapp.statgraph.data

import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import java.time.YearMonth

/**
 * Aggregate-only sample datasets for previews and studio onboarding.
 * Contains zero patient-level or personally identifiable data.
 */
object StatGraphSampleData {

    private const val FIELD_MONTH = "f_month"
    private const val FIELD_WARD = "f_ward"
    private const val FIELD_DENGUE = "f_dengue"
    private const val FIELD_TOTAL = "f_total"
    private const val FIELD_OCCUPANCY = "f_occupancy"
    private const val FIELD_RATE_EST = "f_rate_est"

    fun emptyDataset(): StatDataset = StatDataset(
        id = "ds_empty",
        name = "",
        description = "",
        fields = emptyList(),
        rows = emptyList(),
        derivedFields = emptyList(),
        metadata = DatasetMetadata(sourceLabel = "New Dataset")
    )

    fun dengueMonthlyPopulatedDataset(): StatDataset {
        val fields = listOf(
            StatField(
                id = FIELD_MONTH,
                name = "Month",
                unit = "",
                role = FieldRole.Time.Month,
                suggestedRole = FieldRole.Time.Month,
                isRoleConfirmed = true,
                isRequired = true
            ),
            StatField(
                id = FIELD_WARD,
                name = "Ward Category",
                unit = "",
                role = FieldRole.Category,
                suggestedRole = FieldRole.Category,
                isRoleConfirmed = true,
                allowedCategories = listOf("Medical", "Pediatric", "ICU")
            ),
            StatField(
                id = FIELD_DENGUE,
                name = "Dengue Patients",
                unit = "patients",
                role = FieldRole.PartOfMeasure(parentFieldId = FIELD_TOTAL),
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0
            ),
            StatField(
                id = FIELD_TOTAL,
                name = "Total Patients",
                unit = "patients",
                role = FieldRole.Measure,
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0,
                isRequired = true
            ),
            StatField(
                id = FIELD_OCCUPANCY,
                name = "Bed Occupancy",
                unit = "%",
                role = FieldRole.RateContinuous(scaleHint = "%"),
                suggestedRole = FieldRole.RateContinuous(scaleHint = "%"),
                isRoleConfirmed = true,
                minValue = 0.0,
                maxValue = 150.0
            )
        )

        val rows = listOf(
            sampleRow("r1", 2026, 1, "Medical", 18, 120, 84.5),
            sampleRow("r2", 2026, 2, "Medical", 24, 132, 88.0),
            sampleRow("r3", 2026, 3, "Medical", 31, 145, 92.4),
            sampleRow("r4", 2026, 4, "Medical", 27, 138, 86.2),
            sampleRow("r5", 2026, 5, "Medical", 19, 126, 79.8)
        )

        val derived = listOf(
            DerivedFieldDefinition(
                id = "d_pct",
                name = "Dengue Share",
                unit = "%",
                calculation = DerivedCalculationType.RatePercentage(
                    numeratorFieldId = FIELD_DENGUE,
                    denominatorFieldId = FIELD_TOTAL
                ),
                formulaSummary = "(Dengue Patients / Total Patients) × 100"
            ),
            DerivedFieldDefinition(
                id = "d_ma3",
                name = "Dengue 3-Mo Avg",
                unit = "patients",
                calculation = DerivedCalculationType.MovingAverage(
                    sourceFieldId = FIELD_DENGUE,
                    windowSize = 3
                ),
                formulaSummary = "3-month trailing average"
            )
        )

        return StatDataset(
            id = "ds_dengue_sample",
            name = "Monthly Ward Dengue Surveillance",
            description = "Aggregate monthly admissions & dengue share across Medical ward (no patient identifiers).",
            fields = fields,
            rows = rows,
            derivedFields = derived,
            metadata = DatasetMetadata(sourceLabel = "Sample Aggregate Data")
        )
    }

    fun validationWarningDataset(): StatDataset {
        val fields = listOf(
            StatField(
                id = FIELD_MONTH,
                name = "Month",
                role = FieldRole.Time.Month,
                suggestedRole = FieldRole.Time.Month,
                isRoleConfirmed = true,
                isRequired = true
            ),
            StatField(
                id = FIELD_DENGUE,
                name = "Dengue Patients",
                unit = "patients",
                role = FieldRole.PartOfMeasure(parentFieldId = FIELD_TOTAL),
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = false,
                minValue = 0.0
            ),
            StatField(
                id = FIELD_TOTAL,
                name = "Total Patients",
                unit = "patients",
                role = FieldRole.Measure,
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0,
                isRequired = true
            ),
            StatField(
                id = FIELD_RATE_EST,
                name = "Readmission Rate (95% CI)",
                unit = "%",
                role = FieldRole.EstimateWithLimits(),
                suggestedRole = FieldRole.EstimateWithLimits(),
                isRoleConfirmed = true,
                minValue = 0.0,
                maxValue = 100.0
            )
        )

        val rows = listOf(
            // Row 1: Small count (< 5 warning) + valid estimate
            StatRow(
                id = "w1",
                periodOrCategoryLabel = "Jan 2026",
                values = mapOf(
                    FIELD_MONTH to StatValue.TimePeriodValue(TimePeriod.Month(YearMonth.of(2026, 1)), "2026-01"),
                    FIELD_DENGUE to StatValue.WholeNumber(3),
                    FIELD_TOTAL to StatValue.WholeNumber(95),
                    FIELD_RATE_EST to StatValue.EstimateValue(estimate = 4.2, lowerLimit = 2.1, upperLimit = 6.8)
                )
            ),
            // Row 2: Gap skips Feb 2026 -> Mar 2026, plus missing Dengue count (must stay null, NOT 0)
            StatRow(
                id = "w2",
                periodOrCategoryLabel = "Mar 2026",
                values = mapOf(
                    FIELD_MONTH to StatValue.TimePeriodValue(TimePeriod.Month(YearMonth.of(2026, 3)), "2026-03"),
                    FIELD_DENGUE to null,
                    FIELD_TOTAL to StatValue.WholeNumber(110),
                    FIELD_RATE_EST to StatValue.EstimateValue(estimate = 5.0, lowerLimit = 7.5, upperLimit = 4.0) // lower > upper error
                )
            ),
            // Row 3: Duplicate Mar 2026 warning + part exceeds total error
            StatRow(
                id = "w3",
                periodOrCategoryLabel = "Mar 2026",
                values = mapOf(
                    FIELD_MONTH to StatValue.TimePeriodValue(TimePeriod.Month(YearMonth.of(2026, 3)), "2026-03"),
                    FIELD_DENGUE to StatValue.WholeNumber(48),
                    FIELD_TOTAL to StatValue.WholeNumber(40),
                    FIELD_RATE_EST to StatValue.EstimateValue(estimate = 6.1, lowerLimit = 3.9, upperLimit = 8.4)
                )
            )
        )

        return StatDataset(
            id = "ds_validation_demo",
            name = "Ward Quality & Audit Check",
            description = "Demonstrates missing-value preservation, small-count (<5) warnings, period gaps, and blocking errors.",
            fields = fields,
            rows = rows,
            derivedFields = listOf(
                DerivedFieldDefinition(
                    id = "d_per1k",
                    name = "Dengue per 1,000",
                    unit = "per 1,000",
                    calculation = DerivedCalculationType.PerThousand(
                        numeratorFieldId = FIELD_DENGUE,
                        denominatorFieldId = FIELD_TOTAL
                    ),
                    formulaSummary = "(Dengue Patients / Total Patients) × 1,000"
                )
            ),
            metadata = DatasetMetadata(sourceLabel = "Validation Demo")
        )
    }

    val sampleCsvText: String = """
        Month,Ward Category,Dengue Patients,Total Patients,Percentage
        2026-01,Medical,18,120,15.0
        2026-02,Medical,,132,
        2026-03,Medical,31,145,21.4
        2026-04,Pediatric,4,68,5.9
    """.trimIndent()

    fun pairedWardPrePostDataset(): StatDataset = StatDataset(
        id = "ds_ward_pre_post_sample",
        name = "Fictional Ward Audit (Before vs After)",
        description = "Fictional aggregate hand-hygiene compliance (%) before and after training.",
        fields = listOf(
            StatField(id = "f_ward", name = "Ward", role = FieldRole.Category, isRoleConfirmed = true),
            StatField(id = "f_pre", name = "Baseline", unit = "%", role = FieldRole.RateContinuous(scaleHint = "%"), isRoleConfirmed = true),
            StatField(id = "f_post", name = "Follow-up", unit = "%", role = FieldRole.RateContinuous(scaleHint = "%"), isRoleConfirmed = true),
            StatField(id = "f_target", name = "Target", unit = "%", role = FieldRole.RateContinuous(scaleHint = "%"), isRoleConfirmed = true),
        ),
        rows = listOf(
            StatRow("p1", "Medical", mapOf("f_ward" to StatValue.TextValue("Medical"), "f_pre" to StatValue.DecimalNumber(68.0), "f_post" to StatValue.DecimalNumber(84.0), "f_target" to StatValue.DecimalNumber(85.0))),
            StatRow("p2", "Surgical", mapOf("f_ward" to StatValue.TextValue("Surgical"), "f_pre" to StatValue.DecimalNumber(72.0), "f_post" to StatValue.DecimalNumber(89.0), "f_target" to StatValue.DecimalNumber(85.0))),
            StatRow("p3", "ICU", mapOf("f_ward" to StatValue.TextValue("ICU"), "f_pre" to StatValue.DecimalNumber(81.0), "f_post" to StatValue.DecimalNumber(93.0), "f_target" to StatValue.DecimalNumber(90.0))),
            StatRow("p4", "Pediatric", mapOf("f_ward" to StatValue.TextValue("Pediatric"), "f_pre" to StatValue.DecimalNumber(75.0), "f_post" to StatValue.DecimalNumber(82.0), "f_target" to StatValue.DecimalNumber(85.0))),
        ),
        metadata = DatasetMetadata(sourceLabel = "Fictional sample data (for demonstration only)")
    )

    fun dailyCalendarSampleDataset(): StatDataset = StatDataset(
        id = "ds_daily_calendar_sample",
        name = "Fictional Daily Ward Admissions",
        description = "Fictional daily aggregate admissions with a measured zero and a missing day.",
        fields = listOf(
            StatField(id = "f_day", name = "Date", role = FieldRole.Time.Day, isRoleConfirmed = true),
            StatField(id = "f_adm", name = "Admissions", unit = "patients", role = FieldRole.Measure, isRoleConfirmed = true),
        ),
        rows = listOf(
            StatRow("d1", "2026-03-01", mapOf("f_day" to StatValue.TimePeriodValue(TimePeriod.Day(java.time.LocalDate.of(2026, 3, 1)), "2026-03-01"), "f_adm" to StatValue.WholeNumber(4))),
            StatRow("d2", "2026-03-02", mapOf("f_day" to StatValue.TimePeriodValue(TimePeriod.Day(java.time.LocalDate.of(2026, 3, 2)), "2026-03-02"), "f_adm" to StatValue.WholeNumber(0))),
            StatRow("d3", "2026-03-03", mapOf("f_day" to StatValue.TimePeriodValue(TimePeriod.Day(java.time.LocalDate.of(2026, 3, 3)), "2026-03-03"), "f_adm" to StatValue.Missing)),
            StatRow("d4", "2026-03-04", mapOf("f_day" to StatValue.TimePeriodValue(TimePeriod.Day(java.time.LocalDate.of(2026, 3, 4)), "2026-03-04"), "f_adm" to StatValue.WholeNumber(7))),
        ),
        metadata = DatasetMetadata(sourceLabel = "Fictional sample data (for demonstration only)")
    )

    private fun sampleRow(
        id: String,
        year: Int,
        month: Int,
        ward: String,
        dengue: Long,
        total: Long,
        occupancy: Double
    ): StatRow {
        val period = TimePeriod.Month(YearMonth.of(year, month))
        return StatRow(
            id = id,
            periodOrCategoryLabel = period.formatDisplay(),
            values = mapOf(
                FIELD_MONTH to StatValue.TimePeriodValue(period, period.formatIso()),
                FIELD_WARD to StatValue.TextValue(ward),
                FIELD_DENGUE to StatValue.WholeNumber(dengue),
                FIELD_TOTAL to StatValue.WholeNumber(total),
                FIELD_OCCUPANCY to StatValue.DecimalNumber(occupancy)
            )
        )
    }
}
