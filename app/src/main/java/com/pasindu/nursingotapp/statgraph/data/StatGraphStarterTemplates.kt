package com.pasindu.nursingotapp.statgraph.data

import com.pasindu.nursingotapp.statgraph.engine.recommendation.AnalyticalTask
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import java.time.LocalDate
import java.time.YearMonth

/**
 * Plain-language goals on the Guided Start screen, mapped to existing [AnalyticalTask] values.
 */
enum class StarterGoal(
    val analyticalTask: AnalyticalTask,
    val title: String,
    val subtitle: String,
    val defaultTemplateId: StatGraphStarterTemplateId
) {
    CHANGE_OVER_TIME(
        analyticalTask = AnalyticalTask.SHOW_CHANGE_OVER_TIME,
        title = "Change over time",
        subtitle = "Track monthly admissions, vital signs, or fluid balance across dates.",
        defaultTemplateId = StatGraphStarterTemplateId.DENGUE_MONTHLY
    ),
    COMPARE_GROUPS(
        analyticalTask = AnalyticalTask.COMPARE_CATEGORIES,
        title = "Compare groups",
        subtitle = "Compare counts or rates across wards, age groups, or sex.",
        defaultTemplateId = StatGraphStarterTemplateId.WARD_INFECTION_RATE
    ),
    SHARE_OF_TOTAL(
        analyticalTask = AnalyticalTask.SHOW_COMPOSITION,
        title = "Share of a total",
        subtitle = "Show how part counts contribute to a total patient count.",
        defaultTemplateId = StatGraphStarterTemplateId.DENGUE_MONTHLY
    ),
    RESEARCH_RANGES(
        analyticalTask = AnalyticalTask.SHOW_UNCERTAINTY,
        title = "Research ranges",
        subtitle = "Explore measurements, intake/output balance, and statistical ranges.",
        defaultTemplateId = StatGraphStarterTemplateId.VITAL_SIGNS_OVER_TIME
    )
}

enum class StatGraphStarterTemplateId {
    DENGUE_MONTHLY,
    WARD_INFECTION_RATE,
    VITAL_SIGNS_OVER_TIME,
    FLUID_BALANCE,
    AGE_AND_SEX_COUNTS,
    BLANK
}

data class StatGraphStarterTemplate(
    val id: StatGraphStarterTemplateId,
    val title: String,
    val shortDescription: String,
    val isFictionalSample: Boolean,
    val samplePreviewLines: List<String>,
    val buildDataset: () -> StatDataset
)

/**
 * Six nurse-friendly healthcare starter templates using existing dataset/field-role models.
 * All populated templates use clearly labelled fictional demonstration numbers.
 */
object StatGraphStarterTemplates {

    const val FICTIONAL_SOURCE_LABEL = "Fictional sample data (for demonstration only)"

    val allTemplates: List<StatGraphStarterTemplate> = listOf(
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.DENGUE_MONTHLY,
            title = "Dengue monthly",
            shortDescription = "Month, dengue patient count, and total patient count with share (%).",
            isFictionalSample = true,
            samplePreviewLines = listOf(
                "Jan 2026: 18 dengue / 120 total",
                "Feb 2026: 24 dengue / 132 total",
                "Mar 2026: 31 dengue / 145 total"
            ),
            buildDataset = ::buildDengueMonthly
        ),
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.WARD_INFECTION_RATE,
            title = "Ward infection rate",
            shortDescription = "Ward name, infection count, admission denominator, and infection rate (%).",
            isFictionalSample = true,
            samplePreviewLines = listOf(
                "Medical: 6 infections / 140 admissions",
                "Surgical: 9 infections / 115 admissions",
                "ICU: 5 infections / 42 admissions"
            ),
            buildDataset = ::buildWardInfectionRate
        ),
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.VITAL_SIGNS_OVER_TIME,
            title = "Vital signs over time",
            shortDescription = "Date, systolic blood pressure (mmHg), and heart rate (bpm).",
            isFictionalSample = true,
            samplePreviewLines = listOf(
                "01 Mar: 128 mmHg · 78 bpm",
                "02 Mar: 134 mmHg · 82 bpm",
                "03 Mar: 122 mmHg · 74 bpm"
            ),
            buildDataset = ::buildVitalSignsOverTime
        ),
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.FLUID_BALANCE,
            title = "Fluid balance",
            shortDescription = "Day, fluid intake (mL), output (mL), and net balance (mL).",
            isFictionalSample = true,
            samplePreviewLines = listOf(
                "01 Mar: In 2100 mL · Out 1850 mL · +250 mL",
                "02 Mar: In 1950 mL · Out 2050 mL · -100 mL"
            ),
            buildDataset = ::buildFluidBalance
        ),
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.AGE_AND_SEX_COUNTS,
            title = "Age and sex counts",
            shortDescription = "Age group, sex group, and patient count.",
            isFictionalSample = true,
            samplePreviewLines = listOf(
                "18–39 · Female: 34 patients",
                "18–39 · Male: 28 patients",
                "40–59 · Female: 41 patients"
            ),
            buildDataset = ::buildAgeAndSexCounts
        ),
        StatGraphStarterTemplate(
            id = StatGraphStarterTemplateId.BLANK,
            title = "Blank",
            shortDescription = "Empty dataset with no assumed columns. Add your own fields or import CSV.",
            isFictionalSample = false,
            samplePreviewLines = emptyList(),
            buildDataset = { StatGraphSampleData.emptyDataset() }
        )
    )

    fun templateById(id: StatGraphStarterTemplateId): StatGraphStarterTemplate =
        allTemplates.first { it.id == id }

    private fun buildDengueMonthly(): StatDataset {
        val monthId = "tpl_dengue_month"
        val dengueId = "tpl_dengue_count"
        val totalId = "tpl_dengue_total"
        val fields = listOf(
            StatField(
                id = monthId,
                name = "Month",
                role = FieldRole.Time.Month,
                suggestedRole = FieldRole.Time.Month,
                isRoleConfirmed = true,
                isRequired = true
            ),
            StatField(
                id = dengueId,
                name = "Dengue Patients",
                unit = "patients",
                role = FieldRole.PartOfMeasure(parentFieldId = totalId),
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0
            ),
            StatField(
                id = totalId,
                name = "Total Patients",
                unit = "patients",
                role = FieldRole.Measure,
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0,
                isRequired = true
            )
        )
        val rows = listOf(
            Triple(1, 18L, 120L),
            Triple(2, 24L, 132L),
            Triple(3, 31L, 145L),
            Triple(4, 27L, 138L),
            Triple(5, 19L, 126L)
        ).mapIndexed { index, (month, dengue, total) ->
            val period = TimePeriod.Month(YearMonth.of(2026, month))
            StatRow(
                id = "tpl_dengue_r${index + 1}",
                periodOrCategoryLabel = period.formatDisplay(),
                values = mapOf(
                    monthId to StatValue.TimePeriodValue(period, period.formatIso()),
                    dengueId to StatValue.WholeNumber(dengue),
                    totalId to StatValue.WholeNumber(total)
                )
            )
        }
        val derived = listOf(
            DerivedFieldDefinition(
                id = "tpl_dengue_pct",
                name = "Dengue Share",
                unit = "%",
                calculation = DerivedCalculationType.RatePercentage(
                    numeratorFieldId = dengueId,
                    denominatorFieldId = totalId
                ),
                formulaSummary = "(Dengue Patients / Total Patients) × 100"
            )
        )
        return StatDataset(
            id = "ds_tpl_dengue_monthly",
            name = "Dengue Monthly (Sample)",
            description = "Fictional sample data for demonstrating monthly dengue counts vs. total patients.",
            fields = fields,
            rows = rows,
            derivedFields = derived,
            metadata = DatasetMetadata(sourceLabel = FICTIONAL_SOURCE_LABEL)
        )
    }

    private fun buildWardInfectionRate(): StatDataset {
        val wardId = "tpl_inf_ward"
        val infId = "tpl_inf_cases"
        val admId = "tpl_inf_admissions"
        val fields = listOf(
            StatField(
                id = wardId,
                name = "Ward",
                role = FieldRole.Category,
                suggestedRole = FieldRole.Category,
                isRoleConfirmed = true,
                isRequired = true,
                allowedCategories = listOf("Medical", "Surgical", "Paediatric", "ICU")
            ),
            StatField(
                id = infId,
                name = "Infections",
                unit = "cases",
                role = FieldRole.PartOfMeasure(parentFieldId = admId),
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0
            ),
            StatField(
                id = admId,
                name = "Admissions",
                unit = "patients",
                role = FieldRole.Measure,
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0,
                isRequired = true
            )
        )
        val rows = listOf(
            Triple("Medical", 6L, 140L),
            Triple("Surgical", 9L, 115L),
            Triple("Paediatric", 3L, 88L),
            Triple("ICU", 5L, 42L)
        ).mapIndexed { index, (ward, cases, adm) ->
            StatRow(
                id = "tpl_inf_r${index + 1}",
                periodOrCategoryLabel = ward,
                values = mapOf(
                    wardId to StatValue.TextValue(ward),
                    infId to StatValue.WholeNumber(cases),
                    admId to StatValue.WholeNumber(adm)
                )
            )
        }
        val derived = listOf(
            DerivedFieldDefinition(
                id = "tpl_inf_rate",
                name = "Infection Rate",
                unit = "%",
                calculation = DerivedCalculationType.RatePercentage(
                    numeratorFieldId = infId,
                    denominatorFieldId = admId
                ),
                formulaSummary = "(Infections / Admissions) × 100"
            )
        )
        return StatDataset(
            id = "ds_tpl_ward_infection",
            name = "Ward Infection Rate (Sample)",
            description = "Fictional sample data comparing ward infection counts and admission denominators.",
            fields = fields,
            rows = rows,
            derivedFields = derived,
            metadata = DatasetMetadata(sourceLabel = FICTIONAL_SOURCE_LABEL)
        )
    }

    private fun buildVitalSignsOverTime(): StatDataset {
        val dateId = "tpl_vs_date"
        val sbpId = "tpl_vs_sbp"
        val hrId = "tpl_vs_hr"
        val fields = listOf(
            StatField(
                id = dateId,
                name = "Date",
                role = FieldRole.Time.Day,
                suggestedRole = FieldRole.Time.Day,
                isRoleConfirmed = true,
                isRequired = true
            ),
            StatField(
                id = sbpId,
                name = "Systolic BP",
                unit = "mmHg",
                role = FieldRole.RateContinuous(scaleHint = "mmHg"),
                suggestedRole = FieldRole.RateContinuous(scaleHint = "mmHg"),
                isRoleConfirmed = true,
                minValue = 40.0,
                maxValue = 260.0
            ),
            StatField(
                id = hrId,
                name = "Heart Rate",
                unit = "bpm",
                role = FieldRole.RateContinuous(scaleHint = "bpm"),
                suggestedRole = FieldRole.RateContinuous(scaleHint = "bpm"),
                isRoleConfirmed = true,
                minValue = 30.0,
                maxValue = 220.0
            )
        )
        val rows = listOf(
            Triple(1, 128.0, 78.0),
            Triple(2, 134.0, 82.0),
            Triple(3, 122.0, 74.0),
            Triple(4, 126.0, 76.0),
            Triple(5, 118.0, 72.0)
        ).mapIndexed { index, (day, sbp, hr) ->
            val period = TimePeriod.Day(LocalDate.of(2026, 3, day))
            StatRow(
                id = "tpl_vs_r${index + 1}",
                periodOrCategoryLabel = period.formatDisplay(),
                values = mapOf(
                    dateId to StatValue.TimePeriodValue(period, period.formatIso()),
                    sbpId to StatValue.DecimalNumber(sbp),
                    hrId to StatValue.DecimalNumber(hr)
                )
            )
        }
        return StatDataset(
            id = "ds_tpl_vital_signs",
            name = "Vital Signs Over Time (Sample)",
            description = "Fictional sample data showing daily blood pressure and heart rate readings.",
            fields = fields,
            rows = rows,
            metadata = DatasetMetadata(sourceLabel = FICTIONAL_SOURCE_LABEL)
        )
    }

    private fun buildFluidBalance(): StatDataset {
        val dayId = "tpl_fb_date"
        val intakeId = "tpl_fb_intake"
        val outputId = "tpl_fb_output"
        val balanceId = "tpl_fb_balance"
        val fields = listOf(
            StatField(
                id = dayId,
                name = "Day",
                role = FieldRole.Time.Day,
                suggestedRole = FieldRole.Time.Day,
                isRoleConfirmed = true,
                isRequired = true
            ),
            StatField(
                id = intakeId,
                name = "Intake",
                unit = "mL",
                role = FieldRole.RateContinuous(scaleHint = "mL"),
                suggestedRole = FieldRole.RateContinuous(scaleHint = "mL"),
                isRoleConfirmed = true,
                minValue = 0.0
            ),
            StatField(
                id = outputId,
                name = "Output",
                unit = "mL",
                role = FieldRole.RateContinuous(scaleHint = "mL"),
                suggestedRole = FieldRole.RateContinuous(scaleHint = "mL"),
                isRoleConfirmed = true,
                minValue = 0.0
            ),
            StatField(
                id = balanceId,
                name = "Net Balance",
                unit = "mL",
                role = FieldRole.RateContinuous(allowNegativeValues = true, scaleHint = "mL"),
                suggestedRole = FieldRole.RateContinuous(allowNegativeValues = true, scaleHint = "mL"),
                isRoleConfirmed = true
            )
        )
        val rows = listOf(
            Triple(1, 2100.0, 1850.0),
            Triple(2, 1950.0, 2050.0),
            Triple(3, 2200.0, 2000.0),
            Triple(4, 1800.0, 1900.0)
        ).mapIndexed { index, (day, intake, output) ->
            val period = TimePeriod.Day(LocalDate.of(2026, 3, day))
            StatRow(
                id = "tpl_fb_r${index + 1}",
                periodOrCategoryLabel = period.formatDisplay(),
                values = mapOf(
                    dayId to StatValue.TimePeriodValue(period, period.formatIso()),
                    intakeId to StatValue.DecimalNumber(intake),
                    outputId to StatValue.DecimalNumber(output),
                    balanceId to StatValue.DecimalNumber(intake - output)
                )
            )
        }
        return StatDataset(
            id = "ds_tpl_fluid_balance",
            name = "Fluid Balance (Sample)",
            description = "Fictional sample data tracking daily fluid intake, output, and net balance.",
            fields = fields,
            rows = rows,
            metadata = DatasetMetadata(sourceLabel = FICTIONAL_SOURCE_LABEL)
        )
    }

    private fun buildAgeAndSexCounts(): StatDataset {
        val ageId = "tpl_age_group"
        val sexId = "tpl_sex_group"
        val countId = "tpl_age_count"
        val fields = listOf(
            StatField(
                id = ageId,
                name = "Age Group",
                role = FieldRole.Category,
                suggestedRole = FieldRole.Category,
                isRoleConfirmed = true,
                isRequired = true,
                allowedCategories = listOf("0–17", "18–39", "40–59", "60+")
            ),
            StatField(
                id = sexId,
                name = "Sex",
                role = FieldRole.Group,
                suggestedRole = FieldRole.Group,
                isRoleConfirmed = true,
                isRequired = true,
                allowedCategories = listOf("Female", "Male")
            ),
            StatField(
                id = countId,
                name = "Patient Count",
                unit = "patients",
                role = FieldRole.Measure,
                suggestedRole = FieldRole.Measure,
                isRoleConfirmed = true,
                minValue = 0.0,
                isRequired = true
            )
        )
        val rows = listOf(
            Triple("0–17", "Female", 16L),
            Triple("0–17", "Male", 19L),
            Triple("18–39", "Female", 34L),
            Triple("18–39", "Male", 28L),
            Triple("40–59", "Female", 41L),
            Triple("40–59", "Male", 37L),
            Triple("60+", "Female", 29L),
            Triple("60+", "Male", 24L)
        ).mapIndexed { index, (age, sex, count) ->
            StatRow(
                id = "tpl_age_r${index + 1}",
                periodOrCategoryLabel = "$age · $sex",
                values = mapOf(
                    ageId to StatValue.TextValue(age),
                    sexId to StatValue.TextValue(sex),
                    countId to StatValue.WholeNumber(count)
                )
            )
        }
        return StatDataset(
            id = "ds_tpl_age_sex",
            name = "Age and Sex Counts (Sample)",
            description = "Fictional sample data comparing patient counts by age group and sex.",
            fields = fields,
            rows = rows,
            metadata = DatasetMetadata(sourceLabel = FICTIONAL_SOURCE_LABEL)
        )
    }
}
