package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinical_tasks")
data class ClinicalTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val description: String,
    val priority: String, // e.g., "HIGH", "MEDIUM", "LOW"
    val triggerTime: Long, // Epoch time for the alarm
    val isCompleted: Boolean = false,
    val bypassDnd: Boolean = false // True for critical events like IV antibiotics
)