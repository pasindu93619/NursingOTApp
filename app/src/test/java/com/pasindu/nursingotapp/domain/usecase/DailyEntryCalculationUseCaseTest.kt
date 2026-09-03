package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyEntryCalculationUseCaseTest {

    @Test
    fun calculateDailyEntryHours_delegatesToAuthoritativeOtEngine() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val logs = listOf(
            dailyLog(
                id = 1L,
                date = LocalDate.of(2026, 9, 6),
                normalHours = 6f,
                otHours = 2f
            )
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        assertEquals(6f, result.totalNormalHours, 0.001f)
        assertEquals(2f, result.totalOtHours, 0.001f)
    }

    @Test
    fun calculateDailyEntryHours_splitsHoursAcrossWeekly36HourBoundary() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val logs = listOf(
            dailyLog(1L, LocalDate.of(2026, 9, 6), 12f, 0f),
            dailyLog(2L, LocalDate.of(2026, 9, 7), 12f, 0f),
            dailyLog(3L, LocalDate.of(2026, 9, 8), 12f, 0f),
            dailyLog(4L, LocalDate.of(2026, 9, 9), 12f, 0f)
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        assertEquals(36f, result.totalNormalHours, 0.001f)
        assertEquals(12f, result.totalOtHours, 0.001f)
    }

    @Test
    fun calculateDailyEntryHours_usesTimestampHoursWhenStoredTotalsAreStale() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val logs = listOf(
            dailyLog(
                id = 1L,
                date = LocalDate.of(2026, 9, 6),
                normalHours = 0f,
                otHours = 0f,
                normalIn = "07.00",
                normalOut = "13.00",
                otIn = "13.00",
                otOut = "15.00"
            )
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        assertEquals(8f, result.totalNormalHours, 0.001f)
        assertEquals(0f, result.totalOtHours, 0.001f)
    }

    private fun dailyLog(
        id: Long,
        date: LocalDate,
        normalHours: Float,
        otHours: Float,
        normalIn: String = "07.00",
        normalOut: String = "13.00",
        otIn: String = "13.00",
        otOut: String = "15.00"
    ) = DailyLog(
        id = id,
        date = date,
        isPH = false,
        isDO = false,
        isLeave = false,
        leaveType = null,
        reason = "",
        wardOverride = "Normal",
        normalTimeInStr = normalIn,
        normalTimeOutStr = normalOut,
        otTimeInStr = otIn,
        otTimeOutStr = otOut,
        computedNormalHours = normalHours,
        computedOtHours = otHours
    )
}
