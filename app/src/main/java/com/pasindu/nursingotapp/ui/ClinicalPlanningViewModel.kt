package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import com.pasindu.nursingotapp.domain.usecase.AddClinicalTaskUseCase
import com.pasindu.nursingotapp.domain.usecase.AddIsbarNoteUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClinicalTasksUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.PurgeOldIsbarNotesUseCase
import com.pasindu.nursingotapp.domain.usecase.SetClinicalTaskCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ClinicalPlanningViewModel @Inject constructor(
    observeIsbarNotesUseCase: ObserveIsbarNotesUseCase,
    observeClinicalTasksUseCase: ObserveClinicalTasksUseCase,
    private val addIsbarNoteUseCase: AddIsbarNoteUseCase,
    private val purgeOldIsbarNotesUseCase: PurgeOldIsbarNotesUseCase,
    private val addClinicalTaskUseCase: AddClinicalTaskUseCase,
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

    fun addIsbarNote(
        patientId: String,
        identification: String,
        situation: String,
        background: String,
        assessment: String,
        recommendation: String
    ) = viewModelScope.launch {
        addIsbarNoteUseCase(
            patientId,
            identification,
            situation,
            background,
            assessment,
            recommendation
        )
    }

    fun purgeOldIsbarNotes() = viewModelScope.launch {
        purgeOldIsbarNotesUseCase()
    }

    fun addTask(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean
    ) = viewModelScope.launch {
        addClinicalTaskUseCase(
            taskName,
            description,
            priority,
            triggerTime,
            bypassDnd
        )
    }

    fun setTaskCompleted(taskId: Int, completed: Boolean) = viewModelScope.launch {
        setClinicalTaskCompletedUseCase(taskId, completed)
    }
}
