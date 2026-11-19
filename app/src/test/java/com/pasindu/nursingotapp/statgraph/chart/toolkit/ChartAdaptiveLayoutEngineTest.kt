package com.pasindu.nursingotapp.statgraph.chart.toolkit

import com.pasindu.nursingotapp.statgraph.engine.catalogue.ChartCatalogue
import com.pasindu.nursingotapp.statgraph.export.ImageExportScale
import com.pasindu.nursingotapp.statgraph.export.PdfOrientation
import com.pasindu.nursingotapp.statgraph.orchestration.ChartToolkitSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartAdaptiveLayoutEngineTest {

    private val bujeteCategories = listOf(
        "Emergency Department" to 86.0,
        "General Medicine" to 72.0,
        "General Surgery" to 64.0,
        "Paediatrics" to 58.0,
        "Obstetrics and Gynaecology" to 53.0,
        "Orthopaedics" to 49.0,
        "Cardiology" to 46.0,
        "Oncology" to 43.0,
        "Neurology" to 40.0,
        "Intensive Care Unit" to 38.0,
        "Neonatal Intensive Care Unit" to 35.0,
        "Renal Dialysis Unit" to 33.0,
        "Respiratory Medicine" to 30.0,
        "ENT Department" to 28.0,
        "Ophthalmology" to 25.0,
        "Psychiatry" to 22.0,
        "Dermatology" to 20.0,
        "Physiotherapy and Rehabilitation" to 18.0,
        "Infectious Diseases" to 16.0,
        "Palliative Care" to 14.0,
    )

    private fun buildBarRenderData(
        pairs: List<Pair<String, Double?>>,
        chartKind: ChartKind = ChartKind.BAR,
        userExplicitlySelectedChart: Boolean = true,
    ): ChartRenderData {
        val labels = pairs.map { it.first }
        val points = pairs.mapIndexed { idx, (label, value) ->
            ChartDatum(
                domainIndex = idx,
                domainLabel = label,
                numericValue = value,
                formattedValue = value?.toInt()?.toString() ?: "Missing",
                unit = "patients",
            )
        }
        return ChartRenderData(
            title = "PatientAdmissions by Category",
            subtitle = "${chartKind.name} • Bujete • Unit: patients",
            chartKind = chartKind,
            domainAxisTitle = "Category",
            valueAxisTitle = "PatientAdmissions (patients)",
            valueUnit = "patients",
            domainLabels = labels,
            series = listOf(
                ChartDataSeries(
                    id = "s_admissions",
                    name = "PatientAdmissions",
                    unit = "patients",
                    colorIndex = 0,
                    points = points,
                ),
            ),
            userExplicitlySelectedChart = userExplicitlySelectedChart,
        )
    }

    private val fakeMeasurer = object : AxisTextMeasurer {
        override fun measure(text: String, fontSizePx: Float, fontWeight: Int): AxisTextMetrics =
            AxisTextMetrics(widthPx = text.length * (fontSizePx * 0.56f), heightPx = fontSizePx * 1.25f)
    }

    @Test
    fun `5 short categories use full horizontal labels without abbreviation key`() {
        val pairs = listOf(
            "Ward A" to 42.0,
            "Ward B" to 38.0,
            "Ward C" to 51.0,
            "Ward D" to 29.0,
            "Ward E" to 45.0,
        )
        val data = buildBarRenderData(pairs, ChartKind.BAR)
        val decision = ChartLayoutDecisionEngine.decideForScreen(
            renderData = data,
            viewportWidthPx = 955,
            baseHeightPx = 700,
            density = 2.75f,
        )
        assertEquals(955f, decision.targetCanvasWidthPx, 1f)
        assertTrue(decision.abbreviationKey.isEmpty())

        val slotW = 800f / 5f
        val centers = List(5) { idx -> 100f + (idx + 0.5f) * slotW }
        val plan = AxisLabelPlanner.plan(
            input = AxisLabelInput(
                labels = data.domainLabels,
                slotCentersPx = centers,
                axisStartPx = 100f,
                axisEndPx = 900f,
                fontSizePx = 28f,
                textMeasurer = fakeMeasurer,
                canvasLeftPx = 10f,
                canvasRightPx = 945f,
            ),
        )
        assertEquals(AxisLabelStrategy.FULL_HORIZONTAL, plan.strategy)
        assertEquals(5, plan.items.count { it.visible })
        assertTrue(plan.abbreviationKey.isEmpty())
    }

    @Test
    fun `Defect A regression - 20 Bujete vertical bar categories all fit on phone viewport with unambiguous abbreviation key`() {
        val data = buildBarRenderData(bujeteCategories, ChartKind.BAR)
        val decision = ChartLayoutDecisionEngine.decideForScreen(
            renderData = data,
            viewportWidthPx = 955,
            baseHeightPx = 700,
            density = 2.75f,
        )
        // Must fit all 20 bars within the 955px phone viewport instead of clipping after 6.5 bars
        assertEquals(955f, decision.targetCanvasWidthPx, 1f)
        assertEquals(20, decision.abbreviationKey.size)
        assertEquals(20, decision.abbreviationKey.map { it.shortCode }.distinct().size)

        val plotLeft = 110f
        val plotRight = 935f
        val slotW = (plotRight - plotLeft) / 20f
        val centers = List(20) { idx -> plotLeft + (idx + 0.5f) * slotW }
        val plan = AxisLabelPlanner.plan(
            input = AxisLabelInput(
                labels = data.domainLabels,
                slotCentersPx = centers,
                axisStartPx = plotLeft,
                axisEndPx = plotRight,
                fontSizePx = 30f,
                textMeasurer = fakeMeasurer,
                canvasLeftPx = 8f,
                canvasRightPx = 948f,
            ),
        )
        // Every single one of the 20 bars must have a visible label (never 1 visible and 19 hidden)
        assertEquals(20, plan.items.count { it.visible })
        assertEquals(20, plan.abbreviationKey.size)
        assertEquals("ED", plan.abbreviationKey.first().shortCode)
        assertEquals("Emergency Department", plan.abbreviationKey.first().fullCategory)
    }

    @Test
    fun `Defect B regression - 20 Bujete horizontal bar chart preserves viewport width and 0 to 100 numeric X scale`() {
        val data = buildBarRenderData(bujeteCategories, ChartKind.HORIZONTAL_BAR)
        val decision = ChartLayoutDecisionEngine.decideForScreen(
            renderData = data,
            viewportWidthPx = 955,
            baseHeightPx = 700,
            density = 2.75f,
        )
        // Defect B root cause was expanding HORIZONTAL_BAR width to 20 * 44dp = 3108px so only 0..20 ("0..2") showed.
        // Width MUST remain equal to viewportWidthPx (955px) while height expands vertically for all 20 rows.
        assertEquals(955f, decision.targetCanvasWidthPx, 1f)
        assertTrue(
            "Expected vertical expansion for 20 horizontal bars, got ${decision.targetCanvasHeightPx}",
            decision.targetCanvasHeightPx > 1200f,
        )

        val resolved = ChartGeometryResolver.resolve(
            outerRect = ChartRect(0f, 0f, 955f, decision.targetCanvasHeightPx),
            data = data,
            insets = ChartInsets(left = 360f, top = 30f, right = 40f, bottom = 80f),
        )
        val xScale = resolved.xNumericScale
        assertNotNull(xScale)
        assertEquals(0.0, xScale!!.domainMin, 1e-6)
        assertEquals(100.0, xScale.domainMax, 1e-6)
        assertTrue(xScale.ticks.contains(0.0))
        assertTrue(xScale.ticks.contains(100.0))

        val maxBarRightPx = xScale.toPixel(86.0)!!
        val minBarRightPx = xScale.toPixel(14.0)!!
        assertTrue(maxBarRightPx <= 915f && maxBarRightPx > 780f)
        assertTrue(minBarRightPx > 360f && minBarRightPx < 500f)
    }

    @Test
    fun `20 very long categories generate collision-free abbreviations and complete reference key`() {
        val longCategories = (1..20).map { idx ->
            "Regional Tertiary Subspecialty Clinical Unit of Medical Science Division $idx" to (120.0 - idx * 4.0)
        }
        val key = ChartAbbreviationResolver.buildAbbreviationKey(longCategories.map { it.first })
        assertEquals(20, key.size)
        assertEquals(20, key.map { it.shortCode }.distinct().size)
        key.forEachIndexed { index, entry ->
            assertEquals(longCategories[index].first, entry.fullCategory)
            assertTrue(entry.shortCode.isNotBlank())
        }
    }

    @Test
    fun `Defect C regression - A4 export planner auto-selects Landscape for 20 vertical bars and splits 48 and 100 categories across shared-scale panels`() {
        val data20Vertical = buildBarRenderData(bujeteCategories, ChartKind.BAR)
        val plan20Vertical = ChartExportLayoutPlanner.planPdfExport(
            data = data20Vertical,
            requestedOrientation = null,
            allowMultiPagePanels = true,
        )
        assertEquals(PdfOrientation.LANDSCAPE, plan20Vertical.orientation)
        assertEquals(1, plan20Vertical.panels.size)

        val data20Horizontal = buildBarRenderData(bujeteCategories, ChartKind.HORIZONTAL_BAR)
        val plan20Horizontal = ChartExportLayoutPlanner.planPdfExport(
            data = data20Horizontal,
            requestedOrientation = null,
            allowMultiPagePanels = true,
        )
        assertEquals(PdfOrientation.PORTRAIT, plan20Horizontal.orientation)
        assertEquals(1, plan20Horizontal.panels.size)

        // 48 categories
        val pairs48 = (1..48).map { idx -> "Hospital Clinical Unit $idx" to (200.0 - idx * 3.0) }
        val data48 = buildBarRenderData(pairs48, ChartKind.BAR)
        val plan48 = ChartExportLayoutPlanner.planPdfExport(
            data = data48,
            requestedOrientation = null,
            allowMultiPagePanels = true,
        )
        assertEquals(PdfOrientation.LANDSCAPE, plan48.orientation)
        assertEquals(2, plan48.panels.size)
        assertEquals(48, plan48.panels.sumOf { it.sliceData.domainLabels.size })
        // Shared numeric scale across panels
        val sharedMax48 = plan48.panels.first().sliceData.maxNumericOverride
        assertNotNull(sharedMax48)
        plan48.panels.forEach { panel ->
            assertEquals(0.0, panel.sliceData.minNumericOverride ?: -1.0, 1e-6)
            assertEquals(sharedMax48!!, panel.sliceData.maxNumericOverride ?: -1.0, 1e-6)
        }

        // 100 categories
        val pairs100 = (1..100).map { idx -> "Specialized Ward Category $idx" to (500.0 - idx * 4.0) }
        val data100 = buildBarRenderData(pairs100, ChartKind.BAR)
        val plan100 = ChartExportLayoutPlanner.planPdfExport(
            data = data100,
            requestedOrientation = null,
            allowMultiPagePanels = true,
        )
        assertEquals(PdfOrientation.LANDSCAPE, plan100.orientation)
        assertEquals(4, plan100.panels.size)
        assertEquals(100, plan100.panels.sumOf { it.sliceData.domainLabels.size })
    }

    @Test
    fun `unequal values, zero values, and missing values preserve statistical meaning without coercion`() {
        val pairs = listOf(
            "Acute Trauma" to 12500.0,
            "Outpatient Triage" to 0.0,
            "Unreported Unit" to null,
            "Rare Specialty" to 0.25,
            "Day Surgery" to 410.0,
        )
        val data = buildBarRenderData(pairs, ChartKind.BAR)
        val resolved = ChartGeometryResolver.resolve(
            outerRect = ChartRect(0f, 0f, 955f, 700f),
            data = data,
            insets = ChartInsets(left = 80f, top = 30f, right = 30f, bottom = 80f),
        )
        assertEquals(0.0, resolved.yScale.domainMin, 1e-6)
        assertTrue(resolved.yScale.domainMax >= 12500.0)

        val points = data.series.first().points
        assertEquals(0.0, points[1].numericValue!!, 1e-6)
        assertEquals(null, points[2].numericValue)
        assertEquals("Missing", points[2].formattedValue)
    }

    @Test
    fun `all 52 catalogue chart specifications and all 39 ChartKind values are covered by AdaptiveChartEngine`() {
        assertEquals(52, ChartCatalogue.allSpecifications.size)
        ChartCatalogue.allSpecifications.forEach { spec ->
            val kind = ChartToolkitSupport.chartKindFor(spec.id)
            assertNotNull("Expected ChartKind mapping for ${spec.id}", kind)
        }

        ChartKind.entries.forEach { kind ->
            val sampleData = buildBarRenderData(bujeteCategories, chartKind = kind)
            val family = ChartReadabilityAnalyzer.classifyFamily(kind)
            assertNotNull(family)
            val screenDecision = ChartLayoutDecisionEngine.decideForScreen(
                renderData = sampleData,
                viewportWidthPx = 955,
                baseHeightPx = 700,
                density = 2.75f,
            )
            assertTrue(screenDecision.targetCanvasWidthPx >= 955f)
            assertTrue(screenDecision.targetCanvasHeightPx >= 400f)

            val pdfPlan = ChartExportLayoutPlanner.planPdfExport(sampleData)
            assertTrue(pdfPlan.panels.isNotEmpty())
            assertEquals(20, pdfPlan.panels.sumOf { it.sliceData.domainLabels.size })

            val (imgW, imgH) = ChartExportLayoutPlanner.planImageExportDimensions(
                data = sampleData,
                scale = ImageExportScale.HIGH_2X,
            )
            assertTrue(imgW >= ImageExportScale.HIGH_2X.widthPx)
            assertTrue(imgH >= ImageExportScale.HIGH_2X.heightPx)
        }
    }
}
