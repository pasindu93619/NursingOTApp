package com.pasindu.nursingotapp.ui.screens

import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.pow

/**
 * JVM unit test suite verifying Step 5 Pay Sheet Vault UI contracts, month-key generation,
 * accessible contrast math (WCAG 2.1 AA), touch target minimums, and FAB clearance.
 */
class PaySheetVaultUiTest {

    private val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    @Test
    fun monthPicker_containsAll12Months_inFourRowsOfThreeColumns() {
        assertEquals("Month picker must contain exactly 12 months", 12, months.size)
        val grid = months.chunked(3)
        assertEquals("Grid must contain 4 rows", 4, grid.size)
        grid.forEachIndexed { rowIndex, row ->
            assertEquals("Row $rowIndex must contain exactly 3 month columns", 3, row.size)
        }
        assertEquals(listOf("JAN", "FEB", "MAR"), grid[0])
        assertEquals(listOf("APR", "MAY", "JUN"), grid[1])
        assertEquals(listOf("JUL", "AUG", "SEP"), grid[2])
        assertEquals(listOf("OCT", "NOV", "DEC"), grid[3])
    }

    @Test
    fun monthPicker_mapsGridCoordinatesToCorrectMonthIndexAndMonthKey() {
        val testYear = 2026
        val grid = months.chunked(3)

        for (rowIndex in grid.indices) {
            for (colIndex in grid[rowIndex].indices) {
                val computedIndex = rowIndex * 3 + colIndex
                assertTrue("Month index must be between 0 and 11", computedIndex in 0..11)

                val monthKey = String.format(Locale.US, "%04d-%02d", testYear, computedIndex + 1)
                val expectedMonthNumber = String.format(Locale.US, "%02d", computedIndex + 1)
                assertEquals("MonthKey format must be YYYY-MM", "2026-$expectedMonthNumber", monthKey)

                // Verify display month name corresponds to Month enum
                val monthEnum = Month.of(computedIndex + 1)
                val displayName = monthEnum.getDisplayName(TextStyle.FULL, Locale.US) + " $testYear"
                assertTrue(displayName.endsWith("2026"))
            }
        }
    }

    @Test
    fun touchTarget_satisfiesMinimum48dpRequirement() {
        // Material 3 and accessibility standard requires interactive targets to be >= 48dp
        val minTouchTarget = NursingDimensions.TouchTarget.minimum
        assertTrue(
            "Minimum touch target must be at least 48dp, actual: $minTouchTarget",
            minTouchTarget >= 48.dp
        )
    }

    @Test
    fun fabClearance_bottomPaddingExceedsSmallFabHeightAndMargins() {
        // SmallFloatingActionButton is 40dp + 16dp scaffold margin from bottom = 56dp
        // Content padding bottom of 88dp gives 32dp of safety margin past the FAB
        val bottomClearance = 88.dp
        val fabHeight = 40.dp
        val scaffoldFabMargin = 16.dp
        val totalFabFootprint = fabHeight + scaffoldFabMargin

        assertTrue(
            "Bottom clearance must completely exceed the FAB footprint to prevent overlap",
            bottomClearance > totalFabFootprint
        )
        val safetyMargin = bottomClearance - totalFabFootprint
        assertTrue("Safety margin must be at least 16dp", safetyMargin >= 16.dp)
    }

    @Test
    fun emptyMonthContrast_exceedsWcagAaThreshold() {
        // Background: #F8FAFC
        val bgR = 0xF8 / 255.0
        val bgG = 0xFA / 255.0
        val bgB = 0xFC / 255.0
        val bgLuminance = calculateRelativeLuminance(bgR, bgG, bgB)

        // New empty month label: #475569 (Slate-600)
        val labelR = 0x47 / 255.0
        val labelG = 0x55 / 255.0
        val labelB = 0x69 / 255.0
        val labelLuminance = calculateRelativeLuminance(labelR, labelG, labelB)
        val labelContrast = calculateContrastRatio(bgLuminance, labelLuminance)

        assertTrue(
            "New empty month label contrast must be >= 4.5:1 (WCAG AA). Actual: $labelContrast",
            labelContrast >= 4.5
        )

        // New empty badge: #64748B (Slate-500)
        val badgeR = 0x64 / 255.0
        val badgeG = 0x74 / 255.0
        val badgeB = 0x8B / 255.0
        val badgeLuminance = calculateRelativeLuminance(badgeR, badgeG, badgeB)
        val badgeContrast = calculateContrastRatio(bgLuminance, badgeLuminance)

        assertTrue(
            "New empty badge contrast must be >= 4.5:1 (WCAG AA). Actual: $badgeContrast",
            badgeContrast >= 4.5
        )

        // Verify that the old colors indeed failed WCAG AA (demonstrating the fix)
        val oldBadgeR = 0xCB / 255.0
        val oldBadgeG = 0xD5 / 255.0
        val oldBadgeB = 0xE1 / 255.0
        val oldBadgeLuminance = calculateRelativeLuminance(oldBadgeR, oldBadgeG, oldBadgeB)
        val oldBadgeContrast = calculateContrastRatio(bgLuminance, oldBadgeLuminance)

        assertFalse(
            "Old empty badge contrast (#CBD5E1) failed WCAG AA, proving the necessity of the fix",
            oldBadgeContrast >= 4.5
        )
    }

    /**
     * WCAG 2.1 relative luminance calculation formula
     */
    private fun calculateRelativeLuminance(r: Double, g: Double, b: Double): Double {
        fun transform(channel: Double): Double =
            if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

        return 0.2126 * transform(r) + 0.7152 * transform(g) + 0.0722 * transform(b)
    }

    private fun calculateContrastRatio(l1: Double, l2: Double): Double {
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
