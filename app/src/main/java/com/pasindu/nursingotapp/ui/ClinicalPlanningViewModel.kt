package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ClinicalPlanningViewModel @Inject constructor(
    private val clinicalDao: ClinicalPlanningDao
) : ViewModel() {

    val isbarNotes: StateFlow<List<IsbarNoteEntity>> = clinicalDao.getAllIsbarNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val clinicalTasks: StateFlow<List<ClinicalTaskEntity>> = clinicalDao.getAllTasks()
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
    ) {
        viewModelScope.launch {
            clinicalDao.insertIsbarNote(
                IsbarNoteEntity(
                    patientIdentifier = patientId,
                    identification = identification,
                    situation = situation,
                    background = background,
                    assessment = assessment,
                    recommendation = recommendation,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun purgeOldIsbarNotes() {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
            clinicalDao.deleteOldNotes(cutoff)
        }
    }

    fun addTask(
        taskName: String,
        description: String,
        priority: String,
        triggerTime: Long,
        bypassDnd: Boolean
    ) {
        viewModelScope.launch {
            clinicalDao.insertTask(
                ClinicalTaskEntity(
                    taskName = taskName,
                    description = description,
                    priority = priority,
                    triggerTime = triggerTime,
                    isCompleted = false,
                    bypassDnd = bypassDnd
                )
            )
        }
    }
}
