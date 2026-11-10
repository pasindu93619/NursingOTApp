package com.pasindu.nursingotapp.domain.preset

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Standard shift time specification.
 *
 * @property timeIn Formatted start time string (e.g. "07.00").
 * @property timeOut Formatted end time string (e.g. "13.00", "16.00", "19.00", "07.00").
 * @property hours Total duty duration in decimal hours (e.g. 6.0f, 9.0f, 12.0f).
 * @property brushLabel Corresponding brush label in DailyEntryScreen.
 */
data class ShiftTimeSpec(
    val timeIn: String,
    val timeOut: String,
    val hours: Float,
    val brushLabel: String
)

enum class DutyPresetId {
    STANDARD_WARD_MORNING,
    STANDARD_WARD_DAY,
    ICU_12H_DAY,
    ICU_12H_NIGHT,
    STANDARD_WARD_EVENING
}

/**
 * Domain representation of a clinical duty preset pattern.
 * Supports standard ward rotations, ICU/Emergency 12h shifts, and weekend/short-day rules.
 */
data class DutyPreset(
    val id: DutyPresetId,
    val title: String,
    val subtitle: String,
    val weekdayShift: ShiftTimeSpec,
    val shortDayShift: ShiftTimeSpec
) {
    /**
     * Resolves the duty specification for a given calendar date.
     * Saturday, Sunday, or Public Holiday are treated as short days for day shifts.
     */
    fun getShiftForDate(date: LocalDate, isPublicHoliday: Boolean = false): ShiftTimeSpec {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isShortDay = isWeekend || isPublicHoliday
        return if (isShortDay) shortDayShift else weekdayShift
    }

    companion object {
        /**
         * 1. Standard Ward (Morning Focus):
         * Morning duty (07:00–13:00, 6h) every day.
         */
        val StandardWardMorning = DutyPreset(
            id = DutyPresetId.STANDARD_WARD_MORNING,
            title = "Standard Ward (Morning)",
            subtitle = "Daily morning shift: 07:00–13:00 (6.0h)",
            weekdayShift = ShiftTimeSpec("07.00", "13.00", 6.0f, "Morn (7-13)"),
            shortDayShift = ShiftTimeSpec("07.00", "13.00", 6.0f, "Morn (7-13)")
        )

        /**
         * 2. Standard Ward (Day / Full Duty Focus):
         * Weekday day duty (07:00–16:00, 9h), weekend/PH morning duty (07:00–13:00, 6h).
         */
        val StandardWardDay = DutyPreset(
            id = DutyPresetId.STANDARD_WARD_DAY,
            title = "Standard Ward (Day / Full)",
            subtitle = "Weekday 07:00–16:00 (9.0h) • Weekend 07:00–13:00 (6.0h)",
            weekdayShift = ShiftTimeSpec("07.00", "16.00", 9.0f, "Day (7-16)"),
            shortDayShift = ShiftTimeSpec("07.00", "13.00", 6.0f, "Morn (7-13)")
        )

        /**
         * 3. ICU / Emergency (12h Day Focus):
         * Continuous 12-hour day shift (07:00–19:00, 12h) weekdays and weekends.
         */
        val Icu12hDay = DutyPreset(
            id = DutyPresetId.ICU_12H_DAY,
            title = "ICU / Emergency (12h Day)",
            subtitle = "Daily 12h day duty: 07:00–19:00 (12.0h)",
            weekdayShift = ShiftTimeSpec("07.00", "19.00", 12.0f, "Custom Shift"),
            shortDayShift = ShiftTimeSpec("07.00", "19.00", 12.0f, "Custom Shift")
        )

        /**
         * 4. ICU / Emergency (12h Night Focus) & Night-Heavy:
         * Continuous 12-hour overnight shift (19:00–07:00, 12h) crossing midnight.
         */
        val Icu12hNight = DutyPreset(
            id = DutyPresetId.ICU_12H_NIGHT,
            title = "Night-Heavy / ICU (12h Night)",
            subtitle = "Daily overnight duty: 19:00–07:00 (12.0h across midnight)",
            weekdayShift = ShiftTimeSpec("19.00", "07.00", 12.0f, "Night (19-7)"),
            shortDayShift = ShiftTimeSpec("19.00", "07.00", 12.0f, "Night (19-7)")
        )

        /**
         * 5. Standard Ward (Evening Focus):
         * Evening duty (13:00–19:00, 6h) weekdays and weekends.
         */
        val StandardWardEvening = DutyPreset(
            id = DutyPresetId.STANDARD_WARD_EVENING,
            title = "Standard Ward (Evening)",
            subtitle = "Daily evening shift: 13:00–19:00 (6.0h)",
            weekdayShift = ShiftTimeSpec("13.00", "19.00", 6.0f, "Eve (13-19)"),
            shortDayShift = ShiftTimeSpec("13.00", "19.00", 6.0f, "Eve (13-19)")
        )

        /**
         * List of all 5 approved presets.
         */
        val allPresets: List<DutyPreset> = listOf(
            StandardWardMorning,
            StandardWardDay,
            Icu12hDay,
            Icu12hNight,
            StandardWardEvening
        )
    }
}
