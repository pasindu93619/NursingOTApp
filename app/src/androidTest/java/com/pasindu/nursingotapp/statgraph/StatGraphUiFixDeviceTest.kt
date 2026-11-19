package com.pasindu.nursingotapp.statgraph

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import com.pasindu.nursingotapp.statgraph.engine.recommendation.RecommendationEngine
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.ui.components.StatGraphRecommendationSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class StatGraphUiFixDeviceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun buildTwoMeasureRelationshipDataset(): StatDataset {
        val xField = StatField("f_x", "Workload", unit = "hrs", role = FieldRole.Measure)
        val yField = StatField("f_y", "Overtime", unit = "hrs", role = FieldRole.Measure)
        val rows = listOf(
            StatRow("r1", "1", mapOf("f_x" to StatValue.DecimalNumber(10.0), "f_y" to StatValue.DecimalNumber(2.0))),
            StatRow("r2", "2", mapOf("f_x" to StatValue.DecimalNumber(14.0), "f_y" to StatValue.DecimalNumber(3.5))),
            StatRow("r3", "3", mapOf("f_x" to StatValue.DecimalNumber(18.0), "f_y" to StatValue.DecimalNumber(5.0))),
            StatRow("r4", "4", mapOf("f_x" to StatValue.DecimalNumber(22.0), "f_y" to StatValue.DecimalNumber(6.5))),
            StatRow("r5", "5", mapOf("f_x" to StatValue.DecimalNumber(26.0), "f_y" to StatValue.DecimalNumber(8.0))),
        )
        return StatDataset("ds_rel", "Workload vs Overtime", fields = listOf(xField, yField), rows = rows)
    }

    @Test
    fun selectorAndWhyNotSheetLayout_areCorrectInSimpleAndResearchModes() {
        val dataset = buildTwoMeasureRelationshipDataset()
        val suite = RecommendationEngine.recommend(dataset)
        var isResearchMode by mutableStateOf(false)
        var densityPx = 1f

        composeRule.setContent {
            densityPx = LocalDensity.current.density
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    StatGraphRecommendationSection(
                        suite = suite,
                        selectedSpecId = null,
                        activeSpecId = suite.recommendedCharts.firstOrNull()?.specification?.id,
                        showAllCatalogueCharts = false,
                        onSelectChartSpec = {},
                        onToggleShowAllCatalogueCharts = {},
                        dataset = dataset,
                        isResearchMode = isResearchMode,
                        onToggleResearchMode = { isResearchMode = it },
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Verify Simple / Research selector is on its own full-width row below the heading
        val headingNode = composeRule.onNodeWithTag("statgraph_best_chart_heading").fetchSemanticsNode()
        val selectorNode = composeRule.onNodeWithTag("statgraph_mode_selector").fetchSemanticsNode()
        val simpleNode = composeRule.onNodeWithTag("statgraph_mode_option_simple").fetchSemanticsNode()
        val researchNode = composeRule.onNodeWithTag("statgraph_mode_option_research").fetchSemanticsNode()

        val headingBounds = headingNode.boundsInRoot
        val selectorBounds = selectorNode.boundsInRoot
        val simpleBounds = simpleNode.boundsInRoot
        val researchBounds = researchNode.boundsInRoot

        assertTrue(
            "Selector must be placed below the heading row, not beside it (selectorTop=${selectorBounds.top}, headingBottom=${headingBounds.bottom})",
            selectorBounds.top >= headingBounds.bottom - 2f,
        )
        assertTrue(
            "Selector must be full-width inside the card (>= 280dp), was ${selectorBounds.width / densityPx}dp",
            selectorBounds.width / densityPx >= 280f,
        )
        assertTrue(
            "Simple and Research segments must have equal width (${simpleBounds.width} vs ${researchBounds.width})",
            abs(simpleBounds.width - researchBounds.width) <= 4f,
        )
        assertTrue(
            "Simple segment must have >= 48dp touch target height, was ${simpleBounds.height / densityPx}dp",
            simpleBounds.height / densityPx >= 47.5f,
        )
        assertTrue(
            "Research segment must have >= 48dp touch target height, was ${researchBounds.height / densityPx}dp",
            researchBounds.height / densityPx >= 47.5f,
        )

        // Verify initial Simple selection and toggle to Research & back
        composeRule.onNodeWithTag("statgraph_mode_option_simple").assertIsSelected()
        composeRule.onNodeWithTag("statgraph_mode_option_research").assertIsNotSelected()

        composeRule.onNodeWithTag("statgraph_mode_option_research").performClick()
        composeRule.waitForIdle()
        assertTrue(isResearchMode)
        composeRule.onNodeWithTag("statgraph_mode_option_research").assertIsSelected()
        composeRule.onNodeWithTag("statgraph_mode_option_simple").assertIsNotSelected()

        composeRule.onNodeWithTag("statgraph_mode_option_simple").performClick()
        composeRule.waitForIdle()
        assertEquals(false, isResearchMode)
        composeRule.onNodeWithTag("statgraph_mode_option_simple").assertIsSelected()
        saveScreenshot("statgraph_selector.png")

        // 2. Simple mode: open "Why not the other charts?" and verify Bar chart & Horizontal bar chart layout
        composeRule.onNodeWithTag("statgraph_why_not_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertWhyNotCardLayout(chartId = "bar_chart", minTitleWidthDp = 45f, densityPx = densityPx)
        assertWhyNotCardLayout(chartId = "horizontal_bar_chart", minTitleWidthDp = 90f, densityPx = densityPx)
        saveScreenshot("statgraph_whynot_simple.png")

        // Switch to Research mode and verify
        composeRule.runOnUiThread { isResearchMode = true }
        composeRule.waitForIdle()

        // 3. Research mode: verify Bar chart & Horizontal bar chart remain properly laid out
        assertWhyNotCardLayout(chartId = "bar_chart", minTitleWidthDp = 45f, densityPx = densityPx)
        assertWhyNotCardLayout(chartId = "horizontal_bar_chart", minTitleWidthDp = 90f, densityPx = densityPx)
        saveScreenshot("statgraph_whynot_research.png")
    }

    private fun saveScreenshot(fileName: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = instrumentation.targetContext.getExternalFilesDir(null) ?: return
        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun assertWhyNotCardLayout(
        chartId: String,
        minTitleWidthDp: Float,
        densityPx: Float,
    ) {
        val cardNode = composeRule.onNodeWithTag("statgraph_why_not_card_$chartId").fetchSemanticsNode()
        val titleNode = composeRule.onNodeWithTag("statgraph_why_not_title_$chartId").fetchSemanticsNode()
        val verdictNode = composeRule.onNodeWithTag("statgraph_why_not_verdict_$chartId").fetchSemanticsNode()

        val cardBounds = cardNode.boundsInRoot
        val titleBounds = titleNode.boundsInRoot
        val verdictBounds = verdictNode.boundsInRoot

        val cardWidthDp = cardBounds.width / densityPx
        val titleWidthDp = titleBounds.width / densityPx
        val titleHeightDp = titleBounds.height / densityPx

        assertTrue(
            "$chartId card width should use available width (>= 260dp), was ${cardWidthDp}dp",
            cardWidthDp >= 260f,
        )
        assertTrue(
            "$chartId title must be horizontal and not collapsed (width=${titleWidthDp}dp, height=${titleHeightDp}dp)",
            titleWidthDp >= minTitleWidthDp && titleBounds.width > titleBounds.height,
        )
        assertTrue(
            "$chartId title height should be <= 2 lines (<= 48dp), was ${titleHeightDp}dp",
            titleHeightDp <= 48f,
        )
        assertTrue(
            "$chartId verdict pill must have non-zero readable width, was ${verdictBounds.width / densityPx}dp",
            verdictBounds.width / densityPx >= 60f,
        )
    }
}
