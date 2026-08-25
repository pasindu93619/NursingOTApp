package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "isbar_notes")
data class IsbarNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientIdentifier: String, // Keep anonymized/encrypted
    val identification: String,
    val situation: String,
    val background: String,
    val assessment: String,
    val recommendation: String,
    val timestamp: Long // Used to trigger the 48-hour auto-purge
)