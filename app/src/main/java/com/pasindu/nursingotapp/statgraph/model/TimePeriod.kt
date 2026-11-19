package com.pasindu.nursingotapp.statgraph.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Strongly typed representation of a period belonging to [FieldRole.Time].
 * Supports sorting, gap detection, display formatting, and next-period generation.
 */
sealed class TimePeriod : Comparable<TimePeriod> {
    abstract val granularity: TimeGranularity

    abstract fun formatIso(): String
    abstract fun formatDisplay(): String
    abstract fun nextPeriod(): TimePeriod
    abstract fun sortKey(): Long

    override fun compareTo(other: TimePeriod): Int = sortKey().compareTo(other.sortKey())

    data class Day(val date: LocalDate) : TimePeriod() {
        override val granularity: TimeGranularity = TimeGranularity.DAY

        override fun formatIso(): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

        override fun formatDisplay(): String =
            date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

        override fun nextPeriod(): Day = Day(date.plusDays(1))

        override fun sortKey(): Long = date.toEpochDay()
    }

    data class Week(val year: Int, val weekOfYear: Int) : TimePeriod() {
        override val granularity: TimeGranularity = TimeGranularity.WEEK

        override fun formatIso(): String = String.format(Locale.US, "%04d-W%02d", year, weekOfYear)

        override fun formatDisplay(): String = "W$weekOfYear $year"

        override fun nextPeriod(): Week {
            val monday = LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekOfYear.toLong())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val nextMonday = monday.plusWeeks(1)
            return Week(
                year = nextMonday.get(IsoFields.WEEK_BASED_YEAR),
                weekOfYear = nextMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            )
        }

        override fun sortKey(): Long = (year.toLong() * 100L) + weekOfYear.toLong()
    }

    data class Month(val yearMonth: YearMonth) : TimePeriod() {
        override val granularity: TimeGranularity = TimeGranularity.MONTH

        override fun formatIso(): String = String.format(
            Locale.US,
            "%04d-%02d",
            yearMonth.year,
            yearMonth.monthValue
        )

        override fun formatDisplay(): String {
            val monthShort = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            return "$monthShort ${yearMonth.year}"
        }

        override fun nextPeriod(): Month = Month(yearMonth.plusMonths(1))

        override fun sortKey(): Long = (yearMonth.year.toLong() * 12L) + (yearMonth.monthValue - 1).toLong()
    }

    data class Year(val year: Int) : TimePeriod() {
        override val granularity: TimeGranularity = TimeGranularity.YEAR

        override fun formatIso(): String = String.format(Locale.US, "%04d", year)

        override fun formatDisplay(): String = year.toString()

        override fun nextPeriod(): Year = Year(year + 1)

        override fun sortKey(): Long = year.toLong()
    }
}

/**
 * Parses common ISO and clinical reporting period strings into [TimePeriod].
 */
object TimePeriodParser {

    private val isoDateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val slashDateRegex = Regex("""^\d{2}/\d{2}/\d{4}$""")
    private val isoWeekRegex = Regex("""^(\d{4})-?[Ww](\d{1,2})$""")
    private val isoMonthRegex = Regex("""^(\d{4})-(\d{1,2})$""")
    private val yearRegex = Regex("""^(\d{4})$""")
    private val monthNameYearRegex = Regex("""^([A-Za-z]{3,9})\s+(\d{4})$""")

    private val monthFormatters = listOf(
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    )

    fun parse(raw: String?, expectedGranularity: TimeGranularity? = null): TimePeriod? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        return when (expectedGranularity) {
            TimeGranularity.DAY -> parseDay(text)
            TimeGranularity.WEEK -> parseWeek(text)
            TimeGranularity.MONTH -> parseMonth(text)
            TimeGranularity.YEAR -> parseYear(text)
            null -> parseDay(text) ?: parseWeek(text) ?: parseMonth(text) ?: parseYear(text)
        }
    }

    fun detectGranularity(samples: List<String>): TimeGranularity? {
        val nonBlank = samples.map { it.trim() }.filter { it.isNotEmpty() }
        if (nonBlank.isEmpty()) return null

        return when {
            nonBlank.all { parseDay(it) != null } -> TimeGranularity.DAY
            nonBlank.all { parseWeek(it) != null } -> TimeGranularity.WEEK
            nonBlank.all { parseMonth(it) != null } -> TimeGranularity.MONTH
            nonBlank.all { parseYear(it) != null } -> TimeGranularity.YEAR
            else -> null
        }
    }

    fun enumerateBetween(start: TimePeriod, end: TimePeriod, maxSteps: Int = 24): List<TimePeriod> {
        if (start.granularity != end.granularity || start.sortKey() >= end.sortKey()) return emptyList()
        val missing = mutableListOf<TimePeriod>()
        var cursor = start.nextPeriod()
        var steps = 0
        while (cursor.sortKey() < end.sortKey() && steps < maxSteps) {
            missing.add(cursor)
            cursor = cursor.nextPeriod()
            steps++
        }
        return missing
    }

    private fun parseDay(text: String): TimePeriod.Day? {
        if (isoDateRegex.matches(text)) {
            return runCatching { TimePeriod.Day(LocalDate.parse(text)) }.getOrNull()
        }
        if (slashDateRegex.matches(text)) {
            return runCatching {
                TimePeriod.Day(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            }.getOrNull()
        }
        return null
    }

    private fun parseWeek(text: String): TimePeriod.Week? {
        val match = isoWeekRegex.matchEntire(text) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val week = match.groupValues[2].toIntOrNull() ?: return null
        if (week !in 1..53) return null
        return TimePeriod.Week(year, week)
    }

    private fun parseMonth(text: String): TimePeriod.Month? {
        val isoMatch = isoMonthRegex.matchEntire(text)
        if (isoMatch != null) {
            val year = isoMatch.groupValues[1].toIntOrNull() ?: return null
            val month = isoMatch.groupValues[2].toIntOrNull() ?: return null
            if (month !in 1..12) return null
            return TimePeriod.Month(YearMonth.of(year, month))
        }
        if (monthNameYearRegex.matches(text)) {
            for (formatter in monthFormatters) {
                val parsed = runCatching { YearMonth.parse(text, formatter) }.getOrNull()
                if (parsed != null) return TimePeriod.Month(parsed)
            }
        }
        return null
    }

    private fun parseYear(text: String): TimePeriod.Year? {
        val match = yearRegex.matchEntire(text) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        if (year !in 1900..2100) return null
        return TimePeriod.Year(year)
    }
}
