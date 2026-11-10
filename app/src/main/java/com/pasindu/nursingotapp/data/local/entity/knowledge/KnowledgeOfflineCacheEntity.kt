package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_offline_cache",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["localRelativePath"], unique = true)
    ]
)
data class KnowledgeOfflineCacheEntity(
    @PrimaryKey
    val articleId: String,
    val localRelativePath: String? = null,
    val mimeType: String = "application/pdf",
    val fileSizeBytes: Long = 0L,
    val sha256Hash: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isFullTextCached: Int = 1
)
