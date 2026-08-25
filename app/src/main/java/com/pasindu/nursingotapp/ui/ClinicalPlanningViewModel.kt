package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.ClinicalPlanningDao
import com.pasindu.nursingotapp.data.local.entity.ClinicalTaskEntity
import com.pasindu.nursingotapp.data.local.entity.IsbarNoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClinicalPlanningViewModel(application: Application) : AndroidViewModel(application) {

    private val clinicalDao: ClinicalPlanningDao = DatabaseProvider.getDatabase(application).clinicalPlanningDao()

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
            val note = IsbarNoteEntity(
                patientIdentifier = patientId,
                identification = identification,
                situation = situation,
                background = background,
                assessment = assessment,
                recommendation = recommendation,
                timestamp = System.currentTimeMillis()
            )
            clinicalDao.insertIsbarNote(note)
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
            val task = ClinicalTaskEntity(
                taskName = taskName,
                description = description,
                priority = priority,
                triggerTime = triggerTime,
                isCompleted = false,
                bypassDnd = bypassDnd
            )
            clinicalDao.insertTask(task)
        }
    }
}