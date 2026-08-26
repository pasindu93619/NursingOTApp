package com.pasindu.nursingotapp.data.repository

import com.pasindu.nursingotapp.data.local.AppDatabase
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        val grossSalary: Double,
        val netSalary: Double,
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
            dailyEntryDao.observeAllEntries(),
            financialDao.getAllFinancialRecords(),
            clinicalPlanningDao.getAllTasks(),
            knowledgeHubDao.getAllCpdLogs()
        ) { currentProfile, entries, finance, clinicalTasks, cpdLogs ->
            val monthlyEntries = entries.filter { entry ->
                entry.date >= start && entry.date <= end
            }

            val todayEntry = entries
                .filter { it.date == today }
                .maxByOrNull { it.id }

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

            Snapshot(
                profile = currentProfile,
                dutyHours = monthlyEntries.sumOf { it.normalHours.toDouble() },
                otHours = monthlyEntries.sumOf { it.otHours.toDouble() },
                phHours = monthlyEntries
                    .filter { it.isPH }
                    .sumOf { it.normalHours.toDouble() + it.otHours.toDouble() },
                claimCompletedDays = monthlyEntries.count {
                    !it.isLeave && (
                        it.normalHours > 0f ||
                            it.otHours > 0f ||
                            it.isPH ||
                            it.isDO
                        )
                },
                claimTotalDays = end.dayOfMonth,
                grossSalary = currentMonthFinance?.grossSalary
                    ?: currentProfile?.basicSalary
                    ?: 0.0,
                netSalary = currentMonthFinance?.netSalary
                    ?: currentProfile?.basicSalary
                    ?: 0.0,
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

    suspend fun setClinicalTaskCompleted(taskId: Int, completed: Boolean = true) {
        clinicalPlanningDao.setTaskCompleted(taskId, completed)
    }

    private fun priorityRank(priority: String): Int = when (priority.uppercase()) {
        "HIGH", "CRITICAL", "URGENT" -> 0
        "MEDIUM" -> 1
        else -> 2
    }
}
