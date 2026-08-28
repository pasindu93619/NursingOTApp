package com.pasindu.nursingotapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pay_sheet_documents",
    indices = [Index(value = ["monthKey"], unique = true)]
)
data class PaySheetDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthKey: String,
    val displayMonth: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val createdAt: Long,
    val updatedAt: Long
)
