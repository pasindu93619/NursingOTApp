package com.pasindu.nursingotapp.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class BuildClaimPeriodDateRangeUseCase {
    operator fun invoke(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        val firstSunday = today.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = today.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        return firstSunday to lastSaturday
    }
}

class ParseClaimPeriodDateUseCase {
    operator fun invoke(year: String, month: String, day: String): LocalDate? =
        runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
}

class SummarizeClaimPeriodWeeksUseCase {
    operator fun invoke(start: LocalDate, end: LocalDate): Int {
        val firstSunday = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        return if (firstSunday.isAfter(lastSaturday)) 0
        else ChronoUnit.WEEKS.between(firstSunday, lastSaturday.plusDays(1)).toInt()
    }
}
