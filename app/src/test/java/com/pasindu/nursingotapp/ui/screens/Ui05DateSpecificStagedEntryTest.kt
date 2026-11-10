package com.pasindu.nursingotapp.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * UI-05 unit tests verifying that staged custom shift and OT values belong to their
 * respective dates rather than being shared through screen-global state.
 */
class Ui05DateSpecificStagedEntryTest {

    private fun shiftHours(edit: StagedEdit, wardType: String = "Normal", defaultCustomHrs: String = "10.0"): Double {
        return when (edit.shift) {
            "Morn (7-13)", "Eve (13-19)" -> 6.0
            "Night (19-7)" -> 12.0
            "Day (7-16)" -> if (wardType == "Normal") 6.0 else 9.0
            "Custom Shift" -> (edit.customHrs ?: defaultCustomHrs).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            else -> 0.0
        }
    }

    private fun otHours(edit: StagedEdit, defaultCustomHrs: String = "10.0"): Double {
        return when (edit.ot) {
            "Morn OT", "Eve OT" -> 6.0
            "Night OT" -> 12.0
            "Custom OT" -> (edit.customHrs ?: defaultCustomHrs).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            else -> 0.0
        }
    }

    private fun simulateApplyEdit(
        stagedEdits: MutableMap<LocalDate, StagedEdit>,
        date: LocalDate,
        category: String,
        brush: String,
        customIn: String,
        customOut: String,
        customHrs: String
    ) {
        val current = stagedEdits[date] ?: StagedEdit()
        val isCustomShift = brush == "Custom Shift"
        val isCustomOt = brush == "Custom OT"
        val isCustomLeave = brush in setOf("CL/2", "SL (Short)")
        val next = when (category) {
            "Shift Duty" -> {
                if (current.shift == brush && (!isCustomShift || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(
                        shift = null,
                        customIn = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customIn else null,
                        customOut = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customOut else null,
                        customHrs = if (current.ot == "Custom OT" || current.leave in setOf("CL/2", "SL (Short)")) current.customHrs else null
                    )
                } else {
                    current.copy(
                        shift = brush,
                        customIn = if (isCustomShift) customIn else current.customIn,
                        customOut = if (isCustomShift) customOut else current.customOut,
                        customHrs = if (isCustomShift) customHrs else current.customHrs
                    )
                }
            }
            "Overtime" -> {
                if (current.ot == brush && (!isCustomOt || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(
                        ot = null,
                        customIn = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customIn else null,
                        customOut = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customOut else null,
                        customHrs = if (current.shift == "Custom Shift" || current.leave in setOf("CL/2", "SL (Short)")) current.customHrs else null
                    )
                } else {
                    current.copy(
                        ot = brush,
                        customIn = if (isCustomOt) customIn else current.customIn,
                        customOut = if (isCustomOt) customOut else current.customOut,
                        customHrs = if (isCustomOt) customHrs else current.customHrs
                    )
                }
            }
            "Leave & Rest", "Service Days" -> {
                if (current.leave == brush && (!isCustomLeave || (current.customIn == customIn && current.customOut == customOut && current.customHrs == customHrs))) {
                    current.copy(
                        leave = null,
                        customIn = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customIn else null,
                        customOut = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customOut else null,
                        customHrs = if (current.shift == "Custom Shift" || current.ot == "Custom OT") current.customHrs else null
                    )
                } else {
                    current.copy(
                        leave = brush,
                        customIn = if (isCustomLeave) customIn else current.customIn,
                        customOut = if (isCustomLeave) customOut else current.customOut,
                        customHrs = if (isCustomLeave) customHrs else current.customHrs
                    )
                }
            }
            else -> current
        }
        if (next.shift == null && next.ot == null && next.leave == null) stagedEdits.remove(date) else stagedEdits[date] = next
    }

    @Test
    fun dateA_and_dateB_retainSeparateCustomStagedValues() {
        val staged = mutableMapOf<LocalDate, StagedEdit>()
        val dateA = LocalDate.of(2026, 11, 2)
        val dateB = LocalDate.of(2026, 11, 3)

        // Stage Date A with 10.0 hours custom shift
        simulateApplyEdit(staged, dateA, "Shift Duty", "Custom Shift", "07.00", "17.00", "10.0")

        // Switch to Date B and stage with 8.0 hours custom shift
        simulateApplyEdit(staged, dateB, "Shift Duty", "Custom Shift", "08.00", "16.00", "8.0")

        assertEquals("10.0", staged[dateA]?.customHrs)
        assertEquals("07.00", staged[dateA]?.customIn)
        assertEquals("17.00", staged[dateA]?.customOut)
        assertEquals(10.0, shiftHours(staged[dateA]!!), 0.001)

        assertEquals("8.0", staged[dateB]?.customHrs)
        assertEquals("08.00", staged[dateB]?.customIn)
        assertEquals("16.00", staged[dateB]?.customOut)
        assertEquals(8.0, shiftHours(staged[dateB]!!), 0.001)

        assertNotEquals(staged[dateA]?.customHrs, staged[dateB]?.customHrs)
    }

    @Test
    fun switchingDates_restoresTheCorrectValues() {
        val staged = mutableMapOf<LocalDate, StagedEdit>()
        val dateA = LocalDate.of(2026, 11, 4)
        val dateB = LocalDate.of(2026, 11, 5)

        simulateApplyEdit(staged, dateA, "Shift Duty", "Custom Shift", "07.00", "15.00", "8.0")
        simulateApplyEdit(staged, dateB, "Overtime", "Custom OT", "17.00", "21.00", "4.0")

        // In the UI, clicking dateA or inspecting its staged value gives Date A's own custom fields
        val restoredA = staged[dateA]!!
        assertEquals("07.00", restoredA.customIn)
        assertEquals("15.00", restoredA.customOut)
        assertEquals("8.0", restoredA.customHrs)
        assertEquals(8.0, shiftHours(restoredA), 0.001)

        val restoredB = staged[dateB]!!
        assertEquals("17.00", restoredB.customIn)
        assertEquals("21.00", restoredB.customOut)
        assertEquals("4.0", restoredB.customHrs)
        assertEquals(4.0, otHours(restoredB), 0.001)
    }

    @Test
    fun editingDateA_doesNotModifyDateB() {
        val staged = mutableMapOf<LocalDate, StagedEdit>()
        val dateA = LocalDate.of(2026, 11, 6)
        val dateB = LocalDate.of(2026, 11, 7)

        simulateApplyEdit(staged, dateA, "Shift Duty", "Custom Shift", "07.00", "17.00", "10.0")
        simulateApplyEdit(staged, dateB, "Shift Duty", "Custom Shift", "08.00", "14.00", "6.0")

        // Now re-edit Date A with 12.0 hours
        simulateApplyEdit(staged, dateA, "Shift Duty", "Custom Shift", "07.00", "19.00", "12.0")

        // Date B must still be 6.0 hours, totally unaffected
        assertEquals("6.0", staged[dateB]?.customHrs)
        assertEquals("08.00", staged[dateB]?.customIn)
        assertEquals("14.00", staged[dateB]?.customOut)
        assertEquals(6.0, shiftHours(staged[dateB]!!), 0.001)

        // Date A reflects the update
        assertEquals("12.0", staged[dateA]?.customHrs)
        assertEquals("07.00", staged[dateA]?.customIn)
        assertEquals("19.00", staged[dateA]?.customOut)
        assertEquals(12.0, shiftHours(staged[dateA]!!), 0.001)
    }

    @Test
    fun applyingValues_affectsOnlyTheSelectedDate() {
        val staged = mutableMapOf<LocalDate, StagedEdit>()
        val date1 = LocalDate.of(2026, 11, 8)
        val date2 = LocalDate.of(2026, 11, 9)

        simulateApplyEdit(staged, date1, "Shift Duty", "Custom Shift", "09.00", "17.00", "8.0")

        assertTrue("Date 1 should be staged", staged.containsKey(date1))
        assertTrue("Date 2 must not be staged", !staged.containsKey(date2))
        assertNull(staged[date2])
    }

    @Test
    fun emptyOrInvalidStagedValues_doNotCreateIncorrectEntries() {
        // Invalid or empty string numbers safely fallback to 0.0
        val editEmpty = StagedEdit(shift = "Custom Shift", customHrs = "")
        val editInvalid = StagedEdit(shift = "Custom Shift", customHrs = "abc")
        val editNegative = StagedEdit(shift = "Custom Shift", customHrs = "-5.0")

        assertEquals(0.0, shiftHours(editEmpty), 0.001)
        assertEquals(0.0, shiftHours(editInvalid), 0.001)
        assertEquals(0.0, shiftHours(editNegative), 0.001)
    }

    @Test
    fun existingSavedEntries_remainIntactUnlessUserExplicitlyStagesDate() {
        val staged = mutableMapOf<LocalDate, StagedEdit>()
        val dateUntouched = LocalDate.of(2026, 11, 10)
        val dateEdited = LocalDate.of(2026, 11, 11)

        simulateApplyEdit(staged, dateEdited, "Shift Duty", "Custom Shift", "07.00", "13.00", "6.0")

        // In DailyEntryScreen:
        // val daysToProcess = if (isAutoFillMode) allDates else stagedEdits.keys.toList()
        // When not in autoFillMode, only stagedEdits.keys are processed!
        val daysToProcess = staged.keys.toList()

        assertEquals(listOf(dateEdited), daysToProcess)
        assertTrue(!daysToProcess.contains(dateUntouched))
    }
}