package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ClinicalWorkloadState
import com.pasindu.nursingotapp.domain.usecase.DeleteClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.DeleteIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveShiftWorkloadUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.RestoreClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.RestoreIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.UpdateIsbarNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel
class ClinicalPlanningViewModel @Inject constructor(
    observeIsbarNotesUseCase: ObserveIsbarNotesUseCase,
    observeClinicalTasksUseCase: ObserveClinicalTasksUseCase,
    observeShiftWorkloadUseCase: ObserveShiftWorkloadUseCase,
    private val addIsbarNoteUseCase: AddIsbarNoteUseCase,
    private val updateIsbarNoteUseCase: UpdateIsbarNoteUseCase,
    private val deleteIsbarNoteUseCase: DeleteIsbarNoteUseCase,
    private val restoreIsbarNoteUseCase: RestoreIsbarNoteUseCase,
    private val purgeOldIsbarNotesUseCase: PurgeOldIsbarNotesUseCase,
    private val addClinicalTaskUseCase: AddClinicalTaskUseCase,
    private val updateClinicalTaskUseCase: UpdateClinicalTaskUseCase,
    private val deleteClinicalTaskUseCase: DeleteClinicalTaskUseCase,
    private val restoreClinicalTaskUseCase: RestoreClinicalTaskUseCase,
    private val setClinicalTaskCompletedUseCase: SetClinicalTaskCompletedUseCase
) : ViewModel() {

    val isbarNotes: StateFlow<List<IsbarNoteEntity>> = observeIsbarNotesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val clinicalTasks: StateFlow<List<ClinicalTaskEntity>> = observeClinicalTasksUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val workloadState: StateFlow<ClinicalWorkloadState> = observeShiftWorkloadUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ClinicalWorkloadState(
                startDate = LocalDate.now().minusDays(6),
                endDate = LocalDate.now(),
                avgWeeklyHours = 0f,
                consecutiveNightShifts = 0,
                suggestionText = "Loading shift records...",
                totalHours = 0f,
                periodDays = 7,
                weeksEquivalent = 1f,
                periodLabel = "Initializing...",
                hasRecordedShifts = false
            )
        )

    fun addIsbarNote(
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            addIsbarNoteUseCase(
                patientId = patientId,
                identification = identification,
                situation = situation,
                background = background,
                assessment = assessment,
                recommendation = recommendation
            )
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun updateIsbarNote(
        id: Int,
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String,
        originalTimestamp: Long,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            updateIsbarNoteUseCase(
                id = id,
                patientId = patientId,
                identification = identification,
                situation = situation,
                background = background,
                assessment = assessment,
                recommendation = recommendation,
                originalTimestamp = originalTimestamp
            )
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun deleteIsbarNote(
        note: IsbarNoteEntity,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            deleteIsbarNoteUseCase(note)
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun restoreIsbarNote(
        note: IsbarNoteEntity,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            restoreIsbarNoteUseCase(note)
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    suspend fun saveIsbarNote(
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String
    ): Result<Unit> = try {
        addIsbarNoteUseCase(
            patientId = patientId,
            identification = identification,
            situation = situation,
            background = background,
            assessment = assessment,
            recommendation = recommendation
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    fun purgeOldIsbarNotes() = viewModelScope.launch {
        purgeOldIsbarNotesUseCase()
    }

    fun addTask(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            addClinicalTaskUseCase(
                taskName = taskName,
                description = description,
                priority = priority,
                triggerTime = triggerTime,
                bypassDnd = bypassDnd
            )
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun updateTask(
        taskId: Int,
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        isCompleted: Boolean,
        bypassDnd: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            updateClinicalTaskUseCase(
                taskId = taskId,
                taskName = taskName,
                description = description,
                priority = priority,
                triggerTime = triggerTime,
                isCompleted = isCompleted,
                bypassDnd = bypassDnd
            )
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun deleteTask(
        task: ClinicalTaskEntity,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            deleteClinicalTaskUseCase(task)
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun restoreTask(
        task: ClinicalTaskEntity,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            restoreClinicalTaskUseCase(task)
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    suspend fun saveClinicalTask(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean
    ): Result<Unit> = try {
        addClinicalTaskUseCase(
            taskName = taskName,
            description = description,
            priority = priority,
            triggerTime = triggerTime,
            bypassDnd = bypassDnd
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    fun setTaskCompleted(taskId: Int, completed: Boolean) = viewModelScope.launch {
        setClinicalTaskCompletedUseCase(taskId, completed)
    }
}
