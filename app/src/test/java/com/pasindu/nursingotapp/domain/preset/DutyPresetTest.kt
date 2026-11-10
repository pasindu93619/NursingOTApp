package com.pasindu.nursingotapp.domain.preset

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DutyPresetTest {

    @Test
    fun allFiveApprovedPresetsExist() {
        val presets = DutyPreset.allPresets
        assertEquals(5, presets.size)

        val ids = presets.map { it.id }.toSet()
        assertTrue(ids.contains(DutyPresetId.STANDARD_WARD_MORNING))
        assertTrue(ids.contains(DutyPresetId.STANDARD_WARD_DAY))
        assertTrue(ids.contains(DutyPresetId.ICU_12H_DAY))
        assertTrue(ids.contains(DutyPresetId.ICU_12H_NIGHT))
        assertTrue(ids.contains(DutyPresetId.STANDARD_WARD_EVENING))
    }

    @Test
    fun standardWardMorningIsConsistentAcrossWeekdaysAndWeekends() {
        val preset = DutyPreset.StandardWardMorning
        val monday = LocalDate.of(2026, 3, 2)
        val saturday = LocalDate.of(2026, 3, 7)
        val sunday = LocalDate.of(2026, 3, 8)

        val monSpec = preset.getShiftForDate(monday)
        val satSpec = preset.getShiftForDate(saturday)
        val sunSpec = preset.getShiftForDate(sunday)

        assertEquals("07.00", monSpec.timeIn)
        assertEquals("13.00", monSpec.timeOut)
        assertEquals(6.0f, monSpec.hours, 0.01f)

        assertEquals("07.00", satSpec.timeIn)
        assertEquals("13.00", satSpec.timeOut)
        assertEquals(6.0f, satSpec.hours, 0.01f)

        assertEquals("07.00", sunSpec.timeIn)
        assertEquals("13.00", sunSpec.timeOut)
        assertEquals(6.0f, sunSpec.hours, 0.01f)
    }

    @Test
    fun standardWardDayAdjustsForWeekendsAndPublicHolidays() {
        val preset = DutyPreset.StandardWardDay
        val wednesday = LocalDate.of(2026, 3, 4)
        val saturday = LocalDate.of(2026, 3, 7)
        val wednesdayPH = LocalDate.of(2026, 3, 4)

        val weekdaySpec = preset.getShiftForDate(wednesday, isPublicHoliday = false)
        assertEquals("07.00", weekdaySpec.timeIn)
        assertEquals("16.00", weekdaySpec.timeOut)
        assertEquals(9.0f, weekdaySpec.hours, 0.01f)

        val weekendSpec = preset.getShiftForDate(saturday, isPublicHoliday = false)
        assertEquals("07.00", weekendSpec.timeIn)
        assertEquals("13.00", weekendSpec.timeOut)
        assertEquals(6.0f, weekendSpec.hours, 0.01f)

        val phSpec = preset.getShiftForDate(wednesdayPH, isPublicHoliday = true)
        assertEquals("07.00", phSpec.timeIn)
        assertEquals("13.00", phSpec.timeOut)
        assertEquals(6.0f, phSpec.hours, 0.01f)
    }

    @Test
    fun icu12hDayProvides12HourDayCoverage() {
        val preset = DutyPreset.Icu12hDay
        val tuesday = LocalDate.of(2026, 3, 3)

        val spec = preset.getShiftForDate(tuesday)
        assertEquals("07.00", spec.timeIn)
        assertEquals("19.00", spec.timeOut)
        assertEquals(12.0f, spec.hours, 0.01f)
    }

    @Test
    fun icu12hNightPreservesOvernightShiftAcrossMidnight() {
        val preset = DutyPreset.Icu12hNight
        val thursday = LocalDate.of(2026, 3, 5)

        val spec = preset.getShiftForDate(thursday)
        assertEquals("19.00", spec.timeIn)
        assertEquals("07.00", spec.timeOut)
        assertEquals(12.0f, spec.hours, 0.01f)
        assertEquals("Night (19-7)", spec.brushLabel)
    }

    @Test
    fun standardWardEveningProvidesEveningShift() {
        val preset = DutyPreset.StandardWardEvening
        val friday = LocalDate.of(2026, 3, 6)

        val spec = preset.getShiftForDate(friday)
        assertEquals("13.00", spec.timeIn)
        assertEquals("19.00", spec.timeOut)
        assertEquals(6.0f, spec.hours, 0.01f)
        assertEquals("Eve (13-19)", spec.brushLabel)
    }
}
