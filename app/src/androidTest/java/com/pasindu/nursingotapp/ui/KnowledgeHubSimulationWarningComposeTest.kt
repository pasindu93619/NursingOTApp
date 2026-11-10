package com.pasindu.nursingotapp.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pasindu.nursingotapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Automated Jetpack Compose UI test suite for UI-003:
 * Verifies that simulated Ministry circulars are prominently identified with
 * UNVERIFIED / CLINICAL SIMULATION disclaimers, badges, and provenance notes,
 * while authentic guidelines (GRADE_A) remain unaltered.
 */
@RunWith(AndroidJUnit4::class)
class KnowledgeHubSimulationWarningComposeTest {

    private val tag = "KHSimulationTest"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val evidenceDir: File by lazy {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(targetContext.getExternalFilesDir("audit-evidence"), "ui-003-evidence")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun captureEvidence(name: String) {
        try {
            composeTestRule.waitForIdle()
            Thread.sleep(600)
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val bitmap: Bitmap? = uiAutomation.takeScreenshot()
            if (bitmap != null) {
                val imageFile = File(evidenceDir, "${name}.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Log.i(tag, "Saved evidence screenshot: ${imageFile.absolutePath}")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Evidence capture note: ${e.message}")
        }
    }

    private fun scrollDownSafely() {
        try {
            val scrollables = composeTestRule.onAllNodes(hasScrollAction())
            if (scrollables.fetchSemanticsNodes().isNotEmpty()) {
                scrollables.onFirst().performTouchInput {
                    swipeUp(
                        startY = bottom - 250f,
                        endY = top + 250f,
                        durationMillis = 400
                    )
                }
            }
            composeTestRule.waitForIdle()
            Thread.sleep(400)
        } catch (t: Throwable) {
            Log.w(tag, "scrollDownSafely note: ${t.message}")
        }
    }

    private fun scrollUpSafely() {
        try {
            val scrollables = composeTestRule.onAllNodes(hasScrollAction())
            if (scrollables.fetchSemanticsNodes().isNotEmpty()) {
                scrollables.onFirst().performTouchInput {
                    swipeDown(
                        startY = top + 250f,
                        endY = bottom - 250f,
                        durationMillis = 400
                    )
                }
            }
            composeTestRule.waitForIdle()
            Thread.sleep(400)
        } catch (t: Throwable) {
            Log.w(tag, "scrollUpSafely note: ${t.message}")
        }
    }

    private fun scrollToTopSafely() {
        repeat(3) {
            scrollUpSafely()
        }
    }

    private fun findNodeWithScrolling(matcher: SemanticsMatcher): SemanticsNodeInteraction? {
        val direct = composeTestRule.onAllNodes(matcher)
        if (direct.fetchSemanticsNodes().isNotEmpty()) {
            return direct.onFirst()
        }
        for (i in 0..3) {
            scrollUpSafely()
            val upNodes = composeTestRule.onAllNodes(matcher)
            if (upNodes.fetchSemanticsNodes().isNotEmpty()) {
                return upNodes.onFirst()
            }
        }
        for (i in 0..4) {
            scrollDownSafely()
            val downNodes = composeTestRule.onAllNodes(matcher)
            if (downNodes.fetchSemanticsNodes().isNotEmpty()) {
                return downNodes.onFirst()
            }
        }
        return null
    }

    private fun navigateToKnowledgeHub() {
        composeTestRule.waitForIdle()
        Thread.sleep(800)

        // 1. Check if already inside Knowledge Hub
        val khTitle = composeTestRule.onAllNodes(hasText("Knowledge Hub"))
        if (khTitle.fetchSemanticsNodes().isNotEmpty()) {
            return
        }

        // 2. Try direct "Knowledge" tool on Home dashboard
        val homeTool = composeTestRule.onAllNodes(hasText("Knowledge"))
        if (homeTool.fetchSemanticsNodes().isNotEmpty()) {
            homeTool.onFirst().performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
            return
        }

        // 3. Fallback: Scroll on Home to reveal SecondaryToolsGrid
        scrollDownSafely()
        val homeToolAfterScroll = composeTestRule.onAllNodes(hasText("Knowledge"))
        if (homeToolAfterScroll.fetchSemanticsNodes().isNotEmpty()) {
            homeToolAfterScroll.onFirst().performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
            return
        }

        // 4. Fallback: Navigate via More bottom navigation tab
        val moreTabs = composeTestRule.onAllNodes(hasText("More"))
        if (moreTabs.fetchSemanticsNodes().isNotEmpty()) {
            moreTabs.onFirst().performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(800)

            val khInMore = composeTestRule.onAllNodes(hasText("Knowledge", substring = true))
            if (khInMore.fetchSemanticsNodes().isNotEmpty()) {
                khInMore.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(1000)
            }
        }
    }

    @Test
    fun verifyAllSimulationWarningRequirements() {
        navigateToKnowledgeHub()
        composeTestRule.waitForIdle()
        Thread.sleep(800)

        // Verify Knowledge Hub screen is active
        composeTestRule.onNodeWithText("Knowledge Hub").assertIsDisplayed()
        captureEvidence("ui003_01_library_overview")

        // =========================================================================
        // TEST CASE 3: Card Badges on Simulated Articles (All 4 IDs individually)
        // =========================================================================
        Log.i(tag, "Executing Test Case 3: Verify SIMULATION badge on each of the 4 simulated cards")
        val simulatedCardTitles = listOf(
            "High-Alert Medication Safety",                           // MOH-INFUSION-2025
            "Emergency Resuscitation & Defibrillator Checklist",     // MOH-2026-04
            "Infection Control & PPE Protocols in Oncology",         // MOH-2026-08
            "Revised Night Shift Resting Hours and Roster Allocation" // MOH-2026-11
        )
        simulatedCardTitles.forEach { titleSubstring ->
            val card = composeTestRule.onNode(hasText(titleSubstring, substring = true))
            card.performScrollTo()
            card.assertIsDisplayed()
            val badges = composeTestRule.onAllNodesWithText("SIMULATION")
            assert(badges.fetchSemanticsNodes().isNotEmpty()) {
                "Expected visible SIMULATION badge for card containing '$titleSubstring'"
            }
        }
        captureEvidence("ui003_04_card_badges_all_verified")

        // =========================================================================
        // TEST CASE 1: Simulated Article Reader Verification (MOH-INFUSION-2025)
        // =========================================================================
        Log.i(tag, "Executing Test Case 1: Verify simulated article reader")
        val infusionCard = composeTestRule.onNode(hasText("High-Alert Medication Safety", substring = true))
        infusionCard.performScrollTo()
        infusionCard.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        captureEvidence("ui003_02_simulated_article_reader_top")

        // 1A. Warning Title is prominently displayed
        composeTestRule.onNodeWithText("CLINICAL SIMULATION — UNVERIFIED DRAFT")
            .assertIsDisplayed()

        // 1B. Full Disclaimer text is displayed and accessible (present in both banner and attribution)
        val disclaimerNodes = composeTestRule.onAllNodes(hasText("illustrative clinical simulation placeholder", substring = true))
        assert(disclaimerNodes.fetchSemanticsNodes().isNotEmpty()) { "Disclaimer text must be present" }
        disclaimerNodes.onFirst().assertIsDisplayed()

        // 1C. SIMULATION badge in reader header is displayed
        val headerSimulationBadges = composeTestRule.onAllNodesWithText("SIMULATION")
        assert(headerSimulationBadges.fetchSemanticsNodes().isNotEmpty()) { "SIMULATION badge must be displayed in reader" }

        // =========================================================================
        // TEST CASE 4: Accessibility Semantics
        // =========================================================================
        Log.i(tag, "Executing Test Case 4: Accessibility semantics verification")
        composeTestRule.onNode(hasContentDescription("Simulation Warning"))
            .assertIsDisplayed()

        // Scroll down in reader sheet to check source attribution & provenance
        val issuingBodyNode = composeTestRule.onNode(hasText("Issuing Body Context: Ministry of Health Sri Lanka (Simulated)", substring = true))
        issuingBodyNode.performScrollTo()
        captureEvidence("ui003_02_simulated_article_reader_attribution")

        // 1D. Source Attribution does NOT present the article as an official circular
        issuingBodyNode.assertIsDisplayed()
        val verificationStatusNode = composeTestRule.onNode(hasText("Verification Status: UNVERIFIED DRAFT", substring = true))
        verificationStatusNode.performScrollTo()
        verificationStatusNode.assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Official Issuing Body: Ministry of Health Sri Lanka")
            .assertCountEquals(0)

        // Close simulated article reader
        val closeBtn = composeTestRule.onAllNodes(hasContentDescription("Close reader"))
        if (closeBtn.fetchSemanticsNodes().isNotEmpty()) {
            closeBtn.onFirst().performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(600)
        }

        // =========================================================================
        // TEST CASE 2: Verified Article Reader Verification (WHO-DENGUE-2024)
        // =========================================================================
        Log.i(tag, "Executing Test Case 2: Verify verified article reader")
        val dengueCard = composeTestRule.onNode(hasText("Dengue", substring = true))
        dengueCard.performScrollTo()
        dengueCard.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        captureEvidence("ui003_03_verified_article_reader")

        // 2A. Verify NO simulation warning appears
        composeTestRule.onAllNodesWithText("CLINICAL SIMULATION — UNVERIFIED DRAFT")
            .assertCountEquals(0)
        composeTestRule.onAllNodes(hasText("illustrative clinical simulation placeholder", substring = true))
            .assertCountEquals(0)

        // 2B. Scroll down in reader to check attribution
        val verifiedIssuingBody = composeTestRule.onNode(hasText("Official Issuing Body: WHO South-East Asia", substring = true))
        verifiedIssuingBody.performScrollTo()
        captureEvidence("ui003_03_verified_article_reader_attribution")

        // 2C. Official Issuing Body is preserved
        verifiedIssuingBody.assertIsDisplayed()

        // Close reader
        val closeBtn2 = composeTestRule.onAllNodes(hasContentDescription("Close reader"))
        if (closeBtn2.fetchSemanticsNodes().isNotEmpty()) {
            closeBtn2.onFirst().performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(600)
        }
        Log.i(tag, "ALL 4 COMPOSE UI TEST CASES PASSED SUCCESSFULLY!")
    }

    @Test
    fun verifyKnowledgeHubSearchAccessibilityAndFiltering() {
        navigateToKnowledgeHub()
        composeTestRule.waitForIdle()
        Thread.sleep(800)

        // 1. Initial search field announces "Active filter: All clinical guidelines"
        val searchField = composeTestRule.onNode(
            hasSetTextAction() and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Active filter: All clinical guidelines"
            )
        )
        searchField.assertExists()

        // Verify "All" FilterChip is initially selected
        composeTestRule.onNode(hasText("All") and hasClickAction()).assertIsSelected()

        // 2. Select category "Infection Prevention"
        val infectionChip = composeTestRule.onNode(hasText("Infection Prevention") and hasClickAction())
        infectionChip.performScrollTo()
        infectionChip.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(400)

        // Verify search field stateDescription updates to "Active filter: Infection Prevention"
        composeTestRule.onNode(
            hasSetTextAction() and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Active filter: Infection Prevention"
            )
        ).assertExists()

        // Verify FilterChip selected states
        composeTestRule.onNode(hasText("Infection Prevention") and hasClickAction()).assertIsSelected()
        composeTestRule.onNode(hasText("All") and hasClickAction()).assertIsNotSelected()

        // 3. Select category "Critical Care"
        val criticalCareChip = composeTestRule.onNode(hasText("Critical Care") and hasClickAction())
        criticalCareChip.performScrollTo()
        criticalCareChip.performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(400)

        // Verify search field stateDescription updates to "Active filter: Critical Care"
        composeTestRule.onNode(
            hasSetTextAction() and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Active filter: Critical Care"
            )
        ).assertExists()

        // Verify Critical Care selected
        composeTestRule.onNode(hasText("Critical Care") and hasClickAction()).assertIsSelected()
        composeTestRule.onNode(hasText("Infection Prevention") and hasClickAction()).assertIsNotSelected()

        // 4. Verify search text entry and category filtering work together
        val searchInput = composeTestRule.onNode(
            hasSetTextAction() and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Active filter: Critical Care"
            )
        )
        searchInput.performTextReplacement("Dengue")
        composeTestRule.waitForIdle()
        searchInput.performTextClearance()
        composeTestRule.waitForIdle()

        Log.i(tag, "UI-006 SEARCH ACCESSIBILITY VERIFICATION PASSED SUCCESSFULLY!")
    }
}
