package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_personal_notes",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["articleId"])
    ]
)
data class KnowledgePersonalNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val articleId: String? = null,
    val originalArticleId: String,
    val noteContent: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
