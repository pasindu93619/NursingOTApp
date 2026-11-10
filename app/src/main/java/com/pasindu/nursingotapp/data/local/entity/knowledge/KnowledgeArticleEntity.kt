package com.pasindu.nursingotapp.data.local.entity.knowledge

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_articles",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceInstitution"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sourceInstitution"]),
        Index(value = ["category"]),
        Index(value = ["evidenceGrade"]),
        Index(value = ["year"])
    ]
)
data class KnowledgeArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sourceInstitution: String,
    val category: String,
    val clinicalSummary: String,
    val recommendationsJson: String,
    val evidenceGrade: String,
    val year: Int,
    val lastReviewed: String,
    val sourceUrl: String? = null,
    val circularNumber: String? = null,
    val effectiveDate: String? = null,
    val isFullTextAvailable: Boolean = false,
    val licenseNotice: String? = null,
    val sourceUrlDisclaimer: String? = null,
    val isSuperseded: Boolean = false,
    val supersededByArticleId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
