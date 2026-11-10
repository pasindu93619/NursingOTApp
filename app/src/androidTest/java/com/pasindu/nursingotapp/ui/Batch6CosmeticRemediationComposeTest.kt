package com.pasindu.nursingotapp.ui

import android.accessibilityservice.AccessibilityService
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileOutputStream

/**
 * Physical-device & UI Automator verification suite for Batch 6 (NEW-UI-01 and NEW-UI-02)
 * running against the isolated verification sandbox (`com.pasindu.nursingotapp.verification`)
 * on the Redmi Note 14 5G (1080 x 2400 px, 450 dpi, 384dp viewport width).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Batch6CosmeticRemediationComposeTest {

    private val tag = "Batch6Verify"

    private val uiAuto: UiAutomation
        get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val outDir: File by lazy {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(ctx.getExternalFilesDir("audit-evidence"), "batch-6-2026-11-10")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private fun saveScreenshot(name: String) {
        try {
            Thread.sleep(900)
            val bmp = uiAuto.takeScreenshot()
            if (bmp != null) {
                val f = File(outDir, "${name}.png")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Log.i(tag, "Saved screenshot: ${f.absolutePath}")
            }
        } catch (t: Throwable) {
            Log.e(tag, "Screenshot failed for $name: ${t.message}")
        }
    }

    private fun getAllWindowRoots(): List<AccessibilityNodeInfo> {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (win in uiAuto.windows) {
                win.root?.let { roots.add(it) }
            }
        } catch (_: Throwable) {}
        uiAuto.rootInActiveWindow?.let { active ->
            if (roots.none { it == active }) roots.add(active)
        }
        return roots
    }

    private fun dumpTreeNodes(label: String) {
        val sb = StringBuilder("=== DUMP: $label ===\n")
        for (root in getAllWindowRoots()) {
            dumpRecursive(root, 0, sb)
        }
        Log.i(tag, sb.toString())
        try {
            File(outDir, "${label}_nodes.txt").writeText(sb.toString())
        } catch (_: Throwable) {}
    }

    private fun dumpRecursive(node: AccessibilityNodeInfo?, depth: Int, sb: StringBuilder) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val r = Rect().also { node.getBoundsInScreen(it) }
        val t = node.text?.toString()?.replace("\n", "\\n") ?: ""
        val d = node.contentDescription?.toString()?.replace("\n", "\\n") ?: ""
        val actions = node.actionList.map { it.id }
        if (t.isNotEmpty() || d.isNotEmpty() || node.isScrollable || node.isCheckable) {
            sb.append("$indent[text='$t' desc='$d' bounds=$r scrollable=${node.isScrollable} actions=$actions]\n")
        }
        for (i in 0 until node.childCount) {
            dumpRecursive(node.getChild(i), depth + 1, sb)
        }
    }

    private fun findNodeAcrossWindows(
        textSub: String? = null,
        descSub: String? = null,
        exactText: Boolean = false
    ): AccessibilityNodeInfo? {
        for (root in getAllWindowRoots()) {
            val matched = findNodeRecursive(root, textSub, descSub, exactText)
            if (matched != null) return matched
        }
        return null
    }

    private fun findNodeRecursive(
        node: AccessibilityNodeInfo?,
        textSub: String?,
        descSub: String?,
        exactText: Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        val t = node.text?.toString() ?: ""
        val d = node.contentDescription?.toString() ?: ""
        val textMatch = when {
            textSub == null -> true
            exactText -> t.trim().equals(textSub.trim(), ignoreCase = true)
            else -> t.contains(textSub, ignoreCase = true)
        }
        val descMatch = when {
            descSub == null -> true
            else -> d.contains(descSub, ignoreCase = true)
        }
        if ((textSub != null || descSub != null) && textMatch && descMatch) return node
        for (i in 0 until node.childCount) {
            val found = findNodeRecursive(node.getChild(i), textSub, descSub, exactText)
            if (found != null) return found
        }
        return null
    }

    private fun findAndClick(
        textSub: String? = null,
        descSub: String? = null,
        exactText: Boolean = false,
        maxAttempts: Int = 8
    ): Boolean {
        for (attempt in 0 until maxAttempts) {
            val matched = findNodeAcrossWindows(textSub, descSub, exactText)
            if (matched != null) {
                var clickable: AccessibilityNodeInfo? = matched
                while (clickable != null && !clickable.isClickable) {
                    clickable = clickable.parent
                }
                val target = clickable ?: matched
                val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(1000)
                return ok
            }
            Thread.sleep(450)
        }
        return false
    }

    private fun scrollDownVertically(): Boolean {
        var scrolled = false
        for (root in getAllWindowRoots()) {
            val scrollables = mutableListOf<AccessibilityNodeInfo>()
            collectScrollables(root, scrollables)
            for (s in scrollables) {
                val r = Rect().also { s.getBoundsInScreen(it) }
                if (r.height() < 600) continue
                val hasScrollDown = s.actionList.any { it.id == android.R.id.accessibilityActionScrollDown }
                if (hasScrollDown) {
                    if (s.performAction(android.R.id.accessibilityActionScrollDown)) {
                        Thread.sleep(850)
                        scrolled = true
                    }
                } else if (s.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                    Thread.sleep(850)
                    scrolled = true
                }
            }
        }
        return scrolled
    }

    private fun scrollUpVertically(): Boolean {
        var scrolled = false
        for (root in getAllWindowRoots()) {
            val scrollables = mutableListOf<AccessibilityNodeInfo>()
            collectScrollables(root, scrollables)
            for (s in scrollables) {
                val r = Rect().also { s.getBoundsInScreen(it) }
                if (r.height() < 600) continue
                val hasScrollUp = s.actionList.any { it.id == android.R.id.accessibilityActionScrollUp }
                if (hasScrollUp) {
                    if (s.performAction(android.R.id.accessibilityActionScrollUp)) {
                        Thread.sleep(850)
                        scrolled = true
                    }
                } else if (s.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                    Thread.sleep(850)
                    scrolled = true
                }
            }
        }
        return scrolled
    }

    /**
     * Scrolls DailyEntryScreen to the bottom (maxScroll) and then one page up (maxScroll - viewportHeight),
     * positioning the entire ExportCenterCard (title, subtitle, status pill, 4 tiles, and CTA button)
     * squarely in the middle of the 290..1780px viewport.
     */
    private fun bringExportCenterIntoView() {
        repeat(3) { scrollDownVertically() }
        scrollUpVertically()
        Thread.sleep(600)
        // If still not showing both title and button, adjust by one scroll step
        if (findNodeAcrossWindows(textSub = "Export Center", exactText = true) == null) {
            scrollUpVertically()
            Thread.sleep(600)
        }
    }

    private fun collectScrollables(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isScrollable || node.actionList.any { it.id == android.R.id.accessibilityActionScrollDown }) {
            out.add(node)
        }
        for (i in 0 until node.childCount) {
            collectScrollables(node.getChild(i), out)
        }
    }

    private fun launchVerificationApp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = Intent().apply {
            setClassName(
                "com.pasindu.nursingotapp.verification",
                "com.pasindu.nursingotapp.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        Thread.sleep(2800)
    }

    @Test
    fun test01_newUi01_verifyExportCenterIncompleteAndReadyStatesOnDevice() {
        launchVerificationApp()

        // Navigate to Work -> Open duty calendar (seeded with 4/28 days -> Incomplete state)
        assertTrue("Must click Work tab", findAndClick(textSub = "Work", exactText = true))
        Thread.sleep(1000)
        if (!findAndClick(textSub = "Open duty calendar", maxAttempts = 3)) {
            scrollDownVertically()
            assertTrue("Must click Open duty calendar", findAndClick(textSub = "Open duty calendar", maxAttempts = 5))
        }
        Thread.sleep(2200)

        // Scroll down one step so subtitle, status pill, 4 readiness tiles, and CTA button are in full view
        scrollDownVertically()
        Thread.sleep(1200)
        if (findNodeAcrossWindows(textSub = "Pre-export verification & PDF form generation") == null) {
            scrollDownVertically()
            Thread.sleep(1200)
        }
        saveScreenshot("BATCH6_NEW-UI-01_export_center_incomplete")
        dumpTreeNodes("01_export_center_incomplete")

        val subtitleNode = findNodeAcrossWindows(textSub = "Pre-export verification & PDF form generation")
        val incompleteNode = findNodeAcrossWindows(textSub = "Incomplete", exactText = true)

        assertNotNull("Export Center subtitle must be visible", subtitleNode)
        assertNotNull("Incomplete status pill must be visible", incompleteNode)

        val subtitleRect = Rect().also { subtitleNode!!.getBoundsInScreen(it) }
        val incompleteRect = Rect().also { incompleteNode!!.getBoundsInScreen(it) }

        Log.i(tag, "NEW-UI-01 Incomplete bounds: pill=$incompleteRect, subtitle=$subtitleRect")

        // Single-line 9.sp text at 450dpi is ~34-68px tall; wrapped 2-line text was >78px tall
        assertTrue(
            "Incomplete status pill must stay on one line (heightPx=${incompleteRect.height()}, widthPx=${incompleteRect.width()})",
            incompleteRect.height() <= 72 && incompleteRect.width() >= 110
        )
        assertTrue(
            "Incomplete status pill left (${incompleteRect.left}) must not overlap subtitle right (${subtitleRect.right})",
            incompleteRect.left >= subtitleRect.right
        )

        // Verify clicking Review & Export PDF Form triggers Incomplete Calendar confirmation dialog
        assertTrue(
            "Must click Review & Export PDF Form",
            findAndClick(textSub = "Review & Export PDF Form", maxAttempts = 3)
        )
        Thread.sleep(900)
        saveScreenshot("BATCH6_NEW-UI-01_incomplete_validation_dialog")
        if (!findAndClick(textSub = "Keep Editing", maxAttempts = 3) &&
            !findAndClick(textSub = "Cancel", maxAttempts = 2)
        ) {
            uiAuto.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Thread.sleep(800)
        }

        // Scroll back to top and use Duty Presets & Auto-Fill to populate all 28/28 days -> "Ready to Export"
        scrollUpVertically()
        Thread.sleep(1200)
        if (findNodeAcrossWindows(textSub = "Duty Presets & Auto-Fill") == null) {
            scrollUpVertically()
            Thread.sleep(1000)
        }
        assertTrue(
            "Must click Duty Presets & Auto-Fill",
            findAndClick(textSub = "Duty Presets & Auto-Fill", maxAttempts = 4)
        )
        Thread.sleep(800)
        assertTrue("Must click Apply Preset", findAndClick(textSub = "Apply Preset", maxAttempts = 4))
        Thread.sleep(800)
        findAndClick(textSub = "Yes, Update Shifts", maxAttempts = 3)
        Thread.sleep(800)
        assertTrue(
            "Must click Auto-Fill & Save or Save selected days",
            findAndClick(textSub = "Auto-Fill & Save", maxAttempts = 4) ||
                findAndClick(textSub = "Save selected days", maxAttempts = 3)
        )
        Thread.sleep(3800)

        // Navigate Back to ClaimPeriodModernScreen and re-open the duty calendar so DailyEntryScreen
        // mounts fresh at scroll=0 with a clean AccessibilityNodeInfo tree and 28/28 saved entries.
        if (!findAndClick(descSub = "Back", maxAttempts = 3)) {
            uiAuto.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Thread.sleep(1200)
        }
        Thread.sleep(1000)
        if (!findAndClick(textSub = "Open duty calendar", maxAttempts = 3)) {
            scrollDownVertically()
            assertTrue("Must re-open duty calendar", findAndClick(textSub = "Open duty calendar", maxAttempts = 5))
        }
        Thread.sleep(2200)

        // Scroll down so ExportCenterCard ("Ready to Export") is in full view
        scrollDownVertically()
        Thread.sleep(1200)
        if (findNodeAcrossWindows(textSub = "Pre-export verification & PDF form generation") == null) {
            scrollDownVertically()
            Thread.sleep(1200)
        }
        saveScreenshot("BATCH6_NEW-UI-01_export_center_ready")
        dumpTreeNodes("02_export_center_ready")

        val readyNode = findNodeAcrossWindows(textSub = "Ready to Export", exactText = true)
        val readySubtitleNode = findNodeAcrossWindows(textSub = "Pre-export verification & PDF form generation")
        assertNotNull("Ready to Export status pill must be visible", readyNode)
        assertNotNull("Subtitle must be visible in Ready state", readySubtitleNode)

        val readyRect = Rect().also { readyNode!!.getBoundsInScreen(it) }
        val readySubRect = Rect().also { readySubtitleNode!!.getBoundsInScreen(it) }
        Log.i(tag, "NEW-UI-01 Ready bounds: pill=$readyRect, subtitle=$readySubRect")

        assertTrue(
            "Ready to Export pill must stay on one line (heightPx=${readyRect.height()}, widthPx=${readyRect.width()})",
            readyRect.height() <= 72 && readyRect.width() >= 140
        )
        assertTrue(
            "Ready to Export pill left (${readyRect.left}) must not overlap subtitle right (${readySubRect.right})",
            readyRect.left >= readySubRect.right
        )
    }

    @Test
    fun test02_newUi02_verifyClinicalTaskCardPriorityDndBypassAndDoneOnDevice() {
        launchVerificationApp()

        // Navigate to More -> Clinical Planning -> Shift Tasks
        assertTrue("Must click More tab", findAndClick(textSub = "More", exactText = true))
        Thread.sleep(800)
        if (!findAndClick(textSub = "Clinical Planning", maxAttempts = 4)) {
            scrollDownVertically()
            assertTrue("Must click Clinical Planning", findAndClick(textSub = "Clinical Planning", maxAttempts = 4))
        }
        Thread.sleep(1200)

        assertTrue("Must click Shift Tasks tab", findAndClick(textSub = "Shift Tasks", maxAttempts = 5))
        Thread.sleep(1200)
        if (findNodeAcrossWindows(textSub = "HIGH", exactText = true) == null) {
            findAndClick(textSub = "Shift Tasks", maxAttempts = 3)
            Thread.sleep(1200)
        }

        dumpTreeNodes("03_clinical_task_card")
        saveScreenshot("BATCH6_NEW-UI-02_clinical_task_priority_dnd_done")

        val highNode = findNodeAcrossWindows(textSub = "HIGH", exactText = true)
        val dndNode = findNodeAcrossWindows(textSub = "DND Bypass", exactText = true)
        val doneNode = findNodeAcrossWindows(textSub = "Done", exactText = true)
        val editNode = findNodeAcrossWindows(descSub = "Edit task")
        val deleteNode = findNodeAcrossWindows(descSub = "Delete task")

        assertNotNull("HIGH priority badge must be visible", highNode)
        assertNotNull("DND Bypass badge must be visible", dndNode)
        assertNotNull("Done badge must be visible", doneNode)
        assertNotNull("Edit button must be visible", editNode)
        assertNotNull("Delete button must be visible", deleteNode)

        val highRect = Rect().also { highNode!!.getBoundsInScreen(it) }
        val dndRect = Rect().also { dndNode!!.getBoundsInScreen(it) }
        val doneRect = Rect().also { doneNode!!.getBoundsInScreen(it) }
        val editRect = Rect().also { editNode!!.getBoundsInScreen(it) }

        Log.i(tag, "NEW-UI-02 Task badges bounds: HIGH=$highRect, DND=$dndRect, Done=$doneRect, EditBtn=$editRect")

        // Before the fix, Done was crushed into a vertical 4-line strip (width < 25px, height > 140px).
        // With FlowRow + maxLines=1/softWrap=false, Done renders horizontally on one line (width >= 55px, height <= 72px).
        assertTrue(
            "Done badge must render horizontally on one single line without vertical stacking (widthPx=${doneRect.width()}, heightPx=${doneRect.height()})",
            doneRect.width() >= 55 && doneRect.height() <= 72
        )
        assertTrue(
            "DND Bypass badge must render horizontally on one single line (widthPx=${dndRect.width()}, heightPx=${dndRect.height()})",
            dndRect.width() >= 120 && dndRect.height() <= 72
        )

        // Verify zero right-edge clipping into the trailing Edit/Delete buttons
        assertTrue(
            "HIGH badge right (${highRect.right}) must not clip into Edit button left (${editRect.left})",
            highRect.right <= editRect.left
        )
        assertTrue(
            "DND Bypass badge right (${dndRect.right}) must not clip into Edit button left (${editRect.left})",
            dndRect.right <= editRect.left
        )
        assertTrue(
            "Done badge right (${doneRect.right}) must not clip into Edit button left (${editRect.left})",
            doneRect.right <= editRect.left
        )

        // Verify Edit button remains interactive (opens Edit Task dialog, then cancel)
        assertTrue("Edit button must be clickable", findAndClick(descSub = "Edit task"))
        Thread.sleep(800)
        saveScreenshot("BATCH6_NEW-UI-02_edit_dialog_usable")
        if (!findAndClick(textSub = "Cancel", maxAttempts = 2)) {
            uiAuto.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Thread.sleep(800)
        }
    }
}
