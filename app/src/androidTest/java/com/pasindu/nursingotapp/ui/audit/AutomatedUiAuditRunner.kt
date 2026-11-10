package com.pasindu.nursingotapp.ui.audit

import android.content.pm.ActivityInfo
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
 * Non-destructive, isolated automated UI audit test harness.
 * Inspects all 5 root navigation destinations and key child screens using Jetpack Compose UI Testing.
 * Captures full-resolution native portrait screenshots and Compose semantics trees without modifying
 * any application data or triggering data-saving actions.
 */
@RunWith(AndroidJUnit4::class)
class AutomatedUiAuditRunner {

    private val tag = "UiAuditRunner"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val evidenceDir: File by lazy {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(targetContext.getExternalFilesDir("audit-evidence"), "redmi-ui-audit-2026-11-05")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun captureEvidence(screenName: String) {
        try {
            composeTestRule.waitForIdle()
            Thread.sleep(800) // Allow layout transitions to settle

            // 1. Capture screen bitmap
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val bitmap = uiAutomation.takeScreenshot()
            if (bitmap != null) {
                val imageFile = File(evidenceDir, "${screenName}.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Log.i(tag, "Saved screenshot: ${imageFile.absolutePath}")
            } else {
                Log.w(tag, "uiAutomation.takeScreenshot() returned null for $screenName")
            }

            // 2. Capture Compose semantics tree safely (handles single or multiple roots)
            try {
                val roots = composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes()
                val semanticsDump = roots.indices.joinToString("\n\n" + "=".repeat(40) + "\n\n") { idx ->
                    "=== ROOT NODE $idx ===\n" + composeTestRule.onAllNodes(isRoot())[idx].printToString()
                }
                val dumpFile = File(evidenceDir, "${screenName}_semantics.txt")
                dumpFile.writeText(semanticsDump)
                Log.i(tag, "Saved semantics: ${dumpFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(tag, "Could not capture semantics for $screenName: ${e.message}")
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed capturing evidence for $screenName", t)
        }
    }

    private fun scrollDownSafely() {
        try {
            composeTestRule.waitForIdle()
            val scrollables = composeTestRule.onAllNodes(hasScrollAction())
            if (scrollables.fetchSemanticsNodes().isNotEmpty()) {
                scrollables.onFirst().performTouchInput {
                    swipeUp(
                        startY = bottom - 300f,
                        endY = top + 300f,
                        durationMillis = 500
                    )
                }
            } else {
                val roots = composeTestRule.onAllNodes(isRoot())
                if (roots.fetchSemanticsNodes().isNotEmpty()) {
                    roots.onFirst().performTouchInput {
                        swipeUp(
                            startY = centerY + 450f,
                            endY = centerY - 450f,
                            durationMillis = 500
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()
            Thread.sleep(600)
        } catch (t: Throwable) {
            Log.w(tag, "scrollDownSafely note: ${t.message}")
        }
    }

    private fun navigateBackSafely() {
        try {
            composeTestRule.waitForIdle()
            val backButtons = composeTestRule.onAllNodes(
                hasContentDescription("Back") or
                hasContentDescription("Navigate back") or
                hasContentDescription("Close")
            )
            if (backButtons.fetchSemanticsNodes().isNotEmpty()) {
                backButtons.onFirst().performClick()
            } else {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
            composeTestRule.waitForIdle()
            Thread.sleep(700)
        } catch (t: Throwable) {
            Log.w(tag, "navigateBackSafely note: ${t.message}")
        }
    }

    @Test
    fun executeFullUiAudit() {
        Log.i(tag, "Starting Automated UI Audit on Redmi device. Evidence dir: ${evidenceDir.absolutePath}")

        // 0. Lock orientation to portrait so audit matches standard clinical handheld usage
        try {
            composeTestRule.activityRule.scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            composeTestRule.waitForIdle()
            Thread.sleep(600)
        } catch (t: Throwable) {
            Log.w(tag, "Orientation lock note: ${t.message}")
        }

        // ==========================================
        // 1. HOME SCREEN
        // ==========================================
        Log.i(tag, "--- Auditing Stage 1: HOME ---")
        try {
            composeTestRule.waitForIdle()
            captureEvidence("01_home_initial")

            // Inspect guide dialog before scrolling
            val guideNodes = composeTestRule.onAllNodes(hasContentDescription("Open NursingOS Guide"))
            if (guideNodes.fetchSemanticsNodes().isNotEmpty()) {
                guideNodes.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(600)
                captureEvidence("01_home_guide_dialog")

                val closeNodes = composeTestRule.onAllNodes(hasContentDescription("Close guide"))
                if (closeNodes.fetchSemanticsNodes().isNotEmpty()) {
                    closeNodes.onFirst().performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(500)
                }
            }

            scrollDownSafely()
            captureEvidence("01_home_scrolled")
        } catch (e: Throwable) {
            Log.e(tag, "Home screen audit exception: ${e.message}")
        }

        // ==========================================
        // 2. WORK SCREEN (Claim Period & OT)
        // ==========================================
        Log.i(tag, "--- Auditing Stage 2: WORK ---")
        try {
            val workTabs = composeTestRule.onAllNodes(hasText("Work"))
            if (workTabs.fetchSemanticsNodes().isNotEmpty()) {
                workTabs.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(800)
                captureEvidence("02_work_initial")

                scrollDownSafely()
                captureEvidence("02_work_scrolled")
            } else {
                Log.w(tag, "Work tab not found in semantics")
            }
        } catch (e: Throwable) {
            Log.e(tag, "Work tab audit exception: ${e.message}")
        }

        // ==========================================
        // 3. CLINICAL SCREEN & CALCULATORS
        // ==========================================
        Log.i(tag, "--- Auditing Stage 3: CLINICAL ---")
        try {
            val clinicalTabs = composeTestRule.onAllNodes(hasText("Clinical"))
            if (clinicalTabs.fetchSemanticsNodes().isNotEmpty()) {
                clinicalTabs.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(800)
                captureEvidence("03_clinical_initial")

                scrollDownSafely()
                captureEvidence("03_clinical_scrolled")

                // 3A. IV Drip Calculator
                try {
                    val ivNodes = composeTestRule.onAllNodes(hasText("IV Drip Rate", substring = true))
                    if (ivNodes.fetchSemanticsNodes().isNotEmpty()) {
                        ivNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("03_clinical_iv_drip")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "IV Drip calculator note: ${e.message}")
                }

                // 3B. ICU Calculator
                try {
                    val icuNodes = composeTestRule.onAllNodes(hasText("ICU & Critical Care", substring = true))
                    if (icuNodes.fetchSemanticsNodes().isNotEmpty()) {
                        icuNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("03_clinical_icu")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "ICU calculator note: ${e.message}")
                }

                // 3C. Pediatric Rules
                try {
                    val pediaNodes = composeTestRule.onAllNodes(hasText("Pediatric Rules", substring = true))
                    if (pediaNodes.fetchSemanticsNodes().isNotEmpty()) {
                        pediaNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("03_clinical_pediatric")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Pediatric rules note: ${e.message}")
                }
            } else {
                Log.w(tag, "Clinical tab not found in semantics")
            }
        } catch (e: Throwable) {
            Log.e(tag, "Clinical tab audit exception: ${e.message}")
        }

        // ==========================================
        // 4. FINANCE SCREEN
        // ==========================================
        Log.i(tag, "--- Auditing Stage 4: FINANCE ---")
        try {
            val financeTabs = composeTestRule.onAllNodes(hasText("Finance"))
            if (financeTabs.fetchSemanticsNodes().isNotEmpty()) {
                financeTabs.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(800)
                captureEvidence("04_finance_initial")

                scrollDownSafely()
                captureEvidence("04_finance_scrolled")
            } else {
                Log.w(tag, "Finance tab not found in semantics")
            }
        } catch (e: Throwable) {
            Log.e(tag, "Finance tab audit exception: ${e.message}")
        }

        // ==========================================
        // 5. MORE SCREEN & KNOWLEDGE HUB
        // ==========================================
        Log.i(tag, "--- Auditing Stage 5: MORE ---")
        try {
            val moreTabs = composeTestRule.onAllNodes(hasText("More"))
            if (moreTabs.fetchSemanticsNodes().isNotEmpty()) {
                moreTabs.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(800)
                captureEvidence("05_more_initial")

                // 5A. Knowledge & CPD
                try {
                    val khNodes = composeTestRule.onAllNodes(hasText("Knowledge & CPD", substring = true))
                    if (khNodes.fetchSemanticsNodes().isNotEmpty()) {
                        khNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(900)
                        captureEvidence("05_more_knowledge_hub")

                        scrollDownSafely()
                        captureEvidence("05_more_knowledge_hub_scrolled")

                        // Inspect article reader sheet (read-only)
                        try {
                            val articleNodes = composeTestRule.onAllNodes(hasText("Dengue", substring = true))
                            if (articleNodes.fetchSemanticsNodes().isNotEmpty()) {
                                articleNodes.onFirst().performClick()
                                composeTestRule.waitForIdle()
                                Thread.sleep(900)
                                captureEvidence("05_more_article_reader")

                                scrollDownSafely()
                                captureEvidence("05_more_article_reader_scrolled")

                                val closeReader = composeTestRule.onAllNodes(hasContentDescription("Close reader"))
                                if (closeReader.fetchSemanticsNodes().isNotEmpty()) {
                                    closeReader.onFirst().performClick()
                                    composeTestRule.waitForIdle()
                                    Thread.sleep(600)
                                }
                            }
                        } catch (ae: Throwable) {
                            Log.w(tag, "Article reader interaction note: ${ae.message}")
                        }

                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Knowledge Hub audit note: ${e.message}")
                }

                // Return to More screen and scroll down
                val moreTabs2 = composeTestRule.onAllNodes(hasText("More"))
                if (moreTabs2.fetchSemanticsNodes().isNotEmpty()) {
                    moreTabs2.onFirst().performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(600)
                }

                scrollDownSafely()
                captureEvidence("05_more_scrolled")

                // 5B. Pay Sheet Bank
                try {
                    val psbNodes = composeTestRule.onAllNodes(hasText("Pay Sheet Bank", substring = true))
                    if (psbNodes.fetchSemanticsNodes().isNotEmpty()) {
                        psbNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("05_more_pay_sheet_bank")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Pay Sheet Bank note: ${e.message}")
                }

                // 5C. Clinical Planning
                try {
                    val cpNodes = composeTestRule.onAllNodes(hasText("Clinical Planning", substring = true))
                    if (cpNodes.fetchSemanticsNodes().isNotEmpty()) {
                        cpNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("05_more_clinical_planning")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Clinical Planning note: ${e.message}")
                }

                // 5D. Nurse Command Center
                try {
                    val ccNodes = composeTestRule.onAllNodes(hasText("Nurse Command Center", substring = true))
                    if (ccNodes.fetchSemanticsNodes().isNotEmpty()) {
                        ccNodes.onFirst().performClick()
                        composeTestRule.waitForIdle()
                        Thread.sleep(800)
                        captureEvidence("05_more_command_center")
                        navigateBackSafely()
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Command center note: ${e.message}")
                }
            } else {
                Log.w(tag, "More tab not found in semantics")
            }
        } catch (e: Throwable) {
            Log.e(tag, "More tab audit exception: ${e.message}")
        }

        // ==========================================
        // 6. FINAL RETURN TO HOME
        // ==========================================
        Log.i(tag, "--- Final Stage: RETURN HOME ---")
        try {
            val homeTabs = composeTestRule.onAllNodes(hasText("Home"))
            if (homeTabs.fetchSemanticsNodes().isNotEmpty()) {
                homeTabs.onFirst().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(600)
                captureEvidence("06_final_return_home")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Final return home note: ${e.message}")
        }

        Log.i(tag, "Automated UI Audit successfully completed. All evidence stored in: ${evidenceDir.absolutePath}")
    }
}
