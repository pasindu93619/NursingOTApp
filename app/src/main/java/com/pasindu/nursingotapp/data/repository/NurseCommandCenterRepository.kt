package com.pasindu.nursingotapp.data.repository

import com.pasindu.nursingotapp.data.local.AppDatabase
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
        val cpdPoints: Int
    )

    fun observeSnapshot(
        month: YearMonth = YearMonth.now()
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

            // Prefer the financial record for this month. Fall back to the
            // newest stored record, then finally to the saved basic salary.
            val currentMonthKey = month.toString()
            val currentMonthFinance = finance.firstOrNull {
                it.recordMonth == currentMonthKey
            }
            val latestFinance = currentMonthFinance ?: finance.firstOrNull()

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
                grossSalary = latestFinance?.grossSalary
                    ?: currentProfile?.basicSalary
                    ?: 0.0,
                netSalary = latestFinance?.netSalary
                    ?: currentProfile?.basicSalary
                    ?: 0.0,
                pendingClinicalTasks = clinicalTasks.count { !it.isCompleted },
                cpdPoints = cpdLogs.sumOf { it.earnedPoints }
            )
        }
    }
}
