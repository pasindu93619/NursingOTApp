package com.pasindu.nursingotapp.domain.preset

import com.pasindu.nursingotapp.domain.calculation.WeeklyCalculationEngine
import com.pasindu.nursingotapp.ui.model.DailyEntryUiModel
import com.pasindu.nursingotapp.ui.screens.StagedEdit
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DutyPresetApplicationTest {

    private val marchDates = (1..31).map { LocalDate.of(2026, 3, it) }

    @Test
    fun overnightShiftDurationIsAccuratelyCalculatedAcrossMidnight() {
        val preset = DutyPreset.Icu12hNight
        val sampleDate = LocalDate.of(2026, 3, 10)
        val spec = preset.getShiftForDate(sampleDate)

        assertEquals("19.00", spec.timeIn)
        assertEquals("07.00", spec.timeOut)
        assertEquals(12.0f, spec.hours, 0.01f)

        val duration = WeeklyCalculationEngine.calculateShiftHours(
            LocalTime.of(19, 0),
            LocalTime.of(7, 0)
        )
        assertEquals(12.0, duration, 0.001)
    }

    @Test
    fun applyingPresetPreservesPlannedLeaveAndOffDays() {
        // Setup: March 1 is Sunday, March 2 is CL, March 3 is DO, March 4 is PH, March 5-31 unassigned
        val existingEntries = listOf(
            DailyEntryUiModel(
                id = 1,
                claimPeriodId = 1L,
                date = LocalDate.of(2026, 3, 2),
                isPH = false,
                isDO = false,
                isLeave = true,
                leaveType = "CL",
                normalTimeIn = "",
                normalTimeOut = "",
                normalHours = 6f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            ),
            DailyEntryUiModel(
                id = 2,
                claimPeriodId = 1L,
                date = LocalDate.of(2026, 3, 3),
                isPH = false,
                isDO = true,
                isLeave = true,
                leaveType = "Unworked DO",
                normalTimeIn = "",
                normalTimeOut = "",
                normalHours = 0f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            ),
            DailyEntryUiModel(
                id = 3,
                claimPeriodId = 1L,
                date = LocalDate.of(2026, 3, 4),
                isPH = true,
                isDO = false,
                isLeave = true,
                leaveType = "PH",
                normalTimeIn = "",
                normalTimeOut = "",
                normalHours = 6f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            )
        )

        val stagedEdits = mutableMapOf<LocalDate, StagedEdit>()

        val exceptionDates = marchDates.filter { date ->
            val staged = stagedEdits[date]
            val existing = existingEntries.find { it.date == date }
            (staged?.leave != null && staged.leave !in listOf("Clear Leave", "Clear Exceptions")) ||
                (existing?.isLeave == true || existing?.isDO == true || existing?.isPH == true)
        }.toSet()

        assertEquals(3, exceptionDates.size)
        assertTrue(exceptionDates.contains(LocalDate.of(2026, 3, 2)))
        assertTrue(exceptionDates.contains(LocalDate.of(2026, 3, 3)))
        assertTrue(exceptionDates.contains(LocalDate.of(2026, 3, 4)))

        // Filter target dates with preserveExceptions = true
        val targetDates = marchDates.filter { it !in exceptionDates }
        assertEquals(28, targetDates.size)

        // Apply StandardWardDay preset
        val preset = DutyPreset.StandardWardDay
        targetDates.forEach { date ->
            val spec = preset.getShiftForDate(date, isPublicHoliday = false)
            stagedEdits[date] = StagedEdit(
                shift = spec.brushLabel,
                customIn = spec.timeIn,
                customOut = spec.timeOut,
                customHrs = spec.hours.toString()
            )
        }

        // Verify exceptions were not modified in stagedEdits
        assertFalse(stagedEdits.containsKey(LocalDate.of(2026, 3, 2)))
        assertFalse(stagedEdits.containsKey(LocalDate.of(2026, 3, 3)))
        assertFalse(stagedEdits.containsKey(LocalDate.of(2026, 3, 4)))

        // Verify target weekday got 9h Day shift
        val mondayStaged = stagedEdits[LocalDate.of(2026, 3, 9)]
        assertEquals("Day (7-16)", mondayStaged?.shift)
        assertEquals("07.00", mondayStaged?.customIn)
        assertEquals("16.00", mondayStaged?.customOut)
        assertEquals("9.0", mondayStaged?.customHrs)

        // Verify weekend got 6h Morning shift
        val saturdayStaged = stagedEdits[LocalDate.of(2026, 3, 7)]
        assertEquals("Morn (7-13)", saturdayStaged?.shift)
        assertEquals("07.00", saturdayStaged?.customIn)
        assertEquals("13.00", saturdayStaged?.customOut)
        assertEquals("6.0", saturdayStaged?.customHrs)
    }

    @Test
    fun stagedLeaveExceptionsArePreservedWhenApplyingPresets() {
        val stagedEdits = mutableMapOf<LocalDate, StagedEdit>()
        // User staged a VL (Vacation Leave) on March 15 in memory
        stagedEdits[LocalDate.of(2026, 3, 15)] = StagedEdit(leave = "VL")

        val exceptionDates = marchDates.filter { date ->
            val staged = stagedEdits[date]
            (staged?.leave != null && staged.leave !in listOf("Clear Leave", "Clear Exceptions"))
        }.toSet()

        assertTrue(exceptionDates.contains(LocalDate.of(2026, 3, 15)))

        val targetDates = marchDates.filter { it !in exceptionDates }
        val preset = DutyPreset.StandardWardMorning

        targetDates.forEach { date ->
            val spec = preset.getShiftForDate(date)
            stagedEdits[date] = StagedEdit(shift = spec.brushLabel, customIn = spec.timeIn, customOut = spec.timeOut, customHrs = spec.hours.toString())
        }

        // March 15 should still be Vacation Leave
        assertEquals("VL", stagedEdits[LocalDate.of(2026, 3, 15)]?.leave)
        // Other dates have Morning shift
        assertEquals("Morn (7-13)", stagedEdits[LocalDate.of(2026, 3, 16)]?.shift)
    }

    @Test
    fun existingRecordedShiftsAreDetectedForOverwriteProtection() {
        // March 10 already has a Morning duty in DB
        val existingEntries = listOf(
            DailyEntryUiModel(
                id = 10,
                claimPeriodId = 1L,
                date = LocalDate.of(2026, 3, 10),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                normalTimeIn = "07.00",
                normalTimeOut = "13.00",
                normalHours = 6f,
                otTimeIn = "",
                otTimeOut = "",
                otHours = 0f,
                wardOverride = "",
                reason = ""
            )
        )
        val stagedEdits = mutableMapOf<LocalDate, StagedEdit>()

        val existingShiftDates = marchDates.filter { date ->
            val staged = stagedEdits[date]
            val existing = existingEntries.find { it.date == date }
            staged?.shift != null || (existing != null && existing.normalTimeIn.isNotBlank())
        }

        assertEquals(1, existingShiftDates.size)
        assertEquals(LocalDate.of(2026, 3, 10), existingShiftDates[0])
    }
}
