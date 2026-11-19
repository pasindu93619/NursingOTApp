package com.pasindu.nursingotapp.statgraph

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartRenderData
import com.pasindu.nursingotapp.statgraph.chart.toolkit.ChartStyle
import com.pasindu.nursingotapp.statgraph.chart.toolkit.OkabeItoPalette
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartHost
import com.pasindu.nursingotapp.statgraph.chart.toolkit.StatGraphChartPalette
import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartCatalogue
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.orchestration.ChartPainterResolver
import com.pasindu.nursingotapp.statgraph.orchestration.ChartRenderState
import com.pasindu.nursingotapp.statgraph.orchestration.PainterSupportResult
import com.pasindu.nursingotapp.statgraph.orchestration.StatGraphOrchestrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class StatGraphAll52ChartsDeviceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun testStyle(): ChartStyle = ChartStyle(
        cardBackground = Color.White,
        cardBorder = Color(0xFFD0D7DE),
        plotBackground = Color.White,
        gridLineColor = Color(0xFFE5E7EB),
        zeroAxisColor = Color(0xFF4B5563),
        crosshairColor = Color(0xFF2563EB),
        truncationIndicatorColor = Color(0xFF6B7280),
        titleColor = Color(0xFF111827),
        subtitleColor = Color(0xFF4B5563),
        axisLabelColor = Color(0xFF374151),
        tooltipBackground = Color(0xFFF9FAFB),
        tooltipBorder = Color(0xFFD1D5DB),
        tooltipTextColor = Color(0xFF111827),
        errorColor = Color(0xFFB91C1C),
        seriesColors = OkabeItoPalette.seriesColors,
        titleStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        subtitleStyle = TextStyle(fontSize = 12.sp),
        axisLabelStyle = TextStyle(fontSize = 11.sp),
        tooltipValueStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        legendLabelStyle = TextStyle(fontSize = 11.sp),
        palette = StatGraphChartPalette.OKABE_ITO,
    )

    @Test
    fun renderAll52CatalogueChartsOnPhysicalDevice() {
        val allSpecs = ChartCatalogue.allSpecifications
        assertEquals(52, allSpecs.size)

        val currentRenderData = mutableStateOf<ChartRenderData?>(null)
        val style = testStyle()

        composeRule.setContent {
            val data = currentRenderData.value
            if (data != null) {
                StatGraphChartHost(
                    data = data,
                    style = style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )
            }
        }

        val failures = mutableListOf<String>()
        var passed = 0

        for (spec in allSpecs) {
            try {
                val support = ChartPainterResolver.resolveSupport(spec)
                if (support !is PainterSupportResult.Supported) {
                    failures.add("${spec.id}: PainterSupportResult.Unsupported")
                    continue
                }

                val (dataset, selection) = buildDatasetAndSelectionFor(spec.id)
                val state = StatGraphOrchestrator.orchestrate(dataset, selection)
                if (state !is ChartRenderState.Ready) {
                    failures.add("${spec.id}: Orchestrator returned ${state::class.simpleName}")
                    continue
                }
                if (!state.renderData.hasDrawablePoints) {
                    failures.add("${spec.id}: renderData.hasDrawablePoints is false")
                    continue
                }

                composeRule.runOnUiThread {
                    currentRenderData.value = state.renderData
                }
                composeRule.waitForIdle()
                passed++
                Log.i("StatGraph52DeviceTest", "PASS [$passed/52]: ${spec.id} (${state.renderData.chartKind})")
            } catch (t: Throwable) {
                failures.add("${spec.id}: ${t::class.simpleName}: ${t.message}")
                Log.e("StatGraph52DeviceTest", "FAIL: ${spec.id}", t)
            }
        }

        Log.i("StatGraph52DeviceTest", "SUMMARY: passed=$passed/52, failed=${failures.size}")
        assertTrue(
            "Passed $passed/52. Failures:\n${failures.joinToString("\n")}",
            failures.isEmpty() && passed == 52,
        )
    }

    private fun buildDatasetAndSelectionFor(specId: String): Pair<StatDataset, ChartMappingSelection> {
        val selection = ChartMappingSelection(chartSpecId = specId)
        return when (specId) {
            // Time-series single & multi-series
            "line_chart", "area_chart", "step_chart", "control_chart" -> buildMonthlySeriesDataset(multiSeries = false) to selection
            "multi_series_line_chart", "stacked_area_chart", "sparkline_small_multiples", "slope_chart", "bump_chart" ->
                buildMonthlySeriesDataset(multiSeries = true) to selection
            "range_band_chart" -> buildRangeBandDataset() to selection
            "cycle_plot" -> buildCyclePlotDataset() to selection
            "calendar_heatmap" -> buildCalendarDataset() to selection

            // Categorical comparisons & rankings
            "bar_chart", "horizontal_bar_chart", "lollipop_chart", "dot_plot", "pareto_chart",
            "pie_chart", "donut_chart", "waffle_chart", "waterfall_chart" -> buildCategoryMeasureDataset() to selection

            "grouped_bar_chart", "stacked_bar_chart",
            "radar_chart", "mosaic_plot", "population_pyramid" -> buildCategoryGroupMeasureDataset() to selection
            "stacked_bar_100_chart" -> buildPartToWholeCategoryDataset() to selection

            "treemap", "sunburst_chart" -> buildHierarchyDataset() to selection
            "funnel_chart" -> buildFunnelDataset() to selection
            "bullet_chart" -> buildBulletDataset() to selection
            "dumbbell_chart" -> buildDumbbellDataset() to selection
            "diverging_bar_chart" -> buildDivergingDataset() to selection
            "likert_stacked_diverging_chart" -> buildLikertDataset() to selection

            // Distributions & continuous
            "histogram", "density_plot", "ecdf_chart", "qq_plot", "stem_and_leaf_plot" ->
                buildDistributionDataset(withGroups = false) to selection
            "box_plot", "violin_plot", "strip_plot" ->
                buildDistributionDataset(withGroups = true) to selection

            // Bivariate & multivariate numeric
            "scatter_plot", "connected_scatter_plot", "hexbin_plot" ->
                buildTwoNumericDataset() to ChartMappingSelection(
                    chartSpecId = specId,
                    xFieldId = "f_x",
                    yFieldIds = listOf("f_y"),
                )
            "bubble_chart" ->
                buildThreeNumericDataset() to ChartMappingSelection(
                    chartSpecId = specId,
                    xFieldId = "f_x",
                    yFieldIds = listOf("f_y", "f_z"),
                )
            "scatterplot_matrix", "correlation_heatmap" ->
                buildThreeNumericDataset() to ChartMappingSelection(
                    chartSpecId = specId,
                    yFieldIds = listOf("f_x", "f_y", "f_z"),
                )

            // Uncertainty & Survival
            "error_bar_chart", "range_plot", "forest_plot" -> buildEstimateWithLimitsDataset() to selection
            "kaplan_meier_curve" -> buildSurvivalDataset() to selection

            else -> buildCategoryMeasureDataset() to selection
        }
    }

    private fun buildMonthlySeriesDataset(multiSeries: Boolean): StatDataset {
        val monthField = StatField("f_month", "Month", role = FieldRole.Time.Month)
        val m1 = StatField("f_m1", "Medical Ward", unit = "cases", role = FieldRole.Measure)
        val m2 = StatField("f_m2", "Surgical Ward", unit = "cases", role = FieldRole.Measure)
        val fields = if (multiSeries) listOf(monthField, m1, m2) else listOf(monthField, m1)
        val rows = (1..8).map { m ->
            val tp = TimePeriod.Month(YearMonth.of(2026, m))
            val vals = mutableMapOf<String, StatValue>(
                "f_month" to StatValue.TimePeriodValue(tp, tp.formatIso()),
                "f_m1" to StatValue.DecimalNumber(20.0 + m * 2.5),
            )
            if (multiSeries) vals["f_m2"] = StatValue.DecimalNumber(15.0 + ((m * 3) % 9))
            StatRow("m_$m", tp.formatDisplay(), vals)
        }
        return StatDataset("ds_monthly", "Monthly Admissions", fields = fields, rows = rows)
    }

    private fun buildRangeBandDataset(): StatDataset {
        val monthField = StatField("f_month", "Month", role = FieldRole.Time.Month)
        val estField = StatField("f_est", "Mean Wait", unit = "min", role = FieldRole.EstimateWithLimits())
        val rows = (1..6).map { m ->
            val tp = TimePeriod.Month(YearMonth.of(2026, m))
            val mid = 18.0 + m
            StatRow("rb_$m", tp.formatDisplay(), mapOf("f_month" to StatValue.TimePeriodValue(tp, tp.formatIso()), "f_est" to StatValue.EstimateValue(mid, mid - 4.0, mid + 5.0)))
        }
        return StatDataset("ds_rb", "Wait Time Band", fields = listOf(monthField, estField), rows = rows)
    }

    private fun buildCyclePlotDataset(): StatDataset {
        val monthField = StatField("f_month", "Month", role = FieldRole.Time.Month)
        val m1 = StatField("f_val", "Admissions", unit = "pts", role = FieldRole.Measure)
        val rows = mutableListOf<StatRow>()
        for (yr in listOf(2025, 2026)) {
            for (m in 1..4) {
                val tp = TimePeriod.Month(YearMonth.of(yr, m))
                rows.add(StatRow("c_${yr}_$m", tp.formatDisplay(), mapOf("f_month" to StatValue.TimePeriodValue(tp, tp.formatIso()), "f_val" to StatValue.DecimalNumber(25.0 + m * 3 + (yr - 2025) * 4))))
            }
        }
        return StatDataset("ds_cycle", "Seasonal Cycle", fields = listOf(monthField, m1), rows = rows)
    }

    private fun buildCalendarDataset(): StatDataset {
        val dayField = StatField("f_day", "Date", role = FieldRole.Time.Day)
        val m1 = StatField("f_val", "Visits", unit = "visits", role = FieldRole.Measure)
        val start = LocalDate.of(2026, 1, 1)
        val rows = (0 until 21).map { d ->
            val date = start.plusDays(d.toLong())
            val tp = TimePeriod.Day(date)
            StatRow("d_$d", tp.formatDisplay(), mapOf("f_day" to StatValue.TimePeriodValue(tp, tp.formatIso()), "f_val" to StatValue.WholeNumber((10 + (d % 7) * 3).toLong())))
        }
        return StatDataset("ds_cal", "Daily Visits", fields = listOf(dayField, m1), rows = rows)
    }

    private fun buildCategoryMeasureDataset(): StatDataset {
        val cat = StatField("f_cat", "Ward", role = FieldRole.Category)
        val valF = StatField("f_val", "Cases", unit = "cases", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("c1", "Medical", mapOf("f_cat" to StatValue.TextValue("Medical"), "f_val" to StatValue.WholeNumber(45))),
            StatRow("c2", "Surgical", mapOf("f_cat" to StatValue.TextValue("Surgical"), "f_val" to StatValue.WholeNumber(32))),
            StatRow("c3", "ICU", mapOf("f_cat" to StatValue.TextValue("ICU"), "f_val" to StatValue.WholeNumber(18))),
            StatRow("c4", "Pediatrics", mapOf("f_cat" to StatValue.TextValue("Pediatrics"), "f_val" to StatValue.WholeNumber(25))),
        )
        return StatDataset("ds_cat", "Ward Cases", fields = listOf(cat, valF), rows = rows)
    }

    private fun buildCategoryGroupMeasureDataset(): StatDataset {
        val cat = StatField("f_cat", "Shift", role = FieldRole.Category)
        val grp = StatField("f_grp", "Cohort", role = FieldRole.Group)
        val valF = StatField("f_val", "Staff", unit = "nurses", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("r1", "Morning", mapOf("f_cat" to StatValue.TextValue("Morning"), "f_grp" to StatValue.TextValue("RN"), "f_val" to StatValue.WholeNumber(24))),
            StatRow("r2", "Morning", mapOf("f_cat" to StatValue.TextValue("Morning"), "f_grp" to StatValue.TextValue("NO"), "f_val" to StatValue.WholeNumber(18))),
            StatRow("r3", "Evening", mapOf("f_cat" to StatValue.TextValue("Evening"), "f_grp" to StatValue.TextValue("RN"), "f_val" to StatValue.WholeNumber(20))),
            StatRow("r4", "Evening", mapOf("f_cat" to StatValue.TextValue("Evening"), "f_grp" to StatValue.TextValue("NO"), "f_val" to StatValue.WholeNumber(16))),
            StatRow("r5", "Night", mapOf("f_cat" to StatValue.TextValue("Night"), "f_grp" to StatValue.TextValue("RN"), "f_val" to StatValue.WholeNumber(14))),
            StatRow("r6", "Night", mapOf("f_cat" to StatValue.TextValue("Night"), "f_grp" to StatValue.TextValue("NO"), "f_val" to StatValue.WholeNumber(12))),
        )
        return StatDataset("ds_cat_grp", "Shift Cohort", fields = listOf(cat, grp, valF), rows = rows)
    }

    private fun buildPartToWholeCategoryDataset(): StatDataset {
        val cat = StatField("f_cat", "Shift", role = FieldRole.Category)
        val grp = StatField("f_grp", "Cohort", role = FieldRole.Group)
        val total = StatField("f_total", "Total Staff", unit = "nurses", role = FieldRole.Measure)
        val part = StatField("f_part", "Cohort Staff", unit = "nurses", role = FieldRole.PartOfMeasure(parentFieldId = "f_total"))
        val rows = listOf(
            StatRow("r1", "Morning", mapOf("f_cat" to StatValue.TextValue("Morning"), "f_grp" to StatValue.TextValue("RN"), "f_part" to StatValue.WholeNumber(24), "f_total" to StatValue.WholeNumber(42))),
            StatRow("r2", "Morning", mapOf("f_cat" to StatValue.TextValue("Morning"), "f_grp" to StatValue.TextValue("NO"), "f_part" to StatValue.WholeNumber(18), "f_total" to StatValue.WholeNumber(42))),
            StatRow("r3", "Evening", mapOf("f_cat" to StatValue.TextValue("Evening"), "f_grp" to StatValue.TextValue("RN"), "f_part" to StatValue.WholeNumber(20), "f_total" to StatValue.WholeNumber(36))),
            StatRow("r4", "Evening", mapOf("f_cat" to StatValue.TextValue("Evening"), "f_grp" to StatValue.TextValue("NO"), "f_part" to StatValue.WholeNumber(16), "f_total" to StatValue.WholeNumber(36))),
            StatRow("r5", "Night", mapOf("f_cat" to StatValue.TextValue("Night"), "f_grp" to StatValue.TextValue("RN"), "f_part" to StatValue.WholeNumber(14), "f_total" to StatValue.WholeNumber(26))),
            StatRow("r6", "Night", mapOf("f_cat" to StatValue.TextValue("Night"), "f_grp" to StatValue.TextValue("NO"), "f_part" to StatValue.WholeNumber(12), "f_total" to StatValue.WholeNumber(26))),
        )
        return StatDataset("ds_p2w", "Shift Composition", fields = listOf(cat, grp, part, total), rows = rows)
    }

    private fun buildHierarchyDataset(): StatDataset {
        val cat = StatField("f_cat", "Diagnosis", role = FieldRole.Category)
        val grp = StatField("f_grp", "Department", role = FieldRole.Group)
        val valF = StatField("f_val", "Cases", unit = "cases", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("h1", "Dengue", mapOf("f_cat" to StatValue.TextValue("Dengue"), "f_grp" to StatValue.TextValue("Medical"), "f_val" to StatValue.WholeNumber(42))),
            StatRow("h2", "Asthma", mapOf("f_cat" to StatValue.TextValue("Asthma"), "f_grp" to StatValue.TextValue("Medical"), "f_val" to StatValue.WholeNumber(28))),
            StatRow("h3", "Appendicitis", mapOf("f_cat" to StatValue.TextValue("Appendicitis"), "f_grp" to StatValue.TextValue("Surgical"), "f_val" to StatValue.WholeNumber(19))),
            StatRow("h4", "Trauma", mapOf("f_cat" to StatValue.TextValue("Trauma"), "f_grp" to StatValue.TextValue("Surgical"), "f_val" to StatValue.WholeNumber(25))),
        )
        return StatDataset("ds_hier", "Department Hierarchy", fields = listOf(cat, grp, valF), rows = rows)
    }

    private fun buildFunnelDataset(): StatDataset {
        val stage = StatField("f_stage", "Stage", role = FieldRole.Category)
        val cnt = StatField("f_cnt", "Patients", unit = "pts", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("f1", "Screened", mapOf("f_stage" to StatValue.TextValue("Screened"), "f_cnt" to StatValue.WholeNumber(120))),
            StatRow("f2", "Eligible", mapOf("f_stage" to StatValue.TextValue("Eligible"), "f_cnt" to StatValue.WholeNumber(78))),
            StatRow("f3", "Enrolled", mapOf("f_stage" to StatValue.TextValue("Enrolled"), "f_cnt" to StatValue.WholeNumber(45))),
            StatRow("f4", "Completed", mapOf("f_stage" to StatValue.TextValue("Completed"), "f_cnt" to StatValue.WholeNumber(32))),
        )
        return StatDataset("ds_funnel", "Screening Cascade", fields = listOf(stage, cnt), rows = rows)
    }

    private fun buildBulletDataset(): StatDataset {
        val cat = StatField("f_cat", "Metric", role = FieldRole.Category)
        val actual = StatField("f_act", "Actual", unit = "%", role = FieldRole.Measure)
        val target = StatField("f_tgt", "Target", unit = "%", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("b1", "Hand Hygiene", mapOf("f_cat" to StatValue.TextValue("Hand Hygiene"), "f_act" to StatValue.DecimalNumber(88.0), "f_tgt" to StatValue.DecimalNumber(90.0))),
            StatRow("b2", "Medication Audit", mapOf("f_cat" to StatValue.TextValue("Medication Audit"), "f_act" to StatValue.DecimalNumber(94.0), "f_tgt" to StatValue.DecimalNumber(92.0))),
        )
        return StatDataset("ds_bullet", "Quality Targets", fields = listOf(cat, actual, target), rows = rows)
    }

    private fun buildDumbbellDataset(): StatDataset {
        val cat = StatField("f_cat", "Ward", role = FieldRole.Category)
        val pre = StatField("f_pre", "Pre-Intervention", unit = "min", role = FieldRole.Measure)
        val post = StatField("f_post", "Post-Intervention", unit = "min", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("db1", "Ward A", mapOf("f_cat" to StatValue.TextValue("Ward A"), "f_pre" to StatValue.DecimalNumber(42.0), "f_post" to StatValue.DecimalNumber(26.0))),
            StatRow("db2", "Ward B", mapOf("f_cat" to StatValue.TextValue("Ward B"), "f_pre" to StatValue.DecimalNumber(38.0), "f_post" to StatValue.DecimalNumber(22.0))),
            StatRow("db3", "Ward C", mapOf("f_cat" to StatValue.TextValue("Ward C"), "f_pre" to StatValue.DecimalNumber(50.0), "f_post" to StatValue.DecimalNumber(31.0))),
        )
        return StatDataset("ds_db", "Wait Reduction", fields = listOf(cat, pre, post), rows = rows)
    }

    private fun buildDivergingDataset(): StatDataset {
        val cat = StatField("f_cat", "Unit", role = FieldRole.Category)
        val delta = StatField("f_delta", "Variance", unit = "hrs", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("dv1", "Medical", mapOf("f_cat" to StatValue.TextValue("Medical"), "f_delta" to StatValue.DecimalNumber(12.5))),
            StatRow("dv2", "Surgical", mapOf("f_cat" to StatValue.TextValue("Surgical"), "f_delta" to StatValue.DecimalNumber(-8.0))),
            StatRow("dv3", "ICU", mapOf("f_cat" to StatValue.TextValue("ICU"), "f_delta" to StatValue.DecimalNumber(5.0))),
            StatRow("dv4", "OPD", mapOf("f_cat" to StatValue.TextValue("OPD"), "f_delta" to StatValue.DecimalNumber(-4.5))),
        )
        return StatDataset("ds_div", "OT Variance", fields = listOf(cat, delta), rows = rows)
    }

    private fun buildLikertDataset(): StatDataset {
        val item = StatField("f_item", "Survey Item", role = FieldRole.Category)
        val sd = StatField("f_sd", "Strongly Disagree", unit = "%", role = FieldRole.Measure)
        val d = StatField("f_d", "Disagree", unit = "%", role = FieldRole.Measure)
        val n = StatField("f_n", "Neutral", unit = "%", role = FieldRole.Measure)
        val a = StatField("f_a", "Agree", unit = "%", role = FieldRole.Measure)
        val sa = StatField("f_sa", "Strongly Agree", unit = "%", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("l1", "Staffing Ratio", mapOf("f_item" to StatValue.TextValue("Staffing Ratio"), "f_sd" to StatValue.DecimalNumber(10.0), "f_d" to StatValue.DecimalNumber(15.0), "f_n" to StatValue.DecimalNumber(20.0), "f_a" to StatValue.DecimalNumber(35.0), "f_sa" to StatValue.DecimalNumber(20.0))),
            StatRow("l2", "Handover Clarity", mapOf("f_item" to StatValue.TextValue("Handover Clarity"), "f_sd" to StatValue.DecimalNumber(5.0), "f_d" to StatValue.DecimalNumber(10.0), "f_n" to StatValue.DecimalNumber(15.0), "f_a" to StatValue.DecimalNumber(45.0), "f_sa" to StatValue.DecimalNumber(25.0))),
        )
        return StatDataset("ds_likert", "Staff Survey", fields = listOf(item, sd, d, n, a, sa), rows = rows)
    }

    private fun buildDistributionDataset(withGroups: Boolean): StatDataset {
        val grp = StatField("f_grp", "Ward", role = FieldRole.Group)
        val los = StatField("f_los", "Length of Stay", unit = "days", role = FieldRole.Measure)
        val fields = if (withGroups) listOf(grp, los) else listOf(los)
        val valuesA = listOf(2.0, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 7.0, 8.0, 9.5, 12.0)
        val valuesB = listOf(3.0, 4.0, 5.0, 6.0, 6.5, 7.0, 8.0, 9.0, 10.0, 11.0, 13.0)
        val rows = mutableListOf<StatRow>()
        valuesA.forEachIndexed { idx, v ->
            val m = mutableMapOf<String, StatValue>("f_los" to StatValue.DecimalNumber(v))
            if (withGroups) m["f_grp"] = StatValue.TextValue("Medical")
            rows.add(StatRow("a_$idx", "Med $idx", m))
        }
        if (withGroups) {
            valuesB.forEachIndexed { idx, v ->
                rows.add(StatRow("b_$idx", "Surg $idx", mapOf("f_grp" to StatValue.TextValue("Surgical"), "f_los" to StatValue.DecimalNumber(v))))
            }
        }
        return StatDataset("ds_dist", "LOS Distribution", fields = fields, rows = rows)
    }

    private fun buildTwoNumericDataset(): StatDataset {
        val fx = StatField("f_x", "Patient Load", unit = "pts", role = FieldRole.Measure)
        val fy = StatField("f_y", "Overtime Hours", unit = "hrs", role = FieldRole.Measure)
        val rows = (1..10).map { i ->
            StatRow("p_$i", "W$i", mapOf("f_x" to StatValue.DecimalNumber(15.0 + i * 2.0), "f_y" to StatValue.DecimalNumber(4.0 + i * 1.3)))
        }
        return StatDataset("ds_xy", "Load vs OT", fields = listOf(fx, fy), rows = rows)
    }

    private fun buildThreeNumericDataset(): StatDataset {
        val fx = StatField("f_x", "Age", unit = "yrs", role = FieldRole.Measure)
        val fy = StatField("f_y", "LOS", unit = "days", role = FieldRole.Measure)
        val fz = StatField("f_z", "Acuity Score", unit = "pts", role = FieldRole.Measure)
        val rows = (1..8).map { i ->
            StatRow(
                "t_$i",
                "Pt $i",
                mapOf(
                    "f_x" to StatValue.DecimalNumber(22.0 + i * 5.0),
                    "f_y" to StatValue.DecimalNumber(2.0 + i * 1.1),
                    "f_z" to StatValue.DecimalNumber(10.0 + i * 3.0),
                ),
            )
        }
        return StatDataset("ds_xyz", "Multivariate Cohort", fields = listOf(fx, fy, fz), rows = rows)
    }

    private fun buildEstimateWithLimitsDataset(): StatDataset {
        val study = StatField("f_study", "Unit", role = FieldRole.Category)
        val est = StatField("f_est", "Odds Ratio", unit = "OR", role = FieldRole.EstimateWithLimits())
        val rows = listOf(
            StatRow("e1", "Ward A", mapOf("f_study" to StatValue.TextValue("Ward A"), "f_est" to StatValue.EstimateValue(1.42, 1.08, 1.88))),
            StatRow("e2", "Ward B", mapOf("f_study" to StatValue.TextValue("Ward B"), "f_est" to StatValue.EstimateValue(0.85, 0.62, 1.16))),
            StatRow("e3", "Ward C", mapOf("f_study" to StatValue.TextValue("Ward C"), "f_est" to StatValue.EstimateValue(1.18, 0.94, 1.49))),
        )
        return StatDataset("ds_est", "Unit Estimates", fields = listOf(study, est), rows = rows)
    }

    private fun buildSurvivalDataset(): StatDataset {
        val dur = StatField("f_dur", "Follow-up Days", unit = "days", role = FieldRole.DurationWithEvent())
        val rows = listOf(
            StatRow("s1", "P1", mapOf("f_dur" to StatValue.DurationEventValue(4.0, true))),
            StatRow("s2", "P2", mapOf("f_dur" to StatValue.DurationEventValue(7.0, false))),
            StatRow("s3", "P3", mapOf("f_dur" to StatValue.DurationEventValue(10.0, true))),
            StatRow("s4", "P4", mapOf("f_dur" to StatValue.DurationEventValue(14.0, true))),
            StatRow("s5", "P5", mapOf("f_dur" to StatValue.DurationEventValue(18.0, false))),
        )
        return StatDataset("ds_surv", "Recovery Time", fields = listOf(dur), rows = rows)
    }
}
