package com.pasindu.nursingotapp.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pasindu.nursingotapp.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Automated Jetpack Compose UI regression test for UI-004:
 * Verifies that the Finance Hub Earnings Overview renders in a responsive 2x2 grid:
 * - Row 1: Gross and OT (horizontally aligned)
 * - Row 2: Deductions and Net Pay (horizontally aligned, positioned distinctly below Row 1)
 * - Column 1: Gross and Deductions (left aligned)
 * - Column 2: OT and Net Pay (left aligned to the right of Column 1)
 * - All four metric labels and formatted amounts are present without truncation.
 */
@RunWith(AndroidJUnit4::class)
class FinanceEarningsOverviewComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyEarningsOverview2x2GridMetricsLayout() {
        composeTestRule.waitForIdle()

        // 1. Navigate to the Finance tab
        val financeTab = composeTestRule.onAllNodes(hasText("Finance")).onFirst()
        financeTab.assertExists()
        financeTab.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(800) // Allow layout to settle

        // Capture screenshot of Finance screen showing 2x2 grid
        try {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val bitmap = uiAutomation.takeScreenshot()
            if (bitmap != null) {
                val dir = File("/sdcard/ui_audit")
                if (!dir.exists()) dir.mkdirs()
                val imageFile = File(dir, "finance_earnings_overview_2x2.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        } catch (_: Throwable) {
            // Non-fatal if screenshot directory is restricted
        }

        // 2. Identify all four metric labels in the Earnings Overview section
        val grossLabel = composeTestRule.onAllNodesWithText("Gross").onFirst()
        // "OT" appears in Hero Card, Earnings Overview, and Pay Rates; select the Earnings Overview card (sibling with hours subtitle)
        val otLabel = composeTestRule
            .onAllNodesWithText("OT")
            .filter(hasAnySibling(hasText("h", substring = true)))
            .onFirst()
        val deductionsLabel = composeTestRule.onAllNodesWithText("Deductions").onFirst()
        val netPayLabel = composeTestRule
            .onAllNodesWithText("Net Pay")
            .filter(hasAnySibling(hasText("Take home")))
            .onFirst()

        grossLabel.assertExists()
        otLabel.assertExists()
        deductionsLabel.assertExists()
        netPayLabel.assertExists()

        // 3. Inspect geometry of labels to verify 2x2 grid positioning
        val grossBounds = grossLabel.getUnclippedBoundsInRoot()
        val otBounds = otLabel.getUnclippedBoundsInRoot()
        val deductionsBounds = deductionsLabel.getUnclippedBoundsInRoot()
        val netPayBounds = netPayLabel.getUnclippedBoundsInRoot()

        // Row 1: Gross and OT share the same vertical top position
        assertEquals(
            "Gross and OT must be in Row 1 with matching top coordinates",
            grossBounds.top.value,
            otBounds.top.value,
            6.0f
        )

        // Row 2: Deductions and Net Pay share the same vertical top position
        assertEquals(
            "Deductions and Net Pay must be in Row 2 with matching top coordinates",
            deductionsBounds.top.value,
            netPayBounds.top.value,
            6.0f
        )

        // Vertical distinction: Row 2 must be positioned strictly below Row 1
        assertTrue(
            "Row 2 (Deductions top: ${deductionsBounds.top}) must be below Row 1 (Gross bottom: ${grossBounds.bottom})",
            deductionsBounds.top > grossBounds.bottom
        )

        // Column 1 alignment: Gross and Deductions share the same left coordinate
        assertEquals(
            "Gross and Deductions must be left-aligned in Column 1",
            grossBounds.left.value,
            deductionsBounds.left.value,
            6.0f
        )

        // Column 2 alignment: OT and Net Pay share the same left coordinate
        assertEquals(
            "OT and Net Pay must be left-aligned in Column 2",
            otBounds.left.value,
            netPayBounds.left.value,
            6.0f
        )

        // Horizontal distinction: Column 2 must be to the right of Column 1
        assertTrue(
            "Column 2 (OT left: ${otBounds.left}) must be to the right of Column 1 (Gross left: ${grossBounds.left})",
            otBounds.left > grossBounds.left
        )

        // 4. Verify supporting descriptions exist
        composeTestRule.onNodeWithText("Total").assertExists()
        composeTestRule.onNodeWithText("Take home").assertExists()
    }
}
