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
 * This keeps Room details out of the Compose UI and gives the command center
 * one consistent stream that can be expanded later without changing the UI.
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
        val monthEntries = dailyEntryDao.observeAllEntries()
        val financialRecords = financialDao.getAllFinancialRecords()
        val tasks = clinicalPlanningDao.getAllTasks()
        val cpdLogs = knowledgeHubDao.getAllCpdLogs()
        val profile = profileDao.observeProfile()

        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        return combine(
            profile,
            monthEntries,
            financialRecords,
            tasks,
            cpdLogs
        ) { currentProfile, entries, finance, clinicalTasks, cpd ->
            val monthlyEntries = entries.filter { entry ->
                entry.date >= start && entry.date <= end
            }

            val latestFinance = finance.firstOrNull()

            Snapshot(
                profile = currentProfile,
                dutyHours = monthlyEntries.sumOf { it.normalHours.toDouble() },
                otHours = monthlyEntries.sumOf { it.otHours.toDouble() },
                phHours = monthlyEntries
                    .filter { it.isPH }
                    .sumOf { maxOf(it.normalHours.toDouble(), it.otHours.toDouble()) },
                claimCompletedDays = monthlyEntries.count {
                    !it.isLeave && (it.normalHours > 0f || it.otHours > 0f || it.isPH || it.isDO)
                },
                claimTotalDays = end.dayOfMonth,
                grossSalary = latestFinance?.grossSalary ?: currentProfile?.basicSalary ?: 0.0,
                netSalary = latestFinance?.netSalary ?: currentProfile?.basicSalary ?: 0.0,
                pendingClinicalTasks = clinicalTasks.count { !it.isCompleted },
                cpdPoints = cpd.sumOf { it.earnedPoints }
            )
        }
    }
}
