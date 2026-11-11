package com.pasindu.nursingotapp.ui

import com.pasindu.nursingotapp.ui.components.shouldStackTopBarActions
import com.pasindu.nursingotapp.ui.components.shouldUseCompactMonthGridText
import com.pasindu.nursingotapp.ui.components.shouldUseScrollableBottomNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveAccessibilityPolicyTest {

    @Test
    fun `bottom navigation uses balanced M3 NavigationBar at 1_0x and 1_3x and switches to scrollable row at 2_0x`() {
        assertFalse(shouldUseScrollableBottomNav(1.0f))
        assertFalse(shouldUseScrollableBottomNav(1.3f))
        assertTrue(shouldUseScrollableBottomNav(1.35f))
        assertTrue(shouldUseScrollableBottomNav(2.0f))
    }

    @Test
    fun `daily entry top bar keeps wide action pill inline at 1_0x and 1_3x and stacks at 2_0x`() {
        assertFalse(shouldStackTopBarActions(fontScale = 1.0f, stackWideActionsAtLargeFont = true))
        assertFalse(shouldStackTopBarActions(fontScale = 1.3f, stackWideActionsAtLargeFont = true))
        assertTrue(shouldStackTopBarActions(fontScale = 2.0f, stackWideActionsAtLargeFont = true))
        assertFalse(shouldStackTopBarActions(fontScale = 2.0f, stackWideActionsAtLargeFont = false))
    }

    @Test
    fun `month picker switches to compact single-line label typography at large font scale`() {
        assertFalse(shouldUseCompactMonthGridText(1.0f))
        assertFalse(shouldUseCompactMonthGridText(1.3f))
        assertTrue(shouldUseCompactMonthGridText(2.0f))

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val rows = months.chunked(3)
        assertEquals(4, rows.size)
        assertTrue(rows.all { it.size == 3 })
    }
}
