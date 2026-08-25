package com.pasindu.nursingotapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.DatabaseProvider
import com.pasindu.nursingotapp.data.local.dao.KnowledgeHubDao
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity
import com.pasindu.nursingotapp.domain.calculation.KnowledgeHubEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CircularItem(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val category: String
)

data class FlashcardItem(
    val question: String,
    val answer: String
)

class KnowledgeHubViewModel(application: Application) : AndroidViewModel(application) {

    private val knowledgeHubDao: KnowledgeHubDao = DatabaseProvider.getDatabase(application).knowledgeHubDao()

    val cpdLogs: StateFlow<List<CpdLogEntity>> = knowledgeHubDao.getAllCpdLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _circulars = MutableStateFlow<List<CircularItem>>(emptyList())
    val circulars: StateFlow<List<CircularItem>> = _circulars.asStateFlow()

    private val _flashcards = MutableStateFlow<List<FlashcardItem>>(emptyList())
    val flashcards: StateFlow<List<FlashcardItem>> = _flashcards.asStateFlow()

    init {
        loadMockCirculars()
        loadDefaultFlashcards()
    }

    fun addCpdLog(
        title: String,
        earnedPoints: Int,
        institution: String,
        notes: String
    ) {
        viewModelScope.launch {
            val log = CpdLogEntity(
                seminarTitle = title,
                date = System.currentTimeMillis(),
                earnedPoints = earnedPoints,
                speakerOrInstitution = institution,
                notes = notes
            )
            knowledgeHubDao.insertCpdLog(log)
        }
    }

    private fun loadMockCirculars() {
        _circulars.value = listOf(
            CircularItem(
                id = "MOH-2026-04",
                title = "Infection Control & PPE Protocols in Oncology Units",
                date = "2026-05-12",
                summary = "Updated Ministry of Health standard operating procedures for central line dressing changes and cytotoxic waste disposal.",
                category = "Clinical Protocol"
            ),
            CircularItem(
                id = "MOH-2026-08",
                title = "Emergency Resuscitation & Defibrillator Checklist Compliance",
                date = "2026-06-01",
                summary = "Mandatory daily shift verification of crash cart emergency trays and pediatric laryngoscope blade readiness.",
                category = "Patient Safety"
            ),
            CircularItem(
                id = "MOH-2026-11",
                title = "Revised Night Shift Resting Hours and Roster Allocation",
                date = "2026-07-15",
                summary = "Guidelines ensuring maximum consecutive night shift limits to mitigate clinical fatigue and safeguard medication administration.",
                category = "Administration"
            )
        )
    }

    private fun loadDefaultFlashcards() {
        _flashcards.value = listOf(
            FlashcardItem(
                question = "What is the optimal infusion rate check interval for high-risk inotropes?",
                answer = "Every 15 to 30 minutes, with dual-nurse verification of syringe pump settings."
            ),
            FlashcardItem(
                question = "According to ISBAR, what does the 'B' stand for and require?",
                answer = "Background: Clinical history, admitting diagnosis, recent procedures, and baseline vitals."
            ),
            FlashcardItem(
                question = "What is the APIT tax-free monthly threshold from 2025 onwards in Sri Lanka?",
                answer = "LKR 150,000 per month (earnings above this are taxed progressively starting at 6%)."
            )
        )
    }
}