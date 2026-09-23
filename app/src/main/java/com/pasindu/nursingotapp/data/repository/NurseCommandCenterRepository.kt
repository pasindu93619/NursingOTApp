package com.pasindu.nursingotapp.data.repository

import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.FinancialRecordEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Single data gateway for the Nurse Command Center.
 *
 * Room details stay here instead of leaking into Compose UI. The snapshot is
 * intentionally small and dashboard-friendly so more sources can be added
 * without redesigning the screen.
 */
class NurseCommandCenterRepository(
    private val database: AppDatabase
) {
    private val profileDao = database.profileDao()
    private val claimPeriodDao = database.claimPeriodDao()
    private val dailyEntryDao = database.dailyEntryDao()
    private val financialDao = database.financialDao()
    private val clinicalPlanningDao = database.clinicalPlanningDao()
    private val knowledgeHubDao = database.knowledgeHubDao()

    data class Snapshot(
        val profile: ProfileEntity?,
        val dutyHours: Double,
        val otHours: Double,
        val phHours: Double,
        val claimCompletedDays: Int,
        val claimTotalDays: Int,
        val consecutiveWorkedDays: Int,
        val grossSalary: Double,
        val netSalary: Double?,
        val pendingClinicalTasks: Int,
        val cpdPoints: Int,
        val pendingClinicalTaskDetails: List<ClinicalTaskEntity>,
        val todayDutyRecorded: Boolean,
        val todayDutyHours: Double,
        val todayOtHours: Double,
        val todayPh: Boolean,
        val todayClaimRecorded: Boolean
    )

    fun observeSnapshot(
        month: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now()
    ): Flow<Snapshot> {
        val start: LocalDate = month.atDay(1)
        val end: LocalDate = month.atEndOfMonth()

        return combine(
            profileDao.observeProfile(),
            claimPeriodDao.observeClaimPeriods(),
            dailyEntryDao.observeAllEntries(),
            financialDao.getAllFinancialRecords(),
            clinicalPlanningDao.getAllTasks()
        ) { currentProfile, claimPeriods, entries, finance, clinicalTasks ->
            FiveWay(
                currentProfile = currentProfile,
                claimPeriods = claimPeriods,
                entries = entries,
                finance = finance,
                clinicalTasks = clinicalTasks
            )
        }.combine(knowledgeHubDao.getAllCpdLogs()) { five, cpdLogs ->
            val currentProfile = five.currentProfile
            val claimPeriods = five.claimPeriods
            val entries = five.entries
            val finance = five.finance
            val clinicalTasks = five.clinicalTasks

            val currentClaimPeriod = claimPeriods
                .asSequence()
                .filter { period ->
                    !today.isBefore(period.startDate) &&
                        !today.isAfter(period.endDate)
                }
                .maxByOrNull { it.startDate }

            val claimEndThroughToday = currentClaimPeriod?.endDate?.let { periodEnd ->
                minOf(periodEnd, today)
            }

            val claimEntries = currentClaimPeriod?.let { period ->
                val effectiveEnd = claimEndThroughToday ?: period.endDate
                entries.filter { entry ->
                    entry.claimPeriodId == period.id &&
                        !entry.date.isBefore(period.startDate) &&
                        !entry.date.isAfter(effectiveEnd)
                }
            }.orEmpty()

            // Home Duty is the actual recorded hours in the CURRENT OT claim period,
            // not the calendar month. A DailyEntry shift can contain both normal-duty
            // hours and separately entered OT hours, so both fields are part of the
            // recorded duty-shift total.
            val dutyHoursToDate = claimEntries.sumOf { entry ->
                (entry.normalHours.toDouble() + entry.otHours.toDouble())
                    .coerceAtLeast(0.0)
            }

            // Home OT uses only the universal 36-hour Sunday-Saturday threshold.
            // There is no separate Home "additional OT" category: DailyEntryEntity.otHours
            // is already a component of the same recorded duty-shift entry.
            val dutyOtHours = claimEntries
                .groupBy { sundayOfWeek(it.date) }
                .values
                .sumOf { weekEntries ->
                    val weekDutyHours = weekEntries.sumOf { entry ->
                        (entry.normalHours.toDouble() + entry.otHours.toDouble())
                            .coerceAtLeast(0.0)
                    }
                    (weekDutyHours - WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS)
                        .coerceAtLeast(0.0)
                }

            val phHours = claimEntries
                .filter { it.isPH }
                .sumOf { it.normalHours.toDouble() + it.otHours.toDouble() }

            val consecutiveWorkedDays = calculateCurrentConsecutiveWorkedDays(claimEntries)

            val monthlyEntries = entries.filter { entry ->
                entry.date >= start && entry.date <= end
            }

            val currentMonthKey = month.toString()
            val currentMonthFinance = finance.firstOrNull {
                it.recordMonth == currentMonthKey
            }

            val pendingTaskDetails = clinicalTasks
                .filter { !it.isCompleted }
                .sortedWith(
                    compareBy<ClinicalTaskEntity> { priorityRank(it.priority) }
                        .thenBy { it.triggerTime }
                )

            val todayEntry = entries
                .filter { it.date == today }
                .maxByOrNull { it.id }

            Snapshot(
                profile = currentProfile,
                dutyHours = dutyHoursToDate,
                otHours = dutyOtHours,
                phHours = phHours,
                claimCompletedDays = monthlyEntries.count {
                    !it.isLeave && (
                        it.normalHours > 0f ||
                            it.otHours > 0f ||
                            it.isPH ||
                            it.isDO
                        )
                },
                claimTotalDays = end.dayOfMonth,
                consecutiveWorkedDays = consecutiveWorkedDays,
                grossSalary = currentMonthFinance?.grossSalary
                    ?: currentProfile?.basicSalary
                    ?: 0.0,
                netSalary = currentMonthFinance?.netSalary,
                pendingClinicalTasks = pendingTaskDetails.size,
                cpdPoints = cpdLogs.sumOf { it.earnedPoints },
                pendingClinicalTaskDetails = pendingTaskDetails,
                todayDutyRecorded = todayEntry != null && (
                    todayEntry.normalHours > 0f ||
                        todayEntry.otHours > 0f ||
                        todayEntry.isPH ||
                        todayEntry.isDO ||
                        todayEntry.isLeave
                    ),
                todayDutyHours = todayEntry?.normalHours?.toDouble() ?: 0.0,
                todayOtHours = todayEntry?.otHours?.toDouble() ?: 0.0,
                todayPh = todayEntry?.isPH == true,
                todayClaimRecorded = todayEntry != null
            )
        }
    }

    private fun calculateCurrentConsecutiveWorkedDays(
        entries: List<DailyEntryEntity>
    ): Int {
        val workedDates = entries
            .asSequence()
            .filter { entry ->
                !entry.isLeave &&
                    (entry.normalHours > 0f || entry.otHours > 0f)
            }
            .map { it.date }
            .distinct()
            .sorted()
            .toList()

        if (workedDates.isEmpty()) return 0

        var currentRun = 1

        for (index in workedDates.lastIndex downTo 1) {
            val currentDate = workedDates[index]
            val previousDate = workedDates[index - 1]

            if (previousDate == currentDate.minusDays(1)) {
                currentRun++
            } else {
                break
            }
        }

        return currentRun
    }

    private data class FiveWay(
        val currentProfile: ProfileEntity?,
        val claimPeriods: List<ClaimPeriodEntity>,
        val entries: List<DailyEntryEntity>,
        val finance: List<FinancialRecordEntity>,
        val clinicalTasks: List<ClinicalTaskEntity>
    )

    suspend fun setClinicalTaskCompleted(taskId: Int, completed: Boolean = true) {
        clinicalPlanningDao.setTaskCompleted(taskId, completed)
    }

    private fun sundayOfWeek(date: LocalDate): LocalDate {
        val daysFromSunday = when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0L
            DayOfWeek.MONDAY -> 1L
            DayOfWeek.TUESDAY -> 2L
            DayOfWeek.WEDNESDAY -> 3L
            DayOfWeek.THURSDAY -> 4L
            DayOfWeek.FRIDAY -> 5L
            DayOfWeek.SATURDAY -> 6L
        }
        return date.minusDays(daysFromSunday)
    }

    private fun priorityRank(priority: String): Int = when (priority.uppercase()) {
        "HIGH", "CRITICAL", "URGENT" -> 0
        "MEDIUM" -> 1
        else -> 2
    }
}
