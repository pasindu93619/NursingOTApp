package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.model.DailyLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyEntryCalculationUseCaseTest {

    @Test
    fun calculateDailyEntryHours_usesRecordedNormalAndOtSeparatelyForWeeklyAllocation() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val result = useCase(
            logs = listOf(
                dailyLog(
                    id = 1L,
                    date = LocalDate.of(2026, 9, 6),
                    normalHours = 6f,
                    otHours = 2f
                )
            ),
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        // Weekly rule: normal duty is 6h, so 2h recorded OT fills the
        // missing part of the 36h normal requirement first.
        assertEquals(8f, result.totalNormalHours, 0.001f)
        assertEquals(0f, result.totalOtHours, 0.001f)
    }

    @Test
    fun calculateDailyEntryHours_movesNormalDutyOverflowToOtAtWeeklyBoundary() {
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
    fun calculateDailyEntryHours_usesOtToFillWeeklyNormalDeficit() {
        val useCase = CalculateDailyEntryHoursUseCase()
        val logs = listOf(
            dailyLog(1L, LocalDate.of(2026, 9, 6), 12f, 0f),
            dailyLog(2L, LocalDate.of(2026, 9, 7), 12f, 0f),
            dailyLog(3L, LocalDate.of(2026, 9, 8), 6f, 6f),
            dailyLog(4L, LocalDate.of(2026, 9, 9), 0f, 6f)
        )

        val result = useCase(
            logs = logs,
            claimStart = LocalDate.of(2026, 9, 1),
            claimEnd = LocalDate.of(2026, 9, 30)
        )

        // Normal duty = 30h. First 6h of recorded OT fills the normal
        // requirement; the remaining 6h remains OT.
        assertEquals(36f, result.totalNormalHours, 0.001f)
        assertEquals(6f, result.totalOtHours, 0.001f)
    }

    @Test
    fun calculateDailyEntryHours_doesNotInventHoursFromTimestampsWhenStoredTotalsAreZero() {
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

        assertEquals(0f, result.totalNormalHours, 0.001f)
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
