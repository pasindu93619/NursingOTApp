package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.domain.calculation.ShiftEntry
import com.pasindu.nursingotapp.domain.calculation.WeeklyCalculationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Domain boundary for clinical planning persistence.
 * UI/ViewModels depend on these use cases rather than Room directly.
 */
class ObserveIsbarNotesUseCase(private val dao: ClinicalPlanningDao) {
    operator fun invoke(): Flow<List<IsbarNoteEntity>> = dao.getAllIsbarNotes()
}

class ObserveClinicalTasksUseCase(private val dao: ClinicalPlanningDao) {
    operator fun invoke(): Flow<List<ClinicalTaskEntity>> = dao.getAllTasks()
}

class AddIsbarNoteUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String
    ) {
        require(patientId.isNotBlank()) { "Patient identifier cannot be blank." }
        require(identification.isNotBlank()) { "Identification cannot be blank." }

        dao.insertIsbarNote(
            IsbarNoteEntity(
                patientIdentifier = patientId.trim(),
                identification = identification.trim(),
                situation = situation.trim(),
                background = background.trim(),
                assessment = assessment.trim(),
                recommendation = recommendation.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

class UpdateIsbarNoteUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        id: Int,
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String,
        originalTimestamp: Long
    ) {
        require(id > 0) { "ISBAR note id must be positive." }
        require(patientId.isNotBlank()) { "Patient identifier cannot be blank." }
        require(identification.isNotBlank()) { "Identification cannot be blank." }

        dao.updateIsbarNote(
            IsbarNoteEntity(
                id = id,
                patientIdentifier = patientId.trim(),
                identification = identification.trim(),
                situation = situation.trim(),
                background = background.trim(),
                assessment = assessment.trim(),
                recommendation = recommendation.trim(),
                timestamp = originalTimestamp
            )
        )
    }
}

class DeleteIsbarNoteUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(note: IsbarNoteEntity) {
        require(note.id > 0) { "ISBAR note id must be positive." }
        dao.deleteIsbarNote(note)
    }
}

class PurgeOldIsbarNotesUseCase(private val dao: ClinicalPlanningDao) {
    companion object {
        const val RETENTION_HOURS = 48L
        const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }

    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()) {
        val cutoff = nowMillis - (RETENTION_HOURS * MILLIS_PER_HOUR)
        dao.deleteOldNotes(cutoff)
    }
}

class AddClinicalTaskUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean
    ) {
        require(taskName.isNotBlank()) { "Task name cannot be blank." }
        require(priority.isNotBlank()) { "Task priority cannot be blank." }
        require(triggerTime >= 0L) { "Task trigger time cannot be negative." }

        dao.insertTask(
            ClinicalTaskEntity(
                taskName = taskName.trim(),
                description = description.trim(),
                priority = priority.trim(),
                triggerTime = triggerTime,
                isCompleted = false,
                bypassDnd = bypassDnd
            )
        )
    }
}

class UpdateClinicalTaskUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(
        taskId: Int,
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        isCompleted: Boolean,
        bypassDnd: Boolean
    ) {
        require(taskId > 0) { "Clinical task id must be positive." }
        require(taskName.isNotBlank()) { "Task name cannot be blank." }
        require(priority.isNotBlank()) { "Task priority cannot be blank." }
        require(triggerTime >= 0L) { "Task trigger time cannot be negative." }

        dao.updateTask(
            ClinicalTaskEntity(
                id = taskId,
                taskName = taskName.trim(),
                description = description.trim(),
                priority = priority.trim(),
                triggerTime = triggerTime,
                isCompleted = isCompleted,
                bypassDnd = bypassDnd
            )
        )
    }
}

class DeleteClinicalTaskUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(task: ClinicalTaskEntity) {
        require(task.id > 0) { "Clinical task id must be positive." }
        dao.deleteTask(task)
    }
}

class SetClinicalTaskCompletedUseCase(private val dao: ClinicalPlanningDao) {
    suspend operator fun invoke(taskId: Int, completed: Boolean) {
        require(taskId > 0) { "Clinical task id must be positive." }
        dao.setTaskCompleted(taskId, completed)
    }
}

/**
 * Workload aggregation model for Clinical Planning dashboard.
 * Purely an operational workload/recovery indicator, not a medical/clinical diagnosis.
 */
data class ClinicalWorkloadState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val avgWeeklyHours: Float,
    val consecutiveNightShifts: Int,
    val suggestionText: String,
    val totalHours: Float,
    val periodDays: Int,
    val weeksEquivalent: Float,
    val isWeeklyView: Boolean = false,
    val periodLabel: String = "",
    val hasRecordedShifts: Boolean = false
)

private data class WorkloadScope(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val label: String,
    val isClaimPeriod: Boolean
)

class ObserveShiftWorkloadUseCase(
    private val dailyEntryDao: DailyEntryDao,
    private val claimPeriodDao: ClaimPeriodDao
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<ClinicalWorkloadState> {
        return combine(
            dailyEntryDao.observeAllEntries(),
            claimPeriodDao.observeClaimPeriods()
        ) { allEntries, claimPeriods ->
            calculateWorkload(allEntries, claimPeriods, today)
        }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

        fun parseToLocalTime(timeStr: String?): LocalTime? {
            if (timeStr.isNullOrBlank() || timeStr == "-") return null
            return try {
                val parts = timeStr.replace(".", ":").split(":")
                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()
                if (hour in 0..23 && minute in 0..59) {
                    LocalTime.of(hour, minute)
                } else null
            } catch (_: Exception) {
                null
            }
        }

        fun calculateWorkload(
            allEntries: List<DailyEntryEntity>,
            claimPeriods: List<ClaimPeriodEntity>,
            today: LocalDate = LocalDate.now()
        ): ClinicalWorkloadState {
            val activeClaimPeriod = claimPeriods
                .asSequence()
                .filter { period ->
                    !today.isBefore(period.startDate) && !today.isAfter(period.endDate)
                }
                .maxByOrNull { it.startDate }

            val scope = if (activeClaimPeriod != null) {
                WorkloadScope(
                    startDate = activeClaimPeriod.startDate,
                    endDate = activeClaimPeriod.endDate,
                    label = "Claim Period: ${activeClaimPeriod.startDate.format(dateFormatter)} – ${activeClaimPeriod.endDate.format(dateFormatter)}",
                    isClaimPeriod = true
                )
            } else {
                val start = today.minusDays(6)
                WorkloadScope(
                    startDate = start,
                    endDate = today,
                    label = "Last 7 Days: ${start.format(dateFormatter)} – ${today.format(dateFormatter)}",
                    isClaimPeriod = false
                )
            }

            val filteredEntries = if (scope.isClaimPeriod && activeClaimPeriod != null) {
                allEntries.filter { entry ->
                    entry.claimPeriodId == activeClaimPeriod.id &&
                        !entry.date.isBefore(scope.startDate) &&
                        !entry.date.isAfter(scope.endDate)
                }
            } else {
                allEntries.filter { entry ->
                    !entry.date.isBefore(scope.startDate) && !entry.date.isAfter(scope.endDate)
                }
            }

            val shiftEntries = filteredEntries.mapNotNull { entry ->
                val start = parseToLocalTime(entry.normalTimeIn)
                val end = parseToLocalTime(entry.otTimeOut) ?: parseToLocalTime(entry.normalTimeOut)
                if (start != null && end != null) {
                    ShiftEntry(entry.date, start, end, entry.isPH, entry.isDO)
                } else {
                    null
                }
            }

            val daysInPeriod = (ChronoUnit.DAYS.between(scope.startDate, scope.endDate) + 1).coerceAtLeast(1L)
            val weeksEquivalent = (daysInPeriod / 7.0).toFloat()

            if (shiftEntries.isEmpty()) {
                val explanation = if (scope.isClaimPeriod) {
                    "No recorded duty shifts in active claim period. Log shifts in Daily Entry to calculate weekly workload and recovery signals."
                } else {
                    "No recorded duty shifts in the last 7 days. Log shifts in Daily Entry to calculate weekly workload and recovery signals."
                }
                return ClinicalWorkloadState(
                    startDate = scope.startDate,
                    endDate = scope.endDate,
                    avgWeeklyHours = 0f,
                    consecutiveNightShifts = 0,
                    suggestionText = explanation,
                    totalHours = 0f,
                    periodDays = daysInPeriod.toInt(),
                    weeksEquivalent = weeksEquivalent,
                    periodLabel = scope.label,
                    hasRecordedShifts = false
                )
            }

            val totalHours = WeeklyCalculationEngine.calculateTotalHoursForCalendarPeriod(
                shiftEntries,
                scope.startDate,
                scope.endDate
            ).toFloat().coerceAtLeast(0f)

            val consecutiveNights = WeeklyCalculationEngine.calculateMaxConsecutiveNightShifts(shiftEntries)
            val avgWeeklyHours = (totalHours / daysInPeriod * 7.0f).coerceAtLeast(0f)

            val suggestion = when {
                avgWeeklyHours > 48f -> "Workload alert: Averaging ${avgWeeklyHours.toInt()}h/week. Exceeds safe practice threshold (48h max). Prioritize rest and recovery."
                consecutiveNights > 3 -> "Workload notice: $consecutiveNights consecutive night shifts detected. Ensure adequate circadian recovery."
                avgWeeklyHours > 40f -> "Workload notice: Averaging ${avgWeeklyHours.toInt()}h/week with elevated overtime. Monitor shift intervals and fatigue."
                else -> "Workload optimal: Averaging ${avgWeeklyHours.toInt()}h/week. Standard nursing workload within safe operational thresholds."
            }

            return ClinicalWorkloadState(
                startDate = scope.startDate,
                endDate = scope.endDate,
                avgWeeklyHours = avgWeeklyHours,
                consecutiveNightShifts = consecutiveNights,
                suggestionText = suggestion,
                totalHours = totalHours,
                periodDays = daysInPeriod.toInt(),
                weeksEquivalent = weeksEquivalent,
                periodLabel = scope.label,
                hasRecordedShifts = true
            )
        }
    }
}
