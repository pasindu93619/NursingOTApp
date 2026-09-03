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
            DailyLog(
                id = 1L,
                date = LocalDate.of(2026, 9, 6),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                reason = "",
                wardOverride = "Normal",
                normalTimeInStr = "07.00",
                normalTimeOutStr = "13.00",
                otTimeInStr = "13.00",
                otTimeOutStr = "15.00",
                computedNormalHours = 6f,
                computedOtHours = 2f
            )
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        // WeeklyOtCalculator is authoritative: it derives the normal/OT split
        // from the weekly 36-hour rule. This test checks that this use case
        // returns the engine's result rather than trusting pre-filled fields.
        assertEquals(6f, result.totalNormalHours, 0.001f)
        assertEquals(2f, result.totalOtHours, 0.001f)
    }

    @Test
    fun calculateDailyEntryHours_splitsHoursAcrossWeekly36HourBoundary() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val logs = listOf(
            DailyLog(
                id = 1L,
                date = LocalDate.of(2026, 9, 6),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                reason = "",
                wardOverride = "Normal",
                normalTimeInStr = "07.00",
                normalTimeOutStr = "19.00",
                otTimeInStr = "",
                otTimeOutStr = "",
                computedNormalHours = 12f,
                computedOtHours = 0f
            ),
            DailyLog(
                id = 2L,
                date = LocalDate.of(2026, 9, 7),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                reason = "",
                wardOverride = "Normal",
                normalTimeInStr = "07.00",
                normalTimeOutStr = "19.00",
                otTimeInStr = "",
                otTimeOutStr = "",
                computedNormalHours = 12f,
                computedOtHours = 0f
            ),
            DailyLog(
                id = 3L,
                date = LocalDate.of(2026, 9, 8),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                reason = "",
                wardOverride = "Normal",
                normalTimeInStr = "07.00",
                normalTimeOutStr = "19.00",
                otTimeInStr = "",
                otTimeOutStr = "",
                computedNormalHours = 12f,
                computedOtHours = 0f
            ),
            DailyLog(
                id = 4L,
                date = LocalDate.of(2026, 9, 9),
                isPH = false,
                isDO = false,
                isLeave = false,
                leaveType = null,
                reason = "",
                wardOverride = "Normal",
                normalTimeInStr = "07.00",
                normalTimeOutStr = "19.00",
                otTimeInStr = "",
                otTimeOutStr = "",
                computedNormalHours = 12f,
                computedOtHours = 0f
            )
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        assertEquals(36f, result.totalNormalHours, 0.001f)
        assertEquals(12f, result.totalOtHours, 0.001f)
    }
}
