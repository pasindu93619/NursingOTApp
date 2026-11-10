package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity

@Entity(
    tableName = "knowledge_reading_history",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CpdLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedCpdLogId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["linkedCpdLogId"])
    ]
)
data class KnowledgeReadingHistoryEntity(
    @PrimaryKey
    val articleId: String,
    val lastReadAt: Long = System.currentTimeMillis(),
    val readingProgressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val linkedCpdLogId: Int? = null
)
